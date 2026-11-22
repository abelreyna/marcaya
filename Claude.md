# Especificaciones Técnicas y Funcionales: Registro de Tiempo de Personal (Android)

## 1. Información General del Proyecto

* **Nombre de la Aplicación:** Registro de Tiempo de Personal
* **Versión:** 1.0.0
* **Autor y Desarrollador Principal:** **Juan Abel Reyna Gonzales — Ingeniero Industrial**
* **Correo Electrónico de Contacto:** `abel.reynago@gmail.com`
* **Repositorio en GitHub:** `registro-de-tiempo-de-personal`
* **Plataforma Objetivo:** Android (SDK mínimo: API 24 / Android 7.0 Nougat | SDK objetivo: API 35 / Android 15)
* **País de Aplicación Normativa:** Perú (Moneda principal: Soles `S/`, con soporte configurable para `USD $`).
* **Descripción Ejecutiva:** Solución integral diseñada y desarrollada por **Juan Abel Reyna Gonzales (Ingeniero Industrial)** para gestionar, auditar y automatizar el control de asistencia del personal mediante validación biométrica facial instantánea en dispositivo (sin almacenamiento de imágenes), delimitación geográfica matemática (*geofencing* GPS) y cálculo salarial transparente bajo dos modalidades seleccionables por el Administrador: **Modo Simple (Pago directo por horas para MYPES)** y **Modo Avanzado (Cálculo integral de planilla, beneficios sociales y tributación vigente en Perú)**.

## 2. Arquitectura y Stack Tecnológico (Optimizado para Costo \$0)

* **Lenguaje de Programación:** Kotlin (100%)
* **Interfaz de Usuario (UI):** Jetpack Compose con Material Design 3
* **Patrón de Arquitectura:** MVVM (Model-View-ViewModel) + Clean Architecture (Capas: `ui`, `domain`, `data`)
* **Inyección de Dependencias:** Hilt / Dagger
* **Persistencia Local Segura de Sesión:** Jetpack DataStore cifrado con Android Keystore + Firebase Auth Local Token Cache (mantiene la sesión activa de forma indefinida hasta que el usuario pulse explícitamente *"Cerrar Sesión"*).
* **Backend as a Service (Firebase — Plan Spark Gratuito):**
  * **Firebase Authentication:** Inicio de sesión y registro exclusivo mediante cuenta de **Google** (`Credential Manager` / `GoogleSignInClient`), eliminando costos por envío de SMS OTP.
  * **Cloud Firestore:** Base de datos NoSQL en tiempo real exclusivamente para registros ligeros de texto y números (perfiles, vínculos por código de administrador, configuración de régimen laboral, marcas booleanas de asistencia y proyecciones de boleta).
  * **Firebase Remote Config:** Control remoto en tiempo real para activar campañas, banners de aviso o el muro de suscripción **VIP** sin necesidad de publicar una actualización en Google Play Store.
  * **Firebase App Check (Play Integrity API):** Protección contra acceso no autorizado, bots y clientes modificados.
  * **Firebase Cloud Messaging (FCM):** Notificaciones *push* en tiempo real para alertas de geocerca, tardanzas, aprobaciones y recordatorios de descanso.
  * *Nota de Optimización:* Se prescinde de Firebase Storage para fotografías de marcación, manteniendo la infraestructura dentro de las cuotas gratuitas de Firebase.
* **Actualizaciones Nativas Automáticas:**
  * **Google Play In-App Updates API:** Permite solicitar actualizaciones flexibles o inmediatas dentro de la misma aplicación cuando se publiquen nuevas versiones en Play Store.
* **Biometría Facial en Dispositivo (Sin Almacenamiento):**
  * **CameraX API + Google ML Kit Face Detection (On-Device):** Detecta presencia de rostro humano vivo (incluyendo prueba de vida activa mediante parpadeo o giro facial) directamente en la memoria RAM del celular y descarta la imagen inmediatamente tras validar (`validacionFacial = true`), sin guardarla en el dispositivo ni subirla a servidores.
