package com.bbobbo.pet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.ui.theme.Palette

/** 기획서 S-01 (9) 하단 탭바: 홈 / 뽀뽀 / 보관함 / 상점 / 더보기 */
data class BottomTab(val route: String, val label: String, val emoji: String)

@Composable
fun BottomTabBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Palette.CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                TabItem(
                    tab = tab,
                    active = currentRoute == tab.route,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: BottomTab,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(if (active) 1.12f else 1f, tween(180), label = "tabScale")
    val labelColor by animateColorAsState(
        if (active) Palette.PrimaryDeep else Palette.SubText, tween(180), label = "tabColor"
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .scale(scale)
                    .background(
                        if (active) Palette.SubPink else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(tab.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                tab.label,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
        }
    }
}
