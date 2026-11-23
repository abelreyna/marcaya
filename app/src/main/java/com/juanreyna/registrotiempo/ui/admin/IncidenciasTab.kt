package com.juanreyna.registrotiempo.ui.admin

import com.juanreyna.registrotiempo.domain.model.Incidencia
import androidx.compose.runtime.remember
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanreyna.registrotiempo.domain.calc.DetectorIncidencias
import com.juanreyna.registrotiempo.domain.model.EstadoIncidencia
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.Justificacion
import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.ui.components.Avatar
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.BotonPillContorno
import com.juanreyna.registrotiempo.ui.components.Cargando
import com.juanreyna.registrotiempo.ui.components.ChipSimple
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.TarjetaIncidencia
import com.juanreyna.registrotiempo.ui.components.TextoCentrado
import com.juanreyna.registrotiempo.ui.theme.Ambar
import com.juanreyna.registrotiempo.ui.theme.AmbarTinte
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.RojoTinte
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TealClaro
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.ui.theme.VerdeEsmeralda
import com.juanreyna.registrotiempo.util.Fechas

private enum class Filtro(val etiqueta: String) { REVISAR("Por revisar"), SIN_JUSTIFICAR("Sin justificar"), HISTORIAL("Historial") }

/**
 * Incidencias del equipo: justificaciones por revisar (aprobar / no autorizar), incidencias del mes
 * sin justificar (faltas, tardanzas, salidas anticipadas, excesos de descanso…) e historial resuelto.
 */
