package com.example.pentametod2.ui.resultados

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pentametod2.model.Bloque
import com.example.pentametod2.model.BloqueGrupo
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.Formato
import com.example.pentametod2.model.Formula
import com.example.pentametod2.model.ItemResumen
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.model.TipoTexto
import com.example.pentametod2.ui.componentes.AccionBanner
import com.example.pentametod2.ui.componentes.BannerEstado
import com.example.pentametod2.ui.componentes.ChipEstado
import com.example.pentametod2.ui.componentes.EtiquetaSeccion
import com.example.pentametod2.ui.componentes.FilaResumen
import com.example.pentametod2.ui.componentes.TarjetaContorno
import com.example.pentametod2.ui.componentes.TipoBanner
import com.example.pentametod2.ui.componentes.TituloTarjeta
import com.example.pentametod2.ui.grafica.GraficaView
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.TextoFormula
import com.example.pentametod2.ui.tema.extendidos
import com.example.pentametod2.ui.tema.mono

/**
 * Resultados, en dos formatos:
 *  - Con pasos (regresión polinomial): separador PASOS + chip → banners → lista de pasos → Gráfica →
 *    tablas → Conclusión. No se muestran la tarjeta de fórmulas ni la de resumen (están en los pasos).
 *  - Anterior (los demás métodos): EXPLICACIÓN → RESULTADOS + chip → banners → Resumen → Gráfica → bloques.
 * "Copiar resultados" está en el menú de la barra superior.
 */
@Composable
fun ResultadosMetodo(salida: SalidaMetodo, alEditar: () -> Unit, alAbrirMetodo: (MetodoId) -> Unit, claveCalculo: Int,
                     mostrarIndicador: Boolean = true) {
    if (salida.comparacionLagrange != null) {
        ResultadosLagrange(salida, alEditar, alAbrirMetodo, claveCalculo)
        return
    }
    var avisoDescartado by remember(claveCalculo) { mutableStateOf(false) }
    val conPasos = usaPasos(salida)
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.entreTarjetas)) {
        if (!conPasos && salida.formulas.isNotEmpty()) TarjetaExplicacion(salida.formulas)
        SeparadorResultados(salida, if (conPasos) "Pasos" else "Resultados", mostrarIndicador)
        if (salida.estado == Estado.ERROR) {
            val (titulo, acciones) = bannerError(salida)
            BannerEstado(
                TipoBanner.ERROR, titulo, salida.error,
                acciones = acciones.map { AccionBanner(textoAccion(it), accionError(it, alEditar, alAbrirMetodo)) },
            )
        }
        val avisos = bannerAvisos(salida.advertencias)
        if (avisos != null && !avisoDescartado) {
            BannerEstado(TipoBanner.AVISO, avisos.first, avisos.second, acciones = listOf(AccionBanner("Entendido") { avisoDescartado = true }))
        }
        if (conPasos) ListaPasos(pasosDe(salida), claveCalculo)
        if (!conPasos && salida.resumen.isNotEmpty()) TarjetaResumen(salida)
        val g = salida.grafica
        if (g != null) GraficaView(g)
        val resto = salida.bloques.filterNot { ocultarBloque(it, salida) || it is BloquePaso || yaEnUnPaso(it, salida) }
        for (e in agruparBloques(resto)) ElementoView(e, salida, conPasos)
    }
}

private fun accionError(a: AccionError, alEditar: () -> Unit, alAbrirMetodo: (MetodoId) -> Unit): () -> Unit = when (a) {
    AccionError.CAMBIAR_DESPEJE, AccionError.EDITAR_DATOS -> alEditar
    AccionError.PROBAR_NEWTON -> abrirNewton(alAbrirMetodo)
}

private fun abrirNewton(alAbrirMetodo: (MetodoId) -> Unit): () -> Unit = { alAbrirMetodo(MetodoId.NEWTON_SISTEMAS) }

/** Vista completa (para usos fuera de MetodoScreen, p. ej. vistas previas). */
@Composable
fun SalidaView(salida: SalidaMetodo) {
    ResultadosMetodo(salida, alEditar = {}, alAbrirMetodo = {}, claveCalculo = 0)
}

// ── 1. Explicación ──

