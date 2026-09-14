package com.bbobbo.pet.domain

import com.bbobbo.pet.data.local.PetStateEntity
import kotlin.math.floor
import kotlin.math.min

data class DecayResult(
    val state: PetStateEntity,
    val awayHours: Float,
    val changed: Boolean,
)

object StatEngine {

    private fun clamp(v: Float) = v.coerceIn(0f, 100f)

    /**
     * 마지막 접속 시각 기준 일괄 정산.
     * - 기기 시간을 되돌린 경우(now < lastSeenAt) 경과시간 0으로 처리해 치트를 막는다.
     * - 오프라인 감소는 최대 12시간치까지만 적용한다.
     */
    fun applyElapsed(state: PetStateEntity, now: Long = System.currentTimeMillis()): DecayResult {
        val rawHours = (now - state.lastSeenAt) / 3_600_000f
        if (rawHours <= 0f) {
            return DecayResult(state.copy(lastSeenAt = now), 0f, false)
        }
        val capped = min(rawHours, Balance.MAX_OFFLINE_HOURS)

        val sleeping = state.sleepEndAt != null && now < state.sleepEndAt
        val decayMul = if (sleeping) 0.5f else 1f

        var energy = state.energy
        energy += if (state.sleepEndAt != null) {
            // 수면 구간과 일반 구간을 나눠 회복량 계산
            val sleepEnd = state.sleepEndAt
            val sleptMs = (min(now, sleepEnd) - state.lastSeenAt).coerceAtLeast(0L)
            val sleptHours = sleptMs / 3_600_000f
            val awakeHours = (capped - sleptHours).coerceAtLeast(0f)
            sleptHours * Balance.SLEEP_ENERGY_PER_HOUR + awakeHours * Balance.ENERGY_RECOVER_PER_HOUR
        } else {
            capped * Balance.ENERGY_RECOVER_PER_HOUR
        }

        val next = state.copy(
            fullness = clamp(state.fullness - capped * Balance.DECAY_FULLNESS * decayMul),
            mood = clamp(state.mood - capped * Balance.DECAY_MOOD * decayMul),
            clean = clamp(state.clean - capped * Balance.DECAY_CLEAN),
            energy = clamp(energy),
            bond = clamp(state.bond - capped * Balance.DECAY_BOND),
            sleepEndAt = if (state.sleepEndAt != null && now >= state.sleepEndAt) null else state.sleepEndAt,
            walkEndAt = if (state.walkEndAt != null && now >= state.walkEndAt) null else state.walkEndAt,
            lastSeenAt = now,
        )
        return DecayResult(next, rawHours, rawHours > 0.05f)
    }

    /** 스탯/재화 변화 적용 + 레벨업 판정. 반환: (상태, 오른 레벨 수) */
    fun apply(state: PetStateEntity, c: StatChange): Pair<PetStateEntity, Int> {
        var level = state.level
        var exp = state.exp + c.exp
        var bonusCoin = 0L
        var levelUps = 0
        while (exp >= Balance.expForLevel(level)) {
            exp -= Balance.expForLevel(level)
            level += 1
            levelUps += 1
            bonusCoin += level * 50L
        }
        val next = state.copy(
            fullness = clamp(state.fullness + c.fullness),
            mood = clamp(state.mood + c.mood),
            clean = clamp(state.clean + c.clean),
            energy = clamp(state.energy + c.energy),
            bond = clamp(state.bond + c.bond),
            coin = (state.coin + c.coin + bonusCoin).coerceAtLeast(0),
            heart = (state.heart + c.heart).coerceAtLeast(0),
            level = level,
            exp = exp,
        )
        return next to levelUps
    }

    fun canAfford(state: PetStateEntity, currency: Currency, price: Int): Boolean = when (currency) {
        Currency.FREE -> true
        Currency.COIN -> state.coin >= price
        Currency.HEART -> state.heart >= price
    }

    fun pay(state: PetStateEntity, currency: Currency, price: Int): PetStateEntity = when (currency) {
        Currency.FREE -> state
        Currency.COIN -> state.copy(coin = state.coin - price)
        Currency.HEART -> state.copy(heart = state.heart - price)
    }

    /** 가장 낮은 스탯 기준 캐릭터 감정 판정 */
    fun mood(state: PetStateEntity, now: Long = System.currentTimeMillis()): PetMood {
        if (state.sleepEndAt != null && now < state.sleepEndAt) return PetMood.SLEEPING
        val lowest = minOf(state.fullness, state.mood, state.clean, state.bond)
        return when {
            lowest <= 30f -> PetMood.SAD
            state.mood >= 80f && state.fullness >= 60f -> PetMood.HAPPY
            else -> PetMood.NORMAL
        }
    }

    fun expRatio(state: PetStateEntity): Float =
        state.exp.toFloat() / Balance.expForLevel(state.level).toFloat()

    /** 미니게임 보상 산정 (기획서 S-06) */
    fun gameReward(score: Int): StatChange = StatChange(
        coin = floor(score / 10.0).toLong(),
        heart = when {
            score >= 3000 -> 3
            score >= 1500 -> 1
            else -> 0
        },
        exp = floor(score / 100.0).toInt(),
        mood = 5f,
    )
}

enum class PetMood { NORMAL, HAPPY, SAD, SLEEPING }

enum class PetAnim { IDLE, JUMP, EAT, BATH, SLEEP, WALK, SAD }
