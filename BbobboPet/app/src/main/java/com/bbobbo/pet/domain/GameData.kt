package com.bbobbo.pet.domain

/** 기획서 §1 밸런싱 테이블. 추후 assets/balance.json 으로 외부화 예정. */
object Balance {
    // 시간당 자연 감소량
    const val DECAY_FULLNESS = 5f
    const val DECAY_MOOD = 4f
    const val DECAY_CLEAN = 3f
    const val DECAY_BOND = 1f

    /** 오프라인 감소 상한 (시간) */
    const val MAX_OFFLINE_HOURS = 12f

    /** 에너지 자동 회복: 6분당 +1 */
    const val ENERGY_RECOVER_PER_HOUR = 10f

    /** 수면 중 회복량 (시간당) */
    const val SLEEP_ENERGY_PER_HOUR = 15f

    const val FREE_FOOD_PER_DAY = 5

    fun expForLevel(level: Int): Int = 100 + level * 40

    fun title(level: Int): String = when {
        level < 5 -> "아기 뽀뽀"
        level < 10 -> "쑥쑥 뽀뽀"
        level < 15 -> "행복한 뽀뽀"
        level < 20 -> "사랑둥이 뽀뽀"
        else -> "반짝반짝 뽀뽀"
    }
}

enum class StatKind(val label: String) {
    FULLNESS("배고픔"), MOOD("기분"), CLEAN("청결"), ENERGY("에너지"), BOND("친밀도")
}

data class StatChange(
    val fullness: Float = 0f,
    val mood: Float = 0f,
    val clean: Float = 0f,
    val energy: Float = 0f,
    val bond: Float = 0f,
    val exp: Int = 0,
    val coin: Long = 0,
    val heart: Int = 0,
) {
    /** "배고픔 +30 · 기분 +5 · EXP +12" 형태 요약 */
    fun summary(): String = buildList {
        if (fullness != 0f) add("배고픔 ${signed(fullness)}")
        if (mood != 0f) add("기분 ${signed(mood)}")
        if (clean != 0f) add("청결 ${signed(clean)}")
        if (energy != 0f) add("에너지 ${signed(energy)}")
        if (bond != 0f) add("친밀도 ${signed(bond)}")
        if (exp != 0) add("EXP +$exp")
        if (coin != 0L) add("코인 +$coin")
        if (heart != 0) add("하트 +$heart")
    }.joinToString(" · ")

    private fun signed(v: Float): String {
        val i = v.toInt()
        return if (v >= 0) "+$i" else "$i"
    }
}

enum class Currency { FREE, COIN, HEART }

data class Food(
    val id: String,
    val name: String,
    val emoji: String,
    val currency: Currency,
    val price: Int,
    val effect: StatChange,
    val desc: String,
)

object Foods {
    val ALL = listOf(
        Food("food_basic", "기본 사료", "🥣", Currency.FREE, 0,
            StatChange(fullness = 15f, mood = 2f, bond = 1f, exp = 8),
            "하루 ${Balance.FREE_FOOD_PER_DAY}번까지 무료"),
        Food("food_rice", "맛있는 밥", "🍚", Currency.COIN, 100,
            StatChange(fullness = 30f, mood = 5f, bond = 2f, exp = 12),
            "든든하게 배를 채워요"),
        Food("food_berry", "딸기 간식", "🍓", Currency.COIN, 250,
            StatChange(fullness = 20f, mood = 15f, bond = 3f, exp = 15),
            "기분이 확 좋아져요"),
        Food("food_cake", "생일 케이크", "🎂", Currency.HEART, 10,
            StatChange(fullness = 50f, mood = 30f, bond = 10f, exp = 40),
            "특별한 날을 위한 선물"),
    )
    fun byId(id: String) = ALL.first { it.id == id }
}

enum class WearSlot(val label: String) { HAT("모자"), CLOTH("옷"), ACC("액세서리") }

data class WearItem(
    val id: String,
    val name: String,
    val slot: WearSlot,
    val currency: Currency,
    val price: Int,
    val unlockLevel: Int,
    val perk: String? = null,
)

