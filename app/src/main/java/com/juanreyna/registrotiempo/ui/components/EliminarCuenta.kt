package com.juanreyna.registrotiempo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario

private const val PALABRA = "ELIMINAR"

/**
 * Confirmación para eliminar la cuenta: explica qué se borra según el rol y exige escribir "ELIMINAR".
 * Mientras se procesa muestra el avance; al terminar la app vuelve a la pantalla de inicio de sesión.
 */
@Composable
fun DialogoEliminarCuenta(
    esAdmin: Boolean,
    procesando: Boolean,
    progreso: String?,
    error: String?,
    alConfirmar: () -> Unit,
    alCerrar: () -> Unit
) {
    var texto by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!procesando) alCerrar() },
        title = { Text(if (esAdmin) "Eliminar empresa y cuenta" else "Eliminar mi cuenta", color = Rojo, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (esAdmin) {
                    Text("Se borrará de forma permanente:", fontWeight = FontWeight.SemiBold)
                    Text("• Tu empresa, su Código de Administrador y la zona de trabajo\n• Todos los marcajes, justificaciones y alertas del equipo\n• Tu cuenta de administrador")
                    Text("Tus colaboradores quedarán desvinculados (sin perder su cuenta) y podrán unirse a otra empresa.")
                    Text(
                        "Importante: la ley peruana obliga a conservar los registros de asistencia y planillas. Antes de continuar, exporta tus reportes (PDF / Excel) desde la pestaña Reportes.",
                        color = Rojo, style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("Se borrará de forma permanente:", fontWeight = FontWeight.SemiBold)
                    Text("• Tu perfil: nombre, DNI, teléfono y foto\n• Tu acceso a la app con esta cuenta de Google")
                    Text(
                        "Tus marcajes y justificaciones quedan en poder de tu empleador como registro de asistencia, que la ley le obliga a conservar.",
                        style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                    )
                }
                Text("Después podrás volver a registrarte con la misma cuenta de Google, como una cuenta nueva.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                Text("Se te pedirá elegir de nuevo tu cuenta de Google para confirmar.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                OutlinedTextField(
                    texto, { texto = it.uppercase().take(12) }, Modifier.fillMaxWidth(), enabled = !procesando,
                    label = { Text("Escribe $PALABRA para confirmar") }, singleLine = true, shape = FormaCampo,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                if (procesando) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(progreso ?: "Procesando…", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = Rojo, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = alConfirmar, enabled = !procesando && texto.trim() == PALABRA) {
                Text(if (esAdmin) "Eliminar todo" else "Eliminar mi cuenta", color = if (!procesando && texto.trim() == PALABRA) Rojo else TextoSecundario)
            }
        },
        dismissButton = { TextButton(onClick = alCerrar, enabled = !procesando) { Text("Cancelar") } }
    )
}
