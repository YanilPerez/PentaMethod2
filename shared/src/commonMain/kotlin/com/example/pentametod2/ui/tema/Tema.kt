package com.example.pentametod2.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * Tema "cuaderno de cálculo limpio": Material 3, claro u oscuro según el sistema, sin elevación.
 * Tokens: [EsquemaClaro]/[EsquemaOscuro] y [ColoresExtendidos] (Color.kt), tipografía (Tipografia.kt),
 * [Formas] (Forma.kt), [Dimens] (Espaciado.kt), [Iconos] (Iconos.kt). Mapa completo en docs/DISENO.md.
 */
@Composable
fun TemaMetodos(oscuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val sans = fuenteInterfaz()
    val mono = fuenteMono()
    val tipografia = remember(sans) { tipografia(sans) }
    val estilos = remember(mono) { estilosMono(mono) }
    CompositionLocalProvider(
        LocalColoresExtendidos provides if (oscuro) ExtendidosOscuro else ExtendidosClaro,
        LocalEstilosMono provides estilos,
    ) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = tipografia,
            shapes = FormasMaterial,
            content = content,
        )
    }
}
