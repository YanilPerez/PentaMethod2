package com.example.pentametod2.parser

import kotlin.math.E
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EvaluadorFuncionTest {

    private val tol = 1e-9

    private fun eval(texto: String, x: Double = 0.0): Double = EvaluadorFuncion.compilar(texto)(x)

    private fun evalMulti(texto: String, variables: List<String>, vararg valores: Double): Double =
        EvaluadorFuncion.compilarMulti(texto, variables)(valores)

    @Test
    fun operacionesBasicas() {
        assertEquals(1.0, eval("x^2-3", 2.0), tol)
        assertEquals(6.0, eval("2x", 3.0), tol)
        assertEquals(-9.0, eval("-x^2", 3.0), tol)
        assertEquals(512.0, eval("2^3^2"), tol)
        assertEquals(0.5, eval("2^-1"), tol)
        assertEquals(-1.0, eval("x^3 - 2x - 5", 2.0), tol)
    }

    @Test
    fun funcionesYConstantes() {
        assertEquals(2.0, eval("3sin(x)+2cos(x)", 0.0), tol)
        assertEquals(1.0, eval("sin(pi/2)"), tol)
        assertEquals(E, eval("e^x", 1.0), tol)
        assertEquals(1.0, eval("ln(e)"), tol)
        assertEquals(3.0, eval("sqrt(x)", 9.0), tol)
    }

    @Test
    fun multiplicacionImplicitaYComaDecimal() {
        assertEquals(6.0, eval("2(x+1)", 2.0), tol)
        assertEquals(3.0, eval("1,5*x", 2.0), tol)
    }

    @Test
    fun multivariable() {
        val xy = listOf("x", "y")
        assertEquals(1.0, evalMulti("x^2+y^2-4", xy, 1.0, 2.0), tol)
        assertEquals(12.0, evalMulti("xy", xy, 3.0, 4.0), tol)
        assertEquals(24.0, evalMulti("2xy", xy, 3.0, 4.0), tol)
        assertEquals(10.0, evalMulti("x1*x2+x3", listOf("x1", "x2", "x3"), 2.0, 3.0, 4.0), tol)
        assertEquals(10.0, evalMulti("x1^2 + x2", listOf("x1", "x2"), 3.0, 1.0), tol)
    }

    @Test
    fun errores() {
        for (texto in listOf("", "x+", "(x", "foo(x)", "x$", "sin x")) {
            assertFailsWith<FuncionInvalidaException>("Debió fallar: '$texto'") {
                EvaluadorFuncion.compilar(texto)
            }
        }
        val ex = assertFailsWith<FuncionInvalidaException> {
            EvaluadorFuncion.compilarMulti("y", listOf("x"))
        }
        assertEquals("Variable desconocida 'y'", ex.message)
    }

    @Test
    fun divisionEntreCeroNoLanza() {
        val r = eval("1/x", 0.0)
        assertTrue(r.isInfinite() && r > 0, "Se esperaba Infinity y salió $r")
    }
}
