package com.example.pentametod2.ui.resultados

import com.example.pentametod2.model.Bloque
import com.example.pentametod2.model.BloqueGrupo
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Formato
import com.example.pentametod2.model.SalidaMetodo
import kotlin.math.abs

/** Máximo de filas que se dibujan por tabla. */
const val MAX_FILAS_TABLA = 200

/**
 * Texto de una celda: Num con `decimales` fijos (científica si 0 < |v| < 1e-4 o |v| ≥ 1e9),
 * Entero sin decimales, Txt tal cual, Vacia vacía.
 */
fun textoCelda(celda: Celda, decimales: Int): String = when (celda) {
    is Celda.Num -> textoNumero(celda.v, decimales)
    is Celda.Entero -> celda.v.toString()
    is Celda.Txt -> celda.s
    Celda.Vacia -> ""
}

fun textoNumero(v: Double, decimales: Int): String {
    val a = abs(v)
    return if (v.isFinite() && ((a > 0 && a < 1e-4) || a >= 1e9)) Formato.auto(v, decimales) else Formato.fijo(v, decimales)
}

/** Ancho de cada columna en caracteres: el máximo entre el encabezado y sus celdas. */
fun anchosColumnas(columnas: List<String>, filas: List<List<String>>): List<Int> =
    columnas.indices.map { j ->
        var m = columnas[j].length
        for (f in filas) m = maxOf(m, f.getOrNull(j)?.length ?: 0)
        m
    }

/** Filas a dibujar (las primeras [max], más la fila total si la hay) y cuántas quedaron ocultas. */
fun filasVisibles(tabla: BloqueTabla, max: Int = MAX_FILAS_TABLA): Pair<List<List<Celda>>, Int> {
    val datos = if (tabla.filaTotal) tabla.filas.dropLast(1) else tabla.filas
    if (datos.size <= max) return tabla.filas to 0
    val visibles = datos.take(max) + (if (tabla.filaTotal) listOf(tabla.filas.last()) else emptyList())
    return visibles to (datos.size - max)
}

/** Resumen y tablas como texto separado por tabulaciones (para pegar en Excel). */
fun textoParaCopiar(salida: SalidaMetodo): String {
    val sb = StringBuilder()
    salida.comparacionLagrange?.let { c ->
        c.funcion?.let { sb.append("Función original\t").append(it).append('\n') }
        sb.append("Datos ingresados\nPunto\tX\tY\n")
        c.puntos.forEach { sb.append(com.example.pentametod2.catalogo.nombrePunto(it.id)).append('\t').append(it.x).append('\t').append(it.y).append('\n') }
    }
    salida.error?.let { sb.append("Error\t").append(it).append('\n') }
    for (a in salida.advertencias) sb.append("Advertencia\t").append(a).append('\n')
    for (r in salida.resumen) sb.append(r.etiqueta).append('\t').append(r.valor).append('\n')
    for (t in tablas(salida.bloques)) {
        sb.append('\n').append(t.titulo).append('\n')
        sb.append(t.columnas.joinToString("\t")).append('\n')
        for (f in t.filas) sb.append(f.joinToString("\t") { textoCelda(it, t.decimales) }).append('\n')
    }
    salida.comparacionLagrange?.resultados?.forEach { r ->
        sb.append("\nGrado ").append(r.grado).append('\n')
        for (p in r.detalle.bloques.filterIsInstance<com.example.pentametod2.model.BloquePaso>()) {
            sb.append(p.numero).append(". ").append(p.titulo).append('\n')
            p.conTusDatos.forEach { sb.append(it).append('\n') }
        }
        sb.append(textoParaCopiar(r.detalle)).append('\n')
    }
    return sb.toString().trimEnd('\n')
}

/** Todas las tablas, incluidas las que están dentro de grupos. */
private fun tablas(bloques: List<Bloque>): List<BloqueTabla> = bloques.flatMap { tablasDe(it) }

private fun tablasDe(b: Bloque): List<BloqueTabla> = when (b) {
    is BloqueTabla -> listOf(b)
    is BloqueGrupo -> tablas(b.bloques)
    else -> emptyList()
}
