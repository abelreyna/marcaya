package com.juanreyna.registrotiempo.ui.colaborador

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanreyna.registrotiempo.domain.calc.JornadaCalculator
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.MotivoJustificacion
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.ui.components.BannerVip
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.BotonPillContorno
import com.juanreyna.registrotiempo.ui.components.Cargando
import com.juanreyna.registrotiempo.ui.components.ChipSimple
import com.juanreyna.registrotiempo.ui.components.FilaDato
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.components.InsigniaFecha
import com.juanreyna.registrotiempo.ui.components.MarcoPantalla
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.Semaforo
import com.juanreyna.registrotiempo.ui.components.TarjetaDosMetricas
import com.juanreyna.registrotiempo.ui.components.TextoCentrado
import com.juanreyna.registrotiempo.ui.components.TituloSeccion
import com.juanreyna.registrotiempo.ui.components.coloresCampo
import com.juanreyna.registrotiempo.ui.components.dinero
import com.juanreyna.registrotiempo.ui.theme.Ambar
import com.juanreyna.registrotiempo.ui.theme.AzulDescanso
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.RojoTinte
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TealClaro
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.ui.theme.VerdeEsmeralda
import com.juanreyna.registrotiempo.ui.theme.VerdeIngreso
import com.juanreyna.registrotiempo.util.Fechas
import com.juanreyna.registrotiempo.util.ReportExporter
import com.juanreyna.registrotiempo.util.ResultadoUbicacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InfoVip(val mensaje: String?, val bloqueado: Boolean, val cerrable: Boolean)

@Composable
fun NavInferior(items: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, Int>>, seleccion: Int, alElegir: (Int) -> Unit, insignia: (Int) -> Int = { 0 }) {
    NavigationBar(containerColor = Color.White) {
        items.forEach { (t, i, idx) ->
            NavigationBarItem(
                selected = seleccion == idx, onClick = { alElegir(idx) },
                icon = {
                    if (insignia(idx) > 0) androidx.compose.material3.BadgedBox(badge = { androidx.compose.material3.Badge { Text("${insignia(idx)}") } }) { Icon(i, null) }
                    else Icon(i, null)
                },
                label = { Text(t) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White, indicatorColor = Teal,
                    selectedTextColor = Teal, unselectedIconColor = TextoSecundario, unselectedTextColor = TextoSecundario
                )
            )
        }
    }
}

