package com.example.pentametod2.ui.formularios

import com.example.pentametod2.presentacion.Notacion
import kotlin.math.max
import kotlin.math.min

/**
 * Reglas puras del formulario de puntos (x, y) de la regresión polinomial: conteo y validación en vivo,
 * grado permitido, conversión Lista ↔ Tabla, "Pegar de Excel" y la vista previa de la fórmula.
 * Mismo criterio que el catálogo: separadores `,` `;` espacios y saltos de línea; punto decimal.
 */

private val SEPARADORES_LISTA = Regex("[,;\\s]+")
private val SEPARADORES_COLUMNA = Regex("[\\t; ]+")
private val NUMERO = Regex("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?")

/** Grado más alto que ofrece el control segmentado (el catálogo admite más; la pantalla no los ofrece). */
const val GRADO_MAXIMO_UI = 5
const val FILAS_MINIMAS_TABLA = 2
const val AYUDA_LISTA = "Separa con comas y usa punto decimal."

/** "1, 2;3\n4," → ["1", "2", "3", "4"] (los vacíos se ignoran). */
fun valoresDeTexto(texto: String): List<String> = texto.split(SEPARADORES_LISTA).filter { it.isNotEmpty() }

fun conteoValores(texto: String): Int = valoresDeTexto(texto).size

fun esNumero(t: String): Boolean = NUMERO.matches(t.trim()) && t.trim().toDoubleOrNull()?.isFinite() == true

/** Posición (desde 1) del primer valor que no es número, o null. */
fun primerTokenInvalido(texto: String): Int? = primerInvalido(valoresDeTexto(texto))

/** Posición (desde 1) del primer valor vacío o no numérico de una lista ya separada, o null. */
fun primerInvalido(valores: List<String>): Int? = valores.indexOfFirst { !esNumero(it) }.takeIf { it >= 0 }?.plus(1)

/** Quita las celdas vacías del final (filas nuevas sin escribir). Las vacías intermedias se conservan. */
fun sinVaciosFinales(valores: List<String>): List<String> = valores.dropLastWhile { it.isBlank() }

fun textoValores(n: Int): String = if (n == 1) "1 valor" else "$n valores"

fun textoPuntos(n: Int): String = if (n == 1) "1 punto" else "$n puntos"

fun mensajeLongitudes(nx: Int, ny: Int): String = "x tiene ${textoValores(nx)} y y tiene $ny. Deben ser iguales."

enum class Columna { X, Y }

sealed interface ValidacionPares {
    data class Ok(val n: Int) : ValidacionPares
    data object Vacio : ValidacionPares
    data class ValorInvalido(val columna: Columna, val posicion: Int) : ValidacionPares
    data class LongitudesDistintas(val nx: Int, val ny: Int) : ValidacionPares
    data class PocosPuntos(val n: Int) : ValidacionPares
}

/** Mensaje para el alumno; null si los datos están bien. */
fun ValidacionPares.mensaje(): String? = when (this) {
    is ValidacionPares.Ok -> null
    ValidacionPares.Vacio -> "Escribe los valores de x y de y."
    is ValidacionPares.ValorInvalido -> "Revisa el valor $posicion de ${if (columna == Columna.X) "x" else "y"}: debe ser un número con punto decimal."
    is ValidacionPares.LongitudesDistintas -> mensajeLongitudes(nx, ny)
    is ValidacionPares.PocosPuntos -> "Se necesitan al menos 2 puntos; hay ${textoPuntos(n)}."
}

/** Validación completa (la que bloquea "Calcular"). Orden: vacío, valor inválido, longitudes, mínimo 2 puntos. */
fun validarParesXY(xs: List<String>, ys: List<String>): ValidacionPares {
    val x = sinVaciosFinales(xs)
    val y = sinVaciosFinales(ys)
    if (x.isEmpty() && y.isEmpty()) return ValidacionPares.Vacio
    primerInvalido(x)?.let { return ValidacionPares.ValorInvalido(Columna.X, it) }
    primerInvalido(y)?.let { return ValidacionPares.ValorInvalido(Columna.Y, it) }
    if (x.size != y.size) return ValidacionPares.LongitudesDistintas(x.size, y.size)
    if (x.size < 2) return ValidacionPares.PocosPuntos(x.size)
    return ValidacionPares.Ok(x.size)
}

/** Pares (x, y) en los que ambos valores son números: es el n con el que se limita el grado. */
fun puntosValidos(xs: List<String>, ys: List<String>): Int {
    val n = min(xs.size, ys.size)
    return (0 until n).count { esNumero(xs[it]) && esNumero(ys[it]) }
}

/**
 * Celda (columna, fila desde 0) que recibe el foco cuando "Calcular" encuentra un error.
 * Con longitudes distintas va a la primera posición que le falta a la lista más corta.
 */
fun objetivoFoco(v: ValidacionPares): Pair<Columna, Int>? = when (v) {
    is ValidacionPares.Ok -> null
    ValidacionPares.Vacio -> Columna.X to 0
    is ValidacionPares.ValorInvalido -> v.columna to v.posicion - 1
    is ValidacionPares.LongitudesDistintas -> if (v.nx < v.ny) Columna.X to v.nx else Columna.Y to v.ny
    is ValidacionPares.PocosPuntos -> Columna.X to v.n
}

// ── Píldoras de conteo ──

