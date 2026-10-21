package com.example.pentametod2.methods

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SistemasNoLinealesTest {

    private val xy = listOf("x", "y")

    @Test
    fun newtonCircunferenciaRecta() {
        val r = SistemasNoLineales.newtonSistema(listOf("x^2+y^2-4", "x-y"), xy, v(1.0, 1.0))
        assertTrue(r.convergio, r.mensaje)
        assertArrayClose(v(sqrt(2.0), sqrt(2.0)), assertNotNull(r.solucion), 1e-8, "solución")
        assertTrue(r.iteraciones.size - 1 <= 8, "iteraciones: ${r.iteraciones.size - 1}")
        assertTrue(assertNotNull(r.residual) < 1e-8, "residual ${r.residual}")
        assertEquals(r.iteraciones.size - 1, r.detalle.size)
    }

    @Test
    fun newtonTresVariables() {
        val r = SistemasNoLineales.newtonSistema(listOf("x^2+y^2+z^2-3", "x-y", "y-z"), listOf("x", "y", "z"), v(2.0, 1.0, 1.0))
        assertTrue(r.convergio, r.mensaje)
        assertArrayClose(v(1.0, 1.0, 1.0), assertNotNull(r.solucion), 1e-8, "solución")
    }

    @Test
    fun newtonCircunferenciaUnitaria() {
        val r = SistemasNoLineales.newtonSistema(listOf("x^2+y^2-1", "x-y"), xy, v(1.0, 0.5))
        assertTrue(r.convergio, r.mensaje)
        assertArrayClose(v(sqrt(2.0) / 2, sqrt(2.0) / 2), assertNotNull(r.solucion), 1e-8, "solución")
    }

    @Test
    fun newtonJacobianoSingular() {
        val r = SistemasNoLineales.newtonSistema(listOf("x*y", "x*y-1"), xy, v(1.0, 1.0))
        assertFalse(r.convergio)
        assertTrue(r.mensaje.contains("Jacobiano"), r.mensaje)
        assertNull(r.solucion)
    }

    @Test
    fun puntoFijoContractivo() {
        val r = SistemasNoLineales.puntoFijoSistema(listOf("0.5*cos(y)", "0.5*sin(x)"), xy, v(0.0, 0.0), tol = 1e-8)
        assertTrue(r.convergio, r.mensaje)
        assertArrayClose(v(0.486405, 0.233726), assertNotNull(r.solucion), 1e-5, "solución")
        val n = r.iteraciones.size - 1
        assertTrue(abs(n - 13) <= 1, "iteraciones: $n")
        assertTrue(assertNotNull(r.residual) < 1e-7, "residual ${r.residual}")
    }

    @Test
    fun puntoFijoErrorPrimeraIteracion() {
        val r = SistemasNoLineales.puntoFijoSistema(listOf("0.5*cos(y)", "0.5*sin(x)"), xy, v(0.0, 0.0), tol = 1e-8)
        assertNull(r.iteraciones[0].error)
        assertEquals(1, r.iteraciones[1].k)
        assertArrayClose(v(0.5, 0.0), r.iteraciones[1].x, 1e-15, "x(1)")
        assertEquals(0.5, assertNotNull(r.iteraciones[1].error), 1e-15)
    }

    @Test
    fun puntoFijoDivergente() {
        val r = SistemasNoLineales.puntoFijoSistema(listOf("2x+y", "2y+x"), xy, v(1.0, 1.0))
        assertFalse(r.convergio)
        assertTrue(r.mensaje.contains("diverge"), r.mensaje)
    }

    @Test
    fun noConvergeEnMaxIter() {
        val r = SistemasNoLineales.puntoFijoSistema(listOf("0.5*cos(y)", "0.5*sin(x)"), xy, v(0.0, 0.0), tol = 1e-8, maxIter = 3)
        assertFalse(r.convergio)
        assertTrue(r.mensaje.startsWith("No convergió en 3 iteraciones"), r.mensaje)
        assertEquals(4, r.iteraciones.size)
    }

    @Test
    fun validacion() {
        assertFailsWith<MetodoNumericoException> {
            SistemasNoLineales.newtonSistema(listOf("x+y+z", "x-y"), listOf("x", "y", "z"), v(1.0, 1.0, 1.0))
        }
        assertFailsWith<MetodoNumericoException> {
            SistemasNoLineales.puntoFijoSistema(listOf("x", "y"), xy, v(1.0))
        }
        assertFailsWith<MetodoNumericoException> {
            SistemasNoLineales.newtonSistema(listOf("x+", "y"), xy, v(1.0, 1.0))
        }
    }
}
