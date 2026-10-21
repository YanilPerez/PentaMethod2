package com.example.pentametod2

import androidx.compose.ui.window.ComposeUIViewController
import com.example.pentametod2.exportar.ExportadorNoDisponible

/** iOS: guardar archivos aún no está implementado (no se puede compilar iOS en Windows); el menú avisa "no disponible". */
fun MainViewController() = ComposeUIViewController { App(ExportadorNoDisponible) }
