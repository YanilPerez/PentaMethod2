package com.example.pentametod2.presentacion

import com.example.pentametod2.methods.AlgebraLineal
import com.example.pentametod2.methods.PasoNewton
import com.example.pentametod2.methods.ResultadoSistema
import com.example.pentametod2.model.Bloque
import com.example.pentametod2.model.BloqueGrupo
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.DatosExportables
import com.example.pentametod2.model.Formato
import com.example.pentametod2.model.ResultadoPaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Estado
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
import com.example.pentametod2.presentacion.Notacion.entreParentesisSiNegativo
import com.example.pentametod2.presentacion.Notacion.num
import com.example.pentametod2.presentacion.Notacion.sub
import com.example.pentametod2.presentacion.Notacion.sup
import kotlin.math.abs
import kotlin.math.max

object PresentadorSistemas {

    /** `expresiones[i]` es Gᵢ (xᵢ = Gᵢ(x)). `maxIter` solo se muestra (el método ya lo usó). */
    fun presentarPuntoFijo(
        r: ResultadoSistema,
        expresiones: List<String>,
        tolerancia: Double? = null,
        maxIter: Int? = null,
    ): SalidaMetodo {
        val sustituida = r.variables.indices.map { i -> "${r.variables[i]} = ${expresiones.getOrElse(i) { "?" }}" } +
            reglaDeParada(tolerancia)
        val formula = Formula(
            titulo = "Punto fijo para sistemas",
            general = listOf("x⁽ᵏ⁺¹⁾ = G(x⁽ᵏ⁾)", "E = ‖x⁽ᵏ⁺¹⁾ − x⁽ᵏ⁾‖∞"),
            sustituida = sustituida,
        )
        val filas = r.iteraciones.map { iter ->
            listOf<Celda>(Celda.Entero(iter.k)) + iter.x.map { v -> Celda.Num(v) } + celdaError(iter.error)
        }
        val tabla = BloqueTabla("Iteraciones", listOf("k") + r.variables + "E", filas)
        val pasos = pasosPuntoFijo(r, expresiones, tolerancia, maxIter)
        return salidaSistema(MetodoId.PUNTO_FIJO, r, formula, pasos + tabla, "‖G(x*) − x*‖∞", tolerancia)
            .copy(exportable = exportable(r, expresiones, tolerancia, maxIter))
    }

    /** `expresiones[i]` es Fᵢ, con Fᵢ(x) = 0. `maxIter` solo se muestra (el método ya lo usó). */
    fun presentarNewtonSistemas(
        r: ResultadoSistema,
        expresiones: List<String>,
        tolerancia: Double? = null,
        maxIter: Int? = null,
    ): SalidaMetodo {
        val sustituida = r.variables.indices.map { i -> "F${sub(i + 1)} = ${expresiones.getOrElse(i) { "?" }}" } +
            reglaDeParada(tolerancia)
        val formula = Formula(
            titulo = "Newton-Raphson para sistemas",
            general = listOf(
                "J(x⁽ᵏ⁾) · Δ = −F(x⁽ᵏ⁾)",
                "x⁽ᵏ⁺¹⁾ = x⁽ᵏ⁾ + Δ",
                "Jᵢⱼ ≈ ( Fᵢ(x + hⱼeⱼ) − Fᵢ(x − hⱼeⱼ) ) / (2hⱼ)   (Jacobiano numérico)",
                "E = ‖x⁽ᵏ⁺¹⁾ − x⁽ᵏ⁾‖∞",
            ),
            sustituida = sustituida,
        )
        val filas = r.iteraciones.map { iter ->
            listOf<Celda>(Celda.Entero(iter.k)) + iter.x.map { v -> Celda.Num(v) } +
                normaF(r, iter.k) + celdaError(iter.error)
        }
        val tabla = BloqueTabla("Iteraciones", listOf("k") + r.variables + listOf("‖F(x)‖∞", "E"), filas)
        val detalles = r.detalle.map { detalleNewton(it, r.variables) }
        val pasos = pasosNewton(r, expresiones, tolerancia, maxIter)
        return salidaSistema(MetodoId.NEWTON_SISTEMAS, r, formula, pasos + tabla + detalles, "‖F(x*)‖∞", tolerancia)
            .copy(exportable = exportable(r, expresiones, tolerancia, maxIter))
    }

    // ── Pasos ──

