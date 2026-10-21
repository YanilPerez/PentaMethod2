package com.example.pentametod2.methods

import kotlin.math.abs
import kotlin.math.max

/**
 * Operaciones de álgebra lineal sobre matrices `Array<DoubleArray>` (fila por fila).
 * Ninguna función modifica sus argumentos.
 */
object AlgebraLineal {

    /** Tolerancia relativa por defecto para decidir que un pivote es cero. */
    const val TOL_PIVOTE = 1e-12

    /**
     * Resuelve A·x = b por eliminación gaussiana con pivoteo parcial y sustitución regresiva.
     * Un pivote con |pivote| < tolRelativa · max|aij| se considera cero (matriz singular).
     */
    fun resolverGauss(a: Array<DoubleArray>, b: DoubleArray, tolRelativa: Double = TOL_PIVOTE): DoubleArray {
        val n = validarCuadrada(a)
        if (b.size != n) {
            throw MetodoNumericoException("El vector b tiene ${b.size} elementos pero la matriz es de ${n}x$n")
        }
        val m = copiar(a)
        val v = b.copyOf()
        val umbral = tolRelativa * escala(a)

        for (k in 0 until n) {
            val p = filaPivote(m, k)
            if (abs(m[p][k]) <= umbral) throw singular(k)
            intercambiar(m, k, p)
            v[k] = v[p].also { v[p] = v[k] }
            for (i in k + 1 until n) {
                val factor = m[i][k] / m[k][k]
                if (factor == 0.0) continue
                for (j in k until n) m[i][j] -= factor * m[k][j]
                v[i] -= factor * v[k]
            }
        }

        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var suma = v[i]
            for (j in i + 1 until n) suma -= m[i][j] * x[j]
            x[i] = suma / m[i][i]
        }
        return x
    }

    /** Inversa por Gauss-Jordan con pivoteo parcial. Lanza MetodoNumericoException si es singular. */
    fun invertir(a: Array<DoubleArray>): Array<DoubleArray> {
        val n = validarCuadrada(a)
        val m = copiar(a)
        val inv = identidad(n)
        val umbral = TOL_PIVOTE * escala(a)

        for (k in 0 until n) {
            val p = filaPivote(m, k)
            if (abs(m[p][k]) <= umbral) throw singular(k)
            intercambiar(m, k, p)
            intercambiar(inv, k, p)
            val piv = m[k][k]
            for (j in 0 until n) {
                m[k][j] /= piv
                inv[k][j] /= piv
            }
            for (i in 0 until n) {
                if (i == k) continue
                val factor = m[i][k]
                if (factor == 0.0) continue
                for (j in 0 until n) {
                    m[i][j] -= factor * m[k][j]
                    inv[i][j] -= factor * inv[k][j]
                }
            }
        }
        return inv
    }

    fun multiplicar(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val colsA = validarRectangular(a, "A")
        val colsB = validarRectangular(b, "B")
        if (colsA != b.size) {
            throw MetodoNumericoException("No se pueden multiplicar: A tiene $colsA columnas y B tiene ${b.size} filas")
        }
        return Array(a.size) { i ->
            DoubleArray(colsB) { j ->
                var s = 0.0
                for (k in 0 until colsA) s += a[i][k] * b[k][j]
                s
            }
        }
    }

    fun transpuesta(a: Array<DoubleArray>): Array<DoubleArray> {
        val cols = validarRectangular(a, "A")
        return Array(cols) { j -> DoubleArray(a.size) { i -> a[i][j] } }
    }

    fun multiplicarMatrizVector(a: Array<DoubleArray>, v: DoubleArray): DoubleArray {
        val cols = validarRectangular(a, "A")
        if (cols != v.size) {
            throw MetodoNumericoException("No se puede multiplicar: la matriz tiene $cols columnas y el vector ${v.size} elementos")
        }
        return DoubleArray(a.size) { i ->
            var s = 0.0
            for (j in 0 until cols) s += a[i][j] * v[j]
            s
        }
    }

    fun normaInf(v: DoubleArray): Double {
        var m = 0.0
        for (x in v) m = max(m, abs(x))
        return m
    }

    fun normaInfDiferencia(a: DoubleArray, b: DoubleArray): Double {
        if (a.size != b.size) {
            throw MetodoNumericoException("Los vectores tienen tamaños distintos (${a.size} y ${b.size})")
        }
        var m = 0.0
        for (i in a.indices) m = max(m, abs(a[i] - b[i]))
        return m
    }

    /** Norma 1 de una matriz: máxima suma de valores absolutos por columna. */
    fun norma1(a: Array<DoubleArray>): Double {
        val cols = validarRectangular(a, "A")
        var m = 0.0
        for (j in 0 until cols) {
            var s = 0.0
            for (fila in a) s += abs(fila[j])
            m = max(m, s)
        }
        return m
    }

    /** Número de condición en norma 1: ‖A‖₁·‖A⁻¹‖₁. Infinito si A es singular. */
    fun numeroCondicion(a: Array<DoubleArray>): Double {
        val inv = try {
            invertir(a)
        } catch (_: MetodoNumericoException) {
            return Double.POSITIVE_INFINITY
        }
        return norma1(a) * norma1(inv)
    }

    fun identidad(n: Int): Array<DoubleArray> = Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }

    // ── auxiliares ──

    private fun copiar(a: Array<DoubleArray>): Array<DoubleArray> = Array(a.size) { a[it].copyOf() }

    private fun escala(a: Array<DoubleArray>): Double {
        var m = 0.0
        for (fila in a) for (x in fila) m = max(m, abs(x))
        return m
    }

    private fun filaPivote(m: Array<DoubleArray>, k: Int): Int {
        var p = k
        for (i in k + 1 until m.size) if (abs(m[i][k]) > abs(m[p][k])) p = i
        return p
    }

    private fun intercambiar(m: Array<DoubleArray>, i: Int, j: Int) {
        if (i != j) m[i] = m[j].also { m[j] = m[i] }
    }

    private fun singular(k: Int) =
        MetodoNumericoException("La matriz es singular (no tiene inversa): no hay pivote distinto de cero en la columna ${k + 1}")

    private fun validarCuadrada(a: Array<DoubleArray>): Int {
        val n = a.size
        if (n == 0) throw MetodoNumericoException("La matriz está vacía")
        for ((i, fila) in a.withIndex()) {
            if (fila.size != n) {
                throw MetodoNumericoException("La matriz debe ser cuadrada: tiene $n filas pero la fila ${i + 1} tiene ${fila.size} columnas")
            }
        }
        return n
    }

    private fun validarRectangular(a: Array<DoubleArray>, nombre: String): Int {
        if (a.isEmpty()) throw MetodoNumericoException("La matriz $nombre está vacía")
        val cols = a[0].size
        for ((i, fila) in a.withIndex()) {
            if (fila.size != cols) {
                throw MetodoNumericoException("Las filas de $nombre no tienen el mismo tamaño (la fila ${i + 1} tiene ${fila.size} y la primera $cols)")
            }
        }
        return cols
    }
}
