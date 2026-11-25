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
    // Rostro solo al iniciar el turno de la mañana (Ingreso) y el de la tarde (Fin Descanso)
    INGRESO("Ingreso", true),
    INICIO_DESCANSO("Inicio Descanso", false),
    FIN_DESCANSO("Fin Descanso", true),
    SALIDA("Salida", false)
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

/** Tipos de incidencia de asistencia detectados automáticamente a partir del horario. */
enum class TipoIncidencia(val etiqueta: String) {
    FALTA("Falta (inasistencia)"),
    TARDANZA("Tardanza en ingreso"),
    DESCANSO_FUERA_HORARIO("Descanso fuera de horario"),
    EXCESO_DESCANSO("Retorno tardío del descanso"),
    SALIDA_ANTICIPADA("Salida anticipada"),
    SALIDA_TARDIA("Salida después del horario"),
    SIN_SALIDA("No marcó salida"),
    FUERA_GEOCERCA("Marcaje fuera de la zona")
}

/** Estado de una incidencia según su justificación. */
enum class EstadoIncidencia(val etiqueta: String) {
    POR_JUSTIFICAR("Por justificar"),
    EN_REVISION("En revisión"),
    JUSTIFICADA("Justificada"),
    NO_AUTORIZADA("No autorizada")
}

/** Motivos de justificación; [aplica] indica a qué incidencias corresponde (vacío = a todas). */
enum class MotivoJustificacion(val etiqueta: String, val aplica: Set<TipoIncidencia> = emptySet()) {
    DESCANSO_MEDICO("Enfermedad / descanso médico", setOf(TipoIncidencia.FALTA, TipoIncidencia.TARDANZA, TipoIncidencia.SALIDA_ANTICIPADA)),
    CITA_MEDICA("Cita médica", setOf(TipoIncidencia.FALTA, TipoIncidencia.TARDANZA, TipoIncidencia.SALIDA_ANTICIPADA, TipoIncidencia.EXCESO_DESCANSO, TipoIncidencia.DESCANSO_FUERA_HORARIO)),
    EMERGENCIA_FAMILIAR("Emergencia familiar", setOf(TipoIncidencia.FALTA, TipoIncidencia.TARDANZA, TipoIncidencia.SALIDA_ANTICIPADA, TipoIncidencia.EXCESO_DESCANSO, TipoIncidencia.DESCANSO_FUERA_HORARIO)),
    DUELO("Fallecimiento de familiar (duelo)", setOf(TipoIncidencia.FALTA, TipoIncidencia.SALIDA_ANTICIPADA)),
    TRANSPORTE("Problemas de transporte / tránsito", setOf(TipoIncidencia.TARDANZA, TipoIncidencia.EXCESO_DESCANSO)),
    PERMISO_AUTORIZADO("Permiso autorizado por el jefe", setOf(TipoIncidencia.FALTA, TipoIncidencia.TARDANZA, TipoIncidencia.SALIDA_ANTICIPADA, TipoIncidencia.EXCESO_DESCANSO, TipoIncidencia.DESCANSO_FUERA_HORARIO, TipoIncidencia.SALIDA_TARDIA)),
    HORAS_EXTRA("Horas extras / trabajo pendiente", setOf(TipoIncidencia.SALIDA_TARDIA, TipoIncidencia.DESCANSO_FUERA_HORARIO)),
    ATENCION_CLIENTE("Atención a cliente o urgencia del trabajo", setOf(TipoIncidencia.SALIDA_TARDIA, TipoIncidencia.DESCANSO_FUERA_HORARIO, TipoIncidencia.EXCESO_DESCANSO)),
    VACACIONES("Vacaciones", setOf(TipoIncidencia.FALTA)),
    LICENCIA("Licencia (maternidad, paternidad u otra)", setOf(TipoIncidencia.FALTA)),
    TRAMITE("Trámite personal / judicial", setOf(TipoIncidencia.FALTA, TipoIncidencia.TARDANZA, TipoIncidencia.SALIDA_ANTICIPADA)),
    COMISION_SERVICIOS("Comisión de servicios", setOf(TipoIncidencia.FALTA, TipoIncidencia.FUERA_GEOCERCA, TipoIncidencia.SALIDA_ANTICIPADA, TipoIncidencia.TARDANZA)),
    TRABAJO_CAMPO("Trabajo de campo", setOf(TipoIncidencia.FUERA_GEOCERCA, TipoIncidencia.FALTA, TipoIncidencia.SIN_SALIDA)),
    CAPACITACION("Capacitación / curso", setOf(TipoIncidencia.FALTA, TipoIncidencia.TARDANZA, TipoIncidencia.SALIDA_ANTICIPADA)),
    OLVIDO_MARCAJE("Olvidé marcar a tiempo", setOf(TipoIncidencia.SIN_SALIDA, TipoIncidencia.SALIDA_TARDIA, TipoIncidencia.DESCANSO_FUERA_HORARIO, TipoIncidencia.EXCESO_DESCANSO)),
    PROBLEMA_TECNICO("Problemas técnicos (app / GPS / celular)"),
    OTRO("Otro");