    internal fun pasosPuntoFijo(r: ResultadoSistema, expresiones: List<String>, tol: Double?, maxIter: Int?): List<BloquePaso> {
        val v = r.variables
        val p1 = BloquePaso(
            numero = 1,
            titulo = "Sistema y función de iteración",
            explicacion = "Cada ecuación está despejada como xᵢ = Gᵢ(x); se aplica G una y otra vez desde el punto inicial.",
            formulaGeneral = listOf("x⁽ᵏ⁺¹⁾ = G(x⁽ᵏ⁾)", "xᵢ⁽ᵏ⁺¹⁾ = Gᵢ(x₁⁽ᵏ⁾, …, xₙ⁽ᵏ⁾)"),
            conTusDatos = v.indices.map { i -> "${v[i]} = ${expresiones.getOrElse(i) { "?" }}" },
            resultados = listOf(ResultadoPaso("Variables", v.joinToString(", "))),
            resumenCorto = "x = G(x)",
            abiertoInicial = true,
        )
        val p3 = primeraIteracionPuntoFijo(r, expresiones)
        return listOf(p1, pasoCriterio(r, tol, maxIter, "x⁽ᵏ⁺¹⁾ − x⁽ᵏ⁾"), p3, pasoResultado(r, 4, "‖G(x*) − x*‖∞", tol))
    }

    internal fun pasosNewton(r: ResultadoSistema, expresiones: List<String>, tol: Double?, maxIter: Int?): List<BloquePaso> {
        val v = r.variables
        val x0 = r.iteraciones.first().x
        val primero = r.detalle.firstOrNull()
        val p1 = BloquePaso(
            numero = 1,
            titulo = "Sistema F(x) = 0",
            explicacion = "Cada ecuación está igualada a cero; se busca el punto que las anula todas a la vez.",
            formulaGeneral = listOf("F(x) = (F₁(x), …, Fₙ(x)) = 0"),
            conTusDatos = v.indices.map { i -> "F${sub(i + 1)}(${v.joinToString(", ")}) = ${expresiones.getOrElse(i) { "?" }} = 0" },
            resultados = listOf(ResultadoPaso("Variables", v.joinToString(", "))),
            resumenCorto = "F(x) = 0",
            abiertoInicial = true,
        )
        val h = x0.map { 6e-6 * max(1.0, abs(it)) }
        val p2 = BloquePaso(
            numero = 2,
            titulo = "Jacobiano",
            explicacion = "Las derivadas se aproximan numéricamente por diferencias centrales, igual que el motor.",
            formulaGeneral = listOf(
                "Jᵢⱼ = ∂Fᵢ/∂xⱼ",
                "Jᵢⱼ ≈ (Fᵢ(x + hⱼeⱼ) − Fᵢ(x − hⱼeⱼ)) / (2hⱼ)",
                "hⱼ = 6·10⁻⁶ · max(1, |xⱼ|)",
            ),
            conTusDatos = listOf(
                "En x⁽⁰⁾ = ${vectorDatos(x0)}:  h = (${h.joinToString(", ") { Formato.corto(it) }})",
            ) + (if (primero == null) listOf("No se llegó a evaluar el Jacobiano.") else emptyList()),
            matrices = if (primero == null) emptyList() else listOf(BloqueMatriz("Jacobiano en x⁽⁰⁾", "J(x⁽⁰⁾)", Notacion.matriz(primero.jacobiano))),
            resultados = listOf(ResultadoPaso("J(x⁽⁰⁾)", "matriz ${v.size}×${v.size}")),
            resumenCorto = "J(x)",
        )
        val criterio = listOfNotNull(tol?.let { "tol = ${dato(it)}" }, maxIter?.let { "máximo de iteraciones = $it" })
        val p3 = BloquePaso(
            numero = 3,
            titulo = "Método de Newton",
            explicacion = "En cada iteración se resuelve un sistema lineal para la corrección Δ y se suma al punto actual.",
            formulaGeneral = listOf("J(xₖ) · Δ = −F(xₖ)", "xₖ₊₁ = xₖ + Δ", "E = ‖Δ‖∞ = ‖xₖ₊₁ − xₖ‖∞"),
            conTusDatos = listOf("J·Δ = −F se resuelve por eliminación de Gauss en cada iteración") +
                (if (criterio.isEmpty()) emptyList() else listOf(criterio.joinToString(",  "))),
            resultados = listOf(
                if (tol != null) ResultadoPaso("tol", dato(tol), tol) else ResultadoPaso("Criterio", "E < tol"),
            ),
            resumenCorto = "J·Δ = −F",
        )
        return listOf(p1, p2, p3, primeraIteracionNewton(r), pasoResultado(r, 5, "‖F(x*)‖∞", tol))
    }