@Composable
private fun TarjetaExplicacion(formulas: List<Formula>) {
    var abierta by rememberSaveable { mutableStateOf(true) }
    val giro by animateFloatAsState(if (abierta) 0f else 180f, tween(150), label = "chevron")
    TarjetaContorno(Modifier.animateContentSize(tween(150)), padding = androidx.compose.foundation.layout.PaddingValues(0.dp), espacio = 0.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.encabezadoExplicacion)
                .clickable(role = Role.Button, onClickLabel = if (abierta) "Ocultar fórmula" else "Mostrar fórmula") { abierta = !abierta }
                .semantics { stateDescription = if (abierta) "Abierta" else "Cerrada" }
                .padding(horizontal = Dimens.paddingTarjeta, vertical = Dimens.e12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                EtiquetaSeccion("Explicación")
                TituloTarjeta("Fórmula")
            }
            Icono(Iconos.chevronArriba, null, Modifier.rotate(giro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (abierta) {
            Column(
                Modifier.padding(start = Dimens.paddingTarjeta, end = Dimens.paddingTarjeta, bottom = Dimens.paddingTarjeta),
                verticalArrangement = Arrangement.spacedBy(Dimens.e8),
            ) {
                for (f in formulas) {
                    if (formulas.size > 1) Text(f.titulo, style = MaterialTheme.typography.titleSmall)
                    EtiquetaFormula("Fórmula general")
                    CajaFormula(f.general)
                    if (f.sustituida.isNotEmpty()) {
                        EtiquetaFormula("Con tus datos")
                        CajaFormula(f.sustituida)
                    }
                }
            }
        }
    }
}

@Composable
private fun EtiquetaFormula(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Dimens.e4),
    )
}

@Composable
private fun CajaFormula(lineas: List<String>) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = Formas.chip, modifier = Modifier.fillMaxWidth()) {
        SelectionContainer {
            Column(Modifier.padding(Dimens.e12), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (l in lineas) TextoFormula(l, secundaria = esLineaSecundaria(l))
            }
        }
    }
}

// ── 2. Separador ──

@Composable
private fun SeparadorResultados(s: SalidaMetodo, etiqueta: String, mostrarIndicador: Boolean = true) {
    val (texto, tipo) = chipResultados(s)
    Row(Modifier.fillMaxWidth().padding(top = Dimens.e8), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.e12)) {
        EtiquetaSeccion(etiqueta, Modifier.semantics { heading() })
        HorizontalDivider(Modifier.weight(1f), thickness = Dimens.borde, color = MaterialTheme.colorScheme.outlineVariant)
        if (mostrarIndicador) ChipEstado(texto, tipo)
    }
}

// ── 4. Resumen ──

@Composable
private fun TarjetaResumen(s: SalidaMetodo) {
    TarjetaContorno(espacio = 0.dp) {
        TituloTarjeta("Resumen", Modifier.padding(bottom = Dimens.e8))
        SelectionContainer {
            Column {
                s.resumen.forEachIndexed { i, item ->
                    if (i > 0 && !item.destacado && !s.resumen[i - 1].destacado) {
                        HorizontalDivider(thickness = Dimens.borde, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    FilaResumenSalida(item, s)
                }
            }
        }
    }
}

@Composable
private fun FilaResumenSalida(item: ItemResumen, s: SalidaMetodo) {
    when {
        item.etiqueta == "Convergió" || item.etiqueta == "Estado" -> {
            val (texto, tipo) = chipConvergencia(item.valor, s)
            FilaResumen(item, valorPersonalizado = { ChipEstado(texto, tipo) })
        }
        item.etiqueta == "Error final" && s.estado == Estado.ERROR -> FilaResumen(item, colorValor = MaterialTheme.colorScheme.error)
        else -> FilaResumen(item)
    }
}

// ── 6–9. Bloques ──

@Composable
private fun ElementoView(e: ElementoVista, s: SalidaMetodo, conPasos: Boolean = false) {
    when (e) {
        is ElementoVista.Matrices -> TarjetaMatrices(e.matrices)
        is ElementoVista.Simple -> BloqueView(e.bloque, s, conPasos)
    }
}

@Composable
private fun BloqueView(b: Bloque, s: SalidaMetodo, conPasos: Boolean = false) {
    when (b) {
        is BloqueTexto -> TextoView(b)
        is BloqueTabla -> TarjetaTabla(b, s, conPasos)
        is BloqueMatriz -> TarjetaMatrices(listOf(b))
        is BloqueGrupo -> GrupoView(b, s)
        is BloquePaso -> ListaPasos(listOf(b), claveCalculo = 0) // normalmente van todos juntos en ResultadosMetodo
    }
}

@Composable
private fun TextoView(b: BloqueTexto) {
    when (b.tipo) {
        TipoTexto.NORMAL -> Text(b.texto, style = MaterialTheme.typography.bodyMedium)
        TipoTexto.ADVERTENCIA -> BannerEstado(TipoBanner.AVISO, b.texto, null)
        TipoTexto.CONCLUSION -> TarjetaConclusion(b)
    }
}

/** Conclusión: sin borde, fondo primaryContainer, radio 16, padding 20/16, ícono checkCirculo y barra de R². */
@Composable
private fun TarjetaConclusion(b: BloqueTexto) {
    val texto = MaterialTheme.colorScheme.onPrimaryContainer
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = texto, shape = Formas.tarjeta, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = Dimens.e16, vertical = Dimens.e20), verticalArrangement = Arrangement.spacedBy(Dimens.e12)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                Icono(Iconos.checkCirculo, null)
                TituloTarjeta("Conclusión")
            }
            Text(conPorcentajesEnNegrita(b.texto), style = MaterialTheme.typography.bodyLarge)
            val p = b.progreso
            if (p != null) BarraR2(p, texto)
        }
    }
}

