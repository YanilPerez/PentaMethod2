package com.example.pentametod2.ui

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.ui.navegacion.Pantalla
import com.example.pentametod2.ui.navegacion.aClave
import com.example.pentametod2.catalogo.Secciones
import com.example.pentametod2.ui.navegacion.agruparPorSeccion
import com.example.pentametod2.ui.navegacion.glifoMetodo
import com.example.pentametod2.ui.navegacion.anterior
import com.example.pentametod2.ui.navegacion.pantallaDesdeClave
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavegacionTest {

    @Test
    fun claveIdaYVuelta() {
        val todas = listOf(Pantalla.Menu) + MetodoId.entries.map { Pantalla.Metodo(it) }
        for (p in todas) assertEquals(p, pantallaDesdeClave(p.aClave()))
        assertEquals(Pantalla.Menu, pantallaDesdeClave("basura"))
    }

    @Test
    fun atras() {
        assertNull(Pantalla.Menu.anterior())
        assertEquals(Pantalla.Menu, Pantalla.Metodo(MetodoId.LAGRANGE).anterior())
    }

    // Fase 4B: el menú agrupa por sección (antes por semana, que ya no se muestra).
    @Test
    fun menuTresSeccionesDeDos() {
        val grupos = agruparPorSeccion(Catalogo.metodos())
        assertEquals(listOf("Sistemas no lineales", "Ajuste de curvas", "Interpolación"), grupos.map { it.first })
        assertEquals(Secciones.orden, grupos.map { it.first })
        assertEquals(listOf(2, 2, 2), grupos.map { it.second.size })
        assertEquals(listOf(MetodoId.PUNTO_FIJO, MetodoId.NEWTON_SISTEMAS), grupos[0].second.map { it.id })
        assertEquals(listOf(MetodoId.LAGRANGE, MetodoId.NEWTON_INTERPOLACION), grupos[2].second.map { it.id })
        for ((titulo, _) in grupos) kotlin.test.assertFalse(titulo.contains("Semana"))
    }

    @Test
    fun glifosDelMenu() {
        assertEquals(
            listOf("G(x)", "J⁻¹", "Pₘ", "βₖ", "Lₖ", "f[ ]"),
            listOf(
                MetodoId.PUNTO_FIJO, MetodoId.NEWTON_SISTEMAS, MetodoId.REGRESION_POLINOMIAL,
                MetodoId.REGRESION_MULTIPLE, MetodoId.LAGRANGE, MetodoId.NEWTON_INTERPOLACION,
            ).map { glifoMetodo(it) },
        )
    }
}
