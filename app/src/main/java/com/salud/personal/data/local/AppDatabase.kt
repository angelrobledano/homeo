package com.salud.personal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.salud.personal.data.local.converter.Converters
import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.DailyCheckInDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.dao.HourlyStepsDao
import com.salud.personal.data.local.dao.SleepSessionDao
import com.salud.personal.data.local.dao.WeightDao
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.HeartRateSampleEntity
import com.salud.personal.data.local.entity.HourlyStepsEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        WeightMeasurementEntity::class,
        HeartRateSampleEntity::class,
        DailyActivityEntity::class,
        HourlyStepsEntity::class,
        WorkoutSessionEntity::class,
        DailyCheckInEntity::class,
        SleepSessionEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun hourlyStepsDao(): HourlyStepsDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun dailyCheckInDao(): DailyCheckInDao
    abstract fun sleepSessionDao(): SleepSessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `heart_rate_samples` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `bpm` INTEGER NOT NULL,
                        `sourceDevice` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_heart_rate_samples_timestamp` ON `heart_rate_samples` (`timestamp`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_heart_rate_samples_sourceDevice_timestamp` ON `heart_rate_samples` (`sourceDevice`, `timestamp`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_activity` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `steps` INTEGER NOT NULL,
                        `distanceMeters` INTEGER NOT NULL,
                        `caloriesBurned` INTEGER NOT NULL,
                        `sourceDevice` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_activity_sourceDevice_date` ON `daily_activity` (`sourceDevice`, `date`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hourly_steps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `hour` INTEGER NOT NULL,
                        `steps` INTEGER NOT NULL,
                        `source_device` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_hourly_steps_source_device_date_hour` ON `hourly_steps` (`source_device`, `date`, `hour`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_hourly_steps_date` ON `hourly_steps` (`date`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `durationMinutes` REAL NOT NULL,
                        `avgHeartRate` INTEGER,
                        `maxHeartRate` INTEGER,
                        `trimp` REAL,
                        `caloriesBurned` INTEGER,
                        `rpe` INTEGER NOT NULL,
                        `notes` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_startTime` ON `workout_sessions` (`startTime`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_type` ON `workout_sessions` (`type`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_checkins` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `energyRating` INTEGER NOT NULL,
                        `sleepQualityRating` INTEGER NOT NULL,
                        `muscleSorenessRating` INTEGER NOT NULL,
                        `readinessRating` INTEGER NOT NULL,
                        `sleepHours` REAL NOT NULL,
                        `restingHeartRate` INTEGER,
                        `calculatedRecoveryScore` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_checkins_date` ON `daily_checkins` (`date`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sleep_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `deepSleepMinutes` INTEGER NOT NULL,
                        `lightSleepMinutes` INTEGER NOT NULL,
                        `remSleepMinutes` INTEGER NOT NULL,
                        `awakeMinutes` INTEGER NOT NULL,
                        `totalSleepMinutes` INTEGER NOT NULL,
                        `sleepEfficiencyScore` INTEGER,
                        `sourceDevice` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sleep_sessions_date` ON `sleep_sessions` (`date`)"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_checkins` ADD COLUMN `lateMeal` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_checkins` ADD COLUMN `lateCaffeine` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_checkins` ADD COLUMN `alcohol` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_checkins` ADD COLUMN `highStress` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_checkins` ADD COLUMN `supplements` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_checkins` ADD COLUMN `screenBeforeBed` INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salud_personal.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
