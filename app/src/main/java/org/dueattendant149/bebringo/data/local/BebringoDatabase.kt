package org.dueattendant149.bebringo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LemmaEntity::class,
        SrsCardEntity::class,
        BlockProgressEntity::class,
        TelemetryEntity::class,
        StreakDayEntity::class,
        CollectionEntity::class,
        SkillEntity::class,
        EventEntity::class,
        ActivityProfileEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class BebringoDatabase : RoomDatabase() {

    abstract fun lemmaDao(): LemmaDao
    abstract fun srsDao(): SrsDao
    abstract fun blockProgressDao(): BlockProgressDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun streakDao(): StreakDao
    abstract fun collectionDao(): CollectionDao
    abstract fun skillDao(): SkillDao
    abstract fun eventDao(): EventDao
    abstract fun activityProfileDao(): ActivityProfileDao

    companion object {
        @Volatile
        private var instance: BebringoDatabase? = null

        /** Фаза 9.1: слой событий и профиль активности. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ts` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `sessionId` TEXT,
                        `payload` TEXT,
                        `snapshot` TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activity_profile` (
                        `id` INTEGER NOT NULL,
                        `profileJson` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): BebringoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BebringoDatabase::class.java,
                    "bebringo.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
