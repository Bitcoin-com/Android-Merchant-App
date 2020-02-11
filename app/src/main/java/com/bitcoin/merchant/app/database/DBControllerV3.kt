package com.bitcoin.merchant.app.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Log
import com.bitcoin.merchant.app.application.CashRegisterApplication
import com.crashlytics.android.Crashlytics
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.util.CharSequenceX
import java.util.*

fun ContentValues.toPaymentRecord(): PaymentRecord {
    return PaymentRecord(timeInSec = getAsLong("ts"),
            tx = getAsString("tx"),
            address = getAsString("iad"),
            bchAmount = getAsLong("amt"),
            fiatAmount = getAsString("famt"),
            confirmations = getAsInteger("cfm"),
            message = getAsString("msg"))
}

private fun PaymentRecord.toContentValues(): ContentValues {
    val c = ContentValues()
    c.put("ts", timeInSec)
    c.put("tx", tx)
    c.put("iad", address)
    c.put("amt", bchAmount.toString())
    c.put("famt", fiatAmount)
    c.put("cfm", confirmations.toString())
    c.put("msg", message)
    return c
}

class DBControllerV3(app: CashRegisterApplication?) : SQLiteOpenHelper(app, DB, null, 1) {
    private val salt: String = Build.MANUFACTURER + Build.BRAND + Build.MODEL + Build.DEVICE + Build.PRODUCT + Build.SERIAL
    override fun onCreate(database: SQLiteDatabase) {
        val query: String
        query = "CREATE TABLE " + TABLE + " ( " +
                "_id INTEGER PRIMARY KEY, " +
                "ts integer, " +
                "iad text, " +
                "amt text, " +
                "famt text, " +
                "cfm text, " +
                "msg text," +
                "tx text" +
                ")"
        database.execSQL(query)
    }

    override fun onUpgrade(database: SQLiteDatabase, version_old: Int, current_version: Int) {
        val query: String
        query = "DROP TABLE IF EXISTS $TABLE"
        database.execSQL(query)
        onCreate(database)
    }

    @Throws(Exception::class)
    fun insertPayment(record: PaymentRecord) {
        insertPayment(record.toContentValues())
    }

    @Throws(Exception::class)
    fun insertPayment(record: ContentValues) {
        var database: SQLiteDatabase? = null
        try {
            database = this.writableDatabase
            database.insert(TABLE, null, record)
        } finally {
            closeAll(database, null)
        }
    }

    fun getPaymentFromTx(tx: String): ContentValues? {
        var vals: ContentValues? = null
        var database: SQLiteDatabase? = null
        var c: Cursor? = null
        try {
            val selectQuery = "SELECT * FROM payment WHERE tx = ?"
            database = this.readableDatabase
            c = database.rawQuery(selectQuery, arrayOf(tx))
            if (c.moveToFirst()) {
                vals = parseRecord(c)
            }
        } finally {
            closeAll(database, c)
        }
        return vals
    }

    // decrypt and overwrite decrypted
    @get:Throws(Exception::class)
    val allPayments: ArrayList<ContentValues>
        get() {
            val data = ArrayList<ContentValues>()
            var database: SQLiteDatabase? = null
            var c: Cursor? = null
            try {
                val selectQuery = "SELECT * FROM payment ORDER BY ts DESC"
                database = this.readableDatabase
                c = database.rawQuery(selectQuery, null)
                if (c.moveToFirst()) {
                    do {
                        val vals = parseRecord(c)
                        data.add(vals)
                    } while (c.moveToNext())
                }
            } finally {
                closeAll(database, c)
            }
            // decrypt and overwrite decrypted
            formatDecryptAndResave(data)
            return data
        }

    private fun parseRecord(c: Cursor): ContentValues {
        val vals = ContentValues()
        vals.put("_id", c.getString(0))
        vals.put("ts", c.getLong(1))
        vals.put("iad", c.getString(2))
        vals.put("amt", c.getString(3))
        vals.put("famt", c.getString(4))
        vals.put("cfm", c.getString(5))
        vals.put("msg", c.getString(6))
        vals.put("tx", c.getString(7))
        return vals
    }

    private fun formatDecryptAndResave(data: ArrayList<ContentValues>) {
        var database: SQLiteDatabase? = null
        try {
            for (i in data.indices) {
                val vals = data[i]
                try {
                    formatValues(vals)
                } catch (e: Exception) { // decrypt
                    decrypt(vals, "iad")
                    decrypt(vals, "amt")
                    decrypt(vals, "famt")
                    decrypt(vals, "cfm")
                    decrypt(vals, "msg")
                    decrypt(vals, "tx")
                    Log.i(TAG, "decrypted record:" + vals["_id"])
                    // resave
                    if (database == null) {
                        database = this.writableDatabase
                    }
                    val result = database!!.update(TABLE, vals, "_id=" + vals["_id"], null)
                    Log.i(TAG, "resaved record:" + vals["_id"] + ", update:" + result)
                    // format
                    formatValues(vals)
                }
            }
        } finally {
            closeAll(database, null)
        }
    }

    private fun decryptValue(value: String): String {
        if (pw == null) {
            pw = CharSequenceX("pin" + salt) // legacy code
        }
        return AESUtil.decrypt(value, pw, AESUtil.PinPbkdf2Iterations)
    }

    private fun decrypt(vals: ContentValues, name: String) {
        val value = vals.getAsString(name)
        if (value != null) {
            vals.put(name, decryptValue(value))
        }
    }

    private fun formatValues(vals: ContentValues) {
        vals.put("amt", vals.getAsString("amt").toLong())
        vals.put("cfm", vals.getAsString("cfm").toInt())
    }

    @get:Throws(Exception::class)
    val allAddresses: Set<String>
        get() {
            val data: MutableSet<String> = HashSet()
            var database: SQLiteDatabase? = null
            var cursor: Cursor? = null
            try {
                val selectQuery = "SELECT iad FROM payment"
                database = this.readableDatabase
                cursor = database.rawQuery(selectQuery, null)
                if (cursor.moveToFirst()) {
                    do {
                        val decrypt = decryptValue(cursor.getString(0))
                        data.add(decrypt)
                    } while (cursor.moveToNext())
                }
            } finally {
                closeAll(database, cursor)
            }
            return data
        }

    private fun closeAll(database: SQLiteDatabase?, cursor: Cursor?) {
        try {
            cursor?.close()
            database?.close()
            close()
        } catch (e: Exception) {
            Log.e(TAG, "closeAll", e)
            Crashlytics.logException(e)
        }
    }

    companion object {
        private const val TAG = "DBControllerV3"
        private const val DB = "paymentsV3.db"
        private const val TABLE = "payment"
        private var pw: CharSequenceX? = null
    }

}