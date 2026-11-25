package com.juanreyna.registrotiempo.ui.ayuda

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PunchClock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

/** Un tema del manual: qué es, pasos para usarlo y consejos. */
data class Tema(
    val titulo: String,
    val icono: ImageVector,
    val resumen: String,
    val pasos: List<String>,
    val consejos: List<String> = emptyList()
) {
    fun coincide(q: String): Boolean {
        val t = q.trim().lowercase()
        return t.isEmpty() || (listOf(titulo, resumen) + pasos + consejos).any { it.lowercase().contains(t) }
    }
}

data class Capitulo(val titulo: String, val temas: List<Tema>)

/** Beneficio de usar la app, para la portada de la guía. */
data class Beneficio(val icono: ImageVector, val titulo: String, val detalle: String)

/** Pantalla del recorrido de bienvenida. */
data class PasoRecorrido(val icono: ImageVector, val titulo: String, val texto: String)

object ContenidoManual {

    // ---------------------------------------------------------------- Administrador

    val beneficiosAdmin = listOf(
        Beneficio(Icons.Default.Verified, "Asistencia que no se puede falsear",
            "Cada marca lleva rostro en vivo, GPS real dentro de tu zona y la hora del servidor de Google. Se acabaron el «marca por mí», el GPS falso y el reloj adelantado."),
        Beneficio(Icons.Default.Payments, "Planilla en minutos",
            "Las horas, tardanzas, extras y descuentos se calculan solos. Pagas exactamente lo trabajado, sin hojas de cálculo ni errores de digitación."),
        Beneficio(Icons.Default.Dashboard, "Control en tiempo real",
            "Ves quién está trabajando, en descanso o fuera de la zona en este momento, y cuánto te cuesta el día, desde cualquier lugar."),
        Beneficio(Icons.Default.Description, "Listo para SUNAFIL",
            "Registro de asistencia (DS 004-2006-TR), PLAME, boletas y planilla en PDF con la normativa peruana vigente: evitas multas por no llevar el registro."),
        Beneficio(Icons.Default.Groups, "Menos discusiones, más confianza",
            "Tus trabajadores ven sus mismas horas y pagos. Las justificaciones y descuentos quedan registrados con motivo y fecha.")
    )

    val recorridoAdmin = listOf(
        PasoRecorrido(Icons.Default.Share, "1. Comparte tu código",
            "En el Panel está tu Código de Administrador (ej. ADM-1234-X). Envíalo por WhatsApp: cada trabajador lo ingresa al registrarse y queda en tu equipo con tu horario."),
        PasoRecorrido(Icons.Default.Map, "2. Define tu zona de trabajo",
            "En Zonas fija el punto de tu local u obra y el radio en metros. Solo se podrá marcar dentro de esa zona."),
        PasoRecorrido(Icons.Default.Groups, "3. Configura cómo pagas",
            "En Equipo elige Modo Simple (pago por hora) o Modo Avanzado Perú (planilla con beneficios) y la tarifa o sueldo de cada trabajador."),
        PasoRecorrido(Icons.Default.Dashboard, "4. Controla en vivo",
            "El Panel muestra quién está trabajando ahora, el costo del día y las alertas. La campana te avisa de tardanzas, faltas y justificaciones."),
        PasoRecorrido(Icons.Default.Assessment, "5. Reportes y planilla",
            "En Reportes descargas la asistencia para SUNAFIL, costos, PLAME y boletas. Toda esta guía está en Ajustes → Guía de uso.")
    )

