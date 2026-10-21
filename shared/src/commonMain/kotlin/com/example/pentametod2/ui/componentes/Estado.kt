package com.example.pentametod2.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pentametod2.model.Estado
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.extendidos

enum class TipoChip { OK, AVISO, ERROR, INFO }

/** Texto y tipo del chip del separador "RESULTADOS". */
fun chipParaEstado(estado: Estado, iterativo: Boolean): Pair<String, TipoChip> = when (estado) {
    Estado.OK -> "Calculado" to TipoChip.OK
    Estado.ADVERTENCIA -> "Con avisos" to TipoChip.AVISO
    Estado.ERROR -> (if (iterativo) "No convergió" else "Con errores") to TipoChip.ERROR
}

/** Chip de estado: alto 28, radio 8, 12/600; OK con check de 14dp. */
@Composable
fun ChipEstado(texto: String, tipo: TipoChip, modifier: Modifier = Modifier) {
    val e = MaterialTheme.extendidos
    val esquema = MaterialTheme.colorScheme
    val (fondo, frente) = when (tipo) {
        TipoChip.OK -> e.successContainer to e.success
        TipoChip.AVISO -> e.warningContainer to e.warning
        TipoChip.ERROR -> esquema.errorContainer to esquema.onErrorContainer
        TipoChip.INFO -> esquema.surfaceContainerHigh to esquema.onSurfaceVariant
    }
    Surface(color = fondo, contentColor = frente, shape = Formas.chip, modifier = modifier.heightIn(min = Dimens.chip)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.e4),
        ) {
            if (tipo == TipoChip.OK) Icono(Iconos.check, null, tamano = Dimens.iconoChip)
            Text(texto, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

enum class TipoBanner { ERROR, AVISO }

/** Acción de un banner (TextButton alineado a la derecha). */
data class AccionBanner(val texto: String, val alPulsar: () -> Unit)

/**
 * Banner del diseño, radio 16. ERROR: errorContainer sin borde, ícono octógono en color error.
 * AVISO: warningContainer con borde 1dp, ícono triángulo en color warning.
 */
@Composable
fun BannerEstado(
    tipo: TipoBanner,
    titulo: String,
    mensaje: String?,
    modifier: Modifier = Modifier,
    acciones: List<AccionBanner> = emptyList(),
) {
    val e = MaterialTheme.extendidos
    val esquema = MaterialTheme.colorScheme
    val esError = tipo == TipoBanner.ERROR
    val fondo = if (esError) esquema.errorContainer else e.warningContainer
    val texto = if (esError) esquema.onErrorContainer else e.onWarningContainer
    val colorIcono = if (esError) esquema.error else e.warning
    val colorBoton = if (esError) esquema.onErrorContainer else e.warningBoton
    Surface(
        color = fondo,
        contentColor = texto,
        shape = Formas.tarjeta,
        border = if (esError) null else BorderStroke(Dimens.borde, e.warningBorde),
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(start = Dimens.e16, end = Dimens.e8, top = Dimens.e16, bottom = if (acciones.isEmpty()) Dimens.e16 else Dimens.e4)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.e12), modifier = Modifier.padding(end = Dimens.e8)) {
                Icono(if (esError) Iconos.errorOctogono else Iconos.avisoTriangulo, null, color = colorIcono)
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.e4)) {
                    Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp))
                    if (!mensaje.isNullOrBlank()) Text(mensaje, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (acciones.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    for (a in acciones) BotonTexto(a.texto, a.alPulsar, color = colorBoton)
                }
            }
        }
    }
}

/** Color de texto de error para valores (p. ej. el error final de un método que no convergió). */
@Composable
fun colorError(): Color = MaterialTheme.colorScheme.error
