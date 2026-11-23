package com.juanreyna.registrotiempo.data.repository

import com.juanreyna.registrotiempo.domain.model.Celular
import com.juanreyna.registrotiempo.domain.model.TipoDocumento
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.juanreyna.registrotiempo.domain.model.Alerta
import com.juanreyna.registrotiempo.domain.model.ConfigPago
import com.juanreyna.registrotiempo.domain.model.Empresa
import com.juanreyna.registrotiempo.domain.model.EstadoJustificacion
import com.juanreyna.registrotiempo.domain.model.Geocerca
import com.juanreyna.registrotiempo.domain.model.Horario
import com.juanreyna.registrotiempo.domain.model.Justificacion
import com.juanreyna.registrotiempo.domain.model.Marcaje
import com.juanreyna.registrotiempo.domain.model.ModoPago
import com.juanreyna.registrotiempo.domain.model.MotivoJustificacion
import com.juanreyna.registrotiempo.domain.model.TipoIncidencia
import com.juanreyna.registrotiempo.domain.model.Rol
import com.juanreyna.registrotiempo.domain.model.TipoMarcaje
import com.juanreyna.registrotiempo.domain.model.Usuario
import com.juanreyna.registrotiempo.util.Fechas
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Acceso a Cloud Firestore. Colecciones: usuarios, empresas, codigos, marcajes, justificaciones.
 * Todas las consultas usan solo filtros de igualdad para no requerir índices compuestos.
 */
@Singleton
class FirestoreRepository @Inject constructor() {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val usuarios get() = db.collection("usuarios")
    private val empresas get() = db.collection("empresas")
    private val codigos get() = db.collection("codigos")
    private val marcajes get() = db.collection("marcajes")
    private val justificaciones get() = db.collection("justificaciones")

    // ---------- Perfiles ----------
    suspend fun obtenerUsuario(uid: String): Usuario? =
        usuarios.document(uid).get().await().takeIf { it.exists() }?.aUsuario()

    /** true/false según exista el perfil en el servidor (ignora la caché local para no confundir "sin conexión" con "eliminado"). */
    fun observarExistePerfil(uid: String): Flow<Boolean> = callbackFlow {
        val reg = usuarios.document(uid).addSnapshotListener { s, _ ->
            if (s != null && !s.metadata.isFromCache) trySend(s.exists())
        }
        awaitClose { reg.remove() }
    }

    fun observarUsuario(uid: String): Flow<Usuario?> = callbackFlow {
        val reg = usuarios.document(uid).addSnapshotListener { s, _ ->
            trySend(s?.takeIf { it.exists() }?.aUsuario())
        }
        awaitClose { reg.remove() }
    }

    /**
     * Datos básicos que el propio usuario puede editar (nombre y teléfono).
     * Rol, empresa, DNI y configuración de pago no se tocan aquí.
     */
    suspend fun actualizarDatosPerfil(uid: String, nombre: String, telefono: String) {
        usuarios.document(uid).update(mapOf("nombre" to nombre, "telefono" to telefono)).await()
    }

    /** Foto de perfil en Base64 (cadena vacía = quitar foto). */
    suspend fun actualizarFotoPerfil(uid: String, foto: String) {
        usuarios.document(uid).update("foto", foto).await()
    }

    /** Registro del Administrador: perfil → empresa (plan FREE) → código único. */
    suspend fun registrarAdministrador(uid: String, correo: String, nombre: String, nombreComercial: String): String {
        usuarios.document(uid).set(
            mapOf(
                "rol" to Rol.ADMINISTRADOR.name, "adminId" to uid, "nombre" to nombre,
                "correo" to correo, "activo" to true, "creado" to FieldValue.serverTimestamp()
            )
        ).await()
        val codigo = reservarCodigoUnico(uid)
        empresas.document(uid).set(
            mapOf(
                "nombreComercial" to nombreComercial,
                "codigoAdministrador" to codigo,
                "plan" to "FREE",
                "fechaCreacion" to FieldValue.serverTimestamp(),
                "moneda" to "PEN",
                "modoPagoGlobal" to ModoPago.SIMPLE.name,
                "geocerca" to Geocerca().aMapa()
            )
        ).await()
        return codigo
    }

