package com.example.pentametod2.presentacion

import com.example.pentametod2.methods.Muestreo
import com.example.pentametod2.methods.ResultadoLagrange
import com.example.pentametod2.methods.ResultadoNewton
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.DatosExportables
import com.example.pentametod2.model.ResultadoPaso
import com.example.pentametod2.model.TipoTexto
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Formula
import com.example.pentametod2.model.Grafica
import com.example.pentametod2.model.ItemResumen
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.Punto
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.model.Serie
import com.example.pentametod2.model.TipoSerie
import com.example.pentametod2.presentacion.Notacion.dato
import com.example.pentametod2.presentacion.Notacion.entreParentesisSiNegativo
import com.example.pentametod2.presentacion.Notacion.num
import com.example.pentametod2.presentacion.Notacion.resta
import com.example.pentametod2.presentacion.Notacion.sub
import com.example.pentametod2.presentacion.Notacion.sup

object PresentadorInterpolacion {

    const val ADVERTENCIA_EXTRAPOLACION =
        "x está fuera del rango de los nodos (extrapolación): el resultado puede ser poco confiable"

    /** `automatico`: los nodos los eligió el modo automático entre `nodosDados` (para decirlo en el paso 1). */
    fun presentarLagrange(r: ResultadoLagrange, automatico: Boolean = false, nodosDados: Int = r.xs.size): SalidaMetodo {
        val n = r.xs.size
        val xt = dato(r.x)
        val sustituida = mutableListOf<String>()
        for (i in 0 until n) {
            val otros = (0 until n).filter { it != i }
            val numerador = otros.joinToString("") { resta(xt, r.xs[it]) }
            val denominador = otros.joinToString("") { resta(dato(r.xs[i]), r.xs[it]) }
            sustituida += "L${sub(i)}($xt) = ${fraccion(numerador, denominador, otros.size)} = ${num(r.L[i])}"
        }
        val simbolos = (0 until n).joinToString(" + ") { "y${sub(it)}·L${sub(it)}" }
        val numeros = (0 until n).joinToString(" + ") { "${dato(r.ys[it])}·${entreParentesisSiNegativo(num(r.L[it]))}" }
        sustituida += "P${sub(r.grado)}($xt) = $simbolos = $numeros = ${num(r.valor)}"

        val formula = Formula(
            titulo = "Polinomio de Lagrange",
            general = listOf("Pₙ(x) = Σ yᵢ · Lᵢ(x)", "Lᵢ(x) = Π (x − xⱼ) / (xᵢ − xⱼ),  j ≠ i"),
            sustituida = sustituida,
        )

        val filas = (0 until n).map { i ->
            listOf(Celda.Entero(i), Celda.Num(r.xs[i]), Celda.Num(r.ys[i]), Celda.Num(r.L[i]), Celda.Num(r.terminos[i]))
        }
        val total = listOf(Celda.Txt("Σ"), Celda.Vacia, Celda.Vacia, Celda.Num(r.L.sum()), Celda.Num(r.valor))
        val tabla = BloqueTabla(
            titulo = "Nodos y polinomios de Lagrange",
            columnas = listOf("i", "xᵢ", "yᵢ", "Lᵢ($xt)", "yᵢ·Lᵢ($xt)"),
            filas = filas + listOf(total),
            filaTotal = true,
        )

        val advertencias = advertenciasInterpolacion(r.extrapolacion)
        val pasos = pasosLagrange(r, sustituida.take(n), numeros, automatico, nodosDados)
        return SalidaMetodo(
            metodo = MetodoId.LAGRANGE,
            estado = Notacion.estado(advertencias),
            resumen = resumenInterpolacion(r.grado, n, r.x, r.valor),
            formulas = listOf(formula),
            grafica = graficaInterpolacion(r.xs, r.ys, r.x, r.valor, r.evaluar),
            bloques = pasos + tabla + conclusionInterpolacion(r.grado, n, r.x, r.valor),
            advertencias = advertencias,
            error = null,
            exportable = DatosExportables.Interpolacion(r.xs.toList(), r.ys.toList(), r.x, automatico, nodosDados),
        )
    }

