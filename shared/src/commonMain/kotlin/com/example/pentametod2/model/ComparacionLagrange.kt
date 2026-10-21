package com.example.pentametod2.model

data class PuntoDatoLagrange(val id: Int, val x: Double, val y: Double)

data class GradoLagrange(
    val grado: Int,
    val puntos: List<PuntoDatoLagrange>,
    val valor: Double,
    val errorAbsoluto: Double?,
    val errorRelativoPorcentaje: Double?,
    val extrapolacion: Boolean,
    val detalle: SalidaMetodo,
)

/** El detalle de cada grado es una salida normal, sin comparaciones anidadas. */
data class ComparacionLagrange(
    val puntos: List<PuntoDatoLagrange>,
    val x: Double,
    val resultados: List<GradoLagrange>,
    val funcion: String?,
    val referencia: Double?,
)
