package com.example.pentametod2.presentacion

import com.example.pentametod2.methods.Muestreo
import com.example.pentametod2.methods.ResultadoRegresion
import com.example.pentametod2.model.Bloque
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.DatosExportables
import com.example.pentametod2.model.ResultadoPaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Formato
import com.example.pentametod2.model.Formula
import com.example.pentametod2.model.Grafica
import com.example.pentametod2.model.ItemResumen
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.Punto
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.model.Serie
import com.example.pentametod2.model.TipoSerie
import com.example.pentametod2.model.TipoTexto
import com.example.pentametod2.presentacion.Notacion.dato
import com.example.pentametod2.presentacion.Notacion.num
import com.example.pentametod2.presentacion.Notacion.potencia
import com.example.pentametod2.presentacion.Notacion.sub
import kotlin.math.pow
import kotlin.math.sqrt

object PresentadorRegresion {

    fun presentarRegresionPolinomial(
        r: ResultadoRegresion,
        xs: DoubleArray,
        ys: DoubleArray,
        xPredecir: Double? = null,
    ): SalidaMetodo {
        val m = r.grado ?: throw IllegalArgumentException("El resultado no es de una regresión polinomial")
        val n = xs.size
        val ecuacion = "P${sub(m)}(x) = " + Notacion.polinomio(r.coeficientes, (0..m).map { k -> factorPotencia("x", k) })

        val formula = Formula(
            titulo = "Regresión polinomial por mínimos cuadrados",
            general = listOf(
                "Pₘ(x) = a₀ + a₁x + … + aₘxᵐ",
                "Sistema normal: C·a = b,  Cᵢⱼ = Σ xₖ^(i+j),  bᵢ = Σ xₖⁱ·yₖ",
                "a = C⁻¹·b",
                "Q = Σ (Pₘ(xₖ) − yₖ)²   ·   R² = (S_yy − Q) / S_yy   ·   S = √(Q / (n − m − 1))",
            ),
            sustituida = listOf(ecuacion) + lineasAjuste(r, "n − m − 1 = ${n - m - 1}"),
        )

        // Tabla de datos: i, xᵢ, yᵢ, xᵢ²…xᵢ^(2m), xᵢyᵢ…xᵢ^m·yᵢ, P(xᵢ), (P(xᵢ)−yᵢ)², (yᵢ−ȳ)²
        val columnas = mutableListOf("i", "xᵢ", "yᵢ")
        for (k in 2..2 * m) columnas += "xᵢ${Notacion.sup(k)}"
        for (k in 1..m) columnas += "xᵢ${potencia(k)}yᵢ"
        columnas += listOf("P(xᵢ)", "(P(xᵢ)−yᵢ)²", "(yᵢ−ȳ)²")
        val valores = (0 until n).map { i ->
            val fila = mutableListOf(xs[i], ys[i])
            for (k in 2..2 * m) fila += xs[i].pow(k)
            for (k in 1..m) fila += xs[i].pow(k) * ys[i]
            fila += finDeFila(r, ys[i], i)
            fila.toDoubleArray()
        }
        val tabla = BloqueTabla(
            titulo = "Datos y sumatorias",
            columnas = columnas,
            filas = Notacion.conFilaDeSumas((1..n).map { Celda.Entero(it) }, valores),
            filaTotal = true,
        )

        val resumen = mutableListOf(ItemResumen("Ecuación", ecuacion))
        resumen += resumenAjuste(r)
        resumen += ItemResumen("Grado", m.toString())

        val curvaDesde = minOf(xs.min(), xPredecir ?: xs.min())
        val curvaHasta = maxOf(xs.max(), xPredecir ?: xs.max())
        val (desde, hasta) = Muestreo.rangoConMargen(curvaDesde, curvaHasta)
        val series = mutableListOf(
            Serie("Datos", TipoSerie.PUNTOS, xs.indices.map { Punto(xs[it], ys[it]) }),
            Serie("P(x)", TipoSerie.LINEA, Muestreo.curva(r::predecir, desde, hasta)),
        )
        if (xPredecir != null) {
            val p = r.predecir(xPredecir)
            resumen += ItemResumen("P(${dato(xPredecir)})", num(p), destacado = true)
            series += Serie("Predicción", TipoSerie.PUNTOS, listOf(Punto(xPredecir, p)), destacada = true)
        }

        return SalidaMetodo(
            metodo = MetodoId.REGRESION_POLINOMIAL,
            estado = Notacion.estado(r.advertencias),
            resumen = resumen,
            formulas = listOf(formula),
            grafica = Grafica("Ajuste polinomial", "x", "y", series),
            bloques = pasosPolinomial(r, xs, ys, xPredecir, ecuacion) + tabla + conclusion(r, "El polinomio"),
            advertencias = r.advertencias,
            error = null,
            exportable = DatosExportables.RegresionPolinomial(xs.toList(), ys.toList(), m, xPredecir),
        )
    }

