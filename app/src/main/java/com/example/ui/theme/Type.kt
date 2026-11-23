package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val ClipForgeTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
)

data class SubtitleFontOption(
    val id: String,
    val displayName: String,
    val category: String,
    @FontRes val fontRes: Int
) {
    val composeFontFamily: FontFamily
        get() = FontFamily(Font(fontRes, FontWeight.Normal))

    fun loadTypeface(context: Context, isBold: Boolean): Typeface {
        val base = ResourcesCompat.getFont(context, fontRes) ?: Typeface.SANS_SERIF
        return if (isBold) Typeface.create(base, Typeface.BOLD) else base
    }
}

object SubtitleFonts {
    val all: List<SubtitleFontOption> = listOf(
        SubtitleFontOption("inter", "Inter", "Modern Sans", R.font.inter),
        SubtitleFontOption("plus_jakarta", "Plus Jakarta Sans", "Modern Sans", R.font.plus_jakarta_sans),
        SubtitleFontOption("space_grotesk", "Space Grotesk", "Geometric", R.font.space_grotesk),
        SubtitleFontOption("montserrat", "Montserrat", "Creator Bold", R.font.montserrat),
        SubtitleFontOption("poppins", "Poppins", "Clean Subtitle", R.font.poppins),
        SubtitleFontOption("outfit", "Outfit", "Modern Sans", R.font.outfit),
        SubtitleFontOption("bebas_neue", "Bebas Neue", "Bold Display", R.font.bebas_neue),
        SubtitleFontOption("oswald", "Oswald", "Condensed Impact", R.font.oswald),
        SubtitleFontOption("playfair", "Playfair Display", "Editorial Serif", R.font.playfair_display),
        SubtitleFontOption("roboto_mono", "Roboto Mono", "Tech Mono", R.font.roboto_mono),
        SubtitleFontOption("jetbrains_mono", "JetBrains Mono", "Code Mono", R.font.jetbrains_mono)
    )

    fun byId(id: String): SubtitleFontOption = all.firstOrNull { it.id == id } ?: all.first()
}
