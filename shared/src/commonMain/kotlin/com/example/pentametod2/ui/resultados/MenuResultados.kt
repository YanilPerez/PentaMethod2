package com.example.pentametod2.ui.resultados

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pentametod2.exportar.ExportadorArchivos
import com.example.pentametod2.exportar.ExportadorNoDisponible
import com.example.pentametod2.exportar.Exportaciones
import com.example.pentametod2.exportar.TipoExportacion
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.ui.tema.DefIcono
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono

/** Selector de archivos de la plataforma; AppMetodos lo provee (por defecto, no disponible). */
val LocalExportador = compositionLocalOf<ExportadorArchivos> { ExportadorNoDisponible }

const val NO_DISPONIBLE = "No disponible para este método todavía"
const val TEXTO_COPIADO = "Copiado: resumen y tablas, separados por tabulaciones."

enum class AccionMenu { EXCEL, OCTAVE, COPIAR, AYUDA }

/** Ítem del menú: título, subtítulo y si está habilitado (puro, para testear). */
data class ItemMenu(val accion: AccionMenu, val titulo: String, val sub: String, val habilitado: Boolean)

fun itemsMenu(s: SalidaMetodo): List<ItemMenu> {
    val excel = Exportaciones.disponible(s, TipoExportacion.EXCEL)
    val octave = Exportaciones.disponible(s, TipoExportacion.OCTAVE)
    return listOf(
        ItemMenu(AccionMenu.EXCEL, "Exportar a Excel", if (excel) ".xlsx con datos, sumatorias y resultados" else NO_DISPONIBLE, excel),
        ItemMenu(AccionMenu.OCTAVE, "Exportar código Octave", if (octave) ".txt listo para pegar en Octave" else NO_DISPONIBLE, octave),
        ItemMenu(AccionMenu.COPIAR, "Copiar resultados", "Resumen y tablas, separados por tabulaciones", true),
        ItemMenu(AccionMenu.AYUDA, "Ayuda del método", "", true),
    )
}

private fun icono(a: AccionMenu): DefIcono = when (a) {
    AccionMenu.EXCEL -> Iconos.hoja
    AccionMenu.OCTAVE -> Iconos.codigo
    AccionMenu.COPIAR -> Iconos.copiar
    AccionMenu.AYUDA -> Iconos.ayuda
}

/**
 * Botón "guardar" (flecha a la bandeja) de la barra superior y su menú: ancho 312, radio 12, borde 1dp,
 * elevación baja; ítems de ≥ 56 con ícono. "Ayuda del método" va tras un separador.
 */
@Composable
fun MenuResultados(salida: SalidaMetodo, alElegir: (AccionMenu) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val esquema = MaterialTheme.colorScheme
    Box {
        IconButton(onClick = { abierto = true }) { Icono(Iconos.guardar, "Guardar o exportar") }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            modifier = Modifier.width(312.dp),
            shape = Formas.tile,
            containerColor = esquema.surfaceContainerLowest,
            tonalElevation = 0.dp,
            shadowElevation = 3.dp,
            border = BorderStroke(Dimens.borde, esquema.outlineVariant),
        ) {
            for (item in itemsMenu(salida)) {
                if (item.accion == AccionMenu.AYUDA) HorizontalDivider(thickness = Dimens.borde, color = esquema.outlineVariant)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.titulo, style = MaterialTheme.typography.bodyLarge)
                            if (item.sub.isNotEmpty()) {
                                Text(item.sub, style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
                            }
                        }
                    },
                    leadingIcon = { Icono(icono(item.accion), null, color = if (item.habilitado) esquema.onSurfaceVariant else esquema.outlineVariant) },
                    onClick = {
                        abierto = false
                        alElegir(item.accion)
                    },
                    enabled = item.habilitado,
                    modifier = Modifier.heightIn(min = 56.dp),
                )
            }
        }
    }
}
