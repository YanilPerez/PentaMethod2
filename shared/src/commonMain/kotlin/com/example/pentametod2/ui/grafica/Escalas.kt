package com.example.pentametod2.ui.grafica

import com.example.pentametod2.model.Formato
import com.example.pentametod2.model.Grafica
import com.example.pentametod2.model.Punto
import com.example.pentametod2.model.Serie
import com.example.pentametod2.model.TipoSerie
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/** Intervalo de un eje. En un eje logarítmico, min y max son exponentes (log₁₀). */
data class Rango(val min: Double, val max: Double) {
    val ancho: Double get() = max - min
}

const val MARGEN_EJES = 0.06
private const val EPS = 1e-9

/**
 * Paso "bonito" (1, 2 o 5 × 10ⁿ) para que haya como mucho `maxTicks` marcas en `ancho`.
 */
fun pasoBonito(ancho: Double, maxTicks: Int): Double {
    val bruto = ancho / max(1, maxTicks - 1)
    if (!(bruto > 0) || !bruto.isFinite()) return 1.0
    val magnitud = 10.0.pow(floor(log10(bruto)))
    val n = bruto / magnitud
    val factor = when {
        n <= 1.0 + EPS -> 1.0
        n <= 2.0 + EPS -> 2.0
        n <= 5.0 + EPS -> 5.0
        else -> 10.0
    }
    return factor * magnitud
}

/** Marcas en múltiplos del paso bonito dentro de [min, max]. ticksBonitos(0, 1, 6) = 0, 0.2, …, 1. */
fun ticksBonitos(min: Double, max: Double, maxTicks: Int): List<Double> {
    if (!(max > min) || !min.isFinite() || !max.isFinite()) return listOf(min)
    val paso = pasoBonito(max - min, maxTicks)
    val k0 = ceil(min / paso - EPS).toLong()
    val k1 = floor(max / paso + EPS).toLong()
    val ticks = ArrayList<Double>()
    for (k in k0..k1) {
        val v = k * paso
        ticks += if (abs(v) < paso * EPS) 0.0 else v
    }
    return ticks
}

/**
 * Rango que cubre [min, max] con un `margen` (fracción del ancho) por lado.
 * Rango degenerado (min == max): ±10 % del valor si |v| > 10; si no, ±1.
 * Datos no finitos: [0, 1].
 */
fun rangoConMargen(min: Double, max: Double, margen: Double = MARGEN_EJES): Rango {
    if (!min.isFinite() || !max.isFinite()) return Rango(0.0, 1.0)
    val lo = minOf(min, max)
    val hi = maxOf(min, max)
    val ancho = hi - lo
    if (ancho <= abs(hi) * 1e-12 || ancho == 0.0) {
        val d = if (abs(lo) > 10) abs(lo) * 0.1 else 1.0
        return Rango(lo - d, hi + d)
    }
    return Rango(lo - ancho * margen, hi + ancho * margen)
}

/**
 * Eje logarítmico: el rango (en exponentes) se amplía a décadas completas, con al menos una década,
 * más el margen. Requiere 0 < min ≤ max.
 */
fun rangoLog(min: Double, max: Double, margen: Double = MARGEN_EJES): Rango {
    var lo = floor(log10(min) + EPS)
    var hi = ceil(log10(max) - EPS)
    if (hi - lo < 1) hi = lo + 1
    val m = (hi - lo) * margen
    lo -= m
    hi += m
    return Rango(lo, hi)
}

/**
 * Marcas de un eje logarítmico en potencias de 10 dentro de [min, max] (valores, no exponentes).
 * Regla: una marca cada `k` décadas, con el menor k tal que haya como mucho `maxTicks` marcas;
 * las marcas caen en exponentes múltiplos de k. (1e-8, 1, 6) → 1e-8, 1e-6, 1e-4, 1e-2, 1.
 */
fun ticksLog(min: Double, max: Double, maxTicks: Int): List<Double> = exponentesLog(min, max, maxTicks).map { 10.0.pow(it) }

