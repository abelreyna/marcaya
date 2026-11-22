package com.juanreyna.registrotiempo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.juanreyna.registrotiempo.util.FotoPerfil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juanreyna.registrotiempo.R
import com.juanreyna.registrotiempo.domain.model.EstadoColaborador
import com.juanreyna.registrotiempo.ui.theme.Ambar
import com.juanreyna.registrotiempo.ui.theme.AmbarTinte
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.AzulMarinoClaro
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.FondoClaro
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.RojoTinte
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.ui.theme.VerdeEsmeralda
import com.juanreyna.registrotiempo.ui.theme.VerdeTinte

val DegradadoMarino = Brush.verticalGradient(listOf(AzulMarino, AzulMarinoClaro))

@Composable
fun Isotipo(tamano: Dp = 96.dp) {
    Image(painterResource(R.drawable.ic_isotipo), contentDescription = "Isotipo", modifier = Modifier.size(tamano))
}

/**
 * Marco común de las capturas: cabecera azul marino y una hoja clara con esquinas superiores redondeadas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarcoPantalla(
    titulo: String,
    subtitulo: String? = null,
    alVolver: (() -> Unit)? = null,
    avatar: String? = null,
    avatarFoto: String = "",
    alTocarAvatar: (() -> Unit)? = null,
    centrado: Boolean = false,
    acciones: @Composable RowScope.() -> Unit = {},
    pie: @Composable (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    // Con edge-to-edge Android no redimensiona la ventana al abrir el teclado: imePadding sube todo el
    // contenido por encima del teclado, y la barra inferior se oculta para dejar más espacio al formulario.
    val tecladoVisible = WindowInsets.isImeVisible
    Column(Modifier.fillMaxSize().background(DegradadoMarino).imePadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (alVolver != null) IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
            else Spacer(Modifier.width(8.dp))
            if (avatar != null) { Avatar(avatar, 44.dp, foto = avatarFoto, alTocar = alTocarAvatar); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f), horizontalAlignment = if (centrado) Alignment.CenterHorizontally else Alignment.Start) {
                Text(titulo, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (subtitulo != null) Text(subtitulo, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = acciones)
        }
        Column(
            Modifier.fillMaxWidth().weight(1f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(FondoClaro),
            content = contenido
        )
        if (!tecladoVisible) pie?.invoke()
    }
}

/** Foto de perfil circular; si no hay foto muestra las iniciales del nombre. */
@Composable
fun Avatar(
    nombre: String,
    tamano: Dp = 44.dp,
    fondo: Color = Color(0xFFDCE4F2),
    foto: String = "",
    alTocar: (() -> Unit)? = null
) {
    val iniciales = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    val imagen = remember(foto) { FotoPerfil.aBitmap(foto)?.asImageBitmap() }
    Box(
        Modifier.size(tamano).clip(CircleShape).background(fondo).border(2.dp, Color.White, CircleShape)
            .then(if (alTocar != null) Modifier.clickable(onClick = alTocar) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (imagen != null) Image(imagen, "Foto de perfil de $nombre", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(iniciales, color = AzulMarino, fontWeight = FontWeight.Bold, fontSize = (tamano.value * 0.36f).sp)
    }
}

@Composable
fun Seccion(
    titulo: String? = null,
    modifier: Modifier = Modifier,
    fondo: Color = Color.White,
    borde: Color? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = borde?.let { BorderStroke(1.dp, it) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AzulMarino)
            contenido()
        }
    }
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(texto, modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1F2B))
}

@Composable
fun FilaDato(etiqueta: String, valor: String, destacado: Boolean = false, colorValor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            valor,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Medium,
            color = colorValor
        )
    }
}

