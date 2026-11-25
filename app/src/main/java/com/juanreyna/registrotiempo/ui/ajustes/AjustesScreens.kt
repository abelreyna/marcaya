package com.juanreyna.registrotiempo.ui.ajustes

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juanreyna.registrotiempo.BuildConfig
import com.juanreyna.registrotiempo.domain.vip.VipConfig
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.BotonPillContorno
import com.juanreyna.registrotiempo.ui.components.DegradadoMarino
import com.juanreyna.registrotiempo.ui.components.FilaDato
import com.juanreyna.registrotiempo.ui.components.Isotipo
import com.juanreyna.registrotiempo.ui.components.MarcoPantalla
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.theme.Ambar
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario

private const val CORREO_AUTOR = "abel.reynago@gmail.com"
/** WhatsApp del propietario en formato internacional (Perú +51), sin "+" ni espacios, como lo exige wa.me. */
private const val WHATSAPP_PROPIETARIO = "51943402706"
private const val URL_REPOSITORIO = "https://github.com/abelreyna/Registro_Personal"

@Composable
private fun Grupo(titulo: String, contenido: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(titulo, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = AzulMarino, style = MaterialTheme.typography.titleMedium)
        Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)) { contenido() }
    }
}

@Composable
private fun Opcion(icono: ImageVector, texto: String, detalle: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = AzulMarino, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(texto, fontWeight = FontWeight.Medium)
            if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
    }
}

@Composable
fun AjustesScreen(
    alVolver: () -> Unit, alPerfil: () -> Unit, alAcercaDe: () -> Unit, alVip: () -> Unit, alCerrarSesion: () -> Unit,
    esAdmin: Boolean = false,
    estado: com.juanreyna.registrotiempo.ui.EstadoSesion = com.juanreyna.registrotiempo.ui.EstadoSesion(),
    alEliminarCuenta: () -> Unit = {}
) {
    var eliminar by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var confirmar by remember { mutableStateOf(false) }
    var terminos by remember { mutableStateOf(false) }
    MarcoPantalla("Ajustes de la Aplicación", alVolver = alVolver) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            Grupo("Mi Cuenta") {
                Opcion(Icons.Default.AccountCircle, "Mi Perfil", "Foto, nombre, teléfono e información de la cuenta", alPerfil)
                HorizontalDivider(color = Borde)
                Opcion(Icons.Default.Security, "Cuenta de Google vinculada", "Sesión persistente cifrada con Android Keystore") {}
                HorizontalDivider(color = Borde)
                Opcion(Icons.Default.WorkspacePremium, "Plan VIP", "Beneficios y activación de licencia", alVip)
            }
            Grupo("Notificaciones") {
                Opcion(Icons.Default.NotificationsActive, "Alertas de entrada/salida y geocerca", "Gestionar permisos de notificación") {
                    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(i) }
                }
            }
            Grupo("Ayuda y Soporte") {
                Opcion(Icons.Default.Description, "Términos y condiciones") { terminos = true }
                HorizontalDivider(color = Borde)
                Opcion(Icons.AutoMirrored.Filled.HelpOutline, "Soporte técnico", CORREO_AUTOR) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CORREO_AUTOR"))) }
                }
                HorizontalDivider(color = Borde)
                Opcion(Icons.Default.Info, "Acerca de", "Versión ${BuildConfig.VERSION_NAME} · Autoría y contacto", alAcercaDe)
            }
            Spacer(Modifier.height(24.dp))
            BotonPill("Cerrar Sesión", { confirmar = true }, Modifier.padding(horizontal = 16.dp), color = Rojo, icono = Icons.AutoMirrored.Filled.Logout)
            Spacer(Modifier.height(12.dp))
            com.juanreyna.registrotiempo.ui.components.BotonPillContorno(
                if (esAdmin) "Eliminar empresa y cuenta" else "Eliminar mi cuenta", { eliminar = true },
                Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = Rojo
            )
            Text(
                if (esAdmin) "Borra tu empresa y todos sus datos. Tus colaboradores quedan desvinculados."
                else "Borra tu perfil. Tus marcajes quedan con tu empleador como registro de asistencia.",
                Modifier.padding(horizontal = 24.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall,
                color = com.juanreyna.registrotiempo.ui.theme.TextoSecundario
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false },
        title = { Text("¿Cerrar sesión?") },
        text = { Text("La app dejará de recordar tu cuenta en este dispositivo.") },
        confirmButton = { TextButton(onClick = { confirmar = false; alCerrarSesion() }) { Text("Cerrar sesión", color = Rojo) } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
    )
    if (terminos) DialogoLicencia { terminos = false }
    if (eliminar) com.juanreyna.registrotiempo.ui.components.DialogoEliminarCuenta(
        esAdmin = esAdmin, procesando = estado.procesando, progreso = estado.progreso, error = estado.error,
        alConfirmar = alEliminarCuenta, alCerrar = { eliminar = false }
    )
}

