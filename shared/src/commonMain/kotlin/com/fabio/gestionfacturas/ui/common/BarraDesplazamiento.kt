package com.fabio.gestionfacturas.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A visible vertical scrollbar. Desktop draws one (Compose's `VerticalScrollbar`, which only exists on
 * the JVM target); Android draws nothing because touch scrolling already shows its own indicator.
 * Place it in a `Box` over the scrolling content, aligned to the end and with `fillMaxHeight()`.
 */
@Composable
expect fun BarraDesplazamiento(estado: ScrollState, modifier: Modifier = Modifier)

@Composable
expect fun BarraDesplazamiento(estado: LazyListState, modifier: Modifier = Modifier)

/** A full-size column that scrolls vertically and shows the [BarraDesplazamiento] on the right. */
@Composable
fun ColumnaConBarra(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val desplazamiento = rememberScrollState()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(desplazamiento),
            verticalArrangement = verticalArrangement,
            content = content,
        )
        BarraDesplazamiento(desplazamiento, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

/** A full-size lazy list with the [BarraDesplazamiento] on the right. */
@Composable
fun ListaConBarra(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val estado = rememberLazyListState()
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = estado,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = verticalArrangement,
            content = content,
        )
        BarraDesplazamiento(estado, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}
