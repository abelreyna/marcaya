package com.juanreyna.registrotiempo.domain.model

/** Documento de identidad aceptado en Perú, con la cantidad exacta de dígitos. */
enum class TipoDocumento(val etiqueta: String, val nombreLargo: String, val nombreCorto: String, val digitos: Int) {
    DNI("DNI", "Documento Nacional de Identidad", "DNI", 8),
    CE("C.E.", "Carnet de Extranjería", "Carnet de Extranjería", 9);

    fun esValido(numero: String) = numero.length == digitos && numero.all(Char::isDigit)

    /** Deja solo dígitos y corta en la cantidad exacta del documento. */
    fun limpiar(texto: String) = texto.filter(Char::isDigit).take(digitos)
}

/** Celular peruano: 9 dígitos y empieza en 9 (sin +51). */
object Celular {
    const val DIGITOS = 9
    fun limpiar(texto: String) = texto.filter(Char::isDigit).let { if (it.startsWith("51") && it.length > DIGITOS) it.drop(2) else it }.take(DIGITOS)
    fun esValido(numero: String) = numero.length == DIGITOS && numero.first() == '9' && numero.all(Char::isDigit)
}
