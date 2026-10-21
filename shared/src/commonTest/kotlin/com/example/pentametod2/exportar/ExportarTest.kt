package com.example.pentametod2.exportar

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.EntradaRegresionPolinomial
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.ui.resultados.AccionMenu
import com.example.pentametod2.ui.resultados.NO_DISPONIBLE
import com.example.pentametod2.ui.resultados.alternarPaso
import com.example.pentametod2.ui.resultados.itemsMenu
import com.example.pentametod2.ui.resultados.pasosAbiertos
import com.example.pentametod2.ui.resultados.pasosDe
import com.example.pentametod2.ui.resultados.pasosIniciales
import com.example.pentametod2.ui.resultados.textoDesliza
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportarTest {

    private val ejemplo: SalidaMetodo = Catalogo.ejecutar(MetodoId.REGRESION_POLINOMIAL, Catalogo.ejemplo(MetodoId.REGRESION_POLINOMIAL))
    private val sinPrediccion: SalidaMetodo =
        Catalogo.ejecutar(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1,2,3,4,5", "9,7.5,4.2,3,2.1", "2"))

    // ── Octave ──

    @Test
    fun octaveDelEjemplo() {
        val t = Exportaciones.octave(ejemplo)
        assertTrue(t.contains("x = [1 2 3 4 5]';"), t)
        assertTrue(t.contains("y = [9 7.5 4.2 3 2.1]';"), t)
        assertTrue(t.contains("m = 2;"))
        assertTrue(t.contains("a = C\\b;"))
        assertTrue(t.contains("xp = 2.5;"))
        assertTrue(t.contains("Pxp = polyval(flipud(a), xp);"))
        // Los 6 comentarios, exactos y en orden.
        var desde = 0
        for (c in GeneradorOctave.PASOS) {
            val i = t.indexOf("% $c\n", desde)
            assertTrue(i >= 0, "falta o fuera de orden: $c")
            desde = i
        }
        assertEquals("% 1. Sistema normal C*a = b", "% " + GeneradorOctave.PASOS[0])
        assertFalse(t.contains("\r"), "solo \\n como salto de línea")
        assertFalse(t.contains("end;") || t.contains("fprintf("), "nada de MATLAB-only")
        assertTrue(t.endsWith("\n"))
    }

    @Test
    fun octaveSinPrediccion() {
        val t = Exportaciones.octave(sinPrediccion)
        assertFalse(t.contains("xp ="), t)
        assertFalse(t.contains("Pxp"), t)
        assertFalse(t.contains("% 6. Predicción"), t)
        assertTrue(t.contains("% 5. Error estándar"))
        assertEquals("0.001", GeneradorOctave.numero(0.001))
        assertEquals("-2.5", GeneradorOctave.numero(-2.5))
    }

    @Test
    fun nombresDeArchivo() {
        assertEquals("regresion_polinomial_grado2.txt", Exportaciones.nombreArchivo(ejemplo, TipoExportacion.OCTAVE))
        assertEquals("regresion_polinomial_grado2.xlsx", Exportaciones.nombreArchivo(ejemplo, TipoExportacion.EXCEL))
        assertEquals("newton_raphson_para_sistemas", Exportaciones.sinTildes("Newton-Raphson para sistemas"))
        assertEquals("interpolacion", Exportaciones.sinTildes("Interpolación"))
    }

    // ── ZIP ──

    @Test
    fun crc32Conocido() {
        assertEquals(0xCBF43926.toInt(), Crc32.de("123456789".encodeToByteArray()))
        assertEquals(0, Crc32.de(ByteArray(0)))
    }

    @Test
    fun zipStoredBienFormado() {
        val z = EscritorZip()
        z.agregar("a.txt", "hola")
        z.agregar("carpeta/b.xml", "<x/>")
        val bytes = z.terminar()
        assertEquals(listOf(0x50, 0x4B, 0x03, 0x04), bytes.take(4).map { it.toInt() and 0xFF })
        val partes = leerZip(bytes)
        assertEquals(mapOf("a.txt" to "hola", "carpeta/b.xml" to "<x/>"), partes.mapValues { it.value.decodeToString() })
    }

    // ── XLSX ──

    @Test
    fun columnasDeExcel() {
        assertEquals("A", LibroXlsx.columna(0))
        assertEquals("Z", LibroXlsx.columna(25))
        assertEquals("AA", LibroXlsx.columna(26)) // la 27.ª
        assertEquals("AZ", LibroXlsx.columna(51))
        assertEquals("ZZ", LibroXlsx.columna(701))
        assertEquals("AAA", LibroXlsx.columna(702))
        assertEquals("C7", LibroXlsx.referencia(2, 6))
        assertEquals("a &amp; b &lt;c&gt; &quot;d&quot; &apos;e&apos;", LibroXlsx.escapar("a & b <c> \"d\" 'e'"))
        assertEquals(listOf("Datos", "datos (2)", "a b"), LibroXlsx.nombresValidos(listOf("Datos", "datos", "a/b")))
        assertEquals("15", LibroXlsx.numero(15.0))
        assertEquals("0.5", LibroXlsx.numero(0.5))
    }

    @Test
    fun xlsxDelEjemplo() {
        val partes = leerZip(Exportaciones.contenido(ejemplo, TipoExportacion.EXCEL)).mapValues { it.value.decodeToString() }
        val obligatorias = listOf(
            "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels", "xl/styles.xml",
            "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml", "xl/worksheets/sheet4.xml",
        )
        for (p in obligatorias) assertTrue(p in partes, "falta $p")
        assertEquals(obligatorias.size, partes.size)
        for ((nombre, xml) in partes) assertTrue(xmlBalanceado(xml), "XML mal formado: $nombre")

        val libro = partes.getValue("xl/workbook.xml")
        for (h in listOf("Datos y sumatorias", "Sistema normal", "Resultados", "Entrada")) assertTrue(libro.contains("name=\"$h\""), h)

        val datos = partes.getValue("xl/worksheets/sheet1.xml")
        assertEquals(7, Regex("<row ").findAll(datos).count()) // encabezado + 5 filas + Σ
        assertTrue(datos.contains("<f>SUM(B2:B6)</f><v>15</v>"), "Σxᵢ como fórmula con su valor")
        assertTrue(datos.contains("<f>SUM(K2:K6)</f>"), "11 columnas: la última es K")
        assertEquals(10, Regex("<f>SUM\\(").findAll(datos).count()) // todas menos la columna i
        assertTrue(datos.contains("<t xml:space=\"preserve\">xᵢ²</t>"))

        val numeros = partes.values.flatMap { x -> Regex("<v>([^<]+)</v>").findAll(x).map { it.groupValues[1].toDouble() } }
        for (esperado in listOf(0.905143, 0.974265, 12.3, 35.172, 0.987049, 0.672734, 5.6625)) {
            assertTrue(numeros.any { abs(it - esperado) < 5e-7 }, "no aparece $esperado")
        }
        assertTrue(partes.getValue("xl/styles.xml").contains("formatCode=\"0.000000\""))
        assertTrue(partes.getValue("xl/worksheets/sheet3.xml").contains("12.300000 − 3.244286x + 0.235714x²"), "ecuación en texto")
    }

    @Test
    fun finDeDirectorioConElConteo() {
        val bytes = Exportaciones.contenido(ejemplo, TipoExportacion.EXCEL)
        val fin = bytes.size - 22
        assertEquals(listOf(0x50, 0x4B, 0x05, 0x06), (fin until fin + 4).map { bytes[it].toInt() and 0xFF })
        assertEquals(9, u16(bytes, fin + 10))
    }

    // ── Menú y lista ──

    @Test
    fun menuSegunElMetodo() {
        val items = itemsMenu(ejemplo)
        assertEquals(listOf(AccionMenu.EXCEL, AccionMenu.OCTAVE, AccionMenu.COPIAR, AccionMenu.AYUDA), items.map { it.accion })
        assertTrue(items.all { it.habilitado })
        val lagrange = itemsMenu(Catalogo.ejecutar(MetodoId.LAGRANGE, Catalogo.ejemplo(MetodoId.LAGRANGE)))
        // Desde la Fase 8 los 6 métodos exportan.
        assertEquals(listOf(true, true, true, true), lagrange.map { it.habilitado })
        assertTrue(lagrange.none { it.sub == NO_DISPONIBLE })
        // Con error de entrada no se exporta.
        val conError = Catalogo.ejecutar(MetodoId.REGRESION_POLINOMIAL, EntradaRegresionPolinomial("1,2", "1", "1"))
        assertFalse(itemsMenu(conError)[0].habilitado)
    }

    @Test
    fun abrirYCerrarPasos() {
        val inicial = pasosIniciales(pasosDe(ejemplo))
        assertEquals(setOf(1), pasosAbiertos(inicial))
        val dos = alternarPaso(inicial, 3)
        assertEquals(setOf(1, 3), pasosAbiertos(dos)) // varios abiertos a la vez
        assertEquals(setOf(3), pasosAbiertos(alternarPaso(dos, 1)))
        assertEquals(emptySet(), pasosAbiertos(""))
        assertEquals("Desliza para ver las 11 columnas →", textoDesliza(11, conPasos = true))
        assertEquals("Desliza para ver más columnas →", textoDesliza(11, conPasos = false))
    }

    // ── Auxiliares: lector ZIP (stored) y verificador de XML ──

    private fun u16(b: ByteArray, i: Int) = (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)
    private fun u32(b: ByteArray, i: Int) = u16(b, i) or (u16(b, i + 2) shl 16)

    /** Lee el directorio central desde el fin de directorio y extrae cada entrada "stored", comprobando su CRC. */
    private fun leerZip(b: ByteArray): Map<String, ByteArray> {
        val fin = b.size - 22
        assertEquals(0x06054b50, u32(b, fin), "falta el fin de directorio")
        val total = u16(b, fin + 10)
        var p = u32(b, fin + 16)
        val r = linkedMapOf<String, ByteArray>()
        repeat(total) {
            assertEquals(0x02014b50, u32(b, p))
            assertEquals(0, u16(b, p + 10), "método stored")
            val crc = u32(b, p + 16)
            val tam = u32(b, p + 24)
            val largoNombre = u16(b, p + 28)
            val local = u32(b, p + 42)
            val nombre = b.copyOfRange(p + 46, p + 46 + largoNombre).decodeToString()
            assertEquals(0x04034b50, u32(b, local))
            val inicio = local + 30 + u16(b, local + 26) + u16(b, local + 28)
            val datos = b.copyOfRange(inicio, inicio + tam)
            assertEquals(crc, Crc32.de(datos), "CRC de $nombre")
            r[nombre] = datos
            p += 46 + largoNombre + u16(b, p + 30) + u16(b, p + 32)
        }
        return r
    }

    /** Etiquetas balanceadas (ignora la declaración <?xml?> y las autocerradas). */
    private fun xmlBalanceado(xml: String): Boolean {
        val pila = ArrayDeque<String>()
        for (m in Regex("<(/?)([A-Za-z_][\\w:.-]*)([^>]*?)(/?)>").findAll(xml)) {
            val (cierre, nombre, _, auto) = m.destructured
            when {
                auto == "/" -> Unit
                cierre == "/" -> if (pila.removeLastOrNull() != nombre) return false
                else -> pila.addLast(nombre)
            }
        }
        return pila.isEmpty() && !Regex("&(?!amp;|lt;|gt;|quot;|apos;)").containsMatchIn(xml)
    }
}
