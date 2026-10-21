package com.example.pentametod2.exportar

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.EntradaInterpolacion
import com.example.pentametod2.catalogo.EntradaRegresionMultiple
import com.example.pentametod2.catalogo.EntradaSistema
import com.example.pentametod2.catalogo.ModoNodos
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.model.TipoTexto
import com.example.pentametod2.ui.resultados.yaEnUnPaso
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Fase 8: los 5 métodos restantes con la misma estructura que la regresión polinomial. */
class MetodosExportablesTest {

    private fun ejemplo(id: MetodoId): SalidaMetodo = Catalogo.ejecutar(id, Catalogo.ejemplo(id))

    private fun SalidaMetodo.pasos() = bloques.filterIsInstance<BloquePaso>()

    private fun SalidaMetodo.resultado(etiqueta: String): String =
        pasos().flatMap { it.resultados }.first { it.etiqueta == etiqueta }.texto

    /** Reglas comunes: pasos numerados 1…n, solo el primero abierto, todos completos y el último destacado. */
    private fun comprobarEstructura(s: SalidaMetodo, titulos: List<String>) {
        val p = s.pasos()
        assertEquals(titulos, p.map { it.titulo })
        assertEquals((1..titulos.size).toList(), p.map { it.numero })
        assertEquals(listOf(true) + List(titulos.size - 1) { false }, p.map { it.abiertoInicial })
        assertTrue(p.all { it.explicacion.isNotBlank() && it.formulaGeneral.isNotEmpty() && it.resultados.isNotEmpty() })
        assertTrue(p.last().destacado)
        // La tabla principal sigue tras la lista (no está dentro de un paso) y la conclusión es lo último.
        assertTrue(s.bloques.any { it is BloqueTabla && !yaEnUnPaso(it, s) }, "falta la tabla principal")
        val ultimo = s.bloques.last()
        assertTrue(ultimo is BloqueTexto && (ultimo.tipo == TipoTexto.CONCLUSION || ultimo.tipo == TipoTexto.ADVERTENCIA), "$ultimo")
    }

    /** Los comentarios "% n. Título" del .txt, en el orden de los pasos de la pantalla. */
    private fun comprobarComentarios(octave: String, s: SalidaMetodo) {
        var desde = 0
        for (p in s.pasos()) {
            val c = "% ${p.numero}. ${p.titulo}\n"
            val i = octave.indexOf(c, desde)
            assertTrue(i >= 0, "falta o fuera de orden: $c")
            desde = i
        }
        assertFalse(octave.contains("\r"))
        assertFalse(octave.contains("fprintf(") || octave.contains("end;"), "nada de MATLAB-only")
        assertTrue(octave.contains("printf("))
    }

    private fun libro(s: SalidaMetodo, entrada: com.example.pentametod2.catalogo.Entrada? = null): Map<String, String> {
        val partes = ZipDePrueba.leer(Exportaciones.contenido(s, TipoExportacion.EXCEL, entrada))
        for ((nombre, xml) in partes) assertTrue(ZipDePrueba.xmlBalanceado(xml), "XML mal formado: $nombre")
        return partes
    }

    private fun contieneNumeros(partes: Map<String, String>, vararg esperados: Double) {
        val numeros = ZipDePrueba.numeros(partes)
        for (e in esperados) assertTrue(numeros.any { abs(it - e) < 5e-7 }, "no aparece $e")
    }

    // ── Regresión múltiple ──

