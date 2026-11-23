package com.juanreyna.registrotiempo.ui.admin

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import com.juanreyna.registrotiempo.domain.calc.PlanillaAvanzada
import androidx.compose.material.icons.filled.Event
import com.juanreyna.registrotiempo.ui.components.SelectorDocumento
import com.juanreyna.registrotiempo.ui.login.CampoDigitos
import com.juanreyna.registrotiempo.domain.model.TipoDocumento
import androidx.compose.material.icons.filled.Logout
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToLong
import com.juanreyna.registrotiempo.domain.calc.ResumenColaborador
import com.juanreyna.registrotiempo.domain.model.Horario
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Checkbox
import android.app.TimePickerDialog
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.EstadoColaborador
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.Geocerca
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.Moneda
import com.juanreyna.registrotiempo.domain.model.RegimenLaboral
import com.juanreyna.registrotiempo.domain.model.SistemaPension
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.ui.colaborador.InfoVip
import com.juanreyna.registrotiempo.ui.colaborador.NavInferior
import com.juanreyna.registrotiempo.ui.components.Avatar
import com.juanreyna.registrotiempo.ui.components.BannerVip
import com.juanreyna.registrotiempo.ui.components.BarraResumen
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.BotonPillContorno
import com.juanreyna.registrotiempo.ui.components.Cargando
import com.juanreyna.registrotiempo.ui.components.ChipEstado
import com.juanreyna.registrotiempo.ui.components.ChipSimple
import com.juanreyna.registrotiempo.ui.components.FilaDato
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.components.MarcoPantalla
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.TarjetaAviso
import com.juanreyna.registrotiempo.ui.components.TextoCentrado
import com.juanreyna.registrotiempo.ui.components.TituloSeccion
import com.juanreyna.registrotiempo.ui.components.coloresCampo
import com.juanreyna.registrotiempo.ui.components.dinero
import com.juanreyna.registrotiempo.ui.components.tinteEstado
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

@Composable
fun AdminScreen(
    infoVip: InfoVip,
    alVerVip: () -> Unit,
    alCerrarBanner: () -> Unit,
    alAjustes: () -> Unit,
    alPerfil: () -> Unit,
    vm: AdminViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val incidencias by vm.incidencias.collectAsStateWithLifecycle()
    val admin by vm.admin.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val leidas by vm.leidas.collectAsStateWithLifecycle()
    val notificaciones = notificacionesSinLeer(ui, incidencias.orEmpty(), leidas)
    val totalSinLeer = notificaciones.sumOf { it.claves.size }
    val snackbar = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var verNotificaciones by remember { mutableStateOf(false) }
    LaunchedEffect(mensaje) { mensaje?.let { snackbar.showSnackbar(it); vm.consumirMensaje() } }

    val nombre = admin?.nombre?.substringBefore(" ")?.ifBlank { null } ?: "Administrador"
    val titulos = listOf("Hola, $nombre", "Gestión de Colaboradores", "Zonas de Trabajo (Geocercas)", "Justificaciones", "Reportes SUNAFIL / Planilla", "Horarios de Trabajo")
    Box(Modifier.fillMaxSize()) {
        MarcoPantalla(
            titulo = titulos[pestana],
            subtitulo = if (pestana == 0) "(Admin) · ${ui.empresa?.nombreComercial.orEmpty()}" else ui.empresa?.nombreComercial,
            avatar = if (pestana == 0) admin?.nombre ?: "A" else null,
            avatarFoto = admin?.foto.orEmpty(),
            alTocarAvatar = alPerfil,
            acciones = {
                IconButton(onClick = { verNotificaciones = true }) {
                    androidx.compose.material3.BadgedBox(badge = { if (totalSinLeer > 0) androidx.compose.material3.Badge { Text(if (totalSinLeer > 99) "99+" else "$totalSinLeer") } }) {
                        Icon(Icons.Default.Notifications, "Notificaciones", tint = Color.White)
                    }
                }
                IconButton(onClick = alAjustes) { Icon(Icons.Default.Settings, "Ajustes", tint = Color.White) }
            },
            pie = {
                Column {
                    if (pestana == 0) BarraResumen(
                        "Costo Estimado Hoy", dinero(ui.empresa?.moneda?.simbolo ?: "S/", ui.costoHoy),
                        "Horas Totales", "%.1f hrs".format(ui.horasHoy)
                    )
                    NavInferior(
                        listOf(
                            Triple("Panel", Icons.Default.Dashboard, 0), Triple("Equipo", Icons.Default.Groups, 1),
                            Triple("Horarios", Icons.Default.Schedule, 5),
                            Triple("Zonas", Icons.Default.Map, 2), Triple("Incid.", Icons.Default.Rule, 3),
                            Triple("Reportes", Icons.Default.Assessment, 4)
                        ), pestana, { pestana = it }, insignia = { if (it == 3) ui.pendientes + (incidencias?.count { i -> i.estado == com.juanreyna.registrotiempo.domain.model.EstadoIncidencia.POR_JUSTIFICAR } ?: 0) else 0 }
                    )
                }
            }
        ) {
            infoVip.mensaje?.let { BannerVip(it, alVerVip, if (infoVip.cerrable) alCerrarBanner else null) }
            val empresa = ui.empresa
            if (empresa == null) { Cargando(); return@MarcoPantalla }
            when (pestana) {
                0 -> PestanaPanel(vm, ui)
                1 -> PestanaEquipo(vm, ui)
                2 -> PestanaGeocerca(empresa.geocerca) { vm.guardarGeocerca(it) }
                3 -> PestanaIncidencias(vm, ui)
                5 -> PestanaHorarios(vm, ui)
                else -> PestanaReportes(vm, ui, infoVip)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
    if (verNotificaciones) {
        // Al cerrar el centro, todo lo que se mostró queda como leído y el contador baja a cero
        val marcarTodo = { vm.marcarLeidas(notificaciones.flatMap { it.claves }); verNotificaciones = false }
        CentroNotificaciones(
            notificaciones, alDescartar = { vm.marcarLeidas(it.claves) },
            alVerIncidencias = { marcarTodo(); pestana = 3 }, alCerrar = marcarTodo
        )
    }
}

/** Centro de Notificaciones: alertas en vivo, faltas, tardanzas e incidencias sin justificar. */
@Composable
private fun CentroNotificaciones(notificaciones: List<Notificacion>, alDescartar: (Notificacion) -> Unit, alVerIncidencias: () -> Unit, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Centro de Notificaciones", fontWeight = FontWeight.Bold, color = AzulMarino) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notificaciones.forEach { n ->
                    val icono = when {
                        n.grave -> Icons.Default.Warning
                        n.irAIncidencias -> Icons.Default.CheckBox
                        else -> Icons.Default.Notifications
                    }
                    TarjetaAviso(
                        icono, n.titulo, n.detalle,
                        if (n.grave) Rojo else if (n.irAIncidencias) AzulMarino else Ambar,
                        if (n.grave) RojoTinte else Color.White,
                        alDescartar = { alDescartar(n) }
                    )
                }
                if (notificaciones.any { it.irAIncidencias }) TextButton(onClick = alVerIncidencias) { Text("Ver incidencias") }
                if (notificaciones.isEmpty()) Text("Sin notificaciones nuevas.", color = TextoSecundario)
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text(if (notificaciones.isEmpty()) "Cerrar" else "Marcar como leídas") } }
    )
}

