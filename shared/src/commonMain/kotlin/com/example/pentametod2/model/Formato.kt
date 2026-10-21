package com.example.pentametod2.model

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Formato de números sin String.format (Kotlin común).
 * Redondeo half-up (0.5 → 1, −0.5 → −1), tolerante al error de representación binaria
 * (2.675 con 2 decimales da 2.68, como en Excel). El cero negativo se escribe sin signo.
 */
object Formato {

    fun fijo(v: Double, decimales: Int): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return if (v > 0) "∞" else "-∞"
        val d = decimales.coerceIn(0, 15)
        val escalado = abs(v) * 10.0.pow(d)
        if (escalado >= 9e15) return v.toString() // fuera del rango exacto de Long
        var r = floor(escalado)
        if (escalado - r + 1e-9 >= 0.5) r += 1.0
        val n = r.toLong()
        if (n == 0L) return if (d == 0) "0" else "0." + "0".repeat(d)
        val signo = if (v < 0) "-" else ""
        if (d == 0) return signo + n.toString()
        val potencia = potencia10(d)
        val entero = n / potencia
        val frac = (n % potencia).toString().padStart(d, '0')
        return "$signo$entero.$frac"
    }

    /** Notación científica con `cifras` decimales en la mantisa: cientifica(2.8258e-7, 4) = "2.8258e-7". */
    fun cientifica(v: Double, cifras: Int): String {
        if (!v.isFinite()) return fijo(v, 0)
        if (v == 0.0) return fijo(0.0, cifras)
        var e = floor(log10(abs(v))).toInt()
        var mantisa = fijo(abs(v) / 10.0.pow(e), cifras)
        if (mantisa.startsWith("10")) {
            e++
            mantisa = fijo(abs(v) / 10.0.pow(e), cifras)
        }
        val signo = if (v < 0) "-" else ""
        return "$signo${mantisa}e$e"
    }

    /** Fijo, salvo que 0 < |v| < 1e-4 o |v| ≥ 1e9: entonces científica. */
    fun auto(v: Double, decimales: Int = 6): String =
        if (usaCientifica(v)) cientifica(v, decimales) else fijo(v, decimales)

    /** porcentaje(0.974265, 2) = "97.43%" */
    fun porcentaje(fraccion: Double, decimales: Int = 2): String = fijo(fraccion * 100, decimales) + "%"

    /**
     * Forma más corta y legible para datos de entrada (nodos, x, tolerancia): hasta 10 decimales
     * sin ceros a la derecha. corto(2.5) = "2.5", corto(0.5103757) = "0.5103757", corto(1e-8) = "1e-8".
     */
    fun corto(v: Double): String {
        if (!v.isFinite()) return fijo(v, 0)
        if (usaCientifica(v)) {
            val txt = cientifica(v, 6)
            val i = txt.indexOf('e')
            return quitarCeros(txt.substring(0, i)) + txt.substring(i)
        }
        val digitosEnteros = if (abs(v) < 1) 1 else floor(log10(abs(v))).toInt() + 1
        val d = (15 - digitosEnteros).coerceIn(0, 10)
        return quitarCeros(fijo(v, d))
    }

    private fun usaCientifica(v: Double): Boolean {
        val a = abs(v)
        return v.isFinite() && ((a > 0 && a < 1e-4) || a >= 1e9)
    }

    private fun quitarCeros(s: String): String {
        if (!s.contains('.')) return s
        return s.trimEnd('0').trimEnd('.')
    }

    private fun potencia10(d: Int): Long {
        var p = 1L
        repeat(d) { p *= 10 }
        return p
    }
}