    /** Puntos que se desarrollan uno por uno en "Con tus datos"; con más, se resume y se remite a la tabla. */
    const val MAX_TERMINOS_DESARROLLO = 12

    /**
     * Pasos de la explicación, en orden: sistema normal, ecuación, Q, ȳ y S_yy, R² y r, S y (si hay x a predecir)
     * la predicción. Solo el primero viene abierto. Los valores son los del resultado: aquí solo se presentan.
     */
    internal fun pasosPolinomial(
        r: ResultadoRegresion,
        xs: DoubleArray,
        ys: DoubleArray,
        xPredecir: Double?,
        ecuacion: String,
    ): List<BloquePaso> {
        val m = r.grado ?: 0
        val n = xs.size
        val polinomio = ecuacion.substringAfter(" = ")
        val nombreP = "P${sub(m)}(x)"
        val pasos = mutableListOf<BloquePaso>()

        pasos += BloquePaso(
            numero = 1,
            titulo = "Sistema normal",
            explicacion = "Plantea las ecuaciones cuya solución son los coeficientes del polinomio.",
            formulaGeneral = listOf("C · a = b", "Cᵢⱼ = Σ xₖ^(i+j)   bᵢ = Σ xₖ^i · yₖ"),
            conTusDatos = listOf("m = $m (grado),  n = $n (puntos),  C es de ${m + 1}×${m + 1}"),
            matrices = bloquesMatrices(r),
            resultados = listOf(ResultadoPaso(nombreP, polinomio)),
            resumenCorto = "C·a = b",
            abiertoInicial = true,
        )

        val coeficientes = (0..m).map { k -> ResultadoPaso("a${sub(k)}", num(r.coeficientes[k]), r.coeficientes[k]) }
        pasos += BloquePaso(
            numero = 2,
            titulo = "Ecuación del polinomio",
            explicacion = "Los coeficientes salen de resolver el sistema: se multiplica la inversa de C por b.",
            formulaGeneral = listOf("a = C⁻¹ · b", "Pₘ(x) = a₀ + a₁x + … + aₘxᵐ"),
            conTusDatos = coeficientes.map { "${it.etiqueta} = ${it.texto}" } + "$nombreP = $polinomio",
            resultados = coeficientes,
            resumenCorto = (0..m).joinToString(", ") { "a${sub(it)}" },
        )

        pasos += pasosDeAjuste(
            r = r,
            ys = ys,
            terminoQ = { i, y -> "(P(${dato(xs[i])}) − $y)² = (${num(r.yAjustada[i])} − $y)² = ${num(r.residuos[i] * r.residuos[i])}" },
            formulaQ = "Q = Σ (P(xₖ) − yₖ)²",
            sujetoR2 = "el polinomio",
            parametros = "m",
        )

        if (xPredecir != null) {
            val p = r.predecir(xPredecir)
            val xp = "(${dato(xPredecir)})"
            val sustitucion = (0..m).joinToString(" + ") { k ->
                val a = entreParentesis(num(r.coeficientes[k]))
                when (k) {
                    0 -> a
                    1 -> "$a$xp"
                    else -> "$a$xp${Notacion.sup(k)}"
                }
            }
            val terminos = (0..m).map { k -> r.coeficientes[k] * xPredecir.pow(k) }
            pasos += BloquePaso(
                numero = 7,
                titulo = "Predicción",
                explicacion = "Evalúa el polinomio en el valor de x que pediste.",
                formulaGeneral = listOf("P(x) = a₀ + a₁x + … + aₘxᵐ"),
                conTusDatos = listOf(
                    "P(${dato(xPredecir)}) = $sustitucion",
                    "= " + Notacion.polinomio(terminos.toDoubleArray(), List(terminos.size) { "" }),
                    "= ${num(p)}",
                ),
                resultados = listOf(ResultadoPaso("P(${dato(xPredecir)})", num(p), p)),
                resumenCorto = num(p),
                destacado = true,
            )
        }
        return pasos
    }

