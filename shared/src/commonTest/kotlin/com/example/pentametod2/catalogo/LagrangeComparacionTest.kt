package com.example.pentametod2.catalogo

import com.example.pentametod2.exportar.*
import com.example.pentametod2.model.*
import com.example.pentametod2.ui.formularios.*
import com.example.pentametod2.ui.resultados.textoParaCopiar
import kotlin.test.*

class LagrangeComparacionTest {
    private val puntos = (0..3).map { PuntoEntradaLagrange(it, "$it", "${it * it * it}") }
    private fun entrada() = EntradaLagrange(puntos, "1.5", listOf(1, 2, 3), funcion = "x^3")
    private fun calcular(e: EntradaLagrange = entrada()): SalidaMetodo {
        val s = Catalogo.ejecutar(MetodoId.LAGRANGE, e)
        assertNotEquals(Estado.ERROR, s.estado, s.error)
        assertNotNull(s.comparacionLagrange)
        return s
    }

    @Test fun comparaTresGradosConErroresSinRedondear() {
        val c = calcular().comparacionLagrange!!
        assertEquals(3.375, c.referencia)
        assertEquals(listOf(1, 2, 3), c.resultados.map { it.grado })
        assertEquals(listOf(1, 2), c.resultados[0].puntos.map { it.id })
        assertEquals(listOf(0, 1, 2), c.resultados[1].puntos.map { it.id }) // empate: menor X
        assertEquals(4.5, c.resultados[0].valor, 1e-12)
        assertEquals(3.75, c.resultados[1].valor, 1e-12)
        assertEquals(3.375, c.resultados[2].valor, 1e-12)
        assertEquals(1.125, c.resultados[0].errorAbsoluto!!, 1e-12)
        assertEquals(100.0 / 3, c.resultados[0].errorRelativoPorcentaje!!, 1e-12)
        assertEquals(0.0, c.resultados[2].errorAbsoluto!!, 1e-12)
        val paso = c.resultados[1].detalle.bloques.filterIsInstance<BloquePaso>().first { it.numero == 2 }
        assertTrue(paso.conTusDatos.single().contains("(x −"))
    }

    @Test fun manualUtilizaExactamenteUnConjuntoIndependientePorGrado() {
        val c = calcular(entrada().copy(modo = ModoNodos.MANUAL,
            selecciones = mapOf(1 to listOf(0, 3), 2 to listOf(1, 2, 3), 3 to listOf(0, 1, 2, 3)))).comparacionLagrange!!
        assertEquals(13.5, c.resultados[0].valor, 1e-12)
        assertEquals(3.0, c.resultados[1].valor, 1e-12)
        assertEquals(listOf(0, 3), c.resultados[0].puntos.map { it.id })
    }

    @Test fun extrapolacionSeDeterminaPorLosPuntosDeCadaGrado() {
        val c = calcular(entrada().copy(grados = listOf(1, 2), modo = ModoNodos.MANUAL,
            selecciones = mapOf(1 to listOf(0, 1), 2 to listOf(0, 1, 2)))).comparacionLagrange!!
        assertTrue(c.resultados[0].extrapolacion)
        assertFalse(c.resultados[1].extrapolacion)
    }

    @Test fun funcionOpcionalInvalidaOFueraDeDominioNoImpideInterpolar() {
        for (f in listOf("sin(", "ln(-x)", "1/(x-1.5)", "", "x".repeat(201))) {
            val s = calcular(entrada().copy(funcion = f))
            assertNull(s.comparacionLagrange!!.referencia, f)
            assertTrue(s.advertencias.any { it.contains("No se calcularon errores") }, f)
            assertTrue(s.comparacionLagrange.resultados.all { it.errorAbsoluto == null })
        }
        val sinFuncion = calcular(entrada().copy(funcion = null))
        assertTrue(sinFuncion.advertencias.isEmpty())
        assertEquals(3, sinFuncion.bloques.filterIsInstance<BloqueTabla>().single().columnas.size)
    }

    @Test fun referenciaCeroConservaErrorAbsolutoYNoDividePorCero() {
        val s = calcular(entrada().copy(funcion = "0"))
        val c = s.comparacionLagrange!!
        assertEquals(0.0, c.referencia)
        assertEquals(4.5, c.resultados.first().errorAbsoluto)
        assertTrue(c.resultados.all { it.errorRelativoPorcentaje == null })
        assertTrue(s.advertencias.any { it.contains("no está definido") })
    }

    @Test fun detectaDiferenciasEntreFuncionYTablaSinReemplazarY() {
        val c = calcular(entrada().copy(funcion = "x^2")).comparacionLagrange!!
        assertEquals(27.0, c.puntos.last().y)
        assertEquals(2.25, c.referencia)
    }

