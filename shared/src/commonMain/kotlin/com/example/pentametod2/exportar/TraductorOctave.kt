package com.example.pentametod2.exportar

/**
 * Traduce una expresión del parser de la app ([com.example.pentametod2.parser.EvaluadorFuncion]) a Octave.
 * Sigue la MISMA gramática para que Octave calcule exactamente lo mismo:
 *  - multiplicación implícita (2x, 3sin(x), 2(x+1), (x+1)(x−1)) → `*` explícito;
 *  - `xy` con variables de una letra → `x*y`;
 *  - `^` es asociativa a la derecha en la app y a la izquierda en Octave → el exponente va entre paréntesis;
 *  - `-x^2` = −(x²) en ambos; la coma decimal se lee como punto;
 *  - funciones: sin, cos, tan, asin, acos, atan, exp, sqrt, abs (igual), ln → log, log (base 10) → log10;
 *  - constantes: pi → pi, e → exp(1) (así una variable llamada e no la tapa).
 * Si algo no se puede traducir devuelve [Traduccion.Error] con el texto problemático, nunca código inválido.
 */
object TraductorOctave {

    sealed interface Traduccion {
        data class Ok(val codigo: String) : Traduccion
        data class Error(val token: String) : Traduccion
    }

    /** Nombre en Octave de cada función del parser. */
    val FUNCIONES = mapOf(
        "sin" to "sin", "cos" to "cos", "tan" to "tan",
        "asin" to "asin", "acos" to "acos", "atan" to "atan",
        "exp" to "exp", "ln" to "log", "log" to "log10",
        "sqrt" to "sqrt", "abs" to "abs",
    )

    /** Palabras reservadas de Octave: no pueden ser parámetros de una función anónima. */
    val RESERVADAS = setOf(
        "if", "elseif", "else", "end", "endif", "for", "endfor", "parfor", "endparfor", "while", "endwhile",
        "do", "until", "switch", "case", "otherwise", "endswitch", "function", "endfunction", "return",
        "break", "continue", "global", "persistent", "try", "catch", "end_try_catch", "unwind_protect",
        "unwind_protect_cleanup", "end_unwind_protect", "classdef", "endclassdef", "methods", "endmethods",
        "properties", "endproperties", "events", "endevents", "enumeration", "endenumeration",
    )

    fun traducir(texto: String, variables: List<String>): Traduccion {
        val nombres = variables.map { it.lowercase() }
        nombres.firstOrNull { it in RESERVADAS }?.let { return Traduccion.Error(it) }
        return try {
            Traduccion.Ok(imprimir(Lector(texto, nombres).parsear(), 0))
        } catch (e: NoTraducible) {
            Traduccion.Error(e.token)
        }
    }

    /**
     * `g1 = @(x, y) 0.5*cos(y);`. Si no se puede traducir: una línea "% ATENCIÓN" y la expresión original
     * comentada, para que el alumno la complete a mano.
     */
    fun funcionAnonima(nombre: String, texto: String, variables: List<String>): String {
        val params = variables.joinToString(", ") { it.lowercase() }
        return when (val t = traducir(texto, variables)) {
            is Traduccion.Ok -> "$nombre = @($params) ${t.codigo};"
            is Traduccion.Error -> "% ATENCIÓN: no se pudo traducir ${t.token}\n% $nombre = @($params) ${texto.trim()};"
        }
    }

    // ── Árbol y escritura ──

    private sealed interface Nodo
    private data class Num(val texto: String) : Nodo
    private data class Var(val nombre: String) : Nodo
    private data class Constante(val octave: String) : Nodo
    private data class Neg(val a: Nodo) : Nodo
    private data class Bin(val op: Char, val a: Nodo, val b: Nodo) : Nodo
    private data class Pot(val base: Nodo, val exp: Nodo) : Nodo
    private data class Llamada(val funcion: String, val arg: Nodo) : Nodo

    private const val SUMA = 1
    private const val PRODUCTO = 2
    private const val UNARIO = 3
    private const val POTENCIA = 4
    private const val ATOMO = 5

    private fun precedencia(n: Nodo): Int = when (n) {
        is Bin -> if (n.op == '+' || n.op == '-') SUMA else PRODUCTO
        is Neg -> UNARIO
        is Pot -> POTENCIA
        else -> ATOMO
    }