* **Servicios de Ubicación y Geocerca Matemática:**
  * **FusedLocationProviderClient + Fórmula de Haversine Local:** Calcula la distancia exacta en metros entre la coordenada GPS del trabajador y el centro de la sede directamente en Kotlin, sin consumir llamadas de API de pago.
  * **Google Maps SDK for Android:** Utilizado exclusivamente en la vista del Administrador para configurar visualmente el radio de las geocercas (cubierto por el crédito mensual gratuito de Google Cloud).

## 3. Flujo de Autenticación, Persistencia de Sesión y Roles

### 3.1. Pantalla de Bienvenida e Inicio de Sesión Único con Google

Al abrir la aplicación por primera vez, se despliega una interfaz limpia con el **isotipo corporativo** (reloj de precisión integrado con pin de geolocalización GPS y escudo/check de verificación en degradado azul marino y verde esmeralda) con una única vía de acceso rápida, segura y gratuita:

1. **Continuar con Google:** Registra o inicia sesión automáticamente utilizando la cuenta de Google vinculada al dispositivo Android.
2. **Recordatorio y Persistencia de Sesión Obligatoria:**
   * Incluye la opción visual **"Mantener sesión iniciada (Recordarme)"** activada por defecto y respaldada por el token persistente de `FirebaseAuth` + `Jetpack DataStore`.
   * Una vez que el Administrador o Colaborador se registra o inicia sesión por primera vez, **la app recordará su cuenta, su rol y su vinculación de forma permanente**.
   * En los siguientes ingresos, la app omitirá la pantalla de login y abrirá directamente el **Panel del Administrador** o la **Pantalla de Marcaje del Colaborador**, a menos que el usuario pulse explícitamente el botón **"Cerrar Sesión"** en el menú de Ajustes.

### 3.2. Selección de Rol y Vinculación (Primer Registro)

Cuando una cuenta de Google ingresa por primera vez y aún no tiene perfil en Cloud Firestore, se muestra la pantalla de configuración inicial:

* **Administrador (o Coadministrador):** Crea la cuenta de empresa/negocio, ingresa el nombre comercial o razón social y genera automáticamente su **Código de Administrador** único (ej. `ADM-8392-X`). Al crearse, se registra automáticamente su `fechaCreacion` y el estado inicial `plan: "FREE"`.
* **Colaborador:** Completa sus datos personales (Nombre completo, DNI/CE y número de teléfono de contacto como dato informativo) e ingresa obligatoriamente el **Código de Administrador** proporcionado por su empleador para quedar vinculado automáticamente a su equipo y geocerca.

## 4. Requerimientos Funcionales por Rol

### 4.1. Módulo de Administrador

1. **Generación y Compartido de Código de Administrador:**
   * Código alfanumérico único (`ADM-8392-X`) con botones de copiado rápido y envío directo por WhatsApp/Correo.
2. **Configuración de Geocercas (Zonas de Trabajo):**
   * Mapa interactivo de Google Maps donde el administrador fija el punto central de la sede u obra y ajusta el **radio de cobertura en metros** (ej. 50m, 100m, 150m). Las coordenadas y el radio se guardan en Firestore para su evaluación matemática local.
3. **Selector de Modalidad de Pago (Simple vs. Avanzado Perú):**
   * Configuración global o individual por colaborador entre pago directo por hora o planilla formal peruana.
4. **Dashboard de Monitoreo en Tiempo Real:**
   * Visualización en vivo del estado de cada colaborador: *Dentro del área*, *En descanso*, *Fuera de rango* o *Tardanza*.
   * Registro auditado con sello de verificación facial (`validacionFacial: true`), hora oficial del servidor y distancia en metros al centro de la geocerca.
   * Indicador financiero en vivo de horas acumuladas y costo laboral estimado del día/mes.