    companion object {
        fun para(tipo: TipoIncidencia?): List<MotivoJustificacion> =
            if (tipo == null) entries else entries.filter { it.aplica.isEmpty() || tipo in it.aplica }
    }
}

/** Configuración de pago individual del colaborador. */
data class ConfigPago(
    val modo: ModoPago = ModoPago.SIMPLE,
    val tarifaPorHora: Double = 0.0,
    val sueldoBase: Double = 0.0,
    val regimen: RegimenLaboral = RegimenLaboral.GENERAL,
    val asignacionFamiliar: Boolean = false,
    val sistemaPension: SistemaPension = SistemaPension.ONP,
    val comisionMixta: Boolean = false,
    /** Modo Simple: recargo (%) de las 2 primeras horas extra del día y desde la tercera. */
    val recargoExtra1: Double = 25.0,
    val recargoExtra2: Double = 35.0,
    /** Datos de la boleta (Modo Avanzado): cargo, código CUSPP de la AFP y fecha de ingreso "yyyy-MM-dd". */
    val cargo: String = "",
    val cuspp: String = "",
    val fechaIngreso: String = "",
    /**
     * Modo Avanzado: true = remuneración por hora (se paga cada hora trabajada × [tarifaPorHora], con beneficios de ley);
     * false = sueldo mensual fijo ([sueldoBase] por 30 días).
     */
    val pagoPorHora: Boolean = false,
    /** Datos completos para la planilla y la boleta (Modo Avanzado): apellidos y nombres como en el DNI, y centro de costo. */
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val nombres: String = "",
    val centroCosto: String = ""
) {
    /** Valor de una hora ordinaria: tarifa (Simple o Avanzado por hora) o sueldo mensual ÷ 240 h. */
    val valorHora: Double get() = if (modo == ModoPago.SIMPLE || pagoPorHora) tarifaPorHora else sueldoBase / 240.0
}

/** Horas de un día concreto de la semana. Sin descanso: inicio y fin de descanso iguales. */
data class Turno(val entrada: String, val inicioDescanso: String, val finDescanso: String, val salida: String) {
    val tieneDescanso: Boolean get() = inicioDescanso != finDescanso
}

/**
 * Horario de trabajo del colaborador (horas "HH:mm", zona Lima).
 * - [turnos]: horas propias de algunos días (ISO 1 = lunes … 7 = domingo); los demás días usan el horario general.
 * - Ingreso después de [entrada] + [toleranciaMin] → tardanza.
 * - Descanso más largo que el programado + tolerancia → exceso de descanso (no se paga).
 * - Salida después de [salida] + tolerancia → horas extras (desde la hora de salida programada).
 */
