package com.example.pentametod2.catalogo

import com.example.pentametod2.model.Bloque
import com.example.pentametod2.model.BloqueGrupo
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.Formato
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.ui.resultados.textoCelda
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/** Todos los textos que la interfaz mostraría, incluidos los números ya formateados. */
internal fun textosVisibles(s: SalidaMetodo): List<String> {
    val t = mutableListOf<String>()
    s.error?.let { t += it }
    t += s.advertencias
    for (r in s.resumen) t += listOf(r.etiqueta, r.valor)
    for (f in s.formulas) t += listOf(f.titulo) + f.general + f.sustituida
    s.grafica?.let { g -> t += listOf(g.titulo, g.ejeX, g.ejeY) + g.series.map { it.nombre } }
    for (b in s.bloques) textosDe(b, t)
    return t
}

private fun textosDe(b: Bloque, t: MutableList<String>) {
    when (b) {
        is BloqueTexto -> t += b.texto
        is BloqueTabla -> {
            t += b.titulo
            t += b.columnas
            for (f in b.filas) for (c in f) t += textoCelda(c, b.decimales)
        }
        is BloqueMatriz -> {
            t += listOf(b.titulo, b.nombre)
            for (f in b.datos) for (v in f) t += Formato.auto(v, b.decimales)
        }
        is BloqueGrupo -> {
            t += b.titulo
            for (h in b.bloques) textosDe(h, t)
        }
        is BloquePaso -> {
            t += listOf(b.titulo, b.explicacion, b.resumenCorto) + b.formulaGeneral + b.conTusDatos
            for (r in b.resultados) t += listOf(r.etiqueta, r.texto)
            for (m in b.matrices) textosDe(m, t)
        }
    }
}

/** Números de tablas y matrices (deben ser finitos si el método no falló). */
private fun numeros(bloques: List<Bloque>): List<Double> = bloques.flatMap { numerosDe(it) }

private fun numerosDe(b: Bloque): List<Double> = when (b) {
    is BloqueTabla -> b.filas.flatten().filterIsInstance<Celda.Num>().map { it.v }
    is BloqueMatriz -> b.datos.flatten()
    is BloqueGrupo -> numeros(b.bloques)
    is BloquePaso -> numeros(b.matrices) + b.resultados.mapNotNull { it.valor }
    is BloqueTexto -> emptyList()
}

private val PROHIBIDOS = listOf("NaN", "Infinity", "null")

internal fun verificarSalida(id: MetodoId, e: Entrada, s: SalidaMetodo) {
    val donde = "$id con $e".take(300)
    assertEquals(id, s.metodo, donde)
    if (s.estado == Estado.ERROR) {
        val msg = s.error
        assertTrue(!msg.isNullOrBlank(), "ERROR sin mensaje: $donde")
        assertFalse(msg.contains("null"), "Mensaje con 'null': $msg ($donde)")
        return
    }
    for (texto in textosVisibles(s)) {
        for (p in PROHIBIDOS) assertFalse(texto.contains(p), "'$p' visible en «${texto.take(120)}» ($donde)")
    }
    for (r in s.resumen) assertFalse(r.valor.contains("∞"), "∞ en el resumen: $r ($donde)")
    for (v in numeros(s.bloques)) assertTrue(v.isFinite(), "Número no finito en una tabla/matriz ($donde)")
}

class RobustezTest {

    private val hostiles: List<String> = listOf(
        "", "   ", "\t\n", "NaN", "Infinity", "-Infinity", "1e999", "-1e999", "-", "+", ".", "e", "1e", ",,,", ";;",
        "∞", "1..2", "1,,2", "1e308", "-1e308", "1e308, -1e308, 1e308", "0", "-0", "0x10", "1_000", "１", "abc",
        "1 2 3", "1, 2, 3, 4, 5, 6, 7", "x", "1".repeat(10_000), "1,".repeat(5_000), "(".repeat(10_000),
    )

    private val ecuacionesHostiles: List<String> = listOf(
        "", "   ", "((((", "sin(", "x^^2", "1/0", "0/0", "1/x", "1/(x-1)", "sqrt(-1)", "ln(0)", "ln(x-5)", "asin(2)",
        "x^1e308", "e^(e^(e^(x+10)))", "NaN", "∞", "x+".repeat(150), "(".repeat(10_000), "x".repeat(10_000),
        "w", "x y", "sin x", "2^-", "tan(pi/2)*x",
    )