    /**
     * Regresión múltiple: modelo y sistema normal, ecuación, Q, ȳ y S_yy, R² y r, S y (si hay punto) la predicción.
     * Mismos pasos 3–6 que la polinomial, con β en lugar de a y k variables en lugar del grado m.
     */
    internal fun pasosMultiple(
        r: ResultadoRegresion,
        x: Array<DoubleArray>,
        y: DoubleArray,
        xPredecir: DoubleArray?,
        ecuacion: String,
    ): List<BloquePaso> {
        val k = r.coeficientes.size - 1
        val n = y.size
        val modelo = ecuacion.substringAfter(" = ")
        val pasos = mutableListOf<BloquePaso>()

        pasos += BloquePaso(
            numero = 1,
            titulo = "Modelo y sistema normal",
            explicacion = "El modelo es lineal en los coeficientes β; se obtienen resolviendo el sistema normal.",
            formulaGeneral = listOf(
                "y = β₀ + β₁x₁ + … + βₖxₖ",
                "(XᵀX) · β = Xᵀy   (X: una columna de unos y una por variable)",
            ),
            conTusDatos = listOf("k = $k (variables),  n = $n (filas),  XᵀX es de ${k + 1}×${k + 1}"),
            matrices = listOf(
                BloqueMatriz("Matriz del sistema normal", "XᵀX", Notacion.matriz(r.matrizC)),
                BloqueMatriz("Vector independiente", "Xᵀy", Notacion.columna(r.vectorB)),
                BloqueMatriz("Inversa", "(XᵀX)⁻¹", Notacion.matriz(r.inversaC)),
                BloqueMatriz("Coeficientes", "β", Notacion.columna(r.coeficientes)),
            ),
            resultados = listOf(ResultadoPaso("y", modelo)),
            resumenCorto = "(XᵀX)·β = Xᵀy",
            abiertoInicial = true,
        )

        val betas = (0..k).map { j -> ResultadoPaso("β${sub(j)}", num(r.coeficientes[j]), r.coeficientes[j]) }
        pasos += BloquePaso(
            numero = 2,
            titulo = "Ecuación del modelo",
            explicacion = "Los coeficientes salen de multiplicar la inversa de XᵀX por Xᵀy.",
            formulaGeneral = listOf("β = (XᵀX)⁻¹ · Xᵀy"),
            conTusDatos = betas.map { "${it.etiqueta} = ${it.texto}" } + "y = $modelo",
            resultados = betas,
            resumenCorto = (0..k).joinToString(", ") { "β${sub(it)}" },
        )

        pasos += pasosDeAjuste(
            r = r,
            ys = y,
            terminoQ = { i, yi -> "(ŷ${sub(i + 1)} − $yi)² = (${num(r.yAjustada[i])} − $yi)² = ${num(r.residuos[i] * r.residuos[i])}" },
            formulaQ = "Q = Σ (ŷᵢ − yᵢ)²",
            sujetoR2 = "el modelo",
            parametros = "k",
        )

        if (xPredecir != null) {
            val p = r.predecir(xPredecir)
            val punto = xPredecir.joinToString(", ", "(", ")") { dato(it) }
            val sustitucion = (0..k).joinToString(" + ") { j ->
                val b = entreParentesis(num(r.coeficientes[j]))
                if (j == 0) b else "$b(${dato(xPredecir[j - 1])})"
            }
            val terminos = (0..k).map { j -> if (j == 0) r.coeficientes[0] else r.coeficientes[j] * xPredecir[j - 1] }
            pasos += BloquePaso(
                numero = 7,
                titulo = "Predicción",
                explicacion = "Evalúa el modelo en el punto que pediste.",
                formulaGeneral = listOf("ŷ = β₀ + β₁x₁ + … + βₖxₖ"),
                conTusDatos = listOf(
                    "ŷ$punto = $sustitucion",
                    "= " + Notacion.polinomio(terminos.toDoubleArray(), List(terminos.size) { "" }),
                    "= ${num(p)}",
                ),
                resultados = listOf(ResultadoPaso("Predicción $punto", num(p), p)),
                resumenCorto = num(p),
                destacado = true,
            )
        }
        return pasos
    }

