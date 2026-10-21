package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.EntradaSistema
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.ui.grafica.RolSerie
import com.example.pentametod2.ui.grafica.etiquetaValor
import com.example.pentametod2.ui.grafica.rolesSeries
import com.example.pentametod2.ui.grafica.subindice
import com.example.pentametod2.ui.grafica.superindice
import kotlin.test.Test
import kotlin.test.assertEquals

class RolesGraficaTest {

    private fun roles(id: MetodoId) = rolesSeries(Catalogo.ejecutar(id, Catalogo.ejemplo(id)).grafica!!)

    @Test
    fun coloresPorTipoYOrden() {
        assertEquals(listOf(RolSerie.DATOS, RolSerie.CURVA, RolSerie.PREDICCION), roles(MetodoId.REGRESION_POLINOMIAL))
        assertEquals(listOf(RolSerie.DATOS, RolSerie.CURVA, RolSerie.PREDICCION), roles(MetodoId.LAGRANGE))
        assertEquals(listOf(RolSerie.DATOS, RolSerie.CURVA), roles(MetodoId.REGRESION_MULTIPLE))
        assertEquals(listOf(RolSerie.ERROR, RolSerie.TOLERANCIA), roles(MetodoId.NEWTON_SISTEMAS))
        val div = Catalogo.ejecutar(MetodoId.PUNTO_FIJO, EntradaSistema(listOf("2x+y", "2y+x"), "x, y", "1, 1"))
        assertEquals(RolSerie.ERROR, rolesSeries(div.grafica!!).first())
    }

    @Test
    fun etiquetas() {
        assertEquals("⁻²", superindice(-2))
        assertEquals("¹⁰", superindice(10))
        assertEquals("₇", subindice(7))
        assertEquals("1458", etiquetaValor(1458.0))
        assertEquals("0.4982", etiquetaValor(0.498212))
        assertEquals("5.06e-9", etiquetaValor(5.063373e-9))
        assertEquals("0", etiquetaValor(0.0))
    }
}
