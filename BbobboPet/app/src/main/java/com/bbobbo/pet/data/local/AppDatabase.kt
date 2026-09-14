package com.bbobbo.pet.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PetStateEntity::class,
        InventoryEntity::class,
        RoomPlacementEntity::class,
        DailyMissionEntity::class,
        AttendanceEntity::class,
        CollectionEntity::class,
        OutfitSetEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun roomPlacementDao(): RoomPlacementDao
    abstract fun missionDao(): MissionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun collectionDao(): CollectionDao
    abstract fun outfitDao(): OutfitDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v1 → v2: 도감/코디세트 테이블과 pet_state 의 누적 기록 컬럼 추가.
         * 기존 플레이어의 스탯·재화를 잃지 않도록 파괴적 마이그레이션 대신 명시적으로 옮긴다.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `collection_entry` (" +
                        "`entryId` TEXT NOT NULL, `unlockedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`entryId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `outfit_set` (" +
                        "`slot` INTEGER NOT NULL, `setName` TEXT NOT NULL, " +
                        "`hatId` TEXT NOT NULL, `clothId` TEXT NOT NULL, `accId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`slot`))"
                )
                listOf(
                    "`lastPetAt` INTEGER NOT NULL DEFAULT 0",
                    "`bestGameCombo` INTEGER NOT NULL DEFAULT 0",
                    "`onboarded` INTEGER NOT NULL DEFAULT 0",
                    "`walkCountDate` TEXT NOT NULL DEFAULT ''",
                    "`walkCount` INTEGER NOT NULL DEFAULT 0",
                    "`roomBonusDate` TEXT NOT NULL DEFAULT ''",
                    "`collectionMilestone` INTEGER NOT NULL DEFAULT 0",
                    "`totalFeed` INTEGER NOT NULL DEFAULT 0",
                    "`totalBath` INTEGER NOT NULL DEFAULT 0",
                    "`totalPlay` INTEGER NOT NULL DEFAULT 0",
                    "`totalWalk` INTEGER NOT NULL DEFAULT 0",
                    "`totalGame` INTEGER NOT NULL DEFAULT 0",
                ).forEach { db.execSQL("ALTER TABLE `pet_state` ADD COLUMN $it") }
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bbobbo.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