    /** Pasos 3–6, comunes a las dos regresiones: Q, ȳ y S_yy, R² y r, S. */
    private fun pasosDeAjuste(
        r: ResultadoRegresion,
        ys: DoubleArray,
        terminoQ: (Int, String) -> String,
        formulaQ: String,
        sujetoR2: String,
        parametros: String,
    ): List<BloquePaso> {
        val n = ys.size
        val p = r.coeficientes.size // m + 1 o k + 1
        val pasos = mutableListOf<BloquePaso>()

        val cuadrados = (0 until n).map { r.residuos[it] * r.residuos[it] }
        val terminosQ = (0 until n).map { i -> terminoQ(i, entreParentesis(dato(ys[i]))) }
        pasos += BloquePaso(
            numero = 3,
            titulo = "Suma de cuadrados de residuos",
            explicacion = "Mide cuánto se alejan los datos de la curva: suma los cuadrados de las diferencias.",
            formulaGeneral = listOf(formulaQ),
            conTusDatos = recortarDesarrollo(terminosQ) + "Q = ${suma(cuadrados.map { num(it) })} = ${num(r.Q)}",
            resultados = listOf(ResultadoPaso("Q", num(r.Q), r.Q)),
            resumenCorto = num(r.Q),
        )

        val media = num(r.mediaY)
        pasos += BloquePaso(
            numero = 4,
            titulo = "Media y variación de y",
            explicacion = "Resume cuánto varían los datos de y alrededor de su promedio.",
            formulaGeneral = listOf("ȳ = Σ yₖ / n", "S_yy = Σ (yₖ − ȳ)²"),
            conTusDatos = listOf(
                "ȳ = (${suma(ys.map { dato(it) })}) / $n = $media",
                "S_yy = ${suma(ys.map { "(${dato(it)} − $media)²" })} = ${num(r.Syy)}",
            ),
            resultados = listOf(ResultadoPaso("ȳ", media, r.mediaY), ResultadoPaso("S_yy", num(r.Syy), r.Syy)),
            resumenCorto = "ȳ = $media",
        )

        val rCorrelacion = sqrt(r.r2.coerceAtLeast(0.0))
        pasos += BloquePaso(
            numero = 5,
            titulo = "Coeficiente de determinación",
            explicacion = "Indica qué fracción de la variación de y explica $sujetoR2 (1 es un ajuste perfecto).",
            formulaGeneral = listOf("R² = (S_yy − Q) / S_yy", if (r.grado == null) "R = √R² (coeficiente de correlación múltiple)" else "r = √R²"),
            conTusDatos = listOf(
                "R² = (${num(r.Syy)} − ${num(r.Q)}) / ${num(r.Syy)} = ${num(r.r2)}",
                if (r.grado == null) "R = √${num(r.r2)} = ${num(rCorrelacion)}" else "r = √${num(r.r2)} = ${num(rCorrelacion)}",
            ),
            resultados = listOf(ResultadoPaso("R²", num(r.r2), r.r2), ResultadoPaso(if (r.grado == null) "R (correlación múltiple)" else "r", num(rCorrelacion), rCorrelacion)),
            resumenCorto = num(r.r2),
        )

        val s = r.errorEstandar
        val libres = n - p
        pasos += BloquePaso(
            numero = 6,
            titulo = "Error estándar",
            explicacion = if (r.grado == null) "Error típico de los residuos: qué tan lejos caen los datos de la ecuación. Grados de libertad n − ($parametros + 1)." else "Es el error típico de una predicción, teniendo en cuenta los grados de libertad n − ($parametros + 1).",
            formulaGeneral = listOf("S = √(Q / (n − ($parametros + 1)))"),
            conTusDatos = if (s != null) {
                listOf("S = √(${num(r.Q)} / ($n − $p)) = √(${num(r.Q)} / $libres) = ${num(s)}")
            } else {
                listOf("n − ($parametros + 1) = 0: no quedan grados de libertad y S no se puede calcular.")
            },
            resultados = listOf(if (s != null) ResultadoPaso("S", num(s), s) else ResultadoPaso("S", "no definido")),
            resumenCorto = if (s != null) num(s) else "—",
        )
        return pasos
    }