@Composable
fun ColaboradorScreen(
    infoVip: InfoVip,
    alVerVip: () -> Unit,
    alCerrarBanner: () -> Unit,
    alAjustes: () -> Unit,
    alPerfil: () -> Unit,
    vm: ColaboradorViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var verificando by remember { mutableStateOf<TipoMarcaje?>(null) }

    LaunchedEffect(mensaje) { mensaje?.let { snackbar.showSnackbar(it); vm.consumirMensaje() } }

    verificando?.let { tipo ->
        VerificacionFacial(
            tipo = tipo,
            alVerificar = { verificando = null; vm.marcar(tipo, validacionFacial = true, bloqueadoPorVip = infoVip.bloqueado) },
            alCancelar = { verificando = null }
        )
        return
    }

    val primerNombre = ui.usuario?.nombre?.substringBefore(" ")?.ifBlank { null }
    val titulos = listOf(primerNombre?.let { "Hola, $it" } ?: "Registrar Marcaje", "Mi Historial de Pagos", "Justificar Incidencia")
    Box(Modifier.fillMaxSize()) {
        MarcoPantalla(
            titulo = titulos[pestana],
            subtitulo = if (pestana == 0) Fechas.fechaLarga(ui.ahora).replaceFirstChar { it.uppercase() } else ui.empresa?.nombreComercial,
            avatar = if (pestana == 0) ui.usuario?.nombre ?: "?" else null,
            avatarFoto = ui.usuario?.foto.orEmpty(),
            alTocarAvatar = alPerfil,
            acciones = { IconButton(onClick = alAjustes) { Icon(Icons.Default.Settings, "Ajustes", tint = Color.White) } },
            pie = {
                NavInferior(
                    listOf(Triple("Marcaje", Icons.Default.Fingerprint, 0), Triple("Historial", Icons.Default.History, 1), Triple("Justificar", Icons.Default.Description, 2)),
                    pestana, { pestana = it }
                )
            }
        ) {
            infoVip.mensaje?.let { BannerVip(it, alVerVip, if (infoVip.cerrable) alCerrarBanner else null) }
            if (ui.usuario == null || ui.empresa == null) { Cargando(); return@MarcoPantalla }
            when (pestana) {
                0 -> PestanaMarcaje(vm, ui, infoVip.bloqueado) { verificando = it }
                1 -> PestanaHistorial(vm, ui)
                else -> PestanaJustificar(vm, ui)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
}

@Composable
private fun PestanaMarcaje(vm: ColaboradorViewModel, ui: UiColaborador, bloqueado: Boolean, pedirRostro: (TipoMarcaje) -> Unit) {
    val ubic by vm.ultimaUbicacion.collectAsStateWithLifecycle()
    val procesando by vm.procesando.collectAsStateWithLifecycle()
    var tipoPendiente by remember { mutableStateOf<TipoMarcaje?>(null) }
    val permisoGps = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) {
            vm.verificarUbicacion()
            tipoPendiente?.let { t -> if (t.requiereRostro) pedirRostro(t) else vm.marcar(t, false, bloqueado) }
        }
        tipoPendiente = null
    }
    val pedir = { t: TipoMarcaje? ->
        tipoPendiente = t
        permisoGps.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) { pedir(null) }

    val empresa = ui.empresa!!
    val cfg = ui.usuario!!.configPago
    val resumenHoy = JornadaCalculator.resumir(ui.marcajesHoy, ui.ahora)
    val permitidos = JornadaCalculator.siguientesPermitidos(ui.marcajesHoy.lastOrNull()?.tipo)
    val mon = empresa.moneda.simbolo
    val activo = { t: TipoMarcaje -> !procesando && !bloqueado && t in permitidos }
    val valorHora = if (cfg.modo == ModoPago.SIMPLE) cfg.tarifaPorHora else cfg.sueldoBase / 240.0
    val pagoHoy = resumenHoy.horasEfectivas * valorHora

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Recuadro "Selfie de Validación"
        Box(
            Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(20.dp)).background(AzulMarino),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Selfie de Validación", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Face, null, tint = Color(0xFF8FD3D0), modifier = Modifier.size(64.dp)) }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Se activa al marcar Ingreso y Salida · la imagen no se guarda",
                    color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                )
            }
        }
        if (procesando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Teal)

        BotonPill("Ingreso", { pedir(TipoMarcaje.INGRESO) }, color = VerdeIngreso, enabled = activo(TipoMarcaje.INGRESO), alto = 56.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonPillContorno("Inicio Descanso", { pedir(TipoMarcaje.INICIO_DESCANSO) }, Modifier.weight(1f), color = AzulDescanso, enabled = activo(TipoMarcaje.INICIO_DESCANSO))
            BotonPillContorno("Fin Descanso", { pedir(TipoMarcaje.FIN_DESCANSO) }, Modifier.weight(1f), color = AzulDescanso, enabled = activo(TipoMarcaje.FIN_DESCANSO))
        }
        BotonPillContorno(
            "Salida", { pedir(TipoMarcaje.SALIDA) }, Modifier.fillMaxWidth(), color = Rojo, enabled = activo(TipoMarcaje.SALIDA),
            icono = Icons.AutoMirrored.Filled.Logout, extra = "%d:%02d hrs".format(resumenHoy.minutosEfectivos / 60, resumenHoy.minutosEfectivos % 60)
        )
        if (bloqueado) Text("Marcajes bloqueados hasta activar el plan VIP. Tu historial sigue disponible.", color = Rojo, style = MaterialTheme.typography.bodySmall)

        // Hora y ubicación
        Seccion(borde = Borde) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Fechas.horaCorta(ui.ahora), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AzulMarino)
                    Text("Hora oficial del servidor al marcar", style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
                }
                IconButton(onClick = { pedir(null) }) { Icon(Icons.Default.LocationOn, "Actualizar ubicación", tint = Teal) }
            }
            when (val u = ubic) {
                is ResultadoUbicacion.Valida -> if (u.dentro)
                    Semaforo(VerdeEsmeralda, "Ubicación: ${empresa.geocerca.nombre} (Dentro del rango)", "a ${u.distanciaMetros.toInt()} m del centro · radio ${empresa.geocerca.radioMetros.toInt()} m", Icons.Default.LocationOn)
                else Semaforo(Rojo, "Fuera de rango: ${empresa.geocerca.nombre}", "a ${u.distanciaMetros.toInt()} m del centro · radio ${empresa.geocerca.radioMetros.toInt()} m", Icons.Default.LocationOn)
                ResultadoUbicacion.GpsFalso -> Semaforo(Rojo, "Ubicación simulada detectada", "Desactiva apps de GPS falso para poder marcar", Icons.Default.ErrorOutline)
                ResultadoUbicacion.SinSenal -> Semaforo(Ambar, "Sin señal GPS", "Activa la ubicación del dispositivo", Icons.Default.LocationOn)
                null -> Semaforo(Ambar, "Verificando ubicación…", icono = Icons.Default.LocationOn)
            }
        }

        if (cfg.modo == ModoPago.SIMPLE) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Teal).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Total acumulado hoy:", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(dinero(mon, pagoHoy), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("(sin descuentos, ${dinero(mon, cfg.tarifaPorHora)} por hora)", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
        }
        TarjetaDosMetricas(
            "Horas de Hoy", "%.1fh".format(resumenHoy.horasEfectivas),
            if (cfg.modo == ModoPago.SIMPLE) "Pago del Día" else "Bruto del Día", dinero(mon, pagoHoy)
        )
        ui.resumenMes?.let { r ->
            Seccion(borde = Borde) {
                FilaDato("Acumulado del mes (${"%.1f".format(r.horasTotales)} h)", dinero(mon, r.boleta.netoAPagar), destacado = true, colorValor = Teal)
                if (cfg.modo == ModoPago.AVANZADO) Text("Neto proyectado con descuentos de ley (ver Historial → Boleta).", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
        }
        if (ui.marcajesHoy.isNotEmpty()) {
            TituloSeccion("Marcajes de hoy")
            Seccion(borde = Borde) {
                ui.marcajesHoy.forEach { m ->
                    FilaDato(
                        m.tipo.etiqueta + if (m.validacionFacial && m.tipo.requiereRostro) " · rostro ✓" else "",
                        "${Fechas.horaCorta(m.timestampMillis)} · ${m.distanciaMetros.toInt()} m",
                        colorValor = if (m.dentroGeocerca) Color.Unspecified else Rojo
                    )
                }
            }
        }
    }
}

