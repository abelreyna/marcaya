package com.juanreyna.registrotiempo.ui.perfil

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
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

    Box(Modifier.fillMaxSize()) {
        MarcoPantalla("Mi Perfil", subtitulo = "Foto y datos básicos", alVolver = alVolver) {
            val u = ui.usuario
            if (u == null) { Cargando(); return@MarcoPantalla }
            val esAdmin = u.rol == Rol.ADMINISTRADOR
            var nombre by rememberSaveable(u.nombre) { mutableStateOf(u.nombre) }
            var telefono by rememberSaveable(u.telefono) { mutableStateOf(u.telefono) }
            val hayCambios = nombre.trim() != u.nombre || telefono.trim() != u.telefono

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Foto con insignia de cámara para cambiarla
                Box(Modifier.padding(top = 8.dp)) {
                    Avatar(u.nombre, 128.dp, foto = u.foto, alTocar = ::abrirGaleria)
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(40.dp).clip(CircleShape).background(Teal)
                            .border(3.dp, Color.White, CircleShape).clickable(onClick = ::abrirGaleria),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ui.procesandoFoto) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.PhotoCamera, "Cambiar foto", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Text(u.nombre.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AzulMarino)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ChipSimple(if (esAdmin) "Administrador" else "Colaborador", Teal)
                    if (!esAdmin) ChipSimple(if (u.activo) "Enlace Activo" else "Desvinculado", if (u.activo) VerdeEsmeralda else Rojo)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonPillContorno(
                        if (u.foto.isBlank()) "Subir foto" else "Cambiar", ::abrirGaleria,
                        Modifier.weight(1f), color = Teal, enabled = !ui.procesandoFoto, icono = Icons.Default.PhotoLibrary
                    )
                    if (u.foto.isNotBlank()) BotonPillContorno(
                        "Quitar", vm::quitarFoto, Modifier.weight(1f), color = Rojo, enabled = !ui.procesandoFoto, icono = Icons.Default.Delete
                    )
                }

                Seccion("Datos personales", borde = Borde) {
                    OutlinedTextField(
                        nombre, { nombre = it.take(60) }, label = { Text("Nombre completo") }, singleLine = true,
                        shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        telefono, { telefono = it.filter { c -> c.isDigit() || c in "+ " }.take(15) },
                        label = { Text("Teléfono de contacto") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = FormaCampo, colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
                    )
                    BotonPill(
                        if (ui.guardando) "Guardando…" else "Guardar cambios", { vm.guardarDatos(nombre, telefono) },
                        Modifier.fillMaxWidth(), enabled = hayCambios && nombre.isNotBlank() && !ui.guardando, alto = 48.dp
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
                        FilaDato("DNI / CE", u.dni.ifBlank { "—" })
                        HorizontalDivider(color = Borde)
                        FilaDato("Hora de ingreso", u.horaIngresoEsperada)
                    }
                }
                Text(
                    if (esAdmin) "El correo y el código de la empresa no se pueden modificar."
                    else "El DNI, la hora de ingreso y la tarifa solo los puede cambiar tu administrador.",
                    style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }
}
