package com.juanreyna.registrotiempo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Fotos de perfil sin Firebase Storage (no incluido en el plan gratuito Spark): la imagen elegida se
 * recorta al centro, se reduce a 320×320 px y se guarda como JPEG en Base64 dentro del perfil (~25 KB).
 */
object FotoPerfil {
    private const val LADO = 320
    private const val CALIDAD = 80
    /** Límite de seguridad; las reglas de Firestore aplican el mismo tope. */
    const val MAX_CARACTERES = 200_000

    /** Lee la imagen elegida y la devuelve lista para guardar, o null si no se pudo procesar. */
    fun desdeUri(context: Context, uri: Uri): String? = runCatching {
        val resolver = context.contentResolver
        // 1) Solo dimensiones, para decodificar con submuestreo y no cargar fotos de 12 MP en memoria
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        val menor = minOf(limites.outWidth, limites.outHeight).takeIf { it > 0 } ?: return null
        var muestreo = 1
        while (menor / (muestreo * 2) >= LADO) muestreo *= 2

        val original = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestreo })
        } ?: return null

        // 2) Respeta la orientación EXIF (fotos tomadas en vertical)
        val grados = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        // 3) Recorte cuadrado centrado + escala a 320×320
        val lado = minOf(original.width, original.height)
        val matriz = Matrix().apply {
            postRotate(grados)
            val escala = LADO.toFloat() / lado
            postScale(escala, escala)
        }
        val cuadrado = Bitmap.createBitmap(
            original, (original.width - lado) / 2, (original.height - lado) / 2, lado, lado, matriz, true
        )
        if (cuadrado !== original) original.recycle()

        val salida = ByteArrayOutputStream()
        cuadrado.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
        cuadrado.recycle()
        Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP).takeIf { it.length <= MAX_CARACTERES }
    }.getOrNull()

    /** Convierte la foto guardada en imagen para mostrar; null si está vacía o dañada. */
    fun aBitmap(base64: String): Bitmap? = if (base64.isBlank()) null else runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
