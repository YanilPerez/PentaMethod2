package com.example.pentametod2.exportar

/**
 * Guarda bytes en un archivo que elige el usuario (cada plataforma pone su selector).
 * Android: selector del sistema (crear documento); iOS: aún no disponible.
 */
interface ExportadorArchivos {
    suspend fun guardar(nombreSugerido: String, mime: String, bytes: ByteArray): ResultadoGuardado
}

sealed interface ResultadoGuardado {
    /** `nombre`: el que quedó (el usuario puede cambiarlo). `abrir`: abre el archivo; false si no hay app para hacerlo. */
    class Guardado(val nombre: String, val abrir: (() -> Boolean)? = null) : ResultadoGuardado

    /** El usuario cerró el selector sin elegir: no se muestra nada. */
    data object Cancelado : ResultadoGuardado

    data class Error(val mensaje: String) : ResultadoGuardado

    data object NoDisponible : ResultadoGuardado
}

/** Para plataformas sin selector (iOS por ahora, vistas previas). */
object ExportadorNoDisponible : ExportadorArchivos {
    override suspend fun guardar(nombreSugerido: String, mime: String, bytes: ByteArray): ResultadoGuardado = ResultadoGuardado.NoDisponible
}
