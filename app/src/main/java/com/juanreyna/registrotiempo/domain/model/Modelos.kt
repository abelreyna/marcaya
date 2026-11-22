package com.juanreyna.registrotiempo.domain.model

/** Roles soportados por la aplicación. */
enum class Rol { ADMINISTRADOR, COLABORADOR }

/** Modalidad de cálculo salarial seleccionable por el Administrador. */
enum class ModoPago { SIMPLE, AVANZADO }

/** Regímenes laborales soportados en el Modo Avanzado Perú. */
enum class RegimenLaboral(val etiqueta: String) {
    GENERAL("Régimen General"),
    MICROEMPRESA("REMYPE — Microempresa"),
    PEQUENA_EMPRESA("REMYPE — Pequeña Empresa")
}

enum class SistemaPension(val etiqueta: String) {
    ONP("ONP"),
    AFP_INTEGRA("AFP Integra"),
    AFP_PRIMA("AFP Prima"),
    AFP_PROFUTURO("AFP Profuturo"),
    AFP_HABITAT("AFP Hábitat")
}

enum class Moneda(val simbolo: String) { PEN("S/"), USD("$") }

enum class TipoMarcaje(val etiqueta: String, val requiereRostro: Boolean) {
    INGRESO("Ingreso", true),
    INICIO_DESCANSO("Inicio Descanso", false),
    FIN_DESCANSO("Fin Descanso", false),
    SALIDA("Salida", true)
}

enum class EstadoColaborador(val etiqueta: String) {
    SIN_MARCAR("Sin marcar"),
    DENTRO_AREA("Dentro del área"),
    EN_DESCANSO("En descanso"),
    FUERA_RANGO("Fuera de rango"),
    TARDANZA("Tardanza"),
    JORNADA_CERRADA("Jornada cerrada")
}

enum class EstadoJustificacion { PENDIENTE, APROBADA, RECHAZADA }

enum class MotivoJustificacion(val etiqueta: String) {
    COMISION_SERVICIOS("Comisión de servicios"),
    CITA_MEDICA("Cita médica"),
    TRABAJO_CAMPO("Trabajo de campo"),
    PROBLEMA_TECNICO("Problemas técnicos"),
    OTRO("Otro")
}

/** Configuración de pago individual del colaborador. */
data class ConfigPago(
    val modo: ModoPago = ModoPago.SIMPLE,
    val tarifaPorHora: Double = 0.0,
    val sueldoBase: Double = 0.0,
    val regimen: RegimenLaboral = RegimenLaboral.GENERAL,
    val asignacionFamiliar: Boolean = false,
    val sistemaPension: SistemaPension = SistemaPension.ONP,
    val comisionMixta: Boolean = false
)

data class Usuario(
    val uid: String = "",
    val rol: Rol = Rol.COLABORADOR,
    val adminId: String = "",
    val nombre: String = "",
    val correo: String = "",
    val dni: String = "",
    val telefono: String = "",
    val activo: Boolean = true,
    val horaIngresoEsperada: String = "08:00",
    val configPago: ConfigPago = ConfigPago()
)

data class Geocerca(
    val nombre: String = "Oficina Central",
    val latitud: Double = -12.046374,
    val longitud: Double = -77.042793,
    val radioMetros: Double = 100.0
)

data class Empresa(
    val adminId: String = "",
    val nombreComercial: String = "",
    val codigoAdministrador: String = "",
    val plan: String = "FREE",
    val fechaCreacionMillis: Long = 0L,
    val moneda: Moneda = Moneda.PEN,
    val modoPagoGlobal: ModoPago = ModoPago.SIMPLE,
    val geocerca: Geocerca = Geocerca()
)

data class Marcaje(
    val id: String = "",
    val colaboradorId: String = "",
    val colaboradorNombre: String = "",
    val adminId: String = "",
    val tipo: TipoMarcaje = TipoMarcaje.INGRESO,
    val timestampMillis: Long? = null,
    val fecha: String = "",
    val mes: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val distanciaMetros: Double = 0.0,
    val dentroGeocerca: Boolean = false,
    val validacionFacial: Boolean = false
)

data class Alerta(
    val id: String,
    val colaboradorNombre: String,
    val tipo: String,
    val detalle: String,
    val creadoMillis: Long?
)

data class Justificacion(
    val id: String = "",
    val colaboradorId: String = "",
    val colaboradorNombre: String = "",
    val adminId: String = "",
    val motivo: MotivoJustificacion = MotivoJustificacion.OTRO,
    val detalle: String = "",
    val fecha: String = "",
    val estado: EstadoJustificacion = EstadoJustificacion.PENDIENTE,
    val creadoMillis: Long? = null
)
