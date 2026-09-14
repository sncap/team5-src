package com.bbobbo.pet.ui.pet

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.Balance
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.SectionTitle
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.components.StatBar
import com.bbobbo.pet.ui.theme.Palette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 하단 탭 '뽀뽀': 스탯 상세 + 누적 기록 + 착용 정보 (기획서 S-01 동작 정의) */
@Composable
fun PetDetailScreen(vm: PetViewModel, onOpenSettings: () -> Unit) {
    val pet by vm.pet.collectAsState()
    val anim by vm.anim.collectAsState()
    val setBonus = vm.activeSetBonus()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pet.name, style = MaterialTheme.typography.headlineMedium, color = Palette.TextBrown)
                Text(
                    "Lv.${pet.level} ${Balance.title(pet.level)}",
                    style = MaterialTheme.typography.bodyMedium, color = Palette.SubText
                )
            }
            Text(
                "⚙️",
                modifier = Modifier
                    .padding(8.dp)
                    .clickableNoRipple(onOpenSettings),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f),
            contentAlignment = Alignment.Center
        ) {
            PetCharacter(
                modifier = Modifier.fillMaxSize(),
                anim = anim,
                hatId = vm.equippedId(WearSlot.HAT),
                clothId = vm.equippedId(WearSlot.CLOTH),
                accId = vm.equippedId(WearSlot.ACC),
            )
        }

        SectionTitle("스탯")
        Spacer(Modifier.height(6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                StatBar("배고픔", "🍽️", pet.fullness, Palette.Primary)
                StatBar("기분", "🙂", pet.mood, Palette.Yellow)
                StatBar("청결", "🫧", pet.clean, Palette.SkyBlue)
                StatBar("에너지", "⚡", pet.energy, Palette.Mint)
                StatBar("친밀도", "💗", pet.bond, Palette.PrimaryDeep)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("함께한 기록")
        Spacer(Modifier.height(6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Record("만난 날", DATE_FMT.format(Date(pet.createdAt)))
                Record("함께한 지", "${daysTogether(pet.createdAt)}일째")
                Record("밥 준 횟수", "${pet.totalFeed}번")
                Record("씻긴 횟수", "${pet.totalBath}번")
                Record("놀아준 횟수", "${pet.totalPlay}번")
                Record("산책 횟수", "${pet.totalWalk}번")
                Record("미니게임", "${pet.totalGame}판")
                Record("최고 점수", "%,d점".format(pet.bestGameScore))
                Record("최고 콤보", "×${pet.bestGameCombo}")
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("착용 중")
        Spacer(Modifier.height(6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                WearSlot.entries.forEach { slot ->
                    val item = com.bbobbo.pet.domain.Wardrobe.byId(vm.equippedId(slot))
                    Record("${slot.emoji} ${slot.label}", item?.name ?: "없음")
                }
                if (setBonus != null) {
                    Record("✨ ${setBonus.name}", setBonus.desc)
                } else {
                    Text(
                        "세트를 맞추면 추가 보너스를 받아요",
                        style = MaterialTheme.typography.labelSmall, color = Palette.SubText
                    )
                }
                val (grade, bonus) = vm.roomGrade()
                Record("🏠 방 등급", "$grade (아늑함 ${vm.roomCozy()} · 일일 코인 +$bonus)")
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Record(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.SubText)
        Text(value, style = MaterialTheme.typography.labelLarge, color = Palette.TextBrown)
    }
}

private fun daysTogether(createdAt: Long): Long =
    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - createdAt) + 1

private val DATE_FMT = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)

/** 이모지 아이콘 버튼용 — 리플 없이 눌림만 처리한다. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = androidx.compose.runtime.remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    return this.clickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick,
    )
}
