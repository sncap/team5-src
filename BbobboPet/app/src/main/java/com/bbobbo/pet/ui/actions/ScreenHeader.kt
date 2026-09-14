package com.bbobbo.pet.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.ui.theme.Palette

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, light: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (light) Color.White.copy(alpha = 0.2f) else Palette.CardWhite)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", style = MaterialTheme.typography.titleLarge, color = if (light) Color.White else Palette.TextBrown)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = if (light) Color.White else Palette.TextBrown
            )
        }
        Box(Modifier.size(40.dp))
    }
}