@Composable
private fun PestanaPanel(vm: AdminViewModel, ui: UiAdmin) {
    var salidaDe by remember { mutableStateOf<FilaMonitoreo?>(null) }
    salidaDe?.let { f -> DialogoSalida(f, alCerrar = { salidaDe = null }) { h -> vm.marcarSalida(f, h); salidaDe = null } }
    val e = ui.empresa!!
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val mon = e.moneda.simbolo
    val textoCompartir = "Únete a ${e.nombreComercial} en la app Registro de Tiempo de Personal con el Código de Administrador: ${e.codigoAdministrador}"
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // Tarjeta azul marino del código para colaboradores
            Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AzulMarino).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Código para Colaboradores:", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    Text(e.codigoAdministrador, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                IconCuadrado(Icons.Default.ContentCopy) { clipboard.setText(AnnotatedString(e.codigoAdministrador)) }
                Spacer(Modifier.width(8.dp))
                IconCuadrado(Icons.Default.Share) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=" + Uri.encode(textoCompartir)))) }
                }
                Spacer(Modifier.width(8.dp))
                IconCuadrado(Icons.Default.Email) {
                    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "Código de vinculación — ${e.nombreComercial}")
                        putExtra(Intent.EXTRA_TEXT, textoCompartir)
                    }
                    runCatching { context.startActivity(i) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Contador("Activos", ui.filas.count { it.estado == EstadoColaborador.DENTRO_AREA || it.estado == EstadoColaborador.TARDANZA }, TealClaro, Teal, Modifier.weight(1f))
                Contador("En descanso", ui.filas.count { it.estado == EstadoColaborador.EN_DESCANSO }, AmbarTinte, Ambar, Modifier.weight(1f))
                Contador("Fuera de Rango", ui.filas.count { it.estado == EstadoColaborador.FUERA_RANGO }, RojoTinte, Rojo, Modifier.weight(1f))
            }
        }
        item { TituloSeccion("Actividad en Vivo") }
        items(ui.filas, key = { it.usuario.uid }) { f ->
            Seccion(fondo = tinteEstado(f.estado), borde = Borde) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(f.usuario.nombre, 48.dp, foto = f.usuario.foto)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.usuario.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        ChipEstado(f.estado, if (f.estado == EstadoColaborador.DENTRO_AREA) e.geocerca.nombre else null)
                    }
                    if (f.ultimo?.validacionFacial == true) Icon(Icons.Default.Verified, "Validación facial", tint = VerdeEsmeralda)
                }
                f.ultimo?.let { m ->
                    val icono = if (m.tipo == TipoMarcaje.INICIO_DESCANSO) Icons.Default.Coffee else Icons.Default.Schedule
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icono, null, tint = TextoSecundario, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${m.tipo.etiqueta}: ${Fechas.horaCorta(m.timestampMillis)} · ${m.distanciaMetros.toInt()} m del centro", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                FilaDato("Horas hoy · costo", "%.2f h · %s".format(f.horasHoy, dinero(mon, f.costoHoy)))
                if (f.ultimo?.registradoPorAdmin == true)
                    Text("Salida registrada por el empleador", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                if (f.estado != EstadoColaborador.SIN_MARCAR && f.estado != EstadoColaborador.JORNADA_CERRADA)
                    BotonPillContorno("Marcar hora de salida", { salidaDe = f }, Modifier.fillMaxWidth(), color = Rojo, icono = Icons.Default.Logout)
            }
        }
        if (ui.filas.isEmpty()) item { TextoCentrado("Aún no hay colaboradores vinculados. Comparte tu código para empezar.") }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** El empleador elige la hora de salida del colaborador (por defecto, la del horario o la hora actual). */
