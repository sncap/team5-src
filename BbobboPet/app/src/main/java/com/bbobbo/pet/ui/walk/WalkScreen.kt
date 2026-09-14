package com.bbobbo.pet.ui.walk

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.Walks
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette
import kotlinx.coroutines.delay

/**
 * 기획서 S-09 산책.
 * 진행 상태는 DB 의 walkEndAt 하나로만 표현하므로 화면을 벗어나거나 앱을 종료해도 이어진다.
 */
@Composable
fun WalkScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    val result by vm.walkResult.collectAsState()

    var remaining by remember { mutableLongStateOf(vm.walkRemainingMs()) }
    val walking = remaining > 0L

    // 1초 틱으로 남은 시간을 갱신하고, 끝나면 정산을 요청한다.
    LaunchedEffect(pet.walkEndAt) {
        while (true) {
            remaining = vm.walkRemainingMs()
            if (pet.walkEndAt != null && remaining <= 0L) {
                vm.checkWalkFinished()
                break
            }
            delay(500)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("산책", onBack)
        Spacer(Modifier.height(10.dp))

        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("오늘 남은 산책", style = MaterialTheme.typography.bodyLarge, color = Palette.TextBrown)
                    Text(
                        "${vm.walksLeftToday()} / ${Walks.DAILY_LIMIT}회",
                        style = MaterialTheme.typography.labelLarge, color = Palette.PrimaryDeep
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("소모 에너지", style = MaterialTheme.typography.bodyMedium, color = Palette.SubText)
                    Text(
                        "⚡ ${Walks.ENERGY_COST.toInt()}  (보유 ${pet.energy.toInt()})",
                        style = MaterialTheme.typography.labelSmall, color = Palette.SubText
                    )
                }
                Text(
                    "산책은 3분 동안 이어져요. 앱을 닫아도 계속 진행돼요!",
                    style = MaterialTheme.typography.labelSmall, color = Palette.SubText
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ---- 산책길 무대 ----
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            if (walking) StrollingBackground()
            PetCharacter(
                modifier = Modifier.fillMaxSize(),
                anim = if (walking) PetAnim.WALK else PetAnim.IDLE,
                hatId = vm.equippedId(WearSlot.HAT),
                clothId = vm.equippedId(WearSlot.CLOTH),
                accId = vm.equippedId(WearSlot.ACC),
            )
        }

        if (walking) {
            val total = Walks.DURATION_MS.toFloat()
            val progress = ((total - remaining) / total).coerceIn(0f, 1f)
            Text(
                formatRemaining(remaining),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = Palette.TextBrown,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE2D4))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(Palette.Mint)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "산책 중에는 다른 활동을 할 수 없어요",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = Palette.SubText,
                textAlign = TextAlign.Center,
            )
        } else {
            val canWalk = vm.walksLeftToday() > 0 && pet.energy >= Walks.ENERGY_COST
            BouncyButton(
                onClick = { vm.startWalk() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canWalk,
                color = Palette.Primary,
            ) {
                Text(
                    when {
                        vm.walksLeftToday() <= 0 -> "오늘 산책을 다 했어요"
                        pet.energy < Walks.ENERGY_COST -> "에너지가 부족해요"
                        else -> "산책 나가기"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canWalk) Color.White else Palette.SubText,
                )
            }
        }
    }

    // ---- 결과 팝업 ----
    result?.let { r ->
        AlertDialog(
            onDismissRequest = { vm.consumeWalkResult() },
            confirmButton = {
                TextButton(onClick = { vm.consumeWalkResult() }) { Text("좋아요!") }
            },
            title = { Text("${r.event.emoji}  ${r.event.title}") },
            text = { Text(r.total.summary()) },
            containerColor = Palette.CardWhite,
        )
    }
}

/** 산책 중임을 보여주는 가벼운 배경 연출 (좌우로 흐르는 풀밭) */
@Composable
private fun StrollingBackground() {
    val t = rememberInfiniteTransition(label = "stroll")
    val shift by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "shift"
    )
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 26.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("🌿", "🌳", "🌼", "🌿", "🍀").forEachIndexed { i, s ->
                Text(
                    s,
                    modifier = Modifier.padding(top = (if (i % 2 == 0) shift * 6 else (1 - shift) * 6).dp),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}