data class Horario(
    val entrada: String = "08:00",
    val inicioDescanso: String = "13:00",
    val finDescanso: String = "14:00",
    val salida: String = "17:00",
    val toleranciaMin: Int = 5,
    /** Días laborables (ISO: 1 = lunes … 7 = domingo). Por defecto lunes a sábado. */
    val diasLaborables: List<Int> = listOf(1, 2, 3, 4, 5, 6),
    val turnos: Map<Int, Turno> = emptyMap()
) {
    fun trabajaEl(diaIso: Int) = diaIso in diasLaborables

    val turnoGeneral: Turno get() = Turno(entrada, inicioDescanso, finDescanso, salida)

    /** true si algún día laborable tiene horas propias. */
    val porDia: Boolean get() = turnos.keys.any { it in diasLaborables }

    /** Horario efectivo de un día de la semana: las horas propias de ese día o, si no tiene, las generales. */
    fun delDia(diaIso: Int): Horario = turnos[diaIso]?.let {
        copy(entrada = it.entrada, inicioDescanso = it.inicioDescanso, finDescanso = it.finDescanso, salida = it.salida, turnos = emptyMap())
    } ?: this

    /** Horario efectivo de una fecha "yyyy-MM-dd". */
    fun deFecha(fecha: String): Horario = if (turnos.isEmpty()) this else delDia(com.juanreyna.registrotiempo.util.Fechas.diaSemanaIso(fecha))

    val minutosDescansoProgramado: Int get() = diferencia(inicioDescanso, finDescanso)
    val minutosJornadaProgramada: Int get() = diferencia(entrada, salida) - minutosDescansoProgramado

    private fun diferencia(desde: String, hasta: String): Int {
        val d = minutos(hasta) - minutos(desde)
        return if (d < 0) d + 24 * 60 else d
    }

    companion object {
        private val FORMATO = Regex("([01]\\d|2[0-3]):[0-5]\\d")
        fun esHoraValida(h: String) = FORMATO.matches(h)
        fun minutos(h: String): Int = runCatching { h.split(":").let { it[0].trim().toInt() * 60 + it[1].trim().toInt() } }.getOrDefault(0)
    }
}

data class Usuario(
    val uid: String = "",
    val rol: Rol = Rol.COLABORADOR,
    val adminId: String = "",
    val nombre: String = "",
    val correo: String = "",
    val dni: String = "",
    val tipoDocumento: TipoDocumento = TipoDocumento.DNI,
    val telefono: String = "",
    val activo: Boolean = true,
    val horario: Horario = Horario(),
    val configPago: ConfigPago = ConfigPago(),
    /** Foto de perfil como JPEG 320×320 en Base64 (vacía = iniciales). Se guarda en Firestore: sin Firebase Storage. */
    val foto: String = "",
    /** Fecha de registro: las faltas se cuentan desde ese día. */
    val creadoMillis: Long? = null
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
    val geocerca: Geocerca = Geocerca(),
    /** Datos del empleador exigidos en boletas y registros (DS 001-98-TR, DS 004-2006-TR). */
    val ruc: String = "",
    val razonSocial: String = ""
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
    val validacionFacial: Boolean = false,
    /** Salida registrada manualmente por el empleador (olvido de marcaje). */
    val registradoPorAdmin: Boolean = false
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
    val creadoMillis: Long? = null,
    /** Incidencia que justifica (null en justificaciones antiguas: cubren cualquier incidencia de esa fecha). */
    val incidencia: TipoIncidencia? = null
)

/**
 * Descuento que el empleador aplica a un colaborador en un día concreto (p. ej. adelanto, daño de material, faltante de caja).
 * Se resta del pago de ese día y del neto del mes. El colaborador solo lo ve: no puede modificarlo.
 */
data class Descuento(
    val id: String = "",
    val colaboradorId: String = "",
    val colaboradorNombre: String = "",
    val adminId: String = "",
    /** Nombre corto del descuento ("Adelanto de sueldo"). */
    val concepto: String = "",
    /** Motivo o explicación para el colaborador. */
    val motivo: String = "",
    val monto: Double = 0.0,
    /** Día al que se aplica, "yyyy-MM-dd". */
    val fecha: String = "",
    val mes: String = "",
    val creadoMillis: Long? = null
)

/** Incidencia detectada en un día, con el estado de su justificación. */
data class Incidencia(
    val colaboradorId: String,
    val colaboradorNombre: String,
    val fecha: String,
    val tipo: TipoIncidencia,
    val minutos: Long,
    val detalle: String,
    val estado: EstadoIncidencia,
    val justificacion: Justificacion?
) {
    val clave get() = "$colaboradorId|$fecha|${tipo.name}"
    /** "Falta no autorizada", "Tardanza justificada", etc. */
    val titulo get() = when {
        tipo == TipoIncidencia.FALTA && estado == EstadoIncidencia.NO_AUTORIZADA -> "Falta no autorizada"
        tipo == TipoIncidencia.FALTA && estado == EstadoIncidencia.JUSTIFICADA -> "Falta justificada"
        tipo == TipoIncidencia.FALTA && estado == EstadoIncidencia.POR_JUSTIFICAR -> "Falta injustificada"
        else -> tipo.etiqueta
    }
}
