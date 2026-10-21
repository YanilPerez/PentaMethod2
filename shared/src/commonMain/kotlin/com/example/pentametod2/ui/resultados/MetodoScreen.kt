package com.example.pentametod2.ui.resultados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.pentametod2.exportar.ExportadorArchivos
import com.example.pentametod2.exportar.Exportaciones
import com.example.pentametod2.exportar.ResultadoGuardado
import com.example.pentametod2.exportar.TipoExportacion
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pentametod2.catalogo.Catalogo
import com.example.pentametod2.catalogo.Entrada
import com.example.pentametod2.catalogo.MetodoInfo
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.model.SalidaMetodo
import com.example.pentametod2.ui.componentes.BarraSuperior
import com.example.pentametod2.ui.componentes.BotonTexto
import com.example.pentametod2.ui.componentes.ContenidoCentrado
import com.example.pentametod2.ui.componentes.EtiquetaSeccion
import com.example.pentametod2.ui.componentes.TarjetaContorno
import com.example.pentametod2.ui.componentes.TituloTarjeta
import com.example.pentametod2.ui.formularios.EstadoFormulario
import com.example.pentametod2.ui.formularios.FormularioOpcionalSaver
import com.example.pentametod2.ui.formularios.FormularioSaver
import com.example.pentametod2.ui.formularios.LineaEntrada
import com.example.pentametod2.ui.formularios.DockAcciones
import com.example.pentametod2.ui.formularios.TarjetasEdicion
import com.example.pentametod2.ui.formularios.bloqueaCalculo
import com.example.pentametod2.ui.formularios.conVistaDe
import com.example.pentametod2.ui.formularios.estadoDesdeEntrada
import com.example.pentametod2.ui.formularios.estadoInicial
import com.example.pentametod2.ui.formularios.paraMetodo
import com.example.pentametod2.ui.formularios.resumenEntrada
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.mono
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TEXTO_GUIA = "Escribe tus datos y pulsa Calcular, o carga un ejemplo."
const val TEXTO_GUIA_PIE = "Aquí verás la fórmula, el resumen, la gráfica y la tabla de pasos."
const val TEXTO_AYUDA_RESULTADO = "Verás fórmula, resumen, gráfica y tabla de pasos."

/** Frase corta de cada método para el subtítulo de la barra (derivada de su descripción del catálogo). */
private val FRASES_CORTAS = mapOf(
    MetodoId.PUNTO_FIJO to "itera x = G(x)",
    MetodoId.NEWTON_SISTEMAS to "usa el jacobiano",
    MetodoId.REGRESION_POLINOMIAL to "mínimos cuadrados",
    MetodoId.REGRESION_MULTIPLE to "y en función de x₁…xₖ",
    MetodoId.LAGRANGE to "pasa por cada punto",
    MetodoId.NEWTON_INTERPOLACION to "diferencias divididas",
)

/** "Ajuste de curvas · mínimos cuadrados"; solo la sección si el método no tiene frase corta. */
fun subtituloMetodo(info: MetodoInfo): String = FRASES_CORTAS[info.id]?.let { "${info.seccion} · $it" } ?: info.seccion

/**
 * Pantalla de un método con dos estados:
 *  1. EDICIÓN: tarjetas del formulario (y el estado vacío si aún no hay resultado).
 *  2. RESULTADO: la entrada comprimida (solo lectura, botón "Editar") y los resultados.
 *
 * Se guarda (rememberSaveable) el formulario, el formulario con el que se calculó y si se está editando;
 * la [SalidaMetodo] no se guarda: se recalcula al restaurar (rotación, volver del menú).
 */
