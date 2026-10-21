package com.example.pentametod2.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatoTest {

    @Test
    fun fijo() {
        assertEquals("0.495873", Formato.fijo(0.495873, 6))
        assertEquals("2.0000", Formato.fijo(2.0, 4))
        assertEquals("0.0000", Formato.fijo(-0.00000001, 4))
        assertEquals("0.0000", Formato.fijo(-0.0, 4))
        assertEquals("1234.57", Formato.fijo(1234.5678, 2))
        assertEquals("-1234.57", Formato.fijo(-1234.5678, 2))
        assertEquals("1", Formato.fijo(0.5, 0)) // half-up
        assertEquals("-1", Formato.fijo(-0.5, 0))
        assertEquals("2.68", Formato.fijo(2.675, 2)) // 2.675 en binario es 2.67499999…
        assertEquals("0.05", Formato.fijo(0.05, 2))
        assertEquals("-3.244286", Formato.fijo(-3.2442857142, 6))
    }

    @Test
    fun noFinitos() {
        assertEquals("NaN", Formato.fijo(Double.NaN, 4))
        assertEquals("∞", Formato.fijo(Double.POSITIVE_INFINITY, 4))
        assertEquals("-∞", Formato.fijo(Double.NEGATIVE_INFINITY, 4))
        assertEquals("NaN", Formato.auto(Double.NaN, 4))
        assertEquals("∞", Formato.auto(Double.POSITIVE_INFINITY, 4))
        assertEquals("-∞", Formato.cientifica(Double.NEGATIVE_INFINITY, 4))
    }

    @Test
    fun cientificaYAuto() {
        assertEquals("2.8258e-7", Formato.auto(2.8258e-7, 4))
        assertEquals("-2.8258e-7", Formato.auto(-2.8258e-7, 4))
        assertEquals("0.000000", Formato.auto(0.0, 6))
        assertEquals("1.2346e10", Formato.auto(12345678901.0, 4))
        assertEquals("1.0000e-5", Formato.cientifica(9.99999e-6, 4)) // la mantisa no queda en 10.0000
        assertEquals("0.000100", Formato.auto(1e-4, 6))
    }

    @Test
    fun porcentaje() {
        assertEquals("97.43%", Formato.porcentaje(0.974265, 2))
        assertEquals("100.00%", Formato.porcentaje(1.0, 2))
    }

    @Test
    fun corto() {
        assertEquals("2.5", Formato.corto(2.5))
        assertEquals("2", Formato.corto(2.0))
        assertEquals("0.5103757", Formato.corto(0.5103757))
        assertEquals("-3", Formato.corto(-3.0))
        assertEquals("1e-8", Formato.corto(1e-8))
        assertEquals("0", Formato.corto(-0.0))
    }
}
