package com.bbobbo.pet.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.Attendance
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette

/** 기획서 S-11 출석체크. 7일 순환 캘린더. */
@Composable
fun AttendanceScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pending by vm.attendanceDay.collectAsState()
    var streak by remember { mutableIntStateOf(0) }

    // 출석은 앱 진입 시 자동 체크인되므로 여기서는 현재 일차만 읽는다.
    LaunchedEffect(pending) { streak = vm.todayStreak() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("출석체크", onBack)
        Spacer(Modifier.height(10.dp))

        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (streak > 0) "연속 ${streak}일째 출석 중이에요!" else "오늘 출석을 기다리고 있어요",
                    style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown
                )
                Text(
                    "7일마다 한 바퀴 돌아요. 하루라도 빠지면 1일차부터 다시 시작해요.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall, color = Palette.SubText
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 1~4일차 / 5~7일차 두 줄
        DayRow(1..4, streak)
        Spacer(Modifier.height(8.dp))
        DayRow(5..7, streak)

        Spacer(Modifier.height(16.dp))
        Text(
            "이번 주 개근하면 특별 보상을 드려요 🎁",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.SubText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DayRow(days: IntRange, streak: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            DayCell(day, claimed = day <= streak, today = day == streak, Modifier.weight(1f))
        }
        // 마지막 줄(3칸)의 폭을 4칸 줄과 맞춘다.
        repeat(4 - days.count()) { Box(Modifier.weight(1f)) }
    }
}

@Composable
private fun DayCell(day: Int, claimed: Boolean, today: Boolean, modifier: Modifier = Modifier) {
    val (label, _) = Attendance.REWARDS[day - 1]
    SoftCard(
        modifier
            .aspectRatio(0.8f)
            .then(
                if (today) Modifier.border(2.5.dp, Palette.Primary, RoundedCornerShape(18.dp))
                else Modifier
            ),
        color = if (claimed) Palette.SubPink else Palette.CardWhite,
        radius = 18,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "${day}일차",
                style = MaterialTheme.typography.labelSmall,
                color = if (claimed) Palette.PrimaryDeep else Palette.SubText,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (day == 7) "🎁" else if (label.startsWith("하트")) "💗" else "🪙",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Palette.TextBrown,
                textAlign = TextAlign.Center,
            )
            if (claimed) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.PrimaryDeep)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text("완료", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}
