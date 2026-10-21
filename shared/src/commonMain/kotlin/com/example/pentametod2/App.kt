package com.example.pentametod2

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.pentametod2.exportar.ExportadorArchivos
import com.example.pentametod2.exportar.ExportadorNoDisponible
import com.example.pentametod2.ui.AppMetodos

@Composable
@Preview
fun App(exportador: ExportadorArchivos = ExportadorNoDisponible) {
    AppMetodos(exportador = exportador)
}
