package com.example.pentametod2.methods

import com.example.pentametod2.parser.EvaluadorFuncion
import com.example.pentametod2.parser.FuncionInvalidaException
import kotlin.math.abs
import kotlin.math.max

data class IteracionSistema(val k: Int, val x: DoubleArray, val error: Double?)

/** Lo que Newton calculó en la iteración k para pasar de x(k−1) a x(k). */
data class PasoNewton(
    val k: Int,
    /** x(k−1), el punto donde se evaluaron F y J. */
    val x: DoubleArray,
    val fx: DoubleArray,
    val jacobiano: Array<DoubleArray>,
    /** Δ que resuelve J·Δ = −F; null si J fue singular. */
    val delta: DoubleArray?,
)

data class ResultadoSistema(
    val metodo: String,
    val variables: List<String>,
    val convergio: Boolean,
    val iteraciones: List<IteracionSistema>,
    val solucion: DoubleArray?,
    val residual: Double?,
    val mensaje: String,
    /** Solo Newton: F(x) y Jacobiano de cada iteración. */
    val detalle: List<PasoNewton> = emptyList(),
)

/**
 * Sistemas de ecuaciones no lineales. Error de la iteración k: E = ‖x(k) − x(k−1)‖∞.
 * Se detiene cuando E < tol o al llegar a maxIter.
 */
object SistemasNoLineales {

    const val TOL_DEFECTO = 1e-6
    const val MAX_ITER_DEFECTO = 50
    private const val LIMITE_DIVERGENCIA = 1e12

    /**
     * Tolerancia relativa de pivote para el Jacobiano numérico: las diferencias centrales con h ≈ 6e-6
     * tienen un error de redondeo relativo ≈ 1e-11, así que pivotes por debajo de 1e-9·escala son ruido.
     */
    private const val TOL_PIVOTE_JACOBIANO = 1e-9

    // ── Punto fijo ──

    fun puntoFijoSistema(
        g: (DoubleArray) -> DoubleArray,
        x0: DoubleArray,
        tol: Double = TOL_DEFECTO,
        maxIter: Int = MAX_ITER_DEFECTO,
        variables: List<String> = nombresPorDefecto(x0.size),
    ): ResultadoSistema {
        validarParametros(x0, tol, maxIter, variables)
        val metodo = "Punto fijo"
        val iteraciones = mutableListOf(IteracionSistema(0, x0.copyOf(), null))
        var x = x0.copyOf()
        for (k in 1..maxIter) {
            val nuevo = evaluar(g, x, "G")
            val error = AlgebraLineal.normaInfDiferencia(nuevo, x)
            iteraciones.add(IteracionSistema(k, nuevo, error))
            if (divergio(nuevo)) {
                return fallo(metodo, variables, iteraciones, "El método diverge en la iteración $k: los valores crecen sin límite o dejan de ser números. Prueba otra G o otro punto inicial.")
            }
            x = nuevo
            if (error < tol) {
                val residual = AlgebraLineal.normaInfDiferencia(evaluar(g, x, "G"), x)
                return ResultadoSistema(metodo, variables, true, iteraciones, x.copyOf(), residual, "Convergió en $k iteraciones (E < $tol)")
            }
        }
        return fallo(metodo, variables, iteraciones, "No convergió en $maxIter iteraciones (último error ${iteraciones.last().error}). Prueba con más iteraciones u otro punto inicial.")
    }

    fun puntoFijoSistema(
        expresiones: List<String>,
        variables: List<String>,
        x0: DoubleArray,
        tol: Double = TOL_DEFECTO,
        maxIter: Int = MAX_ITER_DEFECTO,
    ): ResultadoSistema {
        val g = compilarSistema(expresiones, variables, x0, "G")
        return puntoFijoSistema(g, x0, tol, maxIter, variables)
    }

    // ── Newton ──

    fun newtonSistema(
        f: (DoubleArray) -> DoubleArray,
        x0: DoubleArray,
        tol: Double = TOL_DEFECTO,
        maxIter: Int = MAX_ITER_DEFECTO,
        variables: List<String> = nombresPorDefecto(x0.size),
    ): ResultadoSistema {
        validarParametros(x0, tol, maxIter, variables)
        val metodo = "Newton"
        val iteraciones = mutableListOf(IteracionSistema(0, x0.copyOf(), null))
        val detalle = mutableListOf<PasoNewton>()
        var x = x0.copyOf()
        for (k in 1..maxIter) {
            val fx = evaluar(f, x, "F")
            if (divergio(fx)) {
                return fallo(metodo, variables, iteraciones, "El método diverge en la iteración $k: F(x) dejó de ser un número finito.", detalle)
            }
            val j = jacobianoNumerico(f, x, fx.size)
            val menosF = DoubleArray(fx.size) { -fx[it] }
            val delta = try {
                AlgebraLineal.resolverGauss(j, menosF, TOL_PIVOTE_JACOBIANO)
            } catch (_: MetodoNumericoException) {
                detalle.add(PasoNewton(k, x.copyOf(), fx, j, null))
                return fallo(metodo, variables, iteraciones, "El Jacobiano es singular en la iteración $k (x = ${x.toList()}): no se puede resolver J·Δ = −F. Prueba otro punto inicial.", detalle)
            }
            detalle.add(PasoNewton(k, x.copyOf(), fx, j, delta))
            val nuevo = DoubleArray(x.size) { x[it] + delta[it] }
            val error = AlgebraLineal.normaInf(delta)
            iteraciones.add(IteracionSistema(k, nuevo, error))
            if (divergio(nuevo)) {
                return fallo(metodo, variables, iteraciones, "El método diverge en la iteración $k: los valores crecen sin límite o dejan de ser números.", detalle)
            }
            x = nuevo
            if (error < tol) {
                val residual = AlgebraLineal.normaInf(evaluar(f, x, "F"))
                return ResultadoSistema(metodo, variables, true, iteraciones, x.copyOf(), residual, "Convergió en $k iteraciones (E < $tol)", detalle)
            }
        }
        return fallo(metodo, variables, iteraciones, "No convergió en $maxIter iteraciones (último error ${iteraciones.last().error}). Prueba con más iteraciones u otro punto inicial.", detalle)
    }

