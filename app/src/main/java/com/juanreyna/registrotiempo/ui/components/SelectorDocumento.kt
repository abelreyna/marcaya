package com.juanreyna.registrotiempo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.juanreyna.registrotiempo.domain.model.TipoDocumento
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario

/** Lista desplegable para elegir el tipo de documento: DNI o Carnet de Extranjería. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDocumento(tipo: TipoDocumento?, alCambiar: (TipoDocumento) -> Unit, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            tipo?.nombreCorto ?: "Selecciona tu documento", {}, readOnly = true, singleLine = true,
            label = { Text("Tipo de documento") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
            shape = FormaCampo, colors = coloresCampo(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            TipoDocumento.entries.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(t.nombreLargo, fontWeight = if (t == tipo) FontWeight.Bold else FontWeight.Normal)
                            Text("${t.etiqueta} · ${t.digitos} dígitos", color = TextoSecundario)
                        }
                    },
                    onClick = { alCambiar(t); abierto = false }
                )
            }
        }
    }
}