@Composable
private fun DialogoSalida(f: FilaMonitoreo, alCerrar: () -> Unit, alConfirmar: (String) -> Unit) {
    val ahora = Fechas.hhmm(System.currentTimeMillis())
    val programada = f.usuario.horario.deFecha(Fechas.hoy()).salida
    var hora by remember { mutableStateOf(if (Horario.minutos(programada) <= Horario.minutos(ahora)) programada else ahora) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Hora de salida") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${f.usuario.nombre} · ingreso/último marcaje: ${Fechas.horaCorta(f.ultimo?.timestampMillis)}", style = MaterialTheme.typography.bodyMedium)
                CampoHora("Salida", hora, Modifier.fillMaxWidth()) { hora = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonPillContorno("Horario $programada", { hora = programada }, Modifier.weight(1f), color = Teal)
                    BotonPillContorno("Ahora $ahora", { hora = ahora }, Modifier.weight(1f), color = Teal)
                }
                Text("Se registrará como salida marcada por el empleador y cerrará la jornada de hoy.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
        },
        confirmButton = { TextButton({ alConfirmar(hora) }) { Text("Registrar salida") } },
        dismissButton = { TextButton(alCerrar) { Text("Cancelar") } }
    )
}

@Composable
private fun IconCuadrado(icono: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Teal).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icono, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
internal fun Contador(titulo: String, valor: Int, fondo: Color, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(fondo).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$valor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun PestanaEquipo(vm: AdminViewModel, ui: UiAdmin) {
    var editando by remember { mutableStateOf<Usuario?>(null) }
    var busqueda by rememberSaveable { mutableStateOf("") }
    val e = ui.empresa!!
    var modoGlobal by remember(e.modoPagoGlobal) { mutableStateOf(e.modoPagoGlobal) }
    var moneda by remember(e.moneda) { mutableStateOf(e.moneda) }
    val filtrados = ui.colaboradores.filter { busqueda.isBlank() || it.nombre.contains(busqueda, true) || it.dni.contains(busqueda) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Seccion("Modo de Cálculo", Modifier.padding(top = 16.dp), borde = Borde) {
                OpcionModo("Modo Simple", "Pago por hora directo (MYPE)", modoGlobal == ModoPago.SIMPLE) { modoGlobal = ModoPago.SIMPLE }
                OpcionModo("Modo Avanzado", "Planilla y Leyes de Perú (REMYPE / Régimen General)", modoGlobal == ModoPago.AVANZADO) { modoGlobal = ModoPago.AVANZADO }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Moneda.entries.forEach { m -> ChipFiltro("${m.name} ${m.simbolo}", moneda == m) { moneda = m } }
                }
                BotonPill("Aplicar a todo el equipo", { vm.aplicarModoGlobal(modoGlobal, moneda.name) }, alto = 46.dp)
            }
        }
        item {
            OutlinedTextField(
                busqueda, { busqueda = it }, placeholder = { Text("Buscar colaborador…") },
                trailingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
            )
        }
        item { Text("Activos registrados: ${ui.colaboradores.count { it.activo }}", color = VerdeEsmeralda, fontWeight = FontWeight.SemiBold) }
        items(filtrados, key = { it.uid }) { u ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(1.dp, Borde, RoundedCornerShape(16.dp)).clickable { editando = u }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(u.nombre, 44.dp, foto = u.foto)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.nombre, fontWeight = FontWeight.SemiBold)
                    val c = u.configPago
                    Text(
                        if (c.modo == ModoPago.SIMPLE) "Modo Simple · ${dinero(e.moneda.simbolo, c.tarifaPorHora)}/h"
                        else if (c.pagoPorHora) "Avanzado · ${dinero(e.moneda.simbolo, c.tarifaPorHora)}/h · ${c.regimen.etiqueta}"
                        else "Avanzado · ${dinero(e.moneda.simbolo, c.sueldoBase)}/mes (${dinero(e.moneda.simbolo, c.valorHora)}/h) · ${c.regimen.etiqueta}",
                        style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                    )
                }
                if (u.activo) Icon(Icons.Default.CheckCircle, "Activo", tint = VerdeEsmeralda) else ChipSimple("Inactivo", Rojo)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Edit, "Editar", tint = TextoSecundario)
            }
        }
        if (filtrados.isEmpty()) item { TextoCentrado("No hay colaboradores para mostrar.") }
        item { Spacer(Modifier.height(8.dp)) }
    }
    var desvinculando by remember { mutableStateOf<Usuario?>(null) }
    var eliminando by remember { mutableStateOf<Usuario?>(null) }
    eliminando?.let { u ->
        DialogoEliminarColaborador(u, alConfirmar = { borrarHistorial -> vm.eliminarColaborador(u, borrarHistorial); eliminando = null }, alCerrar = { eliminando = null })
    }
    editando?.let { u ->
        DialogoColaborador(
            u, e.moneda.simbolo, alCerrar = { editando = null }, alDesvincular = { editando = null; desvinculando = u },
            alEliminar = { editando = null; eliminando = u }
        ) { cfg, horario, activo, tipoDoc, dni ->
            vm.guardarColaborador(u, cfg, horario, activo, tipoDoc, dni); editando = null
        }
    }
    desvinculando?.let { u ->
        AlertDialog(
            onDismissRequest = { desvinculando = null },
            title = { Text("¿Desvincular a ${u.nombre}?", color = Rojo, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Saldrá de tu equipo y ya no podrá marcar asistencia en tu empresa. Su historial de marcajes se conserva en tus reportes. " +
                        "Su cuenta sigue activa: podrá unirse a otra empresa o volver a la tuya con tu código."
                )
            },
            confirmButton = { TextButton(onClick = { vm.desvincular(u); desvinculando = null }) { Text("Desvincular", color = Rojo) } },
            dismissButton = { TextButton(onClick = { desvinculando = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun OpcionModo(titulo: String, detalle: String, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (seleccionado) TealClaro else Color.White)
            .border(1.dp, if (seleccionado) Teal else Borde, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = AzulMarino)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
        RadioButton(seleccionado, onClick, colors = RadioButtonDefaults.colors(selectedColor = Teal))
    }
}

@Composable
private fun ChipFiltro(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        seleccionado, onClick, { Text(texto) }, shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Teal, selectedLabelColor = Color.White)
    )
}

@Composable
private fun DialogoColaborador(u: Usuario, mon: String, alCerrar: () -> Unit, alDesvincular: () -> Unit, alEliminar: () -> Unit, alGuardar: (ConfigPago, Horario, Boolean, TipoDocumento, String) -> Unit) {
    var cfg by remember { mutableStateOf(u.configPago) }
    var tipoDoc by remember { mutableStateOf(u.tipoDocumento) }
    var dni by remember { mutableStateOf(u.dni) }
    val dniValido = tipoDoc.esValido(dni)
    var tarifa by remember { mutableStateOf(if (cfg.tarifaPorHora > 0) cfg.tarifaPorHora.toString() else "") }
    var sueldo by remember { mutableStateOf(if (cfg.sueldoBase > 0) cfg.sueldoBase.toString() else "") }
    val rmv = remember { com.juanreyna.registrotiempo.domain.calc.ParametrosLegales.delMes(Fechas.mesActual()).rmv }
    var horario by remember { mutableStateOf(u.horario) }
    var tolerancia by remember { mutableStateOf(u.horario.toleranciaMin.toString()) }
    val toleranciaMin = tolerancia.toIntOrNull()
    val horarioValido = horario.jornadasValidas() && toleranciaMin != null && toleranciaMin in 0..60
    var activo by remember { mutableStateOf(u.activo) }
    AlertDialog(
        onDismissRequest = alCerrar,
        icon = { Avatar(u.nombre, 72.dp, foto = u.foto) },
        title = { Text(u.nombre, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectorDocumento(tipoDoc, { t -> tipoDoc = t; dni = t.limpiar(dni) })
                CampoDigitos(dni, { dni = tipoDoc.limpiar(it) }, "Número de ${tipoDoc.etiqueta}", tipoDoc.digitos, error = !dniValido)
                HorizontalDivider(color = Borde)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipFiltro("Simple", cfg.modo == ModoPago.SIMPLE) { cfg = cfg.copy(modo = ModoPago.SIMPLE) }
                    ChipFiltro("Avanzado Perú", cfg.modo == ModoPago.AVANZADO) { cfg = cfg.copy(modo = ModoPago.AVANZADO) }
                }
                if (cfg.modo == ModoPago.SIMPLE) {
                    OutlinedTextField(tarifa, { tarifa = it }, label = { Text("Pago por hora ($mon)") }, shape = FormaCampo,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    Text("Horas extras: se pagan a la misma tarifa por hora, sin recargo.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                } else {
                    Text("Tipo de remuneración", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipFiltro("Sueldo mensual", !cfg.pagoPorHora) { cfg = cfg.copy(pagoPorHora = false) }
                        ChipFiltro("Por hora", cfg.pagoPorHora) { cfg = cfg.copy(pagoPorHora = true) }
                    }
                    if (cfg.pagoPorHora) {
                        val rmvHora = rmv / 240.0
                        val tarifaNum = tarifa.replace(',', '.').toDoubleOrNull() ?: 0.0
                        OutlinedTextField(tarifa, { tarifa = it }, label = { Text("Remuneración por hora ($mon)") }, shape = FormaCampo,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = tarifaNum in 0.01..(rmvHora - 0.01),
                            supportingText = {
                                Text(
                                    if (tarifaNum in 0.01..(rmvHora - 0.01)) "Menor que la RMV por hora (S/ ${"%.2f".format(rmvHora)})"
                                    else "Se paga cada hora trabajada. Mínimo: S/ ${"%.2f".format(rmvHora)} por hora (RMV ÷ 240)"
                                )
                            })
                    } else {
                        val sueldoNum = sueldo.replace(',', '.').toDoubleOrNull() ?: 0.0
                        OutlinedTextField(sueldo, { sueldo = it }, label = { Text("Sueldo base mensual ($mon)") }, shape = FormaCampo,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = sueldoNum in 0.01..(rmv - 0.01),
                            supportingText = {
                                Text(
                                    if (sueldoNum in 0.01..(rmv - 0.01)) "Menor que la RMV (S/ ${"%,.2f".format(rmv)}): solo válido con jornada menor a 4 h"
                                    else "Valor hora: ${dinero(mon, sueldoNum / 240.0)} (sueldo ÷ 240 h) · RMV vigente: S/ ${"%,.2f".format(rmv)}"
                                )
                            })
                        Text("El sueldo mensual se paga completo (30 días); solo se descuentan faltas y tardanzas no justificadas.",
                            style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                    Text("Datos completos (planilla y boleta)", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(cfg.apellidoPaterno, { cfg = cfg.copy(apellidoPaterno = it.take(40)) }, label = { Text("Apellido paterno") },
                        shape = FormaCampo, singleLine = true)
                    OutlinedTextField(cfg.apellidoMaterno, { cfg = cfg.copy(apellidoMaterno = it.take(40)) }, label = { Text("Apellido materno") },
                        shape = FormaCampo, singleLine = true)
                    OutlinedTextField(cfg.nombres, { cfg = cfg.copy(nombres = it.take(60)) }, label = { Text("Nombres") },
                        shape = FormaCampo, singleLine = true,
                        supportingText = { Text("Tal como figuran en el ${tipoDoc.etiqueta}") })
                    OutlinedTextField(cfg.cargo, { cfg = cfg.copy(cargo = it.take(60)) }, label = { Text("Cargo o puesto") },
                        shape = FormaCampo, singleLine = true)
                    OutlinedTextField(cfg.centroCosto, { cfg = cfg.copy(centroCosto = it.take(20)) }, label = { Text("Centro de costo (opcional)") },
                        shape = FormaCampo, singleLine = true)
                    CampoFecha("Fecha de ingreso", cfg.fechaIngreso.ifBlank { u.creadoMillis?.let { Fechas.fechaDe(it) } ?: Fechas.hoy() }) {
                        cfg = cfg.copy(fechaIngreso = it)
                    }
                    Text("Régimen Laboral", fontWeight = FontWeight.SemiBold)
                    RegimenLaboral.entries.forEach { r ->
                        Row(Modifier.fillMaxWidth().clickable { cfg = cfg.copy(regimen = r) }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(cfg.regimen == r, { cfg = cfg.copy(regimen = r) }, colors = RadioButtonDefaults.colors(selectedColor = Teal))
                            Text(r.etiqueta)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Asignación familiar (10 % RMV)", Modifier.weight(1f))
                        Switch(cfg.asignacionFamiliar, { cfg = cfg.copy(asignacionFamiliar = it) }, colors = SwitchDefaults.colors(checkedTrackColor = Teal))
                    }
                    Text("Sistema de Pensiones", fontWeight = FontWeight.SemiBold)
                    SistemaPension.entries.chunked(3).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            fila.forEach { s -> ChipFiltro(s.etiqueta.removePrefix("AFP "), cfg.sistemaPension == s) { cfg = cfg.copy(sistemaPension = s) } }
                        }
                    }
                    if (cfg.sistemaPension != SistemaPension.ONP) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Comisión mixta (sin comisión flujo)", Modifier.weight(1f))
                            Switch(cfg.comisionMixta, { cfg = cfg.copy(comisionMixta = it) }, colors = SwitchDefaults.colors(checkedTrackColor = Teal))
                        }
                        OutlinedTextField(cfg.cuspp, { cfg = cfg.copy(cuspp = it.uppercase().filter(Char::isLetterOrDigit).take(12)) },
                            label = { Text("CUSPP (código de afiliado AFP)") }, shape = FormaCampo, singleLine = true)
                    }
                    if (cfg.asignacionFamiliar && cfg.regimen != RegimenLaboral.GENERAL)
                        Text("En el REMYPE la asignación familiar no es obligatoria.", style = MaterialTheme.typography.bodySmall, color = Ambar)
                    Text("Horas extras con recargo de ley: +25 % las 2 primeras horas del día y +35 % desde la tercera.",
                        style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                Text("Horario de trabajo", fontWeight = FontWeight.SemiBold)
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
                    tolerancia, { tolerancia = it.filter(Char::isDigit).take(2) }, label = { Text("Tolerancia (minutos)") },
                    shape = FormaCampo, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = toleranciaMin == null || toleranciaMin > 60
                )
                Text(
                    if (horario.jornadasValidas())
                        (if (horario.porDia) "Total semanal: ${Fechas.duracion(horario.minutosSemana())}\n"
                        else "Jornada: ${Fechas.duracion(horario.minutosJornadaProgramada.toLong())} · Descanso: ${Fechas.duracion(horario.minutosDescansoProgramado.toLong())}\n") +
                            "Tardanza: ingreso después de la entrada + tolerancia. Horas extras: salida después de la hora de salida. " +
                            "El exceso de descanso no se paga."
                    else "Revisa el horario: la jornada no puede quedar en cero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (horario.jornadasValidas()) TextoSecundario else Rojo
                )
                Text("Para cambiar el horario de varios colaboradores a la vez usa la pestaña Horarios.",
                    style = MaterialTheme.typography.bodySmall, color = Teal)
                HorizontalDivider(color = Borde)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        ChipSimple(if (activo) "Activo" else "Dado de baja", if (activo) VerdeEsmeralda else Rojo)
                        Text(
                            if (activo) "Apágalo para dar de baja: no podrá marcar, pero conserva su historial y puedes reactivarlo."
                            else "No puede marcar asistencia. Enciéndelo para reactivarlo.",
                            style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(activo, { activo = it }, colors = SwitchDefaults.colors(checkedTrackColor = Teal))
                }
                TextButton(onClick = alDesvincular) { Text("Desvincular del equipo", color = Rojo) }
                TextButton(onClick = alEliminar) { Text("Eliminar cuenta del colaborador", color = Rojo, fontWeight = FontWeight.SemiBold) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                alGuardar(
                    cfg.copy(
                        tarifaPorHora = tarifa.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        sueldoBase = sueldo.replace(',', '.').toDoubleOrNull() ?: 0.0
                    ), horario.copy(toleranciaMin = toleranciaMin ?: 5), activo, tipoDoc, dni
                )
            }, enabled = horarioValido && dniValido) { Text("Guardar", color = Teal, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}

/** Campo de fecha "yyyy-MM-dd" que abre el calendario del sistema. */
@Composable
internal fun CampoFecha(etiqueta: String, valor: String, modifier: Modifier = Modifier, alCambiar: (String) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Borde, RoundedCornerShape(12.dp))
            .clickable {
                val p = valor.split("-").mapNotNull { it.toIntOrNull() }
                val (a, m, d) = if (p.size == 3) Triple(p[0], p[1] - 1, p[2]) else Triple(2026, 0, 1)
                android.app.DatePickerDialog(context, { _, y, mo, dd -> alCambiar("%04d-%02d-%02d".format(y, mo + 1, dd)) }, a, m, d).show()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(valor.split("-").reversed().joinToString("/"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AzulMarino)
            Icon(Icons.Default.Event, null, tint = Teal, modifier = Modifier.size(18.dp))
        }
    }
}

/** Campo de hora que abre el reloj del sistema (formato 24 h). */
@Composable
internal fun CampoHora(etiqueta: String, valor: String, modifier: Modifier = Modifier, alCambiar: (String) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, Borde, RoundedCornerShape(12.dp))
            .clickable {
                val m = Horario.minutos(valor)
                TimePickerDialog(context, { _, h, min -> alCambiar("%02d:%02d".format(h, min)) }, m / 60, m % 60, true).show()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(valor, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AzulMarino)
            Icon(Icons.Default.Schedule, null, tint = Teal, modifier = Modifier.size(18.dp))
        }
    }
}

/** Radio mínimo de la geocerca: por debajo de 20 m el margen de error del GPS (5–15 m) rechaza marcajes válidos. */
private const val RADIO_MINIMO = 20f

/** Lee la ubicación actual del celular (alta precisión, con respaldo a la última conocida). */
@SuppressLint("MissingPermission")
private suspend fun ubicacionActual(context: Context): LatLng? = runCatching {
    val cliente = LocationServices.getFusedLocationProviderClient(context)
    val loc = cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
        ?: cliente.lastLocation.await()
    loc?.let { LatLng(it.latitude, it.longitude) }
}.getOrNull()

private fun tienePermisoUbicacion(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PestanaGeocerca(actual: Geocerca, alGuardar: (Geocerca) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Una geocerca aún no configurada conserva la coordenada por defecto: se reemplaza por la ubicación real del celular.
    val sinConfigurar = actual.latitud == Geocerca().latitud && actual.longitud == Geocerca().longitud
    var nombre by remember(actual) { mutableStateOf(actual.nombre) }
    var centro by remember(actual) { mutableStateOf(LatLng(actual.latitud, actual.longitud)) }
    // Zonas guardadas antes con radios de 5 o 10 m se ajustan al nuevo mínimo.
    var radio by remember(actual) { mutableStateOf(actual.radioMetros.toFloat().coerceAtLeast(RADIO_MINIMO)) }
    var permiso by remember { mutableStateOf(tienePermisoUbicacion(context)) }
    var buscando by remember { mutableStateOf(false) }
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(centro, 17f) }
    val marcador = rememberMarkerState(position = centro)
    LaunchedEffect(centro) { if (marcador.position != centro) marcador.position = centro }
    // Al arrastrar el pin, el centro (y el círculo de cobertura) lo siguen en tiempo real.
    LaunchedEffect(marcador) {
        snapshotFlow { marcador.position }.collect { if (it != centro) centro = it }
    }

    fun irAMiUbicacion(fijarCentro: Boolean) {
        if (buscando) return
        buscando = true
        scope.launch {
            val aqui = ubicacionActual(context)
            buscando = false
            if (aqui == null) {
                Toast.makeText(context, "No se pudo obtener tu ubicación. Activa el GPS e inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (fijarCentro) centro = aqui
            camara.animate(CameraUpdateFactory.newLatLngZoom(aqui, 17f))
        }
    }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        permiso = r.values.any { it }
        if (permiso) irAMiUbicacion(fijarCentro = sinConfigurar)
    }
    LaunchedEffect(actual) {
        if (permiso) {
            if (sinConfigurar) irAMiUbicacion(fijarCentro = true)
        } else {
            pedirPermiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Altura de la hoja inferior: el mapa la usa como relleno para centrar el pin en la zona visible.
    val densidad = LocalDensity.current
    var alturaHoja by remember { mutableStateOf(0.dp) }
    var ampliado by rememberSaveable { mutableStateOf(false) }
    var recentrar by remember { mutableStateOf(false) }
    var arrastre by remember { mutableFloatStateOf(0f) }
    fun alternarAmpliado() {
        ampliado = !ampliado
        recentrar = true
    }
    // La altura de la hoja cambia en cada fotograma de su animación; cuando se estabiliza, recentra la zona
    // en el nuevo espacio visible (cada cambio cancela la espera anterior).
    LaunchedEffect(alturaHoja, recentrar) {
        if (!recentrar) return@LaunchedEffect
        delay(200)
        camara.animate(CameraUpdateFactory.newLatLng(centro))
        recentrar = false // al final: cambiar esta clave antes cancelaría la animación
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Con el teclado abierto el espacio se reduce: la hoja no pasa de esta altura y se desplaza por dentro.
        val alturaMaxHoja = maxHeight
        GoogleMap(
            Modifier.fillMaxSize(),
            cameraPositionState = camara,
            contentPadding = PaddingValues(bottom = alturaHoja),
            properties = MapProperties(isMyLocationEnabled = permiso),
            uiSettings = MapUiSettings(myLocationButtonEnabled = permiso, zoomControlsEnabled = false),
            onMapClick = { centro = it }
        ) {
            Marker(state = marcador, title = nombre, snippet = "Mantén presionado y arrastra para moverlo", draggable = true)
            Circle(center = centro, radius = radio.toDouble(), strokeColor = VerdeEsmeralda, fillColor = VerdeEsmeralda.copy(alpha = 0.25f), strokeWidth = 4f)
        }
        // Botón para ampliar / restaurar el mapa (esquina superior izquierda; la derecha es de "Mi ubicación")
        Row(
            Modifier.align(Alignment.TopStart).padding(12.dp)
                .clip(RoundedCornerShape(50)).background(Color.White)
                .clickable { alternarAmpliado() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (ampliado) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, null, tint = Teal)
            Spacer(Modifier.width(6.dp))
            Text(if (ampliado) "Reducir mapa" else "Ampliar mapa", color = Teal, fontWeight = FontWeight.SemiBold)
        }
        // Hoja inferior de configuración (compacta cuando el mapa está ampliado)
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .heightIn(max = alturaMaxHoja)
                .onSizeChanged { alturaHoja = with(densidad) { it.height.toDp() } }
                // Mientras se arrastra hacia abajo, la hoja acompaña al dedo.
                .graphicsLayer { translationY = arrastre.coerceAtLeast(0f) }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color.White)
                .animateContentSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Asa de la hoja: tocar alterna; arrastrar hacia abajo amplía el mapa y hacia arriba lo reduce.
            Box(
                Modifier.fillMaxWidth().height(28.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> arrastre += delta },
                        onDragStopped = {
                            val umbral = with(densidad) { 40.dp.toPx() }
                            when {
                                arrastre > umbral && !ampliado -> alternarAmpliado()
                                arrastre < -umbral && ampliado -> alternarAmpliado()
                            }
                            arrastre = 0f
                        }
                    )
                    .clickable { alternarAmpliado() },
                contentAlignment = Alignment.Center
            ) { Box(Modifier.size(width = 48.dp, height = 5.dp).clip(RoundedCornerShape(50)).background(TextoSecundario.copy(alpha = 0.5f))) }
            if (ampliado) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Radio: ${radio.toInt()} m", fontWeight = FontWeight.SemiBold)
                        Text(nombre, style = MaterialTheme.typography.bodySmall, color = TextoSecundario, maxLines = 1)
                    }
                    TextButton(onClick = { alternarAmpliado() }) {
                        Text("Ajustes", color = Teal, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ExpandLess, null, tint = Teal)
                    }
                }
                BotonPill("Guardar Zona", { alGuardar(Geocerca(nombre.trim(), centro.latitude, centro.longitude, radio.toDouble())) }, alto = 48.dp)
            } else {
            Text("Radio de Cobertura: ${radio.toInt()} metros", fontWeight = FontWeight.SemiBold)
            Slider(radio, { radio = it }, valueRange = RADIO_MINIMO..500f, colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(20f, 50f, 100f, 150f).forEach { r -> ChipFiltro("${r.toInt()} m", radio.toInt() == r.toInt()) { radio = r } }
            }
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre de la sede u obra") }, singleLine = true, shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth())
            Text("Mantén presionado el pin rojo y arrástralo, o toca el mapa, para fijar el centro · %.6f, %.6f".format(centro.latitude, centro.longitude), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            BotonPillContorno(if (buscando) "Buscando ubicación…" else "Usar mi ubicación actual", {
                if (permiso) irAMiUbicacion(fijarCentro = true)
                else pedirPermiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, Modifier.fillMaxWidth(), color = Teal)
            BotonPill("Guardar Zona", { alGuardar(Geocerca(nombre.trim(), centro.latitude, centro.longitude, radio.toDouble())) }, alto = 48.dp)
            }
        }
    }
}


@Composable
private fun PestanaReportes(vm: AdminViewModel, ui: UiAdmin, infoVip: InfoVip) {
    val context = LocalContext.current
    val resumen by vm.resumenMes.collectAsStateWithLifecycle()
    val mes by vm.mesReporte.collectAsStateWithLifecycle()
    LaunchedEffect(ui.colaboradores.size) { vm.cargarMes() }
    val mon = ui.empresa!!.moneda.simbolo
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPillContorno("◀ ${Fechas.etiquetaMes(Fechas.mesAnterior(mes))}", { vm.cargarMes(Fechas.mesAnterior(mes)) }, Modifier.weight(1f))
                BotonPillContorno(Fechas.etiquetaMes(mes), { vm.cargarMes(Fechas.mesActual()) }, Modifier.weight(1f), color = Teal)
            }
        }
        item { DatosEmpleador(ui.empresa!!) { ruc, razon -> vm.guardarDatosEmpleador(ruc, razon) } }
        item {
            Seccion(borde = Borde) {
                Text("SUNAFIL · PLAME", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AzulMarino)
                BotonPill("Generar Reporte de Asistencia (DS 004-2006-TR)", { vm.exportar(context, "PDF") }, enabled = resumen != null, icono = Icons.Default.PictureAsPdf, alto = 56.dp)
                BotonPill("Exportar PLAME / T-Registro (.TXT)", { vm.exportar(context, "JOR") }, enabled = resumen != null, icono = Icons.Default.TextSnippet, alto = 52.dp)
                BotonPill("Exportar Reporte Excel (CSV)", { vm.exportar(context, "CSV") }, enabled = resumen != null, icono = Icons.Default.TableChart, alto = 52.dp)
                if (infoVip.bloqueado) Text("Plan VIP requerido para nuevos marcajes; los reportes del historial siguen disponibles.", style = MaterialTheme.typography.bodySmall)
            }
        }
        val r = resumen
        if (r == null) item { Cargando() }
        else {
            item { TituloSeccion("Reporte de Planilla — ${Fechas.etiquetaMes(mes)}") }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Borde, RoundedCornerShape(16.dp))) {
                    r.forEachIndexed { i, c ->
                        if (i > 0) HorizontalDivider(color = Borde)
                        FilaPlanilla(c, mon)
                    }
                    HorizontalDivider(color = Borde)
                    Column(Modifier.background(TealClaro).padding(12.dp)) {
                        FilaDato("Neto total a pagar", dinero(mon, r.sumOf { it.boleta.netoAPagar }), destacado = true)
                        FilaDato("Costo laboral total", dinero(mon, r.sumOf { it.boleta.costoEmpleador }), destacado = true, colorValor = Teal)
                    }
                }
            }
            if (r.isEmpty()) item { TextoCentrado("Sin colaboradores en el periodo.") }
            val avanzados = r.filter { it.usuario.configPago.modo == ModoPago.AVANZADO }
            if (avanzados.isNotEmpty() || ui.empresa!!.modoPagoGlobal == ModoPago.AVANZADO) item {
                PlanillaAvanzadaSeccion(avanzados, Fechas.etiquetaMes(mes)) { vm.exportar(context, it) }
            }
            item { ReporteCostosSeccion(r, mon, Fechas.etiquetaMes(mes), alExportar = { vm.exportar(context, it) }) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** Descargas PDF del Modo Avanzado Perú: planilla completa, planilla mensual, parámetros legales y boleta individual. */
@Composable
private fun PlanillaAvanzadaSeccion(avanzados: List<ResumenColaborador>, mesEtiqueta: String, alExportar: (String) -> Unit) {
    var elegir by remember { mutableStateOf(false) }
    Seccion(borde = Borde) {
        Text("Planilla Modo Avanzado Perú (PDF)", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = AzulMarino)
        Text(
            "$mesEtiqueta · ${avanzados.size} ${if (avanzados.size == 1) "colaborador" else "colaboradores"} en Modo Avanzado. " +
                "La planilla completa reúne la planilla mensual, los parámetros legales y una boleta por colaborador.",
            style = MaterialTheme.typography.bodySmall, color = TextoSecundario
        )
        BotonPill("Planilla completa (PDF)", { alExportar("AVZ_COMPLETA") }, enabled = avanzados.isNotEmpty(), icono = Icons.Default.PictureAsPdf, alto = 52.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonPillContorno("Planilla mensual", { alExportar("AVZ_PLANILLA") }, Modifier.weight(1f), color = Teal, enabled = avanzados.isNotEmpty())
            BotonPillContorno("Parámetros legales", { alExportar("AVZ_PARAMETROS") }, Modifier.weight(1f), color = Teal)
        }
        Row {
            Box(Modifier.fillMaxWidth()) {
                BotonPillContorno("Boleta individual", { elegir = true }, Modifier.fillMaxWidth(), color = Teal, enabled = avanzados.isNotEmpty())
                DropdownMenu(expanded = elegir, onDismissRequest = { elegir = false }) {
                    avanzados.forEach { c ->
                        DropdownMenuItem(
                            text = { Text("${PlanillaAvanzada.nombreLegal(c.usuario)} · ${c.usuario.dni.ifBlank { "sin documento" }}") },
                            onClick = { elegir = false; alExportar("AVZ_BOLETA:${c.usuario.uid}") }
                        )
                    }
                }
            }
        }
        if (avanzados.isEmpty()) Text("Pasa al menos un colaborador a Modo Avanzado (Equipo → editar) para generar la planilla.",
            style = MaterialTheme.typography.bodySmall, color = Ambar)
        if (avanzados.any { it.usuario.configPago.apellidoPaterno.isBlank() || it.usuario.dni.isBlank() }) Text(
            "Completa apellidos, nombres y documento de cada colaborador (Equipo → editar → Modo Avanzado) para que la planilla y la boleta salgan con sus datos completos.",
            style = MaterialTheme.typography.bodySmall, color = Ambar
        )
    }
}

/** RUC y razón social: obligatorios en la boleta (DS 001-98-TR), el registro de asistencia (DS 004-2006-TR) y el PLAME. */
@Composable
private fun DatosEmpleador(e: com.juanreyna.registrotiempo.domain.model.Empresa, alGuardar: (String, String) -> Unit) {
    var editando by remember { mutableStateOf(false) }
    var ruc by remember(e.ruc) { mutableStateOf(e.ruc) }
    var razon by remember(e.razonSocial) { mutableStateOf(e.razonSocial.ifBlank { e.nombreComercial }) }
    val rucValido = ruc.matches(Regex("(10|15|16|17|20)[0-9]{9}"))
    Seccion("Datos del empleador", borde = if (e.ruc.isBlank()) Ambar else Borde) {
        if (!editando && e.ruc.isNotBlank()) {
            FilaDato("Razón social", e.razonSocial.ifBlank { e.nombreComercial })
            FilaDato("RUC", e.ruc)
            TextButton({ editando = true }) { Text("Editar", color = Teal) }
        } else {
            if (e.ruc.isBlank()) Text(
                "Registra el RUC y la razón social: aparecen en la boleta, el registro de asistencia y el archivo PLAME.",
                style = MaterialTheme.typography.bodySmall, color = TextoSecundario
            )
            OutlinedTextField(razon, { razon = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("Razón social") }, shape = FormaCampo, singleLine = true)
            OutlinedTextField(
                ruc, { ruc = it.filter(Char::isDigit).take(11) }, Modifier.fillMaxWidth(), label = { Text("RUC (11 dígitos)") },
                shape = FormaCampo, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = ruc.isNotEmpty() && !rucValido, supportingText = { Text("${ruc.length}/11") }
            )
            BotonPill("Guardar datos del empleador", { alGuardar(ruc, razon); editando = false }, enabled = rucValido && razon.isNotBlank())
        }
    }
}

/** Una fila del reporte de planilla: nombre y horas arriba; bruto, retenciones y neto debajo. */
@Composable
private fun FilaPlanilla(c: ResumenColaborador, mon: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.usuario.nombre, 36.dp, foto = c.usuario.foto)
            Spacer(Modifier.width(10.dp))
            Text(c.usuario.nombre, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(Fechas.duracion((c.horasTotales * 60).roundToLong()), style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CeldaMonto("Bruto", dinero(mon, c.boleta.totalIngresos), Modifier.weight(1f))
            CeldaMonto("Retenciones", "-" + dinero(mon, c.boleta.totalDescuentos), Modifier.weight(1f), Rojo)
            CeldaMonto("Neto", dinero(mon, c.boleta.netoAPagar), Modifier.weight(1f), AzulMarino, negrita = true)
        }
    }
}

@Composable
internal fun CeldaMonto(etiqueta: String, valor: String, modifier: Modifier, color: Color = Color.Unspecified, negrita: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF3F6FA)).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = TextoSecundario, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = if (negrita) FontWeight.Bold else FontWeight.Medium, maxLines = 1, softWrap = false)
    }
}

