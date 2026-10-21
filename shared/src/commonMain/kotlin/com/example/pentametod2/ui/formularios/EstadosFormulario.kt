package com.example.pentametod2.ui.formularios

import androidx.compose.runtime.saveable.Saver
import com.example.pentametod2.catalogo.Entrada
import com.example.pentametod2.catalogo.EntradaInterpolacion
import com.example.pentametod2.catalogo.EntradaLagrange
import com.example.pentametod2.catalogo.EntradaRegresionMultiple
import com.example.pentametod2.catalogo.EntradaRegresionPolinomial
import com.example.pentametod2.catalogo.EntradaSistema
import com.example.pentametod2.catalogo.ModoNodos
import com.example.pentametod2.model.MetodoId

/**
 * Estado de cada formulario: solo textos, tal como los escribe el alumno.
 * Todas las conversiones son funciones puras (testeadas en commonTest).
 */
sealed interface EstadoFormulario {
    fun aEntrada(): Entrada
}

// ── Sistemas ──

const val MIN_ECUACIONES = 2
const val MAX_ECUACIONES = 4
private val NOMBRES_POR_DEFECTO = listOf("x", "y", "z", "w")

/** "x, y" para 2 ecuaciones, "x, y, z" para 3, "x, y, z, w" para 4. */
fun variablesPorDefecto(n: Int): String = NOMBRES_POR_DEFECTO.take(n.coerceIn(1, MAX_ECUACIONES)).joinToString(", ")