5. **Gestión de Incidencias y Reportes Legales:**
   * Revisión y aprobación de justificaciones enviadas por colaboradores (comisión de servicios, cita médica, trabajo de campo, problemas técnicos).
   * Exportación de reportes en **PDF / Excel** y formatos compatibles con **SUNAFIL (DS 004-2006-TR)** y **PLAME / T-Registro (.TXT)**.

### 4.2. Módulo de Colaborador

1. **Marcaje Biométrico Efímero y Geolocalizado (4 Eventos):**
   * **Ingreso:** Activa cámara frontal CameraX + ML Kit para verificación facial en vivo (la imagen se descarta de la RAM al instante sin guardarse) + valida distancia GPS mediante cálculo Haversine local y ausencia de GPS falso.
   * **Inicio Descanso:** Registra timestamp del servidor y estado de geocerca; detiene el cómputo de horas efectivas.
   * **Fin Descanso:** Registra timestamp del servidor y estado de geocerca; reanuda el cómputo de horas efectivas.
   * **Salida:** Valida rostro en vivo (sin guardar foto) + valida GPS + cierra la jornada diaria y consolida el cálculo salarial.
2. **Indicador de Ubicación Optimizado:**
   * Muestra un estado visual claro tipo semáforo (*"Ubicación verificada: Oficina Central — a 18 m del centro"*) sin necesidad de cargar mapas interactivos pesados en cada marcaje.
3. **Panel de Transparencia Financiera:**
   * Visualización en tiempo real de horas trabajadas en el día, tarifa por hora y ganancia acumulada (neta en Modo Simple o desglosada en Modo Avanzado).
4. **Historial, Justificaciones y Boleta Mensual:**
   * Consulta de días trabajados, descarga de boleta mensual proyectada en PDF y envío de justificaciones en caso de marcaje fuera de rango o tardanza.

## 5. Motor Financiero Dual (Modo Simple vs. Modo Avanzado Perú)

### 5.1. Modo Simple (Ideal para MYPES / Pagos Directos por Hora)

* **Objetivo:** Brindar máxima simplicidad a microempresas y emprendimientos que pagan por hora trabajada sin retenciones de planilla.
* **Funcionamiento:** El administrador establece únicamente la **Tarifa por Hora** (ej. `S/ 12.00`).
* **Cálculo:** Multiplica exactamente las horas y minutos efectivos trabajados por la tarifa horaria, sin descuentos ni deducciones. Tanto el empleador como el trabajador ven el monto exacto ganado al instante.

### 5.2. Modo Avanzado (Legislación Laboral y Tributaria del Perú)

* **Objetivo:** Garantizar transparencia total en empresas formales bajo la normativa laboral peruana vigente.
* **Regímenes Laborales Soportados:**
  * **Régimen General**
  * **Régimen MYPE (REMYPE — Microempresa y Pequeña Empresa)**
* **Variables Configurables por Colaborador:**
  * **Sueldo Base / Valor Hora Equivalente** (referencia RMV Perú y jornada base de 30 días / 240 horas mensuales).
  * **Asignación Familiar:** Switch (Sí/No) — Suma el $10\%$ de la Remuneración Mínima Vital (RMV) vigente cuando corresponde.
  * **Horas Extras:** Cálculo automático con recargo del $25\%$ para las 2 primeras horas y $35\%$ a partir de la tercera hora.
  * **Sistema de Pensiones (Descuento al Trabajador):**
    * **ONP:** Retención fija del $13\%$.
    * **AFP (Integra, Prima, Profuturo, Hábitat):** Aporte obligatorio ($10\%$) + Prima de Seguro ($\approx 1.70\%$) + Comisión según flujo/mixta.
  * **Impuesto a la Renta de Quinta Categoría:** Proyección anual automática cuando los ingresos superan las $7 \text{ UIT}$, aplicando los tramos progresivos de ley ($8\%$, $14\%$, $17\%$, $20\%$, $30\%$).
