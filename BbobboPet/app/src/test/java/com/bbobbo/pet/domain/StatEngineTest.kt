package com.bbobbo.pet.domain

import com.bbobbo.pet.data.local.PetStateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 기획서 §10 QA 체크리스트 중 순수 계산으로 검증 가능한 항목들. */
class StatEngineTest {

    private val hour = 3_600_000L
    private val now = 1_700_000_000_000L

    private fun state(
        lastSeenAt: Long = now,
        fullness: Float = 100f,
        mood: Float = 100f,
        clean: Float = 100f,
        energy: Float = 50f,
        bond: Float = 100f,
        sleepEndAt: Long? = null,
    ) = PetStateEntity(
        lastSeenAt = lastSeenAt,
        fullness = fullness,
        mood = mood,
        clean = clean,
        energy = energy,
        bond = bond,
        sleepEndAt = sleepEndAt,
    )

    // ---- 오프라인 정산 ----

    @Test
    fun `1시간 비우면 스탯이 감소율만큼 줄어든다`() {
        val result = StatEngine.applyElapsed(state(lastSeenAt = now - hour), now)

        assertEquals(100f - Balance.DECAY_FULLNESS, result.state.fullness, 0.01f)
        assertEquals(100f - Balance.DECAY_MOOD, result.state.mood, 0.01f)
        assertEquals(100f - Balance.DECAY_CLEAN, result.state.clean, 0.01f)
        assertEquals(100f - Balance.DECAY_BOND, result.state.bond, 0.01f)
        assertEquals(now, result.state.lastSeenAt)
    }

    @Test
    fun `기기 시간을 되돌려도 스탯이 오르지 않는다`() {
        // lastSeenAt 이 미래인 상태 = 사용자가 시계를 과거로 돌린 경우
        val before = state(lastSeenAt = now + 10 * hour, fullness = 40f)
        val result = StatEngine.applyElapsed(before, now)

        assertEquals(40f, result.state.fullness, 0.01f)
        assertEquals(0f, result.awayHours, 0.01f)
        assertEquals(false, result.changed)
        // 되돌린 시각으로 lastSeenAt 을 다시 찍어 이후 정산 기준을 복구한다.
        assertEquals(now, result.state.lastSeenAt)
    }

    @Test
    fun `7일을 비워도 12시간치까지만 적용된다`() {
        val result = StatEngine.applyElapsed(state(lastSeenAt = now - 7 * 24 * hour), now)

        val capped = 100f - Balance.MAX_OFFLINE_HOURS * Balance.DECAY_FULLNESS
        assertEquals(capped, result.state.fullness, 0.01f)
        assertTrue("상한 덕분에 바닥나지 않아야 한다", result.state.fullness > 0f)
    }

    @Test
    fun `잠자는 중에는 배고픔과 기분이 절반만 줄어든다`() {
        val result = StatEngine.applyElapsed(
            state(lastSeenAt = now - hour, sleepEndAt = now + hour), now
        )

        assertEquals(100f - Balance.DECAY_FULLNESS * 0.5f, result.state.fullness, 0.01f)
        assertEquals(100f - Balance.DECAY_MOOD * 0.5f, result.state.mood, 0.01f)
        // 청결은 수면과 무관하게 정상 감소
        assertEquals(100f - Balance.DECAY_CLEAN, result.state.clean, 0.01f)
    }

    @Test
    fun `수면이 끝났으면 sleepEndAt 이 정리된다`() {
        val result = StatEngine.applyElapsed(
            state(lastSeenAt = now - 2 * hour, sleepEndAt = now - hour), now
        )
        assertNull(result.state.sleepEndAt)
    }

    @Test
    fun `수면 중에는 시간당 15씩 에너지가 회복된다`() {
        val result = StatEngine.applyElapsed(
            state(lastSeenAt = now - hour, energy = 0f, sleepEndAt = now + hour), now
        )
        assertEquals(Balance.SLEEP_ENERGY_PER_HOUR, result.state.energy, 0.01f)
    }

    @Test
    fun `스탯은 0 아래로 내려가지 않는다`() {
        val result = StatEngine.applyElapsed(
            state(lastSeenAt = now - 12 * hour, fullness = 1f, mood = 1f, clean = 1f, bond = 1f), now
        )
        assertEquals(0f, result.state.fullness, 0.01f)
        assertEquals(0f, result.state.mood, 0.01f)
        assertEquals(0f, result.state.clean, 0.01f)
        assertEquals(0f, result.state.bond, 0.01f)
    }

    // ---- 레벨 / 재화 ----

