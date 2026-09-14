package com.bbobbo.pet.ui.actions

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette
import kotlinx.coroutines.delay

/** 재우기 화면 (기획서 S-04) */
@Composable
fun SleepScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }

    val sleepEnd = pet.sleepEndAt
    val sleeping = sleepEnd != null && now < sleepEnd

    Box(Modifier.fillMaxSize()) {
        if (sleeping) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3B3350), Color(0xFF5B4E6E))
                        )
                    )
            )
        }

        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader("재우기", onBack, light = sleeping)
            Spacer(Modifier.height(10.dp))

            Box(
                Modifier.fillMaxWidth().aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                PetCharacter(
                    modifier = Modifier.fillMaxSize(),
                    anim = if (sleeping) PetAnim.SLEEP else PetAnim.IDLE,
                    hatId = vm.equippedId(WearSlot.HAT),
                    clothId = vm.equippedId(WearSlot.CLOTH),
                    accId = vm.equippedId(WearSlot.ACC),
                )
            }

            if (sleeping) {
                val remain = ((sleepEnd!! - now) / 1000).coerceAtLeast(0)
                Text(
                    "%02d:%02d:%02d".format(remain / 3600, (remain % 3600) / 60, remain % 60),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                Text(
                    "포근한 꿈나라로…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.weight(1f))
                BouncyButton(
                    onClick = { vm.wakeUp(); onBack() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    color = Palette.Primary
                ) {
                    Text("깨우기", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            } else {
                Text(
                    "얼마나 재울까요?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Palette.TextBrown
                )
                Text(
                    "현재 에너지 ${pet.energy.toInt()}%  ·  시간당 +15 회복",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.SubText
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0.5f to "30분", 2f to "2시간", 8f to "8시간").forEach { (h, label) ->
                        BouncyButton(
                            onClick = { vm.sleep(h) },
                            modifier = Modifier.weight(1f).height(70.dp),
                            color = Palette.Lavender.copy(alpha = 0.55f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌙", style = MaterialTheme.typography.titleLarge)
                                Text(label, style = MaterialTheme.typography.labelLarge, color = Palette.TextBrown)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                SoftCard(Modifier.fillMaxWidth()) {
                    Text(
                        "자는 동안에는 배고픔과 기분이 절반만 줄어요.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.SubText
                    )
                }
            }
        }
    }
}
