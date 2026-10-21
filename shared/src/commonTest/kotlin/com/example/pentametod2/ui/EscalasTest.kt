package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.model.Grafica
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.Punto
import com.example.pentametod2.model.Serie
import com.example.pentametod2.model.TipoSerie
import com.example.pentametod2.ui.grafica.Rango
import com.example.pentametod2.ui.grafica.aPixel
import com.example.pentametod2.ui.grafica.aPixelLog
import com.example.pentametod2.ui.grafica.descripcionGrafica
import com.example.pentametod2.ui.grafica.etiquetaDecada
import com.example.pentametod2.ui.grafica.etiquetaTick
import com.example.pentametod2.ui.grafica.pasoBonito
import com.example.pentametod2.ui.grafica.prepararGrafica
import com.example.pentametod2.ui.grafica.puntoMasCercano
import com.example.pentametod2.ui.grafica.rangoConMargen
import com.example.pentametod2.ui.grafica.rangoLog
import com.example.pentametod2.ui.grafica.ticksBonitos
import com.example.pentametod2.ui.grafica.ticksLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EscalasTest {

    private fun assertLista(esperado: List<Double>, real: List<Double>, tolRel: Double = 1e-9) {
        assertEquals(esperado.size, real.size, "esperado $esperado, salió $real")
        for (i in esperado.indices) {
            val tol = tolRel * maxOf(1.0, kotlin.math.abs(esperado[i]))
            assertEquals(esperado[i], real[i], tol, "[$i] esperado $esperado, salió $real")
        }
    }

    @Test
    fun ticksCeroAUno() {
        val t = ticksBonitos(0.0, 1.0, 6)
        assertLista(listOf(0.0, 0.2, 0.4, 0.6, 0.8, 1.0), t)
        assertEquals(0.2, pasoBonito(1.0, 6), 1e-15)
    }

    @Test
    fun ticksCeroANoventaYSiete() {
        assertEquals(20.0, pasoBonito(97.0, 6), 1e-12)
        assertLista(listOf(0.0, 20.0, 40.0, 60.0, 80.0), ticksBonitos(0.0, 97.0, 6))
    }

    @Test
    fun ticksNegativosYPequenos() {
        assertLista(listOf(-1.0, -0.5, 0.0, 0.5, 1.0), ticksBonitos(-1.2, 1.2, 6))
        assertEquals(5e-5, pasoBonito(2.3e-4, 6), 1e-18)
        assertTrue(ticksBonitos(-1.2, 1.2, 6).contains(0.0)) // sin -0.0 ni 1e-17
    }

    @Test
    fun rangos() {
        val r = rangoConMargen(0.0, 100.0)
        assertEquals(-6.0, r.min, 1e-12)
        assertEquals(106.0, r.max, 1e-12)
        val d = rangoConMargen(5.0, 5.0)
        assertTrue(d.min < d.max)
        assertEquals(Rango(4.0, 6.0), d) // ±1
        assertEquals(Rango(450.0, 550.0), rangoConMargen(500.0, 500.0)) // ±10 %
        assertEquals(Rango(0.0, 1.0), rangoConMargen(Double.NaN, 1.0))
    }

    @Test
    fun logaritmico() {
        assertLista(listOf(1e-8, 1e-6, 1e-4, 1e-2, 1.0), ticksLog(1e-8, 1.0, 6))
        assertLista(listOf(1e-3, 1e-2, 1e-1), ticksLog(1e-3, 0.5, 6)) // cada década si caben
        val r = rangoLog(2.8e-7, 0.5)
        assertTrue(r.min < -7.0 && r.max > 0.0, "$r") // décadas completas -7..0 más margen
        val unico = rangoLog(1e-3, 1e-3)
        assertTrue(unico.ancho >= 1.0)
        // Fase 4B: las décadas se escriben con superíndices (antes "1e-8" y "1").
        assertEquals("10⁻⁸", etiquetaDecada(-8))
        assertEquals("10⁰", etiquetaDecada(0))
        assertEquals("10⁴", etiquetaDecada(4))
    }

    @Test
    fun transformaciones() {
        val r = Rango(0.0, 10.0)
        assertEquals(0f, aPixel(0.0, r, 0f, 100f))
        assertEquals(50f, aPixel(5.0, r, 0f, 100f))
        assertEquals(150f, aPixel(5.0, r, 200f, 100f)) // eje Y invertido: abajo=200, arriba=100
        assertEquals(100f, aPixel(10.0, r, 200f, 100f))
        val rl = Rango(-8.0, 0.0)
        assertEquals(100f, aPixelLog(1.0, rl, 0f, 100f), 1e-4f)
        assertEquals(50f, aPixelLog(1e-4, rl, 0f, 100f), 1e-4f)
        assertTrue(aPixelLog(0.0, rl, 0f, 100f).isNaN())
    }

    @Test
    fun etiquetas() {
        assertEquals("0.2", etiquetaTick(0.2, 0.2))
        assertEquals("20", etiquetaTick(20.0, 20.0))
        assertEquals("0.0", etiquetaTick(0.0, 0.5)) // mismos decimales que 0.5 y 1.0
    }

    @Test
    fun modeloDescartaNoFinitosYLogNoPositivos() {
        val g = Grafica(
            "t", "k", "E",
            listOf(
                Serie("E", TipoSerie.LINEA_CON_PUNTOS, listOf(Punto(1.0, 0.5), Punto(2.0, 0.0), Punto(3.0, Double.NaN), Punto(4.0, 1e-8))),
                Serie("D", TipoSerie.PUNTOS, listOf(Punto(1.0, 1.0)), destacada = true),
            ),
            escalaLogY = true,
        )
        val m = prepararGrafica(g)
        assertEquals(2, m.series[0].puntos.size)
        assertEquals(0, m.series[0].color)
        assertEquals(-1, m.series[1].color) // acento
        assertTrue(m.logY)
        assertLista(listOf(1e-8, 1e-6, 1e-4, 1e-2, 1.0), m.ticksY)
        assertEquals("10⁻⁸", m.etiquetasY.first()) // Fase 4B: superíndices
    }

    @Test
    fun graficaVaciaOUnSoloPunto() {
        val vacia = prepararGrafica(Grafica("t", "x", "y", listOf(Serie("s", TipoSerie.PUNTOS, emptyList()))))
        assertFalse(vacia.hayDatos)
        val uno = prepararGrafica(Grafica("t", "x", "y", listOf(Serie("s", TipoSerie.PUNTOS, listOf(Punto(2.0, 3.0))))))
        assertTrue(uno.ejeX.min < 2.0 && uno.ejeX.max > 2.0)
        assertTrue(uno.ejeY.min < 3.0 && uno.ejeY.max > 3.0)
    }

    @Test
    fun cercano() {
        val c = listOf(0f to 0f, 10f to 10f, 50f to 50f)
        assertEquals(1, puntoMasCercano(c, 12f, 9f, 20f))
        assertNull(puntoMasCercano(c, 100f, 100f, 20f))
    }

    @Test
    fun descripcionAccesible() {
        val s = Catalogo.ejecutar(MetodoId.LAGRANGE, Catalogo.ejemplo(MetodoId.LAGRANGE))
        val d = descripcionGrafica(s.grafica!!)
        assertTrue(d.startsWith("Gráfica: Polinomio interpolante."), d)
        assertTrue(d.contains("Nodos: 3 puntos."), d)
        assertTrue(d.contains("x evaluado: punto destacado en (2.5000, 0.498212)"), d)
    }
}
