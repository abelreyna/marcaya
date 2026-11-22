package com.juanreyna.registrotiempo.ui.admin

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
    vm: AdminViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val admin by vm.admin.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var verNotificaciones by remember { mutableStateOf(false) }
    LaunchedEffect(mensaje) { mensaje?.let { snackbar.showSnackbar(it); vm.consumirMensaje() } }

    val nombre = admin?.nombre?.substringBefore(" ")?.ifBlank { null } ?: "Administrador"
    val titulos = listOf("Hola, $nombre", "Gestión de Colaboradores", "Zonas de Trabajo (Geocercas)", "Justificaciones", "Reportes SUNAFIL / Planilla")
    Box(Modifier.fillMaxSize()) {
        MarcoPantalla(
            titulo = titulos[pestana],
            subtitulo = if (pestana == 0) "(Admin) · ${ui.empresa?.nombreComercial.orEmpty()}" else ui.empresa?.nombreComercial,
            avatar = if (pestana == 0) admin?.nombre ?: "A" else null,
            acciones = {
                IconButton(onClick = { verNotificaciones = true }) {
                    androidx.compose.material3.BadgedBox(badge = { if (ui.alertas.isNotEmpty()) androidx.compose.material3.Badge { Text("${ui.alertas.size}") } }) {
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
                            Triple("Geocerca", Icons.Default.Map, 2), Triple("Incidencias", Icons.Default.Rule, 3),
                            Triple("Reportes", Icons.Default.Assessment, 4)
                        ), pestana, { pestana = it }, insignia = { if (it == 3) ui.pendientes else 0 }
                    )
                }
            }
        ) {
            infoVip.mensaje?.let { BannerVip(it, alVerVip, if (infoVip.cerrable) alCerrarBanner else null) }
            val empresa = ui.empresa
            if (empresa == null) { Cargando(); return@MarcoPantalla }
            when (pestana) {
                0 -> PestanaPanel(ui)
                1 -> PestanaEquipo(vm, ui)
                2 -> PestanaGeocerca(empresa.geocerca) { vm.guardarGeocerca(it) }
                3 -> PestanaIncidencias(vm, ui)
                else -> PestanaReportes(vm, ui, infoVip)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
    if (verNotificaciones) CentroNotificaciones(ui) { verNotificaciones = false }
}

/** Centro de Notificaciones (alertas de ubicación, tardanzas y justificaciones). */
@Composable
private fun CentroNotificaciones(ui: UiAdmin, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Centro de Notificaciones", fontWeight = FontWeight.Bold, color = AzulMarino) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ui.alertas.forEach {
                    TarjetaAviso(Icons.Default.Warning, if (it.tipo == "GPS_FALSO") "¡Ubicación simulada!" else "¡Alerta de Ubicación!",
                        "${Fechas.horaCorta(it.creadoMillis)} · ${it.colaboradorNombre}: ${it.detalle}", Rojo, RojoTinte)
                }
                ui.filas.filter { it.estado == EstadoColaborador.TARDANZA }.forEach {
                    TarjetaAviso(Icons.Default.Notifications, "Faltas o tardanzas",
                        "${it.usuario.nombre} ingresó a las ${Fechas.horaCorta(it.ultimo?.timestampMillis)} (tolerancia superada)", Ambar)
                }
                if (ui.pendientes > 0) TarjetaAviso(Icons.Default.CheckBox, "Justificaciones pendientes", "${ui.pendientes} por revisar en Incidencias")
                if (ui.alertas.isEmpty() && ui.pendientes == 0 && ui.filas.none { it.estado == EstadoColaborador.TARDANZA })
                    Text("Sin notificaciones por ahora.", color = TextoSecundario)
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } }
    )
}

