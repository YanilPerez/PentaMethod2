package com.example.pentametod2.ui.navegacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.pentametod2.catalogo.MetodoInfo
import com.example.pentametod2.catalogo.Secciones
import com.example.pentametod2.model.MetodoId
import com.example.pentametod2.ui.componentes.BotonTexto
import com.example.pentametod2.ui.componentes.ContenidoCentrado
import com.example.pentametod2.ui.tema.Dimens
import com.example.pentametod2.ui.tema.Formas
import com.example.pentametod2.ui.tema.Iconos
import com.example.pentametod2.ui.tema.Icono
import com.example.pentametod2.ui.tema.extendidos
import com.example.pentametod2.ui.tema.mono

/** Versión visible en "Acerca de" (igual a versionName de androidApp/build.gradle.kts). */
const val VERSION_APP = "1.0"

/** Glifo del ícono-tile de cada método. */
fun glifoMetodo(id: MetodoId): String = when (id) {
    MetodoId.PUNTO_FIJO -> "G(x)"
    MetodoId.NEWTON_SISTEMAS -> "J⁻¹"
    MetodoId.REGRESION_POLINOMIAL -> "Pₘ"
    MetodoId.REGRESION_MULTIPLE -> "βₖ"
    MetodoId.LAGRANGE -> "Lₖ"
    MetodoId.NEWTON_INTERPOLACION -> "f[ ]"
}

/** Agrupa por sección en el orden del diseño; las secciones desconocidas van al final. Nunca usa la semana. */
fun agruparPorSeccion(metodos: List<MetodoInfo>): List<Pair<String, List<MetodoInfo>>> {
    val grupos = metodos.groupBy { it.seccion }
    val orden = Secciones.orden.filter { it in grupos } + grupos.keys.filter { it !in Secciones.orden }
    return orden.map { it to grupos.getValue(it) }
}

@Composable
fun MenuScreen(metodos: List<MetodoInfo>, alAbrir: (MetodoId) -> Unit) {
    var acercaDe by rememberSaveable { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
        ) {
            ContenidoCentrado(Modifier.padding(horizontal = Dimens.margenLateral)) {
                Encabezado { acercaDe = true }
                for ((seccion, lista) in agruparPorSeccion(metodos)) {
                    Spacer(Modifier.height(Dimens.e20))
                    Text(
                        seccion,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = Dimens.e4).padding(bottom = Dimens.e8).semantics { heading() },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                        for (m in lista) TarjetaMetodo(m, alAbrir)
                    }
                }
                Spacer(Modifier.height(Dimens.e24))
            }
        }
    }
    if (acercaDe) DialogoAcercaDe { acercaDe = false }
}

@Composable
private fun Encabezado(alInfo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = Dimens.e8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        IconButton(onClick = alInfo) {
            Icono(Iconos.info, "Acerca de la app", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Text(
        "PENTAMETOD",
        style = MaterialTheme.mono.marca.copy(
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.04.em,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        "Elige un método para empezar a calcular.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Dimens.e4),
    )
}


@Composable
private fun TarjetaMetodo(m: MetodoInfo, alAbrir: (MetodoId) -> Unit) {
    Surface(
        onClick = { alAbrir(m.id) },
        shape = Formas.tarjeta,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Dimens.borde, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.filaMenu),
    ) {
        Row(
            Modifier.padding(start = Dimens.e16, top = Dimens.e12, end = Dimens.e8, bottom = Dimens.e12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.e12),
        ) {
            Box(Modifier.size(Dimens.tileMenu), contentAlignment = Alignment.Center) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = Formas.tile, modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(glifoMetodo(m.id), style = MaterialTheme.mono.tile, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(m.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    m.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icono(Iconos.chevronDerecha, null, color = MaterialTheme.extendidos.chevronMenu)
        }
    }
}

@Composable
private fun DialogoAcercaDe(alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        confirmButton = { BotonTexto("Cerrar", alCerrar) },
        title = { Text("Acerca de", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.e8)) {
                Text("Métodos Numéricos (Pentametod2)", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Calcula paso a paso sistemas no lineales, ajuste de curvas e interpolación, con la fórmula, la tabla y la gráfica.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Versión $VERSION_APP", style = MaterialTheme.mono.pequeno, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = Formas.tarjeta,
        tonalElevation = 0.dp,
    )
}
