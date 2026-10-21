package com.example.pentametod2.ui

import androidx.compose.ui.graphics.vector.addPathNodes
import com.example.pentametod2.ui.tema.EsquemaClaro
import com.example.pentametod2.ui.tema.EsquemaOscuro
import com.example.pentametod2.ui.tema.ExtendidosClaro
import com.example.pentametod2.ui.tema.ExtendidosOscuro
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.vector
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemaTest {

    @Test
    fun iconosSeConstruyen() {
        for (d in Iconos.todos) {
            for (p in d.paths) assertTrue(addPathNodes(p).isNotEmpty(), "${d.nombre}: path vacío «$p»")
            assertEquals(d.paths.size, d.vector().root.size, d.nombre)
        }
    }

    @Test
    fun tokensDelDiseno() {
        assertEquals(0xFF0E5E6F.toInt(), argb(EsquemaClaro.primary))
        assertEquals(0xFF84D2E3.toInt(), argb(EsquemaOscuro.primary))
        assertEquals(0xFFFFFFFF.toInt(), argb(EsquemaClaro.surfaceContainerLowest))
        assertEquals(0xFF222A2C.toInt(), argb(EsquemaOscuro.surfaceContainerHigh))
        assertEquals(0xFF9A3F0F.toInt(), argb(ExtendidosClaro.valorClave))
        assertEquals(0xFF0072B2.toInt(), argb(ExtendidosClaro.grafica.datos))
        assertEquals(argb(EsquemaOscuro.tertiary), argb(ExtendidosOscuro.grafica.prediccion))
    }

    /** Pares de texto/fondo derivados por nosotros (los no dados en el diseño): ≥ 4.5:1. */
    @Test
    fun contrasteDeColoresDerivados() {
        val e = ExtendidosOscuro
        val pares = listOf(
            e.onWarningContainer to e.warningContainer,
            e.warningBoton to e.warningContainer,
            EsquemaOscuro.error to e.filaUltimaError,
            EsquemaOscuro.onSurface to e.filaUltimaError,
            EsquemaOscuro.onTertiary to EsquemaOscuro.tertiary,
            EsquemaOscuro.inverseOnSurface to EsquemaOscuro.inverseSurface,
            EsquemaClaro.inverseOnSurface to EsquemaClaro.inverseSurface,
        )
        for ((t, f) in pares) assertTrue(contraste(argb(t), argb(f)) >= 4.5, "contraste ${contraste(argb(t), argb(f))}")
        // Borde (no texto): ≥ 3:1
        assertTrue(contraste(argb(e.warningBorde), argb(e.warningContainer)) >= 3.0)
    }

    private fun argb(c: androidx.compose.ui.graphics.Color): Int {
        fun ch(v: Float) = (v * 255 + 0.5f).toInt()
        return (ch(c.alpha) shl 24) or (ch(c.red) shl 16) or (ch(c.green) shl 8) or ch(c.blue)
    }

    private fun luminancia(argb: Int): Double {
        fun lin(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin((argb shr 16) and 0xFF) + 0.7152 * lin((argb shr 8) and 0xFF) + 0.0722 * lin(argb and 0xFF)
    }

    private fun contraste(a: Int, b: Int): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