* **Aportes del Empleador y Beneficios Sociales (Desglose Transparente):**
  * **EsSalud ($9\%$):** Mostrado como aporte asumido por el empleador (o SIS en Microempresa REMYPE).
  * **CTS (Compensación por Tiempo de Servicios):** Provisión mensual devengada ($\approx 8.33\%$ mensual en Régimen General; $50\%$ en Pequeña Empresa).
  * **Gratificaciones Legales (Julio y Diciembre):** Provisión proporcional mensual ($1/6$ por mes en Régimen General + Bonificación Extraordinaria del $9\%$; $50\%$ en Pequeña Empresa).

## 6. Arquitectura de Ciberseguridad, Protección Antifraude y Blindaje

Para garantizar la integridad de los datos salariales, evitar manipulaciones de asistencia por parte de usuarios y bloquear ataques externos o ingeniería inversa, la aplicación implementa una defensa en profundidad en 4 niveles:

### 6.1. Controles Antifraude en Dispositivo (Nivel Cliente)

1. **Bloqueo de Ubicaciones Falsas (*Anti-Fake GPS / Mock Locations*):**
   * Toda lectura del `FusedLocationProviderClient` es auditada mediante `location.isMock` (API 31+) y `location.isFromMockProvider` (API 24–30). Si se detecta simulación de GPS, el sistema bloquea el registro de asistencia y notifica al Administrador.
2. **Prueba de Vida Facial Activa (*Liveness Detection* con ML Kit):**
   * Para evitar suplantaciones mediante fotografías impresas o pantallas de otros teléfonos, ML Kit exige detectar parpadeo dinámico (`leftEyeOpenProbability` / `rightEyeOpenProbability`) o micro-movimiento de cabeza (`headEulerAngleY`) antes de autorizar `validacionFacial = true`.
3. **Inmunidad contra Alteración del Reloj del Teléfono:**
   * La aplicación nunca confía en el reloj local del dispositivo (`System.currentTimeMillis()`). Todos los marcajes utilizan obligatoriamente `FieldValue.serverTimestamp()` estampado directamente por los servidores de Google.
4. **Detección de Root / Integridad del Entorno y Cifrado Local:**
   * Las preferencias sensibles en caché (rol asignado y `adminId`) se almacenan cifradas mediante **Android Keystore** para evitar escalamiento de privilegios en dispositivos rooteados.

### 6.2. Protección de Infraestructura y APIs (Nivel Servidor)

1. **Firebase App Check con Google Play Integrity API:**
   * Bloquea cualquier petición hacia Firestore que no provenga de la aplicación oficial firmada por **Juan Abel Reyna Gonzales** y descargada desde Google Play Store, neutralizando bots, emuladores alterados y scripts externos.
2. **Restricción Criptográfica de API Keys:**
   * La llave de Google Maps SDK está restringida en Google Cloud Console exclusivamente al `applicationId` de la app y a la huella digital `SHA-1` del certificado de firma de Google Play.