object Wardrobe {
    val ALL = listOf(
        WearItem("hat_none", "기본", WearSlot.HAT, Currency.FREE, 0, 1),
        WearItem("hat_bear", "곰돌이 모자", WearSlot.HAT, Currency.COIN, 800, 3, "기분 +2/시간"),
        WearItem("hat_rabbit", "토끼 모자", WearSlot.HAT, Currency.COIN, 1000, 5, "친밀도 +1/시간"),
        WearItem("hat_frog", "개구리 모자", WearSlot.HAT, Currency.COIN, 1200, 7),
        WearItem("hat_beret", "베레모", WearSlot.HAT, Currency.COIN, 900, 4),
        WearItem("hat_party", "꼬깔 모자", WearSlot.HAT, Currency.COIN, 600, 2),
        WearItem("hat_crown", "왕관", WearSlot.HAT, Currency.HEART, 30, 15, "코인 획득 +5%"),
        WearItem("acc_glasses", "안경", WearSlot.ACC, Currency.COIN, 700, 6),
        WearItem("cloth_none", "기본", WearSlot.CLOTH, Currency.FREE, 0, 1),
        WearItem("cloth_scarf", "체크 스카프", WearSlot.CLOTH, Currency.FREE, 0, 1),
        WearItem("cloth_apron", "앞치마", WearSlot.CLOTH, Currency.COIN, 850, 4),
        WearItem("cloth_cape", "별무늬 망토", WearSlot.CLOTH, Currency.COIN, 1400, 9),
        WearItem("acc_none", "없음", WearSlot.ACC, Currency.FREE, 0, 1),
        WearItem("acc_ribbon", "리본", WearSlot.ACC, Currency.COIN, 500, 2),
        WearItem("acc_bell", "방울 목걸이", WearSlot.ACC, Currency.COIN, 750, 5),
    )
    fun bySlot(slot: WearSlot) = ALL.filter { it.slot == slot }
    fun byId(id: String) = ALL.firstOrNull { it.id == id }
    val DEFAULTS = listOf("hat_none", "cloth_scarf", "acc_none")
}

enum class FurnitureCategory(val label: String) {
    FURNITURE("가구"), WALL("벽지"), FLOOR("바닥"), PROP("소품")
}

data class Furniture(
    val id: String,
    val name: String,
    val emoji: String,
    val category: FurnitureCategory,
    val currency: Currency,
    val price: Int,
    val cozy: Int,
    val perk: String? = null,
)

object Furnitures {
    val ALL = listOf(
        Furniture("f_bed", "체크 침대", "🛏️", FurnitureCategory.FURNITURE, Currency.COIN, 1200, 12, "수면 회복 +20%"),
        Furniture("f_rug", "곰돌이 러그", "🟤", FurnitureCategory.FURNITURE, Currency.COIN, 800, 8, "기분 +1/시간"),
        Furniture("f_cabinet", "우드 수납장", "🗄️", FurnitureCategory.FURNITURE, Currency.COIN, 1500, 14),
        Furniture("f_lamp", "토끼 스탠드", "💡", FurnitureCategory.FURNITURE, Currency.COIN, 900, 9),
        Furniture("f_plant", "초록 화분", "🪴", FurnitureCategory.PROP, Currency.COIN, 600, 6, "청결 감소 -10%"),
        Furniture("f_bowl", "뽀뽀 밥그릇", "🍜", FurnitureCategory.PROP, Currency.HEART, 50, 10, "무료 사료 +2회"),
        Furniture("f_teddy", "곰인형", "🧸", FurnitureCategory.PROP, Currency.COIN, 450, 5),
        Furniture("f_frame", "액자", "🖼️", FurnitureCategory.PROP, Currency.COIN, 380, 4),
        Furniture("w_cream", "크림 벽지", "🟨", FurnitureCategory.WALL, Currency.FREE, 0, 0),
        Furniture("w_check", "핑크 체크 벽지", "🩷", FurnitureCategory.WALL, Currency.COIN, 700, 6),
        Furniture("w_night", "별밤 벽지", "🌙", FurnitureCategory.WALL, Currency.COIN, 1100, 10),
        Furniture("fl_wood", "우드 바닥", "🟫", FurnitureCategory.FLOOR, Currency.FREE, 0, 0),
    )
    fun byCategory(c: FurnitureCategory) = ALL.filter { it.category == c }
    fun byId(id: String) = ALL.firstOrNull { it.id == id }

    fun grade(cozy: Int): Pair<String, Int> = when {
        cozy >= 60 -> "S" to 200
        cozy >= 40 -> "A" to 100
        cozy >= 20 -> "B" to 50
        else -> "C" to 0
    }
}

data class MissionDef(
    val id: String,
    val title: String,
    val target: Int,
    val rewardCoin: Int = 0,
    val rewardHeart: Int = 0,
)

object Missions {
    val POOL = listOf(
        MissionDef("m_feed3", "뽀뽀에게 밥 3번 주기", 3, rewardCoin = 100),
        MissionDef("m_bath1", "뽀뽀 씻겨주기 1번", 1, rewardCoin = 80),
        MissionDef("m_game2", "미니게임 2회 플레이", 2, rewardCoin = 150),
        MissionDef("m_play2", "뽀뽀와 2번 놀아주기", 2, rewardCoin = 120),
        MissionDef("m_dress1", "옷 갈아입히기 1번", 1, rewardHeart = 3),
        MissionDef("m_touch5", "뽀뽀 5번 쓰다듬기", 5, rewardCoin = 60),
    )
    fun byId(id: String) = POOL.first { it.id == id }
}

object Attendance {
    /** 1~7일차 보상 */
    val REWARDS = listOf(
        "코인 100" to StatChange(coin = 100),
        "기본 사료 3개" to StatChange(coin = 60),
        "코인 200" to StatChange(coin = 200),
        "하트 3" to StatChange(heart = 3),
        "코인 300" to StatChange(coin = 300),
        "딸기 간식" to StatChange(coin = 250),
        "하트 10 + 꼬깔 모자" to StatChange(heart = 10),
    )
}