    fun presentarNewton(r: ResultadoNewton, automatico: Boolean = false, nodosDados: Int = r.xs.size): SalidaMetodo {
        val n = r.xs.size
        val xt = dato(r.x)
        val factoresX = (0 until n).map { k -> if (k == 0) "" else Notacion.factorNewton("x", r.xs, k) }
        val sustituida = listOf(
            "P${sub(r.grado)}(x) = ${Notacion.polinomio(r.coeficientes, factoresX)}",
            "P${sub(r.grado)}($xt) = ${r.terminos.joinToString(" + ") { entreParentesisSiNegativo(num(it)) }} = ${num(r.valor)}",
        )
        val formula = Formula(
            titulo = "Polinomio de Newton (diferencias divididas)",
            general = listOf(
                "Pₙ(x) = f[x₀] + f[x₀,x₁](x−x₀) + f[x₀,x₁,x₂](x−x₀)(x−x₁) + …",
                "f[xᵢ,…,xᵢ₊ₖ] = ( f[xᵢ₊₁,…,xᵢ₊ₖ] − f[xᵢ,…,xᵢ₊ₖ₋₁] ) / ( xᵢ₊ₖ − xᵢ )",
            ),
            sustituida = sustituida,
        )

        val columnas = listOf("k", "xₖ", "f[xₖ]") + (1 until n).map { "Δ${sup(it)}" }
        val filasDif = (0 until n).map { k ->
            listOf(Celda.Entero(k), Celda.Num(r.xs[k])) + r.tabla[k].map { Notacion.celda(it) }
        }
        val tablaDif = BloqueTabla("Tabla de diferencias divididas", columnas, filasDif)

        val filasTerm = (0 until n).map { k ->
            listOf(
                Celda.Entero(k), Celda.Num(r.coeficientes[k]),
                Celda.Txt(Notacion.factorNewton(xt, r.xs, k)), Celda.Num(producto(r.xs, r.x, k)),
                Celda.Num(r.terminos[k]),
            )
        }
        val totalTerm = listOf(Celda.Txt("Σ"), Celda.Vacia, Celda.Vacia, Celda.Vacia, Celda.Num(r.valor))
        val tablaTerm = BloqueTabla(
            titulo = "Términos de Newton",
            columnas = listOf("k", "f[x₀,…,xₖ]", "Factor", "Producto", "Término"),
            filas = filasTerm + listOf(totalTerm),
            filaTotal = true,
        )

        val advertencias = advertenciasInterpolacion(r.extrapolacion)
        val pasos = pasosNewton(r, tablaDif, sustituida[0], automatico, nodosDados)
        return SalidaMetodo(
            metodo = MetodoId.NEWTON_INTERPOLACION,
            estado = Notacion.estado(advertencias),
            resumen = resumenInterpolacion(r.grado, n, r.x, r.valor),
            formulas = listOf(formula),
            grafica = graficaInterpolacion(r.xs, r.ys, r.x, r.valor, r.evaluar),
            bloques = pasos + tablaDif + tablaTerm + conclusionInterpolacion(r.grado, n, r.x, r.valor),
            advertencias = advertencias,
            error = null,
            exportable = DatosExportables.Interpolacion(r.xs.toList(), r.ys.toList(), r.x, automatico, nodosDados),
        )
    }

    // ── Pasos ──

