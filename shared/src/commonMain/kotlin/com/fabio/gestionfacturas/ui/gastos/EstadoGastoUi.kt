package com.fabio.gestionfacturas.ui.gastos

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fabio.gestionfacturas.domain.model.EstadoGasto
import com.fabio.gestionfacturas.domain.model.Importe
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.gasto_estado_borrador
import com.fabio.gestionfacturas.resources.gasto_estado_exportada
import com.fabio.gestionfacturas.resources.gasto_estado_procesada
import com.fabio.gestionfacturas.resources.gasto_estado_revisada
import com.fabio.gestionfacturas.resources.gastos_sin_dato
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Shared by the list and the detail: the texts and colours of each estado. */
private fun EstadoGasto.textoRes(): StringResource = when (this) {
    EstadoGasto.BORRADOR -> Res.string.gasto_estado_borrador
    EstadoGasto.PROCESADA -> Res.string.gasto_estado_procesada
    EstadoGasto.REVISADA -> Res.string.gasto_estado_revisada
    EstadoGasto.EXPORTADA -> Res.string.gasto_estado_exportada
}

/** Container and content colours of the estado chip. BORRADOR uses the tertiary pair so it stands out. */
@Composable
internal fun EstadoGasto.colores(): Pair<Color, Color> {
    val esquema = MaterialTheme.colorScheme
    return when (this) {
        EstadoGasto.BORRADOR -> esquema.tertiaryContainer to esquema.onTertiaryContainer
        EstadoGasto.PROCESADA -> esquema.secondaryContainer to esquema.onSecondaryContainer
        EstadoGasto.REVISADA -> esquema.primaryContainer to esquema.onPrimaryContainer
        EstadoGasto.EXPORTADA -> esquema.surfaceVariant to esquema.onSurfaceVariant
    }
}

/** A chip with the estado name; a missing estado renders the neutral placeholder. */
@Composable
internal fun EtiquetaEstado(estado: EstadoGasto?, modifier: Modifier = Modifier) {
    if (estado == null) {
        Text(text = stringResource(Res.string.gastos_sin_dato), modifier = modifier)
        return
    }
    val (fondo, contenido) = estado.colores()
    Surface(color = fondo, contentColor = contenido, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(
            text = stringResource(estado.textoRes()),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** The importe with a comma decimal, followed by the moneda when known; null renders the placeholder. */
@Composable
internal fun textoImporte(importe: Importe?, moneda: String?): String {
    if (importe == null) {
        return stringResource(Res.string.gastos_sin_dato)
    }
    return if (moneda.isNullOrBlank()) importe.mostrar() else importe.mostrar() + " " + moneda
}
