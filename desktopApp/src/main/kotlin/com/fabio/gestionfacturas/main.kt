package com.fabio.gestionfacturas

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.fabio.gestionfacturas.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GestionFacturascliente",
        ) {
            App()
        }
    }
}
