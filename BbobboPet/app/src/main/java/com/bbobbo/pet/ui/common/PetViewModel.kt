package com.bbobbo.pet.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bbobbo.pet.BbobboApp
import com.bbobbo.pet.data.local.CollectionEntity
import com.bbobbo.pet.data.local.DailyMissionEntity
import com.bbobbo.pet.data.local.InventoryEntity
import com.bbobbo.pet.data.local.OutfitSetEntity
import com.bbobbo.pet.data.local.PetStateEntity
import com.bbobbo.pet.data.local.RoomPlacementEntity
import com.bbobbo.pet.data.prefs.AppSettings
import com.bbobbo.pet.data.prefs.SettingsStore
import com.bbobbo.pet.data.repo.PetRepository
import com.bbobbo.pet.domain.Balance
import com.bbobbo.pet.domain.Collections
import com.bbobbo.pet.domain.Currency
import com.bbobbo.pet.domain.Food
import com.bbobbo.pet.domain.Furniture
import com.bbobbo.pet.domain.Furnitures
import com.bbobbo.pet.domain.NotifyKind
import com.bbobbo.pet.domain.OutfitBonus
import com.bbobbo.pet.domain.OutfitBonuses
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.PetMood
import com.bbobbo.pet.domain.RoomLayout
import com.bbobbo.pet.domain.Perks
import com.bbobbo.pet.domain.StatChange
import com.bbobbo.pet.domain.StatEngine
import com.bbobbo.pet.domain.WalkEvent
import com.bbobbo.pet.domain.Walks
import com.bbobbo.pet.domain.WearItem
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.domain.Wardrobe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class UiEvent(val message: String, val kind: Kind = Kind.INFO) {
    enum class Kind { INFO, REWARD, DENY, LEVEL_UP }
}

data class WelcomeBack(val hours: Float)

/** 산책 완료 결과 팝업용 (기획서 S-09) */
data class WalkResult(val event: WalkEvent, val total: StatChange)

/** 도감 수집률 마일스톤 달성 팝업용 (기획서 S-10) */
data class MilestoneResult(val percent: Int, val rewardLabel: String)

class PetViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: PetRepository = (app as BbobboApp).repository
    private val settingsStore = SettingsStore(app)

    private val _pet = MutableStateFlow(PetStateEntity())
    val pet: StateFlow<PetStateEntity> = _pet.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryEntity>>(emptyList())
    val inventory: StateFlow<List<InventoryEntity>> = _inventory.asStateFlow()

    private val _placements = MutableStateFlow<List<RoomPlacementEntity>>(emptyList())
    val placements: StateFlow<List<RoomPlacementEntity>> = _placements.asStateFlow()

    private val _missions = MutableStateFlow<List<DailyMissionEntity>>(emptyList())
    val missions: StateFlow<List<DailyMissionEntity>> = _missions.asStateFlow()

    private val _collection = MutableStateFlow<List<CollectionEntity>>(emptyList())
    val collection: StateFlow<List<CollectionEntity>> = _collection.asStateFlow()

    private val _outfitSets = MutableStateFlow<List<OutfitSetEntity>>(emptyList())
    val outfitSets: StateFlow<List<OutfitSetEntity>> = _outfitSets.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _anim = MutableStateFlow(PetAnim.IDLE)
    val anim: StateFlow<PetAnim> = _anim.asStateFlow()

    private val _event = MutableStateFlow<UiEvent?>(null)
    val event: StateFlow<UiEvent?> = _event.asStateFlow()

    private val _welcome = MutableStateFlow<WelcomeBack?>(null)
    val welcome: StateFlow<WelcomeBack?> = _welcome.asStateFlow()

    private val _attendanceDay = MutableStateFlow<Int?>(null)
    val attendanceDay: StateFlow<Int?> = _attendanceDay.asStateFlow()

    private val _walkResult = MutableStateFlow<WalkResult?>(null)
    val walkResult: StateFlow<WalkResult?> = _walkResult.asStateFlow()

    private val _milestone = MutableStateFlow<MilestoneResult?>(null)
    val milestone: StateFlow<MilestoneResult?> = _milestone.asStateFlow()

    /** 첫 프레임에서 스플래시/온보딩을 띄울지 판단하기 위해 로딩 완료 여부를 노출한다. */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        viewModelScope.launch {
            val loadedState = repo.getPet()
            val result = StatEngine.applyElapsed(loadedState, perks = perks())
            _pet.value = result.state
            repo.save(result.state)
            if (result.awayHours >= 0.5f) _welcome.value = WelcomeBack(result.awayHours)
            _missions.value = repo.ensureTodayMissions()
            if (result.state.onboarded && repo.todayAttendance() == null) {
                _attendanceDay.value = repo.checkIn().streakDay
            }
            grantRoomBonusIfDue()
            syncAnim()
            _loaded.value = true
        }
        viewModelScope.launch { repo.inventoryFlow.collect { _inventory.value = it } }
        viewModelScope.launch { repo.placementFlow.collect { _placements.value = it } }
        viewModelScope.launch { repo.missionFlow().collect { _missions.value = it } }
        viewModelScope.launch { repo.collectionFlow.collect { _collection.value = it } }
        viewModelScope.launch { repo.outfitFlow.collect { _outfitSets.value = it } }
        viewModelScope.launch { settingsStore.flow.collect { _settings.value = it } }
        // 1분 틱: 자연 감소 / 에너지 회복 / 산책 완료 확인
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                tick()
            }
        }
    }

    private suspend fun tick() {
        val result = StatEngine.applyElapsed(_pet.value, perks = perks())
        _pet.value = result.state
        repo.save(result.state)
        checkWalkFinished()
        syncAnim()
    }

    fun consumeEvent() { _event.value = null }
    fun consumeWelcome() { _welcome.value = null }
    fun consumeAttendance() { _attendanceDay.value = null }
    fun consumeWalkResult() { _walkResult.value = null }
    fun consumeMilestone() { _milestone.value = null }

    // ---- 착용 아이템 / 퍽 ----
    fun equippedId(slot: WearSlot): String {
        val ids = Wardrobe.bySlot(slot).map { it.id }
        return _inventory.value.firstOrNull { it.equipped && it.itemId in ids }?.itemId
            ?: Wardrobe.DEFAULTS.first { Wardrobe.byId(it)?.slot == slot }
    }

    fun owns(itemId: String): Boolean = _inventory.value.any { it.itemId == itemId && it.count > 0 }

    /** 착용 의상 + 배치 가구 + 세트 보너스를 합친 현재 퍽. */
    fun perks(): Perks = Perks.total(
        equippedIds = _inventory.value.filter { it.equipped }.map { it.itemId },
        placedIds = _placements.value.map { it.itemId },
        hatId = equippedId(WearSlot.HAT),
        clothId = equippedId(WearSlot.CLOTH),
        accId = equippedId(WearSlot.ACC),
    )

    /** 현재 착용 조합에 맞는 세트 보너스 (없으면 null) */
    fun activeSetBonus(): OutfitBonus? = OutfitBonuses.match(
        equippedId(WearSlot.HAT), equippedId(WearSlot.CLOTH), equippedId(WearSlot.ACC)
    )

    /** 방에 배치된 아이템의 cozy 합계와 등급. */
    fun roomCozy(): Int = _placements.value.sumOf { Furnitures.byId(it.itemId)?.cozy ?: 0 }
    fun roomGrade(): Pair<String, Int> = Furnitures.grade(roomCozy())

    private fun syncAnim() {
        val s = _pet.value
        _anim.value = when {
            isWalking() -> PetAnim.WALK
            else -> when (StatEngine.mood(s)) {
                PetMood.SLEEPING -> PetAnim.SLEEP
                PetMood.SAD -> PetAnim.SAD
                PetMood.HAPPY, PetMood.NORMAL -> PetAnim.IDLE
            }
        }
    }

    private fun playThen(a: PetAnim, durationMs: Long) {
        viewModelScope.launch {
            _anim.value = a
            delay(durationMs)
            syncAnim()
        }
    }

    private suspend fun commit(next: PetStateEntity, levelUps: Int = 0, msg: String? = null) {
        _pet.value = next
        repo.save(next)
        if (levelUps > 0) {
            _event.value = UiEvent("레벨 ${next.level} 달성! 코인 ${next.level * 50} 받았어요", UiEvent.Kind.LEVEL_UP)
            if (next.level >= 5) unlock("moment_level5")
            if (next.level >= 10) unlock("moment_level10")
        } else if (msg != null) {
            _event.value = UiEvent(msg, UiEvent.Kind.REWARD)
        }
        if (next.bond >= 90f) repo.progressMission("m_bond90")
        if (next.bond >= 100f) unlock("moment_bond100")
        syncAnim()
    }

    private fun deny(msg: String) { _event.value = UiEvent(msg, UiEvent.Kind.DENY) }

    private fun isSleeping(): Boolean {
        val end = _pet.value.sleepEndAt ?: return false
        return System.currentTimeMillis() < end
    }

    fun isWalking(): Boolean {
        val end = _pet.value.walkEndAt ?: return false
        return System.currentTimeMillis() < end
    }

    /** 산책 남은 시간(ms). 산책 중이 아니면 0. */
    fun walkRemainingMs(): Long =
        ((_pet.value.walkEndAt ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    // ---------- 도감 ----------

    private suspend fun unlock(entryId: String) {
        if (!repo.unlockCollection(entryId)) return
        checkMilestone()
    }

    fun collectionRatio(): Float =
        if (Collections.ALL.isEmpty()) 0f
        else _collection.value.size.toFloat() / Collections.ALL.size

    private suspend fun checkMilestone() {
        val percent = (collectionRatio() * 100).toInt()
        val s = _pet.value
        val next = Collections.MILESTONES.firstOrNull {
            it.first > s.collectionMilestone && percent >= it.first
        } ?: return
        val (state, ups) = StatEngine.apply(s.copy(collectionMilestone = next.first), next.third, perks())
        commit(state, ups)
        _milestone.value = MilestoneResult(next.first, next.second)
    }

    // ---------- 액션 ----------

    /** 캐릭터 탭: 점프 + 친밀도 소량 상승 (10분 쿨타임) */
    fun touch() {
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return }
        val now = System.currentTimeMillis()
        val s = _pet.value
        if (now - s.lastTouchAt < 10 * 60_000) {
            playThen(PetAnim.JUMP, 1200)
            return
        }
        viewModelScope.launch {
            val (next, ups) = StatEngine.apply(
                s.copy(lastTouchAt = now), StatChange(bond = 1f, mood = 1f), perks()
            )
            commit(next, ups)
            repo.progressMission("m_touch5")
            playThen(PetAnim.JUMP, 1400)
        }
    }

    /** 캐릭터 롱프레스: 쓰다듬기, 기분 +2 (1시간 쿨타임) */
    fun stroke() {
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return }
        val now = System.currentTimeMillis()
        val s = _pet.value
        if (now - s.lastPetAt < 60 * 60_000) {
            playThen(PetAnim.JUMP, 1000)
            return
        }
        viewModelScope.launch {
            val (next, ups) = StatEngine.apply(
                s.copy(lastPetAt = now), StatChange(mood = 2f), perks()
            )
            commit(next, ups, "쓰다듬기 · 기분 +2")
            playThen(PetAnim.JUMP, 1400)
        }
    }

    fun freeFoodLeft(): Int {
        val s = _pet.value
        val used = if (s.freeFoodUsedDate == PetRepository.today()) s.freeFoodUsedCount else 0
        val cap = Balance.FREE_FOOD_PER_DAY + perks().freeFoodBonus
        return (cap - used).coerceAtLeast(0)
    }

    fun feed(food: Food) {
        val s = _pet.value
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return }
        if (isWalking()) { deny("뽀뽀가 산책 중이에요"); return }
        if (s.fullness >= 100f) { deny("뽀뽀가 배불러요!"); return }
        if (food.currency == Currency.FREE && freeFoodLeft() <= 0) {
            deny("오늘 무료 사료를 다 썼어요"); return
        }
        if (!StatEngine.canAfford(s, food.currency, food.price)) {
            deny(if (food.currency == Currency.HEART) "하트가 부족해요" else "코인이 부족해요"); return
        }
        viewModelScope.launch {
            var base = StatEngine.pay(s, food.currency, food.price)
            if (food.currency == Currency.FREE) {
                val used = if (base.freeFoodUsedDate == PetRepository.today()) base.freeFoodUsedCount else 0
                base = base.copy(freeFoodUsedDate = PetRepository.today(), freeFoodUsedCount = used + 1)
            }
            val (next, ups) = StatEngine.apply(base.copy(totalFeed = base.totalFeed + 1), food.effect, perks())
            playThen(PetAnim.EAT, 2600)
            commit(next, ups, food.effect.summary())
            repo.progressMission("m_feed3")
            unlock(food.id)
            unlock("moment_first_feed")
        }
    }

    fun bath(quality: Float) {
        val s = _pet.value
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return }
        viewModelScope.launch {
            val effect = StatChange(clean = 40f * quality, mood = 10f * quality, bond = 2f, exp = 10)
            val (next, ups) = StatEngine.apply(s.copy(totalBath = s.totalBath + 1), effect, perks())
            commit(next, ups, effect.summary())
            repo.progressMission("m_bath1")
            unlock("moment_first_bath")
            playThen(PetAnim.JUMP, 1400)
        }
    }

    fun play(kind: String, score: Float) {
        val s = _pet.value
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return }
        if (s.energy < 5f) { deny("에너지가 부족해요"); return }
        viewModelScope.launch {
            val effect = StatChange(
                mood = 20f * score, bond = 3f, energy = -5f, exp = 12,
                coin = (20 + 30 * score).toLong()
            )
            val (next, ups) = StatEngine.apply(s.copy(totalPlay = s.totalPlay + 1), effect, perks())
            commit(next, ups, effect.summary())
            repo.progressMission("m_play2")
            playThen(PetAnim.WALK, 1600)
        }
    }

    fun sleep(hours: Float) {
        val s = _pet.value
        if (isWalking()) { deny("뽀뽀가 산책 중이에요"); return }
        val end = System.currentTimeMillis() + (hours * 3_600_000).toLong()
        viewModelScope.launch {
            val next = s.copy(sleepEndAt = end)
            _pet.value = next
            repo.save(next)
            _anim.value = PetAnim.SLEEP
            unlock("moment_first_sleep")
        }
    }

    fun wakeUp() {
        viewModelScope.launch {
            val result = StatEngine.applyElapsed(_pet.value, perks = perks())
            val awake = result.state.copy(sleepEndAt = null)
            val (next, ups) = StatEngine.apply(awake, StatChange(mood = 10f, exp = 20), perks())
            commit(next, ups, "잘 잤어요! 기분 +10 · EXP +20")
            playThen(PetAnim.JUMP, 1400)
        }
    }

    // ---------- S-09 산책 ----------

    fun walksLeftToday(): Int {
        val s = _pet.value
        val used = if (s.walkCountDate == PetRepository.today()) s.walkCount else 0
        return (Walks.DAILY_LIMIT - used).coerceAtLeast(0)
    }

    /** 산책 시작. 성공하면 true. 진행은 walkEndAt 기준이라 앱을 나가도 이어진다. */
    fun startWalk(): Boolean {
        val s = _pet.value
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return false }
        if (isWalking()) { deny("이미 산책 중이에요"); return false }
        if (walksLeftToday() <= 0) { deny("오늘 산책은 다 했어요"); return false }
        if (s.energy < Walks.ENERGY_COST) { deny("에너지가 부족해요"); return false }

        val used = if (s.walkCountDate == PetRepository.today()) s.walkCount else 0
        viewModelScope.launch {
            val next = s.copy(
                walkEndAt = System.currentTimeMillis() + Walks.DURATION_MS,
                walkCountDate = PetRepository.today(),
                walkCount = used + 1,
            )
            _pet.value = next
            repo.save(next)
            _anim.value = PetAnim.WALK
        }
        return true
    }

    /** 산책 시간이 끝났으면 보상을 정산한다. 틱과 화면 양쪽에서 안전하게 여러 번 불러도 된다. */
    fun checkWalkFinished() {
        val s = _pet.value
        val end = s.walkEndAt ?: return
        if (System.currentTimeMillis() < end) return
        viewModelScope.launch {
            // 먼저 walkEndAt 을 비워 중복 정산을 막는다.
            val cleared = s.copy(walkEndAt = null)
            _pet.value = cleared
            repo.save(cleared)

            val event = Walks.pick(Random.nextFloat())
            val total = Walks.BASE_REWARD + event.effect
            val (next, ups) = StatEngine.apply(cleared.copy(totalWalk = cleared.totalWalk + 1), total, perks())
            commit(next, ups)
            repo.progressMission("m_walk1")
            event.collectionId?.let { unlock(it) }
            _walkResult.value = WalkResult(event, total)
            playThen(PetAnim.JUMP, 1400)
        }
    }

    // ---------- 미니게임 ----------

    fun finishMiniGame(score: Int, maxCombo: Int = 0) {
        viewModelScope.launch {
            val reward = StatEngine.gameReward(score)
            val s = _pet.value
            val next0 = s.copy(
                bestGameScore = maxOf(s.bestGameScore, score),
                bestGameCombo = maxOf(s.bestGameCombo, maxCombo),
                totalGame = s.totalGame + 1,
            )
            val (next, ups) = StatEngine.apply(next0, reward, perks())
            commit(next, ups, reward.summary())
            repo.progressMission("m_game2")
            unlock("moment_first_game")
            if (maxCombo >= 30) unlock("moment_combo30")
        }
    }

    fun spendEnergyForGame(): Boolean {
        val s = _pet.value
        if (s.energy < 10f) { deny("에너지가 부족해요. 뽀뽀를 재워줄까요?"); return false }
        viewModelScope.launch {
            val next = s.copy(energy = s.energy - 10f)
            _pet.value = next
            repo.save(next)
        }
        return true
    }

    /** 기획서 §1-4: 하트 10개로 에너지 즉시 완충 (광고 보상은 T-43 에서 대체) */
    fun refillEnergyWithHearts(): Boolean {
        val s = _pet.value
        if (s.energy >= 100f) { deny("에너지가 이미 가득해요"); return false }
        if (s.heart < ENERGY_REFILL_HEARTS) { deny("하트가 부족해요"); return false }
        viewModelScope.launch {
            val next = s.copy(heart = s.heart - ENERGY_REFILL_HEARTS, energy = 100f)
            commit(next, 0, "에너지를 가득 채웠어요!")
        }
        return true
    }

    // ---------- 옷장 / 방꾸미기 ----------

    fun buyAndEquip(item: WearItem) {
        val s = _pet.value
        if (s.level < item.unlockLevel) { deny("Lv.${item.unlockLevel} 부터 입을 수 있어요"); return }
        viewModelScope.launch {
            if (!owns(item.id)) {
                if (!StatEngine.canAfford(s, item.currency, item.price)) {
                    deny(if (item.currency == Currency.HEART) "하트가 부족해요" else "코인이 부족해요")
                    return@launch
                }
                val paid = StatEngine.pay(s, item.currency, item.price)
                _pet.value = paid
                repo.save(paid)
                repo.addItem(item.id)
            }
            repo.equip(item.id, Wardrobe.bySlot(item.slot).map { it.id })
            repo.progressMission("m_dress1")
            unlock(item.id)
            _event.value = UiEvent("${item.name} 착용 완료", UiEvent.Kind.REWARD)
            playThen(PetAnim.JUMP, 1300)
        }
    }

    /** 현재 착용 조합을 슬롯에 저장 (기획서 T-28) */
    fun saveOutfitSet(slot: Int, name: String) {
        viewModelScope.launch {
            repo.saveOutfitSet(
                slot = slot,
                name = name.ifBlank { "코디 $slot" }.take(10),
                hatId = equippedId(WearSlot.HAT),
                clothId = equippedId(WearSlot.CLOTH),
                accId = equippedId(WearSlot.ACC),
            )
            _event.value = UiEvent("코디를 저장했어요", UiEvent.Kind.REWARD)
        }
    }

    /** 저장된 슬롯을 원터치로 적용. 보유하지 않은 아이템이 있으면 거절한다. */
    fun applyOutfitSet(set: OutfitSetEntity) {
        val missing = listOf(set.hatId, set.clothId, set.accId)
            .filter { Wardrobe.byId(it) != null && !owns(it) }
        if (missing.isNotEmpty()) { deny("아직 없는 아이템이 있어요"); return }
        viewModelScope.launch {
            listOf(set.hatId, set.clothId, set.accId).forEach { id ->
                Wardrobe.byId(id)?.let { repo.equip(it.id, Wardrobe.bySlot(it.slot).map { s -> s.id }) }
            }
            _event.value = UiEvent("${set.setName} 적용!", UiEvent.Kind.REWARD)
            playThen(PetAnim.JUMP, 1300)
        }
    }

    fun deleteOutfitSet(slot: Int) {
        viewModelScope.launch { repo.deleteOutfitSet(slot) }
    }

    fun buyFurniture(f: Furniture) {
        val s = _pet.value
        if (owns(f.id)) { deny("이미 가지고 있어요"); return }
        if (!StatEngine.canAfford(s, f.currency, f.price)) {
            deny(if (f.currency == Currency.HEART) "하트가 부족해요" else "코인이 부족해요"); return
        }
        viewModelScope.launch {
            val paid = StatEngine.pay(s, f.currency, f.price)
            _pet.value = paid
            repo.save(paid)
            repo.addItem(f.id)
            unlock(f.id)
            _event.value = UiEvent("${f.name} 구매 완료", UiEvent.Kind.REWARD)
        }
    }

    /**
     * 배치 되돌리기용 스냅샷 스택. 화면을 벗어나면 사라지는 편집용 상태라
     * DB 가 아니라 메모리에만 둔다 (기획서 T-31 Undo).
     */
    private val undoStack = ArrayDeque<List<RoomPlacementEntity>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private fun pushUndo() {
        undoStack.addLast(_placements.value)
        if (undoStack.size > UNDO_DEPTH) undoStack.removeFirst()
        _canUndo.value = true
    }

    /** 해당 카테고리의 배치 슬롯이 남아 있는지 (기획서 S-08). */
    fun placedCount(category: com.bbobbo.pet.domain.FurnitureCategory): Int =
        _placements.value.count { Furnitures.byId(it.itemId)?.category == category }

    fun placeFurniture(itemId: String, x: Float, y: Float) {
        val f = Furnitures.byId(itemId) ?: return
        val limit = RoomLayout.limitFor(f.category)
        if (limit != null && placedCount(f.category) >= limit) {
            deny("${f.category.label}는 최대 ${limit}개까지 놓을 수 있어요")
            return
        }
        pushUndo()
        viewModelScope.launch {
            repo.place(itemId, RoomLayout.snap(x), RoomLayout.snap(y), _placements.value.size)
        }
    }

    /** 드래그 이동. 좌표는 격자에 스냅되고 방 밖으로는 나가지 않는다. */
    fun movePlacement(p: RoomPlacementEntity, x: Float, y: Float) {
        val nx = RoomLayout.snap(x)
        val ny = RoomLayout.snap(y)
        if (nx == p.x && ny == p.y) return
        viewModelScope.launch { repo.movePlacement(p.copy(x = nx, y = ny)) }
    }

    /** 드래그를 시작할 때 한 번 불러 되돌릴 지점을 남긴다. */
    fun beginPlacementDrag() = pushUndo()

    fun removePlacement(id: Long) {
        pushUndo()
        viewModelScope.launch { repo.removePlacement(id) }
    }

    fun clearPlacements() {
        if (_placements.value.isEmpty()) return
        pushUndo()
        viewModelScope.launch { repo.clearPlacements() }
    }

    fun undoPlacement() {
        val previous = undoStack.removeLastOrNull() ?: return
        _canUndo.value = undoStack.isNotEmpty()
        viewModelScope.launch { repo.replacePlacements(previous) }
    }

    /** 방 등급에 따른 일일 코인 보너스 (기획서 T-32). 하루 한 번만 지급된다. */
    private suspend fun grantRoomBonusIfDue() {
        val s = _pet.value
        val today = PetRepository.today()
        if (s.roomBonusDate == today) return
        val (grade, bonus) = roomGrade()
        val stamped = s.copy(roomBonusDate = today)
        if (bonus <= 0) {
            _pet.value = stamped
            repo.save(stamped)
            return
        }
        val (next, ups) = StatEngine.apply(stamped, StatChange(coin = bonus.toLong()), perks())
        commit(next, ups, "방 등급 $grade · 코인 +$bonus")
    }

    // ---------- 미션 / 출석 ----------

    fun claimMission(missionId: String) {
        viewModelScope.launch {
            val claimed = repo.claimMission(missionId) ?: return@launch
            val (next, ups) = StatEngine.apply(
                _pet.value,
                StatChange(coin = claimed.rewardCoin.toLong(), heart = claimed.rewardHeart),
                perks(),
            )
            commit(next, ups, "미션 보상을 받았어요")
            _missions.value = repo.ensureTodayMissions()
        }
    }

    fun claimAttendance(day: Int) {
        viewModelScope.launch {
            val reward = com.bbobbo.pet.domain.Attendance.REWARDS[(day - 1).coerceIn(0, 6)].second
            val (next, ups) = StatEngine.apply(_pet.value, reward, perks())
            commit(next, ups, "출석 보상을 받았어요")
            if (day == 7) repo.addItem("hat_party")
        }
    }

    /** 출석 화면에서 수동으로 오늘 출석을 여는 경로. */
    fun openAttendance() {
        viewModelScope.launch {
            if (repo.todayAttendance() == null) _attendanceDay.value = repo.checkIn().streakDay
        }
    }

    suspend fun todayStreak(): Int = repo.todayAttendance()?.streakDay ?: 0

    // ---------- 온보딩 / 설정 ----------

    fun rename(newName: String) {
        viewModelScope.launch {
            val next = _pet.value.copy(name = newName.ifBlank { "뽀뽀" }.take(6))
            _pet.value = next
            repo.save(next)
        }
    }

    /** 온보딩 완료: 이름 확정 + 튜토리얼 보상 + 출석 팝업 개시 (기획서 T-42) */
    fun finishOnboarding(name: String) {
        viewModelScope.launch {
            val named = _pet.value.copy(
                name = name.ifBlank { "뽀뽀" }.take(6),
                onboarded = true,
            )
            val (next, ups) = StatEngine.apply(named, StatChange(coin = 500), perks())
            commit(next, ups, "환영 선물 코인 500 · 잘 부탁해요!")
            if (repo.todayAttendance() == null) _attendanceDay.value = repo.checkIn().streakDay
        }
    }

    fun setSound(on: Boolean) { viewModelScope.launch { settingsStore.setSound(on) } }
    fun setVibration(on: Boolean) { viewModelScope.launch { settingsStore.setVibration(on) } }
    fun setBgmVolume(v: Float) { viewModelScope.launch { settingsStore.setBgmVolume(v) } }
    fun setNotify(kind: NotifyKind, on: Boolean) {
        viewModelScope.launch { settingsStore.setNotify(kind, on) }
    }

    /** S-12 데이터 초기화. 호출 전에 화면에서 2단계 확인을 받는다. */
    fun wipeAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.wipe()
            settingsStore.clear()
            val fresh = repo.getPet()
            _pet.value = fresh
            _missions.value = repo.ensureTodayMissions()
            _welcome.value = null
            _attendanceDay.value = null
            syncAnim()
            onDone()
        }
    }

    companion object {
        const val ENERGY_REFILL_HEARTS = 10
        private const val UNDO_DEPTH = 20
    }
}
