package com.bbobbo.pet.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bbobbo.pet.domain.Balance
import com.bbobbo.pet.domain.Missions
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.PetMood
import com.bbobbo.pet.domain.StatEngine
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.ui.Routes
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.common.UiEvent
import com.bbobbo.pet.ui.components.CurrencyChip
import com.bbobbo.pet.ui.components.MenuTile
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.components.SpeechBubble
import com.bbobbo.pet.ui.components.StatBar
import com.bbobbo.pet.ui.theme.Palette
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: PetViewModel, nav: NavController) {
    val pet by vm.pet.collectAsState()
    val anim by vm.anim.collectAsState()
    val missions by vm.missions.collectAsState()
    val event by vm.event.collectAsState()
    val welcome by vm.welcome.collectAsState()
    val attendance by vm.attendanceDay.collectAsState()

    var showFeed by remember { mutableStateOf(false) }
    var showMissions by remember { mutableStateOf(false) }
    var bubbleIndex by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState()

    // 8초마다 말풍선 교체
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            bubbleIndex++
        }
    }

    val mood = StatEngine.mood(pet)
    val bubble = remember(bubbleIndex, mood, pet.fullness, pet.clean) {
        pickLine(mood, pet.fullness, pet.clean, pet.mood, bubbleIndex)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ---- 상단: 타이틀 + 재화 ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("뽀뽀 키우기", style = MaterialTheme.typography.headlineMedium, color = Palette.TextBrown)
                Text("BBOBBO PET", style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CurrencyChip("🪙", "%,d".format(pet.coin))
                CurrencyChip("💗", "${pet.heart}")
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- 레벨 카드 ----
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Lv.${pet.level}", style = MaterialTheme.typography.titleLarge, color = Palette.PrimaryDeep)
                    Text(
                        Balance.title(pet.level),
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.SubText
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${pet.exp} / ${Balance.expForLevel(pet.level)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.SubText
                    )
                }
                Spacer(Modifier.height(8.dp))
                val ratio by animateFloatAsState(StatEngine.expRatio(pet), tween(500), label = "exp")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(11.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE2D4))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(ratio.coerceIn(0f, 1f))
                            .height(11.dp)
                            .clip(CircleShape)
                            .background(Palette.Yellow)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ---- 스탯 패널 ----
        SoftCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                StatBar("배고픔", "🍽️", pet.fullness, Palette.Primary)
                StatBar("기분", "🙂", pet.mood, Palette.Yellow)
                StatBar("청결", "🫧", pet.clean, Palette.SkyBlue)
                StatBar("에너지", "⚡", pet.energy, Palette.Mint)
                StatBar("친밀도", "💗", pet.bond, Palette.PrimaryDeep)
            }
        }

        Spacer(Modifier.height(6.dp))

        // ---- 캐릭터 무대 ----
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            PetCharacter(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { vm.touch() }, onLongPress = { vm.touch() }) },
                anim = anim,
                hatId = vm.equippedId(WearSlot.HAT),
                clothId = vm.equippedId(WearSlot.CLOTH),
                accId = vm.equippedId(WearSlot.ACC),
            )
            SpeechBubble(
                text = bubble,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp)
            )
        }

        // ---- 오늘의 미션 ----
        val active = missions.firstOrNull { !it.claimed } ?: missions.firstOrNull()
        if (active != null) {
            val def = Missions.byId(active.missionId)
            val done = active.progress >= active.target && !active.claimed
            SoftCard(
                Modifier
                    .fillMaxWidth()
                    .clickable { showMissions = true },
                color = if (done) Palette.SubPink else Palette.CardWhite
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📋", style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("오늘의 미션", style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
                        Text(
                            "${def.title} (${active.progress}/${active.target})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.SubText
                        )
                    }
                    Text(
                        if (active.rewardHeart > 0) "💗 ${active.rewardHeart}" else "🪙 ${active.rewardCoin}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Palette.PrimaryDeep
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- 메인 액션 4종 ----
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MenuTile("밥주기", "🍚", Palette.SubPink, { showFeed = true }, Modifier.weight(1f), big = true)
            MenuTile("놀아주기", "🎈", Palette.Yellow.copy(alpha = 0.55f), { nav.navigate(Routes.PLAY) }, Modifier.weight(1f), big = true)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MenuTile("재우기", "🌙", Palette.Lavender.copy(alpha = 0.5f), { nav.navigate(Routes.SLEEP) }, Modifier.weight(1f), big = true)
            MenuTile("씻기", "🛁", Palette.SkyBlue.copy(alpha = 0.5f), { nav.navigate(Routes.BATH) }, Modifier.weight(1f), big = true)
        }

        Spacer(Modifier.height(12.dp))

        // ---- 서브 메뉴 ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuTile("미니게임", "🎮", Palette.CardWhite, { nav.navigate(Routes.GAME) }, Modifier.weight(1f))
            MenuTile("옷장", "👕", Palette.CardWhite, { nav.navigate(Routes.WARDROBE) }, Modifier.weight(1f))
            MenuTile("방꾸미기", "🛋️", Palette.CardWhite, { nav.navigate(Routes.ROOM) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuTile("산책", "🌳", Palette.CardWhite, { vm.play("walk", 0.8f) }, Modifier.weight(1f))
            MenuTile("도감", "📖", Palette.CardWhite, { }, Modifier.weight(1f))
            MenuTile("출석체크", "🗓️", Palette.CardWhite, { }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(28.dp))
    }

    // ---- 밥주기 바텀시트 ----
    if (showFeed) {
        ModalBottomSheet(
            onDismissRequest = { showFeed = false },
            sheetState = sheetState,
            containerColor = Palette.CreamBg
        ) {
            FeedSheet(vm) { showFeed = false }
        }
    }

    // ---- 미션 목록 바텀시트 ----
    if (showMissions) {
        ModalBottomSheet(
            onDismissRequest = { showMissions = false },
            containerColor = Palette.CreamBg
        ) {
            MissionSheet(vm)
        }
    }

    // ---- 복귀 팝업 ----
    welcome?.let { w ->
        AlertDialog(
            onDismissRequest = { vm.consumeWelcome() },
            confirmButton = {
                TextButton(onClick = { vm.consumeWelcome() }) { Text("뽀뽀 보러가기") }
            },
            title = { Text("뽀뽀가 기다렸어요") },
            text = {
                val h = w.hours.toInt()
                Text(
                    if (h >= 1) "${h}시간 만이에요. 배고픔과 기분이 줄어들었어요."
                    else "조금 전에 다녀갔네요. 스탯이 살짝 줄었어요."
                )
            },
            containerColor = Palette.CardWhite
        )
    }

    // ---- 출석 팝업 ----
    attendance?.let { day ->
        val reward = com.bbobbo.pet.domain.Attendance.REWARDS[(day - 1).coerceIn(0, 6)]
        AlertDialog(
            onDismissRequest = { vm.consumeAttendance() },
            confirmButton = {
                TextButton(onClick = {
                    vm.claimAttendance(day)
                    vm.consumeAttendance()
                }) { Text("보상 받기") }
            },
            title = { Text("출석 ${day}일차") },
            text = { Text("오늘의 보상: ${reward.first}") },
            containerColor = Palette.CardWhite
        )
    }

    // ---- 토스트 ----
    EventToast(event) { vm.consumeEvent() }
}

@Composable
private fun EventToast(event: UiEvent?, onDone: () -> Unit) {
    LaunchedEffect(event) {
        if (event != null) {
            delay(2000)
            onDone()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = event != null) {
            val bg = when (event?.kind) {
                UiEvent.Kind.DENY -> Palette.TextBrown
                UiEvent.Kind.LEVEL_UP -> Palette.PrimaryDeep
                else -> Palette.TextBrown.copy(alpha = 0.92f)
            }
            Box(
                Modifier
                    .padding(bottom = 40.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bg)
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text(
                    event?.message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

private fun pickLine(mood: PetMood, fullness: Float, clean: Float, happy: Float, seed: Int): String {
    val urgent = buildList {
        if (fullness <= 30f) add("배고파요… 밥 주세요 🥺")
        if (clean <= 30f) add("씻고 싶어요, 뽀득뽀득!")
        if (happy <= 30f) add("심심해요… 같이 놀아요")
    }
    if (urgent.isNotEmpty()) return urgent[seed % urgent.size]
    if (mood == PetMood.SLEEPING) return "쿨쿨… 좋은 꿈 꾸는 중"
    val normal = listOf(
        "오늘도 좋은 하루야! ♡",
        "같이 있어서 행복해요",
        "오늘은 뭐 하고 놀까요?",
        "쓰다듬어 주세요!",
        "작은 하루도 특별해져요"
    )
    return normal[seed % normal.size]
}
