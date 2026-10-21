package com.example.pentametod2.exportar

import kotlin.test.assertEquals

/** Mini-lector de ZIP "stored" y verificador de XML para los tests de exportación (Fases 7 y 8). */
internal object ZipDePrueba {

    private fun u16(b: ByteArray, i: Int) = (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)
    private fun u32(b: ByteArray, i: Int) = u16(b, i) or (u16(b, i + 2) shl 16)

    /** Partes del ZIP como texto UTF-8, leídas desde el directorio central (comprueba firmas y CRC). */
    fun leer(b: ByteArray): Map<String, String> {
        val fin = b.size - 22
        assertEquals(0x06054b50, u32(b, fin), "falta el fin de directorio")
        val total = u16(b, fin + 10)
        var p = u32(b, fin + 16)
        val r = linkedMapOf<String, String>()
        repeat(total) {
            assertEquals(0x02014b50, u32(b, p))
            val crc = u32(b, p + 16)
            val tam = u32(b, p + 24)
            val largoNombre = u16(b, p + 28)
            val local = u32(b, p + 42)
            val nombre = b.copyOfRange(p + 46, p + 46 + largoNombre).decodeToString()
            val inicio = local + 30 + u16(b, local + 26) + u16(b, local + 28)
            val datos = b.copyOfRange(inicio, inicio + tam)
            assertEquals(crc, Crc32.de(datos), "CRC de $nombre")
            r[nombre] = datos.decodeToString()
            p += 46 + largoNombre + u16(b, p + 30) + u16(b, p + 32)
        }
        return r
    }

    /** Etiquetas balanceadas y sin '&' sin escapar. */
    fun xmlBalanceado(xml: String): Boolean {
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

    /** Nombres de las hojas en el orden del libro. */
    fun hojas(partes: Map<String, String>): List<String> =
        Regex("<sheet name=\"([^\"]+)\"").findAll(partes.getValue("xl/workbook.xml")).map { it.groupValues[1] }.toList()

    /** XML de la hoja con ese nombre. */
    fun hoja(partes: Map<String, String>, nombre: String): String =
        partes.getValue("xl/worksheets/sheet${hojas(partes).indexOf(nombre) + 1}.xml")

    /** Todos los valores numéricos (<v>) del libro. */
    fun numeros(partes: Map<String, String>): List<Double> =
        partes.values.flatMap { x -> Regex("<v>([^<]+)</v>").findAll(x).map { it.groupValues[1].toDouble() } }
}
