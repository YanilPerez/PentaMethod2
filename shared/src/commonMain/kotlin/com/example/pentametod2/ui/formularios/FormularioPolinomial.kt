package com.example.pentametod2.ui.formularios

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pentametod2.ui.componentes.BotonTexto
import com.example.pentametod2.ui.componentes.CampoCompacto
import com.example.pentametod2.ui.componentes.PestanasChip
import com.example.pentametod2.ui.componentes.PildoraConteo
import com.example.pentametod2.ui.componentes.Segmentado
import com.example.pentametod2.ui.componentes.TarjetaContorno
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.TextoFormula
import com.example.pentametod2.ui.tema.mono

private val ALTO_CELDA = 36.dp
private val ANCHO_INDICE = 24.dp

/**
 * Regresión polinomial (propuesta C): tarjeta de datos (Lista | Tabla, con conteo y validación en vivo)
 * y tarjeta de ajuste (grado segmentado, x a predecir y vista previa del polinomio).
 * [intentoFallido] sube cada vez que "Calcular" se pulsa con datos inválidos: muestra el mensaje y enfoca el campo.
 */
@Composable
fun FormularioRegresionPolinomial(
    estado: EstadoRegresionPolinomial,
    alCambiar: (EstadoFormulario) -> Unit,
    intentoFallido: Int,
) {
    val cambiar: (EstadoRegresionPolinomial) -> Unit = { alCambiar(it.conGradoAjustado()) }
    val validacion = estado.validacion()
    val focoX = remember { FocusRequester() }
    val focoY = remember { FocusRequester() }
    val filas = listaATabla(estado.xs, estado.ys, 1).first.size
    val focosTabla = remember(filas) { List(filas) { FocusRequester() to FocusRequester() } }
    val objetivo = if (intentoFallido > 0) objetivoFoco(validacion) else null

    LaunchedEffect(intentoFallido) {
        if (intentoFallido == 0) return@LaunchedEffect
        val (columna, fila) = objetivoFoco(validacion) ?: return@LaunchedEffect
        val foco = if (estado.vista == VistaDatos.LISTA) {
            if (columna == Columna.X) focoX else focoY
        } else {
            focosTabla[fila.coerceIn(0, focosTabla.size - 1)].let { if (columna == Columna.X) it.first else it.second }
        }
        runCatching { foco.requestFocus() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.entreTarjetas)) {
        TarjetaContorno(espacio = Dimens.e12) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PestanasChip(
                    opciones = listOf("Lista", "Tabla"),
                    seleccionada = estado.vista.ordinal,
                    alElegir = { cambiar(estado.conVista(VistaDatos.entries[it])) },
                    modifier = Modifier.weight(1f),
                )
                if (estado.vista == VistaDatos.TABLA) {
                    val p = pildoraTabla(estado.xs, estado.ys)
                    PildoraConteo(p.texto, ok = p.tipo == TipoPildora.OK, error = p.tipo == TipoPildora.ERROR, conCheck = true)
                }
            }
            if (estado.vista == VistaDatos.LISTA) {
                CampoLista("Valores x", estado.xs, estado.ys, focoX, null) { cambiar(estado.copy(xs = it)) }
                CampoLista("Valores y", estado.ys, estado.xs, focoY, AYUDA_LISTA) { cambiar(estado.copy(ys = it)) }
                avisoLongitudes(estado.xs, estado.ys)?.let { TextoError(it) }
            } else {
                TablaPuntos(estado, focosTabla, objetivo, cambiar)
            }
            val mensaje = validacion.mensaje()
            if (intentoFallido > 0 && mensaje != null && validacion !is ValidacionPares.LongitudesDistintas) TextoError(mensaje)
        }
        TarjetaAjuste(estado, cambiar)
    }
}

/** Campo de lista con su píldora de conteo. El texto crudo se guarda aparte para no reescribir lo que se teclea. */
@Composable
private fun CampoLista(
    etiqueta: String,
    valores: List<String>,
    otros: List<String>,
    foco: FocusRequester,
    ayuda: String?,
    alCambiar: (List<String>) -> Unit,
) {
    var texto by rememberSaveable { mutableStateOf(tablaALista(valores)) }
    // Si los valores cambian desde fuera ("Cargar ejemplo"), se muestra el texto rehecho. Se deriva en la
    // composición (no en un efecto) para no pisar lo que se está tecleando.
    val mostrado = if (valoresDeTexto(texto) == valores) texto else tablaALista(valores)
    val p = pildoraValores(valores, otros)
    CampoCompacto(
        valor = mostrado,
        alCambiar = {
            texto = it
            alCambiar(valoresDeTexto(it))
        },
        etiqueta = etiqueta,
        accesorio = { PildoraConteo(p.texto, ok = p.tipo == TipoPildora.OK, error = p.tipo == TipoPildora.ERROR) },
        ayuda = ayuda,
        error = p.tipo == TipoPildora.ERROR,
        focusRequester = foco,
    )
}