    /** Lagrange: nodos usados, polinomio, cada Lₖ con tus datos y la evaluación (destacada). */
    internal fun pasosLagrange(
        r: ResultadoLagrange,
        lineasL: List<String>,
        sumaNumerica: String,
        automatico: Boolean,
        nodosDados: Int,
    ): List<BloquePaso> {
        val n = r.xs.size
        val g = r.grado
        val xt = dato(r.x)
        val ks = 0 until n
        val p1 = pasoNodos(r.xs, r.ys, r.x, automatico, nodosDados, "yₖ")
        val p2 = BloquePaso(
            numero = 2,
            titulo = "Polinomio de Lagrange",
            explicacion = "Es una suma de los valores yₖ, cada uno multiplicado por un polinomio Lₖ que vale 1 en su nodo y 0 en los demás.",
            formulaGeneral = listOf("P(x) = Σ yₖ · Lₖ(x)", "Lₖ(x) = Π (x − xⱼ) / (xₖ − xⱼ),  j ≠ k"),
            conTusDatos = listOf(
                "P${sub(g)}(x) = " + ks.joinToString(" + ") { "y${sub(it)}·L${sub(it)}(x)" },
                "P${sub(g)}(x) = " + ks.joinToString(" + ") { "${dato(r.ys[it])}·L${sub(it)}(x)" },
            ),
            resultados = listOf(ResultadoPaso("P${sub(g)}(x)", "Σ yₖ·Lₖ(x),  k = 0…${n - 1}")),
            resumenCorto = "Σ yₖ·Lₖ(x)",
        )
        val p3 = BloquePaso(
            numero = 3,
            titulo = "Cada Lₖ(x) con tus datos",
            explicacion = "Se reemplaza x = $xt en cada producto; los Lₖ deben sumar 1.",
            formulaGeneral = listOf("Lₖ($xt) = Π ($xt − xⱼ) / (xₖ − xⱼ),  j ≠ k"),
            conTusDatos = lineasL,
            resultados = ks.map { ResultadoPaso("L${sub(it)}($xt)", num(r.L[it]), r.L[it]) },
            resumenCorto = "L₀ … L${sub(n - 1)}",
        )
        val p4 = BloquePaso(
            numero = 4,
            titulo = "Evaluación",
            explicacion = "Se suman los productos yₖ·Lₖ($xt): ese es el valor del polinomio en x = $xt.",
            formulaGeneral = listOf("P(x) = Σ yₖ · Lₖ(x)"),
            conTusDatos = listOf(
                "P${sub(g)}($xt) = $sumaNumerica",
                "= " + ks.joinToString(" + ") { entreParentesisSiNegativo(num(r.terminos[it])) },
                "= ${num(r.valor)}",
                "Comprobación: Σ Lₖ($xt) = ${num(r.L.sum())}",
            ),
            resultados = listOf(ResultadoPaso("P($xt)", num(r.valor), r.valor)),
            resumenCorto = num(r.valor),
            destacado = true,
        )
        return listOf(p1, p2, p3, p4)
    }

