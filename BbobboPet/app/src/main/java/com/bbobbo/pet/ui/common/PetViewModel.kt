package com.bbobbo.pet.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bbobbo.pet.BbobboApp
import com.bbobbo.pet.data.local.DailyMissionEntity
import com.bbobbo.pet.data.local.InventoryEntity
import com.bbobbo.pet.data.local.PetStateEntity
import com.bbobbo.pet.data.local.RoomPlacementEntity
import com.bbobbo.pet.data.repo.PetRepository
import com.bbobbo.pet.domain.Balance
import com.bbobbo.pet.domain.Currency
import com.bbobbo.pet.domain.Food
import com.bbobbo.pet.domain.Foods
import com.bbobbo.pet.domain.Furniture
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.PetMood
import com.bbobbo.pet.domain.StatChange
import com.bbobbo.pet.domain.StatEngine
import com.bbobbo.pet.domain.WearItem
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.domain.Wardrobe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiEvent(val message: String, val kind: Kind = Kind.INFO) {
    enum class Kind { INFO, REWARD, DENY, LEVEL_UP }
}

data class WelcomeBack(val hours: Float)

class PetViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: PetRepository = (app as BbobboApp).repository

    private val _pet = MutableStateFlow(PetStateEntity())
    val pet: StateFlow<PetStateEntity> = _pet.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryEntity>>(emptyList())
    val inventory: StateFlow<List<InventoryEntity>> = _inventory.asStateFlow()

    private val _placements = MutableStateFlow<List<RoomPlacementEntity>>(emptyList())
    val placements: StateFlow<List<RoomPlacementEntity>> = _placements.asStateFlow()

    private val _missions = MutableStateFlow<List<DailyMissionEntity>>(emptyList())
    val missions: StateFlow<List<DailyMissionEntity>> = _missions.asStateFlow()

    private val _anim = MutableStateFlow(PetAnim.IDLE)
    val anim: StateFlow<PetAnim> = _anim.asStateFlow()

    private val _event = MutableStateFlow<UiEvent?>(null)
    val event: StateFlow<UiEvent?> = _event.asStateFlow()

    private val _welcome = MutableStateFlow<WelcomeBack?>(null)
    val welcome: StateFlow<WelcomeBack?> = _welcome.asStateFlow()

    private val _attendanceDay = MutableStateFlow<Int?>(null)
    val attendanceDay: StateFlow<Int?> = _attendanceDay.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = repo.getPet()
            val result = StatEngine.applyElapsed(loaded)
            _pet.value = result.state
            repo.save(result.state)
            if (result.awayHours >= 0.5f) _welcome.value = WelcomeBack(result.awayHours)
            _missions.value = repo.ensureTodayMissions()
            if (repo.todayAttendance() == null) {
                _attendanceDay.value = repo.checkIn().streakDay
            }
            syncAnim()
        }
        viewModelScope.launch { repo.inventoryFlow.collect { _inventory.value = it } }
        viewModelScope.launch { repo.placementFlow.collect { _placements.value = it } }
        viewModelScope.launch { repo.missionFlow().collect { _missions.value = it } }
        // 1분 틱: 자연 감소 / 에너지 회복 반영
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                tick()
            }
        }
    }

    private suspend fun tick() {
        val result = StatEngine.applyElapsed(_pet.value)
        _pet.value = result.state
        repo.save(result.state)
        syncAnim()
    }

    fun consumeEvent() { _event.value = null }
    fun consumeWelcome() { _welcome.value = null }
    fun consumeAttendance() { _attendanceDay.value = null }

    // ---- 착용 아이템 조회 ----
    fun equippedId(slot: WearSlot): String {
        val ids = Wardrobe.bySlot(slot).map { it.id }
        return _inventory.value.firstOrNull { it.equipped && it.itemId in ids }?.itemId
            ?: Wardrobe.DEFAULTS.first { Wardrobe.byId(it)?.slot == slot }
    }

    fun owns(itemId: String): Boolean = _inventory.value.any { it.itemId == itemId && it.count > 0 }

    private fun syncAnim() {
        val s = _pet.value
        _anim.value = when (StatEngine.mood(s)) {
            PetMood.SLEEPING -> PetAnim.SLEEP
            PetMood.SAD -> PetAnim.SAD
            PetMood.HAPPY -> PetAnim.IDLE
            PetMood.NORMAL -> PetAnim.IDLE
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
        } else if (msg != null) {
            _event.value = UiEvent(msg, UiEvent.Kind.REWARD)
        }
        syncAnim()
    }

    private fun deny(msg: String) { _event.value = UiEvent(msg, UiEvent.Kind.DENY) }

    private fun isSleeping(): Boolean {
        val end = _pet.value.sleepEndAt ?: return false
        return System.currentTimeMillis() < end
    }

    // ---------- 액션 ----------

    fun touch() {
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return }
        val now = System.currentTimeMillis()
        val s = _pet.value
        if (now - s.lastTouchAt < 10 * 60_000) {
            playThen(PetAnim.JUMP, 1200)
            return
        }
        viewModelScope.launch {
            val (next, ups) = StatEngine.apply(s.copy(lastTouchAt = now), StatChange(bond = 1f, mood = 1f))
            commit(next, ups)
            repo.progressMission("m_touch5")
            playThen(PetAnim.JUMP, 1400)
        }
    }

    fun freeFoodLeft(): Int {
        val s = _pet.value
        val used = if (s.freeFoodUsedDate == PetRepository.today()) s.freeFoodUsedCount else 0
        return (Balance.FREE_FOOD_PER_DAY - used).coerceAtLeast(0)
    }

    fun feed(food: Food) {
        val s = _pet.value
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return }
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
            val (next, ups) = StatEngine.apply(base, food.effect)
            playThen(PetAnim.EAT, 2600)
            commit(next, ups, food.effect.summary())
            repo.progressMission("m_feed3")
        }
    }

    fun bath(quality: Float) {
        val s = _pet.value
        if (isSleeping()) { deny("뽀뽀가 자고 있어요"); return }
        viewModelScope.launch {
            val effect = StatChange(
                clean = 40f * quality, mood = 10f * quality, bond = 2f, exp = 10
            )
            val (next, ups) = StatEngine.apply(s, effect)
            commit(next, ups, effect.summary())
            repo.progressMission("m_bath1")
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
            val (next, ups) = StatEngine.apply(s, effect)
            commit(next, ups, effect.summary())
            repo.progressMission("m_play2")
            playThen(PetAnim.WALK, 1600)
        }
    }

    fun sleep(hours: Float) {
        val s = _pet.value
        val end = System.currentTimeMillis() + (hours * 3_600_000).toLong()
        viewModelScope.launch {
            val next = s.copy(sleepEndAt = end)
            _pet.value = next
            repo.save(next)
            _anim.value = PetAnim.SLEEP
        }
    }

    fun wakeUp() {
        viewModelScope.launch {
            val result = StatEngine.applyElapsed(_pet.value)
            val awake = result.state.copy(sleepEndAt = null)
            val (next, ups) = StatEngine.apply(awake, StatChange(mood = 10f, exp = 20))
            commit(next, ups, "잘 잤어요! 기분 +10 · EXP +20")
            playThen(PetAnim.JUMP, 1400)
        }
    }

    fun finishMiniGame(score: Int) {
        viewModelScope.launch {
            val reward = StatEngine.gameReward(score)
            val s = _pet.value
            val best = maxOf(s.bestGameScore, score)
            val (next, ups) = StatEngine.apply(s.copy(bestGameScore = best), reward)
            commit(next, ups, reward.summary())
            repo.progressMission("m_game2")
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
            _event.value = UiEvent("${item.name} 착용 완료", UiEvent.Kind.REWARD)
            playThen(PetAnim.JUMP, 1300)
        }
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
            _event.value = UiEvent("${f.name} 구매 완료", UiEvent.Kind.REWARD)
        }
    }

    fun placeFurniture(itemId: String, x: Float, y: Float) {
        viewModelScope.launch { repo.place(itemId, x, y, _placements.value.size) }
    }

    fun movePlacement(p: RoomPlacementEntity, x: Float, y: Float) {
        viewModelScope.launch { repo.movePlacement(p.copy(x = x, y = y)) }
    }

    fun removePlacement(id: Long) {
        viewModelScope.launch { repo.removePlacement(id) }
    }

    fun claimMission(missionId: String) {
        viewModelScope.launch {
            val claimed = repo.claimMission(missionId) ?: return@launch
            val (next, ups) = StatEngine.apply(
                _pet.value,
                StatChange(coin = claimed.rewardCoin.toLong(), heart = claimed.rewardHeart)
            )
            commit(next, ups, "미션 보상을 받았어요")
            _missions.value = repo.ensureTodayMissions()
        }
    }

    fun claimAttendance(day: Int) {
        viewModelScope.launch {
            val reward = com.bbobbo.pet.domain.Attendance.REWARDS[(day - 1).coerceIn(0, 6)].second
            val (next, ups) = StatEngine.apply(_pet.value, reward)
            commit(next, ups, "출석 보상을 받았어요")
            if (day == 7) repo.addItem("hat_party")
        }
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            val next = _pet.value.copy(name = newName.ifBlank { "뽀뽀" }.take(6))
            _pet.value = next
            repo.save(next)
        }
    }
}
