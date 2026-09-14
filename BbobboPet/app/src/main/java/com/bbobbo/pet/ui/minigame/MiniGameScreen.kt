package com.bbobbo.pet.ui.minigame

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.StatEngine
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette
import kotlin.math.abs
import kotlin.random.Random

private enum class Phase { LOBBY, PLAYING, RESULT }

private data class Snack(
    val id: Long,
    val x: Float,      // 0..1 비율
    val y: Float,      // 0..1 비율
    val kind: SnackKind,
    val speed: Float,
)

private enum class SnackKind(val emoji: String, val score: Int, val weight: Int) {
    BERRY("🍓", 50, 25),
    COOKIE("🍪", 30, 25),
    HEART_COOKIE("🩷", 80, 15),
    MELON("🍞", 60, 15),
    RICE("🍙", 40, 15),
    BOMB("🪨", -100, 5);

    companion object {
        private val pool = SnackKind.entries.flatMap { k -> List(k.weight) { k } }
        fun random(): SnackKind = pool[Random.nextInt(pool.size)]
    }
}

/** 간식 잡기 (기획서 S-06) */
@Composable
fun MiniGameScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    var phase by remember { mutableStateOf(Phase.LOBBY) }
    var score by remember { mutableStateOf(0) }
    var combo by remember { mutableStateOf(0) }
    var maxCombo by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(60f) }
    // QA: 전화 수신·백그라운드 전환 시 게임을 멈춘다.
    var paused by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_PAUSE -> paused = true
                Lifecycle.Event.ON_RESUME -> paused = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val petXState = remember { mutableStateOf(0.5f) }
    val snacks = remember { mutableListOf<Snack>().toMutableStateList() }

    fun reset() {
        score = 0; combo = 0; maxCombo = 0; timeLeft = 60f; petXState.value = 0.5f; snacks.clear()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("미니게임", onBack)

        when (phase) {
            Phase.LOBBY -> Lobby(
                best = pet.bestGameScore,
                energy = pet.energy.toInt(),
                onStart = {
                    if (vm.spendEnergyForGame()) { reset(); phase = Phase.PLAYING }
                }
            )
            Phase.PLAYING -> {
                GameHud(score, combo, timeLeft)
                Spacer(Modifier.height(8.dp))
                Box {
                    GameField(vm = vm, snacks = snacks, petXState = petXState)
                    if (paused) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(Palette.TextBrown.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "일시정지",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    var last = 0L
                    var spawnAcc = 0f
                    var nextId = 0L
                    while (timeLeft > 0f) {
                        withFrameNanos { now ->
                            // 일시정지 중에는 시간 기준점만 새로 잡고 아무것도 진행하지 않는다.
                            if (paused) { last = 0L; return@withFrameNanos }
                            val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                            last = now
                            if (dt <= 0f || dt > 0.2f) return@withFrameNanos

                            timeLeft = (timeLeft - dt).coerceAtLeast(0f)

                            // 난이도 곡선: 경과에 따라 낙하 속도와 스폰 빈도 상승
                            val elapsed = 60f - timeLeft
                            val speedMul = when {
                                elapsed < 20f -> 1.0f
                                elapsed < 40f -> 1.3f
                                else -> 1.6f
                            }
                            val spawnInterval = when {
                                elapsed < 20f -> 0.75f
                                elapsed < 40f -> 0.55f
                                else -> 0.40f
                            }

                            spawnAcc += dt
                            if (spawnAcc >= spawnInterval) {
                                spawnAcc = 0f
                                snacks.add(
                                    Snack(
                                        id = nextId++,
                                        x = 0.08f + Random.nextFloat() * 0.84f,
                                        y = -0.05f,
                                        kind = SnackKind.random(),
                                        speed = (0.30f + Random.nextFloat() * 0.12f) * speedMul
                                    )
                                )
                            }

                            val catchY = 0.78f
                            val petX = petXState.value
                            val survivors = ArrayList<Snack>(snacks.size)
                            snacks.forEach { s ->
                                val moved = s.copy(y = s.y + s.speed * dt)
                                val caught = moved.y >= catchY && moved.y < catchY + 0.14f &&
                                        abs(moved.x - petX) < 0.13f
                                when {
                                    caught -> {
                                        if (moved.kind == SnackKind.BOMB) {
                                            score = (score + moved.kind.score).coerceAtLeast(0)
                                            combo = 0
                                        } else {
                                            combo = (combo + 1).coerceAtMost(30)
                                            maxCombo = maxOf(maxCombo, combo)
                                            score += (moved.kind.score * (1 + combo * 0.05f)).toInt()
                                        }
                                    }
                                    moved.y > 1.05f -> {
                                        if (moved.kind != SnackKind.BOMB) combo = 0
                                    }
                                    else -> survivors.add(moved)
                                }
                            }
                            snacks.clear()
                            snacks.addAll(survivors)
                        }
                    }
                    vm.finishMiniGame(score, maxCombo)
                    phase = Phase.RESULT
                }
            }
            Phase.RESULT -> ResultPanel(
                score = score,
                maxCombo = maxCombo,
                onRetry = {
                    if (vm.spendEnergyForGame()) { reset(); phase = Phase.PLAYING }
                },
                onExit = onBack
            )
        }
    }
}

