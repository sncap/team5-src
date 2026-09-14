package com.bbobbo.pet.ui.actions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.theme.Palette
import kotlin.math.abs

/**
 * 씻기 화면 (기획서 S-05).
 * 캐릭터 위를 문지르면 진행도가 오르고, 100%에 도달하면 보상이 지급된다.
 */
@Composable
fun BathScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }
    var finished by remember { mutableStateOf(false) }
    val shown by animateFloatAsState(progress, tween(120), label = "scrub")

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("씻기", onBack)
        Spacer(Modifier.height(6.dp))
        Text(
            if (finished) "뽀득뽀득! 아주 깨끗해졌어요" else "화면을 문질러서 거품을 내주세요",
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.SubText
        )
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(finished) {
                    if (finished) return@pointerInput
                    detectDragGesturesCompat { dx, dy ->
                        progress = (progress + (abs(dx) + abs(dy)) / 2600f).coerceAtMost(1f)
                        if (progress >= 1f && !finished) {
                            finished = true
                            vm.bath(1f)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            PetCharacter(
                modifier = Modifier.fillMaxSize(),
                anim = if (finished) PetAnim.JUMP else PetAnim.BATH,
                hatId = "hat_none",
                clothId = "cloth_none",
                accId = vm.equippedId(WearSlot.ACC),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "${(shown * 100).toInt()}%",
            style = MaterialTheme.typography.headlineMedium,
            color = Palette.SkyBlue
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE2D4))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(shown)
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(Palette.SkyBlue)
            )
        }

        Spacer(Modifier.weight(1f))
        BouncyButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            color = if (finished) Palette.Primary else Palette.CardBeige
        ) {
            Text(
                if (finished) "돌아가기" else "나중에 할래요",
                style = MaterialTheme.typography.titleMedium,
                color = if (finished) Color.White else Palette.SubText
            )
        }
    }
}

private suspend fun PointerInputScope.detectDragGesturesCompat(
    onDrag: (Float, Float) -> Unit,
) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        onDrag(dragAmount.x, dragAmount.y)
    }
}
