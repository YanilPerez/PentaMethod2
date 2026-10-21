package com.example.pentametod2.ui.navegacion

import androidx.compose.runtime.saveable.Saver
import com.example.pentametod2.model.MetodoId

/** Navegación simple: el menú o la pantalla de un método. */
sealed interface Pantalla {
    data object Menu : Pantalla
    data class Metodo(val id: MetodoId) : Pantalla
}

private const val CLAVE_MENU = "menu"

/** Pantalla → texto, para guardarla con rememberSaveable. */
fun Pantalla.aClave(): String = when (this) {
    Pantalla.Menu -> CLAVE_MENU
    is Pantalla.Metodo -> id.name
}

/** Texto → Pantalla; cualquier clave desconocida vuelve al menú. */
fun pantallaDesdeClave(clave: String): Pantalla {
    val id = MetodoId.entries.firstOrNull { it.name == clave } ?: return Pantalla.Menu
    return Pantalla.Metodo(id)
}

val PantallaSaver: Saver<Pantalla, String> = Saver(
    save = { it.aClave() },
    restore = { pantallaDesdeClave(it) },
)

/** Qué hace "atrás" desde cada pantalla; null = no hay a dónde volver (dejar que el sistema cierre la app). */
fun Pantalla.anterior(): Pantalla? = when (this) {
    Pantalla.Menu -> null
    is Pantalla.Metodo -> Pantalla.Menu
}
