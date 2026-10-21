package com.example.pentametod2.ui.formularios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.pentametod2.catalogo.*
import com.example.pentametod2.ui.componentes.*
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioLagrange(estado: EstadoLagrange, alCambiar: (EstadoFormulario) -> Unit, intentoFallido: Int) {
    val cambiar: (EstadoLagrange) -> Unit = { alCambiar(it) }
    val errores = DatosLagrange.errores(estado.puntos)
    var borrador by remember { mutableStateOf<EstadoLagrange?>(null) }
    var gradoActivo by rememberSaveable { mutableIntStateOf(1) }
    var pegar by rememberSaveable { mutableStateOf(false) }
    var textoPegado by rememberSaveable { mutableStateOf("") }
    var errorPegado by rememberSaveable { mutableStateOf<String?>(null) }
    var enfocarPunto by rememberSaveable { mutableStateOf<Int?>(null) }
    @Suppress("DEPRECATION")
    val portapapeles = LocalClipboardManager.current
    val focos = remember(estado.puntos.map { it.id }) {
        estado.puntos.associate { it.id to (FocusRequester() to FocusRequester()) }
    }
    val focoListaX = remember { FocusRequester() }
    val focoListaY = remember { FocusRequester() }
    val focoObjetivo = remember { FocusRequester() }
    LaunchedEffect(enfocarPunto, estado.vista) {
        val id = enfocarPunto
        if (id != null && estado.vista == VistaDatos.TABLA) {
            focos[id]?.first?.requestFocus()
            enfocarPunto = null
        }
    }
    LaunchedEffect(intentoFallido) {
        if (intentoFallido == 0) return@LaunchedEffect
        val id = errores.keys.firstOrNull()
        if (id != null) {
            val p = estado.puntos.first { it.id == id }
            val enX = DatosLagrange.numeroCelda(p.x) == null || errores[id]?.contains("repetido") == true
            if (estado.vista == VistaDatos.LISTA) {
                (if (enX) focoListaX else focoListaY).requestFocus()
            } else (if (enX) focos[id]?.first else focos[id]?.second)?.requestFocus()
        } else if (DatosLagrange.numeroCelda(estado.x) == null) focoObjetivo.requestFocus()
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.entreTarjetas)) {
        TarjetaContorno(espacio = Dimens.e12) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                TituloTarjeta("Datos", Modifier.weight(1f))
                PestanasChip(listOf("Lista", "Tabla"), estado.vista.ordinal, { cambiar(estado.conVista(VistaDatos.entries[it])) })
            }
            if (estado.vista == VistaDatos.LISTA) {
                CampoTexto(estado.textoLista(Columna.X), { cambiar(estado.conLista(Columna.X, it)) },
                    "Valores X", Modifier.focusRequester(focoListaX), multilinea = true)
                CampoTexto(estado.textoLista(Columna.Y), { cambiar(estado.conLista(Columna.Y, it)) },
                    "Valores Y", Modifier.focusRequester(focoListaY), multilinea = true,
                    ayuda = "Separa con comas y usa punto decimal.")
                errores.values.distinct().firstOrNull()?.let { AvisoLagrange(it, true) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                    Spacer(Modifier.width(24.dp))
                    Text("X", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Text("Y", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(40.dp))
                }
                estado.puntos.forEach { p ->
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                            Text(nombrePunto(p.id), Modifier.width(24.dp), style = MaterialTheme.typography.labelLarge)
                            CampoCompacto(p.x, { cambiar(estado.conCelda(p.id, Columna.X, it)) }, null, Modifier.weight(1f),
                                error = p.id in errores, focusRequester = focos[p.id]?.first, descripcion = "X del punto ${nombrePunto(p.id)}")
                            CampoCompacto(p.y, { cambiar(estado.conCelda(p.id, Columna.Y, it)) }, null, Modifier.weight(1f),
                                error = p.id in errores, focusRequester = focos[p.id]?.second, descripcion = "Y del punto ${nombrePunto(p.id)}")
                            IconButton(onClick = { cambiar(estado.quitarPunto(p.id)) }, enabled = estado.puntos.size > 1,
                                modifier = Modifier.width(40.dp)) {
                                Icono(Iconos.quitar, "Eliminar punto ${nombrePunto(p.id)}")
                            }
                        }
                        errores[p.id]?.let { AvisoLagrange(it, true) }
                    }
                }
                BotonTexto("Agregar punto", {
                    val nuevo = estado.agregarPunto()
                    if (nuevo.puntos.size > estado.puntos.size) enfocarPunto = nuevo.puntos.last().id
                    cambiar(nuevo)
                }, icono = Iconos.agregar)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val cantidad = estado.puntos.count { DatosLagrange.numeroCelda(it.x) != null && DatosLagrange.numeroCelda(it.y) != null }
                Text(if (cantidad == 1) "1 punto" else "$cantidad puntos", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = {
                    textoPegado = portapapeles.getText()?.text.orEmpty()
                    errorPegado = null
                    pegar = true
                }, modifier = Modifier.size(40.dp)) { Icono(Iconos.pegar, "Pegar dos columnas") }
            }
        }
        TarjetaContorno(espacio = Dimens.e12) {
            TituloTarjeta("Evaluación")
            CampoCompacto(estado.x, { cambiar(estado.copy(x = it)) }, "x a evaluar",
                focusRequester = focoObjetivo,
                error = intentoFallido > 0 && DatosLagrange.numeroCelda(estado.x) == null)
            Text("Grados a calcular", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val maximo = estado.maximoGrado()
            GradosLagrange(
                grados = (1..maxOf(1, maximo, estado.grados.maxOrNull() ?: 1)).toList(),
                seleccionados = estado.grados, alElegir = { cambiar(estado.alternarGrado(it)) },
                multiple = true, disponibles = (1..maximo).toSet(),
            )
            val puedeElegir = estado.grados.isNotEmpty() && estado.grados.all { it in 1..maximo } &&
                errores.isEmpty() && DatosLagrange.numeroCelda(estado.x) != null
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                if (estado.modo == ModoNodos.MANUAL) {
                    Text("Puntos personalizados", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    gradoActivo = estado.grados.first()
                    borrador = estado.conModo(ModoNodos.MANUAL)
                }, enabled = puedeElegir) { Text(if (estado.modo == ModoNodos.MANUAL) "Editar puntos" else "Elegir puntos") }
            }
            if (intentoFallido > 0 || DatosLagrange.filasConDatos(estado.puntos).isNotEmpty()) {
                estado.validacion()?.let { AvisoLagrange(it, true) }
            }
        }
        Column(Modifier.padding(horizontal = Dimens.e4), verticalArrangement = Arrangement.spacedBy(Dimens.e8)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(estado.funcionHabilitada,
                role = Role.Checkbox, onValueChange = { cambiar(estado.copy(funcionHabilitada = it)) }),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                Text(if (estado.funcionHabilitada) "Función original" else "Agregar función original",
                    Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Opcional", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icono(Iconos.chevronArriba, null, Modifier.rotate(if (estado.funcionHabilitada) 0f else 180f))
            }
            if (estado.funcionHabilitada) {
                CampoTexto(estado.funcion, { cambiar(estado.copy(funcion = it)) }, "f(x)",
                    ayuda = "Permite calcular el error absoluto. Ejemplo: x^2 + 1.")
            }
        }
    }

    val editado = borrador
    if (editado != null) {
        ModalBottomSheet(onDismissRequest = { borrador = null },
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))) {
            val activo = gradoActivo.takeIf { it in editado.grados } ?: editado.grados.first()
            val seleccionados = editado.seleccion(activo)
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp)
                .padding(start = Dimens.e16, end = Dimens.e16, bottom = Dimens.e16),
                verticalArrangement = Arrangement.spacedBy(Dimens.e12)) {
                TituloTarjeta("Elegir puntos")
                if (editado.grados.size > 1) {
                    GradosLagrange(editado.grados, listOf(activo), { gradoActivo = it }, multiple = false,
                        descripcion = "Puntos de grado")
                }
                Text("Grado $activo · ${seleccionados.size} de ${activo + 1} puntos seleccionados",
                    style = MaterialTheme.typography.bodyMedium)
                if (seleccionados.size < activo + 1) {
                    Text("Desmarca y marca puntos hasta completar la selección.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    editado.puntos.filter { it.id in seleccionados || it.x.isNotBlank() || it.y.isNotBlank() }.forEach { p ->
                        val marcado = p.id in seleccionados
                        val habilitado = marcado || (seleccionados.size < activo + 1 && p.id !in errores)
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(marcado, enabled = habilitado,
                            role = Role.Checkbox, onValueChange = { borrador = editado.alternarPunto(activo, p.id) }),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(marcado, onCheckedChange = null, enabled = habilitado)
                            Text("${nombrePunto(p.id)} · (${p.x}, ${p.y})", Modifier.weight(1f).padding(vertical = Dimens.e8),
                                color = if (habilitado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                    TextButton(onClick = { borrador = editado.sugerir(activo) }) { Text("Sugerir cercanos") }
                    TextButton(onClick = {
                        cambiar(estado.conModo(ModoNodos.AUTOMATICO))
                        borrador = null
                    }, modifier = Modifier.weight(1f)) { Text("Usar puntos más cercanos") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                    BotonTexto("Cancelar", { borrador = null })
                    BotonPrimario("Usar selección", { cambiar(editado); borrador = null }, Modifier.weight(1f),
                        habilitado = editado.validacion() == null)
                }
            }
        }
    }
    if (pegar) AlertDialog(
        onDismissRequest = { pegar = false },
        title = { Text("Pegar dos columnas") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.e12)) {
                Text("Pega X e Y separados por tabulación, espacios o punto y coma; un punto por línea.",
                    style = MaterialTheme.typography.bodySmall)
                CampoTexto(textoPegado, { textoPegado = it }, "X e Y", multilinea = true)
                Text("Reemplazar los datos vacía las selecciones personalizadas para revisarlas.",
                    style = MaterialTheme.typography.bodySmall)
                errorPegado?.let { AvisoLagrange(it, true) }
            }
        },
        confirmButton = { TextButton(onClick = {
            runCatching {
                val (xs, ys) = leerPegadoLagrange(textoPegado)
                cambiar(estado.reemplazar(xs, ys))
                pegar = false
            }.onFailure { errorPegado = it.message ?: "Revisa los datos." }
        }) { Text("Reemplazar datos") } },
        dismissButton = { TextButton(onClick = { pegar = false }) { Text("Cancelar") } },
    )
}

/** Los huecos entre comas se conservan: nunca se desplazan las parejas X–Y. */
fun leerListaLagrange(texto: String): List<String> = if (',' in texto || ';' in texto)
    texto.split(',', ';').map { it.trim() } else texto.trim().split(Regex("[\\s]+"))

@Composable
private fun AvisoLagrange(texto: String, error: Boolean = false) {
    Text(texto, style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
}
