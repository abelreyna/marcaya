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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.util.LivenessAnalyzer
import com.juanreyna.registrotiempo.util.PasoVida
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private val Menta = Color(0xFF2EE6A6)
private val Esmeralda = Color(0xFF0F8B6D)
private val Velo = Color(0xFF0A1C38)
private val AmbarAviso = Color(0xFFFFC145)

/** Índice de avance de la prueba de vida: 0 rostro, 1 parpadeo, 2 giro, 3 verificado. */
private val PasoVida.indice
    get() = when (this) {
        PasoVida.BUSCANDO_ROSTRO, PasoVida.VARIOS_ROSTROS -> 0
        PasoVida.PARPADEA -> 1
        PasoVida.GIRA_CABEZA -> 2
        PasoVida.VERIFICADO -> 3
    }

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
    LaunchedEffect(paso) {
        if (paso == PasoVida.VERIFICADO) {
            delay(900) // deja ver la confirmación antes de registrar el marcaje
            alVerificar()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Velo)) {
        val densidad = LocalDensity.current
        // El óvalo se ajusta al espacio libre entre la barra superior y el panel inferior
        var finBarra by remember { mutableStateOf(0f) }
        var inicioPanel by remember { mutableStateOf(0f) }
        val libreDp = with(densidad) { (if (inicioPanel > 0f) inicioPanel else constraints.maxHeight * 0.62f).toDp() - finBarra.toDp() }
        val altoOvalo = minOf(maxWidth * 0.70f * 1.3f, libreDp - 56.dp).coerceAtLeast(160.dp)
        val anchoOvalo = altoOvalo / 1.3f
        val centroY = with(densidad) { finBarra.toDp() } + libreDp / 2

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
            MascaraEscaneo(paso, anchoOvalo.value, altoOvalo.value, centroY.value)

            // Confirmación sobre el rostro
            val escalaCheck by animateFloatAsState(
                if (paso == PasoVida.VERIFICADO) 1f else 0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "check"
            )
            if (escalaCheck > 0.01f) {
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = centroY - 44.dp).size(88.dp).scale(escalaCheck)
                        .clip(CircleShape).background(Esmeralda).border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(54.dp)) }
            }
        } else {
            SinPermiso(Modifier.align(Alignment.Center)) { solicitar.launch(Manifest.permission.CAMERA) }
        }

        // Barra superior
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
                .onGloballyPositioned { finBarra = it.boundsInRoot().bottom },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = alCancelar, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f))) {
                Icon(Icons.Default.Close, "Cancelar", tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Verificación facial", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Marcaje de ${tipo.etiqueta.lowercase()}", color = Menta,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(48.dp))
        }

        // Panel inferior: instrucción, pasos y privacidad
        if (permiso) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                    .onGloballyPositioned { inicioPanel = it.boundsInRoot().top }
                    .padding(horizontal = 20.dp).padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = paso,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "instruccion"
                ) { p ->
                    Text(
                        p.instruccion, color = if (p == PasoVida.VARIOS_ROSTROS) AmbarAviso else Color.White,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when (paso) {
                        PasoVida.BUSCANDO_ROSTRO -> "Mantén el celular a la altura de tus ojos, con buena luz."
                        PasoVida.PARPADEA -> "Cierra y abre los ojos una vez."
                        PasoVida.GIRA_CABEZA -> "Un giro suave hacia la izquierda o la derecha."
                        PasoVida.VARIOS_ROSTROS -> "Asegúrate de estar solo frente a la cámara."
                        PasoVida.VERIFICADO -> "Registrando tu ${tipo.etiqueta.lowercase()}…"
                    },
                    color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                IndicadorPasos(paso.indice)
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = Menta, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Se procesa en tu celular · no se guarda",
                        color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/** Velo oscuro con ventana ovalada, anillo de progreso, esquinas guía y línea de escaneo. */
@Composable
private fun MascaraEscaneo(paso: PasoVida, anchoDp: Float, altoDp: Float, centroYDp: Float) {
    val progreso by animateFloatAsState(
        when (paso.indice) { 0 -> 0.06f; 1 -> 0.36f; 2 -> 0.68f; else -> 1f },
        tween(600, easing = FastOutSlowInEasing), label = "progreso"
    )
    val colorAnillo by animateColorAsState(
        when (paso) {
            PasoVida.VARIOS_ROSTROS -> AmbarAviso
            PasoVida.VERIFICADO -> Esmeralda
            else -> Menta
        }, label = "anillo"
    )
    val infinita = rememberInfiniteTransition(label = "escaneo")
    val barrido by infinita.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "barrido"
    )
    val pulso by infinita.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulso"
    )
    val verificado = paso == PasoVida.VERIFICADO

    Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val w = anchoDp.dp.toPx()
        val h = altoDp.dp.toPx()
        val ovalo = Rect(Offset((size.width - w) / 2f, centroYDp.dp.toPx() - h / 2f), Size(w, h))

        // Velo con degradado y ventana transparente
        drawRect(Brush.verticalGradient(listOf(Velo.copy(alpha = 0.92f), Velo.copy(alpha = 0.78f), Velo.copy(alpha = 0.96f))))
        drawOval(Color.Black, ovalo.topLeft, ovalo.size, blendMode = BlendMode.Clear)

        val trazo = 4.dp.toPx()
        val anillo = Rect(ovalo.left - 10.dp.toPx(), ovalo.top - 10.dp.toPx(), ovalo.right + 10.dp.toPx(), ovalo.bottom + 10.dp.toPx())

        // Pista y progreso
        drawOval(Color.White.copy(alpha = 0.18f), anillo.topLeft, anillo.size, style = Stroke(trazo))
        drawArc(
            colorAnillo, startAngle = -90f, sweepAngle = 360f * progreso, useCenter = false,
            topLeft = anillo.topLeft, size = anillo.size, style = Stroke(trazo, cap = StrokeCap.Round)
        )
        // Borde fino del óvalo
        drawOval(Color.White.copy(alpha = if (verificado) 0.9f else 0.55f), ovalo.topLeft, ovalo.size, style = Stroke(1.5.dp.toPx()))

        val forma = Path().apply { addOval(ovalo) }
        clipPath(forma) {
            if (verificado) {
                drawRect(Esmeralda.copy(alpha = 0.28f), ovalo.topLeft, ovalo.size)
            } else {
                // Línea de escaneo con estela
                val y = ovalo.top + ovalo.height * barrido
                val estela = 70.dp.toPx()
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, colorAnillo.copy(alpha = 0.30f)), y - estela, y),
                    Offset(ovalo.left, y - estela), Size(ovalo.width, estela)
                )
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, colorAnillo, Color.Transparent), ovalo.left, ovalo.right),
                    Offset(ovalo.left, y), Offset(ovalo.right, y), strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        // Esquinas guía (marco de enfoque)
        val m = 14.dp.toPx()
        val l = 30.dp.toPx()
        val marco = Rect(anillo.left - m, anillo.top - m, anillo.right + m, anillo.bottom + m)
        val colorEsquina = colorAnillo.copy(alpha = if (verificado) 1f else pulso)
        val sw = 3.dp.toPx()
        listOf(
            Triple(marco.topLeft, 1f, 1f), Triple(marco.topRight, -1f, 1f),
            Triple(marco.bottomLeft, 1f, -1f), Triple(marco.bottomRight, -1f, -1f)
        ).forEach { (p, dx, dy) ->
            val esquina = Path().apply {
                moveTo(p.x, p.y + dy * l)
                lineTo(p.x, p.y + dy * 8.dp.toPx())
                quadraticTo(p.x, p.y, p.x + dx * 8.dp.toPx(), p.y)
                lineTo(p.x + dx * l, p.y)
            }
            drawPath(esquina, colorEsquina, style = Stroke(sw, cap = StrokeCap.Round))
        }
    }
}

