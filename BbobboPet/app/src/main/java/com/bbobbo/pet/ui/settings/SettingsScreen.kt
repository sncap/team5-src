package com.bbobbo.pet.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.BuildConfig
import com.bbobbo.pet.domain.NotifyKind
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.SectionTitle
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette

/** 기획서 S-12 설정 */
@Composable
fun SettingsScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    val settings by vm.settings.collectAsState()

    var nameInput by remember(pet.name) { mutableStateOf(pet.name) }
    // 데이터 초기화는 2단계 확인 (기획서 S-12)
    var wipeStep by remember { mutableStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("설정", onBack)
        Spacer(Modifier.height(12.dp))

        // ---- 이름 변경 ----
        SectionTitle("뽀뽀 이름")
        Spacer(Modifier.height(6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 6) nameInput = it },
                    singleLine = true,
                    label = { Text("최대 6자") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                BouncyButton(
                    onClick = { vm.rename(nameInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    enabled = nameInput.isNotBlank() && nameInput != pet.name,
                ) {
                    Text("이름 바꾸기", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- 사운드 ----
        SectionTitle("사운드")
        Spacer(Modifier.height(6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                ToggleRow("효과음", settings.sound) { vm.setSound(it) }
                Spacer(Modifier.height(4.dp))
                Text("BGM 볼륨", style = MaterialTheme.typography.bodyMedium, color = Palette.TextBrown)
                Slider(
                    value = settings.bgmVolume,
                    onValueChange = { vm.setBgmVolume(it) },
                    enabled = settings.sound,
                )
                ToggleRow("진동", settings.vibration) { vm.setVibration(it) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- 알림 ----
        SectionTitle("알림")
        Spacer(Modifier.height(6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                NotifyKind.entries.forEach { kind ->
                    ToggleRow(kind.label, settings.notifyEnabled(kind), kind.desc) {
                        vm.setNotify(kind, it)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- 정보 ----
        SectionTitle("정보")
        Spacer(Modifier.height(6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("버전", BuildConfig.VERSION_NAME)
                InfoRow("문의", "bbobbo@example.com")
                Text(
                    "이 게임은 기기 안에만 데이터를 저장하고 외부로 전송하지 않아요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.SubText,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        BouncyButton(
            onClick = { wipeStep = 1 },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            color = Palette.CardBeige,
        ) {
            Text("데이터 초기화", style = MaterialTheme.typography.labelLarge, color = Palette.TextBrown)
        }

        Spacer(Modifier.height(28.dp))
    }

    // ---- 초기화 2단계 확인 ----
    if (wipeStep == 1) {
        AlertDialog(
            onDismissRequest = { wipeStep = 0 },
            confirmButton = { TextButton(onClick = { wipeStep = 2 }) { Text("계속") } },
            dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text("취소") } },
            title = { Text("데이터를 초기화할까요?") },
            text = { Text("레벨·재화·아이템·도감이 모두 사라져요. 되돌릴 수 없어요.") },
            containerColor = Palette.CardWhite,
        )
    }
    if (wipeStep == 2) {
        AlertDialog(
            onDismissRequest = { wipeStep = 0 },
            confirmButton = {
                TextButton(onClick = { vm.wipeAllData { wipeStep = 0 } }) {
                    Text("정말 지울래요", color = Palette.PrimaryDeep)
                }
            },
            dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text("아니요") } },
            title = { Text("마지막 확인이에요") },
            text = { Text("${pet.name}와(과) 함께한 기록이 전부 사라져요.") },
            containerColor = Palette.CardWhite,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    desc: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.TextBrown)
            if (desc != null) {
                Text(desc, style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Palette.Primary,
            ),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.TextBrown)
        Text(value, style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
    }
    Box(Modifier.fillMaxWidth())
}
