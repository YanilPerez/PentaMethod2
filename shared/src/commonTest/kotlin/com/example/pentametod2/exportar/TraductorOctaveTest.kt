package com.example.pentametod2.exportar

import com.example.pentametod2.exportar.TraductorOctave.Traduccion
import com.example.pentametod2.parser.EvaluadorFuncion
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TraductorOctaveTest {

    private val xy = listOf("x", "y")

    private fun oct(texto: String, vars: List<String> = xy): String =
        assertIs<Traduccion.Ok>(TraductorOctave.traducir(texto, vars), texto).codigo

    @Test
    fun expresionesReales() {
        assertEquals("0.5*cos(y)", oct("0.5*cos(y)"))                       // ejemplo de punto fijo
        assertEquals("x^2 + y^2 - 4", oct("x^2+y^2-4"))                     // ejemplo de Newton
        assertEquals("2*x + y", oct("2x+y"))                                 // multiplicación implícita
        assertEquals("x*y - 3", oct("xy - 3"))                               // producto de letras
        assertEquals("3*sin(x)*(x + 1)", oct("3sin(x)(x+1)"))
        assertEquals("(x + 1)*(x - 1)", oct("(x+1)(x-1)"))
        assertEquals("2.5*x", oct("2,5x"))                                   // coma decimal
        assertEquals("x1^2 + x2", oct("x1^2 + x2", listOf("x1", "x2")))      // variables con dígito
    }

    @Test
    fun potencias() {
        assertEquals("2^(3^2)", oct("2^3^2"))           // derecha en la app, izquierda en Octave
        assertEquals("x^(-1) + 2^(-x)", oct("x^-1 + 2^-x"))
        assertEquals("-x^2", oct("-x^2"))                // −(x²) en ambos
        assertEquals("(-x)^2", oct("(-x)^2"))
        assertEquals("(x + y)^0.5", oct("(x+y)^0.5"))
    }

    @Test
    fun funcionesYConstantes() {
        assertEquals("log(x) + log10(y)", oct("ln(x) + log(y)"))
        assertEquals("sqrt(abs(sin(x)))*exp(1)^x", oct("sqrt(abs(sin(x)))*e^x"))
        assertEquals("exp(-x^2/2)/sqrt(2*pi)", oct("exp(-x^2/2)/sqrt(2pi)"))
        assertEquals("atan(y/x) + asin(0.5) - acos(0.5) + tan(x)", oct("atan(y/x)+asin(0.5)-acos(0.5)+tan(x)"))
    }

    @Test
    fun ordenDeEvaluacion() {
        assertEquals("x - (y - 1)", oct("x - (y - 1)"))
        assertEquals("x/(y*2)", oct("x/(y*2)"))
        assertEquals("x/y*2", oct("x/y*2"))
        assertEquals("-(-x)", oct("--x"))               // "--x" sería un decremento en Octave
        assertEquals("x - -y", oct("x - -y"))
        assertEquals("2*-x", oct("2*-x"))
    }

    @Test
    fun loQueNoSePuedeTraducir() {
        assertEquals(Traduccion.Error("foo"), TraductorOctave.traducir("foo(x)", xy))
        assertEquals(Traduccion.Error("end"), TraductorOctave.traducir("end + 1", listOf("end")))
        assertIs<Traduccion.Error>(TraductorOctave.traducir("x +", xy))
        val linea = TraductorOctave.funcionAnonima("g1", "foo(x)", xy)
        assertTrue(linea.startsWith("% ATENCIÓN: no se pudo traducir foo"), linea)
        assertTrue(linea.lines().all { it.startsWith("%") }, "nunca código inválido: $linea")
        assertEquals("g2 = @(x, y) 0.5*sin(x);", TraductorOctave.funcionAnonima("g2", "0.5*sin(x)", xy))
    }

    /**
     * La traducción calcula lo mismo que la app: se vuelve a la sintaxis del parser (log10 → log, log → ln)
     * y se comparan los valores en varios puntos.
     */
    @Test
    fun mismosValoresQueLaApp() {
        val expresiones = listOf(
            "0.5*cos(y)", "x^2+y^2-4", "2x+y", "xy - 3", "3sin(x)(x+1)", "2^3^2 + x", "x^-1 + 2^-x",
            "-x^2 + y", "ln(x) + log(y)", "sqrt(abs(sin(x)))*e^x", "x - (y - 1)/x", "--x * y^0.5",
        )
        val puntos = listOf(doubleArrayOf(1.3, 0.7), doubleArrayOf(2.0, 3.5), doubleArrayOf(0.4, 1.9))
        for (e in expresiones) {
            val vuelta = oct(e).replace("log10(", "§(").replace("log(", "ln(").replace("§(", "log(")
            val original = EvaluadorFuncion.compilarMulti(e, xy)
            val traducida = EvaluadorFuncion.compilarMulti(vuelta, xy)
            for (p in puntos) {
                val a = original(p)
                val b = traducida(p)
                assertTrue(abs(a - b) <= 1e-12 * maxOf(1.0, abs(a)), "$e → ${oct(e)}: $a vs $b en ${p.toList()}")
            }
        }
    }
}
