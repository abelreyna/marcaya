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
        val clientId = FirebaseState.webClientId(activity)
            ?: error("Falta default_web_client_id: agrega app/google-services.json con Google Sign-In habilitado.")
        val opcion = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
        val resultado = CredentialManager.create(activity).getCredential(activity, request)
        val credencial = resultado.credential
        require(credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Credencial de Google no reconocida"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credencial.data).idToken
        val authResult = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        return authResult.user ?: error("No se pudo iniciar sesión")
    }

    suspend fun cerrarSesion(activity: Activity?) {
        auth.signOut()
        activity?.let { runCatching { CredentialManager.create(it).clearCredentialState(ClearCredentialStateRequest()) } }
    }
}
