package com.example.pentametod2.methods

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegresionTest {

    private val tol = 1e-6

    private fun ResultadoRegresion.suma(etiqueta: String): Double =
        assertNotNull(sumas.firstOrNull { it.etiqueta == etiqueta }, "Falta la sumatoria $etiqueta en ${sumas.map { it.etiqueta }}").valor

    private val x5 = v(1.0, 2.0, 3.0, 4.0, 5.0)
    private val y5 = v(9.0, 7.5, 4.2, 3.0, 2.1)

    @Test
    fun cincoPuntosGrado1() {
        val r = Regresion.regresionPolinomial(x5, y5, 1)
        assertArrayClose(v(10.65, -1.83), r.coeficientes, tol, "a")
        assertEquals(1.683, r.Q, tol)
        assertEquals(0.952149, r.r2, tol)
        assertTrue(r.advertencias.isEmpty())
    }

    @Test
    fun cincoPuntosGrado2() {
        val r = Regresion.regresionPolinomial(x5, y5, 2)
        assertEquals(5.0, r.suma("n"), 0.0)
        assertEquals(15.0, r.suma("Σxi"), 1e-12)
        assertEquals(55.0, r.suma("Σxi²"), 1e-12)
        assertEquals(225.0, r.suma("Σxi³"), 1e-12)
        assertEquals(979.0, r.suma("Σxi⁴"), 1e-12)
        assertEquals(25.8, r.suma("Σyi"), 1e-12)
        assertEquals(59.1, r.suma("Σxiyi"), 1e-12)
        assertEquals(177.3, r.suma("Σxi²yi"), 1e-12)

        assertArrayClose(v(12.3, -3.244286, 0.235714), r.coeficientes, tol, "a")
        assertEquals(0.905143, r.Q, tol)
        assertEquals(0.974265, r.r2, tol)
        assertEquals(5.6625, r.predecir(2.5), tol)
        assertMatrixClose(
            m(v(4.6, -3.3, 0.5), v(-3.3, 2.671429, -0.428571), v(0.5, -0.428571, 0.071429)),
            r.inversaC, tol, "inversaC",
        )
    }

    @Test
    fun hojaSieteDatos() {
        val x = v(50.0, 52.0, 54.0, 56.0, 58.0, 60.0, 62.0)
        val y = v(31.0, 50.0, 77.0, 96.0, 112.0, 130.0, 162.0)
        val r1 = Regresion.regresionPolinomial(x, y, 1)
        assertArrayClose(v(-494.0, 10.5), r1.coeficientes, tol, "a grado 1")
        assertEquals(94.0, r1.Q, tol)
        assertEquals(0.992445, r1.r2, tol)
        val r2 = Regresion.regresionPolinomial(x, y, 2)
        assertArrayClose(v(-364.0, 5.833333, 0.041667), r2.coeficientes, tol, "a grado 2")
        assertEquals(91.666667, r2.Q, tol)
        assertEquals(0.992632, r2.r2, tol)
    }

    @Test
    fun hojaSeisDatosGrado2() {
        val r = Regresion.regresionPolinomial(v(0.0, 1.0, 2.0, 3.0, 4.0, 5.0), v(2.1, 7.7, 13.6, 27.2, 40.9, 61.1), 2)
        assertArrayClose(v(2.478571, 2.359286, 1.860714), r.coeficientes, tol, "a")
        assertEquals(3.746571, r.Q, tol)
        assertEquals(0.998509, r.r2, tol)
        assertEquals(1.117523, assertNotNull(r.errorEstandar), tol)
        assertEquals(110.168571, r.predecir(7.0), tol)
    }

    @Test
    fun dosRectas() {
        val mm = v(6.0, 4.0, 8.0, 5.0, 3.5, 7.0, 5.0, 10.0, 5.0, 4.0)
        val bb = v(6.5, 4.5, 7.0, 5.0, 4.0, 8.0, 7.0, 10.0, 6.0, 5.0)
        val bDeM = Regresion.regresionPolinomial(mm, bb, 1)
        assertEquals(4.468771, bDeM.Q, tol)
        assertEquals(29.6, bDeM.Syy, tol)
        val mDeB = Regresion.regresionPolinomial(bb, mm, 1)
        assertEquals(5.680321, mDeB.Q, tol)
        assertEquals(37.625, mDeB.Syy, tol)
    }

    @Test
    fun hojaResolverMalCondicionada() {
        val x = v(176.0, 168.0, 202.0, 138.0, 213.0, 159.0, 193.0, 122.0, 185.0, 153.0)
        val y = v(448.0, 556.0, 844.0, 427.0, 811.0, 398.0, 447.0, 154.0, 534.0, 313.0)
        val r = Regresion.regresionPolinomial(x, y, 2)
        assertEquals(365.0086, r.coeficientes[0], 1e-3)
        assertEquals(-5.285210, r.coeficientes[1], 1e-6)
        assertEquals(0.0344425, r.coeficientes[2], 1e-6)
        assertTrue(r.condicion > 1e10, "condición = ${r.condicion}")
        assertTrue(r.advertencias.isNotEmpty())
    }

    @Test
    fun multipleExacta() {
        val x = m(v(0.0, 0.0), v(2.0, 1.0), v(2.5, 2.0), v(1.0, 3.0), v(4.0, 6.0), v(7.0, 2.0))
        val y = v(5.0, 10.0, 9.0, 0.0, 3.0, 27.0)
        val r = Regresion.regresionMultiple(x, y)
        assertArrayClose(v(5.0, 4.0, -3.0), r.coeficientes, 1e-9, "a")
        assertTrue(r.Q < 1e-9, "Q = ${r.Q}")
        assertEquals(1.0, r.r2, 1e-9)
        assertMatrixClose(m(v(6.0, 16.5, 14.0), v(16.5, 76.25, 48.0), v(14.0, 48.0, 54.0)), r.matrizC, 1e-12, "C")
        assertEquals(5.0 + 4.0 * 1.0 - 3.0 * 2.0, r.predecir(v(1.0, 2.0)), 1e-9)
    }

    @Test
    fun multipleConRuidoResiduosOrtogonales() {
        val x1 = v(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)
        val x2 = v(2.0, 1.0, 4.0, 3.0, 6.0, 5.0, 8.0)
        val y = v(3.1, 3.9, 7.2, 7.8, 11.1, 11.9, 15.2)
        val x = Array(x1.size) { v(x1[it], x2[it]) }
        val r = Regresion.regresionMultiple(x, y)
        val diseno = Array(x1.size) { v(1.0, x1[it], x2[it]) }
        val xtE = AlgebraLineal.multiplicarMatrizVector(AlgebraLineal.transpuesta(diseno), r.residuos)
        assertArrayClose(v(0.0, 0.0, 0.0), xtE, 1e-8, "Xᵀ·e")
        assertTrue(r.r2 > 0.0 && r.r2 < 1.0, "r2 = ${r.r2}")
    }


    @Test
    fun inferenciaYMetricasRegresionMultiple() {
        val x1 = v(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)
        val x2 = v(2.0, 1.0, 4.0, 3.0, 6.0, 5.0, 8.0)
        val y = v(3.1, 3.9, 7.2, 7.8, 11.1, 11.9, 15.2)
        val r = Regresion.regresionMultiple(Array(x1.size) { v(x1[it], x2[it]) }, y)
        assertNotNull(r.r2Ajustado)
        assertNotNull(r.erroresCoeficientes)
        assertNotNull(r.estadisticosT)
        assertNotNull(r.valoresP)
        assertNotNull(r.estadisticoF)
        assertNotNull(r.valorPF)
        assertNotNull(r.vif)
        assertTrue(r.valoresP!!.all { it.isFinite() && it in 0.0..1.0 })
        assertTrue(r.vif!!.all { it.isFinite() && it >= 1.0 })
    }

    @Test
    fun distribucionTValoresConocidos() {
        assertEquals(1.0, DistribucionT.pValorDosColas(0.0, 10), 1e-12)
        assertEquals(0.05, DistribucionT.pValorDosColas(2.228, 10), 2e-4)
    }

    @Test
    fun errores() {
        assertFailsWith<MetodoNumericoException> { Regresion.regresionPolinomial(v(1.0, 2.0, 3.0), v(1.0, 2.0, 3.0), 3) }
        val ex = assertFailsWith<MetodoNumericoException> {
            Regresion.regresionMultiple(m(v(1.0, 2.0), v(2.0, 4.0), v(3.0, 6.0), v(4.0, 8.0)), v(1.0, 2.0, 3.0, 5.0))
        }
        assertTrue(ex.message!!.contains("linealmente dependientes"), ex.message)
        assertFailsWith<MetodoNumericoException> { Regresion.regresionPolinomial(v(2.0, 2.0, 2.0, 2.0), v(1.0, 2.0, 3.0, 4.0), 1) }
        assertFailsWith<MetodoNumericoException> { Regresion.regresionPolinomial(x5, y5, 0) }
        assertFailsWith<MetodoNumericoException> { Regresion.regresionMultiple(m(v(1.0, 2.0), v(2.0)), v(1.0, 2.0)) }
    }
}
