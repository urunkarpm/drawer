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

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