3. **Reglas de Seguridad Estrictas en Cloud Firestore (`firestore.rules`):**
   * Garantizan aislamiento total entre empresas (*multi-tenant*), inmutabilidad de los marcajes realizados por colaboradores, protección de tarifas salariales y prevención de spam (*rate limiting* mínimo de 60 segundos entre marcajes):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Funciones auxiliares de verificación
    function isAuthenticated() {
      return request.auth != null;
    }

    function getUserData() {
      return get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data;
    }

    function isAdminOfCompany(companyAdminId) {
      return isAuthenticated()
        && getUserData().rol == 'ADMINISTRADOR'
        && getUserData().adminId == companyAdminId;
    }

    // Colección de Usuarios y Perfiles
    match /usuarios/{userId} {
      // Un usuario lee su propio perfil; el Admin lee los perfiles de su empresa
      allow read: if isAuthenticated() && (
        request.auth.uid == userId ||
        isAdminOfCompany(resource.data.adminId)
      );

      // Creación inicial de perfil tras login con Google
      allow create: if isAuthenticated() && request.auth.uid == userId;

      // Solo el Administrador de la empresa puede modificar tarifas, régimen laboral o estado
      allow update: if isAuthenticated() && (
        isAdminOfCompany(resource.data.adminId) ||
        (request.auth.uid == userId
          && !request.resource.data.diff(resource.data).affectedKeys()
              .hasAny(['rol', 'adminId', 'tarifaPorHora', 'regimenLaboral']))
      );

      allow delete: if false;
    }

    // Colección de Empresas, Geocercas y Estado de Suscripción (FREE / VIP)
    match /empresas/{adminId} {
      allow read: if isAuthenticated() && getUserData().adminId == adminId;
      allow create: if isAuthenticated() && request.auth.uid == adminId
        && request.resource.data.plan == 'FREE';
      // El Admin actualiza datos de empresa/geocerca, pero no puede auto-asignarse plan VIP desde el cliente
      allow update: if isAuthenticated() && request.auth.uid == adminId
        && !request.resource.data.diff(resource.data).affectedKeys().hasAny(['plan', 'vipExpiraEn']);
      allow delete: if false;
    }

    // Colección de Marcajes de Asistencia (Inmutables para el Colaborador)
    match /marcajes/{marcajeId} {
      // El colaborador solo ve sus marcajes; el Admin ve todos los de su empresa
      allow read: if isAuthenticated() && (
        resource.data.colaboradorId == request.auth.uid ||
        isAdminOfCompany(resource.data.adminId)
      );

      // Solo el colaborador dueño crea su marcaje usando timestamp oficial del servidor
      allow create: if isAuthenticated()
        && request.resource.data.colaboradorId == request.auth.uid
        && request.resource.data.adminId == getUserData().adminId
        && request.resource.data.timestamp == request.time
        && request.resource.data.validacionFacial == true;

      // Ningún colaborador puede alterar o borrar historial; solo el Admin aprueba incidencias
      allow update: if isAdminOfCompany(resource.data.adminId);
      allow delete: if false;
    }
  }
}
```

### 6.3. Protección del Código Fuente (Ofuscación R8 / ProGuard)

En el archivo `app/build.gradle.kts`, la compilación de producción (`release`) habilita la ofuscación y reducción de código para impedir ingeniería inversa sobre la lógica de negocio:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

## 7. Monetización Diferida (Plan FREE a VIP) y Actualizaciones Automáticas

Para lanzar la aplicación **100% gratuita** y activar en el futuro (ej. tras 12 o 18 meses de crecimiento) un muro de suscripción **VIP** sin obligar a los usuarios a descargar una actualización desde Google Play Store, el sistema incorpora **Firebase Remote Config** y **Google Play In-App Updates**:

### 7.1. Parámetros Remotos Configurados (`Firebase Remote Config`)

La aplicación consulta automáticamente estos parámetros al iniciar (con caché de 12 horas en producción y 0 segundos en pruebas):

| Parámetro en Remote Config | Valor Inicial (Lanzamiento) | Valor Futuro (Activación VIP) | Función en la App |
| :--- | :--- | :--- | :--- |
| `vip_gate_enabled` | `false` | `true` | Activa/desactiva globalmente la evaluación de suscripción VIP. |
| `vip_free_trial_days` | `365` | `365` (o `540` para 1.5 años) | Días gratuitos permitidos desde `empresa.fechaCreacion` antes de mostrar bloqueo. |
| `vip_warning_days_before` | `15` | `15` | Días previos al vencimiento en que aparece un banner de aviso renovable. |
| `vip_price_monthly_text` | `"S/ 29.90 / mes"` | `"S/ 29.90 / mes"` | Texto dinámico del precio mensual mostrado en la pantalla VIP. |
| `vip_price_annual_text` | `"S/ 290.00 / año"` | `"S/ 290.00 / año"` | Texto dinámico del precio anual mostrado en la pantalla VIP. |
| `vip_promo_message` | `"Disfruta de todas las funciones gratis..."` | `"Actualiza a VIP para seguridad reforzada, mayor velocidad y reportes ilimitados."` | Mensaje comercial editable en tiempo real desde la consola de Firebase. |
| `vip_hard_block` | `false` | `true` | `false`: muestra anuncio cerrable; `true`: bloquea nuevos marcajes hasta activar VIP (conservando lectura del historial). |

### 7.2. Lógica de Evaluación en Kotlin (`VipAccessManager.kt`)

```kotlin
data class VipConfig(
    val isGateEnabled: Boolean,
    val freeTrialDays: Long,
    val warningDaysBefore: Long,
    val monthlyPrice: String,
    val annualPrice: String,
    val promoMessage: String,
    val isHardBlock: Boolean
)

