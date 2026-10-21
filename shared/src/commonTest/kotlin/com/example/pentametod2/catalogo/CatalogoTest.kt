package com.example.pentametod2.catalogo

import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogoTest {

    private fun SalidaMetodo.valor(etiqueta: String): String =
        assertNotNull(resumen.firstOrNull { it.etiqueta == etiqueta }, "Falta '$etiqueta' en $resumen").valor

    private fun error(id: MetodoId, entrada: Entrada, vararg pistas: String) {
        val s = Catalogo.ejecutar(id, entrada)
        assertEquals(Estado.ERROR, s.estado, "Se esperaba ERROR para $entrada")
        val msg = assertNotNull(s.error)
        assertTrue(pistas.any { msg.contains(it, ignoreCase = true) }, "El mensaje '$msg' no menciona ${pistas.toList()}")
    }

    @Test
    fun catalogoOrdenado() {
        val m = Catalogo.metodos()
        assertEquals(
            listOf(
                MetodoId.PUNTO_FIJO, MetodoId.NEWTON_SISTEMAS, MetodoId.REGRESION_POLINOMIAL,
                MetodoId.REGRESION_MULTIPLE, MetodoId.LAGRANGE, MetodoId.NEWTON_INTERPOLACION,
            ),
            m.map { it.id },
        )
        assertEquals(listOf(3, 3, 4, 4, 5, 5), m.map { it.semana })
        assertEquals(MetodoId.entries.toSet(), m.map { it.id }.toSet())
    }

    @Test
    fun ejemplos() {
        for (id in MetodoId.entries) {
            val s = Catalogo.ejecutar(id, Catalogo.ejemplo(id))
            assertEquals(Estado.OK, s.estado, "$id: ${s.error} ${s.advertencias}")
        }
        val pol = Catalogo.ejecutar(MetodoId.REGRESION_POLINOMIAL, Catalogo.ejemplo(MetodoId.REGRESION_POLINOMIAL))
        assertTrue(pol.valor("P(2.5)").startsWith("5.6625"))

        val mul = Catalogo.ejecutar(MetodoId.REGRESION_MULTIPLE, Catalogo.ejemplo(MetodoId.REGRESION_MULTIPLE))
        assertEquals(6.0, mul.valor("Predicción (1, 1)").toDouble(), 1e-6)

        val lag = Catalogo.ejecutar(MetodoId.LAGRANGE, Catalogo.ejemplo(MetodoId.LAGRANGE))
        assertTrue(lag.valor("P(2.5)").contains("0.49821"), lag.valor("P(2.5)"))

        val nwt = Catalogo.ejecutar(MetodoId.NEWTON_INTERPOLACION, Catalogo.ejemplo(MetodoId.NEWTON_INTERPOLACION))
        assertTrue(nwt.valor("P(2.1)").contains("0.518294"), nwt.valor("P(2.1)"))

        val ns = Catalogo.ejecutar(MetodoId.NEWTON_SISTEMAS, Catalogo.ejemplo(MetodoId.NEWTON_SISTEMAS))
        assertEquals("x = 1.414214, y = 1.414214", ns.valor("Solución"))

        val pf = Catalogo.ejecutar(MetodoId.PUNTO_FIJO, Catalogo.ejemplo(MetodoId.PUNTO_FIJO))
        assertEquals("Sí", pf.valor("Convergió"))
    }

    @Test
    fun interpolacionAutomatica() {
        val e = EntradaInterpolacion(
            "2, 2.2, 2.4, 2.6, 2.8", "0.510376, 0.520784, 0.510415, 0.481331, 0.435916", "2.5",
            ModoNodos.AUTOMATICO, "1",
        )
        val s = Catalogo.ejecutar(MetodoId.LAGRANGE, e)
        assertEquals(Estado.OK, s.estado, s.error)
        assertEquals("0.495873", s.valor("P(2.5)")) // nodos 2.4 y 2.6
    }

    @Test
    fun separadoresDeLista() {
        val s = Catalogo.ejecutar(
            MetodoId.REGRESION_POLINOMIAL,
            EntradaRegresionPolinomial("1; 2\t3\n4  5", "9, 7.5; 4.2 3\n2.1", "1"),
        )
        assertEquals(Estado.OK, s.estado, s.error)
        assertEquals("0.952149", s.valor("R²"))
    }

    @Test
    fun erroresAmigables() {
        val lag = MetodoId.LAGRANGE
        error(lag, EntradaInterpolacion("1, abc, 3", "1, 2, 3", "2"), "nodos x", "'abc'")
        error(lag, EntradaInterpolacion("1, 2, 3", "1, 2", "2"), "valores de x", "valores de y")
        error(lag, EntradaInterpolacion("1, 2, 3", "1, 2, 3", ""), "valor de x")
        error(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1,2,3", "1,2,3", ""), "grado")
        error(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1,2,3", "1,2,3", "3"), "puntos")
        error(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1,2,3", "1,2,3", "1", "x"), "predecir")

        val nw = MetodoId.NEWTON_SISTEMAS
        error(nw, EntradaSistema(listOf("x^2+y^2-4", "x^2+"), "x, y", "1, 1"), "Ecuación 2")
        error(nw, EntradaSistema(listOf("x+y", "x-y"), "x, y, z", "1, 1, 1"), "ecuaciones", "variables")
        error(nw, EntradaSistema(listOf("x+y", "x-y"), "x, y", "1"), "punto inicial")
        error(nw, EntradaSistema(listOf("x+y", "x-y"), "x, y", "1, 1", "-1"), "tolerancia")
        error(nw, EntradaSistema(listOf("x+y", "x-w"), "x, y", "1, 1"), "Ecuación 2", "'w'")
        error(MetodoId.PUNTO_FIJO, EntradaSistema(listOf("x", "y"), "x, y", "0, 0", "", "0"), "iteraciones")

        error(
            MetodoId.REGRESION_MULTIPLE,
            EntradaRegresionMultiple(listOf(listOf("1", "2", "3"), listOf("2", "5"), listOf("3", "1", "4"), listOf("4", "2", "1"))),
            "fila 2",
        )
    }

    @Test
    fun valoresPorDefecto() {
        val s = Catalogo.ejecutar(MetodoId.NEWTON_SISTEMAS, EntradaSistema(listOf("x^2+y^2-4", "x-y"), "x, y", "1, 1", "", ""))
        assertEquals(Estado.OK, s.estado, s.error)
        assertTrue(s.formulas.single().sustituida.last().contains("1e-6"))
    }

    @Test
    fun noLanzaConEntradaIncorrecta() {
        val s = Catalogo.ejecutar(MetodoId.LAGRANGE, Catalogo.ejemplo(MetodoId.NEWTON_SISTEMAS))
        assertEquals(Estado.ERROR, s.estado)
        assertTrue(assertNotNull(s.error).contains("no corresponden"))
        assertEquals(Estado.ERROR, Catalogo.ejecutar(MetodoId.REGRESION_MULTIPLE, EntradaRegresionMultiple(emptyList())).estado)
        assertEquals(Estado.ERROR, Catalogo.ejecutar(MetodoId.PUNTO_FIJO, EntradaSistema(emptyList(), "", "")).estado)
    }

    @Test
    fun noConvergenciaEsError() {
        val s = Catalogo.ejecutar(MetodoId.PUNTO_FIJO, EntradaSistema(listOf("2x+y", "2y+x"), "x, y", "1, 1"))
        assertEquals(Estado.ERROR, s.estado)
        assertTrue(assertNotNull(s.error).contains("diverge"))
        assertTrue(s.bloques.isNotEmpty())
    }
}