    /** Newton: nodos, tabla de diferencias divididas, polinomio, términos y evaluación por grado (destacada). */
    internal fun pasosNewton(
        r: ResultadoNewton,
        tablaDif: BloqueTabla,
        polinomio: String,
        automatico: Boolean,
        nodosDados: Int,
    ): List<BloquePaso> {
        val n = r.xs.size
        val g = r.grado
        val xt = dato(r.x)
        val p1 = pasoNodos(r.xs, r.ys, r.x, automatico, nodosDados, "f[xₖ]")

        val primeraFila = (1 until n).map { j ->
            "${nombreDif(0, j)} = (${num(r.tabla[1][j - 1])} − ${entreParentesisSiNegativo(num(r.tabla[0][j - 1]))}) / " +
                "(${dato(r.xs[j])} − ${dato(r.xs[0])}) = ${num(r.tabla[0][j])}"
        }
        val p2 = BloquePaso(
            numero = 2,
            titulo = "Tabla de diferencias divididas",
            explicacion = "Cada columna sale de la anterior: la diferencia de dos valores vecinos entre la distancia de sus nodos.",
            formulaGeneral = listOf(
                "f[xᵢ] = yᵢ",
                "f[xᵢ,…,xᵢ₊ⱼ] = (f[xᵢ₊₁,…,xᵢ₊ⱼ] − f[xᵢ,…,xᵢ₊ⱼ₋₁]) / (xᵢ₊ⱼ − xᵢ)",
            ),
            conTusDatos = listOf("Primera fila (son los coeficientes del polinomio):") + primeraFila,
            tablas = listOf(tablaDif),
            resultados = (0 until n).map { j -> ResultadoPaso(nombreDif(0, j), num(r.coeficientes[j]), r.coeficientes[j]) },
            resumenCorto = "$n×$n",
        )

        val p3 = BloquePaso(
            numero = 3,
            titulo = "Polinomio de Newton",
            explicacion = "Los coeficientes son la primera fila de la tabla; cada uno multiplica a los factores (x − xⱼ) anteriores.",
            formulaGeneral = listOf("P(x) = f[x₀] + f[x₀,x₁](x − x₀) + f[x₀,x₁,x₂](x − x₀)(x − x₁) + …"),
            conTusDatos = listOf(polinomio),
            resultados = listOf(ResultadoPaso("P${sub(g)}(x)", polinomio.substringAfter(" = "))),
            resumenCorto = "P${sub(g)}(x)",
        )

        val lineasTerminos = (0 until n).map { k ->
            if (k == 0) {
                "k = 0: ${num(r.coeficientes[0])} = ${num(r.terminos[0])}"
            } else {
                "k = $k: ${num(r.coeficientes[k])} · ${Notacion.factorNewton(xt, r.xs, k)} = ${num(r.terminos[k])}"
            }
        }
        val p4 = BloquePaso(
            numero = 4,
            titulo = "Términos",
            explicacion = "Cada término es un coeficiente por su factor evaluado en x = $xt (ver la tabla «Términos de Newton»).",
            formulaGeneral = listOf("términoₖ = f[x₀,…,xₖ] · (x − x₀)⋯(x − xₖ₋₁)"),
            conTusDatos = lineasTerminos,
            resultados = (0 until n).map { k -> ResultadoPaso("término${sub(k)}", num(r.terminos[k]), r.terminos[k]) },
            resumenCorto = "$n términos",
        )

        val acumulados = acumulados(r.terminos, r.valor)
        val lineasGrado = (0 until n).map { k ->
            if (k == 0) {
                "P₀($xt) = ${num(acumulados[0])}"
            } else {
                "P${sub(k)}($xt) = ${num(acumulados[k - 1])} + ${entreParentesisSiNegativo(num(r.terminos[k]))} = ${num(acumulados[k])}"
            }
        }
        val p5 = BloquePaso(
            numero = 5,
            titulo = "Evaluación por grado",
            explicacion = "Sumando los términos uno a uno se obtiene el valor con cada grado; el último es P${sub(g)}($xt).",
            formulaGeneral = listOf("Pⱼ(x) = Pⱼ₋₁(x) + términoⱼ"),
            conTusDatos = lineasGrado,
            resultados = (0 until n).map { k -> ResultadoPaso("P${sub(k)}($xt)", num(acumulados[k]), acumulados[k]) },
            resumenCorto = num(r.valor),
            destacado = true,
        )
        return listOf(p1, p2, p3, p4, p5)
    }

    /** Sumas parciales de los términos; la última es el valor que calculó el método (no se recalcula). */
    internal fun acumulados(terminos: DoubleArray, valor: Double): List<Double> {
        val r = mutableListOf<Double>()
        var suma = 0.0
        for (t in terminos) {
            suma += t
            r += suma
        }
        if (r.isNotEmpty()) r[r.lastIndex] = valor
        return r
    }

    /** f[x₀], f[x₀,x₁], f[x₀,x₁,x₂], f[x₀,…,x₃] */
    internal fun nombreDif(i: Int, j: Int): String = when (j) {
        0 -> "f[x${sub(i)}]"
        1 -> "f[x${sub(i)},x${sub(i + 1)}]"
        2 -> "f[x${sub(i)},x${sub(i + 1)},x${sub(i + 2)}]"
        else -> "f[x${sub(i)},…,x${sub(i + j)}]"
    }

