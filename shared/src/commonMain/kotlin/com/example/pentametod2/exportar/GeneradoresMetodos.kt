package com.example.pentametod2.exportar

import com.example.pentametod2.catalogo.Entrada
import com.example.pentametod2.catalogo.EntradaSistema
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.DatosExportables
import com.example.pentametod2.model.Formato
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.exportar.Exportaciones.hojaEntrada
import com.example.pentametod2.exportar.Exportaciones.hojaMatrices
import com.example.pentametod2.exportar.Exportaciones.hojaResultados
import com.example.pentametod2.exportar.Exportaciones.hojaTabla
import com.example.pentametod2.exportar.Exportaciones.pasos
import com.example.pentametod2.exportar.Exportaciones.tabla

// Generadores de cada método. Los comentarios numerados del código Octave salen de los pasos de la
// SalidaMetodo ("% 1. Nodos usados", …), así coinciden siempre con la pantalla.

private const val CABECERA = "% Pega este código en Octave o en Octave Online."

private fun numero(v: Double): String = Formato.corto(v)

/** Vector fila de Octave: [1 2 3]. */
private fun fila(v: List<Double>): String = v.joinToString(" ", "[", "]") { numero(it) }

/** Vector columna de Octave: [1; 2; 3]. */
private fun columna(v: List<Double>): String = v.joinToString("; ", "[", "]") { numero(it) }

/** "% 2. Polinomio de Lagrange" del paso `numero`. */
private fun comentario(s: SalidaMetodo, numero: Int): String {
    val p = pasos(s).firstOrNull { it.numero == numero } ?: return "% $numero."
    return "% ${p.numero}. ${p.titulo}"
}

private fun texto(s: String) = CeldaX.Texto(s)
private fun num(v: Double, estilo: Int = EstiloX.NORMAL) = CeldaX.Numero(v, estilo)
private fun entero(v: Int) = CeldaX.Numero(v.toDouble(), EstiloX.ENTERO)

// ── Regresión polinomial (Fase 7) ──

object GeneradorRegresionPolinomial : GeneradorMetodo {
    private fun datos(s: SalidaMetodo) = s.exportable as DatosExportables.RegresionPolinomial

    override fun nombreBase(s: SalidaMetodo) = "regresion_polinomial_grado${datos(s).grado}"

    override fun hojas(s: SalidaMetodo, entrada: Entrada?): List<HojaX> {
        val d = datos(s)
        val pasos = pasos(s)
        return listOf(hojaTabla(tabla(s, "Datos y sumatorias"))) +
            pasos.filter { it.matrices.isNotEmpty() }.map { hojaMatrices(it.titulo, it.matrices) } +
            hojaResultados(pasos) +
            hojaEntrada(
                listOf("Grado" to entero(d.grado), "x a predecir" to (d.xPredecir?.let { num(it) } ?: texto("—"))),
                listOf("i", "x", "y"),
                d.x.indices.map { listOf(entero(it + 1), num(d.x[it]), num(d.y[it])) },
            )
    }

    override fun codigoOctave(s: SalidaMetodo, entrada: Entrada?): String {
        val d = datos(s)
        return GeneradorOctave.regresionPolinomial(d.x, d.y, d.grado, d.xPredecir)
    }
}

// ── Regresión múltiple ──

object GeneradorRegresionMultiple : GeneradorMetodo {
    private fun datos(s: SalidaMetodo) = s.exportable as DatosExportables.RegresionMultiple
    private fun k(d: DatosExportables.RegresionMultiple) = d.x.firstOrNull()?.size ?: 0

    override fun nombreBase(s: SalidaMetodo) = "regresion_multiple_k${k(datos(s))}"

