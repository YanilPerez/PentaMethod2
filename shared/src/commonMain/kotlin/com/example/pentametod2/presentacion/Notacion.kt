package com.example.pentametod2.presentacion

import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.Formato
import kotlin.math.abs

/** Utilidades de texto compartidas por los presentadores (subíndices, polinomios, formato). */
internal object Notacion {

    private const val SUB = "₀₁₂₃₄₅₆₇₈₉"
    private const val SUP = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    fun sub(n: Int): String = n.toString().map { SUB[it - '0'] }.joinToString("")

    fun sup(n: Int): String = n.toString().map { SUP[it - '0'] }.joinToString("")

    /** Exponente para escribir potencias: 1 → "", 2 → "²". */
    fun potencia(n: Int): String = if (n == 1) "" else sup(n)

    /** Resultado calculado (6 decimales, científica si es muy pequeño o muy grande). */
    fun num(v: Double): String = Formato.auto(v, 6)

    /** Dato de entrada, en su forma más corta. */
    fun dato(v: Double): String = Formato.corto(v)

    fun entreParentesisSiNegativo(s: String): String = if (s.startsWith("-")) "($s)" else s

    /** "(a − b)", con b negativo entre paréntesis: "(2.5 − (-1))". */
    fun resta(a: String, b: Double): String = "($a − ${entreParentesisSiNegativo(dato(b))})"

    /** Producto (x − x₀)(x − x₁)… de los primeros k nodos; "1" si k = 0. */
    fun factorNewton(variable: String, xs: DoubleArray, k: Int): String {
        if (k == 0) return "1"
        val sb = StringBuilder()
        for (j in 0 until k) sb.append(resta(variable, xs[j]))
        return sb.toString()
    }

    /**
     * Polinomio con signos limpios: c₀ + c₁·f₁ − |c₂|·f₂ ...
     * `factores[i]` vacío significa término constante.
     */
    fun polinomio(coeficientes: DoubleArray, factores: List<String>): String {
        val sb = StringBuilder()
        for (i in coeficientes.indices) {
            val c = coeficientes[i]
            val texto = num(abs(c)) + factores[i]
            if (i == 0) {
                sb.append(if (c < 0) "-$texto" else texto)
            } else {
                sb.append(if (c < 0) " − " else " + ").append(texto)
            }
        }
        return sb.toString()
    }

    fun vector(v: DoubleArray): String = v.joinToString(", ", "(", ")") { num(it) }

    fun asignaciones(variables: List<String>, v: DoubleArray): String =
        variables.indices.joinToString(", ") { "${variables[it]} = ${num(v[it])}" }

    fun matriz(a: Array<DoubleArray>): List<List<Double>> = a.map { it.toList() }

    fun columna(v: DoubleArray): List<List<Double>> = v.map { listOf(it) }

    fun celda(v: Double): Celda = if (v.isNaN()) Celda.Vacia else Celda.Num(v)

    fun estado(advertencias: List<String>): Estado = if (advertencias.isEmpty()) Estado.OK else Estado.ADVERTENCIA

    /** Agrega una fila de sumas (Σ en la primera columna) a filas numéricas. */
    fun conFilaDeSumas(primeraColumna: List<Celda>, valores: List<DoubleArray>): List<List<Celda>> {
        val filas = valores.mapIndexed { i, fila -> listOf(primeraColumna[i]) + fila.map { Celda.Num(it) } }
        val columnas = valores.firstOrNull()?.size ?: 0
        val sumas = DoubleArray(columnas)
        for (fila in valores) for (j in 0 until columnas) sumas[j] += fila[j]
        return filas + listOf(listOf<Celda>(Celda.Txt("Σ")) + sumas.map { Celda.Num(it) })
    }
}
