package com.example.pentametod2.methods

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal fun assertArrayClose(esperado: DoubleArray, real: DoubleArray, tol: Double, msg: String = "") {
    assertEquals(esperado.size, real.size, "$msg: tamaños distintos")
    for (i in esperado.indices) {
        assertEquals(esperado[i], real[i], tol, "$msg [$i]: esperado ${esperado.toList()} y salió ${real.toList()}")
    }
}

internal fun assertMatrixClose(esperado: Array<DoubleArray>, real: Array<DoubleArray>, tol: Double, msg: String = "") {
    assertEquals(esperado.size, real.size, "$msg: nº de filas distinto")
    for (i in esperado.indices) assertArrayClose(esperado[i], real[i], tol, "$msg fila $i")
}

internal fun m(vararg filas: DoubleArray): Array<DoubleArray> = arrayOf(*filas)
internal fun v(vararg x: Double): DoubleArray = x

class AlgebraLinealTest {

    @Test
    fun gauss3x3() {
        val a = m(v(2.0, 1.0, -1.0), v(-3.0, -1.0, 2.0), v(-2.0, 1.0, 2.0))
        val b = v(8.0, -11.0, -3.0)
        val x = AlgebraLineal.resolverGauss(a, b)
        assertArrayClose(v(2.0, 3.0, -1.0), x, 1e-10)
        // No modifica los argumentos
        assertArrayClose(v(8.0, -11.0, -3.0), b, 0.0)
        assertArrayClose(v(2.0, 1.0, -1.0), a[0], 0.0)
    }

    @Test
    fun gaussExigePivoteo() {
        val x = AlgebraLineal.resolverGauss(m(v(0.0, 1.0), v(1.0, 0.0)), v(2.0, 3.0))
        assertArrayClose(v(3.0, 2.0), x, 1e-12)
    }

    @Test
    fun singular() {
        val a = m(v(1.0, 2.0), v(2.0, 4.0))
        val ex = assertFailsWith<MetodoNumericoException> { AlgebraLineal.resolverGauss(a, v(1.0, 2.0)) }
        kotlin.test.assertTrue(ex.message!!.startsWith("La matriz es singular"))
        assertFailsWith<MetodoNumericoException> { AlgebraLineal.invertir(a) }
        assertEquals(Double.POSITIVE_INFINITY, AlgebraLineal.numeroCondicion(a))
    }

    @Test
    fun inversa() {
        val a = m(v(5.0, 15.0, 55.0), v(15.0, 55.0, 225.0), v(55.0, 225.0, 979.0))
        val inv = AlgebraLineal.invertir(a)
        val esperado = m(
            v(4.6, -3.3, 0.5),
            v(-3.3, 2.671429, -0.428571),
            v(0.5, -0.428571, 0.071429),
        )
        assertMatrixClose(esperado, inv, 1e-6, "inversa")
        assertMatrixClose(AlgebraLineal.identidad(3), AlgebraLineal.multiplicar(a, inv), 1e-10, "A·inv(A)")
    }

    @Test
    fun auxiliares() {
        val a = m(v(1.0, 2.0, 3.0), v(4.0, 5.0, 6.0))
        assertMatrixClose(m(v(1.0, 4.0), v(2.0, 5.0), v(3.0, 6.0)), AlgebraLineal.transpuesta(a), 0.0)
        assertArrayClose(v(14.0, 32.0), AlgebraLineal.multiplicarMatrizVector(a, v(1.0, 2.0, 3.0)), 0.0)
        assertEquals(3.0, AlgebraLineal.normaInf(v(1.0, -3.0, 2.0)))
        assertEquals(2.5, AlgebraLineal.normaInfDiferencia(v(1.0, 2.0), v(1.5, -0.5)))
        assertEquals(9.0, AlgebraLineal.norma1(a))
    }
}