    override fun hojas(s: SalidaMetodo, entrada: Entrada?): List<HojaX> {
        val d = datos(s)
        val k = k(d)
        val sistema = pasos(s).first { it.numero == 1 }
        val prediccion = d.xPredecir?.joinToString(", ", "(", ")") { numero(it) } ?: "—"
        return listOf(
            hojaTabla(tabla(s, "Datos y residuos"), "Datos y sumatorias"),
            *s.bloques.filterIsInstance<com.example.pentametod2.model.BloqueTabla>().filter { it.titulo == "Coeficientes e inferencia" || it.titulo.contains("VIF") }.map { hojaTabla(it, it.titulo.take(31)) }.toTypedArray(),
            hojaMatrices("Sistema normal", sistema.matrices),
            hojaResultados(pasos(s)),
            hojaEntrada(
                listOf("Variables (k)" to entero(k), "Punto a predecir" to texto(prediccion)),
                listOf("i") + (1..k).map { "x$it" } + "y",
                d.y.indices.map { i -> listOf(entero(i + 1)) + d.x[i].map { num(it) } + num(d.y[i]) },
            ),
        )
    }

    override fun codigoOctave(s: SalidaMetodo, entrada: Entrada?): String {
        val d = datos(s)
        val k = k(d)
        val l = mutableListOf("% Regresión lineal múltiple por mínimos cuadrados (exportado desde Pentametod2)", CABECERA, "")
        for (j in 0 until k) l += "x${j + 1} = ${fila(d.x.map { it[j] })}';"
        l += listOf(
            "y = ${fila(d.y)}';",
            "n = length(y);",
            "k = $k;",
            "",
            comentario(s, 1),
            "X = [ones(n,1) ${(1..k).joinToString(" ") { "x$it" }}];",
            "beta = (X'*X)\\(X'*y);",
            "",
            comentario(s, 2),
            "yhat = X*beta;",
            "",
            comentario(s, 3),
            "Q = sum((yhat - y).^2);",
            "",
            comentario(s, 4),
            "ybar = mean(y);",
            "Syy = sum((y - ybar).^2);",
            "",
            comentario(s, 5),
            "R2 = (Syy - Q)/Syy;",
            "r = sqrt(R2);",
            "",
            comentario(s, 6),
            "S = sqrt(Q/(n-(k+1)));",
            "gl = n-k-1;",
            "if gl > 0",
            "  R2aj = 1-(1-R2)*(n-1)/gl;",
            "  SSR = Syy-Q;",
            "  Fglobal = (SSR/k)/(Q/gl);",
            "  pF = betainc(gl/(gl+k*Fglobal), gl/2, k/2);",
            "  se = S*sqrt(diag(inv(X'*X)));",
            "  tcoef = beta./se;",
            "  pcoef = betainc(gl./(gl+tcoef.^2), gl/2, 0.5);",
            "  VIF = diag(inv(corrcoef([" + (1..k).joinToString(" ") { "x$it" } + "])));",
            "endif",
        )
        if (d.xPredecir != null) {
            l += listOf("", comentario(s, 7), "xp = ${fila(d.xPredecir)};", "yp = [1 xp]*beta;")
        }
        l += listOf(
            "",
            "% Resultados",
            "for j = 1:k+1",
            "  printf(\"beta%d = %.6f\\n\", j-1, beta(j));",
            "endfor",
            "printf(\"Q = %.6f\\n\", Q);",
            "printf(\"R2 = %.6f\\n\", R2);",
            "printf(\"r = %.6f\\n\", r);",
            "printf(\"S = %.6f\\n\", S);",
            "if exist('R2aj','var'), printf(\"R2aj = %.6f\\n\", R2aj); endif",
            "if exist('Fglobal','var'), printf(\"F global = %.6f, p = %.6f\\n\", Fglobal, pF); endif",
            "if exist('se','var'), disp('Errores estándar, t y p:'); disp([beta se tcoef pcoef]); disp('VIF:'); disp(VIF); endif",
        )
        if (d.xPredecir != null) l += "printf(\"Prediccion = %.6f\\n\", yp);"
        return l.joinToString("\n") + "\n"
    }
}

