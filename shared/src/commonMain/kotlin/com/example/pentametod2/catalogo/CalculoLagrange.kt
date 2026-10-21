package com.example.pentametod2.catalogo

import com.example.pentametod2.methods.Interpolacion
import com.example.pentametod2.methods.MetodoNumericoException
import com.example.pentametod2.model.*
import com.example.pentametod2.parser.EvaluadorFuncion
import com.example.pentametod2.parser.FuncionInvalidaException
import com.example.pentametod2.presentacion.PresentadorComparacionLagrange
import kotlin.math.abs

/** Validación compartida por la pantalla y el cálculo, sin depender de Compose. */
object DatosLagrange {
    const val MAX_GRADO = 12
    const val MAX_PUNTOS = 200
    private val numero = Regex("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?")

    fun numeroCelda(texto: String): Double? {
        val t = texto.trim().replace(',', '.')
        return if (numero.matches(t)) t.toDoubleOrNull()?.takeIf { it.isFinite() && abs(it) <= 1e12 } else null
    }

    fun filasConDatos(puntos: List<PuntoEntradaLagrange>) = puntos.dropLastWhile { it.x.isBlank() && it.y.isBlank() }

    fun errores(puntos: List<PuntoEntradaLagrange>): Map<Int, String> {
        val errores = mutableMapOf<Int, String>()
        val vistos = mutableMapOf<Double, Int>()
        for (p in filasConDatos(puntos)) {
            val x = numeroCelda(p.x)
            val y = numeroCelda(p.y)
            val etiqueta = nombrePunto(p.id)
            when {
                p.x.isBlank() -> errores[p.id] = "Falta X en el punto $etiqueta."
                x == null -> errores[p.id] = "Revisa X en el punto $etiqueta: usa un número finito entre −1e12 y 1e12."
                p.y.isBlank() -> errores[p.id] = "Falta Y en el punto $etiqueta."
                y == null -> errores[p.id] = "Revisa Y en el punto $etiqueta: usa un número finito entre −1e12 y 1e12."
            }
            if (x != null) {
                val anterior = vistos[x]
                if (anterior != null) {
                    errores[p.id] = "X está repetido en los puntos ${nombrePunto(anterior)} y $etiqueta."
                    errores[anterior] = errores.getValue(p.id)
                } else vistos[x] = p.id
            }
        }
        return errores
    }

    fun validar(e: EntradaLagrange): String? {
        if (e.puntos.any { it.id < 0 } || e.puntos.map { it.id }.distinct().size != e.puntos.size) return "Los puntos deben tener identificadores distintos."
        val puntos = filasConDatos(e.puntos)
        if (puntos.size > MAX_PUNTOS) return "El máximo es $MAX_PUNTOS puntos."
        errores(e.puntos).values.firstOrNull()?.let { return it }
        if (puntos.size < 2) return "Ingresa al menos 2 puntos completos para interpolar desde grado 1."
        if (numeroCelda(e.x) == null) return "Escribe un valor válido de x a evaluar."
        if (e.grados.isEmpty()) return "Selecciona al menos un grado."
        for (g in e.grados) {
            if (g !in 1..MAX_GRADO) return "El grado debe estar entre 1 y $MAX_GRADO."
            if (g + 1 > puntos.size) return "Para grado $g necesitas ${g + 1} puntos; hay ${puntos.size}. Revisa ese grado."
            if (e.modo == ModoNodos.MANUAL) {
                val ids = e.selecciones[g].orEmpty()
                if (ids.size != g + 1 || ids.distinct().size != ids.size) return "Grado $g: selecciona exactamente ${g + 1} puntos distintos (hay ${ids.size})."
                if (ids.any { id -> puntos.none { it.id == id } }) return "Grado $g: hay puntos eliminados o vacíos en la selección; revísala."
            }
        }
        return null
    }

