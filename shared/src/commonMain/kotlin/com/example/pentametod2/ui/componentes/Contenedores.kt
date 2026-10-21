package com.example.pentametod2.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.mono

/** Centra el contenido con el ancho máximo del diseño (720dp). */
@Composable
fun ContenidoCentrado(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = Dimens.anchoMaximo).fillMaxWidth(), content = content)
    }
}

/** Tarjeta del diseño: borde 1dp outlineVariant, radio 16, fondo surfaceContainerLowest, padding 16, sin elevación. */
@Composable
fun TarjetaContorno(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Dimens.paddingTarjeta),
    espacio: androidx.compose.ui.unit.Dp = Dimens.e12,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Formas.tarjeta,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(Dimens.borde, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(espacio), content = content)
    }
}

/** "ENTRADA", "EXPLICACIÓN", "RESULTADOS": mono 12/16·600, 0.06em, MAYÚSCULAS, primary. */
@Composable
fun EtiquetaSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.mono.etiquetaSeccion,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/** Título de tarjeta (titleMedium) marcado como encabezado para TalkBack. */
@Composable
fun TituloTarjeta(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = modifier.semantics { heading() })
}

/** Caja informativa: fondo surfaceContainerHigh, radio 8, padding 12, ícono info 16dp, texto 12/18. */
@Composable
fun CajaInfo(texto: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = Formas.chip, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(Dimens.e12), horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
            Icono(Iconos.info, null, tamano = Dimens.iconoInfo, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
            Text(
                texto,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
