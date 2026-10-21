package com.example.pentametod2.ui.grafica

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pentametod2.model.Formato
import com.example.pentametod2.model.Grafica
import com.example.pentametod2.model.TipoSerie
import com.example.pentametod2.ui.componentes.TarjetaContorno
import com.example.pentametod2.ui.componentes.TituloTarjeta
import com.example.pentametod2.ui.componentes.marcadorPrediccion
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.extendidos
import com.example.pentametod2.ui.tema.mono

private val ALTO_MIN = 220.dp
private val ALTO_MAX = 340.dp

/** Colores resueltos del tema para usarlos dentro del Canvas. */
private class Paleta(
    val fondo: Color,
    val cuadricula: Color,
    val ejes: Color,
    val texto: Color,
    val valorClave: Color,
    val error: Color,
    val tolerancia: Color,
    val inverso: Color,
    val porRol: Map<RolSerie, Color>,
)

private class Estilos(val tick: TextStyle, val eje: TextStyle, val etiqueta: TextStyle, val etiquetaToque: TextStyle)

/** Gráfica dibujada a mano con Canvas, dentro de la tarjeta "Gráfica". Tocar muestra el punto más cercano. */
@Composable
fun GraficaView(grafica: Grafica?) {
    if (grafica == null) return
    val modelo = remember(grafica) { prepararGrafica(grafica) }
    val roles = remember(grafica) { rolesSeries(grafica) }
    val descripcion = remember(grafica) { descripcionGrafica(grafica) }
    val medidor = rememberTextMeasurer()
    val esquema = MaterialTheme.colorScheme
    val g = MaterialTheme.extendidos.grafica
    val paleta = Paleta(
        fondo = esquema.surfaceContainerLowest,
        cuadricula = g.cuadricula,
        ejes = g.ejes,
        texto = esquema.onSurfaceVariant,
        valorClave = MaterialTheme.extendidos.valorClave,
        error = g.errorSerie,
        tolerancia = g.tolerancia,
        inverso = esquema.inverseSurface,
        porRol = mapOf(
            RolSerie.DATOS to g.datos, RolSerie.CURVA to g.curva, RolSerie.PREDICCION to g.prediccion,
            RolSerie.SERIE4 to g.serie4, RolSerie.SERIE5 to g.serie5, RolSerie.ERROR to g.errorSerie, RolSerie.TOLERANCIA to g.tolerancia,
        ),
    )
    val mono = MaterialTheme.mono
    val estilos = Estilos(
        tick = mono.grafica.copy(color = esquema.onSurfaceVariant),
        eje = mono.grafica.copy(color = esquema.onSurfaceVariant, fontStyle = FontStyle.Italic, fontSize = 12.sp),
        etiqueta = mono.graficaFuerte,
        etiquetaToque = mono.graficaFuerte.copy(color = esquema.inverseOnSurface),
    )
    var toque by remember(grafica) { mutableStateOf<Offset?>(null) }

    TarjetaContorno {
        Column {
            TituloTarjeta("Gráfica")
            val subtitulo = if (grafica.escalaLogY) "${grafica.titulo} · Escala logarítmica" else grafica.titulo
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val alto = (maxWidth * 0.75f).coerceIn(ALTO_MIN, ALTO_MAX)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(alto)
                    .semantics { contentDescription = descripcion }
                    .pointerInput(grafica) { detectTapGestures { toque = if (toque == null) it else null } },
            ) {
                dibujar(modelo, roles, grafica, medidor, paleta, estilos, toque)
            }
        }
        Leyenda(modelo, roles, paleta)
        if (modelo.hayDatos) {
            Text("Toca la gráfica para ver el punto más cercano.", style = MaterialTheme.typography.bodySmall, color = esquema.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Leyenda(modelo: ModeloGrafica, roles: List<RolSerie>, paleta: Paleta) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.e20, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Dimens.e4),
    ) {
        modelo.series.forEachIndexed { i, s ->
            val rol = roles[i]
            val color = paleta.porRol.getValue(rol)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Canvas(Modifier.size(width = 24.dp, height = 16.dp)) { muestra(s.serie.tipo, rol, s.serie.discontinua, color, paleta.fondo) }
                Text(s.serie.nombre, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun DrawScope.muestra(tipo: TipoSerie, rol: RolSerie, discontinua: Boolean, color: Color, fondo: Color) {
    val c = Offset(size.width / 2, size.height / 2)
    when {
        rol == RolSerie.PREDICCION -> marcadorPrediccion(color, c, 7.dp.toPx(), 3.5.dp.toPx(), 1.3.dp.toPx())
        discontinua -> drawLine(color, Offset(0f, c.y), Offset(size.width, c.y), 1.5.dp.toPx(), pathEffect = guion(5f, 4f))
        tipo == TipoSerie.PUNTOS -> punto(color, fondo, c, 5.dp.toPx())
        else -> {
            drawLine(color, Offset(0f, c.y), Offset(size.width, c.y), 2.dp.toPx(), cap = StrokeCap.Round)
            if (tipo == TipoSerie.LINEA_CON_PUNTOS) drawCircle(color, 3.5.dp.toPx(), c)
        }
    }
}

private fun DrawScope.guion(a: Float, b: Float) = PathEffect.dashPathEffect(floatArrayOf(a.dp.toPx(), b.dp.toPx()))

/** Círculo con halo del color de la tarjeta. */
private fun DrawScope.punto(color: Color, fondo: Color, c: Offset, r: Float) {
    drawCircle(fondo, r + 1.5.dp.toPx(), c)
    drawCircle(color, r, c)
}

private fun DrawScope.dibujar(
    m: ModeloGrafica,
    roles: List<RolSerie>,
    g: Grafica,
    medidor: TextMeasurer,
    p: Paleta,
    e: Estilos,
    toque: Offset?,
) {
    val etiquetasY = m.etiquetasY.map { medidor.measure(it, e.tick) }
    val etiquetasX = m.etiquetasX.map { medidor.measure(it, e.tick) }
    val nombreX = medidor.measure(g.ejeX, e.eje)
    val nombreY = medidor.measure(g.ejeY, e.eje)
    val sep = 6.dp.toPx()
    val anchoY = etiquetasY.maxOfOrNull { it.size.width } ?: 0
    val altoTick = etiquetasX.maxOfOrNull { it.size.height } ?: 0

    val area = Rect(
        left = anchoY + sep * 2,
        top = nombreY.size.height + sep,
        right = size.width - nombreX.size.width - sep * 2,
        bottom = size.height - altoTick - sep,
    )
    if (area.width <= 0 || area.height <= 0) return
    val px = { x: Double -> aPixel(x, m.ejeX, area.left, area.right) }
    val py = { y: Double -> if (m.logY) aPixelLog(y, m.ejeY, area.bottom, area.top) else aPixel(y, m.ejeY, area.bottom, area.top) }
    val uno = 1.dp.toPx()

    // Cuadrícula en las marcas
    m.ticksX.forEachIndexed { i, t ->
        val x = px(t)
        if (x < area.left - 0.5f || x > area.right + 0.5f) return@forEachIndexed
        drawLine(p.cuadricula, Offset(x, area.top), Offset(x, area.bottom), uno)
        val et = etiquetasX[i]
        drawText(et, topLeft = Offset((x - et.size.width / 2f).coerceIn(0f, size.width - et.size.width), area.bottom + sep / 2))
    }
    m.ticksY.forEachIndexed { i, t ->
        val y = py(t)
        if (y.isNaN() || y < area.top - 0.5f || y > area.bottom + 0.5f) return@forEachIndexed
        drawLine(p.cuadricula, Offset(area.left, y), Offset(area.right, y), uno)
        val et = etiquetasY[i]
        drawText(et, topLeft = Offset(area.left - sep - et.size.width, y - et.size.height / 2f))
    }
    // Ejes: solo X abajo e Y a la izquierda
    drawLine(p.ejes, Offset(area.left, area.bottom), Offset(area.right, area.bottom), uno)
    drawLine(p.ejes, Offset(area.left, area.top), Offset(area.left, area.bottom), uno)
    // Nombres de los ejes (cursiva): X al final del eje, Y arriba del eje
    drawText(nombreX, topLeft = Offset(area.right + sep, area.bottom - nombreX.size.height / 2f))
    drawText(nombreY, topLeft = Offset((area.left - nombreY.size.width / 2f).coerceAtLeast(0f), 0f))

    if (!m.hayDatos) {
        val aviso = medidor.measure("Sin datos para graficar", e.eje)
        drawText(aviso, topLeft = Offset(area.center.x - aviso.size.width / 2f, area.center.y - aviso.size.height / 2f))
        return
    }

    val candidatos = ArrayList<Pair<Float, Float>>()
    val referencias = ArrayList<Pair<Int, Int>>()
    val orden = m.series.indices.sortedBy { if (m.series[it].serie.destacada) 1 else 0 }
    clipRect(area.left - 12.dp.toPx(), area.top - 12.dp.toPx(), area.right + 12.dp.toPx(), area.bottom + 12.dp.toPx()) {
        for (i in orden) {
            val s = m.series[i]
            val rol = roles[i]
            val color = p.porRol.getValue(rol)
            val coords = s.puntos.map { px(it.x) to py(it.y) }
            when {
                rol == RolSerie.PREDICCION -> for ((x, y) in coords) prediccion(color, p.fondo, Offset(x, y), area)
                s.serie.discontinua -> linea(coords, color, 1.5.dp.toPx(), guion(5f, 4f))
                s.serie.tipo == TipoSerie.PUNTOS -> for ((x, y) in coords) if (!y.isNaN()) punto(color, p.fondo, Offset(x, y), 5.dp.toPx())
                else -> {
                    linea(coords, color, 2.dp.toPx(), null)
                    // Sin halo: con puntos muy juntos el halo taparía la línea que los une.
                    if (s.serie.tipo == TipoSerie.LINEA_CON_PUNTOS) {
                        for ((x, y) in coords) if (!y.isNaN()) drawCircle(color, 4.5.dp.toPx(), Offset(x, y))
                    }
                }
            }
            coords.forEachIndexed { j, c ->
                candidatos += c
                referencias += i to j
            }
        }
    }
    etiquetasSeries(m, roles, coords = { i -> m.series[i].puntos.map { px(it.x) to py(it.y) } }, medidor, p, e, area)

    if (toque != null) {
        val k = puntoMasCercano(candidatos, toque.x, toque.y, 32.dp.toPx()) ?: return
        val (i, j) = referencias[k]
        val s = m.series[i]
        val pt = s.puntos[j]
        val (cx, cy) = candidatos[k]
        drawCircle(p.texto, 9.dp.toPx(), Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
        val t = medidor.measure("${s.serie.nombre}: (${Formato.auto(pt.x, 4)}, ${Formato.auto(pt.y, 6)})", e.etiquetaToque)
        val pad = 6.dp.toPx()
        val w = t.size.width + 2 * pad
        val h = t.size.height + 2 * pad
        val bx = (cx - w / 2).coerceIn(0f, size.width - w)
        val by = if (cy - h - 12.dp.toPx() >= 0) cy - h - 12.dp.toPx() else cy + 12.dp.toPx()
        drawRoundRect(p.inverso, Offset(bx, by), Size(w, h), CornerRadius(pad, pad))
        drawText(t, topLeft = Offset(bx + pad, by + pad))
    }
}

/** Etiquetas fijas: "(x, y)" junto a la predicción, "E₇ = …" al final del error y "tolerancia …" sobre su línea. */
private fun DrawScope.etiquetasSeries(
    m: ModeloGrafica,
    roles: List<RolSerie>,
    coords: (Int) -> List<Pair<Float, Float>>,
    medidor: TextMeasurer,
    p: Paleta,
    e: Estilos,
    area: Rect,
) {
    m.series.forEachIndexed { i, s ->
        val c = coords(i).lastOrNull { !it.second.isNaN() } ?: return@forEachIndexed
        val ultimo = s.puntos.last()
        val (texto, color) = when (roles[i]) {
            RolSerie.PREDICCION -> "(${etiquetaValor(ultimo.x)}, ${etiquetaValor(ultimo.y)})" to p.valorClave
            RolSerie.ERROR -> "E${subindice(ultimo.x.toInt())} = ${etiquetaValor(ultimo.y)}" to p.error
            RolSerie.TOLERANCIA -> "tolerancia ${Formato.corto(ultimo.y)}" to p.tolerancia
            else -> return@forEachIndexed
        }
        val t = medidor.measure(texto, e.etiqueta.copy(color = color))
        val margen = 6.dp.toPx()
        val (x, y) = when (roles[i]) {
            RolSerie.TOLERANCIA -> (area.right - t.size.width) to (c.second - t.size.height - 2.dp.toPx())
            else -> {
                val derecha = c.first + 12.dp.toPx()
                val xx = if (derecha + t.size.width <= size.width) derecha else c.first - 12.dp.toPx() - t.size.width
                xx to (c.second - t.size.height - margen)
            }
        }
        drawText(t, topLeft = Offset(x.coerceIn(0f, (size.width - t.size.width).coerceAtLeast(0f)), y.coerceIn(0f, (size.height - t.size.height).coerceAtLeast(0f))))
    }
}

/** Predicción: guías punteadas a ambos ejes, anillo de radio 11 (1.5) y punto de 6.5 con halo. */
private fun DrawScope.prediccion(color: Color, fondo: Color, c: Offset, area: Rect) {
    if (c.y.isNaN()) return
    val guia = guion(3f, 3f)
    drawLine(color, c, Offset(c.x, area.bottom), 1.dp.toPx(), pathEffect = guia)
    drawLine(color, c, Offset(area.left, c.y), 1.dp.toPx(), pathEffect = guia)
    drawCircle(color, 11.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
    punto(color, fondo, c, 6.5.dp.toPx())
}

/** Trazo continuo (o discontinuo), joins y caps redondeados; se corta donde un punto no es dibujable. */
private fun DrawScope.linea(coords: List<Pair<Float, Float>>, color: Color, grosor: Float, efecto: PathEffect?) {
    val path = Path()
    var abierto = false
    for ((x, y) in coords) {
        if (x.isNaN() || y.isNaN()) {
            abierto = false
            continue
        }
        if (abierto) path.lineTo(x, y) else path.moveTo(x, y)
        abierto = true
    }
    drawPath(path, color, style = Stroke(width = grosor, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = efecto))
}
