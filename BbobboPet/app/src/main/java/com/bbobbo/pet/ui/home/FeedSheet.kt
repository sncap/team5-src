package com.bbobbo.pet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.Currency
import com.bbobbo.pet.domain.Food
import com.bbobbo.pet.domain.Foods
import com.bbobbo.pet.domain.Missions
import com.bbobbo.pet.domain.StatEngine
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette

@Composable
fun FeedSheet(vm: PetViewModel, onClose: () -> Unit) {
    val pet by vm.pet.collectAsState()
    var selected by remember { mutableStateOf(Foods.ALL.first()) }

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Text("무엇을 줄까요?", style = MaterialTheme.typography.titleLarge, color = Palette.TextBrown)
        Text(
            "배고픔 ${pet.fullness.toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.SubText
        )
        Spacer(Modifier.height(14.dp))

        Foods.ALL.forEach { food ->
            val affordable = StatEngine.canAfford(pet, food.currency, food.price) &&
                    (food.currency != Currency.FREE || vm.freeFoodLeft() > 0)
            FoodRow(
                food = food,
                selected = selected.id == food.id,
                enabled = affordable,
                freeLeft = if (food.currency == Currency.FREE) vm.freeFoodLeft() else null,
                onClick = { selected = food }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(10.dp))
        BouncyButton(
            onClick = { vm.feed(selected); onClose() },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            color = Palette.Primary
        ) {
            Text("먹이기", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
private fun FoodRow(
    food: Food,
    selected: Boolean,
    enabled: Boolean,
    freeLeft: Int?,
    onClick: () -> Unit,
) {
    SoftCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) Palette.SubPink else Palette.CardWhite,
        radius = 20
    ) {
        Row(
            Modifier.padding(14.dp).alpha(if (enabled) 1f else 0.45f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.CardBeige),
                contentAlignment = Alignment.Center
            ) {
                Text(food.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(food.name, style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
                    if (freeLeft != null) {
                        Text(
                            "  남은 ${freeLeft}회",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.SubText
                        )
                    }
                }
                Text(
                    food.effect.summary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.SubText
                )
            }
            Text(
                when (food.currency) {
                    Currency.FREE -> "무료"
                    Currency.COIN -> "🪙 ${food.price}"
                    Currency.HEART -> "💗 ${food.price}"
                },
                style = MaterialTheme.typography.labelLarge,
                color = Palette.PrimaryDeep
            )
        }
    }
}

@Composable
fun MissionSheet(vm: PetViewModel) {
    val missions by vm.missions.collectAsState()
    Column(
        Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("오늘의 미션", style = MaterialTheme.typography.titleLarge, color = Palette.TextBrown)
        missions.forEach { m ->
            val def = Missions.byId(m.missionId)
            val done = m.progress >= m.target
            SoftCard(Modifier.fillMaxWidth(), color = if (m.claimed) Palette.CardBeige else Palette.CardWhite) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(def.title, style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
                        Text(
                            "${m.progress} / ${m.target}   ·   " +
                                    if (m.rewardHeart > 0) "💗 ${m.rewardHeart}" else "🪙 ${m.rewardCoin}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.SubText
                        )
                    }
                    BouncyButton(
                        onClick = { vm.claimMission(m.missionId) },
                        modifier = Modifier.height(38.dp).padding(start = 8.dp),
                        enabled = done && !m.claimed,
                        color = if (m.claimed) Palette.CardBeige else Palette.Primary
                    ) {
                        Text(
                            if (m.claimed) "완료" else "받기",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (m.claimed) Palette.SubText else Color.White
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
