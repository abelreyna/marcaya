package com.juanreyna.registrotiempo.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.juanreyna.registrotiempo.ui.EstadoSesion
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.BotonPillContorno
import com.juanreyna.registrotiempo.ui.components.DialogoEliminarCuenta
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.components.MarcoPantalla
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.TarjetaAviso
import com.juanreyna.registrotiempo.ui.components.coloresCampo
import com.juanreyna.registrotiempo.ui.theme.Ambar
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario

/** Colaborador desvinculado de su empresa: puede unirse a otra con un nuevo código o eliminar su cuenta. */
@Composable
fun SinEmpresaScreen(estado: EstadoSesion, alVincular: (String) -> Unit, alEliminarCuenta: () -> Unit, alCerrarSesion: () -> Unit) {
    var codigo by rememberSaveable { mutableStateOf("") }
    var eliminar by remember { mutableStateOf(false) }
    MarcoPantalla("Sin empresa vinculada") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaAviso(
                Icons.Default.LinkOff, "Ya no perteneces a un equipo",
                "Tu empleador te desvinculó o eliminó su empresa. Tu cuenta sigue activa: únete a otra empresa con su Código de Administrador.", Ambar
            )
            Seccion("Unirme a una empresa") {
                OutlinedTextField(
                    codigo, { codigo = it.uppercase().take(10) }, Modifier.fillMaxWidth(),
                    label = { Text("Código de Administrador (ADM-0000-X)") }, singleLine = true, shape = FormaCampo, colors = coloresCampo(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                BotonPill("Vincularme", { alVincular(codigo.trim()) }, enabled = !estado.procesando && codigo.matches(Regex("ADM-\\d{4}-[A-Z]")))
                if (!eliminar) estado.error?.let { Text(it, color = Rojo, style = MaterialTheme.typography.bodySmall) }
            }
            BotonPillContorno("Cerrar sesión", alCerrarSesion, Modifier.fillMaxWidth(), color = AzulMarino)
            BotonPillContorno("Eliminar mi cuenta", { eliminar = true }, Modifier.fillMaxWidth(), color = Rojo)
            Text("Al eliminar tu cuenta se borra tu perfil. Podrás volver a registrarte cuando quieras.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
    }
    if (eliminar) DialogoEliminarCuenta(
        esAdmin = false, procesando = estado.procesando, progreso = estado.progreso, error = estado.error,
        alConfirmar = alEliminarCuenta, alCerrar = { eliminar = false }
    )
}
