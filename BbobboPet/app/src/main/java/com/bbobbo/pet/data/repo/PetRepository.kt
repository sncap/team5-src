package com.bbobbo.pet.data.repo

import com.bbobbo.pet.data.local.AppDatabase
import com.bbobbo.pet.data.local.AttendanceEntity
import com.bbobbo.pet.data.local.CollectionEntity
import com.bbobbo.pet.data.local.OutfitSetEntity
import com.bbobbo.pet.data.local.DailyMissionEntity
import com.bbobbo.pet.data.local.InventoryEntity
import com.bbobbo.pet.data.local.PetStateEntity
import com.bbobbo.pet.data.local.RoomPlacementEntity
import com.bbobbo.pet.domain.Missions
import com.bbobbo.pet.domain.Wardrobe
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PetRepository(private val db: AppDatabase) {

    val petFlow: Flow<PetStateEntity?> = db.petDao().observe()
    val inventoryFlow: Flow<List<InventoryEntity>> = db.inventoryDao().observeAll()
    val placementFlow: Flow<List<RoomPlacementEntity>> = db.roomPlacementDao().observeAll()
    val collectionFlow: Flow<List<CollectionEntity>> = db.collectionDao().observeAll()
    val outfitFlow: Flow<List<OutfitSetEntity>> = db.outfitDao().observeAll()

    fun missionFlow(): Flow<List<DailyMissionEntity>> = db.missionDao().observeByDate(today())

    suspend fun getPet(): PetStateEntity = db.petDao().get() ?: PetStateEntity().also {
        db.petDao().upsert(it)
        Wardrobe.DEFAULTS.forEach { id ->
            db.inventoryDao().upsert(InventoryEntity(itemId = id, equipped = true))
        }
    }

    suspend fun save(state: PetStateEntity) = db.petDao().upsert(state)

    // ---- 인벤토리 ----
    suspend fun addItem(itemId: String, count: Int = 1) {
        val cur = db.inventoryDao().get(itemId)
        db.inventoryDao().upsert(
            cur?.copy(count = cur.count + count) ?: InventoryEntity(itemId, count)
        )
    }

    suspend fun hasItem(itemId: String): Boolean = (db.inventoryDao().get(itemId)?.count ?: 0) > 0

    suspend fun equip(itemId: String, siblingIds: List<String>) {
        db.inventoryDao().unequipAll(siblingIds)
        val cur = db.inventoryDao().get(itemId) ?: InventoryEntity(itemId)
        db.inventoryDao().upsert(cur.copy(equipped = true))
    }

    // ---- 방 배치 ----
    suspend fun place(itemId: String, x: Float, y: Float, z: Int) =
        db.roomPlacementDao().insert(RoomPlacementEntity(itemId = itemId, x = x, y = y, z = z))

    suspend fun movePlacement(p: RoomPlacementEntity) = db.roomPlacementDao().upsert(p)
    suspend fun removePlacement(id: Long) = db.roomPlacementDao().delete(id)
    suspend fun clearPlacements() = db.roomPlacementDao().clear()
    suspend fun replacePlacements(list: List<RoomPlacementEntity>) =
        db.roomPlacementDao().replaceAll(list)

    // ---- 미션 ----
    /** 오늘 미션이 없으면 풀에서 3개 뽑아 생성. 오전 5시 기준 날짜. */
    suspend fun ensureTodayMissions(): List<DailyMissionEntity> {
        val date = today()
        val existing = db.missionDao().getByDate(date)
        if (existing.isNotEmpty()) return existing
        val picked = Missions.POOL.shuffled().take(3).map {
            DailyMissionEntity(
                missionId = it.id,
                date = date,
                target = it.target,
                rewardCoin = it.rewardCoin,
                rewardHeart = it.rewardHeart,
            )
        }
        db.missionDao().upsertAll(picked)
        db.missionDao().deleteOther(date)
        return picked
    }

    suspend fun progressMission(missionId: String, amount: Int = 1) {
        val date = today()
        val m = db.missionDao().getByDate(date).firstOrNull { it.missionId == missionId } ?: return
        if (m.claimed) return
        db.missionDao().upsert(m.copy(progress = (m.progress + amount).coerceAtMost(m.target)))
    }

    suspend fun claimMission(missionId: String): DailyMissionEntity? {
        val m = db.missionDao().getByDate(today()).firstOrNull { it.missionId == missionId } ?: return null
        if (m.claimed || m.progress < m.target) return null
        val done = m.copy(claimed = true)
        db.missionDao().upsert(done)
        return done
    }

    // ---- 출석 ----
    suspend fun todayAttendance(): AttendanceEntity? = db.attendanceDao().get(today())

    suspend fun checkIn(): AttendanceEntity {
        val date = today()
        db.attendanceDao().get(date)?.let { return it }
        val last = db.attendanceDao().latest()
        val streak = when {
            last == null -> 1
            last.date == yesterday() -> (last.streakDay % 7) + 1
            else -> 1
        }
        val entry = AttendanceEntity(date = date, streakDay = streak, claimed = true)
        db.attendanceDao().upsert(entry)
        return entry
    }

    // ---- 도감 ----
    /** 이미 해금돼 있으면 false 를 돌려준다(중복 연출 방지). */
    suspend fun unlockCollection(entryId: String): Boolean {
        if (db.collectionDao().get(entryId) != null) return false
        db.collectionDao().insert(CollectionEntity(entryId))
        return true
    }

    // ---- 코디 세트 ----
    suspend fun saveOutfitSet(slot: Int, name: String, hatId: String, clothId: String, accId: String) =
        db.outfitDao().upsert(OutfitSetEntity(slot, name, hatId, clothId, accId))

    suspend fun deleteOutfitSet(slot: Int) = db.outfitDao().delete(slot)

    // ---- 데이터 초기화 (S-12 설정) ----
    suspend fun wipe() = db.clearAllTables()

    companion object {
        private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

        /** 게임 하루는 오전 5시에 시작한다. */
        fun today(): String {
            val c = Calendar.getInstance()
            if (c.get(Calendar.HOUR_OF_DAY) < 5) c.add(Calendar.DAY_OF_YEAR, -1)
            return fmt.format(c.time)
        }

        fun yesterday(): String {
            val c = Calendar.getInstance()
            if (c.get(Calendar.HOUR_OF_DAY) < 5) c.add(Calendar.DAY_OF_YEAR, -1)
            c.add(Calendar.DAY_OF_YEAR, -1)
            return fmt.format(c.time)
        }
    }
}
