package com.example.pentametod2.ui.resultados

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.ResultadoPaso
import com.example.pentametod2.ui.componentes.EtiquetaSeccion
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.TextoFormula
import com.example.pentametod2.ui.tema.extendidos
import com.example.pentametod2.ui.tema.mono

private const val DURACION_PASO = 200

/**
 * Lista desplegable de pasos: borde 1dp outlineVariant, radio 16, filas separadas por una línea.
 * Varias filas pueden estar abiertas a la vez; cuáles lo están se guarda (rememberSaveable) por cálculo.
 * La animación de altura dura [DURACION_PASO] ms; con "quitar animaciones" del sistema, Compose la omite.
 */
@Composable
fun ListaPasos(pasos: List<BloquePaso>, claveCalculo: Int, modifier: Modifier = Modifier) {
    var abiertos by rememberSaveable(claveCalculo) { mutableStateOf(pasosIniciales(pasos)) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Formas.tarjeta,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Dimens.borde, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            pasos.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(thickness = Dimens.borde, color = MaterialTheme.colorScheme.outlineVariant)
                FilaPaso(p, p.numero in pasosAbiertos(abiertos)) { abiertos = alternarPaso(abiertos, p.numero) }
            }
        }
    }
}

@Composable
private fun FilaPaso(p: BloquePaso, abierto: Boolean, alAlternar: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    val giro by animateFloatAsState(if (abierto) 90f else 0f, tween(DURACION_PASO), label = "chevronPaso")
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.filaDestacada)
                .background(if (abierto) MaterialTheme.extendidos.filaAlterna else Color.Transparent)
                .clickable(role = Role.Button, onClickLabel = if (abierto) "Cerrar paso" else "Abrir paso", onClick = alAlternar)
                .semantics { stateDescription = if (abierto) "Expandido" else "Contraído" }
                .padding(horizontal = Dimens.paddingTarjeta, vertical = Dimens.e8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.e12),
        ) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(esquema.primaryContainer), contentAlignment = Alignment.Center) {
                Text("${p.numero}", style = MaterialTheme.mono.tile, color = esquema.onPrimaryContainer)
            }
            Text(
                p.titulo,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (abierto) FontWeight.SemiBold else FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                p.resumenCorto,
                style = MaterialTheme.mono.dato.copy(fontWeight = FontWeight.SemiBold),
                color = if (p.destacado) MaterialTheme.extendidos.valorClave else esquema.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp),
            )
            Icono(
                Iconos.chevronDerecha,
                if (abierto) "Cerrar ${p.titulo}" else "Abrir ${p.titulo}",
                Modifier.rotate(giro),
                tamano = Dimens.iconoBoton,
                color = esquema.onSurfaceVariant,
            )
        }
        AnimatedVisibility(
            visible = abierto,
            enter = expandVertically(tween(DURACION_PASO)) + fadeIn(tween(DURACION_PASO)),
            exit = shrinkVertically(tween(DURACION_PASO)) + fadeOut(tween(DURACION_PASO)),
        ) {
            ContenidoPaso(p)
        }
    }
}

/** Orden interno fijo: explicación → FÓRMULA → CON TUS DATOS → franja de resultado. */
@Composable
private fun ContenidoPaso(p: BloquePaso) {
    Column(
        Modifier.fillMaxWidth().padding(start = Dimens.paddingTarjeta, end = Dimens.paddingTarjeta, top = Dimens.e12, bottom = Dimens.paddingTarjeta),
        verticalArrangement = Arrangement.spacedBy(Dimens.e8),
    ) {
        Text(p.explicacion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        EtiquetaSeccion("Fórmula", Modifier.padding(top = Dimens.e4))
        CajaMono(p.formulaGeneral)
        if (p.conTusDatos.isNotEmpty() || p.matrices.isNotEmpty() || p.tablas.isNotEmpty()) {
            EtiquetaSeccion("Con tus datos", Modifier.padding(top = Dimens.e4))
            if (p.conTusDatos.isNotEmpty()) CajaMono(p.conTusDatos)
            if (p.matrices.isNotEmpty()) ContenidoMatrices(p.matrices)
            for (t in p.tablas) TablaView(t, ultimaConError = false, aviso = textoDesliza(t.columnas.size, conPasos = true))
        }
        FranjaResultado(p.resultados, p.destacado)
    }
}

/** Caja surfaceContainerHigh, radio 8, mono: las líneas largas pasan a la siguiente (nunca se cortan). */
@Composable
private fun CajaMono(lineas: List<String>) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = Formas.chip, modifier = Modifier.fillMaxWidth()) {
        SelectionContainer {
            Column(Modifier.padding(Dimens.e12), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (l in lineas) TextoFormula(l, ajustar = true)
            }
        }
    }
}

/** Franja de resultado: primaryContainer (tertiaryContainer y valorClave si el paso es destacado). */
@Composable
private fun FranjaResultado(resultados: List<ResultadoPaso>, destacado: Boolean) {
    if (resultados.isEmpty()) return
    val esquema = MaterialTheme.colorScheme
    val fondo = if (destacado) esquema.tertiaryContainer else esquema.primaryContainer
    val texto = if (destacado) MaterialTheme.extendidos.valorClave else esquema.onPrimaryContainer
    Surface(color = fondo, contentColor = texto, shape = Formas.chip, modifier = Modifier.fillMaxWidth().padding(top = Dimens.e4)) {
        SelectionContainer {
            Column(Modifier.padding(horizontal = Dimens.e12, vertical = Dimens.e8), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (r in resultados) {
                    Text(
                        "${r.etiqueta} = ${r.texto}",
                        style = MaterialTheme.mono.dato.copy(fontWeight = FontWeight.SemiBold),
                        color = texto,
                    )
                }
            }
        }
    }
}