    private fun lista(n: Int, f: (Int) -> String): String = (0 until n).joinToString(", ") { f(it) }

    private fun entradasSistema(): List<EntradaSistema> {
        val base = EntradaSistema(listOf("x^2+y^2-4", "x-y"), "x, y", "1, 1", "", "")
        val r = mutableListOf<EntradaSistema>()
        for (h in ecuacionesHostiles) {
            r += base.copy(ecuaciones = listOf(h, "x-y"))
            r += base.copy(ecuaciones = listOf("x-y", h))
        }
        for (h in hostiles) {
            r += base.copy(variables = h)
            r += base.copy(x0 = h)
            r += base.copy(tolerancia = h)
            r += base.copy(maxIter = h)
        }
        r += base.copy(ecuaciones = emptyList())
        r += base.copy(ecuaciones = List(7) { "x" }, variables = lista(7) { "x$it" }, x0 = lista(7) { "1" })
        r += base.copy(ecuaciones = List(6) { i -> "x$i-1" }, variables = lista(6) { "x$it" }, x0 = lista(6) { "0" })
        r += base.copy(x0 = "1e308, 1e308")
        r += base.copy(x0 = "-1e308, 1e308")
        r += base.copy(ecuaciones = listOf("x^2+1", "y^2+1")) // sin raíz real
        r += base.copy(ecuaciones = listOf("e^x", "e^y"), x0 = "0, 0")
        r += base.copy(maxIter = "10000", tolerancia = "1e-300")
        r += base.copy(tolerancia = "1e-400")
        r += base.copy(variables = "x, x")
        r += base.copy(variables = "sin, y")
        r += base.copy(variables = "pi, e")
        return r
    }

    private fun entradasInterpolacion(): List<EntradaInterpolacion> {
        val base = EntradaInterpolacion("2, 2.2, 2.4, 2.6", "0.5, 0.52, 0.51, 0.48", "2.5", ModoNodos.MANUAL, "")
        val auto = base.copy(modo = ModoNodos.AUTOMATICO, grado = "2")
        val r = mutableListOf<EntradaInterpolacion>()
        for (h in hostiles) {
            r += base.copy(xs = h)
            r += base.copy(ys = h)
            r += base.copy(x = h)
            r += auto.copy(grado = h)
        }
        r += base.copy(xs = lista(500) { "$it" }, ys = lista(500) { "1" })
        r += base.copy(xs = lista(200) { "${it * 0.01}" }, ys = lista(200) { "${sin(it * 0.01)}" }, x = "0.5")
        r += base.copy(xs = lista(200) { "$it" }, ys = lista(200) { "${it % 7}" }, x = "199.5")
        r += auto.copy(xs = lista(30) { "$it" }, ys = lista(30) { "${it * it}" }, x = "15", grado = "20")
        r += auto.copy(xs = lista(30) { "$it" }, ys = lista(30) { "${it * it}" }, x = "15", grado = "12")
        r += base.copy(xs = "1, 1, 2", ys = "1, 2, 3")
        r += base.copy(xs = "1, 1.0000000000000002", ys = "1, 2")
        r += base.copy(xs = "1e308, -1e308", ys = "1, 2", x = "0")
        r += base.copy(xs = "0, 1", ys = "1e308, -1e308", x = "1e308")
        r += base.copy(xs = "0, 1e-300", ys = "0, 1", x = "1")
        r += base.copy(x = "1e12")
        r += auto.copy(grado = "9")
        return r
    }

