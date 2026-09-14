package com.bbobbo.pet.domain

/**
 * 아이템이 실제로 게임에 미치는 효과. GameData 의 `perk` 문자열은 UI 표기용이고,
 * 계산에 쓰이는 값은 전부 여기에 모아 둔다 (기획서 T-29 / T-32).
 *
 * - 의상: 착용 중일 때 적용
 * - 가구/소품: 방에 **배치**했을 때 적용
 */
data class Perks(
    val moodPerHour: Float = 0f,
    val bondPerHour: Float = 0f,
    /** 청결 자연 감소 배율 (0.9 = 10% 덜 줄어듦) */
    val cleanDecayMul: Float = 1f,
    /** 수면 중 에너지 회복 배율 */
    val sleepEnergyMul: Float = 1f,
    /** 코인 획득 보너스 (%) */
    val coinBonusPercent: Int = 0,
    /** 무료 사료 일일 추가 횟수 */
    val freeFoodBonus: Int = 0,
) {
    operator fun plus(other: Perks) = Perks(
        moodPerHour = moodPerHour + other.moodPerHour,
        bondPerHour = bondPerHour + other.bondPerHour,
        cleanDecayMul = cleanDecayMul * other.cleanDecayMul,
        sleepEnergyMul = sleepEnergyMul * other.sleepEnergyMul,
        coinBonusPercent = coinBonusPercent + other.coinBonusPercent,
        freeFoodBonus = freeFoodBonus + other.freeFoodBonus,
    )

    /** 코인 보상에 획득 보너스를 적용한다. 보너스가 0이면 원래 값 그대로. */
    fun applyCoin(coin: Long): Long =
        if (coinBonusPercent == 0 || coin <= 0) coin
        else coin + (coin * coinBonusPercent / 100)

    companion object {
        val NONE = Perks()

        private val BY_ITEM = mapOf(
            // 의상 (착용 시)
            "hat_bear" to Perks(moodPerHour = 2f),
            "hat_rabbit" to Perks(bondPerHour = 1f),
            "hat_crown" to Perks(coinBonusPercent = 5),
            // 가구/소품 (배치 시)
            "f_bed" to Perks(sleepEnergyMul = 1.2f),
            "f_rug" to Perks(moodPerHour = 1f),
            "f_plant" to Perks(cleanDecayMul = 0.9f),
            "f_bowl" to Perks(freeFoodBonus = 2),
        )

        fun forItem(itemId: String): Perks = BY_ITEM[itemId] ?: NONE

        /**
         * 착용 의상 + 배치 가구 + 세트 보너스를 모두 합산한다.
         * 같은 가구를 여러 개 배치하면 효과도 그만큼 중첩된다.
         */
        fun total(
            equippedIds: List<String>,
            placedIds: List<String>,
            hatId: String,
            clothId: String,
            accId: String,
        ): Perks {
            var acc = NONE
            equippedIds.forEach { acc += forItem(it) }
            placedIds.forEach { acc += forItem(it) }
            OutfitBonuses.match(hatId, clothId, accId)?.let {
                acc += Perks(moodPerHour = it.moodPerHour, coinBonusPercent = it.coinBonusPercent)
            }
            return acc
        }
    }
}
