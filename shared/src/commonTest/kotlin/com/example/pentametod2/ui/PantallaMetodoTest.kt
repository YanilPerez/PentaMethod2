package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.ui.formularios.EstadoRegresionPolinomial
import com.example.pentametod2.ui.formularios.EstadoSistema
import com.example.pentametod2.ui.formularios.LineaEntrada
import com.example.pentametod2.ui.formularios.botonesSinIcono
import com.example.pentametod2.ui.formularios.campoEcuacion
import com.example.pentametod2.ui.formularios.contadorEcuaciones
import com.example.pentametod2.ui.formularios.estadoDesdeEntrada
import com.example.pentametod2.ui.formularios.listaLegible
import com.example.pentametod2.ui.formularios.resumenEntrada
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PantallaMetodoTest {

    @Test
    fun entradaComprimidaRegresion() {
        val l = resumenEntrada(MetodoId.REGRESION_POLINOMIAL, estadoDesdeEntrada(Catalogo.ejemplo(MetodoId.REGRESION_POLINOMIAL)))
        assertEquals(
            listOf(LineaEntrada("x", "1, 2, 3, 4, 5"), LineaEntrada("y", "9, 7.5, 4.2, 3, 2.1"), LineaEntrada("Grado", "2 · x a predecir 2.5")),
            l,
        )
        val sinPred = resumenEntrada(MetodoId.REGRESION_POLINOMIAL, EstadoRegresionPolinomial("1;2 3", "4,5,6", "1", ""))
        assertEquals(LineaEntrada("x", "1, 2, 3"), sinPred[0])
        assertEquals(LineaEntrada("Grado", "1"), sinPred[2])
    }

    @Test
    fun entradaComprimidaSistemas() {
        val pf = resumenEntrada(MetodoId.PUNTO_FIJO, estadoDesdeEntrada(Catalogo.ejemplo(MetodoId.PUNTO_FIJO)))
        assertEquals(LineaEntrada("G₁", "x = 0.5*cos(y)"), pf[0])
        assertEquals(LineaEntrada("G₂", "y = 0.5*sin(x)"), pf[1])
        assertEquals(LineaEntrada("x₀", "(0, 0)"), pf.first { it.etiqueta == "x₀" })
        assertEquals(LineaEntrada("Criterio", "tol 1e-8 · máx 50"), pf.last())
        val nw = resumenEntrada(MetodoId.NEWTON_SISTEMAS, EstadoSistema(listOf("x^2+y^2-4", "x-y"), "x, y", "1, 1", "", ""))
        assertEquals(LineaEntrada("F₁", "x^2+y^2-4 = 0"), nw[0])
        assertEquals(LineaEntrada("Criterio", "tol 1e-6 · máx 50"), nw.last()) // valores por defecto
    }

    @Test
    fun entradaComprimidaDeTodosLosEjemplos() {
        for (id in MetodoId.entries) {
            val l = resumenEntrada(id, estadoDesdeEntrada(Catalogo.ejemplo(id)))
            assertTrue(l.isNotEmpty() && l.all { it.valor.isNotBlank() }, "$id: $l")
        }
    }

    @Test
    fun camposDeEcuaciones() {
        assertEquals("F₁(x, y)" to "=", campoEcuacion(false, 0, listOf("x", "y")))
        assertEquals("G₂(x, y)" to "y =", campoEcuacion(true, 1, listOf("x", "y")))
        assertEquals("2 de 2", contadorEcuaciones(EstadoSistema()))
        assertEquals("2 de 3", contadorEcuaciones(EstadoSistema(variables = "x, y, z")))
    }

    @Test
    fun quitarEcuacionConcreta() {
        val s = EstadoSistema(listOf("a", "b", "c"), "x, y, z")
        val q = s.quitarEcuacion(0)
        assertEquals(listOf("b", "c"), q.ecuaciones)
        assertEquals("x, y", q.variables) // las variables por defecto se ajustan
        assertEquals(q, q.quitarEcuacion(1)) // mínimo 2
        assertEquals("1, 2", listaLegible(" 1 ;2 "))
    }

    @Test
    fun botonesDelDockSinIconoConFuenteGrande() {
        kotlin.test.assertFalse(botonesSinIcono(1.0f))
        assertTrue(botonesSinIcono(1.3f))
    }
}
