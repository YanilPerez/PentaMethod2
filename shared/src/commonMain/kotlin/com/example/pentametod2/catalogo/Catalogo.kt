package com.example.pentametod2.catalogo

import com.example.pentametod2.exportar.GeneradorLagrange
import com.example.pentametod2.exportar.GeneradorMetodo
import com.example.pentametod2.exportar.GeneradorNewtonInterpolacion
import com.example.pentametod2.exportar.GeneradorNewtonSistemas
import com.example.pentametod2.exportar.GeneradorPuntoFijo
import com.example.pentametod2.exportar.GeneradorRegresionMultiple
import com.example.pentametod2.exportar.GeneradorRegresionPolinomial
import com.example.pentametod2.methods.Interpolacion
import com.example.pentametod2.methods.MetodoNumericoException
import com.example.pentametod2.methods.Regresion
import com.example.pentametod2.methods.SistemasNoLineales
import com.example.pentametod2.model.Bloque
import com.example.pentametod2.model.BloqueGrupo
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.BloqueTexto
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.parser.EvaluadorFuncion
import com.example.pentametod2.parser.FuncionInvalidaException
import com.example.pentametod2.presentacion.PresentadorInterpolacion
import com.example.pentametod2.presentacion.PresentadorRegresion
import com.example.pentametod2.presentacion.PresentadorSistemas

/**
 * Punto de entrada para la interfaz: lista los métodos, da ejemplos del curso y ejecuta
 * un método a partir de textos crudos. [ejecutar] nunca lanza excepciones.
 */
object Catalogo {

    private val METODOS = listOf(
        MetodoInfo(MetodoId.PUNTO_FIJO, "Punto fijo para sistemas", 3, "Itera x = G(x) hasta converger", Secciones.SISTEMAS),
        MetodoInfo(MetodoId.NEWTON_SISTEMAS, "Newton-Raphson para sistemas", 3, "Converge rápido usando el jacobiano", Secciones.SISTEMAS),
        MetodoInfo(MetodoId.REGRESION_POLINOMIAL, "Regresión polinomial", 4, "Mínimos cuadrados con un polinomio de grado m", Secciones.AJUSTE),
        MetodoInfo(MetodoId.REGRESION_MULTIPLE, "Regresión múltiple", 4, "y en función de x₁, x₂, …, xₖ", Secciones.AJUSTE),
        MetodoInfo(MetodoId.LAGRANGE, "Polinomio de Lagrange", 5, "Pasa exactamente por cada punto", Secciones.INTERPOLACION),
        MetodoInfo(MetodoId.NEWTON_INTERPOLACION, "Newton (diferencias divididas)", 5, "Tabla de diferencias divididas", Secciones.INTERPOLACION),
    )

    fun metodos(): List<MetodoInfo> = METODOS

    // ── Exportación: cada método registra aquí lo que sabe exportar ──

    private val GENERADORES: Map<MetodoId, GeneradorMetodo> = mapOf(
        MetodoId.PUNTO_FIJO to GeneradorPuntoFijo,
        MetodoId.NEWTON_SISTEMAS to GeneradorNewtonSistemas,
        MetodoId.REGRESION_POLINOMIAL to GeneradorRegresionPolinomial,
        MetodoId.REGRESION_MULTIPLE to GeneradorRegresionMultiple,
        MetodoId.LAGRANGE to GeneradorLagrange,
        MetodoId.NEWTON_INTERPOLACION to GeneradorNewtonInterpolacion,
    )

    fun generador(id: MetodoId): GeneradorMetodo? = GENERADORES[id]

    fun puedeExportarExcel(id: MetodoId): Boolean = generador(id)?.excel == true

    fun puedeExportarOctave(id: MetodoId): Boolean = generador(id)?.octave == true

    fun info(id: MetodoId): MetodoInfo = METODOS.first { it.id == id }

