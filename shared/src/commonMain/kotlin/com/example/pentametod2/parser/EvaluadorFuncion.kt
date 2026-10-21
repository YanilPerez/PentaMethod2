package com.example.pentametod2.parser

import kotlin.math.*

/**
 * Convierte texto como "x^3 - 2x - 5" o "sin(x) + e^x" en una función (Double) -> Double.
 * Kotlin puro: funciona en commonMain (Android + iOS).
 *
 * Uso:
 *   val f = EvaluadorFuncion.compilar("x^2 - 3")
 *   f(2.0)  // 1.0
 *
 *   val g = EvaluadorFuncion.compilarMulti("x^2 + y^2 - 4", listOf("x", "y"))
 *   g(doubleArrayOf(1.0, 2.0))  // 1.0
 *
 * Soporta + - * / ^ (asociativa a la derecha), multiplicación implícita (2x, 3sin(x), 2(x+1), xy),
 * las constantes pi y e, coma decimal y las funciones sin, cos, tan, asin, acos, atan, exp, ln,
 * log (base 10), sqrt y abs. La división entre cero no lanza excepción: devuelve Infinity o NaN.
 */
object EvaluadorFuncion {

    /** Compila una función de una sola variable `x`. */
    fun compilar(texto: String): (Double) -> Double {
        val f = compilarMulti(texto, listOf("x"))
        return deUnaVariable(f)
    }

    /**
     * Compila una función de varias variables. Al evaluar, `variables[k]` toma el valor `valores[k]`.
     * Los nombres de variable son letras seguidas de dígitos opcionales: x, y, t, x1, x2...
     */
    fun compilarMulti(texto: String, variables: List<String>): (DoubleArray) -> Double {
        val nombres = variables.map { it.lowercase() }
        for (v in nombres) {
            if (!NOMBRE_VARIABLE.matches(v)) {
                throw FuncionInvalidaException("Nombre de variable inválido '$v': usa letras seguidas de dígitos opcionales, ej: x, y, x1")
            }
            if (v in FUNCIONES) throw FuncionInvalidaException("'$v' es una función, no puede usarse como variable")
        }
        if (nombres.toSet().size != nombres.size) throw FuncionInvalidaException("Hay variables repetidas en $variables")
        val f = Parser(texto, nombres).parsear()
        return conValidacion(f, nombres.size)
    }

    private fun deUnaVariable(f: (DoubleArray) -> Double): (Double) -> Double =
        { x -> f(doubleArrayOf(x)) }

    private fun conValidacion(f: (DoubleArray) -> Double, n: Int): (DoubleArray) -> Double = { valores ->
        require(valores.size >= n) { "Se esperaban $n valores y llegaron ${valores.size}" }
        f(valores)
    }

    private val NOMBRE_VARIABLE = Regex("[a-z]+[0-9]*")

    internal val FUNCIONES: Map<String, (Double) -> Double> = mapOf(
        "sin" to ::sin,
        "cos" to ::cos,
        "tan" to ::tan,
        "asin" to ::asin,
        "acos" to ::acos,
        "atan" to ::atan,
        "exp" to ::exp,
        "ln" to ::ln,
        "log" to ::log10,
        "sqrt" to ::sqrt,
        "abs" to ::abs,
    )
}

private typealias Nodo = (DoubleArray) -> Double

// Constructores de nodos. Las lambdas se crean solo aquí para no ponerlas nunca como
// última expresión de un if/when (Kotlin las tomaría como bloque o trailing lambda).
private fun constante(v: Double): Nodo = { _ -> v }
private fun variable(k: Int): Nodo = { vs -> vs[k] }
private fun suma(a: Nodo, b: Nodo): Nodo = { vs -> a(vs) + b(vs) }
private fun resta(a: Nodo, b: Nodo): Nodo = { vs -> a(vs) - b(vs) }
private fun producto(a: Nodo, b: Nodo): Nodo = { vs -> a(vs) * b(vs) }
private fun division(a: Nodo, b: Nodo): Nodo = { vs -> a(vs) / b(vs) }
private fun negar(a: Nodo): Nodo = { vs -> -a(vs) }
private fun elevar(base: Nodo, exp: Nodo): Nodo = { vs -> base(vs).pow(exp(vs)) }
private fun componer(f: (Double) -> Double, arg: Nodo): Nodo = { vs -> f(arg(vs)) }

private class Parser(texto: String, private val variables: List<String>) {
    // Solo se pasa a minúsculas y se cambia la coma por punto: las posiciones coinciden con el texto original.
    private val s = texto.lowercase().replace(',', '.')
    private var i = 0

    fun parsear(): Nodo {
        if (s.isBlank()) throw FuncionInvalidaException("La función está vacía")
        val e = expresion()
        val c = actual()
        if (c == ')') throw FuncionInvalidaException("Paréntesis ')' sin abrir en la posición ${i + 1}")
        if (c != null) throw FuncionInvalidaException("Carácter inesperado '$c' en la posición ${i + 1}")
        return e
    }