@Composable
fun MetodoScreen(info: MetodoInfo, alVolver: () -> Unit, alAbrirMetodo: (MetodoId) -> Unit = {}) {
    val id = info.id
    var formulario by rememberSaveable(stateSaver = FormularioSaver) { mutableStateOf(estadoInicial(id)) }
    var calculado by rememberSaveable(stateSaver = FormularioOpcionalSaver) { mutableStateOf<EstadoFormulario?>(null) }
    var numCalculo by rememberSaveable { mutableIntStateOf(0) }
    var editando by rememberSaveable { mutableStateOf(true) }
    var ayuda by rememberSaveable { mutableStateOf(false) }
    var salida by remember { mutableStateOf<SalidaMetodo?>(null) }
    var calculando by remember { mutableStateOf(false) }
    var intentoFallido by remember { mutableIntStateOf(0) }
    val enEdicion = editando || calculado == null
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val exportador = LocalExportador.current
    @Suppress("DEPRECATION") // LocalClipboard (la API nueva) necesita un ClipEntry que solo se construye por plataforma.
    val portapapeles = LocalClipboardManager.current

    LaunchedEffect(id) {
        formulario = formulario.paraMetodo(id)
        calculado = calculado?.paraMetodo(id)
    }

    LaunchedEffect(numCalculo) {
        val datos = calculado ?: return@LaunchedEffect
        calculando = true
        salida = withContext(Dispatchers.Default) { Catalogo.ejecutar(id, datos.aEntrada()) }
        calculando = false
    }

    Scaffold(
        topBar = {
            val s = if (id == MetodoId.LAGRANGE && (calculando || (enEdicion && formulario != calculado))) null else salida
            BarraSuperior(
                info.nombre,
                alVolver,
                subtitulo = subtituloMetodo(info),
                alAyuda = { ayuda = true },
                cargando = calculando,
                // Con resultado, el "?" se cambia por el menú de guardar/exportar (que incluye la ayuda).
                // Si no se calculó nada (error de entrada), sigue el "?".
                accion = if (s == null || Exportaciones.sinCalculo(s)) null else {
                    {
                        MenuResultados(s) { accion ->
                            when (accion) {
                                AccionMenu.AYUDA -> ayuda = true
                                AccionMenu.COPIAR -> {
                                    portapapeles.setText(AnnotatedString(textoParaCopiar(s)))
                                    alcance.launch { avisos.showSnackbar(TEXTO_COPIADO) }
                                }
                                AccionMenu.EXCEL -> alcance.launch { exportar(s, calculado?.aEntrada(), TipoExportacion.EXCEL, exportador, avisos) }
                                AccionMenu.OCTAVE -> alcance.launch { exportar(s, calculado?.aEntrada(), TipoExportacion.OCTAVE, exportador, avisos) }
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
        bottomBar = {
            if (enEdicion) {
                DockAcciones(
                    calculando = calculando,
                    puedeCalcular = id != MetodoId.LAGRANGE || !bloqueaCalculo(formulario),
                    alCargarEjemplo = { formulario = estadoDesdeEntrada(Catalogo.ejemplo(id)).paraMetodo(id).conVistaDe(formulario) },
                    alCalcular = {
                        if (bloqueaCalculo(formulario)) {
                            intentoFallido++
                        } else {
                            calculado = formulario
                            editando = false
                            numCalculo++
                        }
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            ContenidoCentrado(Modifier.padding(horizontal = Dimens.margenLateral, vertical = Dimens.e8)) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.entreTarjetas)) {
                    val datosCalculados = calculado
                    if (enEdicion || datosCalculados == null) {
                        TarjetasEdicion(
                            id = id,
                            estado = formulario,
                            alCambiar = { formulario = it },
                            intentoFallido = intentoFallido,
                        )
                    } else {
                        TarjetaEntradaComprimida(resumenEntrada(id, datosCalculados)) { editando = true }
                    }

                    val s = if (id == MetodoId.LAGRANGE && enEdicion && formulario != calculado) null else salida
                    if (s == null) {
                        if (!calculando) LineaAyuda()
                    } else {
                        ResultadosMetodo(
                            salida = s,
                            alEditar = { editando = true },
                            alAbrirMetodo = alAbrirMetodo,
                            claveCalculo = numCalculo,
                        )
                    }
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars).heightIn(min = Dimens.e24))
        }
    }

    if (ayuda) DialogoAyuda(info) { ayuda = false }
}

/**
 * Genera el archivo (fuera del hilo principal), abre el selector de la plataforma y avisa con un Snackbar:
 * "Guardado: <nombre>" con ABRIR si se puede; nada si se canceló; "No se pudo guardar el archivo" si falló.
 */
private suspend fun exportar(
    s: SalidaMetodo,
    entrada: Entrada?,
    tipo: TipoExportacion,
    exportador: ExportadorArchivos,
    avisos: SnackbarHostState,
) {
    val bytes = try {
        withContext(Dispatchers.Default) { Exportaciones.contenido(s, tipo, entrada) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        avisos.showSnackbar(TEXTO_ERROR_GUARDADO)
        return
    }
    when (val r = exportador.guardar(Exportaciones.nombreArchivo(s, tipo), tipo.mime, bytes)) {
        is ResultadoGuardado.Guardado -> {
            val abrir = r.abrir
            val respuesta = avisos.showSnackbar(
                "Guardado: ${r.nombre}",
                actionLabel = if (abrir != null) "ABRIR" else null,
                duration = SnackbarDuration.Long,
            )
            if (respuesta == SnackbarResult.ActionPerformed && abrir != null && !abrir()) {
                avisos.showSnackbar("No hay una app para abrir este archivo")
            }
        }
        ResultadoGuardado.Cancelado -> Unit
        is ResultadoGuardado.Error -> avisos.showSnackbar(TEXTO_ERROR_GUARDADO)
        ResultadoGuardado.NoDisponible -> avisos.showSnackbar("Guardar archivos aún no está disponible en esta plataforma")
    }
}

const val TEXTO_ERROR_GUARDADO = "No se pudo guardar el archivo"

/** Datos en modo RESULTADO: solo lectura, con "Editar". */
@Composable
private fun TarjetaEntradaComprimida(lineas: List<LineaEntrada>, alEditar: () -> Unit) {
    TarjetaContorno(espacio = Dimens.e4) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                EtiquetaSeccion("Entrada")
                TituloTarjeta("Datos")
            }
            BotonTexto("Editar", alEditar, icono = Iconos.editar)
        }
        Column(Modifier.padding(top = Dimens.e8), verticalArrangement = Arrangement.spacedBy(Dimens.e4)) {
            for (l in lineas) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.e12)) {
                    Text(
                        l.etiqueta,
                        style = MaterialTheme.mono.dato,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(88.dp),
                    )
                    Text(l.valor, style = MaterialTheme.mono.dato, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Ayuda de una fila mientras no hay resultado: borde punteado 1dp outlineVariant, radio 12, ícono ⓘ de 16. */
@Composable
private fun LineaAyuda() {
    val borde = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val r = 12.dp.toPx()
                drawRoundRect(
                    color = borde,
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = Dimens.borde.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .padding(horizontal = Dimens.e12, vertical = Dimens.e12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.e8),
    ) {
        Icono(Iconos.info, null, tamano = Dimens.iconoInfo, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            TEXTO_AYUDA_RESULTADO,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DialogoAyuda(info: MetodoInfo, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        confirmButton = { BotonTexto("Entendido", alCerrar) },
        title = { Text(info.nombre, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                Text(info.descripcion + ".", style = MaterialTheme.typography.bodyLarge)
                Text(TEXTO_GUIA, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(TEXTO_GUIA_PIE, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = Formas.tarjeta,
        tonalElevation = 0.dp,
    )
}