    private fun entreParentesis(s: String) = Notacion.entreParentesisSiNegativo(s)

    /** "a + b + c", resumido con "…" si hay más de [MAX_TERMINOS_DESARROLLO] términos. */
    private fun suma(terminos: List<String>): String =
        if (terminos.size <= MAX_TERMINOS_DESARROLLO) {
            terminos.joinToString(" + ")
        } else {
            terminos.take(3).joinToString(" + ") + " + … + " + terminos.last()
        }

    private fun recortarDesarrollo(lineas: List<String>): List<String> =
        if (lineas.size <= MAX_TERMINOS_DESARROLLO) {
            lineas
        } else {
            lineas.take(MAX_TERMINOS_DESARROLLO) + "… y ${lineas.size - MAX_TERMINOS_DESARROLLO} puntos más (ver «Datos y sumatorias»)"
        }

    /** `x[i]` = [x₁ᵢ, …, xₖᵢ]. */
    fun presentarRegresionMultiple(
        r: ResultadoRegresion,
        x: Array<DoubleArray>,
        y: DoubleArray,
        xPredecir: DoubleArray? = null,
    ): SalidaMetodo {
        val n = y.size
        val k = r.coeficientes.size - 1
        val ecuacion = "y = " + Notacion.polinomio(r.coeficientes, (0..k).map { j -> if (j == 0) "" else "x${sub(j)}" })

        val formula = Formula(
            titulo = "Regresión lineal múltiple por mínimos cuadrados",
            general = listOf(
                "y = a₀ + a₁x₁ + … + aₖxₖ",
                "Sistema normal: C·a = b,  C = XᵀX,  b = Xᵀy  (X tiene una columna de unos y una por variable)",
                "a = C⁻¹·b",
                "Q = Σ (ŷᵢ − yᵢ)²   ·   R² = (S_yy − Q) / S_yy   ·   S = √(Q / (n − k − 1))",
                "R²aj = 1 − (1 − R²)·(n − 1)/(n − k − 1)",
                "F = ((S_yy − Q)/k) / (Q/(n − k − 1))",
            ),
            sustituida = listOf(ecuacion) + lineasAjuste(r, "n − k − 1 = ${n - k - 1}") + listOfNotNull(
                r.r2Ajustado?.let { "R²aj = 1 − (1 − ${num(r.r2)})·(${n} − 1)/(${n} − ${k} − 1) = ${num(it)}" },
                r.estadisticoF?.let { "F global = ${num(it)}, p = ${num(r.valorPF ?: Double.NaN)}" }
            ),
        )

        val columnas = listOf("i") + (1..k).map { "x${sub(it)}ᵢ" } + listOf("yᵢ", "ŷᵢ", "(ŷᵢ−yᵢ)²", "(yᵢ−ȳ)²")
        val valores = (0 until n).map { i -> (x[i].toList() + y[i] + finDeFila(r, y[i], i)).toDoubleArray() }
        val tabla = BloqueTabla(
            titulo = "Datos y residuos",
            columnas = columnas,
            filas = Notacion.conFilaDeSumas((1..n).map { Celda.Entero(it) }, valores),
            filaTotal = true,
        )

        val resumen = mutableListOf(ItemResumen("Ecuación", ecuacion))
        resumen += resumenAjuste(r)
        r.r2Ajustado?.let { resumen += ItemResumen("R² ajustado", num(it)) }
        r.estadisticoF?.let { resumen += ItemResumen("F global", num(it)) }
        r.valorPF?.let { resumen += ItemResumen("p de F global", num(it)) }
        resumen += ItemResumen("Variables independientes (k)", k.toString())
        if (xPredecir != null) {
            val etiqueta = "Predicción " + xPredecir.joinToString(", ", "(", ")") { dato(it) }
            resumen += ItemResumen(etiqueta, num(r.predecir(xPredecir)), destacado = true)
        }

        val realVsPredicho = (0 until n).map { Punto(r.yAjustada[it], y[it]) }
        val minimo = minOf(y.min(), r.yAjustada.min())
        val maximo = maxOf(y.max(), r.yAjustada.max())
        val grafica = Grafica(
            titulo = "Real vs predicho",
            ejeX = "ŷ (predicho)",
            ejeY = "y (real)",
            series = listOf(
                Serie("Real vs predicho", TipoSerie.PUNTOS, realVsPredicho),
                Serie("y = x", TipoSerie.LINEA, listOf(Punto(minimo, minimo), Punto(maximo, maximo))),
            ),
        )

        return SalidaMetodo(
            metodo = MetodoId.REGRESION_MULTIPLE,
            estado = Notacion.estado(r.advertencias),
            resumen = resumen,
            formulas = listOf(formula),
            grafica = grafica,
            bloques = pasosMultiple(r, x, y, xPredecir, ecuacion) + tabla + tablasInferencia(r) +
                listOf(BloqueTabla("Gráfica de residuos (ŷ, residuo)", listOf("ŷ", "Residuo", "Línea y = 0"),
                    (0 until n).map { i -> listOf(Celda.Num(r.yAjustada[i]), Celda.Num(r.residuos[i]), Celda.Num(0.0)) })) +
                interpretaciones(r) + conclusionMultiple(r),
            advertencias = r.advertencias,
            error = null,
            exportable = DatosExportables.RegresionMultiple(x.map { it.toList() }, y.toList(), xPredecir?.toList()),
        )
    }