@Composable
private fun PestanaPanel(ui: UiAdmin) {
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
                    Avatar(f.usuario.nombre, 48.dp)
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
            }
        }
        if (ui.filas.isEmpty()) item { TextoCentrado("Aún no hay colaboradores vinculados. Comparte tu código para empezar.") }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun IconCuadrado(icono: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Teal).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icono, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun Contador(titulo: String, valor: Int, fondo: Color, color: Color, modifier: Modifier) {
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
                Avatar(u.nombre, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.nombre, fontWeight = FontWeight.SemiBold)
                    val c = u.configPago
                    Text(
                        if (c.modo == ModoPago.SIMPLE) "Modo Simple · ${dinero(e.moneda.simbolo, c.tarifaPorHora)}/h"
                        else "${c.regimen.etiqueta} · ${c.sistemaPension.etiqueta}",
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
    editando?.let { u ->
        DialogoColaborador(u, e.moneda.simbolo, alCerrar = { editando = null }) { cfg, hora, activo ->
            vm.guardarColaborador(u, cfg, hora, activo); editando = null
        }
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
private fun DialogoColaborador(u: Usuario, mon: String, alCerrar: () -> Unit, alGuardar: (ConfigPago, String, Boolean) -> Unit) {
    var cfg by remember { mutableStateOf(u.configPago) }
    var tarifa by remember { mutableStateOf(if (cfg.tarifaPorHora > 0) cfg.tarifaPorHora.toString() else "") }
    var sueldo by remember { mutableStateOf(if (cfg.sueldoBase > 0) cfg.sueldoBase.toString() else "") }
    var hora by remember { mutableStateOf(u.horaIngresoEsperada) }
    var activo by remember { mutableStateOf(u.activo) }
    AlertDialog(
        onDismissRequest = alCerrar,
        icon = { Avatar(u.nombre, 52.dp) },
        title = { Text(u.nombre, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipFiltro("Simple", cfg.modo == ModoPago.SIMPLE) { cfg = cfg.copy(modo = ModoPago.SIMPLE) }
                    ChipFiltro("Avanzado Perú", cfg.modo == ModoPago.AVANZADO) { cfg = cfg.copy(modo = ModoPago.AVANZADO) }
                }
                if (cfg.modo == ModoPago.SIMPLE) {
                    OutlinedTextField(tarifa, { tarifa = it }, label = { Text("Pago por Hora Ordinaria ($mon)") }, shape = FormaCampo,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                } else {
                    OutlinedTextField(sueldo, { sueldo = it }, label = { Text("Sueldo base mensual ($mon)") }, shape = FormaCampo,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
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
                    if (cfg.sistemaPension != SistemaPension.ONP) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Comisión mixta (sin comisión flujo)", Modifier.weight(1f))
                        Switch(cfg.comisionMixta, { cfg = cfg.copy(comisionMixta = it) }, colors = SwitchDefaults.colors(checkedTrackColor = Teal))
                    }
                }
                OutlinedTextField(hora, { hora = it.take(5) }, label = { Text("Hora de ingreso (HH:mm)") }, shape = FormaCampo)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChipSimple(if (activo) "Enlace Activo" else "Desvinculado", if (activo) VerdeEsmeralda else Rojo)
                    Spacer(Modifier.weight(1f))
                    Switch(activo, { activo = it }, colors = SwitchDefaults.colors(checkedTrackColor = Teal))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                alGuardar(
                    cfg.copy(
                        tarifaPorHora = tarifa.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        sueldoBase = sueldo.replace(',', '.').toDoubleOrNull() ?: 0.0
                    ), hora, activo
                )
            }, enabled = hora.matches(Regex("\\d{2}:\\d{2}"))) { Text("Guardar", color = Teal, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
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
private fun PestanaIncidencias(vm: AdminViewModel, ui: UiAdmin) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Revisa y aprueba las justificaciones enviadas por tu equipo.", Modifier.padding(top = 16.dp), color = TextoSecundario) }
        items(ui.justificaciones, key = { it.id }) { j ->
            Seccion(borde = Borde) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(j.colaboradorNombre, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(j.colaboradorNombre, fontWeight = FontWeight.SemiBold)
                        Text(j.fecha, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                    when (j.estado) {
                        EstadoJustificacion.PENDIENTE -> ChipSimple("Pendiente", Ambar)
                        EstadoJustificacion.APROBADA -> ChipSimple("Aprobada", VerdeEsmeralda)
                        EstadoJustificacion.RECHAZADA -> ChipSimple("Rechazada", Rojo)
                    }
                }
                Text(j.motivo.etiqueta, fontWeight = FontWeight.SemiBold, color = AzulMarino)
                Text(j.detalle, style = MaterialTheme.typography.bodyMedium)
                if (j.estado == EstadoJustificacion.PENDIENTE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonPill("Aprobar", { vm.resolver(j, EstadoJustificacion.APROBADA) }, Modifier.weight(1f), alto = 44.dp)
                        BotonPillContorno("Rechazar", { vm.resolver(j, EstadoJustificacion.RECHAZADA) }, Modifier.weight(1f), color = Rojo)
                    }
                }
            }
        }
        if (ui.justificaciones.isEmpty()) item { TextoCentrado("No hay justificaciones registradas.") }
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
                    FilaTabla(listOf("Empleado", "Horas", "Bruto", "Retenc.", "Neto"), encabezado = true)
                    r.forEachIndexed { i, c ->
                        if (i > 0) HorizontalDivider(color = Borde)
                        FilaTabla(
                            listOf(
                                c.usuario.nombre.substringBefore(" ") + " " + c.usuario.nombre.substringAfter(" ", "").take(1),
                                "%.1f".format(c.horasTotales),
                                "%,.0f".format(c.boleta.totalIngresos),
                                "-%,.0f".format(c.boleta.totalDescuentos),
                                "%,.0f".format(c.boleta.netoAPagar)
                            )
                        )
                    }
                    HorizontalDivider(color = Borde)
                    Column(Modifier.background(TealClaro).padding(12.dp)) {
                        FilaDato("Neto total a pagar", dinero(mon, r.sumOf { it.boleta.netoAPagar }), destacado = true)
                        FilaDato("Costo laboral total", dinero(mon, r.sumOf { it.boleta.costoEmpleador }), destacado = true, colorValor = Teal)
                    }
                }
            }
            if (r.isEmpty()) item { TextoCentrado("Sin colaboradores en el periodo.") }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun FilaTabla(celdas: List<String>, encabezado: Boolean = false) {
    val pesos = listOf(2.2f, 1f, 1.2f, 1.2f, 1.2f)
    Row(
        Modifier.fillMaxWidth().background(if (encabezado) Color(0xFFEEF2F7) else Color.White).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        celdas.forEachIndexed { i, c ->
            Text(
                c, Modifier.weight(pesos[i]), maxLines = 1,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                style = if (encabezado) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (encabezado || i == 4) FontWeight.Bold else FontWeight.Normal,
                color = if (i == 3 && !encabezado) Rojo else Color.Unspecified
            )
        }
    }
}
