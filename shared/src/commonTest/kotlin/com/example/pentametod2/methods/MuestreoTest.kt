package com.example.pentametod2.methods

import com.example.pentametod2.model.Punto
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MuestreoTest {

    private fun cuadrado(x: Double) = x * x

    @Test
    fun curvaCuadrado() {
        val c = Muestreo.curva(::cuadrado, 0.0, 1.0, 11)
        assertEquals(11, c.size)
        assertEquals(Punto(0.0, 0.0), c.first())
        assertEquals(Punto(1.0, 1.0), c.last())
        assertEquals(0.25, c[5].y, 1e-15)
    }

    @Test
    fun omiteNoFinitos() {
        val c = Muestreo.curva(::sqrt, -1.0, 1.0, 21) // sqrt(x<0) = NaN
        assertEquals(11, c.size)
        assertTrue(c.all { it.x >= 0.0 && it.y.isFinite() })
    }

    @Test
    fun rango() {
        val (a, b) = Muestreo.rangoConMargen(2.0, 3.0)
        assertEquals(1.9, a, 1e-12)
        assertEquals(3.1, b, 1e-12)
        val (c, d) = Muestreo.rangoConMargen(5.0, 5.0)
        assertTrue(c < 5.0 && d > 5.0)
    }
}
