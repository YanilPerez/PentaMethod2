package com.example.pentametod2.ui.tema

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Tokens del diseño "cuaderno de cálculo limpio". Ver docs/DISENO.md.
// Los valores marcados "derivado" no venían en el diseño (ver el reporte de la Fase 4B).

internal val EsquemaClaro: ColorScheme = lightColorScheme(
    primary = Color(0xFF0E5E6F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE7EC),
    onPrimaryContainer = Color(0xFF04313A),
    secondary = Color(0xFF4A6267), // derivado
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE7EC), // derivado
    onSecondaryContainer = Color(0xFF04313A),
    tertiary = Color(0xFFB54E17),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCE3D4),
    onTertiaryContainer = Color(0xFF4A1C04),
    background = Color(0xFFF8FAFA),
    onBackground = Color(0xFF172024),
    surface = Color(0xFFF8FAFA),
    onSurface = Color(0xFF172024),
    surfaceVariant = Color(0xFFEEF2F2),
    onSurfaceVariant = Color(0xFF3F4A4E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F7F7), // derivado
    surfaceContainer = Color(0xFFF1F5F5), // derivado
    surfaceContainerHigh = Color(0xFFEEF2F2),
    surfaceContainerHighest = Color(0xFFE6ECEC), // derivado
    inverseSurface = Color(0xFF2B3235), // derivado
    inverseOnSurface = Color(0xFFEEF2F2), // derivado
    outline = Color(0xFF5F6B6F),
    outlineVariant = Color(0xFFC3CBCD),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

internal val EsquemaOscuro: ColorScheme = darkColorScheme(
    primary = Color(0xFF84D2E3),
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004E5C),
    onPrimaryContainer = Color(0xFFA9EDFF),
    secondary = Color(0xFFB1CBD0), // derivado
    onSecondary = Color(0xFF1C3438),
    secondaryContainer = Color(0xFF004E5C), // derivado
    onSecondaryContainer = Color(0xFFA9EDFF),
    tertiary = Color(0xFFFFB68F),
    onTertiary = Color(0xFF552100), // derivado
    tertiaryContainer = Color(0xFF5C2604),
    onTertiaryContainer = Color(0xFFFFDBCB),
    background = Color(0xFF0F1416),
    onBackground = Color(0xFFDEE3E5),
    surface = Color(0xFF0F1416),
    onSurface = Color(0xFFDEE3E5),
    surfaceVariant = Color(0xFF222A2C),
    onSurfaceVariant = Color(0xFFBFC8CB),
    surfaceContainerLowest = Color(0xFF171D1F),
    surfaceContainerLow = Color(0xFF1A2123), // derivado
    surfaceContainer = Color(0xFF1D2527), // derivado
    surfaceContainerHigh = Color(0xFF222A2C),
    surfaceContainerHighest = Color(0xFF2A3335), // derivado
    inverseSurface = Color(0xFFDEE3E5), // derivado
    inverseOnSurface = Color(0xFF2B3235), // derivado
    outline = Color(0xFF899295),
    outlineVariant = Color(0xFF3F484B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410), // derivado
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/** Colores que Material 3 no trae. */
@Immutable
data class ColoresExtendidos(
    val warning: Color,
    val warningContainer: Color,
    /** Borde del banner de aviso. */
    val warningBorde: Color,
    /** Texto del banner de aviso. */
    val onWarningContainer: Color,
    /** Botones de texto dentro del banner de aviso. */
    val warningBoton: Color,
    val success: Color,
    val successContainer: Color,
    /** Texto de predicción / valor clave. */
    val valorClave: Color,
    /** Zebra de tablas. */
    val filaAlterna: Color,
    /** Pista de la barra de R². */
    val pistaR2: Color,
    /** Última fila de una tabla de iteraciones que no convergió. */
    val filaUltimaError: Color,
    /** Chevron de las tarjetas del menú. */
    val chevronMenu: Color,
    val grafica: ColoresGrafica,
)

@Immutable
data class ColoresGrafica(
    val cuadricula: Color,
    val ejes: Color,
    val datos: Color,
    val curva: Color,
    val prediccion: Color,
    val serie4: Color,
    val serie5: Color,
    val errorSerie: Color,
    val tolerancia: Color,
)

internal val ExtendidosClaro = ColoresExtendidos(
    warning = Color(0xFF8A5A00),
    warningContainer = Color(0xFFFFEFC7),
    warningBorde = Color(0xFFE5C26B),
    onWarningContainer = Color(0xFF3D2A00),
    warningBoton = Color(0xFF6B4600),
    success = Color(0xFF2E6B3F),
    successContainer = Color(0xFFDDEFE0),
    valorClave = Color(0xFF9A3F0F),
    filaAlterna = Color(0xFFF5F8F8),
    pistaR2 = Color(0xFFA6CDD5),
    filaUltimaError = Color(0xFFFCEEEE),
    chevronMenu = Color(0xFF6F7B7F),
    grafica = ColoresGrafica(
        cuadricula = Color(0xFFE1E7E8),
        ejes = Color(0xFF5F6B6F),
        datos = Color(0xFF0072B2),
        curva = Color(0xFF009E73),
        prediccion = EsquemaClaro.tertiary,
        serie4 = Color(0xFFE69F00),
        serie5 = Color(0xFFCC79A7),
        errorSerie = EsquemaClaro.error,
        tolerancia = Color(0xFF2E6B3F),
    ),
)

internal val ExtendidosOscuro = ColoresExtendidos(
    warning = Color(0xFFF5C35B),
    warningContainer = Color(0xFF4A3500),
    warningBorde = Color(0xFFB08A2E), // derivado (3.6:1 sobre warningContainer)
    onWarningContainer = Color(0xFFFFE3A3), // derivado (9.3:1)
    warningBoton = Color(0xFFF5C35B), // derivado (7.1:1)
    success = Color(0xFF9BD3A6),
    successContainer = Color(0xFF1E3A25),
    valorClave = Color(0xFFFFB68F),
    filaAlterna = Color(0xFF1B2224),
    pistaR2 = Color(0xFF1E6B7A),
    filaUltimaError = Color(0xFF3B2224), // derivado (error 8.5:1, onSurface 11.3:1)
    chevronMenu = Color(0xFF9AA5A9), // derivado (6.8:1)
    grafica = ColoresGrafica(
        cuadricula = Color(0xFF2A3235),
        ejes = Color(0xFF899295),
        datos = Color(0xFF56B4E9),
        curva = Color(0xFF3DCB9E),
        prediccion = EsquemaOscuro.tertiary,
        serie4 = Color(0xFFF0B840),
        serie5 = Color(0xFFE0A3C6),
        errorSerie = EsquemaOscuro.error,
        tolerancia = Color(0xFF9BD3A6),
    ),
)

val LocalColoresExtendidos = staticCompositionLocalOf { ExtendidosClaro }

/** `MaterialTheme.extendidos.warning`, `MaterialTheme.extendidos.grafica.datos`, … */
val MaterialTheme.extendidos: ColoresExtendidos
    @Composable @ReadOnlyComposable get() = LocalColoresExtendidos.current
