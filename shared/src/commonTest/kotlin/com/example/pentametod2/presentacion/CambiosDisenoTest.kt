package com.example.pentametod2.presentacion

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.Secciones
import com.example.pentametod2.methods.Interpolacion
import com.example.pentametod2.methods.Regresion
import com.example.pentametod2.methods.SistemasNoLineales
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.TipoTexto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Cambios de modelo de la Fase 4B (Bloque G). */
class CambiosDisenoTest {

    private fun d(vararg x: Double): DoubleArray = x

    @Test
    fun prediccionDestacadaEnRegresion() {
        val xs = d(1.0, 2.0, 3.0, 4.0, 5.0)
        val ys = d(9.0, 7.5, 4.2, 3.0, 2.1)
        val s = PresentadorRegresion.presentarRegresionPolinomial(Regresion.regresionPolinomial(xs, ys, 2), xs, ys, 2.5)
        assertEquals(listOf("P(2.5)"), s.resumen.filter { it.destacado }.map { it.etiqueta })
        val sinPrediccion = PresentadorRegresion.presentarRegresionPolinomial(Regresion.regresionPolinomial(xs, ys, 2), xs, ys)
        assertTrue(sinPrediccion.resumen.none { it.destacado })

        val x = arrayOf(d(0.0, 0.0), d(2.0, 1.0), d(2.5, 2.0), d(1.0, 3.0), d(4.0, 6.0), d(7.0, 2.0))
        val y = d(5.0, 10.0, 9.0, 0.0, 3.0, 27.0)
        val m = PresentadorRegresion.presentarRegresionMultiple(Regresion.regresionMultiple(x, y), x, y, d(1.0, 1.0))
        assertEquals(listOf("Predicción (1, 1)"), m.resumen.filter { it.destacado }.map { it.etiqueta })
    }

    @Test
    fun valorEvaluadoDestacadoEnInterpolacion() {
        val r = Interpolacion.lagrange(d(2.2, 2.4, 2.6), d(0.520784, 0.510415, 0.481331), 2.5)
        val s = PresentadorInterpolacion.presentarLagrange(r)
        assertEquals(listOf("P(2.5)"), s.resumen.filter { it.destacado }.map { it.etiqueta })
        val n = PresentadorInterpolacion.presentarNewton(Interpolacion.newtonDiferenciasDivididas(d(2.0, 2.2), d(1.0, 2.0), 2.1))
        assertEquals(1, n.resumen.count { it.destacado })
    }

    @Test
    fun progresoEsR2EnLaConclusion() {
        val xs = d(1.0, 2.0, 3.0, 4.0, 5.0)
        val ys = d(9.0, 7.5, 4.2, 3.0, 2.1)
        val r = Regresion.regresionPolinomial(xs, ys, 2)
        val s = PresentadorRegresion.presentarRegresionPolinomial(r, xs, ys)
        val c = s.bloques.filterIsInstance<BloqueTexto>().single { it.tipo == TipoTexto.CONCLUSION }
        assertEquals(r.r2, assertNotNull(c.progreso), 1e-15)
        val x = arrayOf(d(0.0, 0.0), d(2.0, 1.0), d(2.5, 2.0), d(1.0, 3.0), d(4.0, 6.0), d(7.0, 2.0))
        val y = d(5.0, 10.0, 9.0, 0.0, 3.0, 27.0)
        val m = PresentadorRegresion.presentarRegresionMultiple(Regresion.regresionMultiple(x, y), x, y)
        val p = assertNotNull(m.bloques.filterIsInstance<BloqueTexto>().single { it.tipo == TipoTexto.CONCLUSION }.progreso)
        assertTrue(p in 0.0..1.0 && p > 0.999999, "progreso $p") // R² = 1 (acotado)
        // Los demás bloques de texto no traen progreso.
        val sis = Catalogo.ejecutar(MetodoId.NEWTON_SISTEMAS, Catalogo.ejemplo(MetodoId.NEWTON_SISTEMAS))
        assertTrue(sis.bloques.filterIsInstance<BloqueTexto>().all { it.progreso == null })
    }

    @Test
    fun serieToleranciaDiscontinua() {
        val gs = listOf("0.5*cos(y)", "0.5*sin(x)")
        val r = SistemasNoLineales.puntoFijoSistema(gs, listOf("x", "y"), d(0.0, 0.0), tol = 1e-8)
        val g = assertNotNull(PresentadorSistemas.presentarPuntoFijo(r, gs, 1e-8).grafica)
        val tol = g.series.single { it.nombre == PresentadorSistemas.NOMBRE_TOLERANCIA }
        assertTrue(tol.discontinua)
        assertEquals(2, tol.puntos.size)
        assertTrue(tol.puntos.all { it.y == 1e-8 })
        assertEquals(1.0, tol.puntos[0].x)
        assertEquals(r.iteraciones.last().k.toDouble(), tol.puntos[1].x)
        assertFalse(g.series.first { it.nombre == "E(k)" }.discontinua)

        val n = SistemasNoLineales.newtonSistema(listOf("x^2+y^2-4", "x-y"), listOf("x", "y"), d(1.0, 1.0))
        val gn = assertNotNull(PresentadorSistemas.presentarNewtonSistemas(n, listOf("x^2+y^2-4", "x-y"), 1e-6).grafica)
        assertTrue(gn.series.any { it.discontinua && it.puntos.all { p -> p.y == 1e-6 } })
        // Sin tolerancia no hay línea.
        assertNull(PresentadorSistemas.presentarPuntoFijo(r, gs).grafica!!.series.firstOrNull { it.discontinua })
    }

    @Test
    fun seccionesDelCatalogoSinSemana() {
        val m = Catalogo.metodos()
        assertEquals(
            listOf(Secciones.SISTEMAS, Secciones.SISTEMAS, Secciones.AJUSTE, Secciones.AJUSTE, Secciones.INTERPOLACION, Secciones.INTERPOLACION),
            m.map { it.seccion },
        )
        assertEquals(listOf("Sistemas no lineales", "Ajuste de curvas", "Interpolación"), Secciones.orden)
        for (info in m) {
            assertFalse(info.nombre.contains("Semana", ignoreCase = true))
            assertFalse(info.descripcion.contains("Semana", ignoreCase = true))
        }
        assertEquals("Itera x = G(x) hasta converger", Catalogo.info(MetodoId.PUNTO_FIJO).descripcion)
        assertEquals("Pasa exactamente por cada punto", Catalogo.info(MetodoId.LAGRANGE).descripcion)
    }

    @Test
    fun formulasSinGlifoGammaSubindice() {
        for (id in MetodoId.entries) {
            val s = Catalogo.ejecutar(id, Catalogo.ejemplo(id))
            for (f in s.formulas) for (l in f.general + f.sustituida) assertFalse(l.contains('ᵧ'), l)
        }
    }
}
