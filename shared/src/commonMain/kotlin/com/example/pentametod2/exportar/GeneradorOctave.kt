package com.example.pentametod2.exportar

import com.example.pentametod2.model.Formato

/**
 * Código Octave (.txt) que rehace el cálculo. Los comentarios numerados usan los nombres de los pasos
 * de la pantalla. Solo funciones de Octave (endfor, printf, polyval, flipud); corre también en Octave Online.
 */
object GeneradorOctave {

    /** Pasos en el orden en que aparecen como comentarios (el 6 solo si hay x a predecir). */
    val PASOS = listOf(
        "1. Sistema normal C*a = b",
        "2. Suma de cuadrados de residuos",
        "3. Media y variación de y",
        "4. Coeficiente de determinación",
        "5. Error estándar",
        "6. Predicción",
    )

    /** Mismo formato que ve el alumno en los datos: "7.5", "1", "0.001" (punto decimal). */
    fun numero(v: Double): String = Formato.corto(v)

    fun vector(v: List<Double>): String = v.joinToString(" ", "[", "]'") { numero(it) }

    fun regresionPolinomial(x: List<Double>, y: List<Double>, grado: Int, xPredecir: Double? = null): String {
        val l = mutableListOf(
            "% Regresión polinomial por mínimos cuadrados (exportado desde Pentametod2)",
            "% Pega este código en Octave o en Octave Online.",
            "",
            "x = ${vector(x)};",
            "y = ${vector(y)};",
            "m = $grado;",
            "n = length(x);",
            "",
            "% ${PASOS[0]}",
            "C = zeros(m+1);",
            "b = zeros(m+1, 1);",
            "for i = 1:m+1",
            "  for j = 1:m+1",
            "    C(i,j) = sum(x.^(i+j-2));",
            "  endfor",
            "  b(i) = sum((x.^(i-1)).*y);",
            "endfor",
            "a = C\\b;",
            "P = polyval(flipud(a), x);",
            "",
            "% ${PASOS[1]}",
            "Q = sum((P - y).^2);",
            "",
            "% ${PASOS[2]}",
            "ybar = mean(y);",
            "Syy = sum((y - ybar).^2);",
            "",
            "% ${PASOS[3]}",
            "R2 = (Syy - Q)/Syy;",
            "r = sqrt(R2);",
            "",
            "% ${PASOS[4]}",
            "S = sqrt(Q/(n-(m+1)));",
        )
        if (xPredecir != null) {
            l += listOf(
                "",
                "% ${PASOS[5]}",
                "xp = ${numero(xPredecir)};",
                "Pxp = polyval(flipud(a), xp);",
            )
        }
        l += listOf(
            "",
            "% Resultados",
            "for k = 1:m+1",
            "  printf(\"a%d = %.6f\\n\", k-1, a(k));",
            "endfor",
            "printf(\"Q = %.6f\\n\", Q);",
            "printf(\"R2 = %.6f\\n\", R2);",
            "printf(\"r = %.6f\\n\", r);",
            "printf(\"S = %.6f\\n\", S);",
        )
        if (xPredecir != null) l += "printf(\"P(%g) = %.6f\\n\", xp, Pxp);"
        return l.joinToString("\n") + "\n"
    }
}
