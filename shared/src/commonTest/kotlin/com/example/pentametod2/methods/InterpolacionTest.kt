package com.example.pentametod2.methods

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterpolacionTest {

    private val xs = v(2.0, 2.2, 2.4, 2.6, 2.8)
    private val ys = v(0.510376, 0.520784, 0.510415, 0.481331, 0.435916)

    @Test
    fun lagrangeHojaCurso() {
        val r1 = Interpolacion.lagrange(v(2.4, 2.6), v(0.510415, 0.481331), 2.5)
        assertEquals(0.495873, r1.valor, 1e-6)
        assertEquals(1, r1.grado)
        assertFalse(r1.extrapolacion)

        val r2 = Interpolacion.lagrange(v(2.2, 2.4, 2.6), v(0.520784, 0.510415, 0.481331), 2.5)
        assertEquals(0.498212, r2.valor, 1e-6)
        assertArrayClose(v(-0.125, 0.75, 0.375), r2.L, 1e-12, "L grado 2")

        val r3 = Interpolacion.lagrange(xs.copyOfRange(0, 4), ys.copyOfRange(0, 4), 2.5)
        assertEquals(0.498083, r3.valor, 1e-6)
        assertArrayClose(v(0.0625, -0.3125, 0.9375, 0.3125), r3.L, 1e-12, "L grado 3")
        assertEquals(r3.valor, r3.terminos.sum(), 1e-15)
        assertEquals(r3.valor, r3.evaluar(2.5), 1e-12)
    }

    @Test
    fun newtonPrefijos() {
        val esperados = mapOf(1 to 0.51558, 2 to 0.518177, 3 to 0.518306, 4 to 0.518293)
        for ((grado, esperado) in esperados) {
            val r = Interpolacion.newtonDiferenciasDivididas(xs.copyOfRange(0, grado + 1), ys.copyOfRange(0, grado + 1), 2.1)
            assertEquals(grado, r.grado)
            assertEquals(esperado, r.valor, 1e-6, "Newton grado $grado")
        }
    }

    /** La hoja del curso usa ys con 7 decimales. */
    @Test
    fun newtonCoeficientesHojaCurso() {
        val ys7 = v(0.5103757, 0.5207843, 0.5104147, 0.4813306, 0.435916)
        val r = Interpolacion.newtonDiferenciasDivididas(xs, ys7, 2.1)
        assertArrayClose(v(0.5103757, 0.052043, -0.2597275, 0.04299375, 0.0083411458), r.coeficientes, 1e-9, "coeficientes")
        assertEquals(0.5182937445, r.valor, 1e-9)
    }

    @Test
    fun newtonTablaTriangular() {
        val r = Interpolacion.newtonDiferenciasDivididas(xs, ys, 2.1)
        for (k in xs.indices) {
            assertEquals(ys[k], r.tabla[k][0])
            for (j in xs.indices) assertEquals(k + j >= xs.size, r.tabla[k][j].isNaN(), "tabla[$k][$j]")
        }
        for (j in xs.indices) assertEquals(r.coeficientes[j], r.tabla[0][j])
        assertEquals(r.valor, r.terminos.sum(), 1e-15)
    }

    @Test
    fun exponencial() {
        val nx = v(0.0, 0.5, 1.0, 2.0)
        val ny = v(1.0, 1.64872, 2.71828, 7.38906)
        val n = Interpolacion.newtonDiferenciasDivididas(nx, ny, 0.25)
        assertArrayClose(v(1.0, 1.29744, 0.84168, 0.423047), n.coeficientes, 1e-5, "coeficientes e^x")
        for ((x, esperado) in listOf(0.25 to 1.291585, 0.75 to 2.111065)) {
            val pn = Interpolacion.newtonDiferenciasDivididas(nx, ny, x).valor
            val pl = Interpolacion.lagrange(nx, ny, x).valor
            assertEquals(esperado, pn, 1e-5, "Newton P($x)")
            assertEquals(esperado, pl, 1e-5, "Lagrange P($x)")
            assertEquals(pl, pn, 1e-9, "Lagrange vs Newton en $x")
            assertEquals(pn, n.evaluar(x), 1e-12)
        }
        // sanity: el polinomio aproxima e^x
        assertEquals(exp(0.75), n.evaluar(0.75), 0.02)
    }

    @Test
    fun errores() {
        assertFailsWith<MetodoNumericoException> { Interpolacion.lagrange(v(1.0, 1.0), v(2.0, 3.0), 0.5) }
        assertFailsWith<MetodoNumericoException> { Interpolacion.newtonDiferenciasDivididas(v(1.0, 2.0, 1.0), v(2.0, 3.0, 4.0), 0.5) }
        assertFailsWith<MetodoNumericoException> { Interpolacion.lagrange(v(1.0, 2.0), v(2.0), 0.5) }
        assertFailsWith<MetodoNumericoException> { Interpolacion.newtonDiferenciasDivididas(v(1.0), v(2.0), 0.5) }
    }

    @Test
    fun extrapolacion() {
        assertTrue(Interpolacion.lagrange(xs, ys, 3.0).extrapolacion)
        assertTrue(Interpolacion.newtonDiferenciasDivididas(xs, ys, 1.9).extrapolacion)
        assertFalse(Interpolacion.newtonDiferenciasDivididas(xs, ys, 2.8).extrapolacion)
    }

    @Test
    fun nodosCercanos() {
        val (sx, sy) = Interpolacion.seleccionarNodosCercanos(xs, ys, 2.5, 2)
        assertArrayClose(v(2.4, 2.6), sx, 0.0)
        assertArrayClose(v(0.510415, 0.481331), sy, 0.0)
        // empate exacto: x=2.5 con 3 nodos → 2.4, 2.6 y luego empate 2.2/2.8 (gana 2.2)
        val (tx, _) = Interpolacion.seleccionarNodosCercanos(v(1.0, 2.0, 3.0, 4.0), v(1.0, 2.0, 3.0, 4.0), 2.5, 3)
        assertArrayClose(v(1.0, 2.0, 3.0), tx, 0.0)
    }
}
