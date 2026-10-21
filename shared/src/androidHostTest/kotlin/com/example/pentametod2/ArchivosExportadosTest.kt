package com.example.pentametod2

import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.exportar.Exportaciones
import com.example.pentametod2.exportar.TipoExportacion
import com.example.pentametod2.model.BloquePaso
import com.example.pentametod2.model.MetodoId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Escribe los archivos de los ejemplos de los 6 métodos en shared/build/exportados/ (y un volcado de sus pasos)
 * para revisarlos a mano o validarlos con herramientas externas (openpyxl, Octave). No compara contenido:
 * eso lo hacen los tests de commonTest.
 */
class ArchivosExportadosTest {
    @Test
    fun escribeLosArchivosDeLosEjemplos() {
        val carpeta = File("build/exportados").apply { mkdirs() }
        for (id in MetodoId.entries) {
            val entrada = Catalogo.ejemplo(id)
            val s = Catalogo.ejecutar(id, entrada)
            for (tipo in TipoExportacion.entries) {
                val f = File(carpeta, Exportaciones.nombreArchivo(s, tipo))
                f.writeBytes(Exportaciones.contenido(s, tipo, entrada))
                assertTrue(f.length() > 0, f.path)
            }
            val volcado = s.bloques.filterIsInstance<BloquePaso>().joinToString("\n\n") { p ->
                buildString {
                    appendLine("${p.numero}. ${p.titulo}  [${p.resumenCorto}]${if (p.abiertoInicial) " (abierto)" else ""}${if (p.destacado) " (destacado)" else ""}")
                    appendLine("  ${p.explicacion}")
                    p.formulaGeneral.forEach { appendLine("  F: $it") }
                    p.conTusDatos.forEach { appendLine("  D: $it") }
                    p.matrices.forEach { appendLine("  M: ${it.nombre} ${it.datos}") }
                    p.tablas.forEach { appendLine("  T: ${it.titulo} ${it.columnas}") }
                    p.resultados.forEach { append("  R: ${it.etiqueta} = ${it.texto}\n") }
                }
            }
            File(carpeta, "pasos_${id.name.lowercase()}.txt").writeText(volcado)
        }
    }
}
