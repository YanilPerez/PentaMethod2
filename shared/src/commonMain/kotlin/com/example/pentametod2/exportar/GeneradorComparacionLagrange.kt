package com.example.pentametod2.exportar

import com.example.pentametod2.catalogo.Entrada
import com.example.pentametod2.catalogo.nombrePunto
import com.example.pentametod2.model.*
import com.example.pentametod2.presentacion.PresentadorComparacionLagrange

object GeneradorComparacionLagrange : GeneradorMetodo {
    override fun nombreBase(s: SalidaMetodo) = "lagrange_grados_" + s.comparacionLagrange!!.resultados.joinToString("_") { it.grado.toString() }

    override fun hojas(s: SalidaMetodo, entrada: Entrada?): List<HojaX> {
        val c = s.comparacionLagrange!!
        val encabezado = EstiloX.ENCABEZADO
        val comparacion = Exportaciones.hojaTabla(PresentadorComparacionLagrange.tabla(c), "Comparación", EstiloX.NORMAL)
        val pares = listOf("x a evaluar" to CeldaX.Numero(c.x),
            "Grados" to CeldaX.Texto(c.resultados.joinToString { it.grado.toString() }),
            "Función original" to CeldaX.Texto(c.funcion ?: "No proporcionada"),
            "f(x) de referencia" to (c.referencia?.let { CeldaX.Numero(it, EstiloX.NORMAL) } ?: CeldaX.Texto("No disponible")),
            "Selección" to CeldaX.Texto(if ((c.resultados.first().detalle.exportable as DatosExportables.Interpolacion).automatico) "Automática" else "Manual por grado"))
        val datos = Exportaciones.hojaEntrada(pares, listOf("Punto", "X", "Y"),
            c.puntos.map { listOf(CeldaX.Texto(nombrePunto(it.id)), CeldaX.Numero(it.x, EstiloX.NORMAL), CeldaX.Numero(it.y, EstiloX.NORMAL)) })
        val avisos = HojaX("Notas", listOf(listOf(CeldaX.Texto("Observaciones", encabezado))) +
            (s.advertencias + c.resultados.flatMap { r -> r.detalle.advertencias.map { "Grado ${r.grado}: $it" } } +
                s.bloques.filterIsInstance<BloqueTexto>().map { it.texto }).distinct().map { listOf(CeldaX.Texto(it)) })
        val detalles = c.resultados.flatMap { r ->
            val tabla = Exportaciones.tabla(r.detalle, "Nodos y polinomios de Lagrange")
            val filas = mutableListOf<List<CeldaX>>(listOf(CeldaX.Texto("Puntos utilizados: ${r.puntos.joinToString { nombrePunto(it.id) }}", encabezado)))
            for (p in Exportaciones.pasos(r.detalle)) {
                filas += listOf(CeldaX.Texto("${p.numero}. ${p.titulo}", encabezado))
                filas += listOf(CeldaX.Texto(p.explicacion))
                for (linea in p.formulaGeneral + p.conTusDatos) filas += listOf(CeldaX.Texto(linea))
                for (valor in p.resultados) filas += listOf(CeldaX.Texto(valor.etiqueta), valor.valor?.let { CeldaX.Numero(it, EstiloX.NORMAL) } ?: CeldaX.Texto(valor.texto))
                filas += emptyList<CeldaX>()
            }
            listOf(Exportaciones.hojaTabla(tabla, "Nodos grado ${r.grado}", EstiloX.NORMAL), HojaX("Pasos grado ${r.grado}", filas))
        }
        return listOf(comparacion, datos, avisos) + detalles
    }

    /** Los vectores se exportan con precisión Double completa, sin redondear antes de recalcular. */
    override fun codigoOctave(s: SalidaMetodo, entrada: Entrada?): String {
        val c = s.comparacionLagrange!!
        fun vector(v: List<Double>) = v.joinToString(" ", "[", "]") { it.toString() }
        val lineas = mutableListOf("% Comparación de interpolaciones de Lagrange", "% Pega este código en Octave o en Octave Online.",
            "clear;", "x_eval = ${c.x};", "x_datos = ${vector(c.puntos.map { it.x })};", "y_datos = ${vector(c.puntos.map { it.y })};",
            "figure; hold on; grid on;", "plot(x_datos, y_datos, 'o', 'DisplayName', 'Datos ingresados');",
            "t = linspace(min([x_datos x_eval]), max([x_datos x_eval]), 200);",
            "comparacion = []; % columnas: grado, P(x), error absoluto, error relativo porcentual")
        val referencia = c.referencia
        if (referencia != null && c.funcion != null) {
            lineas += TraductorOctave.funcionAnonima("f", c.funcion, listOf("x"))
            lineas += "valor_real = f(x_eval);"
        }
        for (aviso in s.advertencias) lineas += aviso.lines().map { "% Aviso: $it" }
        for (r in c.resultados) {
            lineas += listOf("", "% Grado ${r.grado}; puntos ${r.puntos.joinToString { nombrePunto(it.id) }}",
                "xs = ${vector(r.puntos.map { it.x })};", "ys = ${vector(r.puntos.map { it.y })};", "n = length(xs);",
                "L = ones(1,n);", "Pt = zeros(size(t));", "for k = 1:n", "  Lk = ones(size(t));",
                "  for j = 1:n", "    if j ~= k", "      L(k) = L(k)*(x_eval - xs(j))/(xs(k) - xs(j));",
                "      Lk = Lk.*(t - xs(j))/(xs(k) - xs(j));", "    endif", "  endfor",
                "  Pt = Pt + ys(k)*Lk;", "endfor", "P = sum(ys.*L);", "ea = NaN; er = NaN;")
            if (referencia != null) lineas += listOf("ea = abs(valor_real - P);", "if valor_real ~= 0; er = ea/abs(valor_real)*100; endif")
            lineas += listOf("comparacion(end+1,:) = [${r.grado} P ea er];",
                "plot(t, Pt, '-', 'DisplayName', 'Grado ${r.grado}');", "plot(x_eval, P, '*', 'HandleVisibility', 'off');",
                "printf('Grado ${r.grado}: P(%.15g) = %.15g; error absoluto = %.15g; relativo (%%) = %.15g\\n', x_eval, P, ea, er);")
            if (r.extrapolacion) lineas += "% Aviso: para este grado, x_eval está fuera del intervalo de sus puntos."
        }
        lineas += listOf("xlabel('x'); ylabel('y'); legend('show'); hold off;", "disp('Grado | P(x) | Error absoluto | Error relativo %');", "disp(comparacion);")
        return lineas.joinToString("\n", postfix = "\n")
    }
}
