package com.bitcoin.merchant.app.database

import android.content.ContentValues
import java.util.*

class PaymentRecord {
    val address: String?
    val bchAmount: Long // can negative
    val fiatAmount: String? // can be null
    val message: String
    val tx: String? // can be null or empty
    var timeInSec: Long // can be 0
    var confirmations: Int // can be -1

    constructor(timeInSec: Long, address: String?, bchAmount: Long, fiatAmount: String?, confirmations: Int, message: String, tx: String?) {
        this.timeInSec = timeInSec
        this.address = address
        this.bchAmount = bchAmount
        this.fiatAmount = fiatAmount
        this.confirmations = confirmations
        this.message = message
        this.tx = tx
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val txRecord = o as PaymentRecord
        return tx != null && tx == txRecord.tx
    }

    override fun hashCode(): Int {
        return Objects.hash(tx)
    }

    override fun toString(): String {
        return "TxRecord{" +
                "tx='" + tx + '\'' +
                ", bchAmount=" + bchAmount +
                ", fiatAmount='" + fiatAmount + '\'' +
                ", address='" + address + '\'' +
                ", timeInSec=" + timeInSec +
                ", confirmations=" + confirmations +
                ", message='" + message + '\'' +
                '}'
    }
}