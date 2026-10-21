package com.example.pentametod2.presentacion

import com.example.pentametod2.catalogo.nombrePunto
import com.example.pentametod2.methods.Muestreo
import com.example.pentametod2.methods.ResultadoLagrange
import com.example.pentametod2.model.*

object PresentadorComparacionLagrange {
    fun resultado(r: ResultadoLagrange, puntos: List<PuntoDatoLagrange>, automatico: Boolean, dados: Int,
                  absoluto: Double?, relativo: Double?, referencia: Double?): GradoLagrange {
        val original = PresentadorInterpolacion.presentarLagrange(r, automatico, dados)
        val ids = puntos.joinToString { nombrePunto(it.id) }
        val expresion = puntos.indices.joinToString(" + ") { i ->
            val otros = puntos.indices.filter { it != i }
            "(${Formato.corto(puntos[i].y)})·[" + otros.joinToString("·") { j ->
                "(x − (${Formato.corto(puntos[j].x)}))"
            } + "]/[(" + otros.joinToString(")·(") { j ->
                "${Formato.corto(puntos[i].x)} − (${Formato.corto(puntos[j].x)})"
            } + ")]"
        }
        val bloques = original.bloques.map { b ->
            when {
                b is BloquePaso && b.numero == 1 -> b.copy(
                    explicacion = "Puntos utilizados: $ids. Para grado ${r.grado} se utilizan ${puntos.size} puntos de X distintos.",
                    formulaGeneral = listOf("Con n puntos de X distintos, el polinomio tiene grado máximo n − 1."),
                    conTusDatos = puntos.map { "${nombrePunto(it.id)}: (${Formato.corto(it.x)}, ${Formato.corto(it.y)})" } +
                        "x a evaluar = ${Formato.corto(r.x)}" +
                        if (automatico) listOf("Criterio: menor distancia a x; en un empate, menor X.") else listOf("Selección manual: se usaron exactamente estos puntos."),
                )
                b is BloquePaso && b.numero == 2 -> b.copy(
                    conTusDatos = listOf("P${r.grado}(x) = $expresion"),
                    explicacion = b.explicacion + " El grado efectivo puede ser menor si los datos siguen un polinomio más simple.",
                )
                b is BloqueTexto && b.tipo == TipoTexto.CONCLUSION -> b.copy(
                    texto = "Con $ids se obtiene P${r.grado}(${Formato.corto(r.x)}) = ${Formato.auto(r.valor)}.",
                )
                else -> b
            }
        }.toMutableList()
        if (referencia != null) bloques += BloquePaso(
            numero = 5, titulo = "Comparación con la función original",
            explicacion = "Los errores se calculan con los valores completos; solo se redondea su presentación.",
            formulaGeneral = listOf("E absoluto = |f(x) − P(x)|", "E relativo % = E absoluto / |f(x)| × 100, si f(x) ≠ 0"),
            conTusDatos = listOf("f(${Formato.corto(r.x)}) = ${Formato.auto(referencia)}",
                "E absoluto = |${Formato.auto(referencia)} − (${Formato.auto(r.valor)})|"),
            resultados = listOf(ResultadoPaso("Error absoluto", absoluto?.let { Formato.auto(it) } ?: "No disponible", absoluto),
                ResultadoPaso("Error relativo %", relativo?.let { Formato.auto(it) + "%" } ?: if (referencia == 0.0) "No definido: f(x) = 0" else "No disponible", relativo)),
            resumenCorto = absoluto?.let { Formato.auto(it) } ?: "No disponible",
        )
        val avisos = original.advertencias.toMutableList()
        if (puntos.any { it.x == r.x }) avisos += "El valor evaluado coincide con un punto utilizado: se reproduce su Y. Esto no demuestra precisión en otros valores de x."
        val detalle = original.copy(bloques = bloques, advertencias = avisos,
            estado = if (avisos.isEmpty()) Estado.OK else Estado.ADVERTENCIA)
        return GradoLagrange(r.grado, puntos, r.valor, absoluto, relativo, r.extrapolacion, detalle)
    }

    fun tabla(c: ComparacionLagrange): BloqueTabla = BloqueTabla(
        titulo = "Comparación de grados",
        columnas = listOf("Grado", "Puntos", "P(x)") + if (c.referencia != null) listOf("Error absoluto") else emptyList(),
        filas = c.resultados.map { r ->
            listOf(Celda.Entero(r.grado), Celda.Txt(r.puntos.joinToString { nombrePunto(it.id) }), Celda.Num(r.valor)) +
                if (c.referencia != null) listOf(r.errorAbsoluto?.let { Celda.Num(it) } ?: Celda.Txt("No disponible")) else emptyList()
        },
    )

    fun presentar(puntos: List<PuntoDatoLagrange>, x: Double, resultados: List<GradoLagrange>, funcion: String?, referencia: Double?, avisosFuncion: List<String>): SalidaMetodo {
        val comparacion = ComparacionLagrange(puntos, x, resultados, funcion, referencia)
        val avisos = avisosFuncion + resultados.filter { it.extrapolacion }.map {
            "Grado ${it.grado}: x está fuera del intervalo de sus puntos seleccionados; se está extrapolando."
        }
        val resumen = if (referencia != null) listOf(ItemResumen("f(x) de referencia", Formato.auto(referencia), true)) else emptyList()
        return SalidaMetodo(MetodoId.LAGRANGE, if (avisos.isEmpty()) Estado.OK else Estado.ADVERTENCIA,
            resumen, emptyList(), grafica(comparacion), listOf(tabla(comparacion)),
            avisos, null, resultados.first().detalle.exportable, comparacion)
    }

    /** Mismo intervalo en todas las curvas; cada detalle mantiene sus nodos elegidos. */
    fun grafica(c: ComparacionLagrange, grado: Int? = null): Grafica {
        val elegidos = c.resultados.filter { grado == null || it.grado == grado }
        val (desde, hasta) = Muestreo.rangoConMargen(minOf(c.puntos.minOf { it.x }, c.x), maxOf(c.puntos.maxOf { it.x }, c.x))
        val series = mutableListOf(Serie("Datos ingresados", TipoSerie.PUNTOS, c.puntos.map { Punto(it.x, it.y) }))
        for (r in elegidos) {
            val xs = r.puntos.map { it.x }.toDoubleArray()
            val ys = r.puntos.map { it.y }.toDoubleArray()
            val evaluar = com.example.pentametod2.methods.Interpolacion.lagrange(xs, ys, c.x).evaluar
            series += Serie("Grado ${r.grado}", TipoSerie.LINEA, Muestreo.curva(evaluar, desde, hasta))
            series += Serie("P${r.grado}(x*)", TipoSerie.PUNTOS, listOf(Punto(c.x, r.valor)), destacada = true)
        }
        if (elegidos.size == 1) series += Serie("Puntos utilizados", TipoSerie.PUNTOS, elegidos.single().puntos.map { Punto(it.x, it.y) }, destacada = true)
        return Grafica("Polinomios de Lagrange", "x", "y", series)
    }
}