    /** Datos de los Excel del curso. */
    fun ejemplo(id: MetodoId): Entrada = when (id) {
        MetodoId.PUNTO_FIJO -> EntradaSistema(listOf("0.5*cos(y)", "0.5*sin(x)"), "x, y", "0, 0", "1e-8", "50")
        MetodoId.NEWTON_SISTEMAS -> EntradaSistema(listOf("x^2+y^2-4", "x-y"), "x, y", "1, 1", "1e-8", "50")
        MetodoId.REGRESION_POLINOMIAL -> EntradaRegresionPolinomial("1,2,3,4,5", "9,7.5,4.2,3,2.1", "2", "2.5")
        MetodoId.REGRESION_MULTIPLE -> EntradaRegresionMultiple(
            listOf(
                listOf("0", "0", "5"), listOf("2", "1", "10"), listOf("2.5", "2", "9"),
                listOf("1", "3", "0"), listOf("4", "6", "3"), listOf("7", "2", "27"),
            ),
            "1, 1",
        )
        MetodoId.LAGRANGE -> EntradaInterpolacion("2.2, 2.4, 2.6", "0.5, 0.8, 1.4", "2.5", ModoNodos.MANUAL)
        MetodoId.NEWTON_INTERPOLACION -> EntradaInterpolacion(
            "2, 2.2, 2.4, 2.6, 2.8", "0.5, 0.8, 1.4 , 0.1 , 1.2 ", "2.5", ModoNodos.MANUAL,
        )
    }

    fun ejecutar(id: MetodoId, entrada: Entrada): SalidaMetodo {
        return try {
            val salida = when (id) {
                MetodoId.PUNTO_FIJO -> puntoFijo(entrada as? EntradaSistema ?: return entradaIncorrecta(id))
                MetodoId.NEWTON_SISTEMAS -> newtonSistemas(entrada as? EntradaSistema ?: return entradaIncorrecta(id))
                MetodoId.REGRESION_POLINOMIAL -> regresionPolinomial(entrada as? EntradaRegresionPolinomial ?: return entradaIncorrecta(id))
                MetodoId.REGRESION_MULTIPLE -> regresionMultiple(entrada as? EntradaRegresionMultiple ?: return entradaIncorrecta(id))
                MetodoId.LAGRANGE -> when (entrada) {
                    is EntradaLagrange -> CalculoLagrange.ejecutar(entrada)
                    is EntradaInterpolacion -> lagrange(entrada)
                    else -> return entradaIncorrecta(id)
                }
                MetodoId.NEWTON_INTERPOLACION -> newtonInterpolacion(entrada as? EntradaInterpolacion ?: return entradaIncorrecta(id))
            }
            verificarFinitos(salida)
        } catch (e: MetodoNumericoException) {
            SalidaMetodo.conError(id, e.message ?: "Los datos no son válidos")
        } catch (e: FuncionInvalidaException) {
            SalidaMetodo.conError(id, e.message ?: "La función no es válida")
        } catch (e: Throwable) {
            // Throwable y no solo Exception: un StackOverflowError (expresión absurdamente anidada) no debe cerrar la app.
            SalidaMetodo.conError(id, "Ocurrió un error inesperado: ${e.message ?: e::class.simpleName ?: "sin detalle"}")
        }
    }

    const val MENSAJE_DESBORDE =
        "El cálculo se desbordó: algunos resultados no son números finitos. Usa valores más pequeños, nodos menos juntos o un grado menor."

    /**
     * Red de seguridad: si un método "termina bien" pero algún número visible no es finito
     * (desbordamiento con datos extremos), la salida pasa a ERROR conservando las tablas.
     */
    internal fun verificarFinitos(s: SalidaMetodo): SalidaMetodo {
        if (s.estado == Estado.ERROR || !tieneNoFinitos(s)) return s
        return s.copy(estado = Estado.ERROR, error = MENSAJE_DESBORDE)
    }

    private fun tieneNoFinitos(s: SalidaMetodo): Boolean {
        val textos = s.resumen.map { it.valor } + s.formulas.flatMap { it.sustituida }
        if (textos.any { it.contains("NaN") || it.contains("∞") }) return true
        return s.bloques.any { bloqueNoFinito(it) }
    }