// ── Interpolación ──

private fun datosInterpolacion(s: SalidaMetodo) = s.exportable as DatosExportables.Interpolacion

private fun entradaInterpolacion(d: DatosExportables.Interpolacion, columnaY: String): HojaX = hojaEntrada(
    listOf(
        "x a evaluar" to num(d.x),
        "Modo" to texto(if (d.automatico) "Automático" else "Manual"),
        "Grado" to entero(d.grado),
        "Nodos dados" to entero(d.nodosDados),
    ),
    listOf("k", "xₖ", columnaY),
    d.xs.indices.map { listOf(entero(it), num(d.xs[it]), num(d.ys[it])) },
)

/** Datos de los nodos, x y el rango de la gráfica (comunes a Lagrange y Newton). */
private fun datosOctaveInterpolacion(d: DatosExportables.Interpolacion) = listOf(
    "xs = ${fila(d.xs)};",
    "ys = ${fila(d.ys)};",
    "x = ${numero(d.x)};",
    "n = length(xs);",
)

private val RANGO_GRAFICA = listOf(
    "a = min([xs x]);",
    "b = max([xs x]);",
    "margen = 0.1*(b - a);",
    "t = linspace(a - margen, b + margen, 200);",
)

object GeneradorLagrange : GeneradorMetodo {
    override fun nombreBase(s: SalidaMetodo) = if (s.comparacionLagrange != null) GeneradorComparacionLagrange.nombreBase(s) else "lagrange_grado${datosInterpolacion(s).grado}"

    override fun hojas(s: SalidaMetodo, entrada: Entrada?): List<HojaX> = if (s.comparacionLagrange != null) GeneradorComparacionLagrange.hojas(s, entrada) else listOf(
        hojaTabla(tabla(s, "Nodos y polinomios de Lagrange"), "Lagrange"),
        hojaResultados(pasos(s)),
        entradaInterpolacion(datosInterpolacion(s), "yₖ"),
    )

    override fun codigoOctave(s: SalidaMetodo, entrada: Entrada?): String {
        if (s.comparacionLagrange != null) return GeneradorComparacionLagrange.codigoOctave(s, entrada)
        val d = datosInterpolacion(s)
        val l = mutableListOf("% Polinomio de Lagrange (exportado desde Pentametod2)", CABECERA, "")
        l += comentario(s, 1)
        l += datosOctaveInterpolacion(d)
        l += listOf(
            "",
            comentario(s, 2),
            "% P(x) = suma de ys(k)*Lk(x), con Lk(x) = producto de (x - xs(j))/(xs(k) - xs(j)), j distinto de k",
            "",
            comentario(s, 3),
            "L = zeros(1, n);",
            "for k = 1:n",
            "  L(k) = 1;",
            "  for j = 1:n",
            "    if j ~= k",
            "      L(k) = L(k)*(x - xs(j))/(xs(k) - xs(j));",
            "    endif",
            "  endfor",
            "  printf(\"L%d(%g) = %.6f\\n\", k-1, x, L(k));",
            "endfor",
            "",
            comentario(s, 4),
            "P = sum(ys.*L);",
            "printf(\"Suma de Lk = %.6f\\n\", sum(L));",
            "printf(\"P(%g) = %.6f\\n\", x, P);",
            "",
            "% Gráfica: nodos, polinomio y punto evaluado",
        )
        l += RANGO_GRAFICA
        l += listOf(
            "Pt = zeros(size(t));",
            "for k = 1:n",
            "  Lk = ones(size(t));",
            "  for j = 1:n",
            "    if j ~= k",
            "      Lk = Lk.*(t - xs(j))/(xs(k) - xs(j));",
            "    endif",
            "  endfor",
            "  Pt = Pt + ys(k)*Lk;",
            "endfor",
            "plot(xs, ys, \"o\", t, Pt, \"-\", x, P, \"*\");",
            "legend(\"Nodos\", \"P(x)\", \"x evaluado\");",
            "xlabel(\"x\"); ylabel(\"y\"); grid on;",
        )
        return l.joinToString("\n") + "\n"
    }
}

