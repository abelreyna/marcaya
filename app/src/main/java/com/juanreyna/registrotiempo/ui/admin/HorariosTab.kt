package com.juanreyna.registrotiempo.ui.admin

import com.juanreyna.registrotiempo.domain.model.Turno
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
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
internal fun Horario.resumen() =
    if (porDia) "Por día: ${porDiaTexto()} · tol. $toleranciaMin min"
    else "$entrada–$salida · desc. $inicioDescanso–$finDescanso · tol. $toleranciaMin min · ${diasTexto()}"

private val NOMBRES_DIA = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/** Horas de cada día laborable: [("Lun", "08:00–13:00"), ("Mar", "14:00–19:00 · desc. 16:00–16:30")…]. */
internal fun Horario.horasPorDia(): List<Pair<String, String>> = diasLaborables.sorted().map { d ->
    val h = delDia(d)
    NOMBRES_DIA[d - 1].take(3) to ("${h.entrada}–${h.salida}" + if (h.minutosDescansoProgramado > 0) " · desc. ${h.inicioDescanso}–${h.finDescanso}" else " · sin descanso")
}

/** "Lun 08:00–13:00, Mar 14:00–19:00…". */
internal fun Horario.porDiaTexto(): String = diasLaborables.sorted().joinToString(", ") { d -> delDia(d).let { "${NOMBRES_DIA[d - 1].take(3)} ${it.entrada}–${it.salida}" } }

/** Horas de entrada y salida para mostrar en listas. */
internal fun Horario.horasTexto(): String = if (porDia) "Varía por día" else "$entrada–$salida"

/**
 * Interruptor "Horario distinto según el día" y, si está activo, las horas de cada día laborable
 * (por ejemplo lunes en la mañana, martes en la tarde, domingo en la mañana).
 */
@Composable
internal fun EditorTurnos(horario: Horario, alCambiar: (Horario) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Horario distinto según el día", fontWeight = FontWeight.SemiBold)
            Text(
                if (horario.porDia) "Cada día laborable tiene sus propias horas." else "Todos los días laborables usan las mismas horas.",
                style = MaterialTheme.typography.bodySmall, color = TextoSecundario
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            horario.porDia,
            { on ->
                alCambiar(
                    if (on) horario.copy(turnos = horario.diasLaborables.associateWith { horario.turnos[it] ?: horario.turnoGeneral })
                    else horario.copy(turnos = emptyMap())
                )
            },
            colors = SwitchDefaults.colors(checkedTrackColor = Teal)
        )
    }
    if (!horario.porDia) return
    horario.diasLaborables.sorted().forEach { dia ->
        val t = horario.turnos[dia] ?: horario.turnoGeneral
        val poner = { nuevo: Turno -> alCambiar(horario.copy(turnos = horario.turnos + (dia to nuevo))) }
        val jornada = horario.copy(turnos = mapOf(dia to t)).delDia(dia).minutosJornadaProgramada
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, if (jornada > 0) Borde else Rojo, RoundedCornerShape(14.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(NOMBRES_DIA[dia - 1], Modifier.weight(1f), fontWeight = FontWeight.Bold, color = AzulMarino)
                Text(
                    if (jornada > 0) "Jornada ${Fechas.duracion(jornada.toLong())}" else "Revisa las horas",
                    style = MaterialTheme.typography.bodySmall, color = if (jornada > 0) TextoSecundario else Rojo
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoHora("Entrada", t.entrada, Modifier.weight(1f)) { poner(t.copy(entrada = it)) }
                CampoHora("Salida", t.salida, Modifier.weight(1f)) {
                    // Sin descanso, el descanso sigue a la salida para seguir en cero
                    poner(if (t.tieneDescanso) t.copy(salida = it) else t.copy(salida = it, inicioDescanso = it, finDescanso = it))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Descanso", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    t.tieneDescanso,
                    { on ->
                        poner(
                            if (on) t.copy(
                                inicioDescanso = horario.inicioDescanso.takeIf { it != horario.finDescanso } ?: "13:00",
                                finDescanso = horario.finDescanso.takeIf { it != horario.inicioDescanso } ?: "14:00"
                            ) else t.copy(inicioDescanso = t.salida, finDescanso = t.salida)
                        )
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Teal)
                )
            }
            if (t.tieneDescanso) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoHora("Inicio descanso", t.inicioDescanso, Modifier.weight(1f)) { poner(t.copy(inicioDescanso = it)) }
                CampoHora("Fin descanso", t.finDescanso, Modifier.weight(1f)) { poner(t.copy(finDescanso = it)) }
            }
        }
    }
}

/** true si la jornada de cada día laborable dura más de cero. */
internal fun Horario.jornadasValidas(): Boolean = diasLaborables.all { delDia(it).minutosJornadaProgramada > 0 }

/** Total de horas programadas a la semana. */
internal fun Horario.minutosSemana(): Long = diasLaborables.sumOf { delDia(it).minutosJornadaProgramada.toLong() }