    /** Paso 1 de los dos métodos: los nodos que se usan y el grado que resulta. */
    private fun pasoNodos(
        xs: DoubleArray,
        ys: DoubleArray,
        x: Double,
        automatico: Boolean,
        nodosDados: Int,
        columnaY: String,
    ): BloquePaso {
        val n = xs.size
        val explicacion = if (automatico) {
            "En modo automático se eligieron los $n nodos más cercanos a x = ${dato(x)} (de los $nodosDados que diste)."
        } else {
            "Se usan exactamente los $n nodos que diste, así que el polinomio es de grado ${n - 1}."
        }
        val decimales = decimalesDatos(xs + ys)
        val tabla = BloqueTabla(
            titulo = "Nodos usados",
            columnas = listOf("k", "xₖ", columnaY),
            filas = (0 until n).map { listOf(Celda.Entero(it), Celda.Num(xs[it]), Celda.Num(ys[it])) },
            decimales = decimales,
        )
        return BloquePaso(
            numero = 1,
            titulo = "Nodos usados",
            explicacion = explicacion,
            formulaGeneral = listOf("Con n nodos, el polinomio que pasa por todos es de grado n − 1"),
            conTusDatos = listOf("n = $n nodos  →  grado ${n - 1}", "x a evaluar = ${dato(x)}"),
            tablas = listOf(tabla),
            resultados = listOf(ResultadoPaso("Nodos", n.toString(), n.toDouble()), ResultadoPaso("Grado", "${n - 1}", (n - 1).toDouble())),
            resumenCorto = "grado ${n - 1}",
            abiertoInicial = true,
        )
    }

    /** Decimales para mostrar los datos tal como se escribieron: entre 6 y 10. */
    internal fun decimalesDatos(valores: DoubleArray): Int =
        valores.maxOfOrNull { dato(it).substringAfter('.', "").length }?.coerceIn(6, 10) ?: 6

    private fun conclusionInterpolacion(grado: Int, nodos: Int, x: Double, valor: Double): BloqueTexto =
        BloqueTexto(
            "El polinomio de grado $grado que pasa por los $nodos nodos da P(${dato(x)}) = ${num(valor)}.",
            TipoTexto.CONCLUSION,
        )

    // ── auxiliares ──

    private fun fraccion(numerador: String, denominador: String, factores: Int): String =
        if (factores == 1) "$numerador/$denominador" else "$numerador / [$denominador]"

    /** (x − x₀)(x − x₁)…(x − xₖ₋₁) */
    private fun producto(xs: DoubleArray, x: Double, k: Int): Double {
        var p = 1.0
        for (j in 0 until k) p *= x - xs[j]
        return p
    }

    private fun advertenciasInterpolacion(extrapolacion: Boolean): List<String> =
        if (extrapolacion) listOf(ADVERTENCIA_EXTRAPOLACION) else emptyList()

    private fun resumenInterpolacion(grado: Int, nodos: Int, x: Double, valor: Double) = listOf(
        ItemResumen("Grado", grado.toString()),
        ItemResumen("Nodos usados", nodos.toString()),
        ItemResumen("x", dato(x)),
        ItemResumen("P(${dato(x)})", num(valor), destacado = true),
    )

    private fun graficaInterpolacion(
        xs: DoubleArray, ys: DoubleArray, x: Double, valor: Double, evaluar: (Double) -> Double,
    ): Grafica {
        val (desde, hasta) = Muestreo.rangoConMargen(minOf(xs.min(), x), maxOf(xs.max(), x))
        return Grafica(
            titulo = "Polinomio interpolante",
            ejeX = "x",
            ejeY = "y",
            series = listOf(
                Serie("Nodos", TipoSerie.PUNTOS, xs.indices.map { Punto(xs[it], ys[it]) }),
                Serie("P(x)", TipoSerie.LINEA, Muestreo.curva(evaluar, desde, hasta)),
                Serie("x evaluado", TipoSerie.PUNTOS, listOf(Punto(x, valor)), destacada = true),
            ),
        )
    }
}
