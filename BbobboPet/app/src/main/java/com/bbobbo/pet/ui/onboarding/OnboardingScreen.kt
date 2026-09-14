package com.bbobbo.pet.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.Foods
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.notify.Notifier
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.components.SpeechBubble
import com.bbobbo.pet.ui.theme.Palette
import kotlinx.coroutines.delay

/**
 * 기획서 S-00 스플래시 & 온보딩.
 * 스플래시 1.5초 동안 VM 이 DB 초기화/오프라인 정산을 끝내고,
 * 최초 실행이면 3스텝(이름 → 튜토리얼 → 알림 권한)을 거친다.
 */
@Composable
fun SplashGate(vm: PetViewModel, content: @Composable () -> Unit) {
    val pet by vm.pet.collectAsState()
    val loaded by vm.loaded.collectAsState()
    var splashDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        splashDone = true
    }

    when {
        !splashDone || !loaded -> SplashScreen()
        !pet.onboarded -> OnboardingScreen(vm)
        else -> content()
    }
}

@Composable
private fun SplashScreen() {
    val t = rememberInfiniteTransition(label = "splash")
    val fade by t.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "fade"
    )
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("뽀뽀 키우기", style = MaterialTheme.typography.displaySmall, color = Palette.TextBrown)
        Text("BBOBBO PET", style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
        ) {
            PetCharacter(modifier = Modifier.fillMaxSize(), anim = PetAnim.IDLE)
        }
        Text(
            "준비하는 중…",
            modifier = Modifier.alpha(fade),
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.SubText,
        )
    }
}

@Composable
private fun OnboardingScreen(vm: PetViewModel) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("뽀뽀") }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // 거부해도 게임은 그대로 진행한다 (QA 체크리스트: 알림 권한 거부 시에도 정상 동작).
        vm.finishOnboarding(name)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            when (step) {
                0 -> "반가워요!"
                1 -> "첫 밥을 줘볼까요?"
                else -> "알림을 받을까요?"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = Palette.TextBrown,
        )
        Text(
            "${step + 1} / 3",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Palette.SubText,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            PetCharacter(
                modifier = Modifier.fillMaxSize(),
                anim = if (step == 1) PetAnim.EAT else PetAnim.IDLE,
                hatId = vm.equippedId(WearSlot.HAT),
                clothId = vm.equippedId(WearSlot.CLOTH),
                accId = vm.equippedId(WearSlot.ACC),
            )
            SpeechBubble(
                text = when (step) {
                    0 -> "내 이름을 지어줘!"
                    1 -> "냠냠… 맛있어요!"
                    else -> "보고 싶을 때 부를게요"
                },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        when (step) {
            0 -> SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 6) name = it },
                        singleLine = true,
                        label = { Text("이름 (최대 6자)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            1 -> SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${Foods.ALL.first().emoji} ${Foods.ALL.first().name}를 주면 배고픔이 채워져요.",
                        style = MaterialTheme.typography.bodyLarge, color = Palette.TextBrown
                    )
                    Text(
                        "밥주기 · 씻기 · 재우기 · 놀아주기로 뽀뽀를 돌봐주세요.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall, color = Palette.SubText
                    )
                }
            }

            else -> SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "배고픔·청결이 낮아지면 알려드릴게요.",
                        style = MaterialTheme.typography.bodyLarge, color = Palette.TextBrown
                    )
                    Text(
                        "나중에 설정에서 종류별로 끌 수 있어요.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall, color = Palette.SubText
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        BouncyButton(
            onClick = {
                when (step) {
                    0 -> step = 1
                    1 -> step = 2
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !Notifier.hasPermission(context)
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.finishOnboarding(name)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = step != 0 || name.isNotBlank(),
        ) {
            Text(
                when (step) {
                    0 -> "이 이름으로 할래요"
                    1 -> "밥 주기 (코인 500 받기)"
                    else -> "알림 허용하고 시작"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }

        if (step == 2) {
            Spacer(Modifier.height(8.dp))
            Text(
                "나중에 할래요",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { vm.finishOnboarding(name) },
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.SubText,
                textAlign = TextAlign.Center,
            )
        }
    }
}
