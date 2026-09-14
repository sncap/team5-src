package com.bbobbo.pet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.ui.theme.Palette

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = Palette.CardWhite,
    radius: Int = 24,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(radius.dp),
        shadowElevation = 2.dp,
        content = content
    )
}

/** 누르면 살짝 눌렸다 튀어오르는 기본 버튼 */
@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Palette.Primary,
    radius: Int = 20,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(90), label = "press")
    val bg by animateColorAsState(if (enabled) color else Color(0xFFE3DAD0), tween(150), label = "bg")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(radius.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun StatBar(
    label: String,
    icon: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val low = value <= 30f
    val t = rememberInfiniteTransition(label = "stat")
    val blink by t.animateFloat(
        initialValue = 1f, targetValue = if (low) 0.35f else 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "blink"
    )
    val animated by animateFloatAsState(value, tween(450), label = "val")

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.bodyMedium)
        Text(
            label,
            modifier = Modifier.padding(start = 6.dp).width(46.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextBrown
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(9.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE2D4))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated / 100f)
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(if (low) color.copy(alpha = blink) else color)
            )
        }
        Text(
            "${animated.toInt()}%",
            modifier = Modifier.padding(start = 6.dp).width(36.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.SubText,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun CurrencyChip(
    icon: String,
    amount: String,
    onPlus: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Palette.CardWhite)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(icon, style = MaterialTheme.typography.bodyLarge)
        Text(amount, style = MaterialTheme.typography.labelLarge, color = Palette.TextBrown)
        if (onPlus != null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Palette.SubPink)
                    .clickable(onClick = onPlus),
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = MaterialTheme.typography.labelLarge, color = Palette.PrimaryDeep)
            }
        }
    }
}

@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Palette.SubPink)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextBrown,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MenuTile(
    label: String,
    emoji: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    big: Boolean = false,
) {
    BouncyButton(
        onClick = onClick,
        modifier = modifier.height(if (big) 84.dp else 66.dp),
        color = tint,
        radius = if (big) 22 else 18
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, style = if (big) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
            Text(
                label,
                style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
                color = Palette.TextBrown,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = Palette.TextBrown
    )
}

@Composable
fun PillTabs(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Palette.CardWhite)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (active) Palette.Primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Color.White else Palette.SubText
                )
            }
        }
    }
}

@Composable
fun LockBadge(modifier: Modifier = Modifier, text: String) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Palette.TextBrown.copy(alpha = 0.75f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
fun SelectedRing(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(2.5.dp, Palette.Primary, RoundedCornerShape(18.dp))
    )
}