@Composable
internal fun PestanaIncidencias(vm: AdminViewModel, ui: UiAdmin) {
    val incidencias by vm.incidencias.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.cargarIncidencias() }
    var filtro by rememberSaveable { mutableStateOf(Filtro.REVISAR) }
    var salidaDe by remember { mutableStateOf<Incidencia?>(null) }
    salidaDe?.let { i ->
        val programada = ui.colaboradores.firstOrNull { it.uid == i.colaboradorId }?.horario?.salida ?: "17:00"
        var hora by remember(i.clave) { mutableStateOf(programada) }
        AlertDialog(
            onDismissRequest = { salidaDe = null },
            title = { Text("Salida del ${i.fecha}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${i.colaboradorNombre} no marcó su salida ese día. Indica la hora en que salió.", style = MaterialTheme.typography.bodyMedium)
                    CampoHora("Salida", hora, Modifier.fillMaxWidth()) { hora = it }
                }
            },
            confirmButton = { TextButton(onClick = { vm.marcarSalidaDia(i, hora); salidaDe = null }) { Text("Registrar salida", color = Teal, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { salidaDe = null }) { Text("Cancelar") } }
        )
    }
    val lista = incidencias
    val porRevisar = ui.justificaciones.filter { it.estado == EstadoJustificacion.PENDIENTE }
    val sinJustificar = lista.orEmpty().filter { it.estado == EstadoIncidencia.POR_JUSTIFICAR }
    val noAutorizadas = lista.orEmpty().count { it.estado == EstadoIncidencia.NO_AUTORIZADA }
    val faltas = lista.orEmpty().count { it.tipo == TipoIncidencia.FALTA && it.estado != EstadoIncidencia.JUSTIFICADA }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Incidencias de ${Fechas.etiquetaMes(Fechas.mesActual())}. Se detectan solas con el horario de cada colaborador.",
                Modifier.padding(top = 16.dp), color = TextoSecundario, style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Contador("Por revisar", porRevisar.size, AmbarTinte, Ambar, Modifier.weight(1f))
                Contador("Sin justificar", sinJustificar.size, RojoTinte, Rojo, Modifier.weight(1f))
                Contador("Faltas", faltas, RojoTinte, Rojo, Modifier.weight(1f))
            }
        }
        if (noAutorizadas > 0) item {
            Text("$noAutorizadas incidencia(s) no autorizada(s) este mes.", color = Rojo, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Filtro.entries.forEach { f ->
                    val n = when (f) { Filtro.REVISAR -> porRevisar.size; Filtro.SIN_JUSTIFICAR -> sinJustificar.size; Filtro.HISTORIAL -> 0 }
                    FilterChip(
                        selected = filtro == f, onClick = { filtro = f },
                        label = { Text(if (n > 0) "${f.etiqueta} ($n)" else f.etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Teal, selectedLabelColor = Color.White)
                    )
                }
            }
        }

        when (filtro) {
            Filtro.REVISAR -> {
                if (porRevisar.isEmpty()) item { TextoCentrado("No hay justificaciones por revisar.") }
                items(porRevisar, key = { it.id }) { j -> TarjetaJustificacion(vm, ui, j, lista.orEmpty()) }
            }
            Filtro.SIN_JUSTIFICAR -> {
                if (lista == null) item { Cargando() }
                else if (sinJustificar.isEmpty()) item { TextoCentrado("Sin incidencias pendientes de justificar.") }
                else {
                    item {
                        Text(
                            "El colaborador recibe una alerta para justificar. Si no lo hace, la incidencia queda registrada en su historial.",
                            style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                        )
                    }
                    items(sinJustificar, key = { it.clave }) { i ->
                        TarjetaIncidencia(i, mostrarNombre = true, accion = if (i.tipo == TipoIncidencia.SIN_SALIDA) {
                            { BotonPillContorno("Marcar hora de salida", { salidaDe = i }, Modifier.fillMaxWidth(), color = Rojo) }
                        } else null)
                    }
                }
            }
            Filtro.HISTORIAL -> {
                val resueltas = ui.justificaciones.filter { it.estado != EstadoJustificacion.PENDIENTE }
                if (resueltas.isEmpty()) item { TextoCentrado("Aún no hay justificaciones resueltas.") }
                items(resueltas, key = { it.id }) { j -> TarjetaJustificacion(vm, ui, j, lista.orEmpty()) }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TarjetaJustificacion(vm: AdminViewModel, ui: UiAdmin, j: Justificacion, incidencias: List<com.juanreyna.registrotiempo.domain.model.Incidencia>) {
    val foto = ui.colaboradores.firstOrNull { it.uid == j.colaboradorId }?.foto.orEmpty()
    // Incidencia que justifica (para mostrar qué ocurrió ese día)
    val inc = incidencias.firstOrNull { it.justificacion?.id == j.id }
        ?: j.incidencia?.let { t -> incidencias.firstOrNull { it.colaboradorId == j.colaboradorId && it.fecha == j.fecha && it.tipo == t } }
    val titulo = inc?.titulo ?: j.incidencia?.etiqueta ?: "Permiso / justificación"
    Seccion(borde = Borde) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(j.colaboradorNombre, 40.dp, foto = foto)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(j.colaboradorNombre, fontWeight = FontWeight.SemiBold)
                val (ds, d, m) = Fechas.partesFecha(j.fecha)
                Text("$ds $d $m".trim(), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
            when (j.estado) {
                EstadoJustificacion.PENDIENTE -> ChipSimple("Por revisar", Ambar)
                EstadoJustificacion.APROBADA -> ChipSimple("Aprobada", VerdeEsmeralda)
                EstadoJustificacion.RECHAZADA -> ChipSimple(if (j.incidencia == TipoIncidencia.FALTA) "Falta no autorizada" else "No autorizada", Rojo)
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TealClaro).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(titulo, fontWeight = FontWeight.Bold, color = AzulMarino)
            inc?.let { Text(it.detalle, style = MaterialTheme.typography.bodySmall) }
        }
        Text("Motivo: ${j.motivo.etiqueta}", fontWeight = FontWeight.SemiBold)
        if (j.detalle.isNotBlank()) Text(j.detalle, style = MaterialTheme.typography.bodyMedium)
        if (j.estado == EstadoJustificacion.PENDIENTE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPill("Aprobar", { vm.resolver(j, EstadoJustificacion.APROBADA) }, Modifier.weight(1f), alto = 44.dp)
                BotonPillContorno("No autorizar", { vm.resolver(j, EstadoJustificacion.RECHAZADA) }, Modifier.weight(1f), color = Rojo)
            }
        }
    }
}
