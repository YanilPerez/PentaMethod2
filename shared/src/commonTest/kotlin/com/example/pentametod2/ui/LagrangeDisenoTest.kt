package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.*
import com.example.pentametod2.model.*
import com.example.pentametod2.ui.formularios.*
import com.example.pentametod2.ui.resultados.detalleLagrange
import kotlin.test.*

class LagrangeDisenoTest {
    private val puntos = (0..3).map { PuntoEntradaLagrange(it, "$it", "${it * it * it}") }

    @Test fun alIngresarListasSeleccionaElMayorGradoDisponibleYLoConservaAlRestaurar() {
        val e = EstadoLagrange().conLista(Columna.X, "0, 1, 2, 3").conLista(Columna.Y, "0, 1, 8, 27")
        assertEquals(listOf(3), e.grados)
        val restaurado = assertIs<EstadoLagrange>(estadoDesdeLista(e.aLista()))
        assertEquals(e, restaurado)
        val ampliado = restaurado.conLista(Columna.X, "0, 1, 2, 3, 4").conLista(Columna.Y, "0, 1, 8, 27, 64")
        assertEquals(listOf(4), ampliado.grados)
    }

    @Test fun completarYEliminarFilasAjustaElGradoPredeterminado() {
        val e = EstadoLagrange().reemplazar(listOf("0", "1", "2"), listOf("0", "1", "8"))
            .conVista(VistaDatos.TABLA)
        assertEquals(listOf(2), e.grados)
        val pendiente = e.agregarPunto().conCelda(3, Columna.X, "3")
        assertEquals(listOf(2), pendiente.grados)
        val completo = pendiente.conCelda(3, Columna.Y, "27")
        assertEquals(listOf(3), completo.grados)
        assertEquals(listOf(2), completo.quitarPunto(3).grados)
    }

    @Test fun elegirGradosDetieneLaSeleccionPredeterminadaSinCambiarLaEleccionDelUsuario() {
        val e = EstadoLagrange().reemplazar(listOf("0", "1", "2", "3"), listOf("0", "1", "8", "27"))
            .alternarGrado(3).alternarGrado(1)
        val restaurado = assertIs<EstadoLagrange>(estadoDesdeLista(e.aLista()))
        val editado = restaurado.conLista(Columna.X, "0, 1, 2, 3, 4").conLista(Columna.Y, "0, 1, 8, 27, 64")
        assertEquals(listOf(1), editado.grados)
        assertFalse(editado.gradoPredeterminado)
    }

    @Test fun restauraL2SinReemplazarLosGradosPreviamenteElegidos() {
        val anterior = listOf("L2", "0.5", "AUTOMATICO", "1", "false", "", "3", "0", "TABLA", "", "", "false", "false",
            "0", "0", "0", "1", "1", "1", "2", "2", "8")
        val e = assertIs<EstadoLagrange>(estadoDesdeLista(anterior))
        assertEquals(listOf(1), e.conCelda(2, Columna.Y, "9").grados)
        assertFalse(e.gradoPredeterminado)
        assertEquals(VistaDatos.TABLA, e.vista)
    }

    @Test fun listaEsLaVistaInicialYConservaLosHuecosYElTextoAlEscribir() {
        val inicial = EstadoLagrange()
        assertEquals(VistaDatos.LISTA, inicial.vista)
        assertEquals("", inicial.textoLista(Columna.X))
        assertEquals("", inicial.textoLista(Columna.Y))
        val e = inicial.conLista(Columna.X, "0, , 2,").conLista(Columna.Y, "0, 1, 8,").copy(x = "1.5")
        assertEquals("0, , 2,", e.textoLista(Columna.X))
        assertEquals("", e.puntos[1].x)
        assertEquals("1", e.puntos[1].y)
        assertNotNull(e.validacion())
        assertEquals(e, estadoDesdeLista(e.aLista()))
        val tabla = e.conVista(VistaDatos.TABLA)
        assertEquals(e.puntos, tabla.puntos)
        assertEquals(tabla.puntos, tabla.conVista(VistaDatos.LISTA).puntos)
    }

    @Test fun cambiarDeVistaYEditarNoReemplazaPuntosNiSelecciones() {
        val e = EstadoLagrange(puntos, "1.5", listOf(1, 2)).conModo(ModoNodos.MANUAL)
        val tabla = e.conVista(VistaDatos.TABLA).conCelda(1, Columna.Y, "1.1")
        val lista = tabla.conVista(VistaDatos.LISTA)
        assertEquals(e.puntos.map { it.id }, lista.puntos.map { it.id })
        assertEquals(e.selecciones, lista.selecciones)
        assertEquals("0, 1.1, 8, 27", lista.textoLista(Columna.Y))
        val editada = lista.conLista(Columna.Y, "0, 1.2, 8, 27")
        assertEquals(e.selecciones, editada.selecciones)
        assertEquals(e.puntos.map { it.id }, editada.puntos.map { it.id })
        assertNull(editada.validacion())
    }

