package com.example.pentametod2.catalogo

/** Identidad estable: editar o quitar otra fila no cambia los puntos elegidos. */
data class PuntoEntradaLagrange(val id: Int, val x: String = "", val y: String = "")

data class EntradaLagrange(
    val puntos: List<PuntoEntradaLagrange>,
    val x: String,
    val grados: List<Int>,
    val modo: ModoNodos = ModoNodos.AUTOMATICO,
    val selecciones: Map<Int, List<Int>> = emptyMap(),
    val funcion: String? = null,
) : Entrada

/** A…Z, AA…; se conserva también en resultados y exportaciones. */
fun nombrePunto(id: Int): String {
    var n = id + 1
    var nombre = ""
    do {
        n--
        nombre = ('A' + n % 26) + nombre
        n /= 26
    } while (n > 0)
    return nombre
}