    /** Escribe `n` con paréntesis si su precedencia es menor que `minimo` (conserva el orden de evaluación). */
    private fun imprimir(n: Nodo, minimo: Int): String {
        val t = when (n) {
            is Num -> n.texto
            is Var -> n.nombre
            is Constante -> n.octave
            is Llamada -> "${n.funcion}(${imprimir(n.arg, 0)})"
            // "--x" sería un decremento en Octave: el doble signo va con paréntesis.
            is Neg -> "-" + if (n.a is Neg) "(${imprimir(n.a, 0)})" else imprimir(n.a, UNARIO)
            is Bin -> when (n.op) {
                '+', '-' -> "${imprimir(n.a, SUMA)} ${n.op} ${imprimir(n.b, PRODUCTO)}"
                else -> "${imprimir(n.a, PRODUCTO)}${n.op}${imprimir(n.b, UNARIO)}"
            }
            is Pot -> "${imprimir(n.base, ATOMO)}^${imprimir(n.exp, ATOMO)}"
        }
        return if (precedencia(n) < minimo) "($t)" else t
    }

    private class NoTraducible(val token: String) : Exception(token)

    /** Mismo algoritmo que el Parser de EvaluadorFuncion, pero construye un árbol en lugar de lambdas. */
    private class Lector(texto: String, private val variables: List<String>) {
        private val s = texto.lowercase().replace(',', '.')
        private var i = 0

        fun parsear(): Nodo {
            if (s.isBlank()) throw NoTraducible("(expresión vacía)")
            val e = expresion()
            val c = actual()
            if (c != null) throw NoTraducible("'$c'")
            return e
        }

        private fun actual(): Char? {
            while (i < s.length && s[i].isWhitespace()) i++
            return if (i < s.length) s[i] else null
        }

        private fun expresion(): Nodo {
            var izq = termino()
            while (true) {
                val op = actual()
                if (op != '+' && op != '-') break
                i++
                izq = Bin(op, izq, termino())
            }
            return izq
        }

        private fun termino(): Nodo {
            var izq = unario()
            while (true) {
                val c = actual() ?: break
                izq = when {
                    c == '*' -> { i++; Bin('*', izq, unario()) }
                    c == '/' -> { i++; Bin('/', izq, unario()) }
                    c.isLetter() || c == '(' -> Bin('*', izq, unario())
                    else -> break
                }
            }
            return izq
        }

        private fun unario(): Nodo {
            val c = actual()
            if (c == '-') { i++; return Neg(unario()) }
            if (c == '+') { i++; return unario() }
            return potencia()
        }

        private fun potencia(): Nodo {
            val base = primario()
            if (actual() != '^') return base
            i++
            return Pot(base, unario())
        }

        private fun primario(): Nodo {
            val c = actual() ?: throw NoTraducible("(fin inesperado)")
            return when {
                c.isDigit() || c == '.' -> numero()
                c == '(' -> parentesis()
                c.isLetter() -> identificador()
                else -> throw NoTraducible("'$c'")
            }
        }

        private fun numero(): Nodo {
            val ini = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            val txt = s.substring(ini, i)
            if (txt.toDoubleOrNull() == null) throw NoTraducible(txt)
            return Num(txt)
        }

        private fun parentesis(): Nodo {
            i++
            val e = expresion()
            if (actual() != ')') throw NoTraducible("'(' sin cerrar")
            i++
            return e
        }

        private fun identificador(): Nodo {
            val ini = i
            while (i < s.length && s[i].isLetter()) i++
            while (i < s.length && s[i].isDigit()) i++
            val nombre = s.substring(ini, i)
            val f = FUNCIONES[nombre]
            if (f != null && actual() == '(') {
                i++
                val arg = expresion()
                if (actual() != ')') throw NoTraducible("$nombre(")
                i++
                return Llamada(f, arg)
            }
            if (nombre in variables) return Var(nombre)
            if (nombre == "pi") return Constante("pi")
            if (nombre == "e") return Constante("exp(1)")
            if (nombre.all { it.toString() in variables }) {
                return nombre.drop(1).fold<Nodo>(Var(nombre[0].toString())) { acc, c -> Bin('*', acc, Var(c.toString())) }
            }
            throw NoTraducible(nombre)
        }
    }
}
