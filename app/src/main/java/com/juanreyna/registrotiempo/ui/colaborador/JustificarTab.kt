package com.juanreyna.registrotiempo.ui.colaborador

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanreyna.registrotiempo.domain.model.EstadoIncidencia
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.MotivoJustificacion
import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.BotonPillContorno
import com.juanreyna.registrotiempo.ui.components.ChipSimple
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.TarjetaIncidencia
import com.juanreyna.registrotiempo.ui.components.TextoCentrado
import com.juanreyna.registrotiempo.ui.components.TituloSeccion
import com.juanreyna.registrotiempo.ui.components.coloresCampo
import com.juanreyna.registrotiempo.ui.theme.Ambar
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.ui.theme.VerdeEsmeralda
import com.juanreyna.registrotiempo.util.Fechas
import java.util.Calendar

/**
 * Justificación de incidencias: el colaborador ve sus incidencias del mes (por justificar, en revisión,
 * justificadas o no autorizadas) y justifica cada una con un motivo acorde a su tipo.
 * También puede pedir un permiso para otra fecha (vacaciones, cita médica, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PestanaJustificar(vm: ColaboradorViewModel, ui: UiColaborador) {
    val context = LocalContext.current
    val incidencias by vm.incidencias.collectAsStateWithLifecycle()
    val porJustificar = incidencias.filter { it.estado == EstadoIncidencia.POR_JUSTIFICAR }
    val resto = incidencias.filter { it.estado != EstadoIncidencia.POR_JUSTIFICAR }

    // Formulario: incidencia seleccionada (clave) o permiso libre (null)
    var seleccion by rememberSaveable { mutableStateOf<String?>(null) }
    var formularioLibre by rememberSaveable { mutableStateOf(false) }
    val sel = porJustificar.firstOrNull { it.clave == seleccion }
    val tipoSel: TipoIncidencia? = sel?.tipo
    val motivos = MotivoJustificacion.para(tipoSel)
    var motivo by rememberSaveable { mutableStateOf<MotivoJustificacion?>(null) }
    var detalle by rememberSaveable { mutableStateOf("") }
    var fechaLibre by rememberSaveable { mutableStateOf(Fechas.hoy()) }
    var abierto by remember { mutableStateOf(false) }
    val motivoValido = motivo?.takeIf { it in motivos }
    val mostrarFormulario = sel != null || formularioLibre

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Cuando se detecta una falta, tardanza, salida anticipada o exceso de descanso, tu empleador recibe una alerta. " +
                    "Justifícala aquí para que la revise.",
                Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyMedium, color = TextoSecundario
            )
        }

        // 1) Incidencias por justificar
        item { TituloSeccion(if (porJustificar.isEmpty()) "Sin incidencias por justificar" else "Por justificar (${porJustificar.size})") }
        if (porJustificar.isEmpty()) item {
            Text("¡Todo en orden! No tienes incidencias pendientes este mes.", color = VerdeEsmeralda, style = MaterialTheme.typography.bodyMedium)
        }
        items(porJustificar, key = { it.clave }) { i ->
            TarjetaIncidencia(i) {
                if (seleccion == i.clave) Text("Completa el formulario de abajo ↓", color = Teal, fontWeight = FontWeight.SemiBold)
                else BotonPill("Justificar", { seleccion = i.clave; formularioLibre = false; motivo = null; detalle = "" }, alto = 44.dp)
            }
        }

        // 2) Formulario
        if (mostrarFormulario) item {
            Seccion(if (sel != null) "Justificar: ${sel.titulo} · ${sel.fecha}" else "Solicitar permiso / justificar otra fecha", borde = Teal) {
                if (sel == null) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val c = Calendar.getInstance(Fechas.zonaLima)
                            runCatching { c.time = java.text.SimpleDateFormat("yyyy-MM-dd").parse(fechaLibre)!! }
                            DatePickerDialog(context, { _, y, m, d -> fechaLibre = "%04d-%02d-%02d".format(y, m + 1, d) },
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                        }
                    ) {
                        OutlinedTextField(
                            fechaLibre, {}, readOnly = true, enabled = false, label = { Text("Fecha") }, shape = FormaCampo,
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, null) }, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Text("Motivo", fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
                    OutlinedTextField(
                        motivoValido?.etiqueta ?: "Selecciona un motivo", {}, readOnly = true, shape = FormaCampo, colors = coloresCampo(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                        motivos.forEach { DropdownMenuItem(text = { Text(it.etiqueta) }, onClick = { motivo = it; abierto = false }) }
                    }
                }
                Text("Detalle", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    detalle, { detalle = it.take(500) },
                    placeholder = { Text(if (motivoValido == MotivoJustificacion.DESCANSO_MEDICO) "Ej. N° de certificado médico, clínica…" else "Describe lo ocurrido") },
                    minLines = 3, shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
                )
                val listo = motivoValido != null && detalle.trim().length >= 5
                BotonPill(
                    "Enviar justificación",
                    {
                        vm.enviarJustificacion(motivoValido!!, detalle.trim(), sel?.fecha ?: fechaLibre, sel?.tipo)
                        seleccion = null; formularioLibre = false; motivo = null; detalle = ""
                    },
                    enabled = listo
                )
                if (!listo) Text("Elige un motivo y escribe al menos 5 caracteres.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                BotonPillContorno("Cancelar", { seleccion = null; formularioLibre = false }, Modifier.fillMaxWidth(), color = TextoSecundario)
            }
        } else item {
            BotonPillContorno("Solicitar permiso para otra fecha", { formularioLibre = true; seleccion = null; motivo = null }, Modifier.fillMaxWidth(), color = AzulMarino)
        }

        // 3) Historial de incidencias del mes (en revisión, justificadas, no autorizadas)
        if (resto.isNotEmpty()) {
            item { TituloSeccion("Incidencias del mes") }
            items(resto, key = { it.clave }) { TarjetaIncidencia(it) }
        }

        // 4) Permisos / justificaciones sin incidencia asociada
        val libres = ui.justificaciones.filter { j -> incidencias.none { it.justificacion?.id == j.id } }
        if (libres.isNotEmpty()) {
            item { TituloSeccion("Otras justificaciones y permisos") }
            items(libres, key = { it.id }) { j ->
                Seccion(borde = Borde) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text((j.incidencia?.etiqueta?.let { "$it · " } ?: "") + j.motivo.etiqueta, fontWeight = FontWeight.SemiBold)
                            Text(j.fecha, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                        }
                        when (j.estado) {
                            EstadoJustificacion.PENDIENTE -> ChipSimple("En revisión", Ambar)
                            EstadoJustificacion.APROBADA -> ChipSimple("Aprobada", VerdeEsmeralda)
                            EstadoJustificacion.RECHAZADA -> ChipSimple("No autorizada", Rojo)
                        }
                    }
                    if (j.detalle.isNotBlank()) Text(j.detalle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (incidencias.isEmpty() && ui.justificaciones.isEmpty()) item { TextoCentrado("Aún no tienes incidencias ni justificaciones.") }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
