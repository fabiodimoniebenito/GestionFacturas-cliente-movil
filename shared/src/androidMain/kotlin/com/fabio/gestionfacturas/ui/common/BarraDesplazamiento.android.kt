package com.fabio.gestionfacturas.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun BarraDesplazamiento(estado: ScrollState, modifier: Modifier) {
    // Intentionally empty: Android shows its own scroll indicator.
}

@Composable
actual fun BarraDesplazamiento(estado: LazyListState, modifier: Modifier) {
    // Intentionally empty: Android shows its own scroll indicator.
}