    @Test fun rechazaDuplicadosFilasIncompletasYSeleccionesInvalidas() {
        val casos = listOf(
            entrada().copy(puntos = puntos + PuntoEntradaLagrange(4, "1", "8")),
            entrada().copy(puntos = puntos.toMutableList().apply { this[1] = this[1].copy(y = "") }),
            entrada().copy(grados = listOf(0)),
            entrada().copy(grados = listOf(4)),
            entrada().copy(grados = emptyList()),
            entrada().copy(modo = ModoNodos.MANUAL, selecciones = mapOf(1 to listOf(1, 1))),
            entrada().copy(grados = listOf(1), modo = ModoNodos.MANUAL, selecciones = mapOf(1 to listOf(0, 99))),
        )
        for (e in casos) assertEquals(Estado.ERROR, Catalogo.ejecutar(MetodoId.LAGRANGE, e).estado, "$e")
        assertEquals(Estado.OK, calcular(entrada().copy(puntos = puntos + PuntoEntradaLagrange(4))).estado)
        assertEquals(1.5, DatosLagrange.numeroCelda("1,5"))
    }

    @Test fun conservaIdentidadesYMarcaLaSeleccionIncompletaAlEliminar() {
        var e = EstadoLagrange(puntos, "1.5", listOf(1, 2)).conModo(ModoNodos.MANUAL)
        assertEquals(listOf(1, 2), e.seleccion(1))
        e = e.quitarPunto(0)
        assertEquals(listOf(1, 2), e.seleccion(1))
        assertEquals(listOf(1, 2), e.seleccion(2))
        assertNotNull(e.validacion())
        e = e.sugerir(2)
        assertNull(e.validacion())
        val editado = e.conCelda(1, Columna.X, "1.1").copy(x = "2.8")
        assertEquals(e.selecciones, editado.selecciones)
        assertEquals(editado, estadoDesdeLista(editado.aLista()))
        assertEquals(EstadoLagrange(), estadoDesdeLista(EstadoLagrange().aLista()))
    }

    @Test fun eliminarDatosNoCambiaSilenciosamenteLosGrados() {
        val e = EstadoLagrange(puntos, "1.5", listOf(1, 2, 3)).quitarPunto(3)
        assertEquals(listOf(1, 2, 3), e.grados)
        assertTrue(e.validacion()!!.contains("grado 3"))
        assertEquals(listOf(1, 2), e.alternarGrado(3).grados)
    }

    @Test fun importarConservaLosHuecosYRechazaColumnasExtra() {
        assertEquals(listOf("1", "", "3"), leerListaLagrange("1,,3"))
        val (xs, ys) = leerPegadoLagrange("X\tY\n1,5\t2\n2\t4")
        assertEquals(listOf("1,5", "2"), xs)
        assertEquals(listOf("2", "4"), ys)
        assertFailsWith<IllegalArgumentException> { leerPegadoLagrange("1\t2\t3") }
        assertFailsWith<IllegalArgumentException> { leerPegadoLagrange("1\t\n2\t4") }
        val e = EstadoLagrange(puntos, "1.5", listOf(1)).conModo(ModoNodos.MANUAL).reemplazar(listOf("1", "", "3"), listOf("2", "4", "6"))
        assertEquals("", e.puntos[1].x)
        assertEquals("4", e.puntos[1].y)
        assertTrue(e.seleccion(1).isEmpty())
        assertNotNull(e.validacion())
    }

    @Test fun exportaTodosLosGradosPuntosFuncionYErrores() {
        val s = calcular()
        val hojas = Exportaciones.hojas(s)
        assertEquals(listOf("Comparación", "Entrada", "Notas", "Nodos grado 1", "Pasos grado 1", "Nodos grado 2", "Pasos grado 2", "Nodos grado 3", "Pasos grado 3"), hojas.map { it.nombre })
        assertTrue(hojas[1].filas.flatten().any { it is CeldaX.Texto && it.s == "x^3" })
        val texto = textoParaCopiar(s)
        assertTrue(texto.contains("Error absoluto"))
        assertFalse(hojas.first().filas.first().any { it is CeldaX.Texto && it.s == "Error relativo %" })
        for (g in 1..3) assertTrue(texto.contains("Grado $g"))
        val octave = Exportaciones.octave(s)
        assertTrue(octave.contains("f = @(x) x^3;"))
        for (g in 1..3) assertTrue(octave.contains("% Grado $g; puntos"))
        val bytes = Exportaciones.contenido(s, TipoExportacion.EXCEL)
        assertTrue(bytes.size > 1000)
    }

    @Test fun newtonConservaSuEntradaYComportamientoYElEjemploLagrangeMigra() {
        val ejemplo = Catalogo.ejemplo(MetodoId.NEWTON_INTERPOLACION)
        val estado = estadoDesdeEntrada(ejemplo).paraMetodo(MetodoId.NEWTON_INTERPOLACION)
        assertIs<EstadoInterpolacion>(estado)
        assertEquals(ejemplo, estado.aEntrada())
        assertNull(Catalogo.ejecutar(MetodoId.NEWTON_INTERPOLACION, ejemplo).comparacionLagrange)
        val lagrange = estadoDesdeEntrada(Catalogo.ejemplo(MetodoId.LAGRANGE)).paraMetodo(MetodoId.LAGRANGE)
        assertIs<EstadoLagrange>(lagrange)
        assertNull(lagrange.validacion())
        assertEquals(listOf(2), lagrange.grados)
        assertEquals(0.4982119625, calcular(lagrange.aEntrada()).comparacionLagrange!!.resultados.single().valor, 1e-10)
    }
}
