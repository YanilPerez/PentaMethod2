package com.example.pentametod2.catalogo

import com.example.pentametod2.model.MetodoId

/** Datos tal como los escribe el alumno en el formulario (todo texto crudo). */
sealed interface Entrada

/** Para punto fijo `ecuaciones` son las Gᵢ (xᵢ = Gᵢ); para Newton son las Fᵢ (Fᵢ = 0). */
data class EntradaSistema(
    val ecuaciones: List<String>,
    val variables: String,
    val x0: String,
    val tolerancia: String = "",
    val maxIter: String = "",
) : Entrada

enum class ModoNodos { MANUAL, AUTOMATICO }

/** En modo MANUAL se usan exactamente los nodos dados (se ignora `grado`); en AUTOMATICO, los grado+1 más cercanos a x. */
data class EntradaInterpolacion(
    val xs: String,
    val ys: String,
    val x: String,
    val modo: ModoNodos = ModoNodos.MANUAL,
    val grado: String = "",
) : Entrada

/** `xPredecir` puede estar vacío. */
data class EntradaRegresionPolinomial(
    val xs: String,
    val ys: String,
    val grado: String,
    val xPredecir: String = "",
) : Entrada

/** Cada fila = x1, …, xk, y. `xPredecir` vacío o "x1, x2, …". */
data class EntradaRegresionMultiple(
    val filas: List<List<String>>,
    val xPredecir: String = "",
) : Entrada

/**
 * `semana` es interna (orden del curso) y nunca se muestra; la interfaz agrupa por `seccion`.
 */
data class MetodoInfo(
    val id: MetodoId,
    val nombre: String,
    val semana: Int,
    val descripcion: String,
    val seccion: String,
)

/** Secciones del menú, en orden. */
object Secciones {
    const val SISTEMAS = "Sistemas no lineales"
    const val AJUSTE = "Ajuste de curvas"
    const val INTERPOLACION = "Interpolación"
    val orden = listOf(SISTEMAS, AJUSTE, INTERPOLACION)
}