    // ── auxiliares ──

    private fun factorPotencia(variable: String, k: Int): String = if (k == 0) "" else variable + potencia(k)

    /** ŷᵢ, (ŷᵢ−yᵢ)², (yᵢ−ȳ)² */
    private fun finDeFila(r: ResultadoRegresion, yi: Double, i: Int): List<Double> {
        val d = yi - r.mediaY
        return listOf(r.yAjustada[i], r.residuos[i] * r.residuos[i], d * d)
    }

    private fun lineasAjuste(r: ResultadoRegresion, gradosLibertad: String): List<String> {
        val lineas = mutableListOf(
            "Q = ${num(r.Q)}",
            "ȳ = ${num(r.mediaY)},  S_yy = ${num(r.Syy)}",
            "R² = (${num(r.Syy)} − ${num(r.Q)}) / ${num(r.Syy)} = ${num(r.r2)}",
        )
        val s = r.errorEstandar
        if (s != null) lineas += "S = √(${num(r.Q)} / ${r.n - r.coeficientes.size}) = ${num(s)}   ($gradosLibertad)"
        return lineas
    }

    private fun resumenAjuste(r: ResultadoRegresion): List<ItemResumen> {
        val items = mutableListOf(
            ItemResumen("Q", num(r.Q)),
            ItemResumen("R²", num(r.r2)),
        )
        val s = r.errorEstandar
        if (s != null) items += ItemResumen("S", num(s))
        items += ItemResumen("n", r.n.toString())
        return items
    }

