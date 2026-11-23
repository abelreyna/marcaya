package com.juanreyna.registrotiempo.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.ui.components.FormaCampo
import com.juanreyna.registrotiempo.ui.theme.Rojo
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario

private const val PALABRA = "ELIMINAR"

/**
 * El administrador elimina la cuenta de un colaborador: se borra su perfil y, si se marca, también su historial.
 * Exige escribir "ELIMINAR" para confirmar.
 */
@Composable
fun DialogoEliminarColaborador(u: Usuario, alConfirmar: (borrarHistorial: Boolean) -> Unit, alCerrar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    var borrarHistorial by remember { mutableStateOf(false) }
    val listo = texto.trim() == PALABRA
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Eliminar cuenta de ${u.nombre}", color = Rojo, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se borrará de forma permanente su perfil: nombre, DNI, teléfono, foto, tarifa y horario.")
                Text(
                    "Ya no podrá marcar asistencia. Si vuelve a entrar con su cuenta de Google, la app le pedirá registrarse desde cero.",
                    style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(borrarHistorial, { borrarHistorial = it }, colors = CheckboxDefaults.colors(checkedColor = Rojo))
                    Text("Borrar también su historial de marcajes, justificaciones y alertas")
                }
                Text(
                    if (borrarHistorial)
                        "Importante: la ley peruana obliga a conservar los registros de asistencia. Exporta tus reportes antes de borrar su historial."
                    else "Su historial se conserva y seguirá apareciendo en tus reportes.",
                    style = MaterialTheme.typography.bodySmall, color = if (borrarHistorial) Rojo else TextoSecundario
                )
                OutlinedTextField(
                    texto, { texto = it.uppercase().take(12) }, Modifier.fillMaxWidth(),
                    label = { Text("Escribe $PALABRA para confirmar") }, singleLine = true, shape = FormaCampo,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { alConfirmar(borrarHistorial) }, enabled = listo) {
                Text("Eliminar cuenta", color = if (listo) Rojo else TextoSecundario)
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}
