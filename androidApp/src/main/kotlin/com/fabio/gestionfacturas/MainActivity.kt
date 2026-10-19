package com.fabio.gestionfacturas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fabio.gestionfacturas.di.initKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Safe to call on every (re)creation: it only starts Koin the first time.
        initKoin()

        setContent {
            App()
        }
    }
}
