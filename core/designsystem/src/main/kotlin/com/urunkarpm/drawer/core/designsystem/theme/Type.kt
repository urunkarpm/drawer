package com.urunkarpm.drawer.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.Font
import com.urunkarpm.drawer.core.designsystem.R

val TexasTroupeFontFamily = FontFamily(
    Font(R.font.texas_troupe_fp, FontWeight.Normal)
)

val HelveticaFontFamily = FontFamily(
    Font(R.font.helvetica_fp, FontWeight.Normal)
)

val RobotoFontFamily = FontFamily(
    Font(R.font.roboto_fp, FontWeight.Normal)
)

val TimesNewRomanFontFamily = FontFamily(
    Font(R.font.times_new_roman_fp, FontWeight.Normal)
)

val ArialFontFamily = FontFamily(
    Font(R.font.arial_fp, FontWeight.Normal)
)

// ponytail: Google Sans font family with Regular, Medium, and Bold weights applied globally across MaterialTheme Typography; ceiling: local TTF bundle; upgrade path: downloadable fonts via Play Services FontsContract.
val GoogleSansFontFamily = FontFamily(
    Font(R.font.google_sans_fp, FontWeight.Normal),
    Font(R.font.google_sans_medium_fp, FontWeight.Medium),
    Font(R.font.google_sans_bold_fp, FontWeight.Bold)
)

val SpookyyFontFamily = FontFamily(
    Font(R.font.spookyy_fp, FontWeight.Normal)
)

val DsDigitalFontFamily = FontFamily(
    Font(R.font.ds_digital_fp, FontWeight.Normal)
)

val EvilEmpireFontFamily = FontFamily(
    Font(R.font.evil_empire_fp, FontWeight.Normal)
)

val HypikFontFamily = FontFamily(
    Font(R.font.hypik_fp, FontWeight.Normal)
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = GoogleSansFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = GoogleSansFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = GoogleSansFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = GoogleSansFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = GoogleSansFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = GoogleSansFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = GoogleSansFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = GoogleSansFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = GoogleSansFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSansFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSansFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSansFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = GoogleSansFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = GoogleSansFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSansFontFamily)
)
