package com.example.pentametod2.ui.resultados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.pentametod2.model.*
import com.example.pentametod2.presentacion.PresentadorComparacionLagrange
import com.example.pentametod2.ui.componentes.GradosLagrange
import com.example.pentametod2.ui.componentes.TarjetaContorno
import com.example.pentametod2.ui.componentes.TituloTarjeta
import com.example.pentametod2.ui.tema.Dimens

/** Se agregan los avisos comunes al detalle sin alterar sus pasos ni su conclusión. */
internal fun detalleLagrange(salida: SalidaMetodo, grado: Int): SalidaMetodo {
    val c = requireNotNull(salida.comparacionLagrange)
    val detalle = c.resultados.first { it.grado == grado }.detalle
    // La extrapolación del grado elegido ya está explicada en su detalle.
    val comunes = salida.advertencias.filterNot { it.startsWith("Grado ") && it.contains("se está extrapolando") }
    val avisos = (comunes + detalle.advertencias).distinct()
    return detalle.copy(advertencias = avisos,
        estado = if (detalle.estado == Estado.ERROR) Estado.ERROR else if (avisos.isEmpty()) Estado.OK else Estado.ADVERTENCIA)
}

@Composable
fun ResultadosLagrange(salida: SalidaMetodo, alEditar: () -> Unit, alAbrirMetodo: (MetodoId) -> Unit, claveCalculo: Int) {
    val c = salida.comparacionLagrange ?: return
    var elegido by rememberSaveable(claveCalculo) { mutableIntStateOf(c.resultados.first().grado) }
    val actual = c.resultados.firstOrNull { it.grado == elegido } ?: c.resultados.first()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.entreTarjetas)) {
        ResultadosMetodo(salida.copy(comparacionLagrange = null, grafica = null, advertencias = emptyList()),
            alEditar, alAbrirMetodo, claveCalculo, mostrarIndicador = false)
        TarjetaContorno(espacio = Dimens.e12) {
            TituloTarjeta("Procedimiento por grado")
            GradosLagrange(c.resultados.map { it.grado }, listOf(actual.grado), { elegido = it }, multiple = false)
        }
        // Se conserva la misma gráfica existente. La vista normal la ubica entre pasos y tablas.
        val grafica = remember(c, actual.grado) { PresentadorComparacionLagrange.grafica(c, actual.grado) }
        key(claveCalculo, actual.grado) {
            val detalle = detalleLagrange(salida, actual.grado).copy(grafica = grafica)
            ResultadosMetodo(detalle, alEditar, alAbrirMetodo, claveCalculo)
        }
    }
}
