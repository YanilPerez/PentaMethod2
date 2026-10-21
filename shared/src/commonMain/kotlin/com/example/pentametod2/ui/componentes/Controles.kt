package com.example.pentametod2.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pentametod2.ui.tema.DefIcono
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.extendidos
import com.example.pentametod2.ui.tema.mono

// ── Campos ──

/** Teclado de texto (las listas necesitan coma, punto, espacio y signo menos), sin autocorrección. */
val TecladoDatos = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrectEnabled = false, imeAction = ImeAction.Next)

/**
 * Campo del diseño: OutlinedTextField con label flotante, texto mono 16, borde 1dp outline (2dp primary con foco),
 * radio 4, alto 56. `prefijo` (p. ej. "=") en mono para ecuaciones.
 */
@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    prefijo: String? = null,
    multilinea: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingText = textoAyuda(ayuda),
        prefix = textoPrefijo(prefijo),
        singleLine = !multilinea,
        maxLines = if (multilinea) 4 else 1,
        keyboardOptions = TecladoDatos,
        textStyle = MaterialTheme.mono.campo,
        shape = Formas.campo,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = Dimens.campo),
    )
}

private fun textoAyuda(ayuda: String?): (@Composable () -> Unit)? {
    if (ayuda == null) return null
    return { Text(ayuda, style = MaterialTheme.typography.bodySmall) }
}

private fun textoPrefijo(prefijo: String?): (@Composable () -> Unit)? {
    if (prefijo == null) return null
    return { Text("$prefijo ", style = MaterialTheme.mono.campo, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

/**
 * Campo compacto: etiqueta de 12sp ARRIBA (con un [accesorio] opcional a la derecha, p. ej. una píldora),
 * caja de 44 (o [alto]) con borde 1dp outline (2dp primary con foco, 2dp error si [error]), radio 8 y valor en mono 14.
 * Con [seleccionarTodoAlEnfocar] el texto queda seleccionado al entrar, para reescribirlo de una vez.
 * Sin [etiqueta] sirve de celda de tabla ([descripcion] la nombra para TalkBack).
 */
@Composable
fun CampoCompacto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String?,
    modifier: Modifier = Modifier,
    accesorio: (@Composable () -> Unit)? = null,
    ayuda: String? = null,
    error: Boolean = false,
    alto: Dp = Dimens.campoCompacto,
    focusRequester: FocusRequester? = null,
    seleccionarTodoAlEnfocar: Boolean = false,
    descripcion: String? = null,
) {
    val esquema = MaterialTheme.colorScheme
    var interno by remember { mutableStateOf(TextFieldValue(valor, TextRange(valor.length))) }
    val actual = if (interno.text == valor) interno else TextFieldValue(valor, TextRange(valor.length))
    var enfocado by remember { mutableStateOf(false) }
    if (seleccionarTodoAlEnfocar) {
        LaunchedEffect(enfocado) {
            if (enfocado) {
                withFrameNanos { } // después del toque que coloca el cursor
                interno = TextFieldValue(valor, TextRange(0, valor.length))
            }
        }
    }
    val colorBorde = when {
        error -> esquema.error
        enfocado -> esquema.primary
        else -> esquema.outline
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.e4)) {
        if (etiqueta != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    etiqueta,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (error) esquema.error else esquema.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                accesorio?.invoke()
            }
        }
        BasicTextField(
            value = actual,
            onValueChange = {
                interno = it
                if (it.text != valor) alCambiar(it.text)
            },
            singleLine = true,
            keyboardOptions = TecladoDatos,
            textStyle = MaterialTheme.mono.dato.copy(color = esquema.onSurface),
            cursorBrush = SolidColor(esquema.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = alto)
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                .onFocusChanged { enfocado = it.isFocused }
                .semantics {
                    (descripcion ?: etiqueta)?.let { contentDescription = it }
                    if (error) error("Valor con error")
                },
            decorationBox = { campo ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = alto)
                        .border(if (enfocado || error) Dimens.bordeFoco else Dimens.borde, colorBorde, Formas.chip)
                        .padding(horizontal = Dimens.e12),
                    contentAlignment = Alignment.CenterStart,
                ) { campo() }
            },
        )
        if (ayuda != null) {
            Text(ayuda, style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
        }
    }
}

