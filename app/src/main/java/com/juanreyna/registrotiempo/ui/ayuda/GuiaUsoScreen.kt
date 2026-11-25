package com.juanreyna.registrotiempo.ui.ayuda

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.juanreyna.registrotiempo.ui.components.BotonPill
import com.juanreyna.registrotiempo.ui.components.DegradadoMarino
import com.juanreyna.registrotiempo.ui.components.MarcoPantalla
import com.juanreyna.registrotiempo.ui.components.coloresCampo
import com.juanreyna.registrotiempo.ui.theme.AmbarTinte
import com.juanreyna.registrotiempo.ui.theme.AzulMarino
import com.juanreyna.registrotiempo.ui.theme.Borde
import com.juanreyna.registrotiempo.ui.theme.Teal
import com.juanreyna.registrotiempo.ui.theme.TealClaro
import com.juanreyna.registrotiempo.ui.theme.TextoSecundario
import kotlinx.coroutines.launch

/** Guarda por usuario si ya vio el recorrido de bienvenida (se muestra una sola vez). */
object RecorridoVisto {
    private fun prefs(c: Context) = c.getSharedPreferences("recorrido_bienvenida", Context.MODE_PRIVATE)
    fun pendiente(c: Context, uid: String) = uid.isNotBlank() && !prefs(c).getBoolean(uid, false)
    fun marcar(c: Context, uid: String) = prefs(c).edit().putBoolean(uid, true).apply()
}

/** Guía de uso completa del rol: beneficios + capítulos con temas desplegables y búsqueda. */
@Composable
fun GuiaUsoScreen(esAdmin: Boolean, alVolver: () -> Unit, alVerRecorrido: () -> Unit) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var abierto by rememberSaveable { mutableStateOf<String?>(null) }
    val capitulos = ContenidoManual.manual(esAdmin)
        .map { c -> c.copy(temas = c.temas.filter { it.coincide(busqueda) }) }
        .filter { it.temas.isNotEmpty() }
    val buscando = busqueda.isNotBlank()

    MarcoPantalla(
        "Guía de uso",
        subtitulo = if (esAdmin) "Manual del Administrador" else "Manual del Colaborador",
        alVolver = alVolver
    ) {
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    busqueda, { busqueda = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar: horario, descuento, rostro…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (buscando) IconButton({ busqueda = "" }) { Icon(Icons.Default.Close, "Borrar") } },
                    singleLine = true, shape = RoundedCornerShape(28.dp), colors = coloresCampo()
                )
            }
            if (!buscando) {
                item { TarjetaRecorrido(alVerRecorrido) }
                item { Text("¿Qué ganas usando Marcaya?", fontWeight = FontWeight.Bold, color = AzulMarino, style = MaterialTheme.typography.titleMedium) }
                items(ContenidoManual.beneficios(esAdmin)) { b ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(TealClaro).padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(b.icono, null, tint = Teal, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(b.titulo, fontWeight = FontWeight.Bold, color = AzulMarino)
                            Text(b.detalle, style = MaterialTheme.typography.bodySmall, color = AzulMarino)
                        }
                    }
                }
            }
            if (capitulos.isEmpty()) item {
                Text("No encontramos «$busqueda». Prueba con otra palabra.", Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = TextoSecundario)
            }
            capitulos.forEach { cap ->
                item(key = "cap-${cap.titulo}") {
                    Text(cap.titulo, Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold, color = AzulMarino, style = MaterialTheme.typography.titleMedium)
                }
                items(cap.temas, key = { "tema-${it.titulo}" }) { t ->
                    TarjetaTema(t, expandido = buscando || abierto == t.titulo) {
                        abierto = if (abierto == t.titulo) null else t.titulo
                    }
                }
            }
            item {
                Text(
                    "¿Te quedó alguna duda? Escríbenos desde Ajustes → Soporte técnico.",
                    Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                )
            }
        }
    }
}

@Composable
private fun TarjetaRecorrido(alVer: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DegradadoMarino).clickable(onClick = alVer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color(0xFF2EE6A6), modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Recorrido rápido", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Lo esencial en 5 pasos, en menos de un minuto.", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
        }
        Text("Ver", color = Color(0xFF2EE6A6), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TarjetaTema(t: Tema, expandido: Boolean, alTocar: () -> Unit) {
    val giro by animateFloatAsState(if (expandido) 180f else 0f, label = "giro")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
    ) {
        Row(Modifier.fillMaxWidth().clickable(onClick = alTocar).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(TealClaro), contentAlignment = Alignment.Center) {
                Icon(t.icono, null, tint = Teal, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.titulo, fontWeight = FontWeight.SemiBold, color = AzulMarino)
                Text(t.resumen, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
            Icon(Icons.Default.ExpandMore, null, tint = TextoSecundario, modifier = Modifier.rotate(giro))
        }
        AnimatedVisibility(expandido) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Borde))
                t.pasos.forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Teal), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(p, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                t.consejos.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AmbarTinte).padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFB07D00), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(c, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5C4300))
                    }
                }
            }
        }
    }
}

/** Recorrido de bienvenida a pantalla completa (deslizable). */
@Composable
fun RecorridoBienvenida(esAdmin: Boolean, alVerGuia: () -> Unit, alCerrar: () -> Unit) {
    val pasos = ContenidoManual.recorrido(esAdmin)
    val pager = rememberPagerState { pasos.size }
    val scope = rememberCoroutineScope()
    val ultimo = pager.currentPage == pasos.lastIndex
    Dialog(alCerrar, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier.fillMaxSize().background(DegradadoMarino).statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (esAdmin) "Bienvenido, Administrador" else "Bienvenido a Marcaya",
                    Modifier.weight(1f), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold
                )
                TextButton(alCerrar) { Text("Omitir", color = Color.White) }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
                val p = pasos[i]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(p.icono, null, tint = Color(0xFF2EE6A6), modifier = Modifier.size(64.dp))
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(p.titulo, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(p.texto, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pasos.size) { i ->
                    Box(
                        Modifier.height(8.dp).width(if (i == pager.currentPage) 24.dp else 8.dp).clip(CircleShape)
                            .background(if (i == pager.currentPage) Color(0xFF2EE6A6) else Color.White.copy(alpha = 0.35f))
                    )
                }
            }
            if (ultimo) {
                BotonPill("Ver guía completa", alVerGuia, icono = Icons.AutoMirrored.Filled.MenuBook)
                Spacer(Modifier.height(8.dp))
                TextButton(alCerrar, Modifier.fillMaxWidth()) { Text("Empezar a usar Marcaya", color = Color.White, fontWeight = FontWeight.SemiBold) }
            } else {
                BotonPill("Siguiente", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } })
                Spacer(Modifier.height(56.dp))
            }
        }
    }
}
