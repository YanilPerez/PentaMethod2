package com.example.pentametod2.ui.resultados

import com.example.pentametod2.model.Bloque
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.model.TipoTexto
import com.example.pentametod2.ui.componentes.TipoChip

/** Cómo se dibuja cada bloque: las matrices consecutivas van juntas en una sola tarjeta. */
sealed interface ElementoVista {
    data class Simple(val bloque: Bloque) : ElementoVista
    data class Matrices(val matrices: List<BloqueMatriz>) : ElementoVista
}

fun agruparBloques(bloques: List<Bloque>): List<ElementoVista> {
    val r = mutableListOf<ElementoVista>()
    var pendientes = mutableListOf<BloqueMatriz>()
    for (b in bloques) {
        if (b is BloqueMatriz) {
            pendientes += b
            continue
        }
        if (pendientes.isNotEmpty()) {
            r += ElementoVista.Matrices(pendientes)
            pendientes = mutableListOf()
        }
        r += ElementoVista.Simple(b)
    }
    if (pendientes.isNotEmpty()) r += ElementoVista.Matrices(pendientes)
    return r
}

/** Métodos iterativos (pueden "no converger"). */
fun esIterativo(id: MetodoId): Boolean = id == MetodoId.PUNTO_FIJO || id == MetodoId.NEWTON_SISTEMAS

/** Error de validación de la entrada (no hay nada calculado) frente a un fallo del método. */
fun esErrorDeEntrada(s: SalidaMetodo): Boolean = s.estado == Estado.ERROR && s.bloques.isEmpty() && s.resumen.isEmpty()

/** Chip del separador "RESULTADOS". Un error de entrada nunca se describe como "No convergió". */
fun chipResultados(s: SalidaMetodo): Pair<String, TipoChip> = when (s.estado) {
    Estado.OK -> "Calculado" to TipoChip.OK
    Estado.ADVERTENCIA -> "Con avisos" to TipoChip.AVISO
    Estado.ERROR -> (if (esIterativo(s.metodo) && !esErrorDeEntrada(s)) "No convergió" else "Con errores") to TipoChip.ERROR
}

enum class AccionError { CAMBIAR_DESPEJE, PROBAR_NEWTON, EDITAR_DATOS }

/** Título del banner de error y sus acciones. */
fun bannerError(s: SalidaMetodo): Pair<String, List<AccionError>> {
    val fallaDelMetodo = esIterativo(s.metodo) && !esErrorDeEntrada(s)
    val titulo = if (fallaDelMetodo) "El método diverge o no convergió" else "No se pudo calcular"
    val acciones = if (s.metodo == MetodoId.PUNTO_FIJO && fallaDelMetodo) {
        listOf(AccionError.CAMBIAR_DESPEJE, AccionError.PROBAR_NEWTON)
    } else {
        listOf(AccionError.EDITAR_DATOS)
    }
    return titulo to acciones
}

fun textoAccion(a: AccionError): String = when (a) {
    AccionError.CAMBIAR_DESPEJE -> "Cambiar despeje"
    AccionError.PROBAR_NEWTON -> "Probar Newton-Raphson"
    AccionError.EDITAR_DATOS -> "Editar datos"
}

/** Banner de avisos: título = primer aviso; mensaje = el resto. */
fun bannerAvisos(advertencias: List<String>): Pair<String, String?>? {
    if (advertencias.isEmpty()) return null
    val resto = advertencias.drop(1)
    return advertencias.first() to (if (resto.isEmpty()) null else resto.joinToString("\n"))
}

/** "Sistema normal: C·a = b" y similares (con dos puntos) van en onSurfaceVariant. */
fun esLineaSecundaria(linea: String): Boolean = linea.contains(": ")

/** Texto del chip de la fila "Convergió". */
fun chipConvergencia(valor: String, s: SalidaMetodo): Pair<String, TipoChip> =
    if (valor == "Sí") {
        "Convergió" to TipoChip.OK
    } else {
        (if (s.error?.contains("diverge") == true) "Divergente" else "No convergió") to TipoChip.ERROR
    }

