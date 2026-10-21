package com.example.pentametod2.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pentametod2.model.ItemResumen
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.extendidos
import com.example.pentametod2.ui.tema.mono

/** Valores más largos que esto se apilan (etiqueta arriba, valor abajo). */
const val LIMITE_EN_LINEA = 24

/** Regla de FilaResumen: ¿apilar etiqueta y valor? */
fun debeApilar(valor: String, limite: Int = LIMITE_EN_LINEA): Boolean = valor.length > limite

/** Fila etiqueta/valor del resumen (alto mínimo 40). Las filas destacadas van con [FilaDestacada]. */
@Composable
fun FilaResumen(
    item: ItemResumen,
    modifier: Modifier = Modifier,
    colorValor: Color = MaterialTheme.colorScheme.onSurface,
    valorPersonalizado: (@Composable () -> Unit)? = null,
) {
    if (item.destacado) {
        FilaDestacada(item, modifier)
        return
    }
    val etiqueta = @Composable {
        Text(item.etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val valor = valorPersonalizado ?: { Text(item.valor, style = MaterialTheme.mono.valor, color = colorValor, textAlign = TextAlign.End) }
    if (debeApilar(item.valor) && valorPersonalizado == null) {
        Column(modifier.fillMaxWidth().heightIn(min = Dimens.filaResumen).padding(vertical = Dimens.e8), verticalArrangement = Arrangement.spacedBy(Dimens.e4)) {
            etiqueta()
            Text(item.valor, style = MaterialTheme.mono.valor, color = colorValor)
        }
    } else {
        Row(
            modifier.fillMaxWidth().heightIn(min = Dimens.filaResumen).padding(vertical = Dimens.e8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.e12),
        ) {
            Column(Modifier.weight(1f)) { etiqueta() }
            valor()
        }
    }
}

/** Fila destacada: tertiaryContainer radio 12, alto ≥ 52, marcador de predicción, valor 20/28·600 en valorClave. */
@Composable
fun FilaDestacada(item: ItemResumen, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = Formas.tile, modifier = modifier.fillMaxWidth().padding(vertical = Dimens.e4)) {
        Row(
            Modifier.heightIn(min = Dimens.filaDestacada).padding(horizontal = Dimens.e12, vertical = Dimens.e8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.e12),
        ) {
            MarcadorPrediccion()
            Text(
                item.etiqueta,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            Text(item.valor, style = MaterialTheme.mono.valorClave, color = MaterialTheme.extendidos.valorClave)
        }
    }
}

/** Marcador de predicción: anillo de radio 8 (grosor 1.3) + punto de radio 4, color tertiary. */
@Composable
fun MarcadorPrediccion(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.tertiary) {
    Canvas(modifier.size(18.dp)) { marcadorPrediccion(color, Offset(size.width / 2, size.height / 2), 8.dp.toPx(), 4.dp.toPx(), 1.3.dp.toPx()) }
}

fun DrawScope.marcadorPrediccion(color: Color, centro: Offset, radioAnillo: Float, radioPunto: Float, grosor: Float) {
    drawCircle(color, radioAnillo, centro, style = Stroke(grosor))
    drawCircle(color, radioPunto, centro)
}