    @Test fun comaDecimalEnTablaSeConvierteSinCambiarSuValorNiSuPareja() {
        val tabla = EstadoLagrange(listOf(PuntoEntradaLagrange(3, "1,5", "2,5"),
            PuntoEntradaLagrange(9, "2,5", "4,5")), "2", listOf(1), ModoNodos.MANUAL,
            mapOf(1 to listOf(3, 9)), vista = VistaDatos.TABLA)
        val lista = tabla.conVista(VistaDatos.LISTA)
        assertEquals("1.5, 2.5", lista.textoLista(Columna.X))
        assertEquals("2.5, 4.5", lista.textoLista(Columna.Y))
        assertEquals(tabla.selecciones, lista.selecciones)
        assertEquals(listOf(3, 9), lista.puntos.map { it.id })
        assertNull(lista.validacion())
    }

    @Test fun tablaPermiteAgregarEliminarYVolverALista() {
        val e = EstadoLagrange(puntos, "1.5", listOf(1)).conModo(ModoNodos.MANUAL).conVista(VistaDatos.TABLA)
        val agregada = e.agregarPunto().conCelda(4, Columna.X, "4").conCelda(4, Columna.Y, "64")
        assertEquals(4, agregada.maximoGrado())
        val eliminada = agregada.quitarPunto(0)
        assertEquals(e.selecciones, eliminada.selecciones)
        assertEquals("1, 2, 3, 4", eliminada.conVista(VistaDatos.LISTA).textoLista(Columna.X))
        assertEquals(eliminada, estadoDesdeLista(eliminada.aLista()))
    }

    @Test fun restauraElFormatoAnteriorYCargarEjemploConservaLaVista() {
        val antiguo = listOf("L1", "1.5", "MANUAL", "1", "false", "", "2", "1",
            "0", "0", "0", "1", "1", "1", "1", "0,1")
        val restaurado = assertIs<EstadoLagrange>(estadoDesdeLista(antiguo))
        assertEquals(mapOf(1 to listOf(0, 1)), restaurado.selecciones)
        assertNull(restaurado.validacion())
        val ejemplo = estadoDesdeEntrada(Catalogo.ejemplo(MetodoId.LAGRANGE)).paraMetodo(MetodoId.LAGRANGE)
            .conVistaDe(EstadoLagrange(vista = VistaDatos.TABLA))
        assertIs<EstadoLagrange>(ejemplo)
        assertEquals(VistaDatos.TABLA, ejemplo.vista)
        assertEquals(ModoNodos.AUTOMATICO, ejemplo.modo)
        assertEquals(listOf(ejemplo.maximoGrado()), ejemplo.grados)
        assertNull(ejemplo.validacion())
    }

    @Test fun comparacionSoloMuestraReferenciaYErrorAbsolutoConservandoLosPasosYLaConclusionFinal() {
        val s = Catalogo.ejecutar(MetodoId.LAGRANGE, EntradaLagrange(puntos, "1.5", listOf(1, 2, 3), funcion = "x^3"))
        val c = assertNotNull(s.comparacionLagrange)
        assertEquals(listOf("f(x) de referencia"), s.resumen.map { it.etiqueta })
        val tabla = s.bloques.filterIsInstance<BloqueTabla>().single()
        assertEquals(listOf("Grado", "Puntos", "P(x)", "Error absoluto"), tabla.columnas)
        assertTrue(s.bloques.none { it is BloqueTexto && it.tipo == TipoTexto.CONCLUSION })
        for (r in c.resultados) {
            val vista = detalleLagrange(s, r.grado)
            assertEquals(r.detalle.bloques, vista.bloques)
            assertEquals(listOf(1, 2, 3, 4, 5), vista.bloques.filterIsInstance<BloquePaso>().map { it.numero })
            assertEquals(1, vista.bloques.filterIsInstance<BloqueTexto>().count { it.tipo == TipoTexto.CONCLUSION })
            val indiceTabla = vista.bloques.indexOfFirst { it is BloqueTabla }
            val indiceConclusion = vista.bloques.indexOfFirst { it is BloqueTexto && it.tipo == TipoTexto.CONCLUSION }
            assertTrue(indiceConclusion > indiceTabla)
        }
        val sinFuncion = Catalogo.ejecutar(MetodoId.LAGRANGE, EntradaLagrange(puntos, "1.5", listOf(1)))
        assertTrue(sinFuncion.resumen.isEmpty())
    }

    @Test fun avisosComunesYDelGradoSeMuestranUnaSolaVezEnSuDetalle() {
        val s = Catalogo.ejecutar(MetodoId.LAGRANGE,
            EntradaLagrange(puntos, "1.5", listOf(1, 2), ModoNodos.MANUAL,
                mapOf(1 to listOf(0, 1), 2 to listOf(0, 1, 2)), "x^2"))
        val d1 = detalleLagrange(s, 1)
        assertEquals(1, d1.advertencias.count { it.contains("extrapolación") })
        assertEquals(1, d1.advertencias.count { it.contains("Y difiere") })
        assertEquals(Estado.ADVERTENCIA, d1.estado)
        val d2 = detalleLagrange(s, 2)
        assertTrue(d2.advertencias.none { it.contains("extrapol") })
        assertEquals(1, d2.advertencias.count { it.contains("Y difiere") })
    }
}
