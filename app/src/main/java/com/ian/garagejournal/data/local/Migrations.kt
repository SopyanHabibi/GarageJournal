package com.ian.garagejournal.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `vehicleId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `intervalKm` INTEGER,
                `intervalMonths` INTEGER,
                `lastDoneOdometer` INTEGER NOT NULL,
                `lastDoneDate` INTEGER NOT NULL,
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_vehicleId` ON `reminders` (`vehicleId`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `lastNotifiedLevel` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `lastNotifiedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `notifyCount` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `snoozedUntil` INTEGER NOT NULL DEFAULT 0")
    }
}