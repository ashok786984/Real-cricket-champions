package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BroadcastColorScheme = darkColorScheme(
    primary = FloodlightMint,
    onPrimary = StadiumObsidian,
    primaryContainer = PitchEmerald,
    onPrimaryContainer = OffWhiteText,
    secondary = ChampionGold,
    onSecondary = StadiumObsidian,
    secondaryContainer = ChampionGoldContainer,
    onSecondaryContainer = ChampionGold,
    tertiary = CricketCrimson,
    onTertiary = OffWhiteText,
    tertiaryContainer = CricketCrimsonContainer,
    onTertiaryContainer = OffWhiteText,
    background = StadiumObsidian,
    onBackground = OffWhiteText,
    surface = TurfSurface,
    onSurface = OffWhiteText,
    surfaceVariant = TurfCardElevated,
    onSurfaceVariant = MutedSageText,
    surfaceContainerHigh = TurfCardHigh,
    outline = DividerEmerald,
    error = CricketCrimson,
    onError = OffWhiteText
)

val BroadcastShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RealCricketChampionsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BroadcastColorScheme,
        typography = AppTypography,
        shapes = BroadcastShapes,
        content = content
    )
}
