package com.juanreyna.registrotiempo.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.ui.components.Avatar
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.TextoCentrado
import com.juanreyna.registrotiempo.ui.components.TituloSeccion
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TealClaro
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.util.Fechas

/** Texto corto de un horario: "08:00–17:00 · desc. 13:00–14:00 · tol. 5 min". */
internal fun Horario.resumen() = "$entrada–$salida · desc. $inicioDescanso–$finDescanso · tol. $toleranciaMin min"

/**
 * Pestaña "Horarios": el administrador define un horario y elige qué colaboradores lo usan
 * (uno, un grupo o todo el equipo). Muestra los horarios vigentes agrupados para seleccionar grupos.
 */
@Composable
internal fun PestanaHorarios(vm: AdminViewModel, ui: UiAdmin) {
    val cols = ui.colaboradores
    // Parte del horario más usado del equipo (o el horario por defecto)
    val inicial = remember(cols.isEmpty()) {
        cols.groupingBy { it.horario }.eachCount().maxByOrNull { it.value }?.key ?: Horario()
    }
    var horario by remember(inicial) { mutableStateOf(inicial) }
    var tolerancia by remember(inicial) { mutableStateOf(inicial.toleranciaMin.toString()) }
    var seleccion by remember { mutableStateOf(setOf<String>()) }
    var confirmar by remember { mutableStateOf(false) }

    val toleranciaMin = tolerancia.toIntOrNull()
    val horarioFinal = horario.copy(toleranciaMin = toleranciaMin ?: 0)
    val valido = horario.minutosJornadaProgramada > 0 && toleranciaMin != null && toleranciaMin in 0..60
    val grupos = cols.groupBy { it.horario }.toList().sortedByDescending { it.second.size }

    fun cargar(h: Horario) { horario = h; tolerancia = h.toleranciaMin.toString() }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Seccion("1. Define el horario", borde = Borde) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoHora("Entrada", horario.entrada, Modifier.weight(1f)) { horario = horario.copy(entrada = it) }
                    CampoHora("Salida", horario.salida, Modifier.weight(1f)) { horario = horario.copy(salida = it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoHora("Inicio descanso", horario.inicioDescanso, Modifier.weight(1f)) { horario = horario.copy(inicioDescanso = it) }
                    CampoHora("Fin descanso", horario.finDescanso, Modifier.weight(1f)) { horario = horario.copy(finDescanso = it) }
                }
                OutlinedTextField(
                    tolerancia, { tolerancia = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(),
                    label = { Text("Tolerancia (minutos)") }, shape = FormaCampo, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valido && (toleranciaMin == null || toleranciaMin > 60)
                )
                Text(
                    if (horario.minutosJornadaProgramada > 0)
                        "Jornada: ${Fechas.duracion(horario.minutosJornadaProgramada.toLong())} · Descanso: ${Fechas.duracion(horario.minutosDescansoProgramado.toLong())}"
                    else "Revisa el horario: la jornada no puede quedar en cero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (horario.minutosJornadaProgramada > 0) TextoSecundario else Rojo
                )
            }
        }

        if (grupos.size > 1) {
            item { TituloSeccion("Horarios actuales del equipo") }
            items(grupos, key = { it.first.resumen() }) { (h, miembros) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color.White)
                        .border(1.dp, Borde, RoundedCornerShape(12.dp))
                        .clickable { cargar(h); seleccion = miembros.map { it.uid }.toSet() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${h.entrada}–${h.salida}", fontWeight = FontWeight.Bold, color = AzulMarino)
                        Text("Descanso ${h.inicioDescanso}–${h.finDescanso} · tol. ${h.toleranciaMin} min", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                    Text("${miembros.size} ${if (miembros.size == 1) "persona" else "personas"}", style = MaterialTheme.typography.bodySmall, color = Teal, fontWeight = FontWeight.SemiBold)
                }
            }
            item { Text("Toca un grupo para seleccionarlo y editar su horario.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario) }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TituloSeccion("2. ¿A quiénes aplica?", Modifier.weight(1f))
                if (cols.isNotEmpty()) TextButton({
                    seleccion = if (seleccion.size == cols.size) emptySet() else cols.map { it.uid }.toSet()
                }) { Text(if (seleccion.size == cols.size) "Ninguno" else "Todos", color = Teal, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (cols.isEmpty()) item { TextoCentrado("Aún no hay colaboradores vinculados.") }
        items(cols, key = { it.uid }) { u ->
            val marcado = u.uid in seleccion
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (marcado) TealClaro else androidx.compose.ui.graphics.Color.White)
                    .border(1.dp, if (marcado) Teal else Borde, RoundedCornerShape(16.dp))
                    .clickable { seleccion = if (marcado) seleccion - u.uid else seleccion + u.uid }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(marcado, { seleccion = if (it) seleccion + u.uid else seleccion - u.uid }, colors = CheckboxDefaults.colors(checkedColor = Teal))
                Avatar(u.nombre, 40.dp, foto = u.foto)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Horario: ${u.horario.entrada}–${u.horario.salida}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    Text("Descanso: ${u.horario.inicioDescanso}–${u.horario.finDescanso}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                if (u.horario == horarioFinal) {
                    androidx.compose.material3.Icon(Icons.Default.Schedule, "Ya tiene este horario", tint = Teal, modifier = Modifier.padding(end = 4.dp))
                }
            }
        }
        item {
            BotonPill(
                when (seleccion.size) {
                    0 -> "Selecciona colaboradores"
                    1 -> "Aplicar horario a 1 colaborador"
                    else -> "Aplicar horario a ${seleccion.size} colaboradores"
                },
                { confirmar = true }, Modifier.padding(vertical = 8.dp),
                enabled = valido && seleccion.isNotEmpty()
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (confirmar) {
        val nombres = cols.filter { it.uid in seleccion }.map { it.nombre }
        AlertDialog(
            onDismissRequest = { confirmar = false },
            icon = { androidx.compose.material3.Icon(Icons.Default.Schedule, null, tint = Teal) },
            title = { Text("¿Aplicar este horario?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(horarioFinal.resumen(), fontWeight = FontWeight.SemiBold, color = AzulMarino)
                    Text("Se cambiará para: ${nombres.joinToString(", ")}.")
                    Text("Las tardanzas, horas extras y excesos de descanso se recalculan con el nuevo horario.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
            },
            confirmButton = {
                TextButton({
                    vm.aplicarHorario(seleccion.toList(), horarioFinal)
                    seleccion = emptySet(); confirmar = false
                }) { Text("Aplicar", color = Teal, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton({ confirmar = false }) { Text("Cancelar") } }
        )
    }
}