private fun conPorcentajesEnNegrita(texto: String): AnnotatedString = buildAnnotatedString {
    for ((parte, negrita) in partesConPorcentajes(texto)) {
        if (negrita) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(parte) } else append(parte)
    }
}

@Composable
private fun BarraR2(progreso: Double, color: Color) {
    val pista = MaterialTheme.extendidos.pistaR2
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
        Text("R² 0", style = MaterialTheme.mono.pequeno)
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(pista)
                .semantics { stateDescription = "R² = ${Formato.fijo(progreso, 4)}" },
        ) {
            Box(Modifier.fillMaxWidth(progreso.toFloat().coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
        Text("1", style = MaterialTheme.mono.pequeno)
    }
}

// ── Tabla con primera columna fija ──

@Composable
private fun TarjetaTabla(b: BloqueTabla, s: SalidaMetodo, conPasos: Boolean) {
    TarjetaContorno {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TituloTarjeta(b.titulo, Modifier.weight(1f))
            Text("${b.columnas.size} columnas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TablaView(b, marcarUltimaFila(b, s), textoDesliza(b.columnas.size, conPasos))
    }
}

@Composable
internal fun TablaView(b: BloqueTabla, ultimaConError: Boolean, aviso: String) {
    val (filas, ocultas) = remember(b) { filasVisibles(b) }
    val textos = remember(b) { filas.map { f -> f.map { textoCelda(it, b.decimales) } } }
    val estiloCelda = MaterialTheme.mono.tabla.copy(textAlign = TextAlign.End)
    val estiloFuerte = MaterialTheme.mono.tablaFuerte.copy(textAlign = TextAlign.End)
    val anchos = anchosEnDp(b.columnas, textos, estiloCelda, estiloFuerte)
    val altoFila = if (esTablaIteraciones(b)) Dimens.filaIteraciones else Dimens.filaTabla
    val linea = MaterialTheme.colorScheme.outlineVariant
    val scroll = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.e8)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(Formas.chip)
                .border(Dimens.borde, linea, Formas.chip),
        ) {
            // Primera columna fija
            Column(Modifier.width(anchos[0]).drawBehind { drawLine(linea, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx()) }) {
                Encabezado(b.columnas[0], anchos[0])
                filas.forEachIndexed { i, _ -> Celdas(b, filas, textos, i, 0..0, anchos, altoFila, ultimaConError) }
            }
            Column(Modifier.weight(1f).horizontalScroll(scroll)) {
                Row { for (j in 1 until b.columnas.size) Encabezado(b.columnas[j], anchos[j]) }
                filas.forEachIndexed { i, _ -> Celdas(b, filas, textos, i, 1 until b.columnas.size, anchos, altoFila, ultimaConError) }
            }
        }
        if (scroll.maxValue > 0) IndicadorDesplazamiento(scroll, aviso)
        if (ocultas > 0) {
            Text(
                "Se muestran las primeras $MAX_FILAS_TABLA filas; hay $ocultas más (usa \"Copiar resultados\" para verlas todas).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Encabezado(texto: String, ancho: Dp) {
    Box(
        Modifier
            .width(ancho)
            .height(Dimens.encabezadoTabla)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Dimens.e8),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(texto, style = MaterialTheme.mono.tablaFuerte.copy(textAlign = TextAlign.End), softWrap = false, maxLines = 1, modifier = Modifier.fillMaxWidth())
    }
}

/** Celdas de la fila `i` en el rango de columnas dado (la parte fija o la desplazable). */
@Composable
private fun Celdas(
    b: BloqueTabla,
    filas: List<List<Celda>>,
    textos: List<List<String>>,
    i: Int,
    columnas: IntRange,
    anchos: List<Dp>,
    altoFila: Dp,
    ultimaConError: Boolean,
) {
    val esTotal = b.filaTotal && i == filas.lastIndex
    val esUltimaError = ultimaConError && i == filas.lastIndex
    val e = MaterialTheme.extendidos
    val esquema = MaterialTheme.colorScheme
    val fondo = when {
        esTotal -> esquema.primaryContainer
        esUltimaError -> e.filaUltimaError
        i % 2 == 1 -> e.filaAlterna
        else -> Color.Transparent
    }
    val alto = if (esTotal) Dimens.filaTotal else altoFila
    val bordeTotal = esquema.onPrimaryContainer
    Row(
        Modifier
            .height(alto)
            .background(fondo)
            .drawBehind { if (esTotal) drawLine(bordeTotal, Offset(0f, 0f), Offset(size.width, 0f), 2.dp.toPx()) },
    ) {
        for (j in columnas) {
            val celda = filas[i].getOrNull(j) ?: Celda.Vacia
            val texto = textos[i].getOrElse(j) { "" }
            val rojo = esUltimaError && esColumnaError(b.columnas.getOrElse(j) { "" })
            val estilo = when {
                esTotal -> MaterialTheme.mono.tablaFuerte.copy(color = esquema.onPrimaryContainer)
                rojo -> MaterialTheme.mono.tablaFuerte.copy(color = esquema.error)
                else -> MaterialTheme.mono.tabla.copy(color = esquema.onSurface)
            }
            val alinear = if (celda is Celda.Txt && j > 0) TextAlign.Start else TextAlign.End
            Box(Modifier.width(anchos.getOrElse(j) { Dimens.anchoMinColumna }).fillMaxHeight().padding(horizontal = Dimens.e8), contentAlignment = Alignment.CenterEnd) {
                Text(texto, style = estilo.copy(textAlign = alinear), softWrap = false, maxLines = 1, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Barra de 4dp: pista de 120dp en surfaceContainerHigh con relleno outline proporcional + texto. */
@Composable
private fun IndicadorDesplazamiento(scroll: ScrollState, aviso: String) {
    val pista = 120.dp
    val total = (scroll.viewportSize + scroll.maxValue).coerceAtLeast(1)
    val fraccionVisible = scroll.viewportSize.toFloat() / total
    val relleno = pista * fraccionVisible.coerceIn(0.1f, 1f)
    val desplazamiento = (pista - relleno) * (if (scroll.maxValue == 0) 0f else scroll.value.toFloat() / scroll.maxValue)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.e12)) {
        Box(Modifier.width(pista).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Box(Modifier.offset(x = desplazamiento).width(relleno).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline))
        }
        Text(aviso, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Ancho de cada columna: el texto más largo (medido) + relleno, con un mínimo de 64dp. */
@Composable
private fun anchosEnDp(columnas: List<String>, textos: List<List<String>>, estiloCelda: TextStyle, estiloEncabezado: TextStyle): List<Dp> {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    return remember(columnas, textos, estiloCelda, estiloEncabezado, densidad) {
        val anchoCaracter = medidor.measure("0", estiloEncabezado).size.width
        val caracteres = anchosColumnas(List(columnas.size) { "" }, textos)
        columnas.indices.map { j ->
            val encabezado = medidor.measure(columnas[j], estiloEncabezado).size.width
            val px = maxOf(encabezado, caracteres[j] * anchoCaracter)
            maxOf(with(densidad) { px.toDp() } + Dimens.e8 * 2 + 4.dp, Dimens.anchoMinColumna)
        }
    }
}

// ── Matrices ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaMatrices(matrices: List<BloqueMatriz>) {
    val (titulo, formula) = tituloMatrices(matrices)
    TarjetaContorno {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.e12)) {
            TituloTarjeta(titulo)
            if (formula != null) Text(formula, style = MaterialTheme.mono.campo, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ContenidoMatrices(matrices)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContenidoMatrices(matrices: List<BloqueMatriz>) {
    val normales = matrices.filter { rotulosCoeficientes(it) == null }
    val coeficientes = matrices.filter { rotulosCoeficientes(it) != null }
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.e12)) {
            if (normales.isNotEmpty()) {
                // FlowRow con el ancho de la tarjeta para que C y b queden lado a lado y salten de línea;
                // cada matriz se desplaza por separado si no cabe.
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.e24),
                    verticalArrangement = Arrangement.spacedBy(Dimens.e16),
                ) {
                    for (m in normales) {
                        Box(Modifier.horizontalScroll(rememberScrollState())) { MatrizView(m, fuerte = false) }
                    }
                }
            }
            for (m in coeficientes) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = Formas.tile, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(Dimens.e12).horizontalScroll(rememberScrollState())) { MatrizView(m, fuerte = true) }
                }
            }
        }
    }
}