    fun sugeridos(puntos: List<PuntoEntradaLagrange>, x: String, grado: Int): List<Int> {
        val objetivo = numeroCelda(x) ?: return emptyList()
        if (errores(puntos).isNotEmpty()) return emptyList()
        return filasConDatos(puntos).filter { numeroCelda(it.x) != null && numeroCelda(it.y) != null }
            .sortedWith(compareBy({ abs(numeroCelda(it.x)!! - objetivo) }, { numeroCelda(it.x)!! }))
            .take(grado + 1).sortedBy { numeroCelda(it.x) }.map { it.id }
    }
}

object CalculoLagrange {
    fun ejecutar(e: EntradaLagrange): SalidaMetodo {
        DatosLagrange.validar(e)?.let { throw MetodoNumericoException(it) }
        val puntos = DatosLagrange.filasConDatos(e.puntos).map {
            PuntoDatoLagrange(it.id, DatosLagrange.numeroCelda(it.x)!!, DatosLagrange.numeroCelda(it.y)!!)
        }
        val x = DatosLagrange.numeroCelda(e.x)!!
        val avisos = mutableListOf<String>()
        var funcion: ((Double) -> Double)? = null
        var referencia: Double? = null
        if (e.funcion != null) {
            try {
                if (e.funcion.length > LectorEntrada.MAX_LARGO_ECUACION) throw FuncionInvalidaException("La función admite hasta ${LectorEntrada.MAX_LARGO_ECUACION} caracteres.")
                funcion = EvaluadorFuncion.compilar(e.funcion)
                referencia = funcion(x).takeIf { it.isFinite() }
                if (referencia == null) avisos += "No se calcularon errores: f(x) no tiene un valor real finito en x = ${Formato.corto(x)}."
            } catch (ex: FuncionInvalidaException) {
                avisos += "No se calcularon errores: ${ex.message}"
            }
            if (referencia != null && abs(referencia) < 1e-10 && referencia != 0.0) avisos += "El valor de referencia está cerca de cero; el error relativo puede ser muy grande. Compara también el error absoluto."
            if (referencia == 0.0) avisos += "f(x) = 0: el error relativo porcentual no está definido. Usa el error absoluto."
            val f = funcion
            if (f != null) {
                val distintos = puntos.filter { p ->
                    val valor = f(p.x)
                    !valor.isFinite() || abs(valor - p.y) > 1e-6 * maxOf(1.0, abs(valor), abs(p.y))
                }
                if (distintos.isNotEmpty()) avisos += "Revisa la función o los datos: Y difiere de f(X) en ${distintos.joinToString { nombrePunto(it.id) }}. Se conservaron los valores ingresados."
            }
        }
        val resultados = e.grados.distinct().sorted().map { g ->
            val ids = if (e.modo == ModoNodos.AUTOMATICO) DatosLagrange.sugeridos(e.puntos, e.x, g) else e.selecciones.getValue(g)
            val usados = ids.map { id -> puntos.first { it.id == id } }.sortedBy { it.x }
            val r = Interpolacion.lagrange(usados.map { it.x }.toDoubleArray(), usados.map { it.y }.toDoubleArray(), x)
            if (!r.valor.isFinite() || r.L.any { !it.isFinite() } || r.terminos.any { !it.isFinite() }) {
                throw MetodoNumericoException("Grado $g: ${Catalogo.MENSAJE_DESBORDE}")
            }
            val absoluto = referencia?.let { abs(it - r.valor) }?.takeIf { it.isFinite() }
            val relativo = if (referencia != null && referencia != 0.0 && absoluto != null) (absoluto / abs(referencia) * 100).takeIf { it.isFinite() } else null
            if (referencia != null && (absoluto == null || (referencia != 0.0 && relativo == null))) avisos += "Grado $g: un error excede el rango numérico y se muestra como no disponible."
            PresentadorComparacionLagrange.resultado(r, usados, e.modo == ModoNodos.AUTOMATICO, puntos.size, absoluto, relativo, referencia)
        }
        return PresentadorComparacionLagrange.presentar(puntos, x, resultados, e.funcion, referencia, avisos)
    }
}
