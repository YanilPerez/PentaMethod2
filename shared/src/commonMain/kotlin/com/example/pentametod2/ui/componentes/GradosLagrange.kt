package com.example.pentametod2.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.Iconos

/** Mismo diseño que el segmentado de regresión; varias filas cuando hay más de cuatro grados. */
@Composable
fun GradosLagrange(
    grados: List<Int>,
    seleccionados: List<Int>,
    alElegir: (Int) -> Unit,
    multiple: Boolean,
    disponibles: Set<Int> = grados.toSet(),
    descripcion: String = if (multiple) "Calcular grado" else "Procedimiento de grado",
) {
    val colores = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        inactiveContainerColor = Color.Transparent,
    )
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.e8)) {
        grados.chunked(4).forEach { fila ->
            if (multiple) {
                MultiChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    fila.forEachIndexed { i, g ->
                        SegmentedButton(
                            checked = g in seleccionados,
                            onCheckedChange = { alElegir(g) },
                            enabled = g in disponibles || g in seleccionados,
                            shape = SegmentedButtonDefaults.itemShape(i, fila.size, Formas.boton),
                            border = SegmentedButtonDefaults.borderStroke(MaterialTheme.colorScheme.outline, Dimens.borde),
                            colors = colores,
                            icon = { if (g in seleccionados) Icono(Iconos.check, null, tamano = Dimens.iconoInfo) },
                            modifier = Modifier.heightIn(min = Dimens.boton).semantics { contentDescription = "$descripcion $g" },
                        ) { Text(g.toString(), style = MaterialTheme.typography.labelLarge) }
                    }
                }
            } else {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    fila.forEachIndexed { i, g ->
                        SegmentedButton(
                            selected = g in seleccionados,
                            onClick = { alElegir(g) },
                            enabled = g in disponibles,
                            shape = SegmentedButtonDefaults.itemShape(i, fila.size, Formas.boton),
                            border = SegmentedButtonDefaults.borderStroke(MaterialTheme.colorScheme.outline, Dimens.borde),
                            colors = colores,
                            icon = { if (g in seleccionados) Icono(Iconos.check, null, tamano = Dimens.iconoInfo) },
                            modifier = Modifier.heightIn(min = Dimens.boton).semantics { contentDescription = "$descripcion $g" },
                        ) { Text(g.toString(), style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }
    }
}
