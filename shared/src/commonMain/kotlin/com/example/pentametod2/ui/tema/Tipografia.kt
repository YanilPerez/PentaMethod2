package com.example.pentametod2.ui.tema

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import pentametod2.shared.generated.resources.Res
import pentametod2.shared.generated.resources.ibm_plex_sans_medium
import pentametod2.shared.generated.resources.ibm_plex_sans_regular
import pentametod2.shared.generated.resources.ibm_plex_sans_semibold
import pentametod2.shared.generated.resources.jetbrains_mono_medium
import pentametod2.shared.generated.resources.jetbrains_mono_regular
import pentametod2.shared.generated.resources.jetbrains_mono_semibold

// Fuentes: IBM Plex Sans (interfaz) y JetBrains Mono (fórmulas y números), OFL 1.1 — ver docs/FUENTES.md.
// Glifos que estas fuentes no tienen (p. ej. ᵢ ₖ ⁽ ⁾) los dibuja la fuente del sistema.

/** IBM Plex Sans 400/500/600. */
@Composable
fun fuenteInterfaz(): FontFamily = FontFamily(
    Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
)

/** JetBrains Mono 400/500/600. ÚNICO lugar donde se elige la fuente de fórmulas y números. */
@Composable
fun fuenteMono(): FontFamily = FontFamily(
    Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(Res.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

/** Escala tipográfica del diseño (tamaño / alto de línea en sp). Todo en sp: respeta el tamaño de fuente del sistema. */
internal fun tipografia(sans: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.con(tam: Int, alto: Int, peso: FontWeight) =
        copy(fontFamily = sans, fontSize = tam.sp, lineHeight = alto.sp, fontWeight = peso, letterSpacing = 0.sp)
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = sans),
        displayMedium = base.displayMedium.copy(fontFamily = sans),
        displaySmall = base.displaySmall.copy(fontFamily = sans),
        headlineLarge = base.headlineLarge.copy(fontFamily = sans),
        headlineMedium = base.headlineMedium.con(28, 36, FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = sans),
        titleLarge = base.titleLarge.con(22, 28, FontWeight.SemiBold),
        titleMedium = base.titleMedium.con(18, 24, FontWeight.SemiBold),
        titleSmall = base.titleSmall.con(16, 22, FontWeight.Medium),
        bodyLarge = base.bodyLarge.con(16, 24, FontWeight.Normal),
        bodyMedium = base.bodyMedium.con(14, 20, FontWeight.Normal),
        bodySmall = base.bodySmall.con(12, 16, FontWeight.Normal),
        labelLarge = base.labelLarge.con(14, 20, FontWeight.SemiBold),
        labelMedium = base.labelMedium.con(12, 16, FontWeight.SemiBold),
        labelSmall = base.labelSmall.con(11, 16, FontWeight.Medium),
    )
}

/** Estilos monoespaciados del diseño (sin color: el color lo pone cada componente). */
@Immutable
data class EstilosMono(
    /** "ENTRADA", "RESULTADOS": 12/16·600, 0.06em, MAYÚSCULAS, color primary. */
    val etiquetaSeccion: TextStyle,
    /** "PENTAMETOD2" del menú: igual pero con 0.08em. */
    val marca: TextStyle,
    /** Fórmulas: 15/24·500. */
    val formula: TextStyle,
    /** Celdas de tabla: 13/18·400 con cifras tabulares. */
    val tabla: TextStyle,
    /** Encabezados y fila Σ: 13/18·600 tabulares. */
    val tablaFuerte: TextStyle,
    /** Valor clave (predicción): 20/28·600. */
    val valorClave: TextStyle,
    /** Valores del resumen: 15/22. */
    val valor: TextStyle,
    /** Valores de la entrada comprimida y matrices: 14/20. */
    val dato: TextStyle,
    /** Texto de campos de entrada: 16/24. */
    val campo: TextStyle,
    /** Glifo del ícono-tile del menú: 13/600. */
    val tile: TextStyle,
    /** Contadores y rótulos pequeños: 12/16. */
    val pequeno: TextStyle,
    /** Números de la gráfica: 11sp. */
    val grafica: TextStyle,
    /** Etiquetas de valor en la gráfica: 11/600. */
    val graficaFuerte: TextStyle,
)

internal fun estilosMono(mono: FontFamily): EstilosMono {
    fun estilo(tam: Int, alto: Int, peso: FontWeight, tnum: Boolean = false) = TextStyle(
        fontFamily = mono,
        fontSize = tam.sp,
        lineHeight = alto.sp,
        fontWeight = peso,
        fontFeatureSettings = if (tnum) "tnum" else null,
    )
    return EstilosMono(
        etiquetaSeccion = estilo(12, 16, FontWeight.SemiBold).copy(letterSpacing = 0.06.em),
        marca = estilo(12, 16, FontWeight.SemiBold).copy(letterSpacing = 0.08.em),
        formula = estilo(15, 24, FontWeight.Medium),
        tabla = estilo(13, 18, FontWeight.Normal, tnum = true),
        tablaFuerte = estilo(13, 18, FontWeight.SemiBold, tnum = true),
        valorClave = estilo(20, 28, FontWeight.SemiBold, tnum = true),
        valor = estilo(15, 22, FontWeight.Normal, tnum = true),
        dato = estilo(14, 20, FontWeight.Normal, tnum = true),
        campo = estilo(16, 24, FontWeight.Normal),
        tile = estilo(13, 18, FontWeight.SemiBold),
        pequeno = estilo(12, 16, FontWeight.Normal, tnum = true),
        grafica = estilo(11, 14, FontWeight.Normal, tnum = true),
        graficaFuerte = estilo(11, 14, FontWeight.SemiBold, tnum = true),
    )
}

val LocalEstilosMono = staticCompositionLocalOf { estilosMono(FontFamily.Monospace) }

/** `MaterialTheme.mono.formula`, `MaterialTheme.mono.tabla`, … */
val MaterialTheme.mono: EstilosMono
    @Composable @ReadOnlyComposable get() = LocalEstilosMono.current

/**
 * ÚNICO punto de texto de fórmulas. Nunca se corta con "…": si la línea es larga se desplaza en horizontal,
 * o, con [ajustar], pasa a la línea siguiente (como `white-space: pre-wrap`; lo usan los pasos).
 * `secundaria` (p. ej. "Sistema normal: C·a = b") va en onSurfaceVariant.
 */
@Composable
fun TextoFormula(
    texto: String,
    modifier: Modifier = Modifier,
    secundaria: Boolean = false,
    color: Color = if (secundaria) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current,
    ajustar: Boolean = false,
) {
    Text(
        text = texto,
        style = MaterialTheme.mono.formula,
        color = color,
        softWrap = ajustar,
        overflow = TextOverflow.Visible,
        modifier = if (ajustar) modifier else modifier.horizontalScroll(rememberScrollState()),
    )
}
