package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.EntradaRegresionPolinomial
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.ui.formularios.Columna
import com.example.pentametod2.ui.formularios.EstadoRegresionPolinomial
import com.example.pentametod2.ui.formularios.Pildora
import com.example.pentametod2.ui.formularios.ResultadoPegado
import com.example.pentametod2.ui.formularios.TipoPildora
import com.example.pentametod2.ui.formularios.ValidacionPares
import com.example.pentametod2.ui.formularios.VistaDatos
import com.example.pentametod2.ui.formularios.aLista
import com.example.pentametod2.ui.formularios.ajustarGrado
import com.example.pentametod2.ui.formularios.avisoLongitudes
import com.example.pentametod2.ui.formularios.bloqueaCalculo
import com.example.pentametod2.ui.formularios.conVistaDe
import com.example.pentametod2.ui.formularios.conteoValores
import com.example.pentametod2.ui.formularios.estadoDesdeEntrada
import com.example.pentametod2.ui.formularios.estadoDesdeLista
import com.example.pentametod2.ui.formularios.formulaVistaPrevia
import com.example.pentametod2.ui.formularios.gradoMaximo
import com.example.pentametod2.ui.formularios.listaATabla
import com.example.pentametod2.ui.formularios.mensaje
import com.example.pentametod2.ui.formularios.objetivoFoco
import com.example.pentametod2.ui.formularios.parsearPegado
import com.example.pentametod2.ui.formularios.pildoraTabla
import com.example.pentametod2.ui.formularios.pildoraValores
import com.example.pentametod2.ui.formularios.primerTokenInvalido
import com.example.pentametod2.ui.formularios.tablaALista
import com.example.pentametod2.ui.formularios.textoGradoMaximo
import com.example.pentametod2.ui.formularios.validarParesXY
import com.example.pentametod2.ui.resultados.subtituloMetodo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidacionPuntosTest {

    @Test
    fun conteoConTodosLosSeparadores() {
        assertEquals(5, conteoValores("1, 2;3 4\n5"))
        assertEquals(3, conteoValores("1,2,3,"))      // vacío final ignorado
        assertEquals(3, conteoValores(" 1 ,, 2 ,3 , ")) // vacíos intermedios también
        assertEquals(0, conteoValores(""))
        assertEquals(0, conteoValores(" , ;\n"))
        assertEquals(2, conteoValores("1.5\t-2.25"))
    }

    @Test
    fun primerTokenInvalidoPorPosicion() {
        assertNull(primerTokenInvalido("1, 2.5, -3, 1e-3, .5"))
        assertEquals(3, primerTokenInvalido("1, 2, abc, 4"))
        assertEquals(3, primerTokenInvalido("1; 2,5x")) // "1", "2", "5x"
        assertNull(primerTokenInvalido("1, 2,5")) // con coma decimal "2,5" son dos valores: el conteo lo delata
        assertEquals(1, primerTokenInvalido("x"))
        assertNull(primerTokenInvalido(""))
        assertNull(primerTokenInvalido("1,2,"))
    }

    @Test
    fun validarParesTodosLosCasos() {
        assertEquals(ValidacionPares.Ok(5), validarParesXY(l("1,2,3,4,5"), l("9,7.5,4.2,3,2.1")))
        assertEquals(ValidacionPares.Vacio, validarParesXY(emptyList(), emptyList()))
        assertEquals(ValidacionPares.Vacio, validarParesXY(listOf("", ""), listOf("", "")))
        val distintas = validarParesXY(l("1,2,3,4,5"), l("1,2,3,4"))
        assertEquals(ValidacionPares.LongitudesDistintas(5, 4), distintas)
        assertEquals("x tiene 5 valores y y tiene 4. Deben ser iguales.", distintas.mensaje())
        assertEquals(ValidacionPares.ValorInvalido(Columna.Y, 2), validarParesXY(l("1,2,3"), l("1,a,3")))
        assertEquals(ValidacionPares.ValorInvalido(Columna.X, 3), validarParesXY(l("1,2,?"), l("1,a")))
        assertEquals(ValidacionPares.PocosPuntos(1), validarParesXY(l("1"), l("2")))
        // Tabla: las filas vacías del final no cuentan; una celda vacía intermedia es un error en su fila.
        assertEquals(ValidacionPares.Ok(2), validarParesXY(listOf("1", "2", ""), listOf("3", "4", "")))
        assertEquals(ValidacionPares.ValorInvalido(Columna.X, 2), validarParesXY(listOf("1", "", "3"), listOf("1", "2", "3")))
        assertEquals(ValidacionPares.LongitudesDistintas(3, 2), validarParesXY(listOf("1", "2", "3"), listOf("1", "2", "")))
        assertNull(ValidacionPares.Ok(3).mensaje())
    }

    @Test
    fun pildorasYAvisoDeLongitudes() {
        assertEquals(Pildora("5 valores", TipoPildora.OK), pildoraValores(l("1,2,3,4,5"), l("1,2,3,4,5")))
        assertEquals(Pildora("5 valores", TipoPildora.ERROR), pildoraValores(l("1,2,3,4,5"), l("1,2,3,4")))
        assertEquals(Pildora("4 valores", TipoPildora.ERROR), pildoraValores(l("1,2,3,4"), l("1,2,3,4,5")))
        assertEquals(Pildora("Revisa el valor 2", TipoPildora.ERROR), pildoraValores(l("1,x"), l("1,2")))
        assertEquals(Pildora("1 valor", TipoPildora.OK), pildoraValores(l("1"), emptyList())) // la otra aún vacía
        assertEquals(Pildora("0 valores", TipoPildora.NEUTRA), pildoraValores(emptyList(), l("1")))
        assertEquals("x tiene 5 valores y y tiene 4. Deben ser iguales.", avisoLongitudes(l("1,2,3,4,5"), l("1,2,3,4")))
        assertNull(avisoLongitudes(l("1,2,3"), emptyList()))
        assertEquals(Pildora("5 puntos", TipoPildora.OK), pildoraTabla(l("1,2,3,4,5"), l("1,2,3,4,5")))
        assertEquals(Pildora("Revisa la fila 2", TipoPildora.ERROR), pildoraTabla(listOf("1", "b"), listOf("1", "2")))
    }

    @Test
    fun gradoMaximoYAjuste() {
        assertEquals(0, gradoMaximo(0))
        assertEquals(0, gradoMaximo(1))
        assertEquals(1, gradoMaximo(2))
        assertEquals(2, gradoMaximo(3))
        assertEquals(5, gradoMaximo(6))
        assertEquals(5, gradoMaximo(40))
        assertEquals(2, ajustarGrado(null, 5))  // valor inicial
        assertEquals(1, ajustarGrado(null, 2))  // G = 1
        assertEquals(1, ajustarGrado(null, 0))  // solo "1" (deshabilitado)
        assertEquals(4, ajustarGrado(4, 10))
        assertEquals(2, ajustarGrado(4, 3))     // se quitaron puntos: baja a G
        assertEquals(1, ajustarGrado(3, 1))
        assertEquals(5, ajustarGrado(9, 20))    // tope de la pantalla
        assertEquals("Grado máximo permitido con 5 puntos: 4.", textoGradoMaximo(5))
        assertEquals("Grado máximo permitido con 9 puntos: 5.", textoGradoMaximo(9))
    }

    @Test
    fun estadoBajaElGradoAlQuitarPuntos() {
        var e = EstadoRegresionPolinomial("1,2,3,4,5", "1,4,9,16,25", "4", "")
        assertEquals(4, e.gradoEfectivo())
        e = e.conVista(VistaDatos.TABLA).quitarPunto(4).quitarPunto(3).conGradoAjustado()
        assertEquals("2", e.grado)
        assertEquals(3, e.puntos())
        // Sin elegir: 2 en cuanto hay 3 puntos.
        assertEquals(2, EstadoRegresionPolinomial("1,2,3", "1,2,3", "", "").gradoEfectivo())
    }

    @Test
    fun pegarDeExcel() {
        assertEquals(ResultadoPegado.Ok(listOf("1", "2", "3"), listOf("9", "7.5", "4.2")), parsearPegado("1\t9\n2\t7.5\r\n3\t4.2\n"))
        assertEquals(ResultadoPegado.Ok(listOf("1", "2"), listOf("9", "8")), parsearPegado("1 9\n\n  2   8  "))
        assertEquals(ResultadoPegado.Ok(listOf("1", "2"), listOf("9", "8")), parsearPegado("1;9\n2;8"))
        assertEquals(ResultadoPegado.Ok(listOf("1"), listOf("9")), parsearPegado("x\ty\n1\t9"))       // encabezado
        assertEquals(ResultadoPegado.Ok(listOf("1"), listOf("9")), parsearPegado("1\t9\tsobra"))     // 3.ª columna ignorada
        assertEquals(ResultadoPegado.Error("La fila 2 tiene una sola columna; se esperan x e y."), parsearPegado("1\t9\n2\n3\t4"))
        assertIs<ResultadoPegado.Error>(parsearPegado(""))
        assertIs<ResultadoPegado.Error>(parsearPegado("\n \n"))
        assertIs<ResultadoPegado.Error>(parsearPegado("x\ty"))
    }

    @Test
    fun listaYTablaConLongitudesDistintas() {
        assertEquals(listOf("1", "2", "3") to listOf("4", "5", ""), listaATabla(l("1,2,3"), l("4,5")))
        assertEquals(listOf("", "") to listOf("", ""), listaATabla(emptyList(), emptyList())) // mínimo 2 filas
        assertEquals("1, 2", tablaALista(listOf("1", "", " 2 ", "")))
        var e = EstadoRegresionPolinomial("1,2,3", "4,5", "", "")
        e = e.conVista(VistaDatos.TABLA)
        assertEquals(listOf("4", "5", ""), e.ys)
        e = e.conCelda(2, Columna.Y, "6").conCelda(3, Columna.X, "7")
        assertEquals(listOf("1", "2", "3", "7"), e.xs)
        assertEquals(listOf("4", "5", "6", ""), e.ys)
        e = e.conVista(VistaDatos.LISTA)
        assertEquals(listOf("4", "5", "6"), e.ys) // las vacías se descartan, nada más se pierde
        assertEquals(listOf("1", "2", "3", "7"), e.xs)
        assertEquals(e, e.conVista(VistaDatos.TABLA).conVista(VistaDatos.LISTA)) // ida y vuelta sin pérdidas
    }

    @Test
    fun formulasDeVistaPrevia() {
        assertEquals("y = a₀ + a₁x", formulaVistaPrevia(1))
        assertEquals("y = a₀ + a₁x + a₂x²", formulaVistaPrevia(2))
        assertEquals("y = a₀ + a₁x + a₂x² + a₃x³", formulaVistaPrevia(3))
        assertEquals("y = a₀ + a₁x + a₂x² + a₃x³ + a₄x⁴ + a₅x⁵", formulaVistaPrevia(5))
    }

    @Test
    fun focoAlPrimerError() {
        assertEquals(Columna.Y to 1, objetivoFoco(ValidacionPares.ValorInvalido(Columna.Y, 2)))
        assertEquals(Columna.Y to 4, objetivoFoco(ValidacionPares.LongitudesDistintas(5, 4)))
        assertEquals(Columna.X to 0, objetivoFoco(ValidacionPares.Vacio))
        assertNull(objetivoFoco(ValidacionPares.Ok(3)))
        assertTrue(bloqueaCalculo(EstadoRegresionPolinomial("1,2,3", "1,2", "", "")))
        assertFalse(bloqueaCalculo(estadoDesdeEntrada(Catalogo.ejemplo(MetodoId.REGRESION_POLINOMIAL))))
    }

    @Test
    fun ejemploDaLaMismaEntradaYElMismoResultado() {
        val ejemplo = Catalogo.ejemplo(MetodoId.REGRESION_POLINOMIAL)
        assertEquals(EntradaRegresionPolinomial("1,2,3,4,5", "9,7.5,4.2,3,2.1", "2", "2.5"), ejemplo)
        // Desde la Tabla y al volver a la Lista la entrada al motor es idéntica.
        val enTabla = (estadoDesdeEntrada(ejemplo).conVistaDe(EstadoRegresionPolinomial(vista = VistaDatos.TABLA)) as EstadoRegresionPolinomial)
        assertEquals(VistaDatos.TABLA, enTabla.vista)
        assertEquals(ejemplo, enTabla.aEntrada())
        val salida = Catalogo.ejecutar(MetodoId.REGRESION_POLINOMIAL, enTabla.aEntrada())
        assertEquals(Catalogo.ejecutar(MetodoId.REGRESION_POLINOMIAL, ejemplo), salida)
        val valor = { e: String -> salida.resumen.first { it.etiqueta == e }.valor }
        assertEquals("0.905143", valor("Q"))
        assertEquals("0.974265", valor("R²"))
        assertEquals("5.662500", valor("P(2.5)"))
        // El guardado (rotación) conserva vista, celdas vacías y longitudes distintas.
        val raro = EstadoRegresionPolinomial(listOf("1", "", "3"), listOf("4"), "3", "2", VistaDatos.TABLA)
        assertEquals(raro, estadoDesdeLista(raro.aLista()))
    }

    @Test
    fun subtitulosDeLaBarra() {
        assertEquals("Ajuste de curvas · mínimos cuadrados", subtituloMetodo(Catalogo.info(MetodoId.REGRESION_POLINOMIAL)))
        for (m in Catalogo.metodos()) assertTrue(subtituloMetodo(m).startsWith(m.seccion + " · "), m.nombre)
    }

    private fun l(texto: String) = texto.split(",").filter { it.isNotEmpty() }
}