    /** Punto fijo, paso 2: x⁽⁰⁾, tolerancia, máximo de iteraciones y el error. */
    private fun pasoCriterio(r: ResultadoSistema, tol: Double?, maxIter: Int?, diferencia: String): BloquePaso {
        val x0 = r.iteraciones.first().x
        val lineas = mutableListOf("x⁽⁰⁾ = ${vectorDatos(x0)}")
        if (tol != null) lineas += "tol = ${dato(tol)}"
        if (maxIter != null) lineas += "máximo de iteraciones = $maxIter"
        val resultados = mutableListOf(ResultadoPaso("x⁽⁰⁾", vectorDatos(x0)))
        if (tol != null) resultados += ResultadoPaso("tol", dato(tol), tol)
        return BloquePaso(
            numero = 2,
            titulo = "Punto inicial y criterio",
            explicacion = "Se parte de x⁽⁰⁾ y se para cuando dos iteraciones seguidas difieren menos que la tolerancia.",
            formulaGeneral = listOf("E = ‖$diferencia‖∞ = máxᵢ |xᵢ⁽ᵏ⁺¹⁾ − xᵢ⁽ᵏ⁾|", "se detiene cuando E < tol"),
            conTusDatos = lineas,
            resultados = resultados,
            resumenCorto = if (tol != null) "tol = ${dato(tol)}" else "x⁽⁰⁾ = ${vectorDatos(x0)}",
        )
    }

    private fun primeraIteracionPuntoFijo(r: ResultadoSistema, expresiones: List<String>): BloquePaso {
        val v = r.variables
        val x0 = r.iteraciones.first().x
        val it1 = r.iteraciones.getOrNull(1)
        val comun = "Se evalúa cada Gᵢ en x⁽⁰⁾ = ${vectorDatos(x0)}"
        if (it1 == null) {
            return BloquePaso(
                numero = 3, titulo = "Primera iteración", explicacion = "$comun.",
                formulaGeneral = listOf("x⁽¹⁾ = G(x⁽⁰⁾)", "E₁ = ‖x⁽¹⁾ − x⁽⁰⁾‖∞"),
                conTusDatos = listOf("No se llegó a calcular x⁽¹⁾."),
                resultados = listOf(ResultadoPaso("x⁽¹⁾", "no calculado")), resumenCorto = "—",
            )
        }
        val e1 = it1.error ?: 0.0
        val lineas = v.indices.map { i ->
            "${v[i]}⁽¹⁾ = G${sub(i + 1)}(x⁽⁰⁾) = ${expresiones.getOrElse(i) { "?" }} = ${num(it1.x[i])}"
        } + ("E₁ = máx(" + v.indices.joinToString(", ") { i -> "|${num(it1.x[i])} − ${entreParentesisSiNegativo(dato(x0[i]))}|" } + ") = ${num(e1)}")
        return BloquePaso(
            numero = 3,
            titulo = "Primera iteración",
            explicacion = "$comun: así se obtiene x⁽¹⁾ y su error.",
            formulaGeneral = listOf("x⁽¹⁾ = G(x⁽⁰⁾)", "E₁ = ‖x⁽¹⁾ − x⁽⁰⁾‖∞"),
            conTusDatos = lineas,
            resultados = v.indices.map { i -> ResultadoPaso("${v[i]}⁽¹⁾", num(it1.x[i]), it1.x[i]) } + ResultadoPaso("E₁", num(e1), e1),
            resumenCorto = num(e1),
        )
    }