    @Test
    fun regresionMultiple() {
        val s = ejemplo(MetodoId.REGRESION_MULTIPLE)
        comprobarEstructura(
            s,
            listOf(
                "Modelo y sistema normal", "Ecuación del modelo", "Suma de cuadrados de residuos", "Media y variación de y",
                "Coeficiente de determinación", "Error estándar", "Predicción",
            ),
        )
        assertEquals(listOf("XᵀX", "Xᵀy", "(XᵀX)⁻¹", "β"), s.pasos()[0].matrices.map { it.nombre })
        assertEquals(listOf("5.000000", "4.000000", "-3.000000"), s.pasos()[1].resultados.map { it.texto })
        assertEquals("1.000000", s.resultado("R²"))
        assertEquals("9.000000", s.resultado("ȳ"))
        assertEquals("458.000000", s.resultado("S_yy"))
        assertEquals("6.000000", s.resultado("Predicción (1, 1)"))
        assertEquals("regresion_multiple_k2.txt", Exportaciones.nombreArchivo(s, TipoExportacion.OCTAVE))

        val sinPunto = Catalogo.ejecutar(MetodoId.REGRESION_MULTIPLE, (Catalogo.ejemplo(MetodoId.REGRESION_MULTIPLE) as EntradaRegresionMultiple).copy(xPredecir = ""))
        assertEquals(6, sinPunto.pasos().size)

        val t = Exportaciones.octave(s)
        comprobarComentarios(t, s)
        assertTrue(t.contains("x1 = [0 2 2.5 1 4 7]';"), t)
        assertTrue(t.contains("x2 = [0 1 2 3 6 2]';"))
        assertTrue(t.contains("y = [5 10 9 0 3 27]';"))
        assertTrue(t.contains("X = [ones(n,1) x1 x2];"))
        assertTrue(t.contains("beta = (X'*X)\\(X'*y);"))
        assertTrue(t.contains("xp = [1 1];"))
        assertFalse(Exportaciones.octave(sinPunto).contains("xp ="))

        val partes = libro(s)
        assertEquals(listOf("Datos y sumatorias", "Sistema normal", "Resultados", "Entrada"), ZipDePrueba.hojas(partes))
        assertTrue(ZipDePrueba.hoja(partes, "Datos y sumatorias").contains("<f>SUM(B2:B7)</f>"))
        contieneNumeros(partes, 5.0, 4.0, -3.0, 458.0, 6.0, 16.5, 76.25)
    }

    // ── Lagrange ──

    @Test
    fun lagrange() {
        val s = ejemplo(MetodoId.LAGRANGE)
        comprobarEstructura(s, listOf("Nodos usados", "Polinomio de Lagrange", "Cada Lₖ(x) con tus datos", "Evaluación"))
        assertEquals("0.498212", s.resultado("P(2.5)"))
        assertEquals(listOf("-0.125000", "0.750000", "0.375000"), s.pasos()[2].resultados.map { it.texto })
        assertTrue(s.pasos()[3].conTusDatos.contains("Comprobación: Σ Lₖ(2.5) = 1.000000"))
        assertTrue(s.pasos()[0].explicacion.contains("exactamente los 3 nodos"))
        assertEquals("lagrange_grado2.xlsx", Exportaciones.nombreArchivo(s, TipoExportacion.EXCEL))

        val t = Exportaciones.octave(s)
        comprobarComentarios(t, s)
        assertTrue(t.contains("xs = [2.2 2.4 2.6];"), t)
        assertTrue(t.contains("ys = [0.5207843 0.5104147 0.4813306];"))
        assertTrue(t.contains("x = 2.5;"))
        assertTrue(t.contains("plot(xs, ys, \"o\", t, Pt, \"-\", x, P, \"*\");"))

        val partes = libro(s)
        assertEquals(listOf("Lagrange", "Resultados", "Entrada"), ZipDePrueba.hojas(partes))
        val hoja = ZipDePrueba.hoja(partes, "Lagrange")
        assertTrue(hoja.contains("<f>SUM(D2:D4)</f><v>1</v>"), "Σ Lₖ = 1 como fórmula")
        assertTrue(hoja.contains("<f>SUM(E2:E4)</f>"))
        contieneNumeros(partes, 0.498212, -0.125, 0.75, 0.375)
    }

    @Test
    fun lagrangeAutomaticoLoDice() {
        val e = EntradaInterpolacion("2, 2.2, 2.4, 2.6, 2.8", "0.5103757, 0.5207843, 0.5104147, 0.4813306, 0.435916", "2.5", ModoNodos.AUTOMATICO, "2")
        val s = Catalogo.ejecutar(MetodoId.LAGRANGE, e)
        assertTrue(s.pasos()[0].explicacion.contains("se eligieron los 3 nodos más cercanos a x = 2.5 (de los 5 que diste)"), s.pasos()[0].explicacion)
        assertEquals("lagrange_grado2.txt", Exportaciones.nombreArchivo(s, TipoExportacion.OCTAVE))
    }

    // ── Newton (diferencias divididas) ──

