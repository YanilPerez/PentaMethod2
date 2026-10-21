package com.example.pentametod2.exportar

import kotlin.math.abs
import kotlin.math.floor

/** Celda de una hoja: número, texto (inlineStr, sin sharedStrings) o fórmula con su valor en caché. */
sealed interface CeldaX {
    val estilo: Int

    data class Numero(val v: Double, override val estilo: Int = EstiloX.DECIMAL) : CeldaX
    data class Texto(val s: String, override val estilo: Int = EstiloX.NORMAL) : CeldaX

    /** `formula` sin "=" (p. ej. "SUM(B2:B6)"); `valor` se guarda en <v> para verse sin recalcular. */
    data class Formula(val formula: String, val valor: Double, override val estilo: Int = EstiloX.TOTAL) : CeldaX
    data object Vacia : CeldaX {
        override val estilo = EstiloX.NORMAL
    }
}

/** Índices de cellXfs en styles.xml. */
object EstiloX {
    const val NORMAL = 0
    const val ENCABEZADO = 1
    const val DECIMAL = 2      // 0.000000
    const val TOTAL = 3        // negrita + relleno + 0.000000 (fila Σ)
    const val TOTAL_TEXTO = 4  // negrita + relleno
    const val ENTERO = 5       // 0
    const val DECIMAL7 = 6     // 0.0000000 (datos escritos con 7 decimales)
}

data class HojaX(val nombre: String, val filas: List<List<CeldaX>>)

/** Libro .xlsx mínimo y válido: ZIP "stored" con [Content_Types], rels, workbook, styles y una hoja por [HojaX]. */
object LibroXlsx {

    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val CABECERA = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"

    fun generar(hojas: List<HojaX>): ByteArray {
        require(hojas.isNotEmpty()) { "Un libro necesita al menos una hoja" }
        val nombres = nombresValidos(hojas.map { it.nombre })
        val zip = EscritorZip()
        zip.agregar("[Content_Types].xml", tiposDeContenido(hojas.size))
        zip.agregar("_rels/.rels", relacionesRaiz())
        zip.agregar("xl/workbook.xml", libro(nombres))
        zip.agregar("xl/_rels/workbook.xml.rels", relacionesLibro(hojas.size))
        zip.agregar("xl/styles.xml", estilos())
        hojas.forEachIndexed { i, h -> zip.agregar("xl/worksheets/sheet${i + 1}.xml", hoja(h)) }
        return zip.terminar()
    }

    /** 0 → "A", 25 → "Z", 26 → "AA", 701 → "ZZ", 702 → "AAA". */
    fun columna(indice: Int): String {
        require(indice >= 0)
        val sb = StringBuilder()
        var n = indice + 1
        while (n > 0) {
            val r = (n - 1) % 26
            sb.append('A' + r)
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    fun referencia(columna: Int, fila: Int): String = columna(columna) + (fila + 1)

    fun escapar(s: String): String = buildString(s.length) {
        for (c in s) {
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (c < ' ' && c != '\n' && c != '\t' && c != '\r') append(' ') else append(c)
            }
        }
    }

    /** Número para <v>: enteros sin ".0"; el resto con toString (válido como xsd:double). */
    fun numero(v: Double): String =
        if (v == floor(v) && abs(v) < 1e15) v.toLong().toString() else v.toString()

    /** Excel: máximo 31 caracteres, sin []:*?/\ y sin repetir. */
    fun nombresValidos(nombres: List<String>): List<String> {
        val usados = mutableSetOf<String>()
        return nombres.map { original ->
            val base = original.map { if (it in "[]:*?/\\") ' ' else it }.joinToString("").trim().ifEmpty { "Hoja" }.take(31)
            var nombre = base
            var k = 2
            while (nombre.lowercase() in usados) {
                val sufijo = " ($k)"
                nombre = base.take(31 - sufijo.length) + sufijo
                k++
            }
            usados += nombre.lowercase()
            nombre
        }
    }

    private fun tiposDeContenido(hojas: Int) = buildString {
        append(CABECERA)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        for (i in 1..hojas) {
            append("<Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("</Types>")
    }

    private fun relacionesRaiz() = CABECERA +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>"

    private fun libro(nombres: List<String>) = buildString {
        append(CABECERA)
        append("<workbook xmlns=\"$NS\" xmlns:r=\"$NS_R\"><sheets>")
        nombres.forEachIndexed { i, n -> append("<sheet name=\"${escapar(n)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>") }
        append("</sheets></workbook>")
    }

    private fun relacionesLibro(hojas: Int) = buildString {
        append(CABECERA)
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        for (i in 1..hojas) {
            append("<Relationship Id=\"rId$i\" Type=\"$NS_R/worksheet\" Target=\"worksheets/sheet$i.xml\"/>")
        }
        append("<Relationship Id=\"rId${hojas + 1}\" Type=\"$NS_R/styles\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    private fun estilos() = CABECERA +
        "<styleSheet xmlns=\"$NS\">" +
        "<numFmts count=\"2\"><numFmt numFmtId=\"164\" formatCode=\"0.000000\"/><numFmt numFmtId=\"165\" formatCode=\"0.0000000\"/></numFmts>" +
        "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
        "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>" +
        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFCDE7EC\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>" +
        "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"7\">" +
        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
        "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
        "<xf numFmtId=\"164\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>" +
        "<xf numFmtId=\"1\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
        "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
        "</cellXfs>" +
        "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
        "</styleSheet>"

    private fun hoja(h: HojaX) = buildString {
        append(CABECERA)
        append("<worksheet xmlns=\"$NS\">")
        val columnas = h.filas.maxOfOrNull { it.size } ?: 0
        if (columnas > 0) {
            append("<cols>")
            for (j in 0 until columnas) {
                append("<col min=\"${j + 1}\" max=\"${j + 1}\" width=\"${anchoColumna(h, j)}\" customWidth=\"1\"/>")
            }
            append("</cols>")
        }
        append("<sheetData>")
        h.filas.forEachIndexed { i, fila ->
            append("<row r=\"${i + 1}\">")
            fila.forEachIndexed { j, c -> celda(this, c, referencia(j, i)) }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun celda(sb: StringBuilder, c: CeldaX, ref: String) {
        when (c) {
            CeldaX.Vacia -> Unit
            is CeldaX.Numero ->
                if (c.v.isFinite()) {
                    sb.append("<c r=\"$ref\" s=\"${c.estilo}\"><v>${numero(c.v)}</v></c>")
                } else {
                    celda(sb, CeldaX.Texto(c.v.toString(), EstiloX.NORMAL), ref)
                }
            is CeldaX.Texto ->
                sb.append("<c r=\"$ref\" t=\"inlineStr\" s=\"${c.estilo}\"><is><t xml:space=\"preserve\">${escapar(c.s)}</t></is></c>")
            is CeldaX.Formula -> {
                val v = if (c.valor.isFinite()) "<v>${numero(c.valor)}</v>" else ""
                sb.append("<c r=\"$ref\" s=\"${c.estilo}\"><f>${escapar(c.formula)}</f>$v</c>")
            }
        }
    }

    /** Ancho en caracteres: el texto más largo de la columna (los números con 6 decimales ocupan ~12). */
    private fun anchoColumna(h: HojaX, j: Int): Int {
        val largo = h.filas.maxOfOrNull { f ->
            when (val c = f.getOrNull(j)) {
                is CeldaX.Texto -> c.s.length
                is CeldaX.Numero, is CeldaX.Formula -> 12
                else -> 0
            }
        } ?: 0
        return (largo + 2).coerceIn(8, 60)
    }
}
