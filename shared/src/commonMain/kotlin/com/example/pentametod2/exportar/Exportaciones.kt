package com.example.pentametod2.exportar

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.Entrada
import com.example.pentametod2.model.BloqueMatriz
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.BloqueTabla
import com.example.pentametod2.model.Celda
import com.example.pentametod2.model.Estado
import com.example.pentametod2.model.SalidaMetodo

enum class TipoExportacion(val extension: String, val mime: String) {
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    OCTAVE("txt", "text/plain"),
}

/**
 * Lo que sabe exportar un método. Se registra en [Catalogo] (ver `Catalogo.generador`); todo se genera desde
 * la MISMA [SalidaMetodo] que dibuja la pantalla, más la entrada original (textos del alumno) si hace falta.
 */
interface GeneradorMetodo {
    val excel: Boolean get() = true
    val octave: Boolean get() = true

    /** "regresion_polinomial_grado2" (sin extensión). */
    fun nombreBase(s: SalidaMetodo): String

    fun hojas(s: SalidaMetodo, entrada: Entrada?): List<HojaX>

    fun codigoOctave(s: SalidaMetodo, entrada: Entrada?): String
}

/** Fachada para la interfaz: qué se puede exportar, con qué nombre y con qué bytes. */
object Exportaciones {

    /** "Error de entrada": no se calculó nada (no hay ni resumen ni bloques). */
    fun sinCalculo(s: SalidaMetodo): Boolean = s.estado == Estado.ERROR && s.bloques.isEmpty() && s.resumen.isEmpty()

    fun disponible(s: SalidaMetodo, tipo: TipoExportacion): Boolean {
        if (sinCalculo(s) || s.exportable == null) return false
        return when (tipo) {
            TipoExportacion.EXCEL -> Catalogo.puedeExportarExcel(s.metodo)
            TipoExportacion.OCTAVE -> Catalogo.puedeExportarOctave(s.metodo)
        }
    }

    fun nombreArchivo(s: SalidaMetodo, tipo: TipoExportacion): String = "${generador(s).nombreBase(s)}.${tipo.extension}"

    fun contenido(s: SalidaMetodo, tipo: TipoExportacion, entrada: Entrada? = null): ByteArray = when (tipo) {
        TipoExportacion.EXCEL -> LibroXlsx.generar(hojas(s, entrada))
        TipoExportacion.OCTAVE -> octave(s, entrada).encodeToByteArray()
    }

    fun octave(s: SalidaMetodo, entrada: Entrada? = null): String = generador(s).codigoOctave(s, entrada)

    fun hojas(s: SalidaMetodo, entrada: Entrada? = null): List<HojaX> = generador(s).hojas(s, entrada)

    private fun generador(s: SalidaMetodo): GeneradorMetodo =
        Catalogo.generador(s.metodo) ?: throw IllegalStateException("Este método todavía no exporta archivos")

    // ── Piezas comunes de las hojas ──

    fun pasos(s: SalidaMetodo): List<BloquePaso> = s.bloques.filterIsInstance<BloquePaso>()

    fun tabla(s: SalidaMetodo, titulo: String): BloqueTabla =
        s.bloques.filterIsInstance<BloqueTabla>().first { it.titulo == titulo }

    /**
     * Encabezados en negrita; números con `estiloNumero` (0.000000 por defecto); la fila Σ, si la hay, con
     * =SUM(rango) y el valor calculado en caché.
     */
    fun hojaTabla(t: BloqueTabla, nombre: String = t.titulo, estiloNumero: Int = EstiloX.DECIMAL): HojaX {
        val filas = mutableListOf<List<CeldaX>>(t.columnas.map { CeldaX.Texto(it, EstiloX.ENCABEZADO) })
        val datos = if (t.filaTotal) t.filas.dropLast(1) else t.filas
        for (f in datos) filas += f.map { celda(it, estiloNumero) }
        if (t.filaTotal && t.filas.isNotEmpty()) filas += filaSuma(t.filas.last(), datos.size)
        return HojaX(nombre, filas)
    }

