package com.sap.codelab.repository.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateLocationFieldsType(db)
        addIndexes(db)
    }

    private fun migrateLocationFieldsType(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `memo_temp` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `reminderDate` INTEGER NOT NULL,
                `isDone` INTEGER NOT NULL,
                `reminderLatitude` REAL,
                `reminderLongitude` REAL
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `memo_temp` (
                `id`, `title`, `description`, `reminderDate`, `isDone`,
                `reminderLatitude`, `reminderLongitude`
            )
            SELECT
                `id`, `title`, `description`, `reminderDate`, `isDone`,
                CAST(`reminderLatitude` AS REAL) / 1000000.0 END,
                CAST(`reminderLongitude` AS REAL) / 1000000.0 END
            FROM `memo`
        """.trimIndent()
        )

        db.execSQL("DROP TABLE `memo`")
        db.execSQL("ALTER TABLE `memo_temp` RENAME TO `memo`")
    }

    private fun addIndexes(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_memo_isDone ON memo(isDone)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_memo_isDone_lat_lon ON memo(isDone, reminderLatitude, reminderLongitude)")
    }
}