enum class AccessStatus {
    GRANTED,          // Acceso libre (Plan VIP activo o muro desactivado)
    TRIAL_ACTIVE,     // Dentro del periodo gratuito (ej. primer año)
    EXPIRING_SOON,    // Faltan <= 15 días: mostrar banner informativo
    BLOCKED_NEED_VIP  // Periodo gratuito cumplido: mostrar pantalla de bloqueo VIP
}

fun evaluateCompanyAccess(
    companyPlan: String,          // "FREE" o "VIP" desde Firestore
    createdAtMillis: Long,        // Fecha de registro de la empresa
    nowMillis: Long,
    config: VipConfig
): AccessStatus {
    if (companyPlan == "VIP" || !config.isGateEnabled) {
        return AccessStatus.GRANTED
    }
    val elapsedDays = (nowMillis - createdAtMillis) / (1000 * 60 * 60 * 24)
    val remainingDays = config.freeTrialDays - elapsedDays

    return when {
        remainingDays <= 0 && config.isHardBlock -> AccessStatus.BLOCKED_NEED_VIP
        remainingDays <= config.warningDaysBefore -> AccessStatus.EXPIRING_SOON
        else -> AccessStatus.TRIAL_ACTIVE
    }
}
```

### 7.3. Flujo de Activación VIP y Actualizaciones en la App

1. **Activación de Licencia VIP (0% Comisión):** El Administrador visualiza la pantalla VIP con los beneficios (mayor seguridad, prioridad de servidores, Modo Avanzado Perú y exportación SUNAFIL), contacta vía correo (`abel.reynago@gmail.com`) o WhatsApp integrado, y tras confirmar el pago mensual/anual, el desarrollador cambia en Firestore el campo de la empresa de `plan: "FREE"` a `plan: "VIP"` (desbloqueando la app al instante).
2. **Actualizaciones Automáticas (`AppUpdateManager`):** Cuando se publique una nueva versión del código en Google Play Store, la app detecta la actualización disponible y muestra el diálogo oficial de actualización dentro de la misma aplicación (`AppUpdateType.IMMEDIATE` para parches de seguridad o `AppUpdateType.FLEXIBLE` para mejoras visuales).

## 8. Apartado "Acerca de", Contacto y Créditos de Autoría

La aplicación cuenta con una pantalla dedicada **"Acerca de"** accesible desde el menú principal y desde **Ajustes de la Aplicación**, diseñada para destacar la autoría intelectual y técnica del proyecto y facilitar el contacto directo con empresas o usuarios:

* **Logotipo:** Isotipo oficial de *Registro de Tiempo de Personal* (`Versión 1.0.0`).
* **Autor y Desarrollador:** **Juan Abel Reyna Gonzales**
* **Especialidad Profesional:** **Ingeniero Industrial**
* **Contacto Directo (Soporte y Empresas):** `abel.reynago@gmail.com` (con botón interactivo *Intent `ACTION_SENDTO`* para redactar un correo electrónico directamente desde el dispositivo móvil).
* **Descripción:** *"Sistema integral de control de asistencia con validación biométrica facial instantánea, geocercas GPS y cálculo salarial (Modo Simple MYPE y Modo Avanzado Perú)"*.
* **Enlaces Oficiales:** Acceso directo a **Contactar por Correo (`abel.reynago@gmail.com`)**, **Repositorio en GitHub** y **Licencia y Términos**.
* **Copyright:** `© 2026 Juan Abel Reyna Gonzales - Todos los derechos reservados`.

## 9. Catálogo Completo de Capturas de Pantalla (`docs/screenshots/`)

Todas las pantallas siguen una identidad visual unificada basada en **Material Design 3** con paleta Azul Marino Corporativo (`#13294B`), Verde Esmeralda (`#0F8B6D`) y fondos claros de alto contraste:

