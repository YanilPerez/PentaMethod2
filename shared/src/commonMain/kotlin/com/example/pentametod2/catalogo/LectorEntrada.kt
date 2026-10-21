package com.example.pentametod2.catalogo

import com.example.pentametod2.methods.MetodoNumericoException

/**
 * Convierte los textos del formulario en números, con mensajes que nombran el campo.
 * Listas: separadas por `,` `;` espacios, tabulaciones o saltos de línea. Separador decimal: punto.
 */
internal object LectorEntrada {

    private val SEPARADORES = Regex("[,;\\s]+")
    private val NUMERO = Regex("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?")
    private val ENTERO = Regex("[+-]?\\d+")
    private val VARIABLE = Regex("[A-Za-z]+[0-9]*")

    const val TOL_DEFECTO = 1e-6
    const val MAX_ITER_DEFECTO = 50

    // ── Límites (ver el reporte de la Fase 4A) ──

    /** Iteraciones: cada una se muestra en la tabla y, en Newton, como un grupo de detalle. */
    const val MAX_ITER_LIMITE = 500

    /** Datos por lista (interpolación y regresión) y filas de la regresión múltiple. */
    const val MAX_DATOS = 200

    /** Variables de un sistema y variables independientes de la regresión múltiple. */
    const val MAX_VARIABLES = 6

    const val MAX_GRADO_REGRESION = 10

    /** Grado del polinomio interpolante (automático y manual: como mucho 13 nodos). */
    const val MAX_GRADO_INTERPOLACION = 12

    const val MAX_LARGO_ECUACION = 200

    /** |valor| máximo de un dato: con grado 10, x²⁰ ≤ 1e240 sigue siendo representable. */
    const val MAX_MAGNITUD = 1e12

    private const val TEXTO_MAGNITUD = "usa valores entre -1e12 y 1e12"

    fun partes(texto: String): List<String> = texto.trim().split(SEPARADORES).filter { it.isNotEmpty() }

    /** `campo` en plural, p. ej. "Los nodos x". */
    fun numeros(texto: String, campo: String, maximo: Int = MAX_DATOS): DoubleArray {
        val tokens = partes(texto)
        if (tokens.isEmpty()) throw MetodoNumericoException("$campo: escribe al menos un valor")
        if (tokens.size > maximo) throw MetodoNumericoException("$campo: hay ${tokens.size} valores; el máximo es $maximo")
        return DoubleArray(tokens.size) { i -> convertir(tokens[i], campo, plural = true) }
    }

    /** `campo` en singular, p. ej. "El valor de x". */
    fun numero(texto: String, campo: String): Double {
        val t = texto.trim()
        if (t.isEmpty()) throw MetodoNumericoException("$campo: falta escribir el valor")
        return convertir(t, campo, plural = false)
    }

    fun numeroOpcional(texto: String, campo: String): Double? = if (texto.isBlank()) null else numero(texto, campo)

    fun entero(texto: String, campo: String, minimo: Int): Int {
        val t = texto.trim()
        if (t.isEmpty()) throw MetodoNumericoException("$campo: falta escribir el valor")
        if (!ENTERO.matches(t)) throw MetodoNumericoException("$campo debe ser un número entero; '${recortar(t)}' no lo es")
        val n = t.toIntOrNull() ?: throw MetodoNumericoException("$campo es demasiado grande")
        if (n < minimo) throw MetodoNumericoException("$campo debe ser al menos $minimo (se escribió $n)")
        return n
    }

    /** Entero en [minimo, maximo] (p. ej. el grado). */
    fun entero(texto: String, campo: String, minimo: Int, maximo: Int): Int {
        val n = entero(texto, campo, minimo)
        if (n > maximo) throw MetodoNumericoException("$campo no puede pasar de $maximo (se escribió $n)")
        return n
    }

    fun tolerancia(texto: String): Double {
        if (texto.isBlank()) return TOL_DEFECTO
        val tol = numero(texto, "La tolerancia")
        if (tol <= 0.0) throw MetodoNumericoException("La tolerancia debe ser un número mayor que cero, por ejemplo 1e-6")
        return tol
    }

    fun maxIter(texto: String): Int {
        if (texto.isBlank()) return MAX_ITER_DEFECTO
        val n = entero(texto, "El número máximo de iteraciones", 1)
        if (n > MAX_ITER_LIMITE) throw MetodoNumericoException("El número máximo de iteraciones no puede pasar de $MAX_ITER_LIMITE")
        return n
    }

    fun variables(texto: String): List<String> {
        val nombres = partes(texto)
        if (nombres.isEmpty()) throw MetodoNumericoException("Las variables: escribe sus nombres separados por comas, por ejemplo: x, y")
        if (nombres.size > MAX_VARIABLES) {
            throw MetodoNumericoException("Hay ${nombres.size} variables; el máximo es $MAX_VARIABLES")
        }
        for (v in nombres) {
            if (!VARIABLE.matches(v)) {
                throw MetodoNumericoException("Las variables deben ser letras seguidas opcionalmente de dígitos (x, y, x1); '$v' no es válida")
            }
        }
        val repetida = nombres.groupBy { it.lowercase() }.entries.firstOrNull { it.value.size > 1 }
        if (repetida != null) throw MetodoNumericoException("La variable '${repetida.key}' está repetida")
        return nombres
    }

    /** Convierte un token o lanza un error que nombra el campo. */
    private fun convertir(t: String, campo: String, plural: Boolean): Double {
        val v = if (NUMERO.matches(t)) t.toDoubleOrNull() else null
        if (v == null) {
            val esperado = if (plural) "deben ser números separados por comas" else "debe ser un número"
            throw MetodoNumericoException("$campo $esperado (con punto decimal); '${recortar(t)}' no es un número")
        }
        if (!v.isFinite() || kotlin.math.abs(v) > MAX_MAGNITUD) {
            throw MetodoNumericoException("$campo: '${recortar(t)}' es demasiado grande; $TEXTO_MAGNITUD")
        }
        return v
    }

    /** Evita repetir en el mensaje un texto de miles de caracteres. */
    fun recortar(t: String, max: Int = 30): String = if (t.length <= max) t else t.take(max) + "…"
}
