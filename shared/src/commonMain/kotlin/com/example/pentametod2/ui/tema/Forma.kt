package com.example.pentametod2.ui.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radios del diseño (dp). */
object Formas {
    /** Campos de texto. */
    val campo = RoundedCornerShape(4.dp)

    /** Chips, cajas de fórmula, tablas, cajas informativas. */
    val chip = RoundedCornerShape(8.dp)

    /** Ícono-tile del menú, fila destacada, caja del vector de coeficientes. */
    val tile = RoundedCornerShape(12.dp)

    /** Tarjetas y banners. */
    val tarjeta = RoundedCornerShape(16.dp)

    /** Botones y segmentados (alto 48 → píldora). */
    val boton = RoundedCornerShape(24.dp)
}

/** Shapes de Material 3 alineadas con [Formas]. */
internal val FormasMaterial = Shapes(
    extraSmall = Formas.campo,
    small = Formas.chip,
    medium = Formas.tile,
    large = Formas.tarjeta,
    extraLarge = Formas.boton,
)
