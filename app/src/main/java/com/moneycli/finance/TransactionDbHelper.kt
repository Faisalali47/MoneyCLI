package com.moneycli.finance

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TransactionDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // ========================================================
    // CREATE DATABASE
    // ========================================================

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                description TEXT NOT NULL,
                wallet TEXT,
                source_wallet TEXT,
                destination_wallet TEXT,
                date TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    // ========================================================
    // DATABASE UPGRADE
    // ========================================================

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        onCreate(db)
    }

    // ========================================================
    // DATABASE CONFIGURATION
    // ========================================================

    companion object {
        private const val DATABASE_NAME = "money_cli.db"
        private const val DATABASE_VERSION = 4
    }
}