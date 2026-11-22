package com.juanreyna.registrotiempo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta tomada de las capturas oficiales (docs/screenshots)
val AzulMarino = Color(0xFF13294B)      // cabeceras y fondos de bienvenida
val AzulMarinoClaro = Color(0xFF1E3A66) // degradado de cabecera
val Teal = Color(0xFF1F7A78)            // botones píldora principales
val TealClaro = Color(0xFFDDEFEE)
val VerdeEsmeralda = Color(0xFF0F8B6D)  // estados correctos / "Dentro del área"
val VerdeIngreso = Color(0xFF1E8E50)    // botón Ingreso
val VerdeTinte = Color(0xFFE6F4EA)
val AzulDescanso = Color(0xFF2F5597)    // botón Inicio Descanso
val Rojo = Color(0xFFD64541)            // Salida / fuera de rango
val RojoTinte = Color(0xFFFCE8E6)
val Ambar = Color(0xFFE0A100)
val AmbarTinte = Color(0xFFFFF3D6)
val FondoClaro = Color(0xFFF3F6FA)
val Borde = Color(0xFFD5DCE5)
val TextoSecundario = Color(0xFF5B6575)

private val Esquema = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealClaro,
    onPrimaryContainer = Color(0xFF0B3B3A),
    secondary = AzulMarino,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE4F2),
    onSecondaryContainer = AzulMarino,
    tertiary = VerdeEsmeralda,
    background = FondoClaro,
    onBackground = Color(0xFF1A1F2B),
    surface = Color.White,
    onSurface = Color(0xFF1A1F2B),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = TextoSecundario,
    outline = Borde,
    error = Rojo
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RegistroTiempoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, shapes = Formas, content = content)
}