fun exponentesLog(min: Double, max: Double, maxTicks: Int): List<Int> {
    if (!(min > 0) || !(max >= min)) return emptyList()
    val e0 = ceil(log10(min) - EPS).toInt()
    val e1 = floor(log10(max) + EPS).toInt()
    if (e1 < e0) return emptyList()
    val n = e1 - e0 + 1
    val k = max(1, ceil(n.toDouble() / max(1, maxTicks)).toInt())
    val inicio = ceil(e0.toDouble() / k).toInt() * k
    return (inicio..e1 step k).toList()
}

/** Transformación lineal dato → píxel (para Y, pasar desde = abajo y hasta = arriba). */
fun aPixel(v: Double, rango: Rango, desde: Float, hasta: Float): Float =
    desde + ((v - rango.min) / rango.ancho * (hasta - desde)).toFloat()

/** Transformación logarítmica (rango en exponentes). Valores ≤ 0 → NaN. */
fun aPixelLog(v: Double, rangoExp: Rango, desde: Float, hasta: Float): Float =
    if (v > 0) aPixel(log10(v), rangoExp, desde, hasta) else Float.NaN

/** Etiqueta de una marca lineal con los decimales que pide el paso (0.2 → "0.2", 20 → "20"). */
fun etiquetaTick(v: Double, paso: Double): String {
    val decimales = if (paso >= 1) 0 else (-floor(log10(paso) + EPS)).toInt().coerceIn(0, 10)
    return Formato.auto(v, decimales)
}

/** Etiqueta de una década con superíndices: 10⁻⁸, 10⁻², 10⁰, 10³. */
fun etiquetaDecada(exponente: Int): String = "10" + superindice(exponente)

private const val SUPER = "⁰¹²³⁴⁵⁶⁷⁸⁹"
private const val SUB = "₀₁₂₃₄₅₆₇₈₉"

/** -2 → "⁻²" */
fun superindice(n: Int): String = (if (n < 0) "⁻" else "") + kotlin.math.abs(n).toString().map { SUPER[it - '0'] }.joinToString("")

/** 7 → "₇" */
fun subindice(n: Int): String = n.toString().map { SUB[it - '0'] }.joinToString("")

/** Valor corto para etiquetas dentro de la gráfica: 1458, 0.4986, 5.06e-9. */
fun etiquetaValor(v: Double): String {
    val a = kotlin.math.abs(v)
    if (!v.isFinite()) return Formato.fijo(v, 0)
    if (a == 0.0 || (a >= 0.01 && a < 1e5)) return Formato.corto(kotlin.math.round(v * 1e4) / 1e4)
    return Formato.cientifica(v, 2)
}

/** Papel (color) de cada serie según el diseño. */
enum class RolSerie { DATOS, CURVA, PREDICCION, SERIE4, SERIE5, ERROR, TOLERANCIA }

/**
 * Asignación de colores: destacada → PREDICCION; discontinua → TOLERANCIA; en la gráfica de error (log) la serie
 * principal → ERROR; si no, la primera de PUNTOS → DATOS y la primera LINEA → CURVA; las demás SERIE4, SERIE5…
 */
fun rolesSeries(g: Grafica): List<RolSerie> {
    var hayDatos = false
    var hayCurva = false
    var hayError = false
    var extra = 0
    return g.series.map { s ->
        when {
            s.destacada -> RolSerie.PREDICCION
            s.discontinua -> RolSerie.TOLERANCIA
            g.escalaLogY && !hayError -> { hayError = true; RolSerie.ERROR }
            s.tipo == TipoSerie.PUNTOS && !hayDatos -> { hayDatos = true; RolSerie.DATOS }
            s.tipo != TipoSerie.PUNTOS && !hayCurva -> { hayCurva = true; RolSerie.CURVA }
            else -> if (extra++ % 2 == 0) RolSerie.SERIE4 else RolSerie.SERIE5
        }
    }
}

// ── Modelo listo para dibujar ──

/** Serie con su color: índice de la paleta, o -1 para el color de acento (serie destacada). */
data class SerieDibujable(val serie: Serie, val color: Int, val puntos: List<Punto>)

data class ModeloGrafica(
    val ejeX: Rango,
    /** En escala log, en exponentes. */
    val ejeY: Rango,
    val logY: Boolean,
    val ticksX: List<Double>,
    /** Valores (no exponentes) donde van las marcas de Y. */
    val ticksY: List<Double>,
    val etiquetasX: List<String>,
    val etiquetasY: List<String>,
    val series: List<SerieDibujable>,
    val hayDatos: Boolean,
)

