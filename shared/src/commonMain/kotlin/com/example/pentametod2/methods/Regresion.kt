package com.example.pentametod2.methods

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/** Una sumatoria de las que arman C y b, con su etiqueta para mostrarla (p. ej. "Σxi²"). */
data class Sumatoria(val etiqueta: String, val valor: Double)

data class ResultadoRegresion(
    val n: Int,
    /** a0, a1, ... */
    val coeficientes: DoubleArray,
    /** C = XᵀX */
    val matrizC: Array<DoubleArray>,
    /** b = Xᵀy */
    val vectorB: DoubleArray,
    val inversaC: Array<DoubleArray>,
    val sumas: List<Sumatoria>,
    val yAjustada: DoubleArray,
    val residuos: DoubleArray,
    /** Σ residuos² */
    val Q: Double,
    val mediaY: Double,
    /** Σ(yi − ȳ)² */
    val Syy: Double,
    val r2: Double,
    /** sqrt(Q/(n−p)); null si n == p. */
    val errorEstandar: Double?,
    /** Número de condición (norma 1) de C. */
    val condicion: Double,
    val advertencias: List<String>,
    /** Grado del polinomio, o null si es una regresión múltiple. */
    val grado: Int?,
    val r2Ajustado: Double? = null,
    val erroresCoeficientes: DoubleArray? = null,
    val estadisticosT: DoubleArray? = null,
    val valoresP: DoubleArray? = null,
    val estadisticoF: Double? = null,
    val valorPF: Double? = null,
    val vif: DoubleArray? = null,
) {
    /** Predicción de una regresión polinomial: a0 + a1·x + ... + ag·x^g. */
    fun predecir(x: Double): Double {
        if (grado == null) {
            throw MetodoNumericoException("Esta es una regresión múltiple: pasa un valor por cada variable independiente")
        }
        var s = 0.0
        for (k in coeficientes.indices.reversed()) s = s * x + coeficientes[k]
        return s
    }

    /** Predicción de una regresión múltiple: a0 + a1·x1 + ... + ak·xk. */
    fun predecir(x: DoubleArray): Double {
        if (grado != null) return if (x.size == 1) predecir(x[0]) else throw MetodoNumericoException("La regresión polinomial recibe un solo valor de x")
        if (x.size != coeficientes.size - 1) {
            throw MetodoNumericoException("Se esperaban ${coeficientes.size - 1} variables independientes y llegaron ${x.size}")
        }
        var s = coeficientes[0]
        for (j in x.indices) s += coeficientes[j + 1] * x[j]
        return s
    }
}

/** Ajuste por mínimos cuadrados resolviendo las ecuaciones normales (XᵀX)·a = Xᵀy. */
object Regresion {

    const val UMBRAL_CONDICION = 1e10

    fun regresionPolinomial(xs: DoubleArray, ys: DoubleArray, grado: Int): ResultadoRegresion {
        if (grado < 1) throw MetodoNumericoException("El grado debe ser al menos 1 (se recibió $grado)")
        if (xs.size != ys.size) {
            throw MetodoNumericoException("Hay ${xs.size} valores de x y ${ys.size} valores de y: deben ser la misma cantidad")
        }
        validarFinitos(xs, "x")
        validarFinitos(ys, "y")
        val p = grado + 1
        if (xs.size < p) {
            throw MetodoNumericoException("Se necesitan al menos $p puntos para grado $grado (hay ${xs.size})")
        }
        val distintos = xs.toSet().size
        if (distintos < p) {
            throw MetodoNumericoException("Se necesitan al menos $p valores de x distintos para grado $grado (hay $distintos)")
        }
        val columnas = List(p) { k -> DoubleArray(xs.size) { i -> xs[i].pow(k) } }
        return ajusteMinimosCuadrados(columnas, ys, grado, ::etiquetaPolinomialC, ::etiquetaPolinomialB)
    }

