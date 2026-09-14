package com.bbobbo.pet.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pet_state WHERE id = 1")
    fun observe(): Flow<PetStateEntity?>

    @Query("SELECT * FROM pet_state WHERE id = 1")
    suspend fun get(): PetStateEntity?

    @Upsert
    suspend fun upsert(state: PetStateEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE itemId = :id")
    suspend fun get(id: String): InventoryEntity?

    @Upsert
    suspend fun upsert(item: InventoryEntity)

    @Query("UPDATE inventory SET equipped = 0 WHERE itemId IN (:ids)")
    suspend fun unequipAll(ids: List<String>)

    @Query("DELETE FROM inventory WHERE itemId = :id")
    suspend fun delete(id: String)
}

@Dao
interface RoomPlacementDao {
    @Query("SELECT * FROM room_placement ORDER BY z ASC")
    fun observeAll(): Flow<List<RoomPlacementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: RoomPlacementEntity): Long

    @Upsert
    suspend fun upsert(p: RoomPlacementEntity)

    @Query("DELETE FROM room_placement WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM room_placement")
    suspend fun clear()
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM daily_mission WHERE date = :date")
    fun observeByDate(date: String): Flow<List<DailyMissionEntity>>

    @Query("SELECT * FROM daily_mission WHERE date = :date")
    suspend fun getByDate(date: String): List<DailyMissionEntity>

    @Upsert
    suspend fun upsertAll(list: List<DailyMissionEntity>)

    @Upsert
    suspend fun upsert(m: DailyMissionEntity)

    @Query("DELETE FROM daily_mission WHERE date != :date")
    suspend fun deleteOther(date: String)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC LIMIT 1")
    suspend fun latest(): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE date = :date")
    suspend fun get(date: String): AttendanceEntity?

    @Upsert
    suspend fun upsert(a: AttendanceEntity)
}
