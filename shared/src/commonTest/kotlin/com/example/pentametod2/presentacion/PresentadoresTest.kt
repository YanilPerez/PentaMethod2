package com.example.pentametod2.presentacion

import com.example.pentametod2.methods.Interpolacion
import com.example.pentametod2.methods.Regresion
import com.example.pentametod2.methods.SistemasNoLineales
import com.example.pentametod2.model.BloqueGrupo
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.model.TipoTexto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PresentadoresTest {

    private fun SalidaMetodo.tabla(titulo: String): BloqueTabla =
        assertNotNull(bloques.filterIsInstance<BloqueTabla>().firstOrNull { it.titulo == titulo }, "Falta la tabla '$titulo'")

    private fun SalidaMetodo.resumenContiene(texto: String): Boolean = resumen.any { it.valor.contains(texto) }

    private fun SalidaMetodo.sinNaN() {
        for (item in resumen) assertFalse(item.valor.contains("NaN"), "NaN en el resumen: $item")
        for (f in formulas) for (l in f.sustituida) assertFalse(l.contains("NaN"), "NaN en la fórmula: $l")
    }

    private fun num(c: Celda): Double = (c as Celda.Num).v

    private fun d(vararg x: Double): DoubleArray = x

    private val xs5 = d(2.0, 2.2, 2.4, 2.6, 2.8)
    private val ys7 = d(0.5103757, 0.5207843, 0.5104147, 0.4813306, 0.435916)

    @Test
    fun lagrange() {
        val r = Interpolacion.lagrange(d(2.2, 2.4, 2.6), d(0.520784, 0.510415, 0.481331), 2.5)
        val s = PresentadorInterpolacion.presentarLagrange(r)
        assertEquals(Estado.OK, s.estado)
        assertTrue(s.resumenContiene("0.498212"), s.resumen.toString())
        val t = s.tabla("Nodos y polinomios de Lagrange")
        assertTrue(t.filaTotal)
        assertEquals(4, t.filas.size)
        val sumaTerminos = t.filas.dropLast(1).sumOf { num(it[4]) }
        assertEquals(r.valor, sumaTerminos, 1e-12)
        assertEquals(r.valor, num(t.filas.last()[4]), 1e-15)

        val g = assertNotNull(s.grafica)
        assertEquals(3, g.series.size)
        assertTrue(g.series.first { it.nombre == "P(x)" }.puntos.size >= 100)
        val xEval = g.series.first { it.nombre == "x evaluado" }
        assertEquals(1, xEval.puntos.size)
        assertTrue(xEval.destacada)
        assertEquals(3, s.formulas.single().sustituida.size - 1) // una línea por Lᵢ + la final
        assertTrue(s.formulas.single().sustituida.last().startsWith("P₂(2.5)"))
        s.sinNaN()
    }

    @Test
    fun lagrangeFormulaDosNodos() {
        val r = Interpolacion.lagrange(d(2.4, 2.6), d(0.510415, 0.481331), 2.5)
        val lineas = PresentadorInterpolacion.presentarLagrange(r).formulas.single().sustituida
        assertEquals("L₀(2.5) = (2.5 − 2.6)/(2.4 − 2.6) = 0.500000", lineas[0])
        assertTrue(lineas.last().startsWith("P₁(2.5) = y₀·L₀ + y₁·L₁ = "), lineas.last())
        assertTrue(lineas.last().endsWith("= 0.495873"), lineas.last())
    }

    @Test
    fun lagrangeExtrapolacion() {
        val s = PresentadorInterpolacion.presentarLagrange(Interpolacion.lagrange(d(2.2, 2.4, 2.6), d(0.520784, 0.510415, 0.481331), 3.5))
        assertEquals(Estado.ADVERTENCIA, s.estado)
        assertEquals(listOf(PresentadorInterpolacion.ADVERTENCIA_EXTRAPOLACION), s.advertencias)
        assertTrue(s.advertencias.single().contains("extrapolación"))
    }

    @Test
    fun newtonInterpolacion() {
        val s = PresentadorInterpolacion.presentarNewton(Interpolacion.newtonDiferenciasDivididas(xs5, ys7, 2.1))
        assertEquals(Estado.OK, s.estado)
        val t = s.tabla("Tabla de diferencias divididas")
        assertEquals(5, t.filas.size)
        assertEquals(listOf("k", "xₖ", "f[xₖ]", "Δ¹", "Δ²", "Δ³", "Δ⁴"), t.columnas)
        assertEquals(0.5103757, num(t.filas[0][2]))
        for (k in 0 until 5) {
            for (j in 0 until 5) {
                val celda = t.filas[k][2 + j]
                if (k + j >= 5) assertEquals(Celda.Vacia, celda, "[$k][$j]") else assertTrue(celda is Celda.Num, "[$k][$j]")
            }
        }
        val terminos = s.tabla("Términos de Newton")
        assertTrue(terminos.filaTotal)
        assertEquals(0.5182937445, num(terminos.filas.last()[4]), 1e-9)
        assertTrue(s.resumenContiene("0.518294"))
        s.sinNaN()
    }

    @Test
    fun regresionPolinomial() {
        val xs = d(1.0, 2.0, 3.0, 4.0, 5.0)
        val ys = d(9.0, 7.5, 4.2, 3.0, 2.1)
        val s = PresentadorRegresion.presentarRegresionPolinomial(Regresion.regresionPolinomial(xs, ys, 2), xs, ys, 2.5)
        assertEquals(Estado.OK, s.estado)
        val conclusion = s.bloques.filterIsInstance<BloqueTexto>().single { it.tipo == TipoTexto.CONCLUSION }
        assertTrue(conclusion.texto.contains("97.43%"), conclusion.texto)
        assertTrue(conclusion.texto.contains("Ajuste muy bueno"), conclusion.texto)
        // Desde la Fase 7 las matrices van dentro del paso "Sistema normal".
        assertEquals(listOf("C", "b", "C⁻¹", "a"), s.bloques.filterIsInstance<BloquePaso>().first().matrices.map { it.nombre })

        val t = s.tabla("Datos y sumatorias")
        assertTrue(t.filaTotal)
        val total = t.filas.last()
        assertEquals(15.0, num(total[t.columnas.indexOf("xᵢ")]), 1e-12)
        assertEquals(55.0, num(total[t.columnas.indexOf("xᵢ²")]), 1e-12)
        assertEquals(979.0, num(total[t.columnas.indexOf("xᵢ⁴")]), 1e-12)
        assertEquals(177.3, num(total[t.columnas.indexOf("xᵢ²yᵢ")]), 1e-12)
        assertEquals(0.905143, num(total[t.columnas.indexOf("(P(xᵢ)−yᵢ)²")]), 1e-6)

        assertTrue(s.resumen.any { it.etiqueta == "P(2.5)" && it.valor.contains("5.6625") }, s.resumen.toString())
        assertEquals("P₂(x) = 12.300000 − 3.244286x + 0.235714x²", s.resumen.first().valor)
        val g = assertNotNull(s.grafica)
        assertEquals(listOf("Datos", "P(x)", "Predicción"), g.series.map { it.nombre })
        s.sinNaN()
    }

    @Test
    fun regresionMalCondicionada() {
        val x = d(176.0, 168.0, 202.0, 138.0, 213.0, 159.0, 193.0, 122.0, 185.0, 153.0)
        val y = d(448.0, 556.0, 844.0, 427.0, 811.0, 398.0, 447.0, 154.0, 534.0, 313.0)
        val s = PresentadorRegresion.presentarRegresionPolinomial(Regresion.regresionPolinomial(x, y, 2), x, y)
        assertEquals(Estado.ADVERTENCIA, s.estado)
        assertTrue(s.advertencias.isNotEmpty())
        s.sinNaN()
    }

    @Test
    fun regresionMultiple() {
        val x = arrayOf(d(0.0, 0.0), d(2.0, 1.0), d(2.5, 2.0), d(1.0, 3.0), d(4.0, 6.0), d(7.0, 2.0))
        val y = d(5.0, 10.0, 9.0, 0.0, 3.0, 27.0)
        val s = PresentadorRegresion.presentarRegresionMultiple(Regresion.regresionMultiple(x, y), x, y, d(1.0, 1.0))
        assertEquals(Estado.OK, s.estado)
        assertEquals("1.000000", s.resumen.first { it.etiqueta == "R²" }.valor)
        assertEquals("6.000000", s.resumen.first { it.etiqueta == "Predicción (1, 1)" }.valor)
        val serie = assertNotNull(s.grafica).series.first { it.nombre == "Real vs predicho" }
        assertEquals(6, serie.puntos.size)
        assertEquals(listOf("i", "x₁ᵢ", "x₂ᵢ", "yᵢ", "ŷᵢ", "(ŷᵢ−yᵢ)²", "(yᵢ−ȳ)²"), s.tabla("Datos y residuos").columnas)
        s.sinNaN()
    }

    @Test
    fun newtonSistemas() {
        val ecs = listOf("x^2+y^2-4", "x-y")
        val r = SistemasNoLineales.newtonSistema(ecs, listOf("x", "y"), d(1.0, 1.0))
        val s = PresentadorSistemas.presentarNewtonSistemas(r, ecs, 1e-6)
        assertEquals(Estado.OK, s.estado)
        val t = s.tabla("Iteraciones")
        val n = r.iteraciones.last().k
        assertEquals((0..n).toList(), t.filas.map { (it[0] as Celda.Entero).v })
        assertEquals(Celda.Vacia, t.filas[0].last())
        assertTrue(t.filas.drop(1).all { it.last() is Celda.Num })
        assertTrue(t.filas.all { it[t.columnas.indexOf("‖F(x)‖∞")] is Celda.Num })
        assertTrue(assertNotNull(s.grafica).escalaLogY)
        val grupos = s.bloques.filterIsInstance<BloqueGrupo>()
        assertEquals(n, grupos.size)
        assertTrue(grupos.all { it.colapsado })
        assertEquals(listOf("J", "F(x)", "Δ"), grupos[0].bloques.filterIsInstance<BloqueMatriz>().map { it.nombre })
        assertTrue(s.resumenContiene("1.414214"))
        s.sinNaN()
    }

    @Test
    fun puntoFijoContractivo() {
        val gs = listOf("0.5*cos(y)", "0.5*sin(x)")
        val r = SistemasNoLineales.puntoFijoSistema(gs, listOf("x", "y"), d(0.0, 0.0), tol = 1e-8)
        val s = PresentadorSistemas.presentarPuntoFijo(r, gs, 1e-8)
        assertEquals(Estado.OK, s.estado)
        assertEquals(listOf("x = 0.5*cos(y)", "y = 0.5*sin(x)", "Se detiene cuando E < 1e-8"), s.formulas.single().sustituida)
        assertEquals(listOf("k", "x", "y", "E"), s.tabla("Iteraciones").columnas)
        // Fase 4B: la gráfica de error ahora también trae la serie "Tolerancia"; se busca la de E(k).
        assertEquals(r.iteraciones.size - 1, assertNotNull(s.grafica).series.first { it.nombre == "E(k)" }.puntos.size)
        s.sinNaN()
    }

    @Test
    fun puntoFijoDivergente() {
        val gs = listOf("2x+y", "2y+x")
        val r = SistemasNoLineales.puntoFijoSistema(gs, listOf("x", "y"), d(1.0, 1.0))
        val s = PresentadorSistemas.presentarPuntoFijo(r, gs)
        assertEquals(Estado.ERROR, s.estado)
        assertNotNull(s.error)
        assertEquals(r.iteraciones.size, s.tabla("Iteraciones").filas.size)
        assertTrue(s.resumen.any { it.etiqueta == "Convergió" && it.valor == "No" })
    }
}