    /** x[i] = [x1i, x2i, ..., xki] (una fila por observación). */
    fun regresionMultiple(x: Array<DoubleArray>, y: DoubleArray): ResultadoRegresion {
        if (x.size != y.size) {
            throw MetodoNumericoException("Hay ${x.size} filas de variables independientes y ${y.size} valores de y: deben ser la misma cantidad")
        }
        if (x.isEmpty()) throw MetodoNumericoException("No hay datos")
        val k = x[0].size
        if (k < 1) throw MetodoNumericoException("Se necesita al menos una variable independiente")
        for ((i, fila) in x.withIndex()) {
            if (fila.size != k) {
                throw MetodoNumericoException("Todas las filas deben tener $k variables, pero la fila ${i + 1} tiene ${fila.size}")
            }
            validarFinitos(fila, "x (fila ${i + 1})")
        }
        validarFinitos(y, "y")
        val p = k + 1
        if (x.size < p) {
            throw MetodoNumericoException("Se necesitan al menos $p puntos para $k variables independientes (hay ${x.size})")
        }
        val columnas = List(p) { j -> DoubleArray(x.size) { i -> if (j == 0) 1.0 else x[i][j - 1] } }
        val base = ajusteMinimosCuadrados(columnas, y, null, ::etiquetaMultipleC, ::etiquetaMultipleB)
        val gl = x.size - k - 1
        val r2a = if (gl > 0) 1.0 - (1.0 - base.r2) * (x.size - 1.0) / gl else null
        val errores = if (gl > 0 && base.errorEstandar != null) DoubleArray(p) { j ->
            val v = base.errorEstandar * sqrt(base.inversaC[j][j].coerceAtLeast(0.0)); if (v.isFinite()) v else Double.NaN
        } else null
        val ts = errores?.let { DoubleArray(p) { j -> if (it[j] > 0.0) base.coeficientes[j] / it[j] else Double.NaN } }
        val ps = ts?.let { t -> DoubleArray(p) { j -> if (t[j].isFinite()) DistribucionT.pValorDosColas(kotlin.math.abs(t[j]), gl) else Double.NaN } }
        val ssr = base.Syy - base.Q
        val f = if (gl > 0 && k > 0 && base.Q > 0.0) (ssr / k) / (base.Q / gl) else null
        val pf = f?.takeIf { it >= 0.0 && it.isFinite() }?.let { DistribucionT.pValorF(it, k, gl) }
        val vif = calcularVif(x, k)
        val advertencias = base.advertencias.toMutableList()
        vif?.forEachIndexed { j, v -> if (v > 10.0) advertencias += "Alerta de multicolinealidad: VIF de x${j + 1} = ${v}" else if (v > 5.0) advertencias += "Advertencia de multicolinealidad: VIF de x${j + 1} = ${v}" }
        return base.copy(r2Ajustado = r2a, erroresCoeficientes = errores, estadisticosT = ts, valoresP = ps,
            estadisticoF = f, valorPF = pf, vif = vif, advertencias = advertencias)
    }

    /**
     * Motor común: X tiene las columnas dadas (la primera suele ser de unos).
     * Resuelve C·a = b con C = XᵀX, b = Xᵀy.
     */
    fun ajusteMinimosCuadrados(
        columnas: List<DoubleArray>,
        y: DoubleArray,
        grado: Int? = null,
        etiquetaC: (Int, Int) -> String = ::etiquetaMultipleC,
        etiquetaB: (Int) -> String = ::etiquetaMultipleB,
    ): ResultadoRegresion {
        val n = y.size
        val p = columnas.size
        if (p == 0) throw MetodoNumericoException("No hay columnas en la matriz de diseño")
        for (col in columnas) {
            if (col.size != n) throw MetodoNumericoException("Las columnas de la matriz de diseño deben tener $n elementos")
        }
        if (n < p) throw MetodoNumericoException("Se necesitan al menos $p puntos (hay $n)")

        val c = Array(p) { i -> DoubleArray(p) { j -> productoPunto(columnas[i], columnas[j]) } }
        val b = DoubleArray(p) { i -> productoPunto(columnas[i], y) }

        val a: DoubleArray
        val inversa: Array<DoubleArray>
        try {
            a = AlgebraLineal.resolverGauss(c, b)
            inversa = AlgebraLineal.invertir(c)
        } catch (_: MetodoNumericoException) {
            throw MetodoNumericoException(
                "Las variables independientes son linealmente dependientes: el sistema de ecuaciones normales no tiene solución única"
            )
        }
        val condicion = AlgebraLineal.norma1(c) * AlgebraLineal.norma1(inversa)

        val sumas = mutableListOf<Sumatoria>()
        val vistas = mutableSetOf<String>()
        for (i in 0 until p) {
            for (j in i until p) {
                val e = etiquetaC(i, j)
                if (vistas.add(e)) sumas.add(Sumatoria(e, c[i][j]))
            }
        }
        for (i in 0 until p) sumas.add(Sumatoria(etiquetaB(i), b[i]))

        val yAjustada = DoubleArray(n) { i ->
            var s = 0.0
            for (j in 0 until p) s += a[j] * columnas[j][i]
            s
        }
        val residuos = DoubleArray(n) { i -> y[i] - yAjustada[i] }
        val q = productoPunto(residuos, residuos)
        val media = y.sum() / n
        var syy = 0.0
        for (yi in y) syy += (yi - media) * (yi - media)
        val r2 = coeficienteDeterminacion(syy, q)

        val advertencias = mutableListOf<String>()
        if (condicion > UMBRAL_CONDICION) {
            advertencias.add(
                "El sistema está mal condicionado (número de condición ≈ ${formatoCientifico(condicion)}); " +
                    "los coeficientes pueden perder precisión. " + if (grado == null) "quita variables muy correlacionadas entre sí o agrega datos." else "Prueba con un grado menor o con datos centrados."
            )
        }

        return ResultadoRegresion(
            n = n, coeficientes = a, matrizC = c, vectorB = b, inversaC = inversa, sumas = sumas,
            yAjustada = yAjustada, residuos = residuos, Q = q, mediaY = media, Syy = syy, r2 = r2,
            errorEstandar = if (n > p) sqrt(q / (n - p)) else null,
            condicion = condicion, advertencias = advertencias, grado = grado,
        )
    }