/** Tres pasos de la prueba de vida con su estado. */
@Composable
private fun IndicadorPasos(actual: Int) {
    val pasos = listOf<Pair<String, ImageVector>>(
        "Rostro" to Icons.Default.Face,
        "Parpadeo" to Icons.Default.RemoveRedEye,
        "Giro" to Icons.Default.ScreenRotation
    )
    Row(verticalAlignment = Alignment.Top) {
        pasos.forEachIndexed { i, (nombre, icono) ->
            val hecho = i < actual
            val activo = i == actual
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(78.dp)) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(
                            when {
                                hecho -> Esmeralda
                                activo -> Color.White.copy(alpha = 0.16f)
                                else -> Color.White.copy(alpha = 0.06f)
                            }
                        )
                        .border(2.dp, if (hecho || activo) Menta else Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (hecho) Icons.Default.Check else icono, null,
                        tint = if (hecho || activo) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    nombre, style = MaterialTheme.typography.labelMedium,
                    color = if (hecho || activo) Color.White else Color.White.copy(alpha = 0.45f),
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (i < pasos.lastIndex) {
                Box(
                    Modifier.padding(top = 21.dp).width(22.dp).height(2.dp)
                        .background(if (i < actual) Menta else Color.White.copy(alpha = 0.2f))
                )
            }
        }
    }
}

@Composable
private fun SinPermiso(modifier: Modifier, alSolicitar: () -> Unit) {
    Column(
        modifier.padding(32.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.08f)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Esmeralda), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Text("Activa la cámara", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "La usamos solo para comprobar que eres tú. La imagen se analiza en tu celular y se descarta al instante.",
            color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center
        )
        Button(onClick = alSolicitar, colors = ButtonDefaults.buttonColors(containerColor = Esmeralda)) { Text("Conceder permiso") }
    }
}
