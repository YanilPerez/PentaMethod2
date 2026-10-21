package com.example.pentametod2.exportar

/** CRC-32 (IEEE 802.3, polinomio 0xEDB88320), el que usa ZIP. crc32("123456789") = 0xCBF43926. */
object Crc32 {
    private val TABLA = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
        c
    }

    fun de(datos: ByteArray): Int {
        var c = -1
        for (b in datos) c = TABLA[(c xor b.toInt()) and 0xFF] xor (c ushr 8)
        return c.inv()
    }
}

/** Arreglo de bytes que crece, con escritura little-endian (Kotlin común no tiene ByteArrayOutputStream). */
internal class Bytes {
    private var buf = ByteArray(4096)
    var tamano = 0
        private set

    private fun asegurar(n: Int) {
        if (tamano + n <= buf.size) return
        var nuevo = buf.size * 2
        while (nuevo < tamano + n) nuevo *= 2
        buf = buf.copyOf(nuevo)
    }

    fun u8(v: Int) {
        asegurar(1)
        buf[tamano++] = v.toByte()
    }

    fun u16(v: Int) {
        u8(v and 0xFF)
        u8((v ushr 8) and 0xFF)
    }

    fun u32(v: Int) {
        u16(v and 0xFFFF)
        u16((v ushr 16) and 0xFFFF)
    }

    fun todo(b: ByteArray) {
        asegurar(b.size)
        b.copyInto(buf, tamano)
        tamano += b.size
    }

    fun aArreglo(): ByteArray = buf.copyOf(tamano)
}

/**
 * ZIP mínimo sin compresión (método 0, "stored"): encabezados locales, directorio central y fin de directorio.
 * Nombres en UTF-8 (bit 11) y fecha fija 1980-01-01 para que el resultado sea reproducible.
 */
class EscritorZip {
    private class Entrada(val nombre: ByteArray, val crc: Int, val tamano: Int, val desplazamiento: Int)

    private val salida = Bytes()
    private val entradas = mutableListOf<Entrada>()

    fun agregar(nombre: String, datos: ByteArray) {
        val n = nombre.encodeToByteArray()
        val e = Entrada(n, Crc32.de(datos), datos.size, salida.tamano)
        salida.u32(FIRMA_LOCAL)
        salida.u16(VERSION)
        salida.u16(BANDERA_UTF8)
        salida.u16(0) // stored
        salida.u16(HORA)
        salida.u16(FECHA)
        salida.u32(e.crc)
        salida.u32(e.tamano)
        salida.u32(e.tamano)
        salida.u16(n.size)
        salida.u16(0)
        salida.todo(n)
        salida.todo(datos)
        entradas += e
    }

    fun agregar(nombre: String, texto: String) = agregar(nombre, texto.encodeToByteArray())

    fun terminar(): ByteArray {
        val inicioCentral = salida.tamano
        for (e in entradas) {
            salida.u32(FIRMA_CENTRAL)
            salida.u16(VERSION) // hecho por
            salida.u16(VERSION) // necesaria
            salida.u16(BANDERA_UTF8)
            salida.u16(0)
            salida.u16(HORA)
            salida.u16(FECHA)
            salida.u32(e.crc)
            salida.u32(e.tamano)
            salida.u32(e.tamano)
            salida.u16(e.nombre.size)
            salida.u16(0) // extra
            salida.u16(0) // comentario
            salida.u16(0) // disco
            salida.u16(0) // atributos internos
            salida.u32(0) // atributos externos
            salida.u32(e.desplazamiento)
            salida.todo(e.nombre)
        }
        val tamanoCentral = salida.tamano - inicioCentral
        salida.u32(FIRMA_FIN)
        salida.u16(0)
        salida.u16(0)
        salida.u16(entradas.size)
        salida.u16(entradas.size)
        salida.u32(tamanoCentral)
        salida.u32(inicioCentral)
        salida.u16(0)
        return salida.aArreglo()
    }

    private companion object {
        const val FIRMA_LOCAL = 0x04034b50
        const val FIRMA_CENTRAL = 0x02014b50
        const val FIRMA_FIN = 0x06054b50
        const val VERSION = 20
        const val BANDERA_UTF8 = 0x0800
        const val HORA = 0
        const val FECHA = (0 shl 9) or (1 shl 5) or 1 // 1980-01-01
    }
}