    // ── auxiliares ──

    private fun calcularVif(x: Array<DoubleArray>, k: Int): DoubleArray? {
        val medias = DoubleArray(k) { j -> x.sumOf { it[j] } / x.size }
        val desv = DoubleArray(k) { j -> sqrt(x.sumOf { (it[j] - medias[j]) * (it[j] - medias[j]) }) }
        if (desv.any { it == 0.0 || !it.isFinite() }) return null
        val corr = Array(k) { i -> DoubleArray(k) { j ->
            x.indices.sumOf { t -> (x[t][i] - medias[i]) * (x[t][j] - medias[j]) } / (desv[i] * desv[j])
        } }
        return try { val inv = AlgebraLineal.invertir(corr); DoubleArray(k) { inv[it][it] } .takeIf { a -> a.all { it.isFinite() && it >= 0.0 } } }
        catch (_: Exception) { null }
    }

    private fun coeficienteDeterminacion(syy: Double, q: Double): Double {
        if (syy == 0.0) return if (abs(q) < 1e-12) 1.0 else 0.0
        return (syy - q) / syy
    }

    private fun productoPunto(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        for (i in a.indices) s += a[i] * b[i]
        return s
    }

    private fun validarFinitos(v: DoubleArray, nombre: String) {
        for ((i, x) in v.withIndex()) {
            if (!x.isFinite()) throw MetodoNumericoException("El valor ${i + 1} de $nombre no es un número válido")
        }
    }

    private fun etiquetaPolinomialC(i: Int, j: Int): String = if (i + j == 0) "n" else "Σxi${exponente(i + j)}"

    private fun etiquetaPolinomialB(i: Int): String = if (i == 0) "Σyi" else "Σxi${exponente(i)}yi"

    private fun etiquetaMultipleC(i: Int, j: Int): String = when {
        i == 0 && j == 0 -> "n"
        i == 0 -> "Σx${j}i"
        i == j -> "Σx${i}i²"
        else -> "Σx${i}i·x${j}i"
    }

    private fun etiquetaMultipleB(i: Int): String = if (i == 0) "Σyi" else "Σx${i}i·yi"

    private fun exponente(k: Int): String {
        if (k == 1) return ""
        val sup = "⁰¹²³⁴⁵⁶⁷⁸⁹"
        return k.toString().map { sup[it - '0'] }.joinToString("")
    }

    private fun formatoCientifico(x: Double): String {
        if (!x.isFinite()) return "∞"
        var e = 0
        var m = x
        while (m >= 10) { m /= 10; e++ }
        while (m < 1 && m > 0) { m *= 10; e-- }
        val mRedondeado = kotlin.math.round(m * 100) / 100
        return "${mRedondeado}e$e"
    }
}
