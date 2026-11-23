package com.juanreyna.registrotiempo.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juanreyna.registrotiempo.domain.calc.CostoColaborador
import com.juanreyna.registrotiempo.domain.calc.CostoDia
import com.juanreyna.registrotiempo.domain.calc.ReporteCostos
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.ui.components.Avatar
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.BotonPillContorno
import com.juanreyna.registrotiempo.ui.components.FilaDato
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.TextoCentrado
import com.juanreyna.registrotiempo.ui.components.dinero
import com.juanreyna.registrotiempo.ui.theme.Ambar
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.FondoClaro
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TealClaro
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.util.Fechas

/**
 * Reporte de costos (salarios) del mes: cuánto se pagó, por día y por colaborador,
 * con horas, fechas, ingreso/salida y exportación a PDF / Excel.
 */
@Composable
internal fun ReporteCostosSeccion(resumen: List<ResumenColaborador>, mon: String, mesEtiqueta: String, alExportar: (String) -> Unit) {
    val costos = remember(resumen) { ReporteCostos.construir(resumen) }
    var filtro by rememberSaveable { mutableStateOf<String?>(null) } // uid o null = todos
    var porDia by rememberSaveable { mutableStateOf(true) }
    val visibles = if (filtro == null) costos else costos.filter { it.colaborador.uid == filtro }

    Seccion(borde = Borde) {
        Text("Reporte de Costos (Salarios)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AzulMarino)
        Text(mesEtiqueta, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)

        // Totales
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeldaMonto("Pagado (días)", dinero(mon, visibles.sumOf { it.pagadoPorDias }), Modifier.weight(1f), Teal, negrita = true)
            CeldaMonto("Horas trabajadas", Fechas.duracion(visibles.sumOf { it.minutosTrabajados }), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeldaMonto("Horas extras", Fechas.duracion(visibles.sumOf { it.minutosExtra }), Modifier.weight(1f), Ambar)
            CeldaMonto("Días pagados", "${visibles.sumOf { it.dias.size }}", Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TealClaro).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilaDato("Neto a pagar (boletas)", dinero(mon, visibles.sumOf { it.boleta.netoAPagar }), destacado = true)
            FilaDato("Costo laboral del empleador", dinero(mon, visibles.sumOf { it.boleta.costoEmpleador }), destacado = true, colorValor = Teal)
        }

        // Filtro por colaborador
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Todos", filtro == null) { filtro = null }
            costos.forEach { c -> Chip(c.colaborador.nombre.substringBefore(" "), filtro == c.colaborador.uid) { filtro = c.colaborador.uid } }
        }
        // Vista
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonPillContorno("Por día", { porDia = true }, Modifier.weight(1f), color = if (porDia) Teal else TextoSecundario)
            BotonPillContorno("Por persona", { porDia = false }, Modifier.weight(1f), color = if (!porDia) Teal else TextoSecundario)
        }

        if (visibles.all { it.dias.isEmpty() }) TextoCentrado("Sin días trabajados en el periodo.")
        else if (porDia) VistaPorDia(visibles, mon) else VistaPorColaborador(visibles, mon)

        Spacer(Modifier.padding(top = 4.dp))
        BotonPill("Exportar costos en PDF", { alExportar("COSTOS_PDF") }, icono = Icons.Default.PictureAsPdf, alto = 52.dp)
        BotonPill("Exportar costos en Excel (CSV)", { alExportar("COSTOS_CSV") }, icono = Icons.Default.TableChart, alto = 52.dp, color = AzulMarino)
        Text(
            "Pago del día = horas ordinarias × valor hora + horas extras (Simple: sin recargo; Avanzado Perú: +25 % / +35 %). El neto y el costo del empleador incluyen asignación familiar, descuentos y aportes de la boleta.",
            style = MaterialTheme.typography.bodySmall, color = TextoSecundario
        )
    }
}

@Composable
private fun Chip(texto: String, sel: Boolean, alTocar: () -> Unit) {
    FilterChip(
        selected = sel, onClick = alTocar, label = { Text(texto, maxLines = 1) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Teal, selectedLabelColor = Color.White)
    )
}

@Composable
private fun VistaPorDia(costos: List<CostoColaborador>, mon: String) {
    ReporteCostos.porDia(costos).forEach { (fecha, dias) ->
        val (dSem, dia, mes) = Fechas.partesFecha(fecha)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FondoClaro).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$dSem $dia $mes".trim(), Modifier.weight(1f), fontWeight = FontWeight.Bold, color = AzulMarino)
                Text(dinero(mon, dias.sumOf { it.total }), fontWeight = FontWeight.Bold, color = Teal)
            }
            dias.forEachIndexed { i, d ->
                if (i > 0) HorizontalDivider(color = Borde)
                FilaCostoDia(d, mon, mostrarNombre = true)
            }
        }
    }
}

@Composable
private fun VistaPorColaborador(costos: List<CostoColaborador>, mon: String) {
    costos.forEach { c ->
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FondoClaro).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(c.colaborador.nombre, 32.dp, foto = c.colaborador.foto)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.colaborador.nombre, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${c.dias.size} ${if (c.dias.size == 1) "día" else "días"} · ${Fechas.duracion(c.minutosTrabajados)} · valor hora ${dinero(mon, ReporteCostos.valorHora(c.colaborador.configPago))}",
                        style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CeldaMonto("Pagado", dinero(mon, c.pagadoPorDias), Modifier.weight(1f), Teal, negrita = true)
                CeldaMonto("Neto mes", dinero(mon, c.boleta.netoAPagar), Modifier.weight(1f), AzulMarino)
                CeldaMonto("Costo empl.", dinero(mon, c.boleta.costoEmpleador), Modifier.weight(1f))
            }
            c.dias.sortedByDescending { it.fecha }.forEach { d ->
                HorizontalDivider(color = Borde)
                FilaCostoDia(d, mon, mostrarNombre = false)
            }
        }
    }
}

@Composable
private fun FilaCostoDia(d: CostoDia, mon: String, mostrarNombre: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (mostrarNombre) d.colaborador.nombre else d.fecha.let { val (s, di, m) = Fechas.partesFecha(it); "$s $di $m".trim() },
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val extra = if (d.minutosExtra > 0) " · extra ${Fechas.duracion(d.minutosExtra)}" else ""
            Text(
                "${Fechas.hora(d.ingreso).take(5)} – ${Fechas.hora(d.salida).take(5)} · ${Fechas.duracion(d.minutosTrabajados)}$extra",
                style = MaterialTheme.typography.bodySmall, color = TextoSecundario
            )
        }
        Text(dinero(mon, d.total), fontWeight = FontWeight.SemiBold, softWrap = false)
    }
}
