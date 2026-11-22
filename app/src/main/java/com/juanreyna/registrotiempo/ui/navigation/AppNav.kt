package com.juanreyna.registrotiempo.ui.navigation

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

private fun rutaDe(d: Destino) = when (d) {
    Destino.Cargando -> "cargando"
    Destino.FirebaseSinConfigurar -> "firebase"
    Destino.Login -> "login"
    Destino.Registro -> "registro"
    is Destino.Admin -> "admin/${d.uid}"
    is Destino.Colaborador -> "colaborador/${d.uid}"
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
            ColaboradorScreen(infoVip(estado), { nav.navigate("vip") }, sesion::cerrarBanner, { nav.navigate("ajustes") }, alPerfil = { nav.navigate("perfil") })
        }
        composable("perfil") { PerfilScreen(alVolver = { nav.popBackStack() }) }
        composable("ajustes") {
            AjustesScreen(
                alVolver = { nav.popBackStack() },
                alPerfil = { nav.navigate("perfil") },
                alAcercaDe = { nav.navigate("acerca") },
                alVip = { nav.navigate("vip") },
                alCerrarSesion = { sesion.cerrarSesion(activity) }
            )
        }
        composable("acerca") { AcercaDeScreen { nav.popBackStack() } }
        composable("vip") {
            VipScreen(estado.vipConfig, estado.diasRestantes, estado.acceso == AccessStatus.BLOCKED_NEED_VIP) { nav.popBackStack() }
        }
    }
}
