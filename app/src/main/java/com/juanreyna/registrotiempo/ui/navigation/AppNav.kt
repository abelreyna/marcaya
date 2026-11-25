package com.juanreyna.registrotiempo.ui.navigation

import com.juanreyna.registrotiempo.ui.login.SinEmpresaScreen
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.juanreyna.registrotiempo.domain.vip.AccessStatus
import com.juanreyna.registrotiempo.ui.Destino
import com.juanreyna.registrotiempo.ui.EstadoSesion
import com.juanreyna.registrotiempo.ui.SesionViewModel
import com.juanreyna.registrotiempo.ui.admin.AdminScreen
import com.juanreyna.registrotiempo.ui.ajustes.AcercaDeScreen
import com.juanreyna.registrotiempo.ui.ajustes.AjustesScreen
import com.juanreyna.registrotiempo.ui.ajustes.VipScreen
import com.juanreyna.registrotiempo.ui.colaborador.ColaboradorScreen
import com.juanreyna.registrotiempo.ui.colaborador.InfoVip
import com.juanreyna.registrotiempo.ui.components.Cargando
import com.juanreyna.registrotiempo.ui.login.FirebaseSinConfigurarScreen
import com.juanreyna.registrotiempo.ui.login.LoginScreen
import com.juanreyna.registrotiempo.ui.login.RegistroScreen
import com.juanreyna.registrotiempo.ui.perfil.PerfilScreen
import com.juanreyna.registrotiempo.ui.ayuda.GuiaUsoScreen
import com.juanreyna.registrotiempo.ui.ayuda.RecorridoBienvenida
import com.juanreyna.registrotiempo.ui.ayuda.RecorridoVisto
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private fun rutaDe(d: Destino) = when (d) {
    Destino.Cargando -> "cargando"
    Destino.FirebaseSinConfigurar -> "firebase"
    Destino.Login -> "login"
    Destino.Registro -> "registro"
    is Destino.Admin -> "admin/${d.uid}"
    is Destino.Colaborador -> "colaborador/${d.uid}"
    is Destino.SinEmpresa -> "sinempresa"
}

private fun infoVip(e: EstadoSesion) = InfoVip(
    mensaje = when (e.acceso) {
        AccessStatus.EXPIRING_SOON -> if (e.diasRestantes > 0) "Tu periodo gratuito vence en ${e.diasRestantes} días. ${e.vipConfig.promoMessage}" else e.vipConfig.promoMessage
        AccessStatus.BLOCKED_NEED_VIP -> "Periodo gratuito finalizado. Activa VIP para seguir marcando."
        else -> null
    }?.takeIf { !(e.bannerCerrado && e.acceso == AccessStatus.EXPIRING_SOON) },
    bloqueado = e.acceso == AccessStatus.BLOCKED_NEED_VIP,
    cerrable = e.acceso == AccessStatus.EXPIRING_SOON
)

@Composable
fun AppNav(sesion: SesionViewModel = hiltViewModel()) {
    val estado by sesion.estado.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val activity = LocalContext.current as Activity
    val esAdmin = estado.destino is Destino.Admin
    // Recorrido de bienvenida: una sola vez por cuenta, al entrar por primera vez a su pantalla principal.
    val uidPrincipal = when (val d = estado.destino) { is Destino.Admin -> d.uid; is Destino.Colaborador -> d.uid; else -> "" }
    var recorrido by remember { mutableStateOf<Boolean?>(null) } // null = mostrar si está pendiente
    LaunchedEffect(uidPrincipal) { recorrido = null }
    val mostrarRecorrido = uidPrincipal.isNotBlank() && (recorrido ?: RecorridoVisto.pendiente(activity, uidPrincipal))
    if (mostrarRecorrido) {
        val cerrar = { RecorridoVisto.marcar(activity, uidPrincipal); recorrido = false }
        RecorridoBienvenida(esAdmin, alVerGuia = { cerrar(); nav.navigate("guia") }, alCerrar = cerrar)
    }

    // Al cambiar el destino raíz (login, registro, panel) se limpia la pila
    LaunchedEffect(estado.destino) {
        val ruta = rutaDe(estado.destino)
        if (nav.currentDestination?.route?.substringBefore("/") != ruta.substringBefore("/")) {
            nav.navigate(ruta) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(nav, startDestination = "cargando") {
        composable("cargando") { Cargando() }
        composable("firebase") { FirebaseSinConfigurarScreen() }
        composable("login") { LoginScreen(estado) { act, recordarme -> sesion.iniciarConGoogle(act, recordarme) } }
        composable("registro") {
            BackHandler { sesion.cerrarSesion(activity) }
            RegistroScreen(
                estado, sesion.nombreGoogle(),
                alVolver = { sesion.cerrarSesion(activity) },
                alRegistrarAdmin = sesion::registrarAdministrador,
                alRegistrarColaborador = sesion::registrarColaborador
            )
        }
        composable("admin/{uid}", arguments = listOf(navArgument("uid") { type = NavType.StringType })) {
            AdminScreen(infoVip(estado), { nav.navigate("vip") }, sesion::cerrarBanner, { nav.navigate("ajustes") }, alPerfil = { nav.navigate("perfil") })
        }
        composable("colaborador/{uid}", arguments = listOf(navArgument("uid") { type = NavType.StringType })) {
            ColaboradorScreen(
                infoVip(estado), { nav.navigate("vip") }, sesion::cerrarBanner, { nav.navigate("ajustes") }, alPerfil = { nav.navigate("perfil") },
                alQuedarSinEmpresa = { sesion.resolver() }
            )
        }
        composable("sinempresa") {
            SinEmpresaScreen(
                estado,
                alVincular = sesion::revincular,
                alEliminarCuenta = { sesion.eliminarCuenta(activity) },
                alCerrarSesion = { sesion.cerrarSesion(activity) }
            )
        }
        composable("perfil") { PerfilScreen(alVolver = { nav.popBackStack() }) }
        composable("ajustes") {
            AjustesScreen(
                alVolver = { nav.popBackStack() },
                alPerfil = { nav.navigate("perfil") },
                alAcercaDe = { nav.navigate("acerca") },
                alVip = { nav.navigate("vip") },
                alCerrarSesion = { sesion.cerrarSesion(activity) },
                esAdmin = esAdmin,
                estado = estado,
                alEliminarCuenta = { sesion.eliminarCuenta(activity) },
                alGuia = { nav.navigate("guia") }
            )
        }
        composable("guia") { GuiaUsoScreen(esAdmin, alVolver = { nav.popBackStack() }, alVerRecorrido = { recorrido = true }) }
        composable("acerca") { AcercaDeScreen { nav.popBackStack() } }
        composable("vip") {
            VipScreen(estado.vipConfig, estado.diasRestantes, estado.acceso == AccessStatus.BLOCKED_NEED_VIP) { nav.popBackStack() }
        }
    }
}
