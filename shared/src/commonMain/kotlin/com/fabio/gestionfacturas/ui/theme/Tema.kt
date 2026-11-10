package com.fabio.gestionfacturas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabio.gestionfacturas.resources.Res
import com.fabio.gestionfacturas.resources.onest
import org.jetbrains.compose.resources.Font

/*
 * Identity "negro y champán" (concept-designs/12-negro-champan): near-black surfaces, off-white text, ONE muted
 * champagne accent for what is active and for the main action. No colors are hardcoded in the screens: they all
 * read these roles. The dark scheme is the concept; the light scheme is a derived counterpart (same hue, text and
 * accent darkened for contrast on a light surface).
 */

private val Negro = Color(0xFF050505)
private val Superficie = Color(0xFF101010)
private val Elevada = Color(0xFF1A1A1A)
private val Linea = Color(0xFF2A2A2A)
private val BlancoRoto = Color(0xFFF4F1EA)
private val GrisSuave = Color(0xFFA39F96)
private val GrisTenue = Color(0xFF8A877F)
private val Champan = Color(0xFFD8BC84)
private val SobreChampan = Color(0xFF1A1204)
private val ChampanOscuro = Color(0xFF2A2314)
private val ChampanSuave = Color(0xFFE9DDBD)
private val RojoSuave = Color(0xFFFF9A8F)
private val SobreRojo = Color(0xFF2A0A06)
private val RojoOscuro = Color(0xFF3A1511)

internal val EsquemaOscuro: ColorScheme = darkColorScheme(
    primary = Champan,
    onPrimary = SobreChampan,
    primaryContainer = Elevada,
    onPrimaryContainer = BlancoRoto,
    secondary = Champan,
    onSecondary = SobreChampan,
    secondaryContainer = Elevada,
    onSecondaryContainer = BlancoRoto,
    tertiary = Champan,
    onTertiary = SobreChampan,
    // Borradores / pendiente de revisión: a warm, dim amber that stays readable on black.
    tertiaryContainer = ChampanOscuro,
    onTertiaryContainer = ChampanSuave,
    background = Negro,
    onBackground = BlancoRoto,
    surface = Negro,
    onSurface = BlancoRoto,
    surfaceVariant = Elevada,
    onSurfaceVariant = GrisSuave,
    surfaceTint = Champan,
    inverseSurface = BlancoRoto,
    inverseOnSurface = Negro,
    inversePrimary = Color(0xFF6B5320),
    error = RojoSuave,
    onError = SobreRojo,
    errorContainer = RojoOscuro,
    onErrorContainer = Color(0xFFFFD9D4),
    outline = GrisTenue,
    outlineVariant = Linea,
    scrim = Color(0xFF000000),
    surfaceBright = Elevada,
    surfaceDim = Negro,
    surfaceContainerLowest = Negro,
    surfaceContainerLow = Superficie,
    surfaceContainer = Superficie,
    surfaceContainerHigh = Elevada,
    surfaceContainerHighest = Elevada,
)

private val Papel = Color(0xFFF7F6F3)
private val Hoja = Color(0xFFFFFFFF)
private val Tinta = Color(0xFF17150F)
private val ChampanProfundo = Color(0xFF6B5320)

internal val EsquemaClaro: ColorScheme = lightColorScheme(
    primary = ChampanProfundo,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFE9DA),
    onPrimaryContainer = Tinta,
    secondary = ChampanProfundo,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE9DA),
    onSecondaryContainer = Tinta,
    tertiary = ChampanProfundo,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8C6),
    onTertiaryContainer = Color(0xFF4A3905),
    background = Papel,
    onBackground = Tinta,
    surface = Papel,
    onSurface = Tinta,
    surfaceVariant = Color(0xFFEDEBE6),
    onSurfaceVariant = Color(0xFF5E5A50),
    surfaceTint = ChampanProfundo,
    inverseSurface = Tinta,
    inverseOnSurface = Papel,
    inversePrimary = Champan,
    error = Color(0xFFB3362B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE0DC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF7C776B),
    outlineVariant = Color(0xFFDAD6CC),
    scrim = Color(0xFF000000),
    surfaceBright = Hoja,
    surfaceDim = Color(0xFFE6E3DC),
    surfaceContainerLowest = Hoja,
    surfaceContainerLow = Color(0xFFF1EFEA),
    surfaceContainer = Color(0xFFEDEBE6),
    surfaceContainerHigh = Color(0xFFE8E5DF),
    surfaceContainerHighest = Color(0xFFE3E0D9),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Onest is a variable font: one file, the weight axis picks 300/400/500/600. */
@Composable
private fun familiaOnest(): FontFamily {
    val ligera = Font(Res.font.onest, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300)))
    val normal = Font(Res.font.onest, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400)))
    val media = Font(Res.font.onest, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500)))
    val seminegrita = Font(Res.font.onest, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600)))
    return remember(ligera, normal, media, seminegrita) { FontFamily(ligera, normal, media, seminegrita) }
}

private fun tipografia(familia: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.onest(peso: FontWeight, tamano: Int? = null, interlineado: Int? = null) = copy(
        fontFamily = familia,
        fontWeight = peso,
        fontSize = tamano?.sp ?: fontSize,
        lineHeight = interlineado?.sp ?: lineHeight,
    )
    return Typography(
        displayLarge = base.displayLarge.onest(FontWeight.Light),
        displayMedium = base.displayMedium.onest(FontWeight.Light),
        displaySmall = base.displaySmall.onest(FontWeight.Light),
        // Page titles and the key figure: large and light, the character of the identity.
        headlineLarge = base.headlineLarge.onest(FontWeight.Light),
        headlineMedium = base.headlineMedium.onest(FontWeight.Normal, tamano = 30, interlineado = 38),
        headlineSmall = base.headlineSmall.onest(FontWeight.Light, tamano = 26, interlineado = 32),
        titleLarge = base.titleLarge.onest(FontWeight.Medium, tamano = 18, interlineado = 24),
        titleMedium = base.titleMedium.onest(FontWeight.Medium),
        titleSmall = base.titleSmall.onest(FontWeight.Medium),
        bodyLarge = base.bodyLarge.onest(FontWeight.Normal),
        bodyMedium = base.bodyMedium.onest(FontWeight.Normal),
        bodySmall = base.bodySmall.onest(FontWeight.Normal),
        labelLarge = base.labelLarge.onest(FontWeight.Medium),
        labelMedium = base.labelMedium.onest(FontWeight.Medium),
        labelSmall = base.labelSmall.onest(FontWeight.Medium),
    )
}

@Composable
fun TemaGestionFacturas(oscuro: Boolean, contenido: @Composable () -> Unit) {
    val familia = familiaOnest()
    val tipos = remember(familia) { tipografia(familia) }
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = tipos,
        shapes = Formas,
        content = contenido,
    )
}