    /** Fila Σ: cada número es =SUM de su columna (filas 2..datos+1) con el valor de la app en caché. */
    fun filaSuma(total: List<Celda>, datos: Int): List<CeldaX> = total.mapIndexed { j, c ->
        when (c) {
            is Celda.Num -> {
                val col = LibroXlsx.columna(j)
                if (datos == 0) CeldaX.Numero(c.v, EstiloX.TOTAL) else CeldaX.Formula("SUM(${col}2:$col${datos + 1})", c.v)
            }
            is Celda.Txt -> CeldaX.Texto(c.s, EstiloX.TOTAL_TEXTO)
            is Celda.Entero -> CeldaX.Numero(c.v.toDouble(), EstiloX.TOTAL)
            Celda.Vacia -> CeldaX.Texto("", EstiloX.TOTAL_TEXTO)
        }
    }

    private fun celda(c: Celda, estiloNumero: Int): CeldaX = when (c) {
        is Celda.Num -> CeldaX.Numero(c.v, estiloNumero)
        is Celda.Entero -> CeldaX.Numero(c.v.toDouble(), EstiloX.ENTERO)
        is Celda.Txt -> CeldaX.Texto(c.s)
        Celda.Vacia -> CeldaX.Vacia
    }

    /** Cada matriz rotulada ("Matriz del sistema normal (C)") con sus valores debajo y una fila en blanco. */
    fun hojaMatrices(nombre: String, matrices: List<BloqueMatriz>): HojaX {
        val filas = mutableListOf<List<CeldaX>>()
        for (m in matrices) {
            filas += listOf(CeldaX.Texto("${m.titulo} (${m.nombre})", EstiloX.ENCABEZADO))
            for (f in m.datos) filas += f.map { CeldaX.Numero(it, EstiloX.DECIMAL) }
            filas += emptyList<CeldaX>()
        }
        return HojaX(nombre, filas.dropLastWhile { it.isEmpty() })
    }

    /** Una fila por resultado de cada paso: Paso | Resultado | Fórmula | Valor. */
    fun hojaResultados(pasos: List<BloquePaso>): HojaX {
        val filas = mutableListOf<List<CeldaX>>(
            listOf("Paso", "Resultado", "Fórmula", "Valor").map { CeldaX.Texto(it, EstiloX.ENCABEZADO) },
        )
        for (p in pasos) {
            for (r in p.resultados) {
                val valor = r.valor
                filas += listOf(
                    CeldaX.Texto("${p.numero}. ${p.titulo}"),
                    CeldaX.Texto(r.etiqueta),
                    CeldaX.Texto(p.formulaGeneral.joinToString("   ")),
                    if (valor != null) CeldaX.Numero(valor, EstiloX.DECIMAL) else CeldaX.Texto(r.texto),
                )
            }
        }
        return HojaX("Resultados", filas)
    }

    /** Hoja "Entrada": pares etiqueta | valor y, si se da, una tabla de datos debajo. */
    fun hojaEntrada(pares: List<Pair<String, CeldaX>>, encabezados: List<String> = emptyList(), filas: List<List<CeldaX>> = emptyList()): HojaX {
        val r = mutableListOf<List<CeldaX>>()
        for ((etiqueta, valor) in pares) r += listOf(CeldaX.Texto(etiqueta, EstiloX.ENCABEZADO), valor)
        if (encabezados.isNotEmpty()) {
            r += emptyList<CeldaX>()
            r += encabezados.map { CeldaX.Texto(it, EstiloX.ENCABEZADO) }
            r += filas
        }
        return HojaX("Entrada", r)
    }

    private val TILDES = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')

    /** "Regresión polinomial" → "regresion_polinomial". */
    fun sinTildes(texto: String): String =
        texto.lowercase().map { TILDES[it] ?: it }.joinToString("")
            .replace(Regex("[^a-z0-9]+"), "_").trim('_')
}
