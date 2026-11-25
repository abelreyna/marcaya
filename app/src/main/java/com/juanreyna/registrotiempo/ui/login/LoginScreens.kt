package com.juanreyna.registrotiempo.ui.login

import com.juanreyna.registrotiempo.ui.components.SelectorDocumento
import com.juanreyna.registrotiempo.domain.model.Celular
import com.juanreyna.registrotiempo.domain.model.TipoDocumento
import android.app.Activity
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juanreyna.registrotiempo.ui.EstadoSesion
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.DegradadoMarino
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.components.Isotipo
import com.juanreyna.registrotiempo.ui.components.MarcoPantalla
import com.juanreyna.registrotiempo.ui.components.Seccion
import com.juanreyna.registrotiempo.ui.components.coloresCampo
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario

/** Pantalla 1 — Bienvenida / acceso único con Google (fondo azul marino, isotipo y botón píldora). */
@Composable
fun LoginScreen(estado: EstadoSesion, alIniciar: (Activity, Boolean) -> Unit) {
    val activity = LocalContext.current as Activity
    var recordarme by rememberSaveable { mutableStateOf(true) }
    Column(
        Modifier.fillMaxSize().background(DegradadoMarino).navigationBarsPadding().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Isotipo(150.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            "Marcaya", color = Color.White, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black
        )
        Text(
            "Marca tu asistencia en un segundo", color = Color(0xFF2EE6A6), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Rostro · GPS · Planilla Perú", color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { alIniciar(activity, recordarme) },
            enabled = !estado.procesando,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Teal, disabledContainerColor = Teal.copy(alpha = 0.6f))
        ) {
            if (estado.procesando) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else {
                Text("G", fontWeight = FontWeight.Black, color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Text("Continuar con Google", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clickable { recordarme = !recordarme }.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mantener sesión iniciada (Recordarme)", Modifier.weight(1f), color = Color.White)
            Switch(
                checked = recordarme, onCheckedChange = { recordarme = it },
                colors = SwitchDefaults.colors(checkedTrackColor = Teal, checkedThumbColor = Color.White)
            )
        }
        estado.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(28.dp))
        Text("© 2026 Juan Abel Reyna Gonzales", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TarjetaRol(titulo: String, descripcion: String, icono: ImageVector, seleccionado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (seleccionado) Color(0xFFE3ECF8) else Color.White),
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) AzulMarino else Color(0xFFD5DCE5))
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icono, null, tint = AzulMarino, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(6.dp))
            Text(titulo, fontWeight = FontWeight.Bold, color = AzulMarino)
            Text(descripcion, style = MaterialTheme.typography.labelSmall, color = TextoSecundario, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Campo(valor: String, alCambiar: (String) -> Unit, etiqueta: String, teclado: KeyboardOptions = KeyboardOptions.Default, obligatorio: Boolean = false) {
    OutlinedTextField(
        valor, alCambiar,
        label = { Text(if (obligatorio) "Obligatorio · $etiqueta" else etiqueta) },
        keyboardOptions = teclado, singleLine = true, shape = FormaCampo,
        colors = if (obligatorio) OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
            unfocusedBorderColor = Rojo.copy(alpha = 0.6f), focusedBorderColor = Rojo, focusedLabelColor = Rojo, unfocusedLabelColor = Rojo
        ) else coloresCampo(),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Campo solo numérico con cantidad exacta de dígitos y contador "n/total". */
@Composable
internal fun CampoDigitos(
    valor: String, alCambiar: (String) -> Unit, etiqueta: String, digitos: Int, error: Boolean,
    prefijo: String? = null, ayuda: String? = null, teclado: KeyboardType = KeyboardType.NumberPassword
) {
    OutlinedTextField(
        valor, alCambiar,
        label = { Text(etiqueta) },
        prefix = prefijo?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado), singleLine = true, shape = FormaCampo,
        colors = coloresCampo(), isError = error,
        supportingText = {
            Row {
                Text(
                    if (error) "Debe tener exactamente $digitos dígitos${ayuda?.let { " · $it" } ?: ""}" else ayuda ?: "$digitos dígitos",
                    Modifier.weight(1f)
                )
                Text("${valor.length}/$digitos", color = if (valor.length == digitos && !error) Teal else TextoSecundario)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/** Pantalla 2 — Crear Cuenta: selección de rol (tarjetas) y datos. */
@Composable
fun RegistroScreen(
    estado: EstadoSesion,
    nombreInicial: String,
    alVolver: () -> Unit,
    alRegistrarAdmin: (String, String) -> Unit,
    alRegistrarColaborador: (String, TipoDocumento, String, String, String) -> Unit
) {
    var esAdmin by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var nombre by rememberSaveable { mutableStateOf(nombreInicial) }
    var empresa by rememberSaveable { mutableStateOf("") }
    var dni by rememberSaveable { mutableStateOf("") }
    var tipoDoc by rememberSaveable { mutableStateOf<TipoDocumento?>(null) }
    var telefono by rememberSaveable { mutableStateOf("") }
    var codigo by rememberSaveable { mutableStateOf("") }

    MarcoPantalla("Crear Cuenta", subtitulo = "Selección de rol y datos", alVolver = alVolver) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaRol("Administrador", "Gestiona equipo, geocercas y planilla", Icons.Default.AssignmentInd, esAdmin == true, Modifier.weight(1f)) { esAdmin = true }
                TarjetaRol("Colaborador", "Marca asistencia y revisa tus pagos", Icons.Default.Engineering, esAdmin == false, Modifier.weight(1f)) { esAdmin = false }
            }
            when (esAdmin) {
                true -> {
                    Campo(nombre, { nombre = it }, "Nombre Completo")
                    Campo(empresa, { empresa = it }, "Razón Social / Nombre comercial")
                    Text("Se generará tu Código de Administrador (ej. ADM-8392-X). Plan inicial: FREE.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    BotonPill("Crear Cuenta", { alRegistrarAdmin(nombre.trim(), empresa.trim()) },
                        enabled = !estado.procesando && nombre.isNotBlank() && empresa.isNotBlank())
                }
                false -> {
                    Campo(nombre, { nombre = it }, "Nombre Completo")
                    SelectorDocumento(tipoDoc, { t -> tipoDoc = t; dni = t.limpiar(dni) })
                    tipoDoc?.let { t ->
                        CampoDigitos(dni, { dni = t.limpiar(it) }, "Número de ${t.etiqueta}", t.digitos,
                            error = dni.isNotEmpty() && dni.length < t.digitos)
                    }
                    CampoDigitos(telefono, { telefono = Celular.limpiar(it) }, "Número de celular", Celular.DIGITOS,
                        error = telefono.isNotEmpty() && !Celular.esValido(telefono) && (telefono.length == Celular.DIGITOS || telefono.first() != '9'),
                        prefijo = "+51 ", ayuda = "Debe empezar con 9", teclado = KeyboardType.Phone)
                    Campo(codigo, { codigo = it.uppercase().take(10) }, "Código de Administrador",
                        KeyboardOptions(capitalization = KeyboardCapitalization.Characters), obligatorio = true)
                    Text("Proporcionado por tu jefe (formato ADM-XXXX-X).", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    BotonPill("Registrarse", { tipoDoc?.let { alRegistrarColaborador(nombre.trim(), it, dni, telefono, codigo.trim()) } },
                        enabled = !estado.procesando && nombre.isNotBlank() && tipoDoc?.esValido(dni) == true && Celular.esValido(telefono) &&
                            codigo.matches(Regex("ADM-\\d{4}-[A-Z]")))
                }
                null -> Text("Elige cómo usarás la aplicación.", color = TextoSecundario)
            }
            if (estado.procesando) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Teal)
            estado.error?.let { Text(it, color = Rojo) }
        }
    }
}

@Composable
fun FirebaseSinConfigurarScreen() {
    Column(
        Modifier.fillMaxSize().background(DegradadoMarino).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Isotipo(110.dp)
        Spacer(Modifier.height(16.dp))
        Seccion("Firebase pendiente de configurar") {
            Text(
                """
                1. Crea un proyecto en console.firebase.google.com (Plan Spark).
                2. Agrega la app Android com.juanreyna.registrotiempo con la huella SHA-1 de tu firma.
                3. Habilita Authentication → Google, Cloud Firestore, Remote Config, Cloud Messaging y App Check.
                4. Descarga google-services.json y cópialo en la carpeta app/.
                5. Publica firestore.rules y vuelve a compilar.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