@Composable
private fun PestanaHistorial(vm: ColaboradorViewModel, ui: UiColaborador) {
    val mes by vm.mesSeleccionado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val r = ui.resumenMes
    val mon = ui.empresa!!.moneda.simbolo
    val descargar = {
        if (r != null) scope.launch {
            val f = withContext(Dispatchers.IO) { ReportExporter.boletaPdf(context, ui.empresa, r, Fechas.etiquetaMes(mes)) }
            ReportExporter.compartir(context, f, "application/pdf")
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.cambiarMes(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior") }
                    Text(Fechas.etiquetaMes(mes), Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { vm.cambiarMes(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente") }
                }
                Spacer(Modifier.width(12.dp))
                FilledIconButton(
                    onClick = { descargar() }, modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AzulMarino)
                ) { Icon(Icons.Default.Download, "Descargar boleta PDF", tint = Color.White) }
            }
        }
        if (r == null) { item { Cargando() }; return@LazyColumn }
        item { TituloSeccion("Días trabajados") }
        items(r.dias.reversed()) { d ->
            val (ds, dn, mm) = Fechas.partesFecha(d.fecha)
            val valorHora = if (r.boleta.modo == ModoPago.SIMPLE) r.usuario.configPago.tarifaPorHora else r.usuario.configPago.sueldoBase / 240.0
            Seccion(borde = Borde) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InsigniaFecha(ds, dn, mm)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("%dh %02dm".format(d.minutosEfectivos / 60, d.minutosEfectivos % 60), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${Fechas.horaCorta(d.ingreso)} - ${Fechas.horaCorta(d.salida)}", style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pago: ${dinero(mon, d.minutosEfectivos / 60.0 * valorHora)}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            when {
                                d.fueraRango -> ChipSimple("Fuera de geocerca", Rojo)
                                d.tardanza -> ChipSimple("Tardanza", Ambar)
                                d.minutosEfectivos < 300 -> ChipSimple("Medio día", TextoSecundario)
                                d.validacionFacial -> ChipSimple("✓ Validado", VerdeEsmeralda)
                            }
                        }
                    }
                }
            }
        }
        if (r.dias.isEmpty()) item { TextoCentrado("Sin registros en este mes.") }
        item {
            val b = r.boleta
            Seccion("Mi Boleta de Pago — ${Fechas.etiquetaMes(mes)}", borde = Borde) {
                Text(
                    if (b.modo == ModoPago.SIMPLE) "Modo Simple (pago directo por hora)" else "Modo Avanzado (Perú) · ${r.usuario.configPago.regimen.etiqueta}",
                    fontWeight = FontWeight.SemiBold
                )
                Text("Ingresos", fontWeight = FontWeight.Bold, color = AzulMarino)
                b.ingresos.forEach { FilaDato(it.concepto, dinero(mon, it.monto)) }
                FilaDato("Total Bruto", dinero(mon, b.totalIngresos), destacado = true)
                if (b.descuentos.isNotEmpty()) {
                    HorizontalDivider(color = Borde)
                    Text("Descuentos", fontWeight = FontWeight.Bold, color = AzulMarino)
                    b.descuentos.forEach { FilaDato(it.concepto, "- ${dinero(mon, it.monto)}", colorValor = Rojo) }
                    FilaDato("Total Descuentos", dinero(mon, b.totalDescuentos), destacado = true)
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TealClaro).padding(14.dp),
                    horizontalArrangement = Arrangement.Center
                ) { Text("Neto a Recibir: ${dinero(mon, b.netoAPagar)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AzulMarino) }
                if (b.aportesEmpleador.isNotEmpty() || b.provisiones.isNotEmpty()) {
                    Text("Aportes Empleador y Beneficios", fontWeight = FontWeight.Bold, color = AzulMarino)
                    (b.aportesEmpleador + b.provisiones).forEach { FilaDato(it.concepto, dinero(mon, it.monto)) }
                }
                BotonPill("Descargar Boleta PDF", { descargar() }, icono = Icons.Default.Download)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                FilaDato("Días trabajados:", "${r.dias.size}", destacado = true)
                FilaDato("Total acumulado estimado:", dinero(mon, r.boleta.netoAPagar), destacado = true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PestanaJustificar(vm: ColaboradorViewModel, ui: UiColaborador) {
    var motivo by rememberSaveable { mutableStateOf(MotivoJustificacion.COMISION_SERVICIOS) }
    var detalle by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf(Fechas.hoy()) }
    var abierto by remember { mutableStateOf(false) }
    val incidencia = ui.resumenMes?.dias?.lastOrNull { it.fueraRango || it.tardanza }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Justifica una falta, tardanza o un marcaje fuera de la geocerca.",
                Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyMedium, color = TextoSecundario
            )
        }
        if (incidencia != null) item {
            Seccion(borde = Rojo.copy(alpha = 0.3f), fondo = RojoTinte) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Rojo)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Tarjeta de la incidencia", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                        Text(
                            (if (incidencia.fueraRango) "Marcaje fuera de geocerca" else "Tardanza en ingreso") + " el ${incidencia.fecha}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Motivo", fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
                    OutlinedTextField(
                        motivo.etiqueta, {}, readOnly = true, shape = FormaCampo, colors = coloresCampo(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                        MotivoJustificacion.entries.forEach {
                            DropdownMenuItem(text = { Text(it.etiqueta) }, onClick = { motivo = it; abierto = false })
                        }
                    }
                }
                OutlinedTextField(fecha, { fecha = it }, label = { Text("Fecha (AAAA-MM-DD)") }, shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth())
                Text("Detalla tu justificación", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(detalle, { detalle = it.take(500) }, placeholder = { Text("Describe lo ocurrido") }, minLines = 3, shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth())
                BotonPill(
                    "Enviar Justificación",
                    { vm.enviarJustificacion(motivo, detalle.trim(), fecha.trim()); detalle = "" },
                    enabled = detalle.isNotBlank() && fecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                )
            }
        }
        if (ui.justificaciones.isNotEmpty()) item { TituloSeccion("Mis justificaciones") }
        items(ui.justificaciones) { j ->
            Seccion(borde = Borde) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(j.motivo.etiqueta, fontWeight = FontWeight.SemiBold)
                        Text(j.fecha, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    }
                    when (j.estado) {
                        EstadoJustificacion.PENDIENTE -> ChipSimple("Pendiente", Ambar)
                        EstadoJustificacion.APROBADA -> ChipSimple("Aprobada", VerdeEsmeralda)
                        EstadoJustificacion.RECHAZADA -> ChipSimple("Rechazada", Rojo)
                    }
                }
                Text(j.detalle, style = MaterialTheme.typography.bodySmall)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

