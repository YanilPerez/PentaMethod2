package com.example.pentametod2.methods

import kotlin.math.abs

data class ResultadoLagrange(
    val xs: DoubleArray,
    val ys: DoubleArray,
    val grado: Int,
    val x: Double,
    /** L[i] = Lᵢ(x) */
    val L: DoubleArray,
    /** terminos[i] = yᵢ·Lᵢ(x) */
    val terminos: DoubleArray,
    /** P(x) = Σ terminos */
    val valor: Double,
    val extrapolacion: Boolean,
    /** El polinomio completo, para graficarlo. */
    val evaluar: (Double) -> Double,
)

data class ResultadoNewton(
    val xs: DoubleArray,
    val ys: DoubleArray,
    val grado: Int,
    val x: Double,
    /** tabla[k][j] = f[xk, ..., xk+j]; los huecos de la parte triangular son NaN. */
    val tabla: Array<DoubleArray>,
    /** f[x0], f[x0,x1], ..., f[x0..xn] */
    val coeficientes: DoubleArray,
    /** terminos[k] = coeficientes[k]·(x−x0)···(x−xk−1) */
    val terminos: DoubleArray,
    val valor: Double,
    val extrapolacion: Boolean,
    val evaluar: (Double) -> Double,
)

/**
 * Interpolación polinomial. Lagrange y Newton usan EXACTAMENTE los nodos que reciben;
 * [seleccionarNodosCercanos] es solo una ayuda para elegirlos automáticamente.
 */
object Interpolacion {

    fun lagrange(xs: DoubleArray, ys: DoubleArray, x: Double): ResultadoLagrange {
        validarNodos(xs, ys)
        val nx = xs.copyOf()
        val ny = ys.copyOf()
        val l = DoubleArray(nx.size) { i -> baseLagrange(nx, i, x) }
        val terminos = DoubleArray(nx.size) { i -> ny[i] * l[i] }
        return ResultadoLagrange(
            xs = nx, ys = ny, grado = nx.size - 1, x = x,
            L = l, terminos = terminos, valor = terminos.sum(),
            extrapolacion = fueraDeRango(nx, x),
            evaluar = polinomioLagrange(nx, ny),
        )
    }

    fun newtonDiferenciasDivididas(xs: DoubleArray, ys: DoubleArray, x: Double): ResultadoNewton {
        validarNodos(xs, ys)
        val nx = xs.copyOf()
        val ny = ys.copyOf()
        val n = nx.size
        val tabla = Array(n) { k -> DoubleArray(n) { j -> if (j == 0) ny[k] else Double.NaN } }
        for (j in 1 until n) {
            for (k in 0 until n - j) {
                tabla[k][j] = (tabla[k + 1][j - 1] - tabla[k][j - 1]) / (nx[k + j] - nx[k])
            }
        }
        val coef = DoubleArray(n) { j -> tabla[0][j] }
        val terminos = DoubleArray(n)
        var producto = 1.0
        for (k in 0 until n) {
            terminos[k] = coef[k] * producto
            producto *= x - nx[k]
        }
        return ResultadoNewton(
            xs = nx, ys = ny, grado = n - 1, x = x,
            tabla = tabla, coeficientes = coef, terminos = terminos, valor = terminos.sum(),
            extrapolacion = fueraDeRango(nx, x),
            evaluar = polinomioNewton(nx, coef),
        )
    }

    /**
     * Los `cantidad` nodos más cercanos a x (en empate gana el de la izquierda), ordenados ascendentemente.
     */
    fun seleccionarNodosCercanos(xs: DoubleArray, ys: DoubleArray, x: Double, cantidad: Int): Pair<DoubleArray, DoubleArray> {
        validarNodos(xs, ys)
        if (cantidad < 1 || cantidad > xs.size) {
            throw MetodoNumericoException("Se pidieron $cantidad nodos, pero hay ${xs.size} disponibles")
        }
        val elegidos = xs.indices
            .sortedWith(compareBy<Int>({ abs(xs[it] - x) }, { xs[it] }))
            .take(cantidad)
            .sortedBy { xs[it] }
        return DoubleArray(cantidad) { xs[elegidos[it]] } to DoubleArray(cantidad) { ys[elegidos[it]] }
    }

    // ── auxiliares ──

    private fun baseLagrange(xs: DoubleArray, i: Int, x: Double): Double {
        var l = 1.0
        for (j in xs.indices) if (j != i) l *= (x - xs[j]) / (xs[i] - xs[j])
        return l
    }

    private fun polinomioLagrange(xs: DoubleArray, ys: DoubleArray): (Double) -> Double = { t ->
        var s = 0.0
        for (i in xs.indices) s += ys[i] * baseLagrange(xs, i, t)
        s
    }

    /** Evaluación tipo Horner de la forma de Newton. */
    private fun polinomioNewton(xs: DoubleArray, coef: DoubleArray): (Double) -> Double = { t ->
        var s = coef[coef.size - 1]
        for (k in coef.size - 2 downTo 0) s = s * (t - xs[k]) + coef[k]
        s
    }

    private fun fueraDeRango(xs: DoubleArray, x: Double): Boolean = x < xs.min() || x > xs.max()

    internal fun validarNodos(xs: DoubleArray, ys: DoubleArray) {
        if (xs.size != ys.size) {
            throw MetodoNumericoException("Hay ${xs.size} valores de x y ${ys.size} valores de y: deben ser la misma cantidad")
        }
        if (xs.size < 2) throw MetodoNumericoException("Se necesitan al menos 2 nodos para interpolar")
        for (i in xs.indices) {
            if (!xs[i].isFinite() || !ys[i].isFinite()) {
                throw MetodoNumericoException("El nodo ${i + 1} tiene un valor no numérico o infinito")
            }
            for (j in 0 until i) {
                if (xs[i] == xs[j]) {
                    throw MetodoNumericoException("El valor x = ${xs[i]} está repetido (nodos ${j + 1} y ${i + 1}); los nodos deben ser distintos")
                }
            }
        }
    }
}
