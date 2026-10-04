package com.luisete.queda.core.database

import androidx.sqlite.db.SupportSQLiteDatabase

internal object StockMigration {
    fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE stock_items ADD COLUMN locationId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE stock_items ADD COLUMN foodType TEXT NOT NULL DEFAULT 'FOOD'")
        db.execSQL("ALTER TABLE stock_items ADD COLUMN label TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE stock_items ADD COLUMN preparedOn TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE stock_items ADD COLUMN bestBefore TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE pending_sync_operations ADD COLUMN batchId TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS storage_locations (
                id TEXT NOT NULL PRIMARY KEY, householdId TEXT NOT NULL,
                name TEXT NOT NULL, normalizedName TEXT NOT NULL, archived INTEGER NOT NULL,
                revision INTEGER NOT NULL DEFAULT 0, syncPending INTEGER NOT NULL DEFAULT 0,
                conflictPayload TEXT DEFAULT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX index_storage_locations_householdId_normalizedName " +
                "ON storage_locations (householdId, normalizedName)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS receipt_drafts (
                id TEXT NOT NULL PRIMARY KEY, householdId TEXT NOT NULL,
                fingerprint TEXT NOT NULL, payload TEXT NOT NULL, imported INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX index_receipt_drafts_householdId_fingerprint " +
                "ON receipt_drafts (householdId, fingerprint)",
        )
    }
}