/** Píldora de conteo ("5 valores", "✓ 5 puntos"): alto 24, radio 12, 12/600. Válida en success, error en errorContainer. */
@Composable
fun PildoraConteo(texto: String, ok: Boolean, error: Boolean, modifier: Modifier = Modifier, conCheck: Boolean = false) {
    val e = MaterialTheme.extendidos
    val esquema = MaterialTheme.colorScheme
    val (fondo, frente) = when {
        error -> esquema.errorContainer to esquema.onErrorContainer
        ok -> e.successContainer to e.success
        else -> esquema.surfaceContainerHigh to esquema.onSurfaceVariant
    }
    Surface(
        color = fondo,
        contentColor = frente,
        shape = Formas.tile,
        modifier = modifier.heightIn(min = 24.dp).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = Dimens.e8, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.e4),
        ) {
            if (conCheck && ok && !error) Icono(Iconos.check, null, tamano = Dimens.iconoChip)
            Text(texto, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp), maxLines = 1)
        }
    }
}

/** Pestañas en chips de 32 (radio 8): la activa en primaryContainer; las otras, con borde outlineVariant. */
@Composable
fun PestanasChip(opciones: List<String>, seleccionada: Int, alElegir: (Int) -> Unit, modifier: Modifier = Modifier) {
    val esquema = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.e8), verticalAlignment = Alignment.CenterVertically) {
        opciones.forEachIndexed { i, texto ->
            val activa = i == seleccionada
            Surface(
                shape = Formas.chip,
                color = if (activa) esquema.primaryContainer else Color.Transparent,
                contentColor = if (activa) esquema.onPrimaryContainer else esquema.onSurfaceVariant,
                border = if (activa) null else BorderStroke(Dimens.borde, esquema.outlineVariant),
                modifier = Modifier
                    .heightIn(min = Dimens.chipCategoria)
                    .selectable(selected = activa, onClick = { alElegir(i) }, role = Role.Tab),
            ) {
                Box(Modifier.padding(horizontal = Dimens.e16, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

// ── Botones (48dp, radio 24, ícono 18dp + labelLarge) ──

private val RellenoBoton = PaddingValues(horizontal = 20.dp, vertical = 10.dp)

@Composable
private fun RowScope.ContenidoBoton(texto: String, icono: DefIcono?) {
    if (icono != null) {
        Icono(icono, null, tamano = Dimens.iconoBoton, enBoton = true)
        Spacer(Modifier.width(Dimens.e8))
    }
    Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun BotonPrimario(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, icono: DefIcono? = null, habilitado: Boolean = true) {
    Button(
        onClick = alPulsar,
        enabled = habilitado,
        shape = Formas.boton,
        contentPadding = RellenoBoton,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        modifier = modifier.heightIn(min = Dimens.boton),
    ) { ContenidoBoton(texto, icono) }
}

@Composable
fun BotonSecundario(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, icono: DefIcono? = null, habilitado: Boolean = true) {
    OutlinedButton(
        onClick = alPulsar,
        enabled = habilitado,
        shape = Formas.boton,
        contentPadding = RellenoBoton,
        border = BorderStroke(Dimens.borde, if (habilitado) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        modifier = modifier.heightIn(min = Dimens.boton),
    ) { ContenidoBoton(texto, icono) }
}

@Composable
fun BotonTexto(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    icono: DefIcono? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = alPulsar,
        shape = Formas.boton,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        modifier = modifier.heightIn(min = Dimens.boton),
    ) { ContenidoBoton(texto, icono) }
}

@Composable
fun BotonTonal(texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, icono: DefIcono? = null, habilitado: Boolean = true) {
    Button(
        onClick = alPulsar,
        enabled = habilitado,
        shape = Formas.boton,
        contentPadding = RellenoBoton,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier.heightIn(min = Dimens.boton),
    ) { ContenidoBoton(texto, icono) }
}

/** Segmentado (Material 3): alto 48, borde 1dp outline, radio 24, seleccionado en primaryContainer con check 16dp. */
@Composable
fun Segmentado(
    opciones: List<String>,
    seleccionado: Int,
    alElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, texto ->
            SegmentedButton(
                selected = i == seleccionado,
                onClick = { alElegir(i) },
                enabled = habilitado,
                shape = SegmentedButtonDefaults.itemShape(i, opciones.size, Formas.boton),
                border = SegmentedButtonDefaults.borderStroke(MaterialTheme.colorScheme.outline, Dimens.borde),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = Color.Transparent,
                ),
                icon = { IconoSegmento(i == seleccionado) },
                modifier = Modifier.heightIn(min = Dimens.boton),
            ) { Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
        }
    }
}

@Composable
private fun IconoSegmento(activo: Boolean) {
    if (activo) Icono(Iconos.check, null, tamano = Dimens.iconoInfo)
}

/** Fila de dos controles con flex 1 y separación 12. */
@Composable
fun FilaDoble(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.e12), verticalAlignment = Alignment.Top, content = content)
}