    @Test
    fun newtonInterpolacion() {
        val s = ejemplo(MetodoId.NEWTON_INTERPOLACION)
        comprobarEstructura(
            s,
            listOf("Nodos usados", "Tabla de diferencias divididas", "Polinomio de Newton", "Términos", "Evaluación por grado"),
        )
        // La tabla triangular va dentro del paso 2; debajo de la lista queda la de términos.
        val dif = s.bloques.filterIsInstance<BloqueTabla>().first { it.titulo == "Tabla de diferencias divididas" }
        assertTrue(yaEnUnPaso(dif, s))
        assertFalse(yaEnUnPaso(s.bloques.filterIsInstance<BloqueTabla>().first { it.titulo == "Términos de Newton" }, s))
        assertEquals(listOf("0.510376", "0.515580", "0.518177", "0.518306", "0.518294"), s.pasos()[4].resultados.map { it.texto })
        assertEquals("0.518294", s.pasos()[4].resumenCorto)
        assertEquals("newton_dif_divididas_grado4.txt", Exportaciones.nombreArchivo(s, TipoExportacion.OCTAVE))

        val t = Exportaciones.octave(s)
        comprobarComentarios(t, s)
        assertTrue(t.contains("xs = [2 2.2 2.4 2.6 2.8];"), t)
        assertTrue(t.contains("ys = [0.5103757 0.5207843 0.5104147 0.4813306 0.435916];"))
        assertTrue(t.contains("D(i, j) = (D(i+1, j-1) - D(i, j-1))/(xs(i+j-1) - xs(i));"))
        assertTrue(t.contains("printf(\"P%d(%g) = %.6f"))
        assertTrue(t.contains("plot("))

        val partes = libro(s)
        assertEquals(listOf("Diferencias divididas", "Términos", "Resultados", "Entrada"), ZipDePrueba.hojas(partes))
        assertTrue(ZipDePrueba.hoja(partes, "Diferencias divididas").contains(" s=\"${EstiloX.DECIMAL7}\""), "7 decimales como los datos")
        assertTrue(partes.getValue("xl/styles.xml").contains("formatCode=\"0.0000000\""))
        assertTrue(ZipDePrueba.hoja(partes, "Términos").contains("<f>SUM(D2:D6)</f>"))
        contieneNumeros(partes, 0.5103757, 0.052043, -0.2597275, 0.5182937445, 0.51558)
    }

    // ── Punto fijo ──

    @Test
    fun puntoFijo() {
        val s = ejemplo(MetodoId.PUNTO_FIJO)
        comprobarEstructura(s, listOf("Sistema y función de iteración", "Punto inicial y criterio", "Primera iteración", "Resultado"))
        assertEquals(listOf("x = 0.5*cos(y)", "y = 0.5*sin(x)"), s.pasos()[0].conTusDatos)
        assertTrue(s.pasos()[1].conTusDatos.contains("máximo de iteraciones = 50"))
        assertEquals("0.500000", s.resultado("E₁"))
        assertEquals("0.486405", s.resultado("x*"))
        assertEquals("0.233725", s.resultado("y*"))
        assertEquals("13", s.resultado("Iteraciones"))
        assertEquals("Convergió en 13 iteraciones (E < 1e-8)", s.pasos()[3].conTusDatos[0])
        assertEquals("punto_fijo_sistemas.xlsx", Exportaciones.nombreArchivo(s, TipoExportacion.EXCEL))

        val t = Exportaciones.octave(s, Catalogo.ejemplo(MetodoId.PUNTO_FIJO))
        comprobarComentarios(t, s)
        assertTrue(t.contains("g1 = @(x, y) 0.5*cos(y);"), t)
        assertTrue(t.contains("g2 = @(x, y) 0.5*sin(x);"))
        assertTrue(t.contains("G = @(v) [g1(v(1), v(2)); g2(v(1), v(2))];"))
        assertTrue(t.contains("xk = [0; 0];") && t.contains("tol = 1e-8;") && t.contains("maxIter = 50;"))
        assertTrue(t.contains("E = norm(xn - xk, Inf);"))
        assertTrue(t.contains("semilogy("))

        val partes = libro(s, Catalogo.ejemplo(MetodoId.PUNTO_FIJO))
        assertEquals(listOf("Iteraciones", "Resultados", "Entrada"), ZipDePrueba.hojas(partes))
        assertFalse(ZipDePrueba.hoja(partes, "Iteraciones").contains("<f>"), "las iteraciones son valores, no fórmulas")
        assertTrue(ZipDePrueba.hoja(partes, "Entrada").contains(">0.5*cos(y)<"))
        contieneNumeros(partes, 0.486405, 0.233725, 0.5, 1e-8, 50.0)
    }

