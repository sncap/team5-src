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
    val lastPetAt: Long = 0L,
    val bestGameScore: Int = 0,
    val bestGameCombo: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    // ---- v2 ----
    /** 온보딩(이름 짓기·튜토리얼)을 마쳤는지 */
    val onboarded: Boolean = false,
    /** 산책 일일 횟수 제한용 */
    val walkCountDate: String = "",
    val walkCount: Int = 0,
    /** 방 등급 일일 보너스를 마지막으로 지급한 날 */
    val roomBonusDate: String = "",
    /** 도감 마일스톤을 마지막으로 수령한 단계(%) */
    val collectionMilestone: Int = 0,
    // 누적 기록 (하단 탭 '뽀뽀' 상세)
    val totalFeed: Int = 0,
    val totalBath: Int = 0,
    val totalPlay: Int = 0,
    val totalWalk: Int = 0,
    val totalGame: Int = 0,
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

/** S-10 도감. 해금된 항목만 행이 생긴다. */
@Entity(tableName = "collection_entry")
data class CollectionEntity(
    @PrimaryKey val entryId: String,
    val unlockedAt: Long = System.currentTimeMillis(),
)

/** S-07 코디 세트 저장 슬롯 (1~5) */
@Entity(tableName = "outfit_set")
data class OutfitSetEntity(
    @PrimaryKey val slot: Int,
    val setName: String,
    val hatId: String,
    val clothId: String,
    val accId: String,
)
