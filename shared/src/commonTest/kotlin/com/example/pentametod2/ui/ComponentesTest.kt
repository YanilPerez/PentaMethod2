package com.example.pentametod2.ui

import com.example.pentametod2.model.Estado
import com.example.pentametod2.ui.componentes.TipoChip
import com.example.pentametod2.ui.componentes.chipParaEstado
import com.example.pentametod2.ui.componentes.debeApilar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComponentesTest {

    @Test
    fun apilarValoresLargos() {
        assertFalse(debeApilar("0.974265"))
        assertFalse(debeApilar("x = 1.414214, y = 1.4142")) // 24 caracteres: en línea
        assertTrue(debeApilar("x = 1.414214, y = 1.414214"))
        assertTrue(debeApilar("P₂(x) = 12.300000 − 3.244286x + 0.235714x²"))
    }

    @Test
    fun chipDelSeparadorResultados() {
        assertEquals("Calculado" to TipoChip.OK, chipParaEstado(Estado.OK, iterativo = true))
        assertEquals("Con avisos" to TipoChip.AVISO, chipParaEstado(Estado.ADVERTENCIA, iterativo = false))
        assertEquals("No convergió" to TipoChip.ERROR, chipParaEstado(Estado.ERROR, iterativo = true))
        assertEquals("Con errores" to TipoChip.ERROR, chipParaEstado(Estado.ERROR, iterativo = false))
    }
}