    val manualAdmin = listOf(
        Capitulo("Primeros pasos", listOf(
            Tema("Crear tu empresa", Icons.Default.Badge,
                "Tu cuenta de administrador crea la empresa y su código único para vincular trabajadores.",
                listOf(
                    "Abre Marcaya y toca «Continuar con Google». Deja activado «Mantener sesión iniciada» para no volver a ingresar.",
                    "Elige la tarjeta «Administrador».",
                    "Escribe tu nombre y la razón social o nombre comercial.",
                    "Revisa el «Horario de trabajo de tus colaboradores»: días, entrada, salida, descanso y tolerancia. Es el horario que recibirá cada trabajador nuevo.",
                    "Toca «Crear Cuenta». Se genera tu Código de Administrador y la empresa empieza en plan FREE."
                ),
                listOf("Podrás cambiar el horario predeterminado cuando quieras en Horarios.")),
            Tema("Invitar a tu equipo", Icons.Default.Share,
                "Cada trabajador se une con tu Código de Administrador.",
                listOf(
                    "En el Panel ubica «Código para Colaboradores».",
                    "Usa el botón de copiar o el de compartir para enviarlo por WhatsApp o correo.",
                    "El trabajador instala Marcaya, entra con su cuenta de Google, elige «Colaborador» y escribe el código con su DNI y celular.",
                    "Aparecerá en tu pestaña Equipo con el horario predeterminado ya asignado."
                ),
                listOf("No compartas el código en grupos públicos: cualquiera que lo tenga puede unirse a tu equipo.")),
            Tema("Configurar la zona de trabajo (geocerca)", Icons.Default.Map,
                "Define dónde se puede marcar asistencia.",
                listOf(
                    "Ve a la pestaña «Zonas».",
                    "Mueve el mapa o usa tu ubicación actual para fijar el centro del local u obra.",
                    "Elige el radio de cobertura (por ejemplo 50, 100 o 150 metros).",
                    "Escribe el nombre de la sede y toca «Guardar Zona»."
                ),
                listOf(
                    "Un radio muy pequeño puede dejar fuera a quien marca en la puerta; 50–100 m suele funcionar bien.",
                    "Si alguien marca fuera de la zona, la marca se guarda igual pero recibes una alerta y se pide justificación."
                ))
        )),
        Capitulo("Día a día", listOf(
            Tema("Panel en vivo", Icons.Default.Dashboard,
                "Lo que pasa hoy en tu empresa, en tiempo real.",
                listOf(
                    "Los contadores muestran cuántos están activos, en descanso, fuera de rango y con falta.",
                    "«Actividad en Vivo» lista a cada trabajador con su estado, última marca, distancia y horas del día.",
                    "La barra inferior muestra el costo estimado de hoy y las horas totales del equipo.",
                    "Si alguien olvidó marcar su salida, toca «Marcar hora de salida» en su tarjeta y elige la hora real."
                ),
                listOf("La salida que registras queda marcada como «registrada por el empleador» para transparencia.")),
            Tema("Notificaciones", Icons.Default.Notifications,
                "La campana reúne todo lo que necesita tu atención.",
                listOf(
                    "El número rojo indica cuántas notificaciones tienes sin leer.",
                    "Recibes avisos de GPS falso, marcas fuera de la zona, tardanzas, faltas, trabajadores que aún no marcan ingreso y justificaciones nuevas.",
                    "Toca la X para descartar una, o «Marcar como leídas» para limpiar todas.",
                    "«Ver incidencias» te lleva directo a revisar justificaciones."
                ),
                listOf("Activa las notificaciones del sistema en Ajustes → Alertas de entrada/salida y geocerca.")),
            Tema("Incidencias y justificaciones", Icons.Default.Rule,
                "Faltas, tardanzas y salidas anticipadas detectadas automáticamente según el horario.",
                listOf(
                    "Abre la pestaña «Incid.». El número en el ícono indica lo pendiente.",
                    "«Por revisar» son justificaciones enviadas por tus trabajadores: lee el motivo y el detalle.",
                    "Toca «Aprobar» para que no se descuente, o «No autorizar» para mantener la incidencia.",
                    "En «No marcó salida» puedes registrar la hora en que salió ese día."
                ),
                listOf("Una falta o tardanza aprobada deja de descontarse en el Modo Avanzado.")),
            Tema("Aplicar un descuento", Icons.Default.MoneyOff,
                "Adelantos, faltantes de caja, daños de material, préstamos, etc.",
                listOf(
                    "Ve a Equipo → «Descuentos al personal» → «Aplicar descuento».",
                    "Busca al trabajador por nombre o DNI y selecciónalo.",
                    "Escribe el nombre del descuento (o elige una sugerencia) y el motivo.",
                    "Indica el monto y el día al que se aplica; revisa el resumen y toca «Aplicar descuento»."
                ),
                listOf(
                    "El trabajador recibe una notificación y el monto se resta de su pago de ese día y de su boleta.",
                    "Si te equivocaste, toca el ícono de borrar junto al descuento para anularlo."
                ))
        )),
        Capitulo("Equipo, horarios y pagos", listOf(
            Tema("Modo Simple o Modo Avanzado", Icons.Default.Payments,
                "Elige cómo se calcula el pago de tu equipo.",
                listOf(
                    "En Equipo → «Modo de Cálculo» elige Modo Simple (pago directo por hora, ideal para MYPE) o Modo Avanzado (planilla y leyes de Perú).",
                    "Elige la moneda (Soles o Dólares).",
                    "Toca «Aplicar a todo el equipo». Las tarifas individuales se conservan."
                ),
                listOf(
                    "Modo Simple: horas trabajadas × tarifa, sin descuentos de ley. Las horas extras se pagan a la misma tarifa.",
                    "Modo Avanzado: ONP/AFP, renta de 5.ª, EsSalud, CTS, gratificaciones, vacaciones, horas extras al 25 % y 35 %."
                )),
            Tema("Ficha de cada trabajador", Icons.Default.Badge,
                "Tarifa, sueldo, datos legales y horario individual.",
                listOf(
                    "En Equipo toca al trabajador.",
                    "Modo Simple: escribe el pago por hora.",
                    "Modo Avanzado: elige «Sueldo mensual» o «Por hora», régimen laboral, sistema de pensiones, asignación familiar, cargo, fecha de ingreso y apellidos y nombres completos.",
                    "Ajusta su horario si es distinto al del equipo y toca «Guardar»."
                ),
                listOf(
                    "Desactiva «Activo» para que deje de marcar sin perder su historial.",
                    "«Desvincular» lo retira del equipo; «Eliminar cuenta» borra su perfil."
                )),
            Tema("Horarios de trabajo", Icons.Default.Schedule,
                "Horario predeterminado, domingos y horarios por grupo.",
                listOf(
                    "En «Horario para nuevos colaboradores» ves y cambias el horario que recibe cada trabajador que se une con tu código.",
                    "El interruptor «Marcaje en domingo» habilita o deshabilita el domingo para todo el equipo.",
                    "En «1. Define el horario» elige días, horas, descanso y tolerancia. Activa «Horario distinto según el día» si cada día tiene horas propias.",
                    "En «2. ¿A quiénes aplica?» marca a los trabajadores (o «Todos») y toca «Aplicar horario»."
                ),
                listOf(
                    "Toca un grupo de «Horarios actuales del equipo» para seleccionarlo y editarlo de una vez.",
                    "La tolerancia son los minutos de gracia antes de contar una tardanza."
                ))
        )),
        Capitulo("Reportes y planilla", listOf(
            Tema("Reporte de asistencia y planilla", Icons.Default.Assessment,
                "Documentos del mes listos para compartir o presentar.",
                listOf(
                    "Abre «Reportes» y elige el mes con las flechas.",
                    "«Generar Reporte de Asistencia (DS 004-2006-TR)» crea el PDF que exige SUNAFIL.",
                    "«Exportar Reporte Excel (CSV)» descarga horas, ingresos, descuentos y neto por trabajador.",
                    "«Reporte de Costos» muestra cuánto pagaste por día o por persona, en PDF o CSV."
                )),
            Tema("PLAME y datos del empleador", Icons.Default.ReceiptLong,
                "Archivo para declarar en el PDT PLAME / T-Registro.",
                listOf(
                    "En «Datos del empleador» escribe el RUC (11 dígitos) y la razón social y toca «Guardar».",
                    "Toca «Exportar PLAME / T-Registro (.TXT)» y compártelo con tu contador."
                ),
                listOf("Los cálculos son referenciales: revísalos con tu contador antes de declarar.")),
            Tema("Planilla del Modo Avanzado (PDF)", Icons.Default.Description,
                "Planilla completa, parámetros legales y boletas individuales.",
                listOf(
                    "Pasa al menos un trabajador a Modo Avanzado.",
                    "En Reportes → «Planilla Modo Avanzado Perú (PDF)» elige: Planilla completa, Planilla mensual, Parámetros legales o Boleta individual.",
                    "Comparte el PDF por WhatsApp, correo o imprímelo para firmar."
                ),
                listOf("Completa apellidos, nombres y DNI de cada trabajador para que la boleta salga con sus datos legales."))
        )),
        Capitulo("Cuenta y seguridad", listOf(
            Tema("Perfil, plan VIP y sesión", Icons.Default.AccountCircle,
                "Tu cuenta y la de tu empresa.",
                listOf(
                    "Toca tu foto (arriba a la izquierda) para cambiar foto, nombre o teléfono.",
                    "En Ajustes → «Plan VIP» ves los beneficios y cómo activarlo.",
                    "«Cerrar Sesión» hace que el teléfono deje de recordar tu cuenta.",
                    "«Eliminar empresa y cuenta» borra todos los datos y desvincula a tu equipo (no se puede deshacer)."
                )),
            Tema("Cómo te protege Marcaya", Icons.Default.Security,
                "Controles antifraude incluidos.",
                listOf(
                    "Rostro en vivo con prueba de parpadeo y giro: una foto o pantalla no sirve.",
                    "Bloqueo de GPS falso: si alguien usa una app de ubicación simulada, no puede marcar y tú recibes una alerta.",
                    "Hora oficial del servidor: cambiar el reloj del celular no altera las marcas.",
                    "Las marcas no se pueden editar ni borrar desde el celular del trabajador."
                ),
                listOf("La imagen del rostro se analiza en el celular y se descarta al instante: no se guarda ninguna foto."))
        ))
    )

