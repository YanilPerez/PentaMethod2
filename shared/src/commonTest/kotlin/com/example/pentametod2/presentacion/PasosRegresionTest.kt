package com.example.pentametod2.presentacion

import com.example.pentametod2.methods.Regresion
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.DatosExportables
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.model.TipoTexto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PasosRegresionTest {

    private val xs = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
    private val ys = doubleArrayOf(9.0, 7.5, 4.2, 3.0, 2.1)

    private fun salida(xPredecir: Double?): SalidaMetodo =
        PresentadorRegresion.presentarRegresionPolinomial(Regresion.regresionPolinomial(xs, ys, 2), xs, ys, xPredecir)

    private fun SalidaMetodo.pasos() = bloques.filterIsInstance<BloquePaso>()

    private fun List<BloquePaso>.resultado(etiqueta: String): String =
        flatMap { it.resultados }.first { it.etiqueta == etiqueta }.texto

    @Test
    fun sietePasosEnOrdenConLosValoresDelEjemplo() {
        val pasos = salida(2.5).pasos()
        assertEquals(
            listOf(
                "Sistema normal", "Ecuación del polinomio", "Suma de cuadrados de residuos", "Media y variación de y",
                "Coeficiente de determinación", "Error estándar", "Predicción",
            ),
            pasos.map { it.titulo },
        )
        assertEquals((1..7).toList(), pasos.map { it.numero })
        assertEquals("0.905143", pasos.resultado("Q"))
        assertEquals("5.160000", pasos.resultado("ȳ"))
        assertEquals("35.172000", pasos.resultado("S_yy"))
        assertEquals("0.974265", pasos.resultado("R²"))
        assertEquals("0.987049", pasos.resultado("r"))
        assertEquals("0.672734", pasos.resultado("S"))
        assertEquals("5.662500", pasos.resultado("P(2.5)"))
        assertEquals(listOf("12.300000", "-3.244286", "0.235714"), pasos[1].resultados.map { it.texto })
        assertEquals("12.300000 − 3.244286x + 0.235714x²", pasos.resultado("P₂(x)"))
    }

    @Test
    fun resumenesCortosYFormulas() {
        val pasos = salida(2.5).pasos()
        assertEquals(listOf("C·a = b", "a₀, a₁, a₂", "0.905143", "ȳ = 5.160000", "0.974265", "0.672734", "5.662500"), pasos.map { it.resumenCorto })
        assertEquals(listOf("C · a = b", "Cᵢⱼ = Σ xₖ^(i+j)   bᵢ = Σ xₖ^i · yₖ"), pasos[0].formulaGeneral)
        assertEquals(
            "Plantea las ecuaciones cuya solución son los coeficientes del polinomio.",
            pasos[0].explicacion,
        )
        assertEquals(listOf("C", "b", "C⁻¹", "a"), pasos[0].matrices.map { it.nombre })
        // Un término por punto en Q y el desarrollo de la predicción.
        assertEquals(5 + 1, pasos[2].conTusDatos.size)
        assertEquals("P(2.5) = 12.300000 + (-3.244286)(2.5) + 0.235714(2.5)²", pasos[6].conTusDatos[0])
        assertEquals("= 5.662500", pasos[6].conTusDatos.last())
        assertTrue(pasos.all { it.explicacion.isNotBlank() && it.formulaGeneral.isNotEmpty() && it.resultados.isNotEmpty() })
    }

    @Test
    fun soloElPrimeroAbiertoYSoloLaPrediccionDestacada() {
        val pasos = salida(2.5).pasos()
        assertEquals(listOf(true, false, false, false, false, false, false), pasos.map { it.abiertoInicial })
        assertEquals(listOf("Predicción"), pasos.filter { it.destacado }.map { it.titulo })
    }

    @Test
    fun sinPrediccionHaySeisPasos() {
        val pasos = salida(null).pasos()
        assertEquals(6, pasos.size)
        assertTrue(pasos.none { it.destacado })
    }

    @Test
    fun ordenDeLosBloquesYDatosExportables() {
        val s = salida(2.5)
        // pasos → tabla → conclusión (la gráfica va aparte en SalidaMetodo.grafica)
        assertEquals(7, s.bloques.takeWhile { it is BloquePaso }.size)
        assertIs<BloqueTabla>(s.bloques[7])
        assertEquals(TipoTexto.CONCLUSION, assertIs<BloqueTexto>(s.bloques[8]).tipo)
        assertEquals(9, s.bloques.size)
        assertEquals(DatosExportables.RegresionPolinomial(xs.toList(), ys.toList(), 2, 2.5), s.exportable)
    }
}
