package com.example.pentametod2.ui.tema

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ícono de trazo (viewBox 24×24, sin relleno, caps y joins redondeados).
 * `grosor` null = 1.75 (o 1.9 dentro de botones).
 */
data class DefIcono(val nombre: String, val paths: List<String>, val grosor: Float? = null)

private const val CIRCULO = "M21 12a9 9 0 1 1 -18 0a9 9 0 1 1 18 0z"

/** Íconos del diseño, construidos a partir de sus paths SVG (sin material-icons). */
object Iconos {
    val atras = DefIcono("atras", listOf("M19 12H5", "M11 6l-6 6 6 6"))
    val info = DefIcono("info", listOf(CIRCULO, "M12 11v5", "M12 8h.01"))
    val ayuda = DefIcono("ayuda", listOf(CIRCULO, "M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .8-1 1.5v.2", "M12 17h.01"))
    val chevronDerecha = DefIcono("chevronDerecha", listOf("M9 6l6 6-6 6"))

    /** Para "abajo", rotar 180°. */
    val chevronArriba = DefIcono("chevronArriba", listOf("M6 15l6-6 6 6"))
    val editar = DefIcono("editar", listOf("M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3", "M13.5 6.5l3 3"))
    val ejemplo = DefIcono(
        "ejemplo",
        listOf("M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z", "M14 3v5h5", "M9 13h6", "M9 17h4"),
    )
    val calculadora = DefIcono(
        "calculadora",
        listOf(
            "M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2H7a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2z",
            "M8 7h8",
            "M8 12h.01", "M12 12h.01", "M16 12h.01", "M8 16h.01", "M12 16h.01", "M16 16h.01",
        ),
    )
    val check = DefIcono("check", listOf("M5 12l5 5 9-10"), grosor = 2.5f)
    val checkCirculo = DefIcono("checkCirculo", listOf(CIRCULO, "M8 12.5l2.8 2.8L16.5 9.5"))
    val errorOctogono = DefIcono("errorOctogono", listOf("M8.2 3h7.6L21 8.2v7.6L15.8 21H8.2L3 15.8V8.2z", "M12 8v5", "M12 16.5h.01"))
    val avisoTriangulo = DefIcono(
        "avisoTriangulo",
        listOf("M10.3 4.1L2.6 17.5A2 2 0 0 0 4.3 20.5h15.4a2 2 0 0 0 1.7-3L13.7 4.1a2 2 0 0 0-3.4 0z", "M12 9.5v4", "M12 17h.01"),
    )
    val quitar = DefIcono("quitar", listOf(CIRCULO, "M8 12h8"))
    val agregar = DefIcono("agregar", listOf("M12 5v14", "M5 12h14"), grosor = 2f)
    val guardar = DefIcono("guardar", listOf("M12 4v11", "M7.5 10.5L12 15l4.5-4.5", "M5 19h14"))
    val hoja = DefIcono("hoja", listOf("M4 5h16v14H4z", "M4 10h16", "M4 14.5h16", "M10 5v14"))
    val codigo = DefIcono("codigo", listOf("M9 8l-4 4 4 4", "M15 8l4 4-4 4"))
    val copiar = DefIcono("copiar", listOf("M9 9h10v10H9z", "M5 15V5h10"))
    val cerrar = DefIcono("cerrar", listOf("M6 6l12 12", "M18 6L6 18"))
    val pegar = DefIcono("pegar", listOf("M9 4h6v3H9z", "M9 5.5H6.5A1.5 1.5 0 0 0 5 7v12.5A1.5 1.5 0 0 0 6.5 21h11a1.5 1.5 0 0 0 1.5-1.5V7a1.5 1.5 0 0 0-1.5-1.5H15"))

    val todos: List<DefIcono> = listOf(
        atras, info, ayuda, chevronDerecha, chevronArriba, editar, ejemplo, calculadora,
        check, checkCirculo, errorOctogono, avisoTriangulo, quitar, agregar, cerrar, pegar,
        guardar, hoja, codigo, copiar,
    )
}

const val GROSOR_ICONO = 1.75f
const val GROSOR_ICONO_BOTON = 1.9f

/** Construye el ImageVector. El color del trazo lo reemplaza el `tint` de [Icon]. */
fun DefIcono.vector(enBoton: Boolean = false): ImageVector {
    val trazo = grosor ?: if (enBoton) GROSOR_ICONO_BOTON else GROSOR_ICONO
    val b = ImageVector.Builder(
        name = nombre + if (enBoton) "_boton" else "",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    for (p in paths) {
        b.addPath(
            pathData = addPathNodes(p),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = trazo,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return b.build()
}

/** Dibuja un [DefIcono]. `descripcion` null = decorativo (TalkBack lo ignora). */
@Composable
fun Icono(
    def: DefIcono,
    descripcion: String?,
    modifier: Modifier = Modifier,
    tamano: Dp = Dimens.icono,
    color: Color = LocalContentColor.current,
    enBoton: Boolean = false,
) {
    val v = remember(def, enBoton) { def.vector(enBoton) }
    Icon(v, contentDescription = descripcion, tint = color, modifier = modifier.size(tamano))
}