/** "Lun–Sáb", "Lun–Vie" o lista de días. */
internal fun Horario.diasTexto(): String {
    val n = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val d = diasLaborables.sorted()
    return if (d.size > 2 && d.zipWithNext().all { (a, b) -> b == a + 1 }) "${n[d.first() - 1]}–${n[d.last() - 1]}" else d.joinToString(", ") { n[it - 1] }
}

/** Días laborables L–D: el día marcado permite marcar ingreso; debe quedar al menos uno. */
@Composable
internal fun SelectorDias(dias: List<Int>, alCambiar: (List<Int>) -> Unit) {
    Text("Días laborables (permiten marcar ingreso)", style = MaterialTheme.typography.labelLarge, color = TextoSecundario)
    // Cada día ocupa 1/7 del ancho: el domingo (D) siempre queda visible, aun en pantallas angostas
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("L", "M", "M", "J", "V", "S", "D").forEachIndexed { idx, letra ->
            val dia = idx + 1
            val activo = dia in dias
            Box(
                Modifier.weight(1f).aspectRatio(1f).widthIn(max = 44.dp).clip(CircleShape).background(if (activo) Teal else Color.White)
                    .border(1.dp, if (activo) Teal else Borde, CircleShape)
                    .clickable {
                        val nuevos = if (activo) dias - dia else (dias + dia).sorted()
                        if (nuevos.isNotEmpty()) alCambiar(nuevos)
                    },
                contentAlignment = Alignment.Center
            ) { Text(letra, color = if (activo) Color.White else AzulMarino, fontWeight = FontWeight.SemiBold) }
        }
    }
}

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
    val valido = horario.jornadasValidas() && toleranciaMin != null && toleranciaMin in 0..60
    val grupos = cols.groupBy { it.horario }.toList().sortedByDescending { it.second.size }

    fun cargar(h: Horario) { horario = h; tolerancia = h.toleranciaMin.toString() }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(6.dp)) }
        if (cols.isNotEmpty()) item {
            // Interruptor rápido: domingo laborable para todo el equipo
            val conDomingo = cols.count { 7 in it.horario.diasLaborables }
            Seccion("Marcaje en domingo", borde = Borde) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (conDomingo) {
                                0 -> "Deshabilitado: nadie puede marcar ingreso los domingos."
                                cols.size -> "Habilitado para todo el equipo."
                                else -> "Habilitado para $conDomingo de ${cols.size} colaboradores."
                            },
                            style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(conDomingo == cols.size, { vm.habilitarDomingo(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Teal))
                }
                Text("Para elegir otros días o solo algunos colaboradores, usa los días laborables de abajo.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
        }
        item {
            Seccion("1. Define el horario", borde = Borde) {
                SelectorDias(horario.diasLaborables) { horario = horario.copy(diasLaborables = it) }
                EditorTurnos(horario) { horario = it }
                if (!horario.porDia) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoHora("Entrada", horario.entrada, Modifier.weight(1f)) { horario = horario.copy(entrada = it) }
                        CampoHora("Salida", horario.salida, Modifier.weight(1f)) { horario = horario.copy(salida = it) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoHora("Inicio descanso", horario.inicioDescanso, Modifier.weight(1f)) { horario = horario.copy(inicioDescanso = it) }
                        CampoHora("Fin descanso", horario.finDescanso, Modifier.weight(1f)) { horario = horario.copy(finDescanso = it) }
                    }
                }
                OutlinedTextField(
                    tolerancia, { tolerancia = it.filter(Char::isDigit).take(2) }, Modifier.fillMaxWidth(),
                    label = { Text("Tolerancia (minutos)") }, shape = FormaCampo, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valido && (toleranciaMin == null || toleranciaMin > 60)
                )
                Text(
                    when {
                        !horario.jornadasValidas() -> "Revisa el horario: la jornada no puede quedar en cero."
                        horario.porDia -> "Total semanal: ${Fechas.duracion(horario.minutosSemana())}"
                        else -> "Jornada: ${Fechas.duracion(horario.minutosJornadaProgramada.toLong())} · Descanso: ${Fechas.duracion(horario.minutosDescansoProgramado.toLong())}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (horario.jornadasValidas()) TextoSecundario else Rojo
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
                        Text(h.horasTexto(), fontWeight = FontWeight.Bold, color = AzulMarino)
                        Text(
                            if (h.porDia) "${h.porDiaTexto()} · tol. ${h.toleranciaMin} min"
                            else "Descanso ${h.inicioDescanso}–${h.finDescanso} · tol. ${h.toleranciaMin} min · ${h.diasTexto()}",
                            style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                        )
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
                    if (u.horario.porDia) {
                        Text("Horario por día: ${u.horario.porDiaTexto()}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    } else {
                        Text("Horario: ${u.horario.entrada}–${u.horario.salida} · ${u.horario.diasTexto()}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                        Text("Descanso: ${u.horario.inicioDescanso}–${u.horario.finDescanso}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
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