object GeneradorNewtonInterpolacion : GeneradorMetodo {
    override fun nombreBase(s: SalidaMetodo) = "newton_dif_divididas_grado${datosInterpolacion(s).grado}"

    override fun hojas(s: SalidaMetodo, entrada: Entrada?): List<HojaX> {
        val d = datosInterpolacion(s)
        val siete = (d.xs + d.ys).any { numero(it).substringAfter('.', "").length >= 7 }
        return listOf(
            hojaTabla(tabla(s, "Tabla de diferencias divididas"), "Diferencias divididas", if (siete) EstiloX.DECIMAL7 else EstiloX.DECIMAL),
            hojaTerminos(s),
            hojaResultados(pasos(s)),
            entradaInterpolacion(d, "f[xₖ]"),
        )
    }

    /** k | coeficiente | factor | término | suma acumulada; Σ (=SUM) de los términos = P(x). */
    private fun hojaTerminos(s: SalidaMetodo): HojaX {
        val t: BloqueTabla = tabla(s, "Términos de Newton")
        val datos = t.filas.dropLast(1)
        val acumulados = pasos(s).last().resultados.map { it.valor ?: Double.NaN }
        val filas = mutableListOf<List<CeldaX>>(
            listOf("k", "Coeficiente f[x₀,…,xₖ]", "Factor", "Término", "Suma acumulada").map { CeldaX.Texto(it, EstiloX.ENCABEZADO) },
        )
        datos.forEachIndexed { i, f ->
            filas += listOf(
                celda(f[0]), celda(f[1]), celda(f[2]), celda(f[4]),
                acumulados.getOrNull(i)?.let { num(it, EstiloX.DECIMAL) } ?: CeldaX.Vacia,
            )
        }
        val total = (t.filas.last()[4] as? Celda.Num)?.v ?: Double.NaN
        filas += listOf(
            CeldaX.Texto("Σ", EstiloX.TOTAL_TEXTO), CeldaX.Texto("", EstiloX.TOTAL_TEXTO), CeldaX.Texto("", EstiloX.TOTAL_TEXTO),
            CeldaX.Formula("SUM(D2:D${datos.size + 1})", total), CeldaX.Texto("", EstiloX.TOTAL_TEXTO),
        )
        return HojaX("Términos", filas)
    }

    private fun celda(c: Celda): CeldaX = when (c) {
        is Celda.Num -> num(c.v, EstiloX.DECIMAL)
        is Celda.Entero -> entero(c.v)
        is Celda.Txt -> texto(c.s)
        Celda.Vacia -> CeldaX.Vacia
    }

    override fun codigoOctave(s: SalidaMetodo, entrada: Entrada?): String {
        val d = datosInterpolacion(s)
        val l = mutableListOf("% Newton con diferencias divididas (exportado desde Pentametod2)", CABECERA, "")
        l += comentario(s, 1)
        l += datosOctaveInterpolacion(d)
        l += listOf(
            "",
            comentario(s, 2),
            "D = zeros(n, n);",
            "D(:, 1) = ys';",
            "for j = 2:n",
            "  for i = 1:n-j+1",
            "    D(i, j) = (D(i+1, j-1) - D(i, j-1))/(xs(i+j-1) - xs(i));",
            "  endfor",
            "endfor",
            "disp(D);",
            "",
            comentario(s, 3),
            "c = D(1, :);",
            "",
            comentario(s, 4),
            comentario(s, 5),
            "P = 0;",
            "fac = 1;",
            "for k = 1:n",
            "  if k > 1",
            "    fac = fac*(x - xs(k-1));",
            "  endif",
            "  termino = c(k)*fac;",
            "  P = P + termino;",
            "  printf(\"P%d(%g) = %.6f   (termino %.6f)\\n\", k-1, x, P, termino);",
            "endfor",
            "",
            "% Gráfica: nodos, polinomio y punto evaluado",
        )
        l += RANGO_GRAFICA
        l += listOf(
            "Pt = c(1)*ones(size(t));",
            "f = ones(size(t));",
            "for k = 2:n",
            "  f = f.*(t - xs(k-1));",
            "  Pt = Pt + c(k)*f;",
            "endfor",
            "plot(xs, ys, \"o\", t, Pt, \"-\", x, P, \"*\");",
            "legend(\"Nodos\", \"P(x)\", \"x evaluado\");",
            "xlabel(\"x\"); ylabel(\"y\"); grid on;",
        )
        return l.joinToString("\n") + "\n"
    }
}