    private fun primeraIteracionNewton(r: ResultadoSistema): BloquePaso {
        val v = r.variables
        val p = r.detalle.firstOrNull()
        val it1 = r.iteraciones.getOrNull(1)
        val formula = listOf("J(x⁽⁰⁾) · Δ = −F(x⁽⁰⁾)", "x⁽¹⁾ = x⁽⁰⁾ + Δ", "E₁ = ‖Δ‖∞")
        if (p == null) {
            return BloquePaso(
                numero = 4, titulo = "Primera iteración", explicacion = "No se llegó a evaluar F en x⁽⁰⁾.",
                formulaGeneral = formula, conTusDatos = listOf(r.mensaje),
                resultados = listOf(ResultadoPaso("x⁽¹⁾", "no calculado")), resumenCorto = "—",
            )
        }
        val matrices = mutableListOf(
            BloqueMatriz("F en x⁽⁰⁾", "F(x⁽⁰⁾)", Notacion.columna(p.fx)),
            BloqueMatriz("Jacobiano en x⁽⁰⁾", "J(x⁽⁰⁾)", Notacion.matriz(p.jacobiano)),
        )
        val delta = p.delta
        val lineas = mutableListOf("Se evalúa en x⁽⁰⁾: ${Notacion.asignaciones(v, p.x)}")
        val resultados = mutableListOf<ResultadoPaso>()
        if (delta == null || it1 == null) {
            lineas += "El Jacobiano es singular: no se puede calcular Δ."
            resultados += ResultadoPaso("Δ", "no calculado")
        } else {
            matrices += BloqueMatriz("Solución de J·Δ = −F", "Δ", Notacion.columna(delta))
            val e1 = it1.error ?: 0.0
            lineas += "x⁽¹⁾ = x⁽⁰⁾ + Δ = ${Notacion.vector(it1.x)}"
            lineas += "E₁ = ‖Δ‖∞ = ${num(e1)}"
            resultados += v.indices.map { i -> ResultadoPaso("${v[i]}⁽¹⁾", num(it1.x[i]), it1.x[i]) }
            resultados += ResultadoPaso("E₁", num(e1), e1)
        }
        return BloquePaso(
            numero = 4,
            titulo = "Primera iteración",
            explicacion = "Con F y J evaluados en x⁽⁰⁾ se resuelve J·Δ = −F y se obtiene x⁽¹⁾.",
            formulaGeneral = formula,
            conTusDatos = lineas,
            matrices = matrices,
            resultados = resultados,
            resumenCorto = it1?.error?.let { num(it) } ?: "—",
        )
    }

    /** Último paso (destacado): solución, iteraciones y error final; o, si no convergió, el aviso del método. */
    private fun pasoResultado(r: ResultadoSistema, numero: Int, etiquetaResidual: String, tol: Double?): BloquePaso {
        val v = r.variables
        val ultimo = r.iteraciones.last()
        val sol = r.solucion ?: ultimo.x
        val e = ultimo.error
        // Si convergió, la frase se arma aquí para escribir la tolerancia como la escribió el alumno (1e-8);
        // si no, se usa el aviso del método, que explica qué pasó.
        val primera = if (r.convergio && tol != null) "Convergió en ${ultimo.k} iteraciones (E < ${dato(tol)})" else r.mensaje
        val lineas = mutableListOf(primera)
        lineas += (if (r.convergio) "Solución: " else "Último valor: ") + Notacion.asignaciones(v, sol)
        lineas += "Iteraciones: ${ultimo.k}"
        lineas += "Error final E = ${if (e == null) "—" else num(e)}"
        r.residual?.let { lineas += "Residual $etiquetaResidual = ${num(it)}" }
        val resultados = v.indices.map { i -> ResultadoPaso(if (r.convergio) "${v[i]}*" else v[i], num(sol[i]), sol[i]) } +
            ResultadoPaso("Iteraciones", ultimo.k.toString(), ultimo.k.toDouble()) +
            (if (e == null) ResultadoPaso("Error final", "—") else ResultadoPaso("Error final", num(e), e))
        return BloquePaso(
            numero = numero,
            titulo = "Resultado",
            explicacion = if (r.convergio) {
                "El método convergió: la última diferencia es menor que la tolerancia."
            } else {
                "El método no convergió: se muestra el último valor calculado."
            },
            formulaGeneral = listOf("x* ≈ x⁽ᵏ⁾ cuando E < tol"),
            conTusDatos = lineas,
            resultados = resultados,
            resumenCorto = if (r.convergio) "k = ${ultimo.k}" else "No convergió",
            destacado = true,
        )
    }

    private fun vectorDatos(x: DoubleArray): String = x.joinToString(", ", "(", ")") { dato(it) }

    private fun exportable(r: ResultadoSistema, expresiones: List<String>, tol: Double?, maxIter: Int?) =
        DatosExportables.Sistema(r.variables, expresiones, r.iteraciones.first().x.toList(), tol, maxIter)

    // ── auxiliares ──

    private fun salidaSistema(
        metodo: MetodoId,
        r: ResultadoSistema,
        formula: Formula,
        bloques: List<Bloque>,
        etiquetaResidual: String,
        tolerancia: Double?,
    ): SalidaMetodo {
        val tipoMensaje = if (r.convergio) TipoTexto.CONCLUSION else TipoTexto.ADVERTENCIA
        return SalidaMetodo(
            metodo = metodo,
            estado = if (r.convergio) Estado.OK else Estado.ERROR,
            resumen = resumenSistema(r, etiquetaResidual),
            formulas = listOf(formula),
            grafica = graficaError(r, tolerancia),
            bloques = bloques + BloqueTexto(r.mensaje, tipoMensaje),
            advertencias = emptyList(),
            error = if (r.convergio) null else r.mensaje,
        )
    }

