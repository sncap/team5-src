package com.bbobbo.pet.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette

/** 하단 탭 '더보기': 도감 / 출석 / 설정 / 공지 / 약관 (기획서 S-01) */
@Composable
fun MoreScreen(
    vm: PetViewModel,
    onCollection: () -> Unit,
    onAttendance: () -> Unit,
    onSettings: () -> Unit,
) {
    val pet by vm.pet.collectAsState()
    var dialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text("더보기", style = MaterialTheme.typography.headlineMedium, color = Palette.TextBrown)
        Text(
            "${pet.name} · Lv.${pet.level}",
            style = MaterialTheme.typography.labelSmall, color = Palette.SubText
        )
        Spacer(Modifier.height(14.dp))

        MoreRow("📖", "도감", "${(vm.collectionRatio() * 100).toInt()}% 수집", onCollection)
        MoreRow("🗓️", "출석체크", "7일 순환 보상", onAttendance)
        MoreRow("⚙️", "설정", "사운드 · 알림 · 이름", onSettings)
        MoreRow("📢", "공지사항", "업데이트 소식") {
            dialog = "공지사항" to NOTICE
        }
        MoreRow("📄", "이용약관 / 개인정보처리방침", "데이터는 기기 안에만 저장돼요") {
            dialog = "이용약관 · 개인정보처리방침" to POLICY
        }

        Spacer(Modifier.height(28.dp))
    }

    dialog?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("닫기") } },
            title = { Text(title) },
            text = { Text(body) },
            containerColor = Palette.CardWhite,
        )
    }
}

@Composable
private fun MoreRow(emoji: String, title: String, sub: String, onClick: () -> Unit) {
    SoftCard(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        radius = 20,
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = Palette.SubText)
        }
    }
}

private const val NOTICE =
    "v1.0 출시!\n\n" +
        "· 산책과 도감이 추가됐어요\n" +
        "· 코디 세트를 5칸까지 저장할 수 있어요\n" +
        "· 방을 꾸미면 등급에 따라 매일 코인을 드려요"

private const val POLICY =
    "뽀뽀 키우기는 계정을 만들지 않고, 플레이 기록을 기기 안(로컬 DB)에만 저장해요. " +
        "이름·스탯·재화·아이템 정보는 외부 서버로 전송되지 않으며, " +
        "앱을 삭제하거나 설정에서 데이터를 초기화하면 함께 지워져요.\n\n" +
        "알림 권한은 뽀뽀의 상태를 알려드리는 용도로만 쓰고, 거부해도 게임은 모두 이용할 수 있어요."