/** Botón píldora principal (teal) como en las capturas. */
@Composable
fun BotonPill(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Teal,
    enabled: Boolean = true,
    icono: ImageVector? = null,
    alto: Dp = 52.dp
) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(alto),
        shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        if (icono != null) { Icon(icono, null); Spacer(Modifier.width(8.dp)) }
        Text(texto, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun BotonPillContorno(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AzulMarino,
    enabled: Boolean = true,
    icono: ImageVector? = null,
    extra: String? = null
) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(50), border = BorderStroke(1.5.dp, if (enabled) color else Borde),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        if (icono != null) { Icon(icono, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(texto, fontWeight = FontWeight.SemiBold, modifier = if (extra != null) Modifier.weight(1f) else Modifier)
        if (extra != null) Text(extra, fontWeight = FontWeight.SemiBold)
    }
}

fun colorEstado(e: EstadoColaborador): Color = when (e) {
    EstadoColaborador.DENTRO_AREA -> VerdeEsmeralda
    EstadoColaborador.EN_DESCANSO -> Ambar
    EstadoColaborador.FUERA_RANGO -> Rojo
    EstadoColaborador.TARDANZA -> Color(0xFFEF6C00)
    EstadoColaborador.JORNADA_CERRADA -> AzulMarino
    EstadoColaborador.SIN_MARCAR -> Color.Gray
}

fun tinteEstado(e: EstadoColaborador): Color = when (e) {
    EstadoColaborador.DENTRO_AREA -> VerdeTinte
    EstadoColaborador.FUERA_RANGO -> RojoTinte
    EstadoColaborador.EN_DESCANSO, EstadoColaborador.TARDANZA -> AmbarTinte
    else -> Color.White
}

/** Etiqueta de estado tipo píldora (“Dentro del área”, “Fuera de rango”). */
@Composable
fun ChipEstado(estado: EstadoColaborador, sufijo: String? = null) {
    val c = colorEstado(estado)
    val relleno = estado == EstadoColaborador.DENTRO_AREA
    Text(
        estado.etiqueta + (sufijo?.let { " ($it)" } ?: ""),
        Modifier.clip(RoundedCornerShape(50))
            .background(if (relleno) c else c.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = if (relleno) Color.White else c,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun ChipSimple(texto: String, color: Color) {
    Text(
        texto,
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp),
        color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun Semaforo(color: Color, texto: String, detalle: String? = null, icono: ImageVector? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.1f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) Icon(icono, null, tint = color, modifier = Modifier.size(28.dp))
        else Box(Modifier.size(16.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(texto, fontWeight = FontWeight.SemiBold, color = color)
            if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
    }
}

/** Tarjeta de alerta del Centro de Notificaciones. */
@Composable
fun TarjetaAviso(icono: ImageVector, titulo: String, detalle: String, color: Color = AzulMarino, tinte: Color = Color.White) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(tinte)
            .border(1.dp, if (tinte == Color.White) Borde else color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, fontWeight = FontWeight.Bold, color = if (tinte == Color.White) Color(0xFF1A1F2B) else color)
            Text(detalle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Barra inferior azul marino con dos indicadores (Costo Estimado Hoy | Horas Totales). */
@Composable
fun BarraResumen(izqTitulo: String, izqValor: String, derTitulo: String, derValor: String) {
    Row(
        Modifier.fillMaxWidth().background(DegradadoMarino).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(izqTitulo, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            Text(izqValor, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        VerticalDivider(Modifier.height(40.dp), color = Color.White.copy(alpha = 0.3f))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(derTitulo, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            Text(derValor, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Tarjeta blanca de dos métricas (Horas de Hoy | Pago del Día). */
@Composable
fun TarjetaDosMetricas(izqTitulo: String, izqValor: String, derTitulo: String, derValor: String) {
    Seccion(borde = Borde) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(izqTitulo, style = MaterialTheme.typography.bodyMedium)
                Text(izqValor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AzulMarino)
            }
            VerticalDivider(Modifier.height(44.dp).fillMaxHeight(), color = Borde)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(derTitulo, style = MaterialTheme.typography.bodyMedium)
                Text(derValor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AzulMarino)
            }
        }
    }
}

/** Insignia de fecha (Lun / 17 / Nov) del historial. */
@Composable
fun InsigniaFecha(diaSemana: String, dia: String, mes: String) {
    Column(
        Modifier.width(52.dp).clip(RoundedCornerShape(12.dp)).background(AzulMarino).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(diaSemana, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
        Text(dia, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(mes, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun BannerVip(mensaje: String, alVer: () -> Unit, alCerrar: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().background(AmbarTinte).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WorkspacePremium, null, tint = Ambar)
        Text(mensaje, Modifier.weight(1f).padding(8.dp), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = alVer) { Text("Ver VIP") }
        if (alCerrar != null) IconButton(onClick = alCerrar) { Icon(Icons.Default.Close, "Cerrar") }
    }
}

@Composable
fun Cargando() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teal) }
}

@Composable
fun TextoCentrado(texto: String) {
    Text(texto, Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = TextoSecundario)
}

@Composable
fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White,
    unfocusedBorderColor = Borde,
    focusedBorderColor = AzulMarino
)

val FormaCampo = RoundedCornerShape(14.dp)

fun dinero(simbolo: String, valor: Double) = "$simbolo %,.2f".format(valor)