    private fun reglaDeParada(tol: Double?): List<String> =
        if (tol == null) emptyList() else listOf("Se detiene cuando E < ${dato(tol)}")

    private fun celdaError(e: Double?): Celda = if (e == null) Celda.Vacia else Celda.Num(e)

    /** ‖F(x⁽ᵏ⁾)‖∞: sale del paso k+1 (que evaluó F en x⁽ᵏ⁾) o, en la última fila, del residual. */
    private fun normaF(r: ResultadoSistema, k: Int): Celda {
        val paso = r.detalle.firstOrNull { it.k == k + 1 }
        if (paso != null) return Celda.Num(AlgebraLineal.normaInf(paso.fx))
        val residual = r.residual
        if (r.convergio && residual != null && k == r.iteraciones.last().k) return Celda.Num(residual)
        return Celda.Vacia
    }

    private fun detalleNewton(p: PasoNewton, variables: List<String>): BloqueGrupo {
        val anterior = "x⁽${sup(p.k - 1)}⁾"
        val bloques = mutableListOf<Bloque>(
            BloqueTexto("Se evalúa en $anterior: ${Notacion.asignaciones(variables, p.x)}"),
            BloqueMatriz("Jacobiano en $anterior", "J", Notacion.matriz(p.jacobiano)),
            BloqueMatriz("F en $anterior", "F(x)", Notacion.columna(p.fx)),
        )
        bloques += bloqueDelta(p)
        return BloqueGrupo("Detalle de la iteración ${p.k}", bloques, colapsado = true)
    }

    private fun bloqueDelta(p: PasoNewton): Bloque {
        val delta = p.delta ?: return BloqueTexto("El Jacobiano es singular: no se puede calcular Δ.", TipoTexto.ADVERTENCIA)
        return BloqueMatriz("Solución de J·Δ = −F", "Δ", Notacion.columna(delta))
    }

    private fun resumenSistema(r: ResultadoSistema, etiquetaResidual: String): List<ItemResumen> {
        val items = mutableListOf<ItemResumen>()
        val sol = r.solucion
        if (sol != null) {
            items += ItemResumen("Solución", Notacion.asignaciones(r.variables, sol))
        } else {
            items += ItemResumen("Último valor", Notacion.asignaciones(r.variables, r.iteraciones.last().x))
        }
        items += ItemResumen("Iteraciones", r.iteraciones.last().k.toString())
        val e = r.iteraciones.last().error
        items += ItemResumen("Error final", if (e == null) "—" else num(e))
        val residual = r.residual
        if (residual != null) items += ItemResumen("Residual $etiquetaResidual", num(residual))
        items += ItemResumen("Convergió", if (r.convergio) "Sí" else "No")
        return items
    }

    private fun graficaError(r: ResultadoSistema, tolerancia: Double?): Grafica {
        val puntos = mutableListOf<Punto>()
        for (iter in r.iteraciones) {
            val e = iter.error ?: continue
            if (e > 0 && e.isFinite()) puntos += Punto(iter.k.toDouble(), e)
        }
        val series = mutableListOf(Serie("E(k)", TipoSerie.LINEA_CON_PUNTOS, puntos))
        val lineaTolerancia = serieTolerancia(puntos, tolerancia)
        if (lineaTolerancia != null) series += lineaTolerancia
        return Grafica(
            titulo = "Error por iteración",
            ejeX = "k",
            ejeY = "E",
            series = series,
            escalaLogY = true,
        )
    }

    const val NOMBRE_TOLERANCIA = "Tolerancia"

    /** Línea horizontal y = tolerancia sobre el rango de k de la serie de error (discontinua). */
    private fun serieTolerancia(puntos: List<Punto>, tolerancia: Double?): Serie? {
        if (tolerancia == null || !(tolerancia > 0) || puntos.isEmpty()) return null
        val desde = puntos.minOf { it.x }
        val hasta = puntos.maxOf { it.x }
        return Serie(
            NOMBRE_TOLERANCIA,
            TipoSerie.LINEA,
            listOf(Punto(desde, tolerancia), Punto(hasta, tolerancia)),
            discontinua = true,
        )
    }
}