    // ---------------------------------------------------------------- Colaborador

    val beneficiosColaborador = listOf(
        Beneficio(Icons.Default.PunchClock, "Marca en segundos",
            "Desde tu celular, sin colas ni relojes de tarjeta: tu rostro y tu ubicación bastan."),
        Beneficio(Icons.Default.Payments, "Sabes cuánto ganas",
            "Ves tus horas y tu pago del día y del mes en tiempo real, con el mismo cálculo que usa tu empleador."),
        Beneficio(Icons.Default.Verified, "Tus horas quedan registradas",
            "Cada marca queda guardada con la hora oficial: nadie puede cambiarla. Es tu respaldo ante cualquier reclamo."),
        Beneficio(Icons.Default.Description, "Justifica sin papeles",
            "Si llegas tarde o faltas por un motivo válido, envías tu justificación desde la app y ves si fue aprobada."),
        Beneficio(Icons.Default.Face, "Tu privacidad, protegida",
            "La verificación del rostro se hace en tu celular y la imagen se descarta al instante: no se guarda ninguna foto.")
    )

    val recorridoColaborador = listOf(
        PasoRecorrido(Icons.Default.PunchClock, "1. Marca tu asistencia",
            "En Marcaje usa los 4 botones en orden: Ingreso, Inicio Descanso, Fin Descanso y Salida."),
        PasoRecorrido(Icons.Default.Face, "2. Verifica tu rostro",
            "Al ingresar y al volver del descanso, mira la cámara, parpadea y gira un poco la cabeza. La imagen no se guarda."),
        PasoRecorrido(Icons.Default.LocationOn, "3. Marca dentro de tu zona",
            "Activa el GPS. La app te dice si estás dentro de tu lugar de trabajo antes de marcar."),
        PasoRecorrido(Icons.Default.History, "4. Revisa tus pagos",
            "En Historial ves tus días trabajados, tu boleta del mes y puedes descargarla en PDF."),
        PasoRecorrido(Icons.Default.Rule, "5. Justifica y entérate",
            "Si tienes una tardanza o falta, justifícala en Justificar. La campana te avisa de descuentos. Toda la guía está en Ajustes → Guía de uso.")
    )

