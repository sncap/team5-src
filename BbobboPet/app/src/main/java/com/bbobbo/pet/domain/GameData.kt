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

    /** 산책 기본 보상 + 이벤트 보상처럼 두 변화를 합칠 때 쓴다. */
    operator fun plus(other: StatChange) = StatChange(
        fullness = fullness + other.fullness,
        mood = mood + other.mood,
        clean = clean + other.clean,
        energy = energy + other.energy,
        bond = bond + other.bond,
        exp = exp + other.exp,
        coin = coin + other.coin,
        heart = heart + other.heart,
    )

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

enum class WearSlot(val label: String, val emoji: String) {
    HAT("모자", "🎩"), CLOTH("옷", "👕"), ACC("액세서리", "🎀")
}

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
        MissionDef("m_walk1", "산책 1번 다녀오기", 1, rewardCoin = 120),
        MissionDef("m_bond90", "친밀도 90 이상 만들기", 1, rewardHeart = 5),
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

// ---------------------------------------------------------------------------
// S-09 산책
// ---------------------------------------------------------------------------

/** 산책 완료 시 1개가 뽑히는 랜덤 이벤트. weight 가 클수록 자주 나온다. */
data class WalkEvent(
    val id: String,
    val title: String,
    val emoji: String,
    val weight: Int,
    val effect: StatChange,
    /** 이 이벤트로 해금되는 도감 항목 (없으면 null) */
    val collectionId: String? = null,
)

object Walks {
    /** 소모 에너지 / 진행 시간 / 일일 횟수 제한 (기획서 S-09) */
    const val ENERGY_COST = 15f
    const val DURATION_MS = 3 * 60_000L
    const val DAILY_LIMIT = 3

    /** 산책 자체의 고정 보상. 랜덤 이벤트 보상은 여기에 더해진다. */
    val BASE_REWARD = StatChange(mood = 15f, energy = -ENERGY_COST, exp = 15)

    val EVENTS = listOf(
        WalkEvent(
            "w_acorn", "도토리를 주웠어요", "🌰", 30,
            StatChange(coin = 150), "friend_acorn"
        ),
        WalkEvent(
            "w_butterfly", "나비를 만났어요", "🦋", 25,
            StatChange(mood = 15f), "friend_butterfly"
        ),
        WalkEvent(
            "w_friend", "친구를 만났어요", "🐰", 20,
            StatChange(bond = 5f, heart = 1), "friend_rabbit"
        ),
        WalkEvent(
            "w_rain", "비가 왔어요", "🌧️", 15,
            StatChange(clean = -15f, mood = 5f), "friend_cloud"
        ),
        WalkEvent(
            "w_flower", "꽃밭을 지나왔어요", "🌷", 10,
            StatChange(mood = 10f, coin = 80), "friend_flower"
        ),
    )

    /** weight 기반 가중 추첨. [roll] 은 0f..1f 범위의 난수. */
    fun pick(roll: Float): WalkEvent {
        val total = EVENTS.sumOf { it.weight }
        var cursor = (roll.coerceIn(0f, 0.999f) * total)
        for (e in EVENTS) {
            cursor -= e.weight
            if (cursor < 0f) return e
        }
        return EVENTS.last()
    }
}

// ---------------------------------------------------------------------------
// S-10 도감
// ---------------------------------------------------------------------------

enum class CollectionTab(val label: String) {
    FOOD("음식"), CLOTH("의상"), FURNITURE("가구"), FRIEND("친구"), MOMENT("순간")
}

data class CollectionEntry(
    val id: String,
    val tab: CollectionTab,
    val name: String,
    val emoji: String,
    val desc: String,
)

object Collections {
    /** 음식·의상·가구는 각 마스터 테이블에서 자동 생성하고, 친구/순간만 직접 정의한다. */
    private val FRIENDS = listOf(
        CollectionEntry("friend_acorn", CollectionTab.FRIEND, "도토리", "🌰", "산책길에 주운 반질반질한 도토리"),
        CollectionEntry("friend_butterfly", CollectionTab.FRIEND, "나비", "🦋", "뽀뽀 코에 살짝 앉았다 날아갔어요"),
        CollectionEntry("friend_rabbit", CollectionTab.FRIEND, "토끼 친구", "🐰", "산책길에서 만난 이웃집 토끼"),
        CollectionEntry("friend_cloud", CollectionTab.FRIEND, "비구름", "🌧️", "갑자기 쏟아진 소나기"),
        CollectionEntry("friend_flower", CollectionTab.FRIEND, "꽃밭", "🌷", "봄이 한가득 피어난 자리"),
    )

