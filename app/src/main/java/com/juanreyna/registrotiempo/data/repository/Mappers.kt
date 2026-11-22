package com.juanreyna.registrotiempo.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.Geocerca
import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.domain.model.Justificacion
import com.juanreyna.registrotiempo.domain.model.Marcaje
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.Moneda
import com.juanreyna.registrotiempo.domain.model.MotivoJustificacion
import com.juanreyna.registrotiempo.domain.model.RegimenLaboral
import com.juanreyna.registrotiempo.domain.model.Rol
import com.juanreyna.registrotiempo.domain.model.SistemaPension
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.domain.model.Usuario

/** Mapeo manual Firestore ⇄ dominio (sin reflexión: compatible con R8). */
internal inline fun <reified T : Enum<T>> enumDe(valor: Any?, defecto: T): T =
    (valor as? String)?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: defecto

private fun DocumentSnapshot.d(campo: String) = (get(campo) as? Number)?.toDouble() ?: 0.0
private fun DocumentSnapshot.s(campo: String) = getString(campo) ?: ""

fun ConfigPago.aMapa(): Map<String, Any> = mapOf(
    "modo" to modo.name,
    "tarifaPorHora" to tarifaPorHora,
    "sueldoBase" to sueldoBase,
    "regimen" to regimen.name,
    "asignacionFamiliar" to asignacionFamiliar,
    "sistemaPension" to sistemaPension.name,
    "comisionMixta" to comisionMixta,
    "recargoExtra1" to recargoExtra1,
    "recargoExtra2" to recargoExtra2
)

fun Horario.aMapa(): Map<String, Any> = mapOf(
    "entrada" to entrada, "inicioDescanso" to inicioDescanso,
    "finDescanso" to finDescanso, "salida" to salida, "toleranciaMin" to toleranciaMin
)

/** Lee el horario; los perfiles antiguos solo tenían "horaIngresoEsperada". */
@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.horarioDe(): Horario {
    val m = get("horario") as? Map<String, Any?>
    val d = Horario(entrada = getString("horaIngresoEsperada") ?: "08:00")
    if (m == null) return d
    return Horario(
        entrada = m["entrada"] as? String ?: d.entrada,
        inicioDescanso = m["inicioDescanso"] as? String ?: d.inicioDescanso,
        finDescanso = m["finDescanso"] as? String ?: d.finDescanso,
        salida = m["salida"] as? String ?: d.salida,
        toleranciaMin = (m["toleranciaMin"] as? Number)?.toInt() ?: d.toleranciaMin
    )
}

@Suppress("UNCHECKED_CAST")
private fun configDe(mapa: Any?): ConfigPago {
    val m = mapa as? Map<String, Any?> ?: return ConfigPago()
    return ConfigPago(
        modo = enumDe(m["modo"], ModoPago.SIMPLE),
        tarifaPorHora = (m["tarifaPorHora"] as? Number)?.toDouble() ?: 0.0,
        sueldoBase = (m["sueldoBase"] as? Number)?.toDouble() ?: 0.0,
        regimen = enumDe(m["regimen"], RegimenLaboral.GENERAL),
        asignacionFamiliar = m["asignacionFamiliar"] as? Boolean ?: false,
        sistemaPension = enumDe(m["sistemaPension"], SistemaPension.ONP),
        comisionMixta = m["comisionMixta"] as? Boolean ?: false,
        recargoExtra1 = (m["recargoExtra1"] as? Number)?.toDouble() ?: 25.0,
        recargoExtra2 = (m["recargoExtra2"] as? Number)?.toDouble() ?: 35.0
    )
}

fun DocumentSnapshot.aUsuario() = Usuario(
    uid = id,
    rol = enumDe(get("rol"), Rol.COLABORADOR),
    adminId = s("adminId"),
    nombre = s("nombre"),
    correo = s("correo"),
    dni = s("dni"),
    telefono = s("telefono"),
    activo = getBoolean("activo") ?: true,
    horario = horarioDe(),
    configPago = configDe(get("configPago")),
    foto = s("foto")
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.aEmpresa(): Empresa {
    val g = get("geocerca") as? Map<String, Any?>
    return Empresa(
        adminId = id,
        nombreComercial = s("nombreComercial"),
        codigoAdministrador = s("codigoAdministrador"),
        plan = getString("plan") ?: "FREE",
        fechaCreacionMillis = getTimestamp("fechaCreacion")?.toDate()?.time ?: System.currentTimeMillis(),
        moneda = enumDe(get("moneda"), Moneda.PEN),
        modoPagoGlobal = enumDe(get("modoPagoGlobal"), ModoPago.SIMPLE),
        geocerca = if (g == null) Geocerca() else Geocerca(
            nombre = g["nombre"] as? String ?: "Sede principal",
            latitud = (g["latitud"] as? Number)?.toDouble() ?: 0.0,
            longitud = (g["longitud"] as? Number)?.toDouble() ?: 0.0,
            radioMetros = (g["radioMetros"] as? Number)?.toDouble() ?: 100.0
        )
    )
}

fun Geocerca.aMapa(): Map<String, Any> = mapOf(
    "nombre" to nombre, "latitud" to latitud, "longitud" to longitud, "radioMetros" to radioMetros
)

fun DocumentSnapshot.aMarcaje() = Marcaje(
    id = id,
    colaboradorId = s("colaboradorId"),
    colaboradorNombre = s("colaboradorNombre"),
    adminId = s("adminId"),
    tipo = enumDe(get("tipo"), TipoMarcaje.INGRESO),
    // Con estimación de servidor para marcajes recién escritos aún pendientes de confirmar
    timestampMillis = (getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) as Timestamp?)?.toDate()?.time,
    fecha = s("fecha"),
    mes = s("mes"),
    latitud = d("latitud"),
    longitud = d("longitud"),
    distanciaMetros = d("distanciaMetros"),
    dentroGeocerca = getBoolean("dentroGeocerca") ?: false,
    validacionFacial = getBoolean("validacionFacial") ?: false
)

fun DocumentSnapshot.aJustificacion() = Justificacion(
    id = id,
    colaboradorId = s("colaboradorId"),
    colaboradorNombre = s("colaboradorNombre"),
    adminId = s("adminId"),
    motivo = enumDe(get("motivo"), MotivoJustificacion.OTRO),
    detalle = s("detalle"),
    fecha = s("fecha"),
    estado = enumDe(get("estado"), EstadoJustificacion.PENDIENTE),
    creadoMillis = getTimestamp("creado", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
)