// ── Sistemas ──

private fun datosSistema(s: SalidaMetodo) = s.exportable as DatosExportables.Sistema

/** Funciones tal como las escribió el alumno: de la entrada original si llegó, si no de la salida. */
private fun expresiones(d: DatosExportables.Sistema, entrada: Entrada?): List<String> =
    (entrada as? EntradaSistema)?.ecuaciones?.map { it.trim() }?.takeIf { it.size == d.variables.size } ?: d.expresiones

private fun entradaSistema(d: DatosExportables.Sistema, entrada: Entrada?, prefijo: String): HojaX {
    val ecs = expresiones(d, entrada)
    val pares = ecs.mapIndexed { i, e -> "$prefijo${i + 1}" to texto(e) } + listOf(
        "Variables" to texto(d.variables.joinToString(", ")),
        "x⁽⁰⁾" to texto(d.x0.joinToString(", ", "(", ")") { numero(it) }),
        "Tolerancia" to (d.tolerancia?.let { num(it) } ?: texto("—")),
        "Máx. iteraciones" to (d.maxIter?.let { entero(it) } ?: texto("—")),
    )
    return hojaEntrada(pares)
}

/** `G = @(v) [g1(v(1), v(2)); g2(v(1), v(2))];` */
private fun vectorial(nombre: String, prefijo: String, n: Int): String {
    val args = (1..n).joinToString(", ") { "v($it)" }
    return "$nombre = @(v) [${(1..n).joinToString("; ") { "$prefijo$it($args)" }}];"
}

private fun criterio(d: DatosExportables.Sistema) = listOf(
    "xk = ${columna(d.x0)};",
    "tol = ${numero(d.tolerancia ?: 1e-6)};",
    "maxIter = ${d.maxIter ?: 50};",
)

private fun resultadosSistema(d: DatosExportables.Sistema): List<String> = listOf(
    "",
    "% Resultados",
    "if convergio",
    "  printf(\"Convergio en %d iteraciones\\n\", k);",
    "else",
    "  printf(\"No convergio en %d iteraciones\\n\", k);",
    "endif",
) + d.variables.mapIndexed { i, v -> "printf(\"$v = %.6f\\n\", xk(${i + 1}));" } + listOf(
    "printf(\"Error final E = %.6e\\n\", E);",
    "",
    "% Gráfica del error por iteración",
    "semilogy(1:numel(errores), errores, \"-o\");",
    "xlabel(\"k\"); ylabel(\"E\"); grid on;",
)

object GeneradorPuntoFijo : GeneradorMetodo {
    override fun nombreBase(s: SalidaMetodo) = "punto_fijo_sistemas"

    override fun hojas(s: SalidaMetodo, entrada: Entrada?): List<HojaX> = listOf(
        hojaTabla(tabla(s, "Iteraciones")),
        hojaResultados(pasos(s)),
        entradaSistema(datosSistema(s), entrada, "G"),
    )

