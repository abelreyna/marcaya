package com.juanreyna.registrotiempo.util

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

enum class PasoVida(val instruccion: String) {
    BUSCANDO_ROSTRO("Ubica tu rostro dentro del óvalo"),
    PARPADEA("Parpadea lentamente"),
    GIRA_CABEZA("Gira ligeramente la cabeza a un lado"),
    VERIFICADO("Rostro verificado"),
    VARIOS_ROSTROS("Solo debe aparecer una persona")
}

/**
 * Prueba de vida activa con ML Kit (en dispositivo):
 * 1) rostro presente → 2) parpadeo (ojos abiertos → cerrados → abiertos) → 3) giro de cabeza.
 * Cada fotograma se procesa en RAM y se libera de inmediato (imageProxy.close()); nunca se guarda ni se sube.
 */
class LivenessAnalyzer(private val alCambiarPaso: (PasoVida) -> Unit) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.25f)
            .build()
    )

    private var paso = PasoVida.BUSCANDO_ROSTRO
    private var ojosCerradosVistos = false
    private var anguloInicial: Float? = null

    private fun emitir(nuevo: PasoVida) {
        if (nuevo != paso) { paso = nuevo; alCambiarPaso(nuevo) }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null || paso == PasoVida.VERIFICADO) { imageProxy.close(); return }
        val imagen = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        detector.process(imagen)
            .addOnSuccessListener { caras ->
                when {
                    caras.isEmpty() -> if (paso != PasoVida.BUSCANDO_ROSTRO) reiniciar()
                    caras.size > 1 -> emitir(PasoVida.VARIOS_ROSTROS)
                    else -> evaluar(caras[0])
                }
            }
            .addOnCompleteListener { imageProxy.close() } // descarte inmediato del fotograma
    }

    private fun evaluar(cara: com.google.mlkit.vision.face.Face) {
        val izq = cara.leftEyeOpenProbability ?: return
        val der = cara.rightEyeOpenProbability ?: return
        when (paso) {
            PasoVida.BUSCANDO_ROSTRO, PasoVida.VARIOS_ROSTROS -> {
                if (izq > 0.7f && der > 0.7f) emitir(PasoVida.PARPADEA)
            }
            PasoVida.PARPADEA -> {
                if (izq < 0.2f && der < 0.2f) ojosCerradosVistos = true
                if (ojosCerradosVistos && izq > 0.7f && der > 0.7f) {
                    anguloInicial = cara.headEulerAngleY
                    emitir(PasoVida.GIRA_CABEZA)
                }
            }
            PasoVida.GIRA_CABEZA -> {
                val base = anguloInicial ?: cara.headEulerAngleY.also { anguloInicial = it }
                if (abs(cara.headEulerAngleY - base) > 15f) emitir(PasoVida.VERIFICADO)
            }
            PasoVida.VERIFICADO -> Unit
        }
    }

    private fun reiniciar() {
        ojosCerradosVistos = false
        anguloInicial = null
        emitir(PasoVida.BUSCANDO_ROSTRO)
    }

    fun cerrar() = detector.close()
}
