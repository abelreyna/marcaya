package com.juanreyna.registrotiempo.ui.perfil

import com.juanreyna.registrotiempo.ui.login.CampoDigitos
import com.juanreyna.registrotiempo.domain.model.Celular
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juanreyna.registrotiempo.domain.model.Rol
import com.juanreyna.registrotiempo.ui.components.Avatar
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.BotonPillContorno
import com.juanreyna.registrotiempo.ui.components.Cargando
import com.juanreyna.registrotiempo.ui.components.ChipSimple
import com.juanreyna.registrotiempo.ui.components.FilaDato
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.components.MarcoPantalla
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.coloresCampo
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import com.juanreyna.registrotiempo.ui.theme.VerdeEsmeralda
import kotlinx.coroutines.launch
import java.io.File

/** Mi Perfil: foto editable, datos personales editables e información básica de la cuenta. */
@Composable
fun PerfilScreen(alVolver: () -> Unit, vm: PerfilViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) { mensaje?.let { snackbar.showSnackbar(it); vm.consumirMensaje() } }

    // Selector de fotos del sistema: no requiere permisos de almacenamiento
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.cambiarFoto(uri)
    }
    fun abrirGaleria() = elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    // Cámara del dispositivo: la foto se escribe en caché (FileProvider), se procesa y se borra
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var elegirOrigen by remember { mutableStateOf(false) }
    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = uriCamara?.let(Uri::parse)
        if (ok && uri != null) vm.cambiarFoto(uri, borrarAlTerminar = true)
    }
    fun lanzarCamara() {
        val archivo = File(context.cacheDir, "fotos").apply { mkdirs() }.let { File(it, "perfil_camara.jpg") }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        uriCamara = uri.toString()
        try { tomarFoto.launch(uri) } catch (e: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar("No se encontró una app de cámara en el dispositivo") }
        }
    }
    val pedirPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) lanzarCamara()
        else scope.launch { snackbar.showSnackbar("Se necesita permiso de cámara para tomar la foto") }
    }
    fun abrirCamara() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) lanzarCamara()
        else pedirPermisoCamara.launch(Manifest.permission.CAMERA)
    }

    if (elegirOrigen) AlertDialog(
        onDismissRequest = { elegirOrigen = false },
        title = { Text("Foto de perfil") },
        text = { Text("¿Quieres tomar una foto ahora o elegir una de tu galería?") },
        confirmButton = { TextButton({ elegirOrigen = false; abrirCamara() }) { Text("Tomar foto") } },
        dismissButton = { TextButton({ elegirOrigen = false; abrirGaleria() }) { Text("Galería") } }
    )

    Box(Modifier.fillMaxSize()) {
        MarcoPantalla("Mi Perfil", subtitulo = "Foto y datos básicos", alVolver = alVolver) {
            val u = ui.usuario
            if (u == null) { Cargando(); return@MarcoPantalla }
            val esAdmin = u.rol == Rol.ADMINISTRADOR
            var nombre by rememberSaveable(u.nombre) { mutableStateOf(u.nombre) }
            var telefono by rememberSaveable(u.telefono) { mutableStateOf(Celular.limpiar(u.telefono)) }
            val hayCambios = nombre.trim() != u.nombre || telefono.trim() != u.telefono

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Foto de perfil (tocarla también permite cambiarla); los botones Cámara / Galería van debajo
                Box(Modifier.padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    Avatar(u.nombre, 128.dp, foto = u.foto, alTocar = { if (!ui.procesandoFoto) elegirOrigen = true })
                    if (ui.procesandoFoto) Box(
                        Modifier.size(128.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(Modifier.size(32.dp), color = Color.White, strokeWidth = 3.dp) }
                }
                Text(u.nombre.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AzulMarino)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ChipSimple(if (esAdmin) "Administrador" else "Colaborador", Teal)
                    if (!esAdmin) ChipSimple(if (u.activo) "Enlace Activo" else "Desvinculado", if (u.activo) VerdeEsmeralda else Rojo)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonPillContorno(
                        "Cámara", ::abrirCamara,
                        Modifier.weight(1f), color = Teal, enabled = !ui.procesandoFoto, icono = Icons.Default.PhotoCamera
                    )
                    BotonPillContorno(
                        "Galería", ::abrirGaleria,
                        Modifier.weight(1f), color = Teal, enabled = !ui.procesandoFoto, icono = Icons.Default.PhotoLibrary
                    )
                }
                if (u.foto.isNotBlank()) TextButton(vm::quitarFoto, enabled = !ui.procesandoFoto) {
                    Icon(Icons.Default.Delete, null, tint = Rojo, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Quitar foto", color = Rojo)
                }

                Seccion("Datos personales", borde = Borde) {
                    OutlinedTextField(
                        nombre, { nombre = it.take(60) }, label = { Text("Nombre completo") }, singleLine = true,
                        shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
                    )
                    CampoDigitos(
                        telefono, { telefono = Celular.limpiar(it) }, "Número de celular", Celular.DIGITOS,
                        error = telefono.isNotEmpty() && !Celular.esValido(telefono) && (telefono.length == Celular.DIGITOS || telefono.first() != '9'),
                        prefijo = "+51 ", ayuda = "Debe empezar con 9", teclado = KeyboardType.Phone
                    )
                    BotonPill(
                        if (ui.guardando) "Guardando…" else "Guardar cambios", { vm.guardarDatos(nombre, telefono) },
                        Modifier.fillMaxWidth(), enabled = hayCambios && nombre.isNotBlank() && (telefono.isEmpty() || Celular.esValido(telefono)) && !ui.guardando, alto = 48.dp
                    )
                }

                Seccion("Información de la cuenta", borde = Borde) {
                    FilaDato("Correo (Google)", u.correo.ifBlank { "—" })
                    HorizontalDivider(color = Borde)
                    FilaDato("Rol", if (esAdmin) "Administrador" else "Colaborador")
                    HorizontalDivider(color = Borde)
                    FilaDato(if (esAdmin) "Empresa" else "Empleador", ui.empresa?.nombreComercial?.ifBlank { null } ?: "—")
                    if (esAdmin) {
                        HorizontalDivider(color = Borde)
                        FilaDato("Código de Administrador", ui.empresa?.codigoAdministrador ?: "—", colorValor = Teal)
                        HorizontalDivider(color = Borde)
                        FilaDato("Plan", ui.empresa?.plan ?: "FREE")
                    } else {
                        HorizontalDivider(color = Borde)
                        FilaDato(u.tipoDocumento.etiqueta, u.dni.ifBlank { "—" })
                        HorizontalDivider(color = Borde)
                        FilaDato("Horario", "${u.horario.entrada} – ${u.horario.salida}")
                        HorizontalDivider(color = Borde)
                        FilaDato("Descanso", "${u.horario.inicioDescanso} – ${u.horario.finDescanso}")
                        HorizontalDivider(color = Borde)
                        FilaDato("Tolerancia", "${u.horario.toleranciaMin} min")
                    }
                }
                Text(
                    if (esAdmin) "El correo y el código de la empresa no se pueden modificar."
                    else "El DNI, el horario y la tarifa solo los puede cambiar tu administrador.",
                    style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }
}