    /** Salta espacios y devuelve el carácter actual, o null si se acabó el texto. */
    private fun actual(): Char? {
        while (i < s.length && s[i].isWhitespace()) i++
        return if (i < s.length) s[i] else null
    }

    // expresion = termino (('+' | '-') termino)*
    private fun expresion(): Nodo {
        var izq = termino()
        while (true) {
            val op = actual()
            if (op != '+' && op != '-') break
            i++
            val der = termino()
            izq = if (op == '+') suma(izq, der) else resta(izq, der)
        }
        return izq
    }

    // termino = unario (('*' | '/' | multiplicación implícita) unario)*
    private fun termino(): Nodo {
        var izq = unario()
        while (true) {
            val c = actual() ?: break
            izq = when {
                c == '*' -> { i++; producto(izq, unario()) }
                c == '/' -> { i++; division(izq, unario()) }
                c.isLetter() || c == '(' -> producto(izq, unario()) // 2x, 3sin(x), 2(x+1)
                else -> break
            }
        }
        return izq
    }

    // unario = '-' unario | '+' unario | potencia      (-x^2 = -(x^2))
    private fun unario(): Nodo {
        val c = actual()
        if (c == '-') {
            i++
            return negar(unario())
        }
        if (c == '+') {
            i++
            return unario()
        }
        return potencia()
    }

    // potencia = primario ('^' unario)?   (asociativa a la derecha: 2^3^2 = 2^9; 2^-1 = 0.5)
    private fun potencia(): Nodo {
        val base = primario()
        if (actual() != '^') return base
        i++
        return elevar(base, unario())
    }

    private fun primario(): Nodo {
        val c = actual() ?: throw FuncionInvalidaException(finInesperado())
        return when {
            c.isDigit() || c == '.' -> numero()
            c == '(' -> parentesis()
            c.isLetter() -> identificador()
            else -> throw FuncionInvalidaException("Carácter inesperado '$c' en la posición ${i + 1}")
        }
    }

    private fun finInesperado(): String {
        val ultimo = s.trimEnd().lastOrNull()
        return if (ultimo != null && ultimo in "+-*/^(") {
            "La expresión termina de forma inesperada después de '$ultimo'"
        } else {
            "La expresión termina de forma inesperada"
        }
    }

    private fun numero(): Nodo {
        val ini = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        val txt = s.substring(ini, i)
        val n = txt.toDoubleOrNull()
            ?: throw FuncionInvalidaException("Número inválido '$txt' en la posición ${ini + 1}")
        return constante(n)
    }

    private fun parentesis(): Nodo {
        val ini = i
        i++
        if (actual() == ')') throw FuncionInvalidaException("Paréntesis vacío en la posición ${ini + 1}")
        val e = expresion()
        if (actual() != ')') {
            throw FuncionInvalidaException("Falta cerrar el paréntesis abierto en la posición ${ini + 1}")
        }
        i++
        return e
    }

    // identificador = letras dígitos*
    private fun identificador(): Nodo {
        val ini = i
        while (i < s.length && s[i].isLetter()) i++
        while (i < s.length && s[i].isDigit()) i++
        val nombre = s.substring(ini, i)
        val f = EvaluadorFuncion.FUNCIONES[nombre]

        // (1) función conocida seguida de '('
        if (f != null && actual() == '(') return llamada(nombre, f)

        // (2) variable declarada
        val k = variables.indexOf(nombre)
        if (k >= 0) return variable(k)

        // (3) constantes
        if (nombre == "pi") return constante(PI)
        if (nombre == "e") return constante(E)

        // (4) producto de variables de una letra: xy = x*y
        if (nombre.all { it.isLetter() && it.toString() in variables }) return productoDeLetras(nombre)

        // (5) error
        if (f != null) {
            throw FuncionInvalidaException("Después de '$nombre' debe ir un paréntesis, ej: $nombre(x)")
        }
        if (actual() == '(') throw FuncionInvalidaException("Función desconocida '$nombre' en la posición ${ini + 1}")
        throw FuncionInvalidaException("Variable desconocida '$nombre'")
    }

    private fun llamada(nombre: String, f: (Double) -> Double): Nodo {
        val abre = i
        i++ // '('
        if (actual() == ')') throw FuncionInvalidaException("Falta el argumento de '$nombre'")
        val arg = expresion()
        if (actual() != ')') {
            throw FuncionInvalidaException("Falta cerrar el paréntesis de '$nombre' abierto en la posición ${abre + 1}")
        }
        i++
        return componer(f, arg)
    }

    private fun productoDeLetras(nombre: String): Nodo {
        var r = variable(variables.indexOf(nombre[0].toString()))
        for (j in 1 until nombre.length) {
            r = producto(r, variable(variables.indexOf(nombre[j].toString())))
        }
        return r
    }
}