    private fun entradasRegresion(): List<EntradaRegresionPolinomial> {
        val base = EntradaRegresionPolinomial("1, 2, 3, 4, 5", "9, 7.5, 4.2, 3, 2.1", "2", "2.5")
        val r = mutableListOf<EntradaRegresionPolinomial>()
        for (h in hostiles) {
            r += base.copy(xs = h)
            r += base.copy(ys = h)
            r += base.copy(grado = h)
            r += base.copy(xPredecir = h)
        }
        r += base.copy(grado = "20")
        r += base.copy(xs = lista(500) { "$it" }, ys = lista(500) { "$it" })
        r += base.copy(xs = lista(200) { "$it" }, ys = lista(200) { "${it * 0.5}" }, grado = "10")
        r += base.copy(xs = lista(30) { "${1000 + it}" }, ys = lista(30) { "$it" }, grado = "10")
        r += base.copy(xs = "1e308, -1e308, 1, 2", ys = "1, 2, 3, 4", grado = "1")
        r += base.copy(xs = "1e12, 2e12, 3e12, 4e12", ys = "1, 2, 3, 4", grado = "3")
        r += base.copy(xs = "1, 2, 3", ys = "1e308, 1e308, 1e308", grado = "1")
        r += base.copy(xs = "2, 2, 2, 2", ys = "1, 2, 3, 4", grado = "1")
        r += base.copy(ys = "5, 5, 5, 5, 5", grado = "1")
        r += base.copy(xPredecir = "1e308")
        return r
    }

    private fun entradasMultiple(): List<EntradaRegresionMultiple> {
        val filas = listOf(
            listOf("0", "0", "5"), listOf("2", "1", "10"), listOf("2.5", "2", "9"),
            listOf("1", "3", "0"), listOf("4", "6", "3"), listOf("7", "2", "27"),
        )
        val base = EntradaRegresionMultiple(filas, "1, 1")
        val r = mutableListOf<EntradaRegresionMultiple>()
        for (h in hostiles) {
            r += base.copy(filas = filas.mapIndexed { i, f -> if (i == 2) listOf(f[0], h, f[2]) else f })
            r += base.copy(xPredecir = h)
        }
        r += base.copy(filas = emptyList())
        r += base.copy(filas = listOf(listOf("1"), listOf("2")))
        r += base.copy(filas = filas + listOf(listOf("1", "2")))
        r += base.copy(filas = List(500) { i -> listOf("$i", "${i % 3}", "$i") })
        r += base.copy(filas = List(20) { i -> List(8) { j -> "${(i + 1) * (j + 2) % 11}" } }, xPredecir = "")
        r += base.copy(filas = List(10) { i -> listOf("$i", "${2 * i}", "${i + 1}") }, xPredecir = "")
        r += base.copy(filas = listOf(listOf("1e308", "1", "2"), listOf("-1e308", "2", "3"), listOf("1", "3", "4"), listOf("2", "5", "1")))
        r += base.copy(filas = List(5) { listOf("1", "1", "1") })
        return r
    }

    private fun todas(): List<Pair<MetodoId, Entrada>> =
        entradasSistema().flatMap { listOf(MetodoId.PUNTO_FIJO to it, MetodoId.NEWTON_SISTEMAS to it) } +
            entradasInterpolacion().flatMap { listOf(MetodoId.LAGRANGE to it, MetodoId.NEWTON_INTERPOLACION to it) } +
            entradasRegresion().map { MetodoId.REGRESION_POLINOMIAL to it } +
            entradasMultiple().map { MetodoId.REGRESION_MULTIPLE to it }

    @Test
    fun nuncaLanzaConEntradasHostiles() {
        val casos = todas()
        assertTrue(casos.size >= 40)
        for (id in MetodoId.entries) assertTrue(casos.count { it.first == id } >= 40, "pocos casos para $id")
        val fallos = mutableListOf<String>()
        for ((id, e) in casos) {
            try {
                val s = Catalogo.ejecutar(id, e)
                verificarSalida(id, e, s)
            } catch (t: Throwable) {
                fallos += "${t::class.simpleName}: ${t.message?.take(300)}"
            }
        }
        if (fallos.isNotEmpty()) fail("${fallos.size} de ${casos.size} casos fallaron:\n" + fallos.distinct().take(25).joinToString("\n"))
    }

    private fun error(id: MetodoId, e: Entrada, pista: String) {
        val s = Catalogo.ejecutar(id, e)
        assertEquals(Estado.ERROR, s.estado, "Se esperaba ERROR: $e".take(200))
        assertTrue(s.error!!.contains(pista), "«${s.error}» no menciona «$pista»")
    }