    val manualColaborador = listOf(
        Capitulo("Primeros pasos", listOf(
            Tema("Registrarte y unirte a tu empresa", Icons.Default.Badge,
                "Necesitas el Código de Administrador que te da tu empleador.",
                listOf(
                    "Abre Marcaya y toca «Continuar con Google».",
                    "Elige la tarjeta «Colaborador».",
                    "Escribe tu nombre completo, elige DNI o Carnet de Extranjería y escribe el número.",
                    "Escribe tu celular (9 dígitos, empieza con 9) y el Código de Administrador (ADM-0000-X).",
                    "Toca «Registrarse»: quedas en el equipo con el horario de tu empresa."
                ),
                listOf("La app recuerda tu sesión: la próxima vez se abre directo en Marcaje.")),
            Tema("Permisos del celular", Icons.Default.Security,
                "La app necesita cámara, ubicación y notificaciones.",
                listOf(
                    "Ubicación: para comprobar que estás en tu lugar de trabajo. Elige «Mientras usas la app».",
                    "Cámara: para verificar tu rostro al ingresar y al volver del descanso.",
                    "Notificaciones: para avisarte de tardanzas, justificaciones y descuentos."
                ),
                listOf("Si negaste un permiso, actívalo en Ajustes del teléfono → Apps → Marcaya → Permisos."))
        )),
        Capitulo("Marcar asistencia", listOf(
            Tema("Los 4 marcajes del día", Icons.Default.PunchClock,
                "Ingreso, Inicio Descanso, Fin Descanso y Salida.",
                listOf(
                    "Al llegar toca «Ingreso» y verifica tu rostro.",
                    "Al salir a refrigerio toca «Inicio Descanso».",
                    "Al volver toca «Fin Descanso» y verifica tu rostro.",
                    "Al terminar toca «Salida»: se cierra tu jornada y se calcula tu pago."
                ),
                listOf(
                    "Solo se habilita el botón que corresponde en cada momento.",
                    "Debes esperar al menos 60 segundos entre un marcaje y otro.",
                    "Si hoy no es día laborable en tu horario, no podrás marcar Ingreso: pide a tu empleador que habilite el día."
                )),
            Tema("Verificación del rostro", Icons.Default.Face,
                "Prueba de vida en tres pasos: rostro, parpadeo y giro.",
                listOf(
                    "Pon tu rostro dentro del óvalo, con buena luz y el celular a la altura de tus ojos.",
                    "Cuando lo indique, parpadea una vez despacio.",
                    "Luego gira ligeramente la cabeza a un lado.",
                    "Al ver el check verde, tu marcaje se registra."
                ),
                listOf("Debe aparecer solo una persona en la cámara. Lentes oscuros o mucha sombra pueden impedir la verificación.")),
            Tema("Ubicación (semáforo)", Icons.Default.LocationOn,
                "Antes de marcar la app comprueba dónde estás.",
                listOf(
                    "Verde: estás dentro de tu zona de trabajo; se muestra a cuántos metros del centro.",
                    "Rojo: estás fuera de la zona. Puedes marcar, pero se avisa a tu empleador y deberás justificar.",
                    "Ámbar: no hay señal GPS; activa la ubicación y toca el ícono de ubicación para reintentar."
                ),
                listOf("Las apps de GPS falso se detectan: el marcaje se bloquea y se avisa al empleador."))
        )),
        Capitulo("Pagos e historial", listOf(
            Tema("Tu ganancia del día y del mes", Icons.Default.Payments,
                "Transparencia total de lo que ganas.",
                listOf(
                    "En Marcaje, «Total acumulado hoy» muestra lo ganado según tus horas y tu tarifa.",
                    "Abajo ves las horas de hoy y el acumulado (o neto proyectado) del mes.",
                    "Si tu empleador aplicó un descuento, se resta de tu pago de ese día."
                )),
            Tema("Historial y boleta", Icons.Default.History,
                "Tus días trabajados y tu boleta mensual.",
                listOf(
                    "Abre «Historial» y cambia de mes con las flechas.",
                    "Cada día muestra horas, ingreso y salida, pago y avisos (tardanza, extras, fuera de zona).",
                    "Al final está tu boleta: ingresos, descuentos y neto a recibir.",
                    "Toca el botón de descarga para obtener la boleta en PDF."
                ))
        )),
        Capitulo("Justificaciones y avisos", listOf(
            Tema("Justificar una incidencia", Icons.Default.Rule,
                "Tardanzas, faltas, salidas anticipadas o marcas fuera de la zona.",
                listOf(
                    "Si tienes incidencias, verás un aviso rojo en Marcaje; tócalo o abre «Justificar».",
                    "Elige la incidencia y toca «Justificar».",
                    "Elige el motivo (cita médica, transporte, comisión de servicios, etc.) y escribe el detalle.",
                    "Envía: tu empleador la aprobará o no, y te llegará una notificación."
                ),
                listOf("Con «Solicitar permiso para otra fecha» puedes pedir un permiso por adelantado.")),
            Tema("Notificaciones y descuentos", Icons.Default.Notifications,
                "La campana te informa de los descuentos aplicados por tu empleador.",
                listOf(
                    "El número rojo en la campana indica avisos sin leer.",
                    "Toca la campana para ver cada descuento: monto, concepto, fecha y motivo.",
                    "Toca «Marcar como leído» y el aviso desaparece."
                ))
        )),
        Capitulo("Cuenta", listOf(
            Tema("Perfil y sesión", Icons.Default.AccountCircle,
                "Tu foto, tus datos y tu cuenta.",
                listOf(
                    "Toca tu foto (arriba a la izquierda) para cambiarla o editar tu nombre y teléfono.",
                    "En Ajustes → «Cerrar Sesión» la app deja de recordar tu cuenta en ese celular.",
                    "«Eliminar mi cuenta» borra tu perfil; tus marcajes quedan con tu empleador como registro legal."
                )),
            Tema("Cambiar de empresa", Icons.AutoMirrored.Filled.Logout,
                "Si tu empleador te desvincula, puedes unirte a otra.",
                listOf(
                    "Al abrir la app verás «Unirme a una empresa».",
                    "Escribe el nuevo Código de Administrador y toca «Vincularme»."
                ))
        ))
    )

    fun beneficios(esAdmin: Boolean) = if (esAdmin) beneficiosAdmin else beneficiosColaborador
    fun recorrido(esAdmin: Boolean) = if (esAdmin) recorridoAdmin else recorridoColaborador
    fun manual(esAdmin: Boolean) = if (esAdmin) manualAdmin else manualColaborador
    val iconoPremium get() = Icons.Default.WorkspacePremium
}
