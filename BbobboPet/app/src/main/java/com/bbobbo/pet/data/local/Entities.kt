package com.bbobbo.pet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_state")
data class PetStateEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "뽀뽀",
    val level: Int = 1,
    val exp: Int = 0,
    val fullness: Float = 80f,
    val mood: Float = 90f,
    val clean: Float = 85f,
    val energy: Float = 70f,
    val bond: Float = 50f,
    val coin: Long = 1500,
    val heart: Int = 10,
    val sleepEndAt: Long? = null,
    val walkEndAt: Long? = null,
    val freeFoodUsedDate: String = "",
    val freeFoodUsedCount: Int = 0,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val lastTouchAt: Long = 0L,
    val bestGameScore: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val itemId: String,
    val count: Int = 1,
    val equipped: Boolean = false,
    val acquiredAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "room_placement")
data class RoomPlacementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val x: Float,
    val y: Float,
    val z: Int = 0,
)

@Entity(tableName = "daily_mission")
data class DailyMissionEntity(
    @PrimaryKey val missionId: String,
    val date: String,
    val progress: Int = 0,
    val target: Int,
    val rewardCoin: Int = 0,
    val rewardHeart: Int = 0,
    val claimed: Boolean = false,
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val date: String,
    val streakDay: Int,
    val claimed: Boolean = false,
)
