package com.example.pentametod2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    private val guardadoPendiente: GuardadoPendiente by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Se registra en onCreate (antes de STARTED), como exige registerForActivityResult.
        val exportador = ExportadorAndroid(this, guardadoPendiente)

        setContent {
            App(exportador)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}