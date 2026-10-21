package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.EntradaInterpolacion
import com.example.pentametod2.catalogo.EntradaRegresionMultiple
import com.example.pentametod2.catalogo.EntradaRegresionPolinomial
import com.example.pentametod2.catalogo.EntradaSistema
import com.example.pentametod2.catalogo.ModoNodos
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.ui.formularios.EstadoInterpolacion
import com.example.pentametod2.ui.formularios.EstadoRegresionMultiple
import com.example.pentametod2.ui.formularios.EstadoRegresionPolinomial
import com.example.pentametod2.ui.formularios.EstadoSistema
import com.example.pentametod2.ui.formularios.aLista
import com.example.pentametod2.ui.formularios.estadoDesdeEntrada
import com.example.pentametod2.ui.formularios.estadoDesdeLista
import com.example.pentametod2.ui.formularios.estadoInicial
import com.example.pentametod2.ui.formularios.variablesPorDefecto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FormulariosTest {

    @Test
    fun sistemaAEntrada() {
        val e = EstadoSistema(listOf("x^2+y^2-4", "x-y"), "x, y", "1, 1", "1e-8", "30").aEntrada()
        assertEquals(EntradaSistema(listOf("x^2+y^2-4", "x-y"), "x, y", "1, 1", "1e-8", "30"), e)
        val inicial = estadoInicial(MetodoId.NEWTON_SISTEMAS) as EstadoSistema
        assertEquals("x, y", inicial.variables)
        assertEquals("1e-6", inicial.tolerancia)
        assertEquals("50", inicial.maxIter)
    }

    @Test
    fun sistemaAgregarYQuitarEcuaciones() {
        var s = EstadoSistema()
        s = s.agregarEcuacion()
        assertEquals(3, s.ecuaciones.size)
        assertEquals("x, y, z", s.variables)
        s = s.agregarEcuacion().agregarEcuacion() // tope 4
        assertEquals(4, s.ecuaciones.size)
        assertEquals("x, y, z, w", s.variables)
        s = s.quitarEcuacion().quitarEcuacion().quitarEcuacion() // mínimo 2
        assertEquals(2, s.ecuaciones.size)
        assertEquals("x, y", s.variables)
        // Si el alumno cambió las variables, no se tocan.
        val propio = EstadoSistema(variables = "a, b").agregarEcuacion()
        assertEquals("a, b", propio.variables)
        assertEquals(listOf("x", "y", "z"), propio.nombresVariables()) // etiquetas de respaldo
        assertEquals("x, y, z", variablesPorDefecto(3))
        assertEquals(listOf("x1", "x2"), EstadoSistema(variables = "x1, x2").nombresVariables())
    }

    @Test
    fun interpolacionAEntrada() {
        val e = EstadoInterpolacion("1, 2, 3", "4, 5, 6", "2.5", ModoNodos.AUTOMATICO, "1").aEntrada()
        assertEquals(EntradaInterpolacion("1, 2, 3", "4, 5, 6", "2.5", ModoNodos.AUTOMATICO, "1"), e)
    }

    @Test
    fun regresionPolinomialAEntrada() {
        assertEquals(
            EntradaRegresionPolinomial("1,2,3", "4,5,6", "2", ""),
            EstadoRegresionPolinomial("1,2,3", "4,5,6", "2", "").aEntrada(),
        )
    }

    @Test
    fun regresionMultipleCuadricula() {
        var m = EstadoRegresionMultiple()
        assertEquals(3, m.filas.size)
        assertEquals(3, m.filas[0].size) // x1, x2, y
        m = m.conCelda(0, 0, "1").conCelda(0, 2, "9")
        m = m.conNumVariables(3)
        assertEquals(listOf("1", "", "", "9"), m.filas[0]) // y sigue al final
        m = m.conNumVariables(1)
        assertEquals(listOf("1", "9"), m.filas[0])
        m = m.agregarFila()
        assertEquals(4, m.filas.size)
        m = m.quitarFila(3).quitarFila(0) // no baja de 3
        assertEquals(3, m.filas.size)
        assertEquals(EntradaRegresionMultiple(m.filas, ""), m.aEntrada())
    }

    @Test
    fun idaYVueltaConEjemplos() {
        for (id in MetodoId.entries) {
            val ejemplo = Catalogo.ejemplo(id)
            val estado = estadoDesdeEntrada(ejemplo)
            assertEquals(ejemplo, estado.aEntrada(), "ida y vuelta de $id")
            assertEquals(estado, estadoDesdeLista(estado.aLista()), "guardado de $id")
            assertEquals(Estado.OK, Catalogo.ejecutar(id, estado.aEntrada()).estado, "ejecutar $id")
        }
    }

    @Test
    fun guardadoDeFormulariosIniciales() {
        for (id in MetodoId.entries) {
            val e = estadoInicial(id)
            assertEquals(e, estadoDesdeLista(e.aLista()), "inicial $id")
        }
        assertEquals(null, estadoDesdeLista(listOf("?")))
        assertIs<EstadoRegresionMultiple>(estadoDesdeEntrada(EntradaRegresionMultiple(listOf(listOf("1", "2")))))
        assertEquals(3, (estadoDesdeEntrada(EntradaRegresionMultiple(listOf(listOf("1", "2")))) as EstadoRegresionMultiple).filas.size)
    }
}