@Composable
private fun DialogoLicencia(alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Licencia y Términos") },
        text = {
            Text(
                "Software propietario. © 2026 Juan Abel Reyna Gonzales. Todos los derechos reservados.\n\n" +
                    "La validación facial se realiza en el dispositivo y ninguna imagen se almacena ni se transmite. " +
                    "Los cálculos de planilla son referenciales y deben contrastarse con la normativa laboral y tributaria vigente en el Perú."
            )
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Entendido") } }
    )
}

/** Pantalla 20 — Acerca de (fondo azul marino con isotipo y tarjeta de autor). */
@Composable
fun AcercaDeScreen(alVolver: () -> Unit) {
    val context = LocalContext.current
    var licencia by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(DegradadoMarino).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
            Text("Acerca de", Modifier.align(Alignment.CenterVertically), color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Isotipo(150.dp)
        Text("Marcaya", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text("Marca tu asistencia en un segundo", color = Color(0xFF2EE6A6), fontWeight = FontWeight.SemiBold)
        Text("Información de la app · Versión ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.75f))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Teal).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Autor y Desarrollador", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
            Text("Juan Abel Reyna Gonzales", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Ingeniero Industrial", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(CORREO_AUTOR, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Sistema integral de control de asistencia con validación biométrica facial instantánea, geocercas GPS y cálculo salarial (Modo Simple MYPE y Modo Avanzado Perú).",
            color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium
        )
        BotonPill("Contactar por Correo", {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CORREO_AUTOR")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "Marcaya — Consulta")
            }
            runCatching { context.startActivity(i) }
        }, icono = Icons.Default.Email)
        BotonPillContorno("Repositorio en GitHub", {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_REPOSITORIO))) }
        }, Modifier.fillMaxWidth(), color = Color.White, icono = Icons.Default.Code)
        BotonPillContorno("Licencia y Términos", { licencia = true }, Modifier.fillMaxWidth(), color = Color.White, icono = Icons.Default.Gavel)
        Text("© 2026 Juan Abel Reyna Gonzales - Todos los derechos reservados", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
    }
    if (licencia) DialogoLicencia { licencia = false }
}

@Composable
fun VipScreen(config: VipConfig, diasRestantes: Long, bloqueado: Boolean, alVolver: () -> Unit) {
    val context = LocalContext.current
    MarcoPantalla("Plan VIP", alVolver = alVolver) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.WorkspacePremium, null, tint = Ambar, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally))
            Text(config.promoMessage, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            if (bloqueado) Text("Tu periodo gratuito terminó. Los nuevos marcajes están bloqueados; el historial sigue disponible.", color = Rojo)
            else if (config.isGateEnabled) Text("Días gratuitos restantes: ${diasRestantes.coerceAtLeast(0)}")
            Seccion("Beneficios VIP", borde = Borde) {
                listOf("Seguridad reforzada y prioridad de servidores", "Modo Avanzado Perú (planilla completa)", "Exportación SUNAFIL / PLAME ilimitada", "Soporte directo del desarrollador")
                    .forEach { Text("✓ $it") }
            }
            Seccion("Precios", borde = Borde) {
                FilaDato("Mensual", config.monthlyPrice, destacado = true, colorValor = Teal)
                FilaDato("Anual", config.annualPrice, destacado = true, colorValor = Teal)
            }
            Text("Activación sin comisiones: escríbenos, confirma tu pago y activaremos tu licencia al instante.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            BotonPill("Solicitar por correo", {
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CORREO_AUTOR")).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "Activación Plan VIP — Marcaya")
                }
                runCatching { context.startActivity(i) }
            }, icono = Icons.Default.Email)
            BotonPillContorno("Solicitar por WhatsApp", {
                val texto = "Hola, deseo activar el Plan VIP de Marcaya."
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WHATSAPP_PROPIETARIO?text=" + Uri.encode(texto)))) }
            }, Modifier.fillMaxWidth(), color = Teal)
        }
    }
}
