package com.juanreyna.registrotiempo.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juanreyna.registrotiempo.domain.model.Descuento
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.ui.components.Avatar
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.coloresCampo
import com.juanreyna.registrotiempo.ui.components.dinero
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.RojoTinte
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TealClaro
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.util.Fechas

private val SUGERENCIAS = listOf("Adelanto de sueldo", "Faltante de caja", "Daño de material", "Préstamo", "Uniforme / EPP", "Consumo")

/** Pestaña Equipo: descuentos aplicados en el mes y botón para aplicar uno nuevo. */
@Composable
internal fun SeccionDescuentos(
    descuentos: List<Descuento>, colaboradores: List<Usuario>, mon: String,
    alAplicar: (Usuario, String, String, Double, String) -> Unit, alAnular: (Descuento) -> Unit
) {
    val mes = Fechas.mesActual()
    val delMes = descuentos.filter { it.mes == mes }
    var verTodos by rememberSaveable { mutableStateOf(false) }
    var nuevo by rememberSaveable { mutableStateOf(false) }
    var anulando by remember { mutableStateOf<Descuento?>(null) }

    Seccion("Descuentos al personal", borde = Borde) {
        Text(
            "Aplica un descuento a un trabajador (adelanto, faltante, daño, etc.). Se resta de su pago de ese día y le llega como notificación.",
            style = MaterialTheme.typography.bodySmall, color = TextoSecundario
        )
        if (delMes.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${Fechas.etiquetaMes(mes)} · ${delMes.size} descuento(s)", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("−${dinero(mon, delMes.sumOf { it.monto })}", color = Rojo, fontWeight = FontWeight.Bold)
            }
            (if (verTodos) delMes else delMes.take(4)).forEach { d ->
                HorizontalDivider(color = Borde)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.colaboradorNombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${d.concepto} · ${Fechas.fechaCorta(d.fecha)}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                        Text(d.motivo, style = MaterialTheme.typography.bodySmall, color = TextoSecundario, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text("−${dinero(mon, d.monto)}", color = Rojo, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { anulando = d }) { Icon(Icons.Default.DeleteOutline, "Anular descuento", tint = TextoSecundario) }
                }
            }
            if (delMes.size > 4) TextButton(onClick = { verTodos = !verTodos }) { Text(if (verTodos) "Ver menos" else "Ver los ${delMes.size}") }
        }
        BotonPill("Aplicar descuento", { nuevo = true }, color = Rojo, icono = Icons.Default.MoneyOff, alto = 46.dp, enabled = colaboradores.isNotEmpty())
    }

    if (nuevo) DialogoDescuento(colaboradores, mon, alCerrar = { nuevo = false }) { u, concepto, motivo, monto, fecha ->
        alAplicar(u, concepto, motivo, monto, fecha); nuevo = false
    }
    anulando?.let { d ->
        AlertDialog(
            onDismissRequest = { anulando = null },
            title = { Text("¿Anular este descuento?", color = Rojo, fontWeight = FontWeight.Bold) },
            text = {
                Text("${d.colaboradorNombre} · ${d.concepto} del ${Fechas.fechaCorta(d.fecha)} por ${dinero(mon, d.monto)}. Se quitará de su pago y de la boleta.")
            },
            confirmButton = { TextButton(onClick = { alAnular(d); anulando = null }) { Text("Anular", color = Rojo) } },
            dismissButton = { TextButton(onClick = { anulando = null }) { Text("Cancelar") } }
        )
    }
}

/** Formulario: buscar al trabajador, nombre y motivo del descuento, monto y día al que se aplica. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoDescuento(
    colaboradores: List<Usuario>, mon: String, alCerrar: () -> Unit,
    alConfirmar: (Usuario, String, String, Double, String) -> Unit
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var elegido by remember { mutableStateOf<Usuario?>(null) }
    var concepto by rememberSaveable { mutableStateOf("") }
    var motivo by rememberSaveable { mutableStateOf("") }
    var montoTxt by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf(Fechas.hoy()) }
    var error by remember { mutableStateOf<String?>(null) }
    val monto = montoTxt.replace(",", ".").toDoubleOrNull()

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Aplicar descuento", fontWeight = FontWeight.Bold, color = AzulMarino) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Trabajador", fontWeight = FontWeight.SemiBold)
                val u = elegido
                if (u != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TealClaro).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(u.nombre, 38.dp, foto = u.foto)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.nombre, fontWeight = FontWeight.SemiBold)
                            if (u.dni.isNotBlank()) Text("${u.tipoDocumento.etiqueta} ${u.dni}", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                        }
                        IconButton(onClick = { elegido = null }) { Icon(Icons.Default.Close, "Cambiar trabajador") }
                    }
                } else {
                    OutlinedTextField(
                        busqueda, { busqueda = it }, placeholder = { Text("Buscar por nombre o DNI…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                        shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
                    )
                    val encontrados = colaboradores.filter { busqueda.isBlank() || it.nombre.contains(busqueda.trim(), true) || it.dni.contains(busqueda.trim()) }
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, Borde, RoundedCornerShape(14.dp))
                            .verticalScroll(rememberScrollState())
                    ) {
                        encontrados.forEach { c ->
                            Row(
                                Modifier.fillMaxWidth().clickable { elegido = c; error = null }.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(c.nombre, 32.dp, foto = c.foto)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (c.dni.isNotBlank()) Text(c.dni, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
                                }
                            }
                        }
                        if (encontrados.isEmpty()) Text("Sin resultados", Modifier.padding(12.dp), color = TextoSecundario)
                    }
                }

                Text("2. Descuento", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    concepto, { if (it.length <= 80) concepto = it }, label = { Text("Nombre del descuento") }, singleLine = true,
                    shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SUGERENCIAS.forEach { s ->
                        FilterChip(
                            selected = concepto == s, onClick = { concepto = s }, label = { Text(s, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealClaro, selectedLabelColor = Teal)
                        )
                    }
                }
                OutlinedTextField(
                    motivo, { if (it.length <= 300) motivo = it }, label = { Text("Motivo") },
                    placeholder = { Text("Ej.: Adelanto solicitado el lunes") }, minLines = 2,
                    shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        montoTxt, { t -> if (t.length <= 9 && t.all { it.isDigit() || it == '.' || it == ',' }) montoTxt = t },
                        label = { Text("Monto") }, prefix = { Text("$mon ") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.weight(1f)
                    )
                    CampoFecha("Día del descuento", fecha, Modifier.weight(1f)) { fecha = it }
                }
                if (u != null && monto != null && monto > 0) Text(
                    "Se descontará ${dinero(mon, monto)} del pago de ${u.nombre} del ${Fechas.fechaCorta(fecha)}. Le llegará una notificación y no podrá modificarlo.",
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(RojoTinte).padding(10.dp),
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A1F1C)
                )
                error?.let { Text(it, color = Rojo, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val u = elegido
                error = when {
                    u == null -> "Elige al trabajador"
                    concepto.isBlank() -> "Escribe el nombre del descuento"
                    motivo.isBlank() -> "Escribe el motivo"
                    monto == null || monto <= 0 -> "Ingresa un monto mayor a 0"
                    fecha > Fechas.hoy() -> "La fecha no puede ser futura"
                    else -> null
                }
                if (error == null && u != null && monto != null) alConfirmar(u, concepto.trim(), motivo.trim(), monto, fecha)
            }) { Text("Aplicar descuento", color = Rojo, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}
