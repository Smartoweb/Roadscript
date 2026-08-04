package com.roadscript.oss.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CountryBadge(
    countryCode: String,
    modifier: Modifier = Modifier,
    // Fond sombre basé sur le thème (mélange 50/50 avec le noir pour rester coloré)
    containerColor: Color = lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.5f),
    contentColor: Color = Color.White
) {
    if (countryCode.isBlank()) return

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = countryCode.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