/** El bloque de texto que repite el error del banner no se vuelve a mostrar. */
fun ocultarBloque(b: Bloque, s: SalidaMetodo): Boolean =
    b is BloqueTexto && b.tipo == TipoTexto.ADVERTENCIA && s.estado == Estado.ERROR && b.texto == s.error

/** Tabla de iteraciones de un método que no convergió: se marca la última fila. */
fun marcarUltimaFila(t: BloqueTabla, s: SalidaMetodo): Boolean =
    s.estado == Estado.ERROR && esIterativo(s.metodo) && t.titulo == "Iteraciones" && t.filas.isNotEmpty()

/** Columnas de error (se pintan en rojo en la última fila): "E" y las normas de F. */
fun esColumnaError(nombre: String): Boolean = nombre == "E" || nombre.startsWith("‖F")

/** Alto de fila: 36 en tablas de iteraciones, 40 en el resto. */
fun esTablaIteraciones(t: BloqueTabla): Boolean = t.titulo == "Iteraciones"

/** Para el vector de coeficientes "a" (o "β" en la regresión múltiple): rótulos "← a₀", "← a₁", … */
fun rotulosCoeficientes(m: BloqueMatriz): List<String>? {
    if ((m.nombre != "a" && m.nombre != "β") || m.datos.any { it.size != 1 }) return null
    return m.datos.indices.map { "← ${m.nombre}" + subindiceTexto(it) }
}

private fun subindiceTexto(n: Int): String = n.toString().map { "₀₁₂₃₄₅₆₇₈₉"[it - '0'] }.joinToString("")

/** Título de la tarjeta de matrices: en regresión, "Sistema normal" + "C·a = b". */
fun tituloMatrices(matrices: List<BloqueMatriz>): Pair<String, String?> {
    val nombres = matrices.map { it.nombre }
    if (nombres.containsAll(listOf("C", "b"))) return "Sistema normal" to "C·a = b"
    return matrices.first().titulo to null
}

/** Partes de un texto de conclusión con los porcentajes marcados (para ponerlos en negrita). */
fun partesConPorcentajes(texto: String): List<Pair<String, Boolean>> {
    val r = mutableListOf<Pair<String, Boolean>>()
    var i = 0
    for (m in Regex("\\d+(\\.\\d+)?%").findAll(texto)) {
        if (m.range.first > i) r += texto.substring(i, m.range.first) to false
        r += m.value to true
        i = m.range.last + 1
    }
    if (i < texto.length) r += texto.substring(i) to false
    return r
}

// ── Pasos desplegables ──

/** Pasos de la salida (vacío si el método aún usa el formato anterior). */
fun pasosDe(s: SalidaMetodo): List<BloquePaso> = s.bloques.filterIsInstance<BloquePaso>()

/** Con pasos, la pantalla omite la tarjeta de fórmulas y la de resumen: su contenido ya está en los pasos y la conclusión. */
fun usaPasos(s: SalidaMetodo): Boolean = pasosDe(s).isNotEmpty()

/** Una tabla que ya se muestra dentro de un paso no se repite debajo de la lista. */
fun yaEnUnPaso(b: Bloque, s: SalidaMetodo): Boolean = b is BloqueTabla && pasosDe(s).any { p -> p.tablas.any { it === b } }

/** Pasos abiertos, guardados como texto ("1,3") para rememberSaveable. */
fun pasosIniciales(pasos: List<BloquePaso>): String = pasos.filter { it.abiertoInicial }.joinToString(",") { it.numero.toString() }

fun pasosAbiertos(guardado: String): Set<Int> = guardado.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

/** Abre o cierra el paso `numero` sin tocar los demás. */
fun alternarPaso(guardado: String, numero: Int): String {
    val abiertos = pasosAbiertos(guardado)
    val nuevos = if (numero in abiertos) abiertos - numero else abiertos + numero
    return nuevos.sorted().joinToString(",")
}

/** Aviso bajo una tabla desplazable: con pasos se nombra el total ("Desliza para ver las 11 columnas →"). */
fun textoDesliza(columnas: Int, conPasos: Boolean): String =
    if (conPasos) "Desliza para ver las $columnas columnas →" else "Desliza para ver más columnas →"
