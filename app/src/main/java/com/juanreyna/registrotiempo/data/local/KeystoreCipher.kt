package com.juanreyna.registrotiempo.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Cifrado AES-256/GCM con llave no exportable resguardada en Android Keystore. */
object KeystoreCipher {
    private const val ALIAS = "registro_tiempo_sesion"
    private const val PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMACION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12

    private fun llave(): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, llave())
        val datos = cipher.iv + cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(datos, Base64.NO_WRAP)
    }

    fun descifrar(cifrado: String): String? = runCatching {
        val datos = Base64.decode(cifrado, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, datos, 0, IV_BYTES))
        String(cipher.doFinal(datos, IV_BYTES, datos.size - IV_BYTES), Charsets.UTF_8)
    }.getOrNull()
}
