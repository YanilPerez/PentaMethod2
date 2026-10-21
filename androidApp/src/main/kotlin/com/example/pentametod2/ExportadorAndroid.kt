package com.example.pentametod2

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.example.pentametod2.exportar.ExportadorArchivos
import com.example.pentametod2.exportar.ResultadoGuardado
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lo que hay que guardar mientras el selector del sistema está abierto. Vive en un ViewModel para sobrevivir
 * a la rotación: la actividad nueva recibe el Uri y escribe los bytes. Si quien esperaba ya no existe (la
 * pantalla se recreó), el aviso sale como Toast en lugar del Snackbar.
 */
class GuardadoPendiente : ViewModel() {
    var bytes: ByteArray? = null
    var mime: String = "application/octet-stream"
    var espera: CompletableDeferred<ResultadoGuardado>? = null

    /**
     * Corrutina de la pantalla que espera el resultado. Al rotar, la composición vieja se descarta y este Job
     * se cancela enseguida (aunque la corrutina reanude después): así se sabe que nadie mostrará el Snackbar.
     */
    var quienEspera: Job? = null

    fun limpiar() {
        bytes = null
        espera = null
        quienEspera = null
    }
}

/** CreateDocument con el MIME de cada archivo (el contrato base lo fija al construirse). */
private class CrearDocumento(private val mime: () -> String) : ActivityResultContracts.CreateDocument("application/octet-stream") {
    override fun createIntent(context: Context, input: String): Intent = super.createIntent(context, input).setType(mime())
}

/**
 * [ExportadorArchivos] de Android: abre el selector "Guardar como" del sistema (sin permisos de almacenamiento)
 * y escribe los bytes en el Uri elegido con el ContentResolver. Se crea en onCreate (antes de STARTED).
 */
class ExportadorAndroid(private val actividad: ComponentActivity, private val pendiente: GuardadoPendiente) : ExportadorArchivos {

    private val selector: ActivityResultLauncher<String> =
        actividad.registerForActivityResult(CrearDocumento { pendiente.mime }) { uri -> alElegir(uri) }

    override suspend fun guardar(nombreSugerido: String, mime: String, bytes: ByteArray): ResultadoGuardado {
        val espera = CompletableDeferred<ResultadoGuardado>()
        pendiente.bytes = bytes
        pendiente.mime = mime
        pendiente.espera = espera
        pendiente.quienEspera = currentCoroutineContext()[Job]
        return try {
            selector.launch(nombreSugerido)
            espera.await()
        } catch (e: ActivityNotFoundException) {
            pendiente.limpiar()
            ResultadoGuardado.Error("No hay un selector de archivos en este dispositivo")
        }
        // Si la pantalla se recrea (rotación) con el selector abierto, los bytes siguen en el ViewModel: la
        // actividad nueva los escribe y, como quien esperaba ya no está activo, avisa con un Toast.
    }

    private fun alElegir(uri: Uri?) {
        val bytes = pendiente.bytes
        val espera = pendiente.espera
        val quienEspera = pendiente.quienEspera
        val mime = pendiente.mime
        pendiente.limpiar()
        if (bytes == null) return
        if (uri == null) {
            espera?.complete(ResultadoGuardado.Cancelado)
            return
        }
        actividad.lifecycleScope.launch {
            val nombre = withContext(Dispatchers.IO) {
                runCatching {
                    val salida = actividad.contentResolver.openOutputStream(uri, "wt") ?: error("sin flujo de salida")
                    salida.use { it.write(bytes) }
                    nombreVisible(uri)
                }
            }
            val resultado = nombre.fold(
                onSuccess = { ResultadoGuardado.Guardado(it) { abrir(uri, mime) } },
                onFailure = { ResultadoGuardado.Error(it.message ?: "error de escritura") },
            )
            if (espera != null && quienEspera?.isActive == true) {
                espera.complete(resultado)
            } else {
                val texto = if (resultado is ResultadoGuardado.Guardado) "Guardado: ${resultado.nombre}" else "No se pudo guardar el archivo"
                Toast.makeText(actividad, texto, Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Nombre que quedó en el selector (el usuario puede cambiarlo); si no se puede leer, el último segmento del Uri. */
    private fun nombreVisible(uri: Uri): String {
        actividad.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) c.getString(i)?.let { return it }
            }
        }
        return uri.lastPathSegment ?: "archivo"
    }

    private fun abrir(uri: Uri, mime: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        actividad.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