/** Puntos que se pueden dibujar: finitos y, en escala log, con y > 0. */
fun puntosValidos(puntos: List<Punto>, logY: Boolean): List<Punto> =
    puntos.filter { it.x.isFinite() && it.y.isFinite() && (!logY || it.y > 0) }

fun prepararGrafica(g: Grafica, maxTicksX: Int = 6, maxTicksY: Int = 6, colores: Int = 4): ModeloGrafica {
    var siguiente = 0
    val series = g.series.map { s ->
        val color = if (s.destacada) -1 else (siguiente++ % colores)
        SerieDibujable(s, color, puntosValidos(s.puntos, g.escalaLogY))
    }
    val todos = series.flatMap { it.puntos }
    val hayDatos = todos.isNotEmpty()
    val ejeX = if (hayDatos) rangoConMargen(todos.minOf { it.x }, todos.maxOf { it.x }) else Rango(0.0, 1.0)
    val ejeY = ejeYPara(todos, g.escalaLogY)
    val ticksX = ticksBonitos(ejeX.min, ejeX.max, maxTicksX)
    val pasoX = pasoBonito(ejeX.ancho, maxTicksX)

    val ticksY: List<Double>
    val etiquetasY: List<String>
    if (g.escalaLogY) {
        val exps = exponentesLog(10.0.pow(ejeY.min), 10.0.pow(ejeY.max), maxTicksY)
        ticksY = exps.map { 10.0.pow(it) }
        etiquetasY = exps.map { etiquetaDecada(it) }
    } else {
        val pasoY = pasoBonito(ejeY.ancho, maxTicksY)
        ticksY = ticksBonitos(ejeY.min, ejeY.max, maxTicksY)
        etiquetasY = ticksY.map { etiquetaTick(it, pasoY) }
    }
    return ModeloGrafica(
        ejeX, ejeY, g.escalaLogY, ticksX, ticksY, ticksX.map { etiquetaTick(it, pasoX) }, etiquetasY, series, hayDatos,
    )
}

private fun ejeYPara(puntos: List<Punto>, logY: Boolean): Rango {
    if (puntos.isEmpty()) return if (logY) Rango(-1.0, 1.0) else Rango(0.0, 1.0)
    val min = puntos.minOf { it.y }
    val max = puntos.maxOf { it.y }
    return if (logY) rangoLog(min, max) else rangoConMargen(min, max)
}

/** Índice del candidato más cercano a (x, y) dentro de `maxDist`, o null. */
fun puntoMasCercano(candidatos: List<Pair<Float, Float>>, x: Float, y: Float, maxDist: Float): Int? {
    var mejor: Int? = null
    var mejorD = maxDist
    candidatos.forEachIndexed { i, (cx, cy) ->
        val d = sqrt((cx - x) * (cx - x) + (cy - y) * (cy - y))
        if (d.isFinite() && d <= mejorD) {
            mejorD = d
            mejor = i
        }
    }
    return mejor
}

/** Resumen textual para lectores de pantalla. */
fun descripcionGrafica(g: Grafica): String {
    val m = prepararGrafica(g)
    val sb = StringBuilder("Gráfica: ${g.titulo}.")
    if (!m.hayDatos) return sb.append(" Sin datos para graficar.").toString()
    sb.append(" Eje horizontal ${g.ejeX}, de ${Formato.auto(m.ejeX.min, 3)} a ${Formato.auto(m.ejeX.max, 3)}.")
    sb.append(" Eje vertical ${g.ejeY}")
    sb.append(if (g.escalaLogY) " en escala logarítmica." else ".")
    for (s in m.series) sb.append(' ').append(descripcionSerie(s))
    return sb.toString()
}

private fun descripcionSerie(s: SerieDibujable): String {
    val n = s.puntos.size
    val tipo = if (s.serie.tipo == TipoSerie.LINEA) "línea" else "$n ${if (n == 1) "punto" else "puntos"}"
    if (s.serie.destacada && n == 1) {
        val p = s.puntos[0]
        return "${s.serie.nombre}: punto destacado en (${Formato.auto(p.x, 4)}, ${Formato.auto(p.y, 6)})."
    }
    return "${s.serie.nombre}: $tipo."
}
