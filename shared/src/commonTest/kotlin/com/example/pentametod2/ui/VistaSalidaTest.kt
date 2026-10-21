package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.EntradaRegresionPolinomial
import com.example.pentametod2.catalogo.EntradaSistema
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.ui.componentes.TipoChip
import com.example.pentametod2.ui.resultados.AccionError
import com.example.pentametod2.ui.resultados.ElementoVista
import com.example.pentametod2.ui.resultados.agruparBloques
import com.example.pentametod2.ui.resultados.bannerAvisos
import com.example.pentametod2.ui.resultados.bannerError
import com.example.pentametod2.ui.resultados.chipConvergencia
import com.example.pentametod2.ui.resultados.chipResultados
import com.example.pentametod2.ui.resultados.esColumnaError
import com.example.pentametod2.ui.resultados.esLineaSecundaria
import com.example.pentametod2.ui.resultados.marcarUltimaFila
import com.example.pentametod2.ui.resultados.ocultarBloque
import com.example.pentametod2.ui.resultados.partesConPorcentajes
import com.example.pentametod2.ui.resultados.rotulosCoeficientes
import com.example.pentametod2.ui.resultados.tituloMatrices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VistaSalidaTest {

    private val polinomial = Catalogo.ejecutar(MetodoId.REGRESION_POLINOMIAL, Catalogo.ejemplo(MetodoId.REGRESION_POLINOMIAL))
    private val divergente = Catalogo.ejecutar(MetodoId.PUNTO_FIJO, EntradaSistema(listOf("2x+y", "2y+x"), "x, y", "1, 1"))

    @Test
    fun matricesConsecutivasEnUnaTarjeta() {
        // Desde la Fase 8 ningún método deja matrices sueltas (van dentro de los pasos): se prueba el agrupado
        // con la tabla y las cuatro matrices del formato anterior armadas a mano.
        val unos = listOf(listOf(1.0))
        val col = listOf(listOf(1.0), listOf(2.0), listOf(3.0))
        val bloques = listOf(
            com.example.pentametod2.model.BloqueTabla("Datos", listOf("i"), emptyList()),
            BloqueMatriz("Matriz del sistema normal", "C", unos),
            BloqueMatriz("Vector independiente", "b", unos),
            BloqueMatriz("Inversa de C", "C⁻¹", unos),
            BloqueMatriz("Coeficientes", "a", col),
        )
        val e = agruparBloques(bloques)
        val matrices = e.filterIsInstance<ElementoVista.Matrices>()
        assertEquals(1, matrices.size)
        assertEquals(listOf("C", "b", "C⁻¹", "a"), matrices[0].matrices.map { it.nombre })
        assertIs<ElementoVista.Simple>(e.first()) // la tabla de datos
        assertEquals("Sistema normal" to "C·a = b", tituloMatrices(matrices[0].matrices))
        val a = matrices[0].matrices.last()
        assertEquals(listOf("← a₀", "← a₁", "← a₂"), rotulosCoeficientes(a))
        assertNull(rotulosCoeficientes(matrices[0].matrices[0]))
    }

    @Test
    fun chipsYBanners() {
        assertEquals("Calculado" to TipoChip.OK, chipResultados(polinomial))
        assertEquals("No convergió" to TipoChip.ERROR, chipResultados(divergente))
        assertEquals("El método diverge o no convergió" to listOf(AccionError.CAMBIAR_DESPEJE, AccionError.PROBAR_NEWTON), bannerError(divergente))

        // Error de entrada en un método iterativo: no es "no convergió".
        val entradaMala = Catalogo.ejecutar(MetodoId.NEWTON_SISTEMAS, EntradaSistema(listOf("x^2+", "y"), "x, y", "1, 1"))
        assertEquals("Con errores" to TipoChip.ERROR, chipResultados(entradaMala))
        assertEquals("No se pudo calcular" to listOf(AccionError.EDITAR_DATOS), bannerError(entradaMala))
        val regMala = Catalogo.ejecutar(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1,2", "1,2", "5"))
        assertEquals("No se pudo calcular" to listOf(AccionError.EDITAR_DATOS), bannerError(regMala))

        val resolver = Catalogo.ejecutar(
            MetodoId.REGRESION_POLINOMIAL,
            EntradaRegresionPolinomial("176,168,202,138,213,159,193,122,185,153", "448,556,844,427,811,398,447,154,534,313", "2"),
        )
        assertEquals("Con avisos" to TipoChip.AVISO, chipResultados(resolver))
        val (titulo, resto) = bannerAvisos(resolver.advertencias)!!
        assertTrue(titulo.startsWith("El sistema está mal condicionado"))
        assertNull(resto)
        assertEquals("a" to "b\nc", bannerAvisos(listOf("a", "b", "c")))
        assertNull(bannerAvisos(emptyList()))
    }

    @Test
    fun filaConvergenciaYUltimaFila() {
        assertEquals("Divergente" to TipoChip.ERROR, chipConvergencia("No", divergente))
        val ok = Catalogo.ejecutar(MetodoId.NEWTON_SISTEMAS, Catalogo.ejemplo(MetodoId.NEWTON_SISTEMAS))
        assertEquals("Convergió" to TipoChip.OK, chipConvergencia("Sí", ok))
        val t = divergente.bloques.filterIsInstance<BloqueTabla>().single()
        assertTrue(marcarUltimaFila(t, divergente))
        assertFalse(marcarUltimaFila(ok.bloques.filterIsInstance<BloqueTabla>().first(), ok))
        assertTrue(esColumnaError("E") && esColumnaError("‖F(x)‖∞") && !esColumnaError("x"))
        // El aviso que repite el error del banner no se dibuja dos veces.
        assertTrue(divergente.bloques.filterIsInstance<BloqueTexto>().any { ocultarBloque(it, divergente) })
    }

    @Test
    fun formulasYConclusion() {
        assertTrue(esLineaSecundaria("Sistema normal: C·a = b,  Cᵢⱼ = Σ xₖ^(i+j)"))
        assertFalse(esLineaSecundaria("Pₘ(x) = a₀ + a₁x + … + aₘxᵐ"))
        assertEquals(
            listOf("El polinomio explica el " to false, "97.43%" to true, " de la variabilidad." to false),
            partesConPorcentajes("El polinomio explica el 97.43% de la variabilidad."),
        )
        assertEquals(listOf("sin porcentaje" to false), partesConPorcentajes("sin porcentaje"))
    }

    @Test
    fun matrizSueltaYGrupos() {
        val e = agruparBloques(listOf(BloqueTexto("t"), BloqueMatriz("J", "J", listOf(listOf(1.0))), BloqueTexto("u")))
        assertEquals(3, e.size)
        assertIs<ElementoVista.Matrices>(e[1])
    }
}