    @Test
    fun limites() {
        val interp = EntradaInterpolacion("", "", "1", ModoNodos.AUTOMATICO, "2")
        error(MetodoId.LAGRANGE, interp.copy(xs = lista(201) { "$it" }, ys = lista(201) { "1" }), "el máximo es 200")
        error(MetodoId.LAGRANGE, interp.copy(xs = lista(30) { "$it" }, ys = lista(30) { "1" }, grado = "13"), "no puede pasar de 12")
        error(
            MetodoId.NEWTON_INTERPOLACION,
            interp.copy(xs = lista(14) { "$it" }, ys = lista(14) { "1" }, modo = ModoNodos.MANUAL),
            "el máximo es 13",
        )
        // 200 datos y grado 12 sí se aceptan
        val ok = Catalogo.ejecutar(MetodoId.LAGRANGE, interp.copy(xs = lista(200) { "${it * 0.1}" }, ys = lista(200) { "${sin(it * 0.1)}" }, x = "10", grado = "12"))
        assertEquals(Estado.OK, ok.estado, ok.error)

        error(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial(lista(30) { "$it" }, lista(30) { "$it" }, "11"), "no puede pasar de 10")
        error(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1, 1e13", "1, 2", "1"), "demasiado grande")
        error(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1, 1e999", "1, 2", "1"), "demasiado grande")
        error(MetodoId.REGRESION_MULTIPLE, EntradaRegresionMultiple(List(201) { listOf("$it", "1") }), "el máximo es 200")
        error(MetodoId.REGRESION_MULTIPLE, EntradaRegresionMultiple(List(10) { i -> List(8) { "${i + it}" } }), "el máximo es 6")

        val sis = EntradaSistema(List(7) { "x$it" }, lista(7) { "x$it" }, lista(7) { "0" })
        error(MetodoId.NEWTON_SISTEMAS, sis, "el máximo es 6")
        error(MetodoId.PUNTO_FIJO, EntradaSistema(listOf("x".repeat(201), "y"), "x, y", "0, 0"), "Ecuación 1: es demasiado larga")
        error(MetodoId.PUNTO_FIJO, EntradaSistema(listOf("(".repeat(10_000), "y"), "x, y", "0, 0"), "Ecuación 1")
        error(MetodoId.PUNTO_FIJO, EntradaSistema(listOf("x", "y"), "x, y", "0, 0", "", "501"), "no puede pasar de 500")
        // 6 variables sí
        val seis = Catalogo.ejecutar(
            MetodoId.NEWTON_SISTEMAS,
            EntradaSistema(List(6) { "x$it-${it + 1}" }, lista(6) { "x$it" }, lista(6) { "0" }),
        )
        assertEquals(Estado.OK, seis.estado, seis.error)
        // Mensajes cortos aunque la entrada sea enorme
        val largo = Catalogo.ejecutar(MetodoId.LAGRANGE, EntradaInterpolacion("a".repeat(10_000), "1", "1"))
        assertTrue(largo.error!!.length < 200, largo.error)
    }

    @Test
    fun redDeSeguridadConvierteNoFinitosEnError() {
        // Nodos casi iguales: la diferencia dividida se desborda aunque los datos sean válidos.
        val s = Catalogo.ejecutar(MetodoId.NEWTON_INTERPOLACION, EntradaInterpolacion("0, 1e-300, 2e-300, 3e-300", "0, 1, 0, 1", "1"))
        assertEquals(Estado.ERROR, s.estado)
        assertEquals(Catalogo.MENSAJE_DESBORDE, s.error)
        assertTrue(s.bloques.isNotEmpty(), "se conservan las tablas para ver dónde se desbordó")
    }

    @Test
    fun entradaDeOtroMetodoNoLanza() {
        val entradas = MetodoId.entries.map { Catalogo.ejemplo(it) }
        for (id in MetodoId.entries) for (e in entradas) verificarSalida(id, e, Catalogo.ejecutar(id, e))
    }

    @Test
    fun ejemplosSinTextosProhibidos() {
        for (id in MetodoId.entries) {
            val e = Catalogo.ejemplo(id)
            val s = Catalogo.ejecutar(id, e)
            assertEquals(Estado.OK, s.estado, "$id: ${s.error}")
            verificarSalida(id, e, s)
            assertTrue(textosVisibles(s).size > 10, "$id: se esperaban más textos")
        }
    }
}