    private fun bloquesMatrices(r: ResultadoRegresion): List<BloqueMatriz> = listOf(
        BloqueMatriz("Matriz del sistema normal", "C", Notacion.matriz(r.matrizC)),
        BloqueMatriz("Vector independiente", "b", Notacion.columna(r.vectorB)),
        BloqueMatriz("Inversa de C", "C⁻¹", Notacion.matriz(r.inversaC)),
        BloqueMatriz("Coeficientes", "a", Notacion.columna(r.coeficientes)),
    )

    private fun conclusion(r: ResultadoRegresion, sujeto: String): BloqueTexto {
        val r2Calidad = if (r.grado == null) r.r2Ajustado ?: r.r2 else r.r2
        val calidad = when { r2Calidad >= 0.95 -> "Ajuste muy bueno."; r2Calidad >= 0.8 -> "Ajuste bueno."; else -> "Ajuste débil." }
        val texto = "$sujeto explica el ${Formato.porcentaje(r.r2, 2)} de la variabilidad de y (R² = ${Formato.fijo(r.r2, 6)}). $calidad"
        return BloqueTexto(texto, TipoTexto.CONCLUSION, progreso = r2Calidad.coerceIn(0.0, 1.0))
    }

    private fun tablasInferencia(r: ResultadoRegresion): List<Bloque> {
        val errores = r.erroresCoeficientes
        val ts = r.estadisticosT
        val ps = r.valoresP
        val tabla = BloqueTabla("Coeficientes e inferencia", listOf("βⱼ", "se(βⱼ)", "t", "p", "¿significativo? (p < 0.05)"),
            r.coeficientes.indices.map { j ->
                val error = errores?.getOrNull(j); val t = ts?.getOrNull(j); val p = ps?.getOrNull(j)
                listOf(Celda.Num(r.coeficientes[j]),
                    if (error != null && error.isFinite()) Celda.Num(error) else Celda.Txt("—"),
                    if (t != null && t.isFinite()) Celda.Num(t) else Celda.Txt("—"),
                    if (p != null && p.isFinite()) Celda.Num(p) else Celda.Txt("—"),
                    Celda.Txt(if (p != null && p.isFinite() && p < 0.05) "Sí" else "No")) })
        val vif = r.vif?.let { v -> BloqueTabla("Factor de inflación de varianza (VIF)", listOf("Variable", "VIF", "Diagnóstico"), v.indices.map { j ->
            listOf(Celda.Txt("x${j + 1}"), Celda.Num(v[j]), Celda.Txt(when { v[j] > 10 -> "Alerta"; v[j] > 5 -> "Advertencia"; else -> "Sin alerta" }))
        }) }
        return listOfNotNull(tabla, vif)
    }

    private fun interpretaciones(r: ResultadoRegresion): List<Bloque> = (1 until r.coeficientes.size).map { j ->
        val p = r.valoresP?.getOrNull(j)
        BloqueTexto("Por cada unidad que aumenta x$j, y cambia en ${num(r.coeficientes[j])} unidades, manteniendo las demás variables constantes " +
            if (p != null && p.isFinite() && p < 0.05) "(significativo)" else "(no significativo)")
    }

    private fun conclusionMultiple(r: ResultadoRegresion): BloqueTexto {
        val significativas = (1 until r.coeficientes.size).filter { j -> r.valoresP?.getOrNull(j)?.let { it.isFinite() && it < 0.05 } == true }.map { "x$it" }
        val frase = "Este modelo explica el ${Formato.porcentaje(r.r2, 2)} de la variación de y. " +
            if (significativas.isEmpty()) "Las variables significativas son: ninguna es significativa. " else "Las variables significativas son: ${significativas.joinToString(", ")}. "
        val base = conclusion(r, "El modelo")
        return base.copy(texto = frase + base.texto)
    }
}