enum class TipoPildora { NEUTRA, OK, ERROR }

data class Pildora(val texto: String, val tipo: TipoPildora)

/** Píldora junto a la etiqueta "Valores x" / "Valores y" (modo Lista). */
fun pildoraValores(valores: List<String>, otros: List<String>): Pildora {
    primerInvalido(valores)?.let { return Pildora("Revisa el valor $it", TipoPildora.ERROR) }
    if (valores.isEmpty()) return Pildora(textoValores(0), TipoPildora.NEUTRA)
    val distinta = otros.isNotEmpty() && otros.size != valores.size
    return Pildora(textoValores(valores.size), if (distinta) TipoPildora.ERROR else TipoPildora.OK)
}

/** Mensaje bajo los campos cuando ambas listas tienen valores pero distinta cantidad; si no, null. */
fun avisoLongitudes(xs: List<String>, ys: List<String>): String? {
    val x = sinVaciosFinales(xs)
    val y = sinVaciosFinales(ys)
    return if (x.isNotEmpty() && y.isNotEmpty() && x.size != y.size) mensajeLongitudes(x.size, y.size) else null
}

/** Píldora "✓ N puntos" sobre la tabla. */
fun pildoraTabla(xs: List<String>, ys: List<String>): Pildora = when (val v = validarParesXY(xs, ys)) {
    is ValidacionPares.Ok -> Pildora(textoPuntos(v.n), TipoPildora.OK)
    ValidacionPares.Vacio -> Pildora(textoPuntos(0), TipoPildora.NEUTRA)
    is ValidacionPares.PocosPuntos -> Pildora(textoPuntos(v.n), TipoPildora.NEUTRA)
    is ValidacionPares.ValorInvalido -> Pildora("Revisa la fila ${v.posicion}", TipoPildora.ERROR)
    is ValidacionPares.LongitudesDistintas -> Pildora("Revisa la fila ${min(v.nx, v.ny) + 1}", TipoPildora.ERROR)
}

// ── Grado ──

/** min(5, n − 1); 0 si hay menos de 2 puntos. */
fun gradoMaximo(n: Int): Int = if (n < 2) 0 else min(GRADO_MAXIMO_UI, n - 1)

/**
 * Grado que se usa con n puntos válidos. Sin elegir (null): 2 si se puede, si no 1.
 * Elegido: se baja al máximo permitido si hace falta (nunca menos de 1).
 */
fun ajustarGrado(gradoActual: Int?, n: Int): Int {
    val tope = max(gradoMaximo(n), 1)
    return if (gradoActual == null) min(2, tope) else gradoActual.coerceIn(1, tope)
}

fun textoGradoMaximo(n: Int): String =
    if (n < 2) "Escribe al menos 2 puntos para elegir el grado." else "Grado máximo permitido con $n puntos: ${gradoMaximo(n)}."

/** grado 2 → "y = a₀ + a₁x + a₂x²". */
fun formulaVistaPrevia(grado: Int): String {
    val g = grado.coerceAtLeast(1)
    return "y = " + (0..g).joinToString(" + ") { k ->
        "a${Notacion.sub(k)}" + when (k) {
            0 -> ""
            else -> "x" + Notacion.potencia(k)
        }
    }
}

// ── Lista ↔ Tabla ──

/** Rellena con celdas vacías hasta igualar longitudes (y al menos [minimo] filas). */
fun listaATabla(xs: List<String>, ys: List<String>, minimo: Int = FILAS_MINIMAS_TABLA): Pair<List<String>, List<String>> {
    val filas = maxOf(xs.size, ys.size, minimo)
    return rellenar(xs, filas) to rellenar(ys, filas)
}

/** Columna de la tabla → texto del campo de lista: ["1", "", " 2 "] → "1, 2". */
fun tablaALista(valores: List<String>): String = valores.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

fun rellenar(valores: List<String>, filas: Int): List<String> = valores + List((filas - valores.size).coerceAtLeast(0)) { "" }

// ── Pegar de Excel ──

sealed interface ResultadoPegado {
    data class Ok(val xs: List<String>, val ys: List<String>) : ResultadoPegado
    data class Error(val mensaje: String) : ResultadoPegado
}

/**
 * Filas separadas por salto de línea, columnas por tabulación, espacios o ';'. Se ignoran las filas vacías y,
 * si la primera fila no tiene ningún número, se toma como encabezado ("x  y"). Se usan las dos primeras columnas.
 */
fun parsearPegado(texto: String): ResultadoPegado {
    val filas = texto.lines().withIndex()
        .map { (i, l) -> i + 1 to l.trim().split(SEPARADORES_COLUMNA).filter { it.isNotEmpty() } }
        .filter { it.second.isNotEmpty() }
    if (filas.isEmpty()) return ResultadoPegado.Error("No hay datos para pegar: copia dos columnas (x e y) de Excel.")
    val datos = if (filas.first().second.none { esNumero(it) }) filas.drop(1) else filas
    if (datos.isEmpty()) return ResultadoPegado.Error("Solo se pegó el encabezado; copia también los valores.")
    val corta = datos.firstOrNull { it.second.size < 2 }
    if (corta != null) return ResultadoPegado.Error("La fila ${corta.first} tiene una sola columna; se esperan x e y.")
    return ResultadoPegado.Ok(datos.map { it.second[0] }, datos.map { it.second[1] })
}