data class EstadoSistema(
    val ecuaciones: List<String> = List(MIN_ECUACIONES) { "" },
    val variables: String = variablesPorDefecto(MIN_ECUACIONES),
    val x0: String = "",
    val tolerancia: String = "1e-6",
    val maxIter: String = "50",
) : EstadoFormulario {
    override fun aEntrada(): Entrada = EntradaSistema(ecuaciones, variables, x0, tolerancia, maxIter)

    fun conEcuacion(i: Int, texto: String): EstadoSistema =
        copy(ecuaciones = ecuaciones.mapIndexed { j, e -> if (j == i) texto else e })

    /** Agrega una ecuación (máx. 4). Si las variables eran las de por defecto, las ajusta. */
    fun agregarEcuacion(): EstadoSistema {
        if (ecuaciones.size >= MAX_ECUACIONES) return this
        return redimensionar(ecuaciones + "")
    }

    /** Quita la última ecuación (mín. 2). */
    fun quitarEcuacion(): EstadoSistema {
        if (ecuaciones.size <= MIN_ECUACIONES) return this
        return redimensionar(ecuaciones.dropLast(1))
    }

    /** Quita la ecuación `i` (mín. 2). */
    fun quitarEcuacion(i: Int): EstadoSistema {
        if (ecuaciones.size <= MIN_ECUACIONES || i !in ecuaciones.indices) return this
        return redimensionar(ecuaciones.filterIndexed { j, _ -> j != i })
    }

    private fun redimensionar(nuevas: List<String>): EstadoSistema {
        val vars = if (normalizar(variables) == normalizar(variablesPorDefecto(ecuaciones.size))) variablesPorDefecto(nuevas.size) else variables
        return copy(ecuaciones = nuevas, variables = vars)
    }

    /** Nombres para las etiquetas; si el campo no tiene tantos nombres válidos, usa x, y, z, w. */
    fun nombresVariables(): List<String> {
        val dados = variables.split(',', ';', ' ', '\t', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        return if (dados.size == ecuaciones.size) dados else NOMBRES_POR_DEFECTO.take(ecuaciones.size)
    }

    private fun normalizar(s: String) = s.filter { !it.isWhitespace() }
}

// ── Interpolación ──

data class EstadoInterpolacion(
    val xs: String = "",
    val ys: String = "",
    val x: String = "",
    val modo: ModoNodos = ModoNodos.MANUAL,
    val grado: String = "",
) : EstadoFormulario {
    override fun aEntrada(): Entrada = EntradaInterpolacion(xs, ys, x, modo, grado)
}

// ── Regresión polinomial ──

enum class VistaDatos { LISTA, TABLA }

/**
 * Fuente de verdad única de los puntos: listas de textos x e y (en Lista, los valores ya separados; en Tabla,
 * las celdas, que pueden estar vacías). `grado` vacío = aún no elegido (se usa [ajustarGrado]).
 */
data class EstadoRegresionPolinomial(
    val xs: List<String> = emptyList(),
    val ys: List<String> = emptyList(),
    val grado: String = "",
    val xPredecir: String = "",
    val vista: VistaDatos = VistaDatos.LISTA,
) : EstadoFormulario {
    /** Desde los textos de las listas ("1, 2, 3"). */
    constructor(xs: String, ys: String, grado: String, xPredecir: String) :
        this(valoresDeTexto(xs), valoresDeTexto(ys), grado, xPredecir)

    override fun aEntrada(): Entrada =
        EntradaRegresionPolinomial(unir(xs), unir(ys), gradoEfectivo().toString(), xPredecir)

    private fun unir(l: List<String>) = l.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")

    fun puntos(): Int = puntosValidos(xs, ys)

    fun gradoEfectivo(): Int = ajustarGrado(grado.trim().toIntOrNull(), puntos())

    fun validacion(): ValidacionPares = validarParesXY(xs, ys)

    /** Si el grado elegido ya no cabe (se quitaron puntos), baja al máximo permitido. */
    fun conGradoAjustado(): EstadoRegresionPolinomial {
        val elegido = grado.trim().toIntOrNull() ?: return this
        val ajustado = ajustarGrado(elegido, puntos())
        return if (ajustado == elegido) this else copy(grado = ajustado.toString())
    }

    /** Lista → Tabla rellena con celdas vacías; Tabla → Lista descarta las vacías. */
    fun conVista(v: VistaDatos): EstadoRegresionPolinomial {
        if (v == vista) return this
        return when (v) {
            VistaDatos.TABLA -> listaATabla(xs, ys).let { (x, y) -> copy(xs = x, ys = y, vista = v) }
            VistaDatos.LISTA -> copy(xs = valoresDeTexto(tablaALista(xs)), ys = valoresDeTexto(tablaALista(ys)), vista = v)
        }
    }

    fun conCelda(fila: Int, columna: Columna, texto: String): EstadoRegresionPolinomial {
        val (x, y) = listaATabla(xs, ys, fila + 1)
        return if (columna == Columna.X) copy(xs = x.cambiar(fila, texto), ys = y) else copy(xs = x, ys = y.cambiar(fila, texto))
    }

    fun agregarPunto(): EstadoRegresionPolinomial {
        val (x, y) = listaATabla(xs, ys, 0)
        return copy(xs = x + "", ys = y + "")
    }

    /** Quita la fila `i` (queda al menos una). */
    fun quitarPunto(i: Int): EstadoRegresionPolinomial {
        val (x, y) = listaATabla(xs, ys, 0)
        if (x.size <= 1 || i !in x.indices) return this
        return copy(xs = x.filterIndexed { j, _ -> j != i }, ys = y.filterIndexed { j, _ -> j != i })
    }

    private fun List<String>.cambiar(i: Int, t: String) = mapIndexed { j, v -> if (j == i) t else v }
}

// ── Regresión múltiple ──

const val MIN_FILAS = 3
const val MIN_VARIABLES = 1
const val MAX_VARIABLES = 4

/** Cuadrícula de datos: cada fila tiene k celdas x más una celda y (la última). */
data class EstadoRegresionMultiple(
    val numVariables: Int = 2,
    val filas: List<List<String>> = List(MIN_FILAS) { List(2 + 1) { "" } },
    val xPredecir: String = "",
) : EstadoFormulario {
    override fun aEntrada(): Entrada = EntradaRegresionMultiple(filas, xPredecir)

    fun conCelda(fila: Int, columna: Int, texto: String): EstadoRegresionMultiple =
        copy(filas = filas.mapIndexed { i, f -> if (i == fila) f.mapIndexed { j, c -> if (j == columna) texto else c } else f })

    fun agregarFila(): EstadoRegresionMultiple = copy(filas = filas + listOf(List(numVariables + 1) { "" }))

    /** Quita la fila `i` si quedan más de [MIN_FILAS]. */
    fun quitarFila(i: Int): EstadoRegresionMultiple {
        if (filas.size <= MIN_FILAS || i !in filas.indices) return this
        return copy(filas = filas.filterIndexed { j, _ -> j != i })
    }

    /** Cambia k conservando las x existentes (recorta o agrega columnas vacías) y la y al final. */
    fun conNumVariables(k: Int): EstadoRegresionMultiple {
        val nuevoK = k.coerceIn(MIN_VARIABLES, MAX_VARIABLES)
        if (nuevoK == numVariables) return this
        val nuevas = filas.map { f ->
            val xs = f.dropLast(1)
            val y = f.lastOrNull() ?: ""
            xs.take(nuevoK) + List((nuevoK - xs.size).coerceAtLeast(0)) { "" } + y
        }
        return copy(numVariables = nuevoK, filas = nuevas)
    }
}

// ── Construcción y conversión ──

/** Formulario vacío para cada método. */
fun estadoInicial(id: MetodoId): EstadoFormulario = when (id) {
    MetodoId.PUNTO_FIJO, MetodoId.NEWTON_SISTEMAS -> EstadoSistema()
    MetodoId.LAGRANGE -> EstadoLagrange()
    MetodoId.NEWTON_INTERPOLACION -> EstadoInterpolacion()
    MetodoId.REGRESION_POLINOMIAL -> EstadoRegresionPolinomial()
    MetodoId.REGRESION_MULTIPLE -> EstadoRegresionMultiple()
}

/** Entrada → estado del formulario (para "Cargar ejemplo"). */
fun estadoDesdeEntrada(e: Entrada): EstadoFormulario = when (e) {
    is EntradaLagrange -> EstadoLagrange(e.puntos, e.x, e.grados, e.modo, e.selecciones, e.funcion != null, e.funcion.orEmpty())
    is EntradaSistema -> EstadoSistema(e.ecuaciones, e.variables, e.x0, e.tolerancia, e.maxIter)
    is EntradaInterpolacion -> EstadoInterpolacion(e.xs, e.ys, e.x, e.modo, e.grado)
    is EntradaRegresionPolinomial -> EstadoRegresionPolinomial(e.xs, e.ys, e.grado, e.xPredecir)
    is EntradaRegresionMultiple -> multipleDesdeEntrada(e)
}

private fun multipleDesdeEntrada(e: EntradaRegresionMultiple): EstadoRegresionMultiple {
    val columnas = e.filas.maxOfOrNull { it.size } ?: 3
    val k = (columnas - 1).coerceIn(MIN_VARIABLES, MAX_VARIABLES)
    val filas = e.filas + List((MIN_FILAS - e.filas.size).coerceAtLeast(0)) { List(k + 1) { "" } }
    return EstadoRegresionMultiple(k, filas, e.xPredecir)
}

// ── Guardado (rememberSaveable): todo se convierte en una lista de textos ──

private const val TAG_SISTEMA = "S"
private const val TAG_INTERPOLACION = "I"
private const val TAG_POLINOMIAL = "P"
private const val TAG_MULTIPLE = "M"

fun EstadoFormulario.aLista(): List<String> = when (this) {
    is EstadoLagrange -> guardar()
    is EstadoSistema -> listOf(TAG_SISTEMA, variables, x0, tolerancia, maxIter) + ecuaciones
    is EstadoInterpolacion -> listOf(TAG_INTERPOLACION, xs, ys, x, modo.name, grado)
    is EstadoRegresionPolinomial -> listOf(TAG_POLINOMIAL, grado, xPredecir, vista.name, xs.size.toString()) + xs + ys
    is EstadoRegresionMultiple -> listOf(TAG_MULTIPLE, numVariables.toString(), xPredecir) + filas.flatten()
}

/** Inversa de [aLista]; null si la lista no es válida. */
fun estadoDesdeLista(l: List<String>): EstadoFormulario? {
    if (l.isEmpty()) return null
    return when (l[0]) {
        "L1", "L2", "L3" -> EstadoLagrange.restaurar(l)
        TAG_SISTEMA -> if (l.size < 5) null else EstadoSistema(l.drop(5), l[1], l[2], l[3], l[4])
        TAG_INTERPOLACION -> if (l.size != 6) null else EstadoInterpolacion(l[1], l[2], l[3], modoDesde(l[4]), l[5])
        TAG_POLINOMIAL -> polinomialDesdeLista(l)
        TAG_MULTIPLE -> multipleDesdeLista(l)
        else -> null
    }
}

private fun modoDesde(s: String): ModoNodos = ModoNodos.entries.firstOrNull { it.name == s } ?: ModoNodos.MANUAL

private fun polinomialDesdeLista(l: List<String>): EstadoRegresionPolinomial? {
    val nx = l.getOrNull(4)?.toIntOrNull() ?: return null
    if (nx < 0 || l.size < 5 + nx) return null
    val vista = VistaDatos.entries.firstOrNull { it.name == l[3] } ?: VistaDatos.LISTA
    return EstadoRegresionPolinomial(l.subList(5, 5 + nx).toList(), l.drop(5 + nx), l[1], l[2], vista)
}

/** "Cargar ejemplo" conserva la vista (Lista o Tabla) que tenía el alumno. */
fun EstadoFormulario.conVistaDe(anterior: EstadoFormulario): EstadoFormulario = when {
    this is EstadoRegresionPolinomial && anterior is EstadoRegresionPolinomial -> conVista(anterior.vista)
    this is EstadoLagrange && anterior is EstadoLagrange ->
        conVista(anterior.vista).copy(modo = ModoNodos.AUTOMATICO, selecciones = emptyMap(), gradoPredeterminado = true)
            .conGradoPredeterminado()
    else -> this
}

private fun multipleDesdeLista(l: List<String>): EstadoRegresionMultiple? {
    val k = l.getOrNull(1)?.toIntOrNull() ?: return null
    if (l.size < 3) return null
    val celdas = l.drop(3)
    if (celdas.size % (k + 1) != 0) return null
    return EstadoRegresionMultiple(k, celdas.chunked(k + 1), l[2])
}

val FormularioSaver: Saver<EstadoFormulario, Any> = Saver(
    save = { ArrayList(it.aLista()) },
    restore = { restaurarFormulario(it) },
)

/** Para el formulario "calculado" (null antes del primer cálculo). */
val FormularioOpcionalSaver: Saver<EstadoFormulario?, Any> = Saver(
    save = { it?.let { e -> ArrayList(e.aLista()) } },
    restore = { restaurarFormulario(it) },
)

@Suppress("UNCHECKED_CAST")
private fun restaurarFormulario(guardado: Any): EstadoFormulario? = (guardado as? List<String>)?.let { estadoDesdeLista(it) }