@Composable
private fun Lobby(best: Int, energy: Int, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("간식 잡기", style = MaterialTheme.typography.displaySmall, color = Palette.TextBrown)
        Text("떨어지는 간식을 받아보자!", style = MaterialTheme.typography.bodyLarge, color = Palette.SubText)
        Spacer(Modifier.height(24.dp))

        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("최고 점수", "%,d".format(best))
                InfoRow("소모 에너지", "⚡ 10  (보유 $energy)")
                InfoRow("제한 시간", "60초")
                InfoRow("획득 보상", "🪙 점수÷10   💗 1,500점 이상")
            }
        }
        Spacer(Modifier.height(16.dp))
        SoftCard(Modifier.fillMaxWidth(), color = Palette.SubPink) {
            Text(
                "많이 잡을수록 콤보가 쌓이고 점수가 크게 올라요. 돌멩이는 피하세요!",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextBrown
            )
        }
        Spacer(Modifier.weight(1f))
        BouncyButton(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            color = Palette.Mint
        ) {
            Text("시작", style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.SubText)
        Text(value, style = MaterialTheme.typography.labelLarge, color = Palette.TextBrown)
    }
}

@Composable
private fun GameHud(score: Int, combo: Int, timeLeft: Float) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HudChip("점수", "%,d".format(score), Modifier.weight(1f))
        HudChip("COMBO", "⭐ x$combo", Modifier.weight(1f))
        HudChip("남은 시간", "%02d:%02d".format(timeLeft.toInt() / 60, timeLeft.toInt() % 60), Modifier.weight(1f))
    }
}

@Composable
private fun HudChip(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.CardWhite)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
            Text(value, style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
        }
    }
}

@Composable
private fun GameField(
    vm: PetViewModel,
    snacks: SnapshotStateList<Snack>,
    petXState: MutableState<Float>,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(460.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFFCDE8F5), Color(0xFFDFF0D8)))
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, drag ->
                    change.consume()
                    petXState.value =
                        (petXState.value + drag / size.width).coerceIn(0.08f, 0.92f)
                }
            }
    ) {
        val w = maxWidth
        val h = maxHeight
        val petX = petXState.value

        snacks.forEach { s ->
            Box(
                Modifier.offset(x = w * s.x - 16.dp, y = h * s.y - 16.dp)
            ) {
                Text(s.kind.emoji, style = MaterialTheme.typography.headlineMedium)
            }
        }

        PetCharacter(
            modifier = Modifier
                .size(130.dp)
                .offset(x = w * petX - 65.dp, y = h * 0.72f),
            anim = PetAnim.WALK,
            hatId = vm.equippedId(WearSlot.HAT),
            clothId = vm.equippedId(WearSlot.CLOTH),
            accId = vm.equippedId(WearSlot.ACC),
        )
    }
}

@Composable
private fun ResultPanel(score: Int, maxCombo: Int, onRetry: () -> Unit, onExit: () -> Unit) {
    val reward = StatEngine.gameReward(score)
    Column(
        Modifier.fillMaxSize().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("게임 끝!", style = MaterialTheme.typography.displaySmall, color = Palette.TextBrown)
        Spacer(Modifier.height(20.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("점수", "%,d".format(score))
                InfoRow("최고 콤보", "x$maxCombo")
                InfoRow("획득 코인", "🪙 ${reward.coin}")
                InfoRow("획득 하트", "💗 ${reward.heart}")
                InfoRow("경험치", "EXP +${reward.exp}")
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BouncyButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(54.dp),
                color = Palette.Primary
            ) {
                Text("다시하기", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            BouncyButton(
                onClick = onExit,
                modifier = Modifier.weight(1f).height(54.dp),
                color = Palette.CardBeige
            ) {
                Text("나가기", style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
