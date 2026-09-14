package com.bbobbo.pet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.bbobbo.pet.ui.NavGraph
import com.bbobbo.pet.ui.theme.BbobboTheme
import com.bbobbo.pet.ui.theme.Palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BbobboTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Palette.CreamBg, Palette.CardBeige.copy(alpha = 0.6f))
                            )
                        )
                        .systemBarsPadding()
                ) {
                    NavGraph()
                }
            }
        }
    }
}