1. `01_flujo_principal_4_pantallas.png` — Acceso con Google, Selección de Rol, Panel del Administrador y Marcaje del Colaborador.
2. `02_inicio_sesion_y_validacion_empleados.png` — Flujo de inicio de sesión persistente y validación en vivo de empleados.
3. `03_vista_operativa_admin_y_marcaje.png` — Panel detallado del administrador (con resumen de costos) y pantalla de marcaje biométrico.
4. `04_configuracion_modo_simple_vs_avanzado_peru.png` — Configuración de Modo Simple (MYPE) vs. Modo Avanzado (legislación laboral de Perú) y reporte de planilla.
5. `05_historial_alertas_y_geocercas.png` — Historial de pagos del colaborador, centro de alertas/notificaciones y configuración de geocercas.
6. `06_boleta_peru_historial_y_ajustes.png` — Persistencia de cuenta, historial de asistencias, boleta detallada Perú y ajustes generales con cierre de sesión.
7. `07_incidencias_reportes_sunafil_y_gestion.png` — Justificación de incidencias, exportación de reportes SUNAFIL/PLAME y gestión de colaboradores.
8. `08_mapa_completo_20_pantallas_y_acerca_de.png` — Mapa panorámico con las 20 vistas de la aplicación.
9. `09_pantalla_acerca_de_autor_juan_abel_reyna_gonzales.png` — Pantalla dedicada **Acerca de** destacando la autoría de **Juan Abel Reyna Gonzales — Ingeniero Industrial** y el contacto directo (`abel.reynago@gmail.com`).

## 10. Estructura de Carpetas y Despliegue en GitHub

```text
registro-de-tiempo-de-personal/
├── app/
│   ├── proguard-rules.pro # Reglas de ofuscación y seguridad R8
│   └── src/main/java/com/juanreyna/registrotiempo/
│       ├── data/          # Repositorios Firebase Auth, Firestore, RemoteConfig y DataStore
│       ├── domain/        # Casos de uso, VipAccessManager y Calculadora Planilla Perú
│       ├── ui/            # Pantallas Jetpack Compose (LoginGoogle, Admin, Colaborador, VipGate, AcercaDe)
│       └── util/          # Validador Geocerca Haversine, Anti-FakeGPS, ML Kit Liveness e InAppUpdates
├── docs/
│   └── screenshots/       # Capturas de pantalla oficiales (01 a 09)
├── firestore.rules        # Reglas de seguridad desplegables en Firebase Console
├── CLAUDE.md              # Guía de arquitectura y contexto para desarrollo asistido por IA
└── README.md              # Documentación principal del repositorio
```

### Comandos para Inicializar y Subir a GitHub

```bash
git init
git add .
git commit -m "feat: arquitectura costo cero con Remote Config VIP, seguridad AppCheck y reglas Firestore - Autor: Juan Abel Reyna Gonzales"
git branch -M main
git remote add origin https://github.com/<tu-usuario>/registro-de-tiempo-de-personal.git
git push -u origin main
```