    override fun codigoOctave(s: SalidaMetodo, entrada: Entrada?): String {
        val d = datosSistema(s)
        val n = d.variables.size
        val ecs = expresiones(d, entrada)
        val l = mutableListOf("% Punto fijo para sistemas (exportado desde Pentametod2)", CABECERA, "")
        l += comentario(s, 1)
        ecs.forEachIndexed { i, e -> l += TraductorOctave.funcionAnonima("g${i + 1}", e, d.variables) }
        l += vectorial("G", "g", n)
        l += listOf("", comentario(s, 2))
        l += criterio(d)
        l += listOf(
            "",
            comentario(s, 3),
            "% Es la vuelta k = 1 del bucle (la primera fila que se imprime).",
            comentario(s, 4),
            "errores = [];",
            "convergio = false;",
            "for k = 1:maxIter",
            "  xn = G(xk);",
            "  E = norm(xn - xk, Inf);",
            "  errores(end+1) = E;",
            "  printf(\"k = %d:\", k); printf(\" %.6f\", xn); printf(\"   E = %.6e\\n\", E);",
            "  xk = xn;",
            "  if E < tol",
            "    convergio = true;",
            "    break;",
            "  endif",
            "endfor",
        )
        l += resultadosSistema(d)
        return l.joinToString("\n") + "\n"
    }
}

object GeneradorNewtonSistemas : GeneradorMetodo {
    override fun nombreBase(s: SalidaMetodo) = "newton_raphson_sistemas"

    override fun hojas(s: SalidaMetodo, entrada: Entrada?): List<HojaX> {
        val primera = pasos(s).firstOrNull { it.titulo == "Primera iteración" }
        return listOfNotNull(
            hojaTabla(tabla(s, "Iteraciones")),
            primera?.takeIf { it.matrices.isNotEmpty() }?.let { hojaMatrices("Primera iteración", it.matrices) },
            hojaResultados(pasos(s)),
            entradaSistema(datosSistema(s), entrada, "F"),
        )
    }

    override fun codigoOctave(s: SalidaMetodo, entrada: Entrada?): String {
        val d = datosSistema(s)
        val n = d.variables.size
        val ecs = expresiones(d, entrada)
        val l = mutableListOf("% Newton-Raphson para sistemas (exportado desde Pentametod2)", CABECERA, "")
        l += comentario(s, 1)
        ecs.forEachIndexed { i, e -> l += TraductorOctave.funcionAnonima("F${i + 1}", e, d.variables) }
        l += vectorial("F", "F", n)
        l += listOf(
            "",
            comentario(s, 2),
            "% Diferencias centrales con h = 6e-6*max(1, |xj|), igual que la app (se calcula dentro del bucle).",
            "",
            comentario(s, 3),
        )
        l += criterio(d)
        l += listOf(
            "n = numel(xk);",
            "",
            comentario(s, 4),
            "% Es la vuelta k = 1 del bucle: se muestran F(x0), J(x0) y delta.",
            comentario(s, 5),
            "errores = [];",
            "convergio = false;",
            "for k = 1:maxIter",
            "  Fx = F(xk);",
            "  J = zeros(n);",
            "  for j = 1:n",
            "    h = 6e-6*max(1, abs(xk(j)));",
            "    ej = zeros(n, 1);",
            "    ej(j) = h;",
            "    J(:, j) = (F(xk + ej) - F(xk - ej))/(2*h);",
            "  endfor",
            "  delta = J\\(-Fx);",
            "  if k == 1",
            "    disp(\"F(x0) =\"); disp(Fx);",
            "    disp(\"J(x0) =\"); disp(J);",
            "    disp(\"delta =\"); disp(delta);",
            "  endif",
            "  xk = xk + delta;",
            "  E = norm(delta, Inf);",
            "  errores(end+1) = E;",
            "  printf(\"k = %d:\", k); printf(\" %.6f\", xk); printf(\"   E = %.6e\\n\", E);",
            "  if E < tol",
            "    convergio = true;",
            "    break;",
            "  endif",
            "endfor",
        )
        l += resultadosSistema(d)
        return l.joinToString("\n") + "\n"
    }
}