    @Test
    fun `경험치가 넘치면 레벨이 오르고 보너스 코인을 받는다`() {
        val before = state().copy(level = 1, exp = 0, coin = 0)
        val need = Balance.expForLevel(1)
        val (after, ups) = StatEngine.apply(before, StatChange(exp = need))

        assertEquals(2, after.level)
        assertEquals(1, ups)
        assertEquals(0, after.exp)
        assertEquals(2 * 50L, after.coin)
    }

    @Test
    fun `한 번에 여러 레벨이 오를 수 있다`() {
        val before = state().copy(level = 1, exp = 0)
        val huge = Balance.expForLevel(1) + Balance.expForLevel(2) + Balance.expForLevel(3)
        val (after, ups) = StatEngine.apply(before, StatChange(exp = huge))

        assertEquals(4, after.level)
        assertEquals(3, ups)
    }

    @Test
    fun `재화는 음수가 되지 않는다`() {
        val before = state().copy(coin = 10, heart = 1)
        val (after, _) = StatEngine.apply(before, StatChange(coin = -500, heart = -50))

        assertEquals(0L, after.coin)
        assertEquals(0, after.heart)
    }

    @Test
    fun `스탯은 100 위로 올라가지 않는다`() {
        val (after, _) = StatEngine.apply(state(fullness = 95f), StatChange(fullness = 50f))
        assertEquals(100f, after.fullness, 0.01f)
    }

    @Test
    fun `보유 재화보다 비싼 물건은 살 수 없다`() {
        val poor = state().copy(coin = 50, heart = 2)

        assertTrue(StatEngine.canAfford(poor, Currency.FREE, 0))
        assertEquals(false, StatEngine.canAfford(poor, Currency.COIN, 100))
        assertEquals(false, StatEngine.canAfford(poor, Currency.HEART, 10))
        assertTrue(StatEngine.canAfford(poor, Currency.COIN, 50))
    }

    // ---- 퍽 ----

    @Test
    fun `왕관을 쓰면 코인 획득이 5퍼센트 늘어난다`() {
        val perks = Perks.forItem("hat_crown")
        val (after, _) = StatEngine.apply(state().copy(coin = 0), StatChange(coin = 1000), perks)
        assertEquals(1050L, after.coin)
    }

    @Test
    fun `화분을 놓으면 청결이 10퍼센트 덜 줄어든다`() {
        val perks = Perks.forItem("f_plant")
        val result = StatEngine.applyElapsed(state(lastSeenAt = now - hour), now, perks)
        assertEquals(100f - Balance.DECAY_CLEAN * 0.9f, result.state.clean, 0.01f)
    }

    @Test
    fun `침대를 놓으면 수면 회복이 20퍼센트 늘어난다`() {
        val perks = Perks.forItem("f_bed")
        val result = StatEngine.applyElapsed(
            state(lastSeenAt = now - hour, energy = 0f, sleepEndAt = now + hour), now, perks
        )
        assertEquals(Balance.SLEEP_ENERGY_PER_HOUR * 1.2f, result.state.energy, 0.01f)
    }

    @Test
    fun `세트를 맞추면 퍽이 합산된다`() {
        val bonus = OutfitBonuses.ALL.first()
        val perks = Perks.total(
            equippedIds = listOf(bonus.hatId, bonus.clothId, bonus.accId),
            placedIds = emptyList(),
            hatId = bonus.hatId, clothId = bonus.clothId, accId = bonus.accId,
        )
        // 곰돌이 모자(기분 +2/시간) + 곰돌이 세트 보너스(기분 +5)
        assertEquals(
            Perks.forItem(bonus.hatId).moodPerHour + bonus.moodPerHour,
            perks.moodPerHour,
            0.01f
        )
        assertEquals(bonus.coinBonusPercent, perks.coinBonusPercent)
    }

    // ---- 미니게임 보상 ----

    @Test
    fun `미니게임 보상은 점수 구간대로 지급된다`() {
        assertEquals(100L, StatEngine.gameReward(1000).coin)
        assertEquals(10, StatEngine.gameReward(1000).exp)
        assertEquals(0, StatEngine.gameReward(1000).heart)
        assertEquals(1, StatEngine.gameReward(1500).heart)
        assertEquals(3, StatEngine.gameReward(3000).heart)
    }

    // ---- 감정 판정 ----

    @Test
    fun `가장 낮은 스탯이 30 이하면 슬퍼한다`() {
        assertEquals(PetMood.SAD, StatEngine.mood(state(fullness = 20f), now))
    }

    @Test
    fun `수면 중이면 다른 스탯과 무관하게 SLEEPING 이다`() {
        assertEquals(
            PetMood.SLEEPING,
            StatEngine.mood(state(fullness = 5f, sleepEndAt = now + hour), now)
        )
    }
}
