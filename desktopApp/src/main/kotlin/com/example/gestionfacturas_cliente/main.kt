package com.example.gestionfacturas_cliente

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GestionFacturascliente",
    ) {
        App()
    }
}