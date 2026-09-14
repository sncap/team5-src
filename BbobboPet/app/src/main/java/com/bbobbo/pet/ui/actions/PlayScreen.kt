package com.bbobbo.pet.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette
import kotlin.random.Random

/** 놀아주기 (기획서 S-03): 공놀이 / 숨바꼭질 / 비눗방울 */
@Composable
fun PlayScreen(vm: PetViewModel, onBack: () -> Unit) {
    var mode by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader("놀아주기", onBack)
        Spacer(Modifier.height(10.dp))

        when (mode) {
            null -> PlayMenu { mode = it }
            "ball" -> BallGame(vm) { onBack() }
            "hide" -> HideGame(vm) { onBack() }
            "bubble" -> BubbleGame(vm) { onBack() }
        }
    }
}

@Composable
private fun PlayMenu(onPick: (String) -> Unit) {
    Text("무엇을 하고 놀까요?", style = MaterialTheme.typography.titleLarge, color = Palette.TextBrown)
    Spacer(Modifier.height(16.dp))
    listOf(
        Triple("ball", "공놀이", "🏐"),
        Triple("hide", "숨바꼭질", "📦"),
        Triple("bubble", "비눗방울", "🫧"),
    ).forEach { (id, name, emoji) ->
        SoftCard(
            Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onPick(id) }
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.headlineMedium)
                Text(
                    name,
                    modifier = Modifier.padding(start = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.TextBrown
                )
                Spacer(Modifier.weight(1f))
                Text("에너지 5", style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
            }
        }
    }
}

@Composable
private fun BallGame(vm: PetViewModel, onDone: () -> Unit) {
    var hits by remember { mutableIntStateOf(0) }
    var pos by remember { mutableStateOf(0.5f to 0.4f) }

    Text("공을 3번 튕겨주세요! ($hits/3)", style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
    Box(Modifier.fillMaxWidth().aspectRatio(0.9f)) {
        PetCharacter(Modifier.fillMaxSize(), anim = PetAnim.WALK,
            hatId = vm.equippedId(WearSlot.HAT), clothId = vm.equippedId(WearSlot.CLOTH), accId = vm.equippedId(WearSlot.ACC))
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    start = (pos.first * 220).dp,
                    top = (pos.second * 240).dp
                )
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Palette.Yellow)
                    .clickable {
                        hits++
                        pos = Random.nextFloat() * 0.7f to Random.nextFloat() * 0.6f
                        if (hits >= 3) { vm.play("ball", 1f); onDone() }
                    },
                contentAlignment = Alignment.Center
            ) { Text("🏐", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
private fun HideGame(vm: PetViewModel, onDone: () -> Unit) {
    val answer = remember { Random.nextInt(3) }
    var picked by remember { mutableStateOf<Int?>(null) }

    Text("뽀뽀는 어느 상자에 있을까요?", style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { i ->
            BouncyButton(
                onClick = {
                    if (picked == null) {
                        picked = i
                        vm.play("hide", if (i == answer) 1f else 0.4f)
                    }
                },
                modifier = Modifier.weight(1f).height(110.dp),
                color = if (picked != null && i == answer) Palette.SubPink else Palette.CardWhite
            ) {
                Text(
                    if (picked != null && i == answer) "🧸" else "📦",
                    style = MaterialTheme.typography.displaySmall
                )
            }
        }
    }
    if (picked != null) {
        Spacer(Modifier.height(20.dp))
        Text(
            if (picked == answer) "정답! 뽀뽀가 신났어요" else "아쉬워요, 여기 있었네요",
            style = MaterialTheme.typography.bodyLarge,
            color = Palette.SubText
        )
        Spacer(Modifier.height(16.dp))
        BouncyButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("돌아가기", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
private fun BubbleGame(vm: PetViewModel, onDone: () -> Unit) {
    var popped by remember { mutableIntStateOf(0) }
    val seeds = remember { List(8) { Random.nextFloat() * 0.75f to Random.nextFloat() * 0.7f } }
    var alive by remember { mutableStateOf(seeds.indices.toSet()) }

    Text("비눗방울을 터뜨려요! ($popped/8)", style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
    Box(Modifier.fillMaxWidth().aspectRatio(0.85f)) {
        PetCharacter(Modifier.fillMaxSize(), anim = PetAnim.JUMP,
            hatId = vm.equippedId(WearSlot.HAT), clothId = vm.equippedId(WearSlot.CLOTH), accId = vm.equippedId(WearSlot.ACC))
        seeds.forEachIndexed { i, (fx, fy) ->
            if (i in alive) {
                Box(
                    Modifier
                        .padding(start = (fx * 260).dp, top = (fy * 280).dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Palette.SkyBlue.copy(alpha = 0.6f))
                        .clickable {
                            alive = alive - i
                            popped++
                            if (popped >= 8) { vm.play("bubble", 1f); onDone() }
                        }
                )
            }
        }
    }
}
