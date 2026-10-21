package com.example.pentametod2.methods

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.ln1p

/** Funciones de cola para inferencia, mediante beta incompleta regularizada. */
object DistribucionT {
    fun pValorDosColas(t: Double, gradosLibertad: Int): Double {
        if (gradosLibertad <= 0 || !t.isFinite()) return Double.NaN
        if (t == 0.0) return 1.0
        val x = gradosLibertad.toDouble() / (gradosLibertad + t * t)
        return betaRegularizada(x, gradosLibertad / 2.0, 0.5).coerceIn(0.0, 1.0)
    }

    fun pValorF(f: Double, glNumerador: Int, glDenominador: Int): Double {
        if (glNumerador <= 0 || glDenominador <= 0 || f < 0.0 || !f.isFinite()) return Double.NaN
        if (f == 0.0) return 1.0
        val x = glDenominador.toDouble() / (glDenominador + glNumerador * f)
        return betaRegularizada(x, glDenominador / 2.0, glNumerador / 2.0).coerceIn(0.0, 1.0)
    }

    private fun betaRegularizada(x0: Double, a: Double, b: Double): Double {
        if (x0 <= 0.0) return 0.0
        if (x0 >= 1.0) return 1.0
        val front = exp(logGamma(a + b) - logGamma(a) - logGamma(b) + a * ln(x0) + b * ln1p(-x0))
        return if (x0 < (a + 1.0) / (a + b + 2.0)) front * fraccionBeta(x0, a, b) / a
        else 1.0 - front * fraccionBeta(1.0 - x0, b, a) / b
    }

    private fun fraccionBeta(x: Double, a: Double, b: Double): Double {
        val maxIter = 300; val eps = 3e-14; val tiny = 1e-300
        val qab = a + b; val qap = a + 1.0; val qam = a - 1.0
        var c = 1.0; var d = 1.0 - qab * x / qap
        if (abs(d) < tiny) d = tiny
        d = 1.0 / d
        var h = d
        for (m in 1..maxIter) {
            val m2 = 2.0 * m
            var aa = m * (b - m) * x / ((qam + m2) * (a + m2))
            d = 1.0 + aa * d; if (abs(d) < tiny) d = tiny
            c = 1.0 + aa / c; if (abs(c) < tiny) c = tiny
            d = 1.0 / d; h *= d * c
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
            d = 1.0 + aa * d; if (abs(d) < tiny) d = tiny
            c = 1.0 + aa / c; if (abs(c) < tiny) c = tiny
            d = 1.0 / d
            val delta = d * c; h *= delta
            if (abs(delta - 1.0) < eps) break
        }
        return h
    }

    private fun logGamma(z0: Double): Double {
        val c = doubleArrayOf(676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012,
            9.9843695780195716e-6, 1.5056327351493116e-7)
        if (z0 < 0.5) return ln(kotlin.math.PI) - ln(kotlin.math.sin(kotlin.math.PI * z0)) - logGamma(1.0 - z0)
        val z = z0 - 1.0
        var x = 0.99999999999980993
        for (i in c.indices) x += c[i] / (z + i + 1.0)
        val t = z + c.size - 0.5
        return 0.5 * ln(2.0 * kotlin.math.PI) + (z + 0.5) * ln(t) - t + ln(x)
    }
}
