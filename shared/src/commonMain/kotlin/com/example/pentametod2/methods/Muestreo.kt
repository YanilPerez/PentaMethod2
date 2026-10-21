package com.example.pentametod2.methods

import com.example.pentametod2.model.Punto
import kotlin.math.abs
import kotlin.math.max

/** Muestreo de funciones para graficarlas. */
object Muestreo {

    /** `n` puntos equiespaciados en [desde, hasta]; descarta los que no dan un valor finito. */
    fun curva(f: (Double) -> Double, desde: Double, hasta: Double, n: Int = 200): List<Punto> {
        if (n < 2) throw MetodoNumericoException("Se necesitan al menos 2 puntos para muestrear una curva")
        val puntos = ArrayList<Punto>(n)
        for (i in 0 until n) {
            val x = if (i == n - 1) hasta else desde + (hasta - desde) * i / (n - 1)
            val y = f(x)
            if (x.isFinite() && y.isFinite()) puntos.add(Punto(x, y))
        }
        return puntos
    }

    /** Amplía [min, max] un `margen` (fracción del ancho) por cada lado. Si min == max, usa un ancho de max(1, |min|). */
    fun rangoConMargen(min: Double, max: Double, margen: Double = 0.1): Pair<Double, Double> {
        val ancho = if (max > min) max - min else max(1.0, abs(min))
        return (min - ancho * margen) to (max + ancho * margen)
    }
}
