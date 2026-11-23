package com.juanreyna.registrotiempo.data.repository

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.juanreyna.registrotiempo.data.remote.FirebaseState
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Inicio de sesión exclusivo con Google (Credential Manager) + Firebase Authentication. */
@Singleton
class AuthRepository @Inject constructor() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val usuarioActual: FirebaseUser? get() = auth.currentUser

    suspend fun iniciarConGoogle(activity: Activity): FirebaseUser {
        val idToken = tokenGoogle(activity)
        val authResult = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        return authResult.user ?: error("No se pudo iniciar sesión")
    }

    /** Firebase exige un inicio de sesión reciente para eliminar la cuenta: se vuelve a pedir la cuenta de Google. */
    suspend fun reautenticar(activity: Activity) {
        val u = auth.currentUser ?: error("No hay sesión activa")
        val idToken = tokenGoogle(activity)
        runCatching { u.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await() }
            .onFailure { error("Elige la misma cuenta de Google con la que iniciaste sesión (${u.email.orEmpty()})") }
    }

    /** Elimina el usuario de Firebase Authentication y olvida la credencial guardada. */
    suspend fun eliminarUsuario(activity: Activity?) {
        auth.currentUser?.delete()?.await()
        auth.signOut()
        activity?.let { runCatching { CredentialManager.create(it).clearCredentialState(ClearCredentialStateRequest()) } }
    }

    private suspend fun tokenGoogle(activity: Activity): String {
        val clientId = FirebaseState.webClientId(activity)
            ?: error("Falta default_web_client_id: agrega app/google-services.json con Google Sign-In habilitado.")
        val opcion = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
        val resultado = CredentialManager.create(activity).getCredential(activity, request)
        val credencial = resultado.credential
        require(credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Credencial de Google no reconocida"
        }
        return GoogleIdTokenCredential.createFrom(credencial.data).idToken
    }

    suspend fun cerrarSesion(activity: Activity?) {
        auth.signOut()
        activity?.let { runCatching { CredentialManager.create(it).clearCredentialState(ClearCredentialStateRequest()) } }
    }
}
