package com.juanreyna.registrotiempo.ui.colaborador

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.ui.theme.VerdeEsmeralda
import com.juanreyna.registrotiempo.util.LivenessAnalyzer
import com.juanreyna.registrotiempo.util.PasoVida
import java.util.concurrent.Executors

/**
 * Verificación facial efímera: la cámara frontal alimenta a ML Kit en memoria;
 * ningún fotograma se guarda en el dispositivo ni se sube a servidores.
 */
@Composable
fun VerificacionFacial(tipo: TipoMarcaje, alVerificar: () -> Unit, alCancelar: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val solicitar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permiso = it }
    LaunchedEffect(Unit) { if (!permiso) solicitar.launch(Manifest.permission.CAMERA) }

    var paso by remember { mutableStateOf(PasoVida.BUSCANDO_ROSTRO) }
    LaunchedEffect(paso) { if (paso == PasoVida.VERIFICADO) alVerificar() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (permiso) {
            val ejecutor = remember { Executors.newSingleThreadExecutor() }
            val analizador = remember { LivenessAnalyzer { nuevo -> paso = nuevo } }
            val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
            var proveedor by remember { mutableStateOf<ProcessCameraProvider?>(null) }
            LaunchedEffect(Unit) {
                val p = ProcessCameraProvider.getInstance(context).await()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { it.setAnalyzer(ejecutor, analizador) }
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analisis)
                proveedor = p
            }
            DisposableEffect(Unit) {
                onDispose {
                    proveedor?.unbindAll()
                    analizador.cerrar()
                    ejecutor.shutdown()
                }
            }
            AndroidView({ previewView }, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width * 0.7f
                val h = w * 1.3f
                drawOval(
                    color = if (paso == PasoVida.VERIFICADO) VerdeEsmeralda else Color.White,
                    topLeft = Offset((size.width - w) / 2, (size.height - h) / 2.4f),
                    size = Size(w, h),
                    style = Stroke(width = 6.dp.toPx())
                )
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Verificación facial — ${tipo.etiqueta}", color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                if (permiso) paso.instruccion else "Se necesita permiso de cámara",
                color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center
            )
            Text("La imagen se procesa en memoria y se descarta al instante.", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            if (!permiso) Button(onClick = { solicitar.launch(Manifest.permission.CAMERA) }) { Text("Conceder permiso") }
            OutlinedButton(onClick = alCancelar) { Text("Cancelar", color = Color.White) }
        }
    }
}