    private val MOMENTS = listOf(
        CollectionEntry("moment_first_feed", CollectionTab.MOMENT, "첫 식사", "🥣", "뽀뽀에게 처음 밥을 준 날"),
        CollectionEntry("moment_first_bath", CollectionTab.MOMENT, "첫 목욕", "🛁", "뽀득뽀득 처음 씻긴 날"),
        CollectionEntry("moment_first_sleep", CollectionTab.MOMENT, "첫 잠", "🌙", "쿨쿨 잠든 모습을 처음 본 날"),
        CollectionEntry("moment_first_game", CollectionTab.MOMENT, "첫 미니게임", "🎮", "간식 잡기를 처음 해본 날"),
        CollectionEntry("moment_level5", CollectionTab.MOMENT, "쑥쑥 뽀뽀", "🌱", "Lv.5 를 달성한 날"),
        CollectionEntry("moment_level10", CollectionTab.MOMENT, "행복한 뽀뽀", "✨", "Lv.10 을 달성한 날"),
        CollectionEntry("moment_bond100", CollectionTab.MOMENT, "단짝", "💗", "친밀도가 가득 찬 날"),
        CollectionEntry("moment_combo30", CollectionTab.MOMENT, "콤보 마스터", "🔥", "미니게임에서 30콤보를 달성한 날"),
    )

    val ALL: List<CollectionEntry> = buildList {
        Foods.ALL.forEach {
            add(CollectionEntry(it.id, CollectionTab.FOOD, it.name, it.emoji, it.desc))
        }
        Wardrobe.ALL.forEach {
            add(CollectionEntry(it.id, CollectionTab.CLOTH, it.name, it.slot.emoji, it.perk ?: "뽀뽀의 ${it.slot.label}"))
        }
        Furnitures.ALL.forEach {
            add(CollectionEntry(it.id, CollectionTab.FURNITURE, it.name, it.emoji, it.perk ?: "아늑함 +${it.cozy}"))
        }
        addAll(FRIENDS)
        addAll(MOMENTS)
    }

    fun byTab(tab: CollectionTab) = ALL.filter { it.tab == tab }
    fun byId(id: String) = ALL.firstOrNull { it.id == id }

    /** 수집률 마일스톤 보상 (기획서 S-10) */
    val MILESTONES = listOf(
        Triple(25, "하트 10", StatChange(heart = 10)),
        Triple(50, "하트 30", StatChange(heart = 30)),
        Triple(75, "하트 50", StatChange(heart = 50)),
        Triple(100, "하트 100", StatChange(heart = 100)),
    )
}

// ---------------------------------------------------------------------------
// T-29 세트 보너스
// ---------------------------------------------------------------------------

/** 지정 조합을 전부 착용하면 붙는 보너스. */
data class OutfitBonus(
    val id: String,
    val name: String,
    val hatId: String,
    val clothId: String,
    val accId: String,
    val desc: String,
    val moodPerHour: Float,
    val coinBonusPercent: Int,
)

object OutfitBonuses {
    val ALL = listOf(
        OutfitBonus(
            "set_bear", "곰돌이 세트", "hat_bear", "cloth_apron", "acc_ribbon",
            "기분 +5 · 코인 획득 +10%", moodPerHour = 5f, coinBonusPercent = 10
        ),
        OutfitBonus(
            "set_star", "별밤 세트", "hat_crown", "cloth_cape", "acc_bell",
            "기분 +5 · 코인 획득 +15%", moodPerHour = 5f, coinBonusPercent = 15
        ),
        OutfitBonus(
            "set_picnic", "소풍 세트", "hat_beret", "cloth_scarf", "acc_glasses",
            "기분 +3 · 코인 획득 +5%", moodPerHour = 3f, coinBonusPercent = 5
        ),
    )

    fun match(hatId: String, clothId: String, accId: String): OutfitBonus? =
        ALL.firstOrNull { it.hatId == hatId && it.clothId == clothId && it.accId == accId }

    /** 코디 세트 저장 슬롯 개수 (기획서 S-07) */
    const val SLOT_COUNT = 5
}

// ---------------------------------------------------------------------------
// T-39 알림
// ---------------------------------------------------------------------------

enum class NotifyKind(val key: String, val label: String, val desc: String) {
    HUNGRY("notify_hungry", "배고픔 알림", "배고픔이 30 이하로 떨어지면 알려드려요"),
    DIRTY("notify_dirty", "청결 알림", "청결이 30 이하로 떨어지면 알려드려요"),
    ATTENDANCE("notify_attendance", "출석 알림", "매일 저녁 8시, 출석을 안 했으면 알려드려요"),
    ENERGY("notify_energy", "에너지 알림", "에너지가 가득 차면 알려드려요"),
}

// ---------------------------------------------------------------------------
// S-08 방 배치 규칙
// ---------------------------------------------------------------------------

object RoomLayout {
    /** 바닥 타일 기준 그리드 칸 수. 배치 좌표는 이 격자에 스냅된다. */
    const val GRID = 12

    /** 배치 슬롯 제한 (기획서 S-08) */
    const val MAX_FURNITURE = 12
    const val MAX_PROP = 20

    /** 0f..1f 좌표를 격자에 맞추고 화면 밖으로 나가지 않게 가둔다. */
    fun snap(v: Float): Float =
        (Math.round(v * GRID).toFloat() / GRID).coerceIn(1f / GRID, 1f - 1f / GRID)

    /** 해당 카테고리를 더 놓을 수 있는지. 벽지·바닥은 배치 대상이 아니다. */
    fun limitFor(category: FurnitureCategory): Int? = when (category) {
        FurnitureCategory.FURNITURE -> MAX_FURNITURE
        FurnitureCategory.PROP -> MAX_PROP
        else -> null
    }
}
