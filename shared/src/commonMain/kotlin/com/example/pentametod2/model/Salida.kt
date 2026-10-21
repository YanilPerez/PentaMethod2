package com.example.pentametod2.model

/**
 * Salida genérica de un método numérico. La interfaz la dibuja sin saber qué método la generó,
 * en tres zonas: fórmulas (arriba); resumen + gráfica + bloques (abajo).
 */
data class SalidaMetodo(
    val metodo: MetodoId,
    val estado: Estado,
    val resumen: List<ItemResumen>,
    val formulas: List<Formula>,
    val grafica: Grafica?,
    val bloques: List<Bloque>,
    val advertencias: List<String>,
    val error: String?,
    /** Datos de entrada ya leídos, para exportar (Octave, Excel). null si el método aún no exporta. */
    val exportable: DatosExportables? = null,
    val comparacionLagrange: ComparacionLagrange? = null,
) {
    companion object {
        fun conError(metodo: MetodoId, mensaje: String) = SalidaMetodo(
            metodo = metodo, estado = Estado.ERROR, resumen = emptyList(), formulas = emptyList(),
            grafica = null, bloques = emptyList(), advertencias = emptyList(), error = mensaje,
        )
    }
}

enum class MetodoId { PUNTO_FIJO, NEWTON_SISTEMAS, REGRESION_POLINOMIAL, REGRESION_MULTIPLE, LAGRANGE, NEWTON_INTERPOLACION }

enum class Estado { OK, ADVERTENCIA, ERROR }

/** `destacado`: el valor clave del ejercicio (predicción, P(x) evaluado); la interfaz lo resalta. */
data class ItemResumen(val etiqueta: String, val valor: String, val destacado: Boolean = false)

/** `general`: la fórmula con símbolos; `sustituida`: la misma con los números del ejercicio. */
data class Formula(val titulo: String, val general: List<String>, val sustituida: List<String>)

// ── Bloques ──

sealed interface Celda {
    /** Número real; la interfaz lo formatea con los `decimales` de la tabla. */
    data class Num(val v: Double) : Celda

    /** Entero (índices i, k): se muestra sin decimales. */
    data class Entero(val v: Int) : Celda

    data class Txt(val s: String) : Celda

    data object Vacia : Celda
}

sealed interface Bloque

enum class TipoTexto { NORMAL, CONCLUSION, ADVERTENCIA }

/** `progreso` (0..1, opcional): p. ej. R² en la conclusión de una regresión, para dibujar una barra. */
data class BloqueTexto(val texto: String, val tipo: TipoTexto = TipoTexto.NORMAL, val progreso: Double? = null) : Bloque

/** Si `filaTotal` es true, la última fila de `filas` es la de totales (Σ). */
data class BloqueTabla(
    val titulo: String,
    val columnas: List<String>,
    val filas: List<List<Celda>>,
    val filaTotal: Boolean = false,
    val decimales: Int = 6,
) : Bloque

/** Matriz o vector (un vector columna es una matriz de n×1). */
data class BloqueMatriz(
    val titulo: String,
    val nombre: String,
    val datos: List<List<Double>>,
    val decimales: Int = 6,
) : Bloque

data class BloqueGrupo(val titulo: String, val bloques: List<Bloque>, val colapsado: Boolean = true) : Bloque

/** Una línea de la franja de resultado de un paso: "Q = 0.905143". `valor` es el número exacto (para exportar). */
data class ResultadoPaso(val etiqueta: String, val texto: String, val valor: Double? = null)

/**
 * Paso desplegable de la explicación: explicación (una frase) → FÓRMULA → CON TUS DATOS (líneas mono, matrices
 * y/o tablas) → franja de resultado. `resumenCorto` se ve en la fila cerrada; `destacado` lo pinta en terracota.
 * Una tabla de `tablas` que también esté en [SalidaMetodo.bloques] no se repite debajo de la lista.
 */
data class BloquePaso(
    val numero: Int,
    val titulo: String,
    val explicacion: String,
    val formulaGeneral: List<String>,
    val conTusDatos: List<String>,
    val matrices: List<BloqueMatriz> = emptyList(),
    val tablas: List<BloqueTabla> = emptyList(),
    val resultados: List<ResultadoPaso>,
    val resumenCorto: String,
    val abiertoInicial: Boolean = false,
    val destacado: Boolean = false,
) : Bloque

// ── Exportación ──

/** Datos ya leídos (los mismos que usó el método) que necesitan los exportadores. */
sealed interface DatosExportables {
    data class RegresionPolinomial(val x: List<Double>, val y: List<Double>, val grado: Int, val xPredecir: Double?) : DatosExportables

    /** `x[i]` = [x₁ᵢ, …, xₖᵢ]. */
    data class RegresionMultiple(val x: List<List<Double>>, val y: List<Double>, val xPredecir: List<Double>?) : DatosExportables

    /** Nodos usados (en automático, los elegidos); `nodosDados` = cuántos escribió el alumno. */
    data class Interpolacion(
        val xs: List<Double>,
        val ys: List<Double>,
        val x: Double,
        val automatico: Boolean = false,
        val nodosDados: Int = xs.size,
    ) : DatosExportables {
        val grado: Int get() = xs.size - 1
    }

    /** `expresiones[i]`: Gᵢ (punto fijo, xᵢ = Gᵢ) o Fᵢ (Newton, Fᵢ = 0), tal como las escribió el alumno. */
    data class Sistema(
        val variables: List<String>,
        val expresiones: List<String>,
        val x0: List<Double>,
        val tolerancia: Double?,
        val maxIter: Int?,
    ) : DatosExportables
}

// ── Gráfica ──

enum class TipoSerie { PUNTOS, LINEA, LINEA_CON_PUNTOS }

data class Punto(val x: Double, val y: Double)

/** `discontinua`: se dibuja con trazo discontinuo (p. ej. la línea de tolerancia). */
data class Serie(
    val nombre: String,
    val tipo: TipoSerie,
    val puntos: List<Punto>,
    val destacada: Boolean = false,
    val discontinua: Boolean = false,
)

data class Grafica(
    val titulo: String,
    val ejeX: String,
    val ejeY: String,
    val series: List<Serie>,
    val escalaLogY: Boolean = false,
)