    private suspend fun reservarCodigoUnico(uid: String): String {
        repeat(10) {
            val codigo = "ADM-%04d-%c".format(Random.nextInt(0, 10_000), ('A'..'Z').random())
            val ref = codigos.document(codigo)
            val ok = db.runTransaction { tx ->
                if (tx.get(ref).exists()) false
                else { tx.set(ref, mapOf("adminId" to uid)); true }
            }.await()
            if (ok) return codigo
        }
        error("No se pudo generar un código único, intenta de nuevo")
    }

    /** Registro del Colaborador vinculado mediante el Código de Administrador. */
    suspend fun registrarColaborador(
        uid: String, correo: String, nombre: String, tipoDocumento: TipoDocumento, dni: String, telefono: String, codigo: String
    ) {
        require(tipoDocumento.esValido(dni)) { "El ${tipoDocumento.etiqueta} debe tener ${tipoDocumento.digitos} dígitos" }
        require(Celular.esValido(telefono)) { "El celular debe tener 9 dígitos y empezar con 9" }
        val cod = codigo.trim().uppercase()
        val doc = codigos.document(cod).get().await()
        val adminId = doc.getString("adminId") ?: error("Código de Administrador no válido")
        usuarios.document(uid).set(
            mapOf(
                "rol" to Rol.COLABORADOR.name, "adminId" to adminId, "codigoVinculo" to cod,
                "nombre" to nombre, "correo" to correo, "dni" to dni, "tipoDocumento" to tipoDocumento.name, "telefono" to telefono,
                "activo" to true, "horaIngresoEsperada" to Horario().entrada, // el horario completo lo fija el administrador
                "creado" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun observarColaboradores(adminId: String): Flow<List<Usuario>> = callbackFlow {
        val reg = usuarios.whereEqualTo("adminId", adminId)
            .whereEqualTo("rol", Rol.COLABORADOR.name)
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.map { it.aUsuario() }?.sortedBy { it.nombre } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    /** [tipoDocumento] y [dni] nulos = no se cambia el documento. */
    suspend fun actualizarColaborador(
        uid: String, config: ConfigPago, horario: Horario, activo: Boolean, tipoDocumento: TipoDocumento? = null, dni: String? = null
    ) {
        val documento = if (tipoDocumento != null && dni != null) {
            require(tipoDocumento.esValido(dni)) { "El ${tipoDocumento.etiqueta} debe tener ${tipoDocumento.digitos} dígitos" }
            mapOf("tipoDocumento" to tipoDocumento.name, "dni" to dni)
        } else emptyMap()
        usuarios.document(uid).update(
            documento + mapOf(
                "configPago" to config.aMapa(), "activo" to activo,
                "horario" to horario.aMapa(), "horaIngresoEsperada" to horario.entrada
            )
        ).await()
    }

    /** Aplica el mismo horario a varios colaboradores en un solo lote. */
    suspend fun aplicarHorario(uids: List<String>, horario: Horario) {
        if (uids.isEmpty()) return
        val lote = db.batch()
        uids.forEach { lote.update(usuarios.document(it), mapOf("horario" to horario.aMapa(), "horaIngresoEsperada" to horario.entrada)) }
        lote.commit().await()
    }

    // ---------- Empresa ----------
    fun observarEmpresa(adminId: String): Flow<Empresa?> = callbackFlow {
        val reg = empresas.document(adminId).addSnapshotListener { s, _ ->
            trySend(s?.takeIf { it.exists() }?.aEmpresa())
        }
        awaitClose { reg.remove() }
    }

    suspend fun obtenerEmpresa(adminId: String): Empresa? =
        empresas.document(adminId).get().await().takeIf { it.exists() }?.aEmpresa()

    suspend fun guardarGeocerca(adminId: String, g: Geocerca) {
        empresas.document(adminId).update("geocerca", g.aMapa()).await()
    }

    suspend fun guardarModoGlobal(adminId: String, modo: ModoPago, moneda: String) {
        empresas.document(adminId).update(mapOf("modoPagoGlobal" to modo.name, "moneda" to moneda)).await()
    }

    // ---------- Marcajes ----------
    /**
     * Crea el marcaje con hora oficial del servidor y actualiza `ultimoMarcaje` en el mismo lote
     * (las reglas exigen ≥ 60 s entre marcajes).
     */
    suspend fun registrarMarcaje(
        usuario: Usuario, tipo: TipoMarcaje, lat: Double, lon: Double,
        distancia: Double, dentro: Boolean, validacionFacial: Boolean
    ) {
        val ref = marcajes.document()
        val batch = db.batch()
        batch.set(
            ref, mapOf(
                "colaboradorId" to usuario.uid,
                "colaboradorNombre" to usuario.nombre,
                "adminId" to usuario.adminId,
                "tipo" to tipo.name,
                "timestamp" to FieldValue.serverTimestamp(),
                "fecha" to Fechas.hoy(),
                "mes" to Fechas.mesActual(),
                "latitud" to lat,
                "longitud" to lon,
                "distanciaMetros" to distancia,
                "dentroGeocerca" to dentro,
                "validacionFacial" to validacionFacial
            )
        )
        batch.update(usuarios.document(usuario.uid), "ultimoMarcaje", FieldValue.serverTimestamp())
        batch.commit().await()
    }

    /**
     * El empleador registra la salida de un colaborador que olvidó marcarla, con la hora que elija.
     * Las reglas solo permiten tipo SALIDA, marcado como registradoPorAdmin y con hora no futura.
     */
    suspend fun registrarSalidaPorAdmin(adminUid: String, colaborador: Usuario, fecha: String, salidaMillis: Long) {
        marcajes.document().set(
            mapOf(
                "colaboradorId" to colaborador.uid,
                "colaboradorNombre" to colaborador.nombre,
                "adminId" to colaborador.adminId,
                "tipo" to TipoMarcaje.SALIDA.name,
                "timestamp" to Timestamp(java.util.Date(salidaMillis)),
                "fecha" to fecha,
                "mes" to fecha.take(7),
                "latitud" to 0.0,
                "longitud" to 0.0,
                "distanciaMetros" to 0.0,
                "dentroGeocerca" to true,
                "validacionFacial" to false,
                "registradoPorAdmin" to true,
                "registradoPor" to adminUid
            )
        ).await()
    }

    fun observarMarcajesColaborador(uid: String, campo: String, valor: String): Flow<List<Marcaje>> = callbackFlow {
        val reg = marcajes.whereEqualTo("colaboradorId", uid).whereEqualTo(campo, valor)
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.map { it.aMarcaje() }?.sortedBy { it.timestampMillis ?: Long.MAX_VALUE } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    fun observarMarcajesEmpresa(adminId: String, campo: String, valor: String): Flow<List<Marcaje>> = callbackFlow {
        val reg = marcajes.whereEqualTo("adminId", adminId).whereEqualTo(campo, valor)
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.map { it.aMarcaje() }?.sortedBy { it.timestampMillis ?: Long.MAX_VALUE } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun marcajesEmpresaMes(adminId: String, mes: String): List<Marcaje> =
        marcajes.whereEqualTo("adminId", adminId).whereEqualTo("mes", mes).get().await()
            .documents.map { it.aMarcaje() }

    // ---------- Alertas antifraude / geocerca (visibles para el Administrador) ----------
    suspend fun registrarAlerta(usuario: Usuario, tipo: String, detalle: String) {
        db.collection("alertas").add(
            mapOf(
                "colaboradorId" to usuario.uid,
                "colaboradorNombre" to usuario.nombre,
                "adminId" to usuario.adminId,
                "tipo" to tipo,
                "detalle" to detalle,
                "fecha" to Fechas.hoy(),
                "creado" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun observarAlertasHoy(adminId: String): Flow<List<Alerta>> = callbackFlow {
        val reg = db.collection("alertas").whereEqualTo("adminId", adminId).whereEqualTo("fecha", Fechas.hoy())
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.map {
                    Alerta(
                        it.id, it.getString("colaboradorNombre").orEmpty(), it.getString("tipo").orEmpty(),
                        it.getString("detalle").orEmpty(),
                        it.getTimestamp("creado", com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
                    )
                }?.sortedByDescending { it.creadoMillis ?: 0 } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    // ---------- Justificaciones ----------
    suspend fun enviarJustificacion(usuario: Usuario, motivo: MotivoJustificacion, detalle: String, fecha: String, incidencia: TipoIncidencia?) {
        justificaciones.add(
            mapOf(
                "colaboradorId" to usuario.uid,
                "colaboradorNombre" to usuario.nombre,
                "adminId" to usuario.adminId,
                "motivo" to motivo.name,
                "detalle" to detalle,
                "fecha" to fecha,
                "estado" to EstadoJustificacion.PENDIENTE.name,
                "incidencia" to incidencia?.name,
                "creado" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Marcajes de un colaborador en un mes (para detectar incidencias). */
    suspend fun marcajesColaboradorMes(uid: String, mes: String): List<Marcaje> =
        marcajes.whereEqualTo("colaboradorId", uid).whereEqualTo("mes", mes).get().await().documents.map { it.aMarcaje() }

    fun observarJustificaciones(campo: String, valor: String): Flow<List<Justificacion>> = callbackFlow {
        val reg = justificaciones.whereEqualTo(campo, valor)
            .addSnapshotListener { s, _ ->
                trySend(s?.documents?.map { it.aJustificacion() }?.sortedByDescending { it.creadoMillis ?: 0 } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun resolverJustificacion(id: String, estado: EstadoJustificacion) {
        justificaciones.document(id).update(
            mapOf("estado" to estado.name, "resuelto" to FieldValue.serverTimestamp())
        ).await()
    }

    // ---------- Bajas, desvinculación y eliminación de cuentas ----------

    private fun exColaboradores(adminId: String) = empresas.document(adminId).collection("exColaboradores")

    private fun copiaExColaborador(u: Usuario, motivo: String) = mapOf(
        "nombre" to u.nombre, "dni" to u.dni, "correo" to u.correo,
        "configPago" to u.configPago.aMapa(), "horario" to u.horario.aMapa(),
        "motivo" to motivo, "fechaBaja" to FieldValue.serverTimestamp()
    )

    private val camposDesvinculo = mapOf(
        "adminId" to "", "codigoVinculo" to "", "activo" to false,
        "configPago" to FieldValue.delete(), "horario" to FieldValue.delete(), "horaIngresoEsperada" to FieldValue.delete()
    )

    /** Ex colaboradores de la empresa (para que sus marcajes sigan apareciendo en los reportes). */
    fun observarExColaboradores(adminId: String): Flow<List<Usuario>> = callbackFlow {
        val reg = exColaboradores(adminId).addSnapshotListener { s, _ ->
            trySend(s?.documents?.map { it.aExColaborador(adminId) } ?: emptyList())
        }
        awaitClose { reg.remove() }
    }

    /** El administrador retira al colaborador del equipo: se guarda una copia y el colaborador queda libre para otra empresa. */
    suspend fun desvincularColaborador(u: Usuario) {
        val b = db.batch()
        b.set(exColaboradores(u.adminId).document(u.uid), copiaExColaborador(u, "DESVINCULADO"))
        b.update(usuarios.document(u.uid), camposDesvinculo)
        b.commit().await()
    }

    /** Un colaborador sin empresa se vincula a otra con un nuevo Código de Administrador. */
    suspend fun revincularColaborador(uid: String, codigo: String) {
        val cod = codigo.trim().uppercase()
        val adminId = codigos.document(cod).get().await().getString("adminId") ?: error("Código de Administrador no válido")
        usuarios.document(uid).update(mapOf("adminId" to adminId, "codigoVinculo" to cod, "activo" to true)).await()
    }

    /**
     * El colaborador elimina su cuenta: se borra su perfil (datos personales y foto).
     * Sus marcajes y justificaciones se conservan para el empleador (registro de asistencia exigido por ley).
     */
    suspend fun eliminarPerfilColaborador(u: Usuario) {
        val b = db.batch()
        if (u.adminId.isNotBlank()) b.set(exColaboradores(u.adminId).document(u.uid), copiaExColaborador(u, "CUENTA_ELIMINADA"))
        b.delete(usuarios.document(u.uid))
        b.commit().await()
    }

    /**
     * El administrador elimina su empresa: desvincula a todo el equipo y borra marcajes, justificaciones,
     * alertas, ex colaboradores, el código, la empresa y su perfil.
     */
    suspend fun eliminarEmpresa(adminId: String, progreso: (String) -> Unit = {}) {
        // Las reglas solo permiten borrar asistencia de una empresa marcada en eliminación
        empresas.document(adminId).update("eliminando", true).await()
        progreso("Desvinculando colaboradores…")
        usuarios.whereEqualTo("adminId", adminId).whereEqualTo("rol", Rol.COLABORADOR.name).get().await()
            .documents.chunked(50).forEach { lote ->
                coroutineScope { lote.map { async { it.reference.update(camposDesvinculo).await() } }.awaitAll() }
            }
        progreso("Borrando marcajes…"); borrarConsulta(marcajes.whereEqualTo("adminId", adminId))
        progreso("Borrando justificaciones…"); borrarConsulta(justificaciones.whereEqualTo("adminId", adminId))
        progreso("Borrando alertas…"); borrarConsulta(db.collection("alertas").whereEqualTo("adminId", adminId))
        borrarConsulta(exColaboradores(adminId))
        progreso("Borrando empresa…")
        val codigo = empresas.document(adminId).get().await().getString("codigoAdministrador")
        val b = db.batch()
        if (!codigo.isNullOrBlank()) b.delete(codigos.document(codigo))
        b.delete(empresas.document(adminId))
        b.delete(usuarios.document(adminId))
        b.commit().await()
    }

    /**
     * El administrador elimina la cuenta de un colaborador de su equipo.
     * Se borra su perfil (datos personales y foto); si vuelve a entrar con su cuenta de Google, deberá registrarse desde cero.
     * Sin [borrarHistorial] sus marcajes se conservan en los reportes (registro de asistencia exigido por ley);
     * con [borrarHistorial] también se borran sus marcajes, justificaciones y alertas.
     * El acceso de Google (Firebase Authentication) solo puede borrarlo el propio usuario en el plan gratuito.
     */
    suspend fun eliminarColaboradorPorAdmin(u: Usuario, borrarHistorial: Boolean) {
        val adminId = u.adminId
        val b = db.batch()
        b.set(exColaboradores(adminId).document(u.uid), copiaExColaborador(u, "ELIMINADO_POR_ADMIN"))
        b.delete(usuarios.document(u.uid))
        b.commit().await()
        if (!borrarHistorial) return
        borrarConsulta(marcajes.whereEqualTo("adminId", adminId).whereEqualTo("colaboradorId", u.uid))
        borrarConsulta(justificaciones.whereEqualTo("adminId", adminId).whereEqualTo("colaboradorId", u.uid))
        borrarConsulta(db.collection("alertas").whereEqualTo("adminId", adminId).whereEqualTo("colaboradorId", u.uid))
        exColaboradores(adminId).document(u.uid).delete().await()
    }

    /** Borra todo lo que devuelve la consulta, un documento por escritura (las reglas consultan otros documentos y los lotes tienen tope). */
    private suspend fun borrarConsulta(q: com.google.firebase.firestore.Query) {
        while (true) {
            val docs = q.limit(100).get().await().documents
            if (docs.isEmpty()) return
            coroutineScope { docs.map { async { it.reference.delete().await() } }.awaitAll() }
        }
    }
}