    @Test
    fun puntoFijoDivergenteConservaElEstado() {
        val e = EntradaSistema(listOf("2x+y", "2y+x"), "x, y", "1, 1")
        val s = Catalogo.ejecutar(MetodoId.PUNTO_FIJO, e)
        assertEquals(Estado.ERROR, s.estado) // el estado actual de un sistema que no converge (no se cambia)
        assertTrue(s.error!!.contains("diverge"))
        val ultimo = s.pasos().last()
        assertEquals("No convergió", ultimo.resumenCorto)
        assertTrue(ultimo.explicacion.contains("no convergió"))
        assertEquals(s.error, ultimo.conTusDatos.first())
        // Se calculó (hay tabla): se puede exportar para revisar las iteraciones.
        assertTrue(Exportaciones.disponible(s, TipoExportacion.EXCEL))
        assertFalse(Exportaciones.sinCalculo(s))
        // Un error de entrada, en cambio, no exporta.
        val vacio = Catalogo.ejecutar(MetodoId.PUNTO_FIJO, EntradaSistema(listOf("", "x"), "x, y", "1, 1"))
        assertTrue(Exportaciones.sinCalculo(vacio))
        assertFalse(Exportaciones.disponible(vacio, TipoExportacion.OCTAVE))
    }

    // ── Newton-Raphson para sistemas ──

    @Test
    fun newtonSistemas() {
        val s = ejemplo(MetodoId.NEWTON_SISTEMAS)
        comprobarEstructura(s, listOf("Sistema F(x) = 0", "Jacobiano", "Método de Newton", "Primera iteración", "Resultado"))
        assertTrue(s.pasos()[1].formulaGeneral.contains("hⱼ = 6·10⁻⁶ · max(1, |xⱼ|)"))
        assertEquals(listOf("F(x⁽⁰⁾)", "J(x⁽⁰⁾)", "Δ"), s.pasos()[3].matrices.map { it.nombre })
        assertEquals("1.500000", s.resultado("x⁽¹⁾"))
        assertEquals("0.500000", s.resultado("E₁"))
        assertEquals("1.414214", s.resultado("x*"))
        assertEquals("5", s.resultado("Iteraciones"))
        assertEquals("newton_raphson_sistemas.txt", Exportaciones.nombreArchivo(s, TipoExportacion.OCTAVE))

        val t = Exportaciones.octave(s, Catalogo.ejemplo(MetodoId.NEWTON_SISTEMAS))
        comprobarComentarios(t, s)
        assertTrue(t.contains("F1 = @(x, y) x^2 + y^2 - 4;"), t)
        assertTrue(t.contains("F2 = @(x, y) x - y;"))
        assertTrue(t.contains("h = 6e-6*max(1, abs(xk(j)));"))
        assertTrue(t.contains("delta = J\\(-Fx);"))
        assertTrue(t.contains("E = norm(delta, Inf);"))
        assertTrue(t.contains("xk = [1; 1];"))

        val partes = libro(s, Catalogo.ejemplo(MetodoId.NEWTON_SISTEMAS))
        assertEquals(listOf("Iteraciones", "Primera iteración", "Resultados", "Entrada"), ZipDePrueba.hojas(partes))
        assertTrue(ZipDePrueba.hoja(partes, "Primera iteración").contains("Jacobiano en x⁽⁰⁾ (J(x⁽⁰⁾))"))
        contieneNumeros(partes, 1.414214, 1.5, 0.5, -2.0, 2.0)
    }

    @Test
    fun losSeisMetodosExportanSusEjemplos() {
        for (id in MetodoId.entries) {
            val s = ejemplo(id)
            assertTrue(Catalogo.puedeExportarExcel(id) && Catalogo.puedeExportarOctave(id), "$id")
            assertTrue(Exportaciones.disponible(s, TipoExportacion.EXCEL), "$id")
            assertTrue(Exportaciones.disponible(s, TipoExportacion.OCTAVE), "$id")
            val nombre = Exportaciones.nombreArchivo(s, TipoExportacion.OCTAVE)
            assertTrue(Regex("[a-z0-9_]+\\.txt").matches(nombre), nombre)
            assertTrue(Exportaciones.octave(s, Catalogo.ejemplo(id)).isNotBlank())
            assertTrue(ZipDePrueba.hojas(libro(s, Catalogo.ejemplo(id))).contains("Resultados"), "$id")
        }
    }
}