@Composable
private fun TextoError(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

// ── Tabla ──

@Suppress("DEPRECATION") // LocalClipboard (la API nueva) necesita un ClipEntry que solo se construye por plataforma.
@Composable
private fun TablaPuntos(
    estado: EstadoRegresionPolinomial,
    focos: List<Pair<FocusRequester, FocusRequester>>,
    objetivo: Pair<Columna, Int>?,
    cambiar: (EstadoRegresionPolinomial) -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val (xs, ys) = listaATabla(estado.xs, estado.ys, 1)
    val portapapeles = LocalClipboardManager.current
    var errorPegado by remember { mutableStateOf<String?>(null) }

    Surface(shape = Formas.chip, color = esquema.surfaceContainerLowest, border = BorderStroke(Dimens.borde, esquema.outlineVariant)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(esquema.primaryContainer).heightIn(min = Dimens.encabezadoTabla).padding(start = Dimens.e8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.e8),
            ) {
                EncabezadoTabla("#", Modifier.width(ANCHO_INDICE), TextAlign.End)
                EncabezadoTabla("x", Modifier.weight(1f))
                EncabezadoTabla("y", Modifier.weight(1f))
                Box(Modifier.width(Dimens.tactilCompacto))
            }
            for (i in xs.indices) {
                if (i > 0) HorizontalDivider(thickness = Dimens.borde, color = esquema.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Dimens.tactilCompacto).padding(start = Dimens.e8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.e8),
                ) {
                    Text(
                        "${i + 1}",
                        Modifier.width(ANCHO_INDICE),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.mono.pequeno,
                        color = esquema.onSurfaceVariant,
                    )
                    CeldaPunto(xs[i], "x del punto ${i + 1}", objetivo == (Columna.X to i), focos.getOrNull(i)?.first, Modifier.weight(1f)) {
                        cambiar(estado.conCelda(i, Columna.X, it))
                    }
                    CeldaPunto(ys[i], "y del punto ${i + 1}", objetivo == (Columna.Y to i), focos.getOrNull(i)?.second, Modifier.weight(1f)) {
                        cambiar(estado.conCelda(i, Columna.Y, it))
                    }
                    val sePuedeQuitar = xs.size > 1
                    Box(
                        Modifier
                            .size(Dimens.tactilCompacto)
                            .clickable(enabled = sePuedeQuitar, role = Role.Button, onClickLabel = "Quitar punto ${i + 1}") {
                                cambiar(estado.quitarPunto(i))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icono(
                            Iconos.cerrar,
                            "Quitar punto ${i + 1}",
                            tamano = Dimens.iconoBoton,
                            color = if (sePuedeQuitar) esquema.onSurfaceVariant else esquema.outlineVariant,
                        )
                    }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.e4)) {
        BotonTexto("Agregar punto", {
            errorPegado = null
            cambiar(estado.agregarPunto())
        }, icono = Iconos.agregar)
        BotonTexto("Pegar de Excel", {
            when (val r = parsearPegado(portapapeles.getText()?.text.orEmpty())) {
                is ResultadoPegado.Ok -> {
                    errorPegado = null
                    cambiar(estado.copy(xs = r.xs, ys = r.ys))
                }
                is ResultadoPegado.Error -> errorPegado = r.mensaje
            }
        }, icono = Iconos.pegar)
    }
    errorPegado?.let { TextoError(it) }
}

@Composable
private fun EncabezadoTabla(texto: String, modifier: Modifier, alineacion: TextAlign = TextAlign.Start) {
    Text(texto, modifier, textAlign = alineacion, style = MaterialTheme.mono.tablaFuerte, color = MaterialTheme.colorScheme.onPrimaryContainer)
}

/** [esObjetivo]: es la celda a la que "Calcular" mandó el foco por un error. */
@Composable
private fun CeldaPunto(
    valor: String,
    descripcion: String,
    esObjetivo: Boolean,
    foco: FocusRequester?,
    modifier: Modifier,
    alCambiar: (String) -> Unit,
) {
    val invalida = valor.isNotBlank() && !esNumero(valor)
    CampoCompacto(
        valor = valor,
        alCambiar = alCambiar,
        etiqueta = null,
        modifier = modifier,
        error = invalida || esObjetivo,
        alto = ALTO_CELDA,
        focusRequester = foco,
        descripcion = descripcion,
    )
}

// ── Ajuste ──

@Composable
private fun TarjetaAjuste(estado: EstadoRegresionPolinomial, cambiar: (EstadoRegresionPolinomial) -> Unit) {
    val n = estado.puntos()
    val tope = maxOf(gradoMaximo(n), 1)
    val grado = estado.gradoEfectivo()
    TarjetaContorno(espacio = Dimens.e12) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.e4)) {
            Text(
                "Grado",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Segmentado(
                opciones = (1..tope).map { it.toString() },
                seleccionado = grado - 1,
                alElegir = { cambiar(estado.copy(grado = (it + 1).toString())) },
                habilitado = n >= 2,
            )
            Text(textoGradoMaximo(n), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CampoCompacto(
            valor = estado.xPredecir,
            alCambiar = { cambiar(estado.copy(xPredecir = it)) },
            etiqueta = "x a predecir (opcional)",
            seleccionarTodoAlEnfocar = true,
        )
        VistaPrevia(formulaVistaPrevia(grado))
    }
}

/** "SE AJUSTARÁ" + el polinomio del grado elegido, en primaryContainer. */
@Composable
private fun VistaPrevia(formula: String) {
    val esquema = MaterialTheme.colorScheme
    Surface(color = esquema.primaryContainer, contentColor = esquema.onPrimaryContainer, shape = Formas.chip, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.e12), verticalArrangement = Arrangement.spacedBy(Dimens.e4)) {
            Text("SE AJUSTARÁ", style = MaterialTheme.mono.etiquetaSeccion, color = esquema.onPrimaryContainer)
            TextoFormula(formula, color = esquema.onPrimaryContainer)
        }
    }
}