@Composable
private fun MatrizView(b: BloqueMatriz, fuerte: Boolean) {
    val textos = remember(b) { b.datos.map { f -> f.map { Formato.auto(it, b.decimales) } } }
    val estilo = (if (fuerte) MaterialTheme.mono.dato.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.mono.dato).copy(textAlign = TextAlign.End)
    val columnas = textos.maxOfOrNull { it.size } ?: 0
    val anchos = anchosMatriz(textos, columnas, estilo)
    val rotulos = rotulosCoeficientes(b)
    val corchete = MaterialTheme.colorScheme.onSurface
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${b.nombre} =",
            style = MaterialTheme.mono.dato.copy(fontStyle = FontStyle.Italic),
            modifier = Modifier.padding(end = Dimens.e8),
        )
        Column(
            Modifier.corchetes(corchete).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.e4),
        ) {
            for (fila in textos) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    fila.forEachIndexed { j, t -> Text(t, style = estilo, softWrap = false, modifier = Modifier.width(anchos[j])) }
                }
            }
        }
        if (rotulos != null) {
            Column(Modifier.padding(start = Dimens.e12, top = 6.dp), verticalArrangement = Arrangement.spacedBy(Dimens.e4)) {
                for (r in rotulos) Text(r, style = MaterialTheme.mono.dato, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun anchosMatriz(textos: List<List<String>>, columnas: Int, estilo: TextStyle): List<Dp> {
    val medidor = rememberTextMeasurer()
    val densidad = LocalDensity.current
    return remember(textos, estilo, densidad) {
        val ancho = medidor.measure("0", estilo).size.width
        val chars = anchosColumnas(List(columnas) { "" }, textos)
        chars.map { with(densidad) { (it * ancho).toDp() } + 2.dp }
    }
}

/** Corchetes: líneas verticales de 1.5dp con patas de 6dp hacia adentro (sin lado interior). */
private fun Modifier.corchetes(color: Color): Modifier = drawBehind {
    val g = 1.5.dp.toPx()
    val pata = 6.dp.toPx()
    val w = size.width
    val h = size.height
    drawLine(color, Offset(0f, 0f), Offset(0f, h), g)
    drawLine(color, Offset(0f, 0f), Offset(pata, 0f), g)
    drawLine(color, Offset(0f, h), Offset(pata, h), g)
    drawLine(color, Offset(w, 0f), Offset(w, h), g)
    drawLine(color, Offset(w, 0f), Offset(w - pata, 0f), g)
    drawLine(color, Offset(w, h), Offset(w - pata, h), g)
}

// ── Grupo (acordeón) ──

@Composable
private fun GrupoView(b: BloqueGrupo, s: SalidaMetodo) {
    var abierto by rememberSaveable(b.titulo) { mutableStateOf(!b.colapsado) }
    val giro by animateFloatAsState(if (abierto) 0f else 180f, tween(150), label = "grupo")
    TarjetaContorno(Modifier.animateContentSize(tween(150)), padding = androidx.compose.foundation.layout.PaddingValues(0.dp), espacio = 0.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.areaTactil)
                .clickable(role = Role.Button, onClickLabel = if (abierto) "Contraer" else "Desplegar") { abierto = !abierto }
                .semantics { stateDescription = if (abierto) "Desplegado" else "Contraído" }
                .padding(horizontal = Dimens.paddingTarjeta, vertical = Dimens.e12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(b.titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icono(Iconos.chevronArriba, null, Modifier.rotate(giro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (abierto) {
            Column(
                Modifier.padding(start = Dimens.paddingTarjeta, end = Dimens.paddingTarjeta, bottom = Dimens.paddingTarjeta),
                verticalArrangement = Arrangement.spacedBy(Dimens.e12),
            ) {
                for (e in agruparBloques(b.bloques)) {
                    when (e) {
                        is ElementoVista.Matrices -> ContenidoMatrices(e.matrices)
                        is ElementoVista.Simple -> BloqueView(e.bloque, s)
                    }
                }
            }
        }
    }
}
