package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.ui.formularios.etiquetaEcuacion
import com.example.pentametod2.ui.resultados.anchosColumnas
import com.example.pentametod2.ui.resultados.filasVisibles
import com.example.pentametod2.ui.resultados.textoCelda
import com.example.pentametod2.ui.resultados.textoParaCopiar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextoSalidaTest {

    @Test
    fun formatoDeCeldas() {
        assertEquals("0.500000", textoCelda(Celda.Num(0.5), 6))
        assertEquals("3", textoCelda(Celda.Entero(3), 6))
        assertEquals("2.8258e-7", textoCelda(Celda.Num(2.8258e-7), 4))
        assertEquals("2.825800e-7", textoCelda(Celda.Num(2.8258e-7), 6))
        assertEquals("", textoCelda(Celda.Vacia, 6))
        assertEquals("(2.1 − 2)", textoCelda(Celda.Txt("(2.1 − 2)"), 6))
        assertEquals("0.000000", textoCelda(Celda.Num(-0.0), 6))
        assertEquals("1.234568e10", textoCelda(Celda.Num(12345678901.0), 6)) // sin "1.2345678901E10"
        assertEquals("NaN", textoCelda(Celda.Num(Double.NaN), 6))
    }

    @Test
    fun anchos() {
        assertEquals(listOf(1, 8, 5), anchosColumnas(listOf("k", "xₖ", "f[xₖ]"), listOf(listOf("0", "2.000000", "0.51"))))
        assertEquals(listOf(2), anchosColumnas(listOf("ab"), emptyList()))
    }

    @Test
    fun filasVisiblesConservanTotal() {
        val filas = List(250) { listOf<Celda>(Celda.Entero(it)) } + listOf(listOf<Celda>(Celda.Txt("Σ")))
        val (visibles, ocultas) = filasVisibles(BloqueTabla("t", listOf("i"), filas, filaTotal = true))
        assertEquals(201, visibles.size)
        assertEquals(50, ocultas)
        assertEquals(Celda.Txt("Σ"), visibles.last().single())
        val (pocas, ninguna) = filasVisibles(BloqueTabla("t", listOf("i"), filas.take(5)))
        assertEquals(5, pocas.size)
        assertEquals(0, ninguna)
    }

    @Test
    fun copiarResultados() {
        val s = Catalogo.ejecutar(MetodoId.LAGRANGE, Catalogo.ejemplo(MetodoId.LAGRANGE))
        val texto = textoParaCopiar(s)
        assertTrue(texto.contains("P(2.5)\t0.49821"), texto)
        assertTrue(texto.contains("Nodos y polinomios de Lagrange\ni\txᵢ\tyᵢ"), texto)
        assertTrue(texto.lines().any { it.startsWith("Σ\t\t\t") }, texto)
    }

    @Test
    fun etiquetasDeEcuaciones() {
        assertEquals("x = G₁(x, y)", etiquetaEcuacion(true, 0, listOf("x", "y")))
        assertEquals("y = G₂(x, y)", etiquetaEcuacion(true, 1, listOf("x", "y")))
        assertEquals("F₃(x, y, z) = 0", etiquetaEcuacion(false, 2, listOf("x", "y", "z")))
    }
}