    private fun bloqueNoFinito(b: Bloque): Boolean = when (b) {
        is BloqueTabla -> b.filas.any { f -> f.any { it is Celda.Num && !it.v.isFinite() } }
        is BloqueMatriz -> b.datos.any { f -> f.any { !it.isFinite() } }
        is BloqueGrupo -> b.bloques.any { bloqueNoFinito(it) }
        is BloquePaso -> b.matrices.any { bloqueNoFinito(it) } || b.resultados.any { it.valor?.isFinite() == false }
        is BloqueTexto -> false
    }

    // ── Sistemas ──

    private class DatosSistema(val ecuaciones: List<String>, val variables: List<String>, val x0: DoubleArray, val tol: Double, val maxIter: Int)

    private fun leerSistema(e: EntradaSistema): DatosSistema {
        val variables = LectorEntrada.variables(e.variables)
        val ecuaciones = e.ecuaciones.map { it.trim() }
        if (ecuaciones.size > LectorEntrada.MAX_VARIABLES) {
            throw MetodoNumericoException("Hay ${ecuaciones.size} ecuaciones; el máximo es ${LectorEntrada.MAX_VARIABLES}")
        }
        if (ecuaciones.size != variables.size) {
            throw MetodoNumericoException(
                "Hay ${ecuaciones.size} ecuaciones y ${variables.size} variables: deben ser la misma cantidad"
            )
        }
        for ((i, texto) in ecuaciones.withIndex()) {
            if (texto.isEmpty()) throw MetodoNumericoException("Ecuación ${i + 1}: está vacía")
            if (texto.length > LectorEntrada.MAX_LARGO_ECUACION) {
                throw MetodoNumericoException(
                    "Ecuación ${i + 1}: es demasiado larga (${texto.length} caracteres; el máximo es ${LectorEntrada.MAX_LARGO_ECUACION})"
                )
            }
            try {
                EvaluadorFuncion.compilarMulti(texto, variables)
            } catch (ex: FuncionInvalidaException) {
                throw MetodoNumericoException("Ecuación ${i + 1}: ${ex.message}")
            }
        }
        val x0 = LectorEntrada.numeros(e.x0, "Los valores del punto inicial x₀")
        if (x0.size != variables.size) {
            throw MetodoNumericoException(
                "El punto inicial x₀ tiene ${x0.size} valores pero hay ${variables.size} variables (${variables.joinToString(", ")})"
            )
        }
        return DatosSistema(ecuaciones, variables, x0, LectorEntrada.tolerancia(e.tolerancia), LectorEntrada.maxIter(e.maxIter))
    }

    private fun puntoFijo(e: EntradaSistema): SalidaMetodo {
        val d = leerSistema(e)
        val r = SistemasNoLineales.puntoFijoSistema(d.ecuaciones, d.variables, d.x0, d.tol, d.maxIter)
        return PresentadorSistemas.presentarPuntoFijo(r, d.ecuaciones, d.tol, d.maxIter)
    }

    private fun newtonSistemas(e: EntradaSistema): SalidaMetodo {
        val d = leerSistema(e)
        val r = SistemasNoLineales.newtonSistema(d.ecuaciones, d.variables, d.x0, d.tol, d.maxIter)
        return PresentadorSistemas.presentarNewtonSistemas(r, d.ecuaciones, d.tol, d.maxIter)
    }

    // ── Interpolación ──

    /** Nodos que usa el método; en automático, `dados` dice cuántos escribió el alumno. */
    private class Nodos(val xs: DoubleArray, val ys: DoubleArray, val x: Double, val automatico: Boolean, val dados: Int)

    private fun leerNodos(e: EntradaInterpolacion): Nodos {
        val xs = LectorEntrada.numeros(e.xs, "Los nodos x")
        val ys = LectorEntrada.numeros(e.ys, "Los valores y")
        val x = LectorEntrada.numero(e.x, "El valor de x a evaluar")
        val maxNodos = LectorEntrada.MAX_GRADO_INTERPOLACION + 1
        if (e.modo == ModoNodos.MANUAL) {
            if (xs.size > maxNodos) {
                throw MetodoNumericoException(
                    "En modo manual se usan todos los nodos y el máximo es $maxNodos (grado ${LectorEntrada.MAX_GRADO_INTERPOLACION}); " +
                        "hay ${xs.size}. Usa el modo Automático para tomar solo los más cercanos a x."
                )
            }
            return Nodos(xs, ys, x, automatico = false, dados = xs.size)
        }
        val grado = LectorEntrada.entero(e.grado, "El grado", 1, LectorEntrada.MAX_GRADO_INTERPOLACION)
        if (xs.size == ys.size && grado + 1 > xs.size) {
            throw MetodoNumericoException("Para grado $grado se necesitan ${grado + 1} nodos y solo hay ${xs.size}")
        }
        val (sx, sy) = Interpolacion.seleccionarNodosCercanos(xs, ys, x, grado + 1)
        return Nodos(sx, sy, x, automatico = true, dados = xs.size)
    }

