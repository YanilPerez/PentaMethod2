package com.example.pentametod2.catalogo

import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.ui.resultados.textoCelda
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verificación de punta a punta contra las hojas de Excel del curso: entra SOLO texto (como lo escribe
 * el alumno) y se compara lo que la interfaz mostraría (resumen y celdas), no los objetos internos.
 * Tolerancia: media unidad del último decimal mostrado por la app.
 */
class ExcelPuntaAPuntaTest {

    private fun ejecutar(id: MetodoId, e: Entrada): SalidaMetodo {
        val s = Catalogo.ejecutar(id, e)
        assertTrue(s.estado != Estado.ERROR, "$id falló: ${s.error}")
        return s
    }

    private fun SalidaMetodo.valor(etiqueta: String): String =
        assertNotNull(resumen.firstOrNull { it.etiqueta == etiqueta }, "Falta «$etiqueta» en el resumen: $resumen").valor

    private fun decimalesMostrados(texto: String): Int {
        val mantisa = texto.substringBefore('e')
        return if ('.' in mantisa) mantisa.substringAfter('.').length else 0
    }

    /** Compara el texto mostrado con el valor de la hoja, con media unidad del último decimal mostrado. */
    private fun assertMuestra(esperado: Double, mostrado: String, que: String) {
        val v = mostrado.toDoubleOrNull() ?: throw AssertionError("$que: «$mostrado» no es un número")
        val exponente = if ('e' in mostrado) mostrado.substringAfter('e').toInt() else 0
        val tol = 0.5 * 10.0.pow(-decimalesMostrados(mostrado) + exponente) * (1 + 1e-9)
        assertTrue(abs(v - esperado) <= tol, "$que: la app muestra «$mostrado» y la hoja dice $esperado (tol $tol)")
    }

    private fun SalidaMetodo.tabla(titulo: String): BloqueTabla =
        assertNotNull(bloques.filterIsInstance<BloqueTabla>().firstOrNull { it.titulo == titulo }, "Falta la tabla «$titulo»")

    // ── Regresión polinomial ──

    @Test
    fun regresionCincoPuntos() {
        val g1 = ejecutar(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1, 2, 3, 4, 5", "9, 7.5, 4.2, 3, 2.1", "1"))
        assertMuestra(1.683000, g1.valor("Q"), "Q grado 1")
        assertMuestra(0.952149, g1.valor("R²"), "R² grado 1")

        val g2 = ejecutar(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1, 2, 3, 4, 5", "9, 7.5, 4.2, 3, 2.1", "2", "2.5"))
        assertMuestra(0.905143, g2.valor("Q"), "Q grado 2")
        assertMuestra(0.974265, g2.valor("R²"), "R² grado 2")
        assertMuestra(5.6625, g2.valor("P(2.5)"), "P(2.5)")
    }

    @Test
    fun regresionSieteDatos() {
        val xs = "50, 52, 54, 56, 58, 60, 62"
        val ys = "31, 50, 77, 96, 112, 130, 162"
        val g1 = ejecutar(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial(xs, ys, "1"))
        assertMuestra(94.0, g1.valor("Q"), "Q grado 1")
        val g2 = ejecutar(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial(xs, ys, "2"))
        assertMuestra(91.666667, g2.valor("Q"), "Q grado 2")
    }

    @Test
    fun regresionSeisDatosPrediccion() {
        val s = ejecutar(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("0, 1, 2, 3, 4, 5", "2.1, 7.7, 13.6, 27.2, 40.9, 61.1", "2", "7"))
        assertMuestra(110.168571, s.valor("P(7)"), "P(7)")
        assertMuestra(1.117523, s.valor("S"), "S")
    }

    @Test
    fun regresionMultipleEjemplo() {
        val s = ejecutar(MetodoId.REGRESION_MULTIPLE, Catalogo.ejemplo(MetodoId.REGRESION_MULTIPLE))
        assertEquals("y = 5.000000 + 4.000000x₁ − 3.000000x₂", s.valor("Ecuación"))
        // Desde la Fase 8 los coeficientes van en el paso "Modelo y sistema normal" con el nombre β.
        val matrices = s.bloques.filterIsInstance<com.example.pentametod2.model.BloquePaso>().flatMap { it.matrices }
        val a = assertNotNull(matrices.firstOrNull { it.nombre == "β" }, "Falta el vector β")
        val mostrados = a.datos.map { com.example.pentametod2.model.Formato.auto(it.single(), a.decimales) }
        for ((esperado, m) in listOf(5.0, 4.0, -3.0).zip(mostrados)) assertMuestra(esperado, m, "coeficiente")
        assertEquals("1.000000", s.valor("R²"))
    }

    // ── Interpolación ──

    @Test
    fun lagrangeTresNodos() {
        val s = ejecutar(MetodoId.LAGRANGE, EntradaInterpolacion("2.2, 2.4, 2.6", "0.5207843, 0.5104147, 0.4813306", "2.5"))
        assertMuestra(0.498212, s.valor("P(2.5)"), "P(2.5)")

        val t = s.tabla("Nodos y polinomios de Lagrange")
        assertTrue(t.filaTotal)
        val ultima = t.columnas.lastIndex
        val total = t.filas.last()
        assertEquals("Σ", textoCelda(total[0], t.decimales))
        val suma = t.filas.dropLast(1).sumOf { (it[ultima] as Celda.Num).v }
        assertEquals(suma, (total[ultima] as Celda.Num).v, 1e-12, "la fila Σ suma yᵢ·Lᵢ")
        assertEquals(s.valor("P(2.5)"), textoCelda(total[ultima], t.decimales), "la fila Σ muestra P(x)")
    }

    @Test
    fun newtonCincoNodos() {
        val s = ejecutar(
            MetodoId.NEWTON_INTERPOLACION,
            EntradaInterpolacion("2, 2.2, 2.4, 2.6, 2.8", "0.5103757, 0.5207843, 0.5104147, 0.4813306, 0.435916", "2.1"),
        )
        assertMuestra(0.518294, s.valor("P(2.1)"), "P(2.1)")

        val t = s.tabla("Tabla de diferencias divididas")
        val n = 5
        val primeraDif = t.columnas.indexOf("f[xₖ]")
        for (k in 0 until n) for (j in 0 until n) {
            val celda = t.filas[k][primeraDif + j]
            if (k + j >= n) assertEquals(Celda.Vacia, celda, "[$k][$j] bajo la diagonal") else assertTrue(celda is Celda.Num, "[$k][$j]")
        }
        assertEquals("0.510376", textoCelda(t.filas[0][primeraDif], t.decimales))
    }

    // ── Sistemas ──

    @Test
    fun newtonSistemas() {
        val s = ejecutar(MetodoId.NEWTON_SISTEMAS, EntradaSistema(listOf("x^2+y^2-4", "x-y"), "x, y", "1, 1"))
        assertEquals("x = 1.414214, y = 1.414214", s.valor("Solución"))
        assertEquals("Sí", s.valor("Convergió"))
    }

    @Test
    fun puntoFijoEjemplo() {
        val s = ejecutar(MetodoId.PUNTO_FIJO, Catalogo.ejemplo(MetodoId.PUNTO_FIJO))
        assertEquals("Sí", s.valor("Convergió"))
        val error = s.valor("Error final").toDouble()
        assertTrue(error < 1e-8, "error final $error")
    }
}
