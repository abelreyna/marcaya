package com.juanreyna.registrotiempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juanreyna.registrotiempo.domain.model.EstadoIncidencia
import com.juanreyna.registrotiempo.domain.model.Incidencia
import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.ui.theme.Ambar
import com.juanreyna.registrotiempo.ui.theme.AmbarTinte
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.RojoTinte
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.ui.theme.VerdeEsmeralda
import com.juanreyna.registrotiempo.util.Fechas

fun iconoIncidencia(t: TipoIncidencia): ImageVector = when (t) {
    TipoIncidencia.FALTA -> Icons.Default.EventBusy
    TipoIncidencia.TARDANZA -> Icons.Default.Schedule
    TipoIncidencia.SALIDA_ANTICIPADA -> Icons.Default.DirectionsRun
    TipoIncidencia.EXCESO_DESCANSO -> Icons.Default.FreeBreakfast
    TipoIncidencia.SIN_SALIDA -> Icons.Default.Logout
    TipoIncidencia.FUERA_GEOCERCA -> Icons.Default.LocationOff
}

fun colorEstadoIncidencia(e: EstadoIncidencia): Color = when (e) {
    EstadoIncidencia.POR_JUSTIFICAR -> Rojo
    EstadoIncidencia.EN_REVISION -> Ambar
    EstadoIncidencia.JUSTIFICADA -> VerdeEsmeralda
    EstadoIncidencia.NO_AUTORIZADA -> Rojo
}

fun etiquetaEstado(i: Incidencia): String =
    if (i.estado == EstadoIncidencia.NO_AUTORIZADA && i.tipo == TipoIncidencia.FALTA) "Falta no autorizada" else i.estado.etiqueta

/** Tarjeta de una incidencia: icono, título, fecha, detalle y estado; con nombre del colaborador si [mostrarNombre]. */
@Composable
fun TarjetaIncidencia(i: Incidencia, mostrarNombre: Boolean = false, accion: (@Composable () -> Unit)? = null) {
    val color = colorEstadoIncidencia(i.estado)
    val tinte = when (i.estado) {
        EstadoIncidencia.POR_JUSTIFICAR, EstadoIncidencia.NO_AUTORIZADA -> RojoTinte
        EstadoIncidencia.EN_REVISION -> AmbarTinte
        EstadoIncidencia.JUSTIFICADA -> Color.White
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(tinte)
            .border(1.dp, if (i.estado == EstadoIncidencia.JUSTIFICADA) Borde else color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(iconoIncidencia(i.tipo), null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (mostrarNombre) Text(i.colaboradorNombre, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                Text(i.titulo, fontWeight = FontWeight.Bold, color = AzulMarino)
                val (ds, d, m) = Fechas.partesFecha(i.fecha)
                Text("$ds $d $m".trim(), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
            ChipSimple(etiquetaEstado(i), color)
        }
        Text(i.detalle, style = MaterialTheme.typography.bodyMedium)
        i.justificacion?.let { j ->
            Text("Motivo: ${j.motivo.etiqueta}" + if (j.detalle.isNotBlank()) " — ${j.detalle}" else "", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
        accion?.invoke()
    }
}

/** Aviso destacado: "Tienes N incidencias por justificar". */
@Composable
fun AvisoIncidencias(cantidad: Int, alTocar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(RojoTinte)
            .border(1.dp, Rojo.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).clickable(onClick = alTocar).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.NotificationsActive, null, tint = Rojo)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (cantidad == 1) "Tienes 1 incidencia por justificar" else "Tienes $cantidad incidencias por justificar", fontWeight = FontWeight.Bold, color = Rojo)
            Text("Toca para enviar tu justificación al empleador", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Rojo)
    }
}