    private fun lagrange(e: EntradaInterpolacion): SalidaMetodo {
        val n = leerNodos(e)
        return PresentadorInterpolacion.presentarLagrange(Interpolacion.lagrange(n.xs, n.ys, n.x), n.automatico, n.dados)
    }

    private fun newtonInterpolacion(e: EntradaInterpolacion): SalidaMetodo {
        val n = leerNodos(e)
        return PresentadorInterpolacion.presentarNewton(Interpolacion.newtonDiferenciasDivididas(n.xs, n.ys, n.x), n.automatico, n.dados)
    }

    // ── Regresión ──

    private fun regresionPolinomial(e: EntradaRegresionPolinomial): SalidaMetodo {
        val xs = LectorEntrada.numeros(e.xs, "Los valores x")
        val ys = LectorEntrada.numeros(e.ys, "Los valores y")
        val grado = LectorEntrada.entero(e.grado, "El grado", 1, LectorEntrada.MAX_GRADO_REGRESION)
        val xPredecir = LectorEntrada.numeroOpcional(e.xPredecir, "El valor de x a predecir")
        val r = Regresion.regresionPolinomial(xs, ys, grado)
        return PresentadorRegresion.presentarRegresionPolinomial(r, xs, ys, xPredecir)
    }

    private fun regresionMultiple(e: EntradaRegresionMultiple): SalidaMetodo {
        val filas = e.filas.filter { fila -> fila.any { it.isNotBlank() } }
        if (filas.isEmpty()) throw MetodoNumericoException("La tabla de datos está vacía")
        if (filas.size > LectorEntrada.MAX_DATOS) {
            throw MetodoNumericoException("Hay ${filas.size} filas de datos; el máximo es ${LectorEntrada.MAX_DATOS}")
        }
        val columnas = filas[0].size
        if (columnas < 2) {
            throw MetodoNumericoException("Cada fila debe tener al menos una variable x y, al final, el valor de y")
        }
        if (columnas - 1 > LectorEntrada.MAX_VARIABLES) {
            throw MetodoNumericoException(
                "Hay ${columnas - 1} variables independientes; el máximo es ${LectorEntrada.MAX_VARIABLES}"
            )
        }
        val datos = filas.mapIndexed { i, fila ->
            if (fila.size != columnas) {
                throw MetodoNumericoException(
                    "La fila ${i + 1} tiene ${fila.size} valores pero la primera tiene $columnas: todas deben tener x₁…xₖ y al final y"
                )
            }
            DoubleArray(columnas) { j -> LectorEntrada.numero(fila[j], "Fila ${i + 1}, columna ${j + 1}") }
        }
        val k = columnas - 1
        val x = Array(datos.size) { datos[it].copyOfRange(0, k) }
        val y = DoubleArray(datos.size) { datos[it][k] }
        val xPredecir = if (e.xPredecir.isBlank()) null else LectorEntrada.numeros(e.xPredecir, "Los valores a predecir")
        if (xPredecir != null && xPredecir.size != k) {
            throw MetodoNumericoException("Para predecir hay que dar $k valores (uno por cada x) y se dieron ${xPredecir.size}")
        }
        val r = Regresion.regresionMultiple(x, y)
        return PresentadorRegresion.presentarRegresionMultiple(r, x, y, xPredecir)
    }

    private fun entradaIncorrecta(id: MetodoId): SalidaMetodo =
        SalidaMetodo.conError(id, "Los datos enviados no corresponden al método ${info(id).nombre}")
}