    /** Las expresiones son F_i(x) = 0, ya igualadas a cero. */
    fun newtonSistema(
        expresiones: List<String>,
        variables: List<String>,
        x0: DoubleArray,
        tol: Double = TOL_DEFECTO,
        maxIter: Int = MAX_ITER_DEFECTO,
    ): ResultadoSistema {
        val f = compilarSistema(expresiones, variables, x0, "F")
        return newtonSistema(f, x0, tol, maxIter, variables)
    }

    /** Jacobiano por diferencias centrales: h_j = 6e-6 · max(1, |x_j|). */
    fun jacobianoNumerico(f: (DoubleArray) -> DoubleArray, x: DoubleArray, m: Int = x.size): Array<DoubleArray> {
        val jac = Array(m) { DoubleArray(x.size) }
        for (j in x.indices) {
            val h = 6e-6 * max(1.0, abs(x[j]))
            val mas = x.copyOf().also { it[j] += h }
            val menos = x.copyOf().also { it[j] -= h }
            val fMas = evaluar(f, mas, "F")
            val fMenos = evaluar(f, menos, "F")
            val paso = mas[j] - menos[j] // el paso realmente representable
            for (i in 0 until m) jac[i][j] = (fMas[i] - fMenos[i]) / paso
        }
        return jac
    }

    // ── auxiliares ──

    private fun compilarSistema(expresiones: List<String>, variables: List<String>, x0: DoubleArray, nombre: String): (DoubleArray) -> DoubleArray {
        if (expresiones.size != variables.size || variables.size != x0.size) {
            throw MetodoNumericoException(
                "El sistema debe ser cuadrado: hay ${expresiones.size} expresiones, ${variables.size} variables y ${x0.size} valores iniciales; deben ser la misma cantidad"
            )
        }
        val funciones = expresiones.mapIndexed { i, texto ->
            try {
                EvaluadorFuncion.compilarMulti(texto, variables)
            } catch (e: FuncionInvalidaException) {
                throw MetodoNumericoException("Error en $nombre${i + 1}: ${e.message}")
            }
        }
        return vectorial(funciones)
    }

    private fun vectorial(funciones: List<(DoubleArray) -> Double>): (DoubleArray) -> DoubleArray =
        { x -> DoubleArray(funciones.size) { funciones[it](x) } }

    private fun evaluar(f: (DoubleArray) -> DoubleArray, x: DoubleArray, nombre: String): DoubleArray {
        val r = f(x.copyOf())
        if (r.size != x.size) {
            throw MetodoNumericoException("$nombre devuelve ${r.size} valores pero hay ${x.size} variables")
        }
        return r
    }

    private fun divergio(v: DoubleArray): Boolean = v.any { !it.isFinite() || abs(it) > LIMITE_DIVERGENCIA }

    private fun fallo(
        metodo: String,
        variables: List<String>,
        iteraciones: List<IteracionSistema>,
        mensaje: String,
        detalle: List<PasoNewton> = emptyList(),
    ) = ResultadoSistema(metodo, variables, false, iteraciones, null, null, mensaje, detalle)

    private fun nombresPorDefecto(n: Int): List<String> = List(n) { "x${it + 1}" }

    private fun validarParametros(x0: DoubleArray, tol: Double, maxIter: Int, variables: List<String>) {
        if (x0.isEmpty()) throw MetodoNumericoException("Falta el punto inicial")
        if (variables.size != x0.size) {
            throw MetodoNumericoException("Hay ${variables.size} nombres de variable y ${x0.size} valores iniciales")
        }
        if (x0.any { !it.isFinite() }) throw MetodoNumericoException("El punto inicial tiene valores no numéricos")
        if (!(tol > 0.0)) throw MetodoNumericoException("La tolerancia debe ser mayor que cero")
        if (maxIter < 1) throw MetodoNumericoException("El número máximo de iteraciones debe ser al menos 1")
    }
}
