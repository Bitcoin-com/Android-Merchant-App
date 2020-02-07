package com.bitcoin.merchant.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*

class MonetaryUtil private constructor() {
    fun getDisplayAmountWithFormatting(value: Long): String {
        val df = DecimalFormat("#")
        df.minimumIntegerDigits = 1
        df.minimumFractionDigits = 1
        df.maximumFractionDigits = 8
        var strAmount = bchFormat.format(value / 1e8)
        val i = strAmount.indexOf('.')
        if (i != -1) {
            val integerPart = strAmount.substring(0, i)
            val decimalParts = strAmount.substring(i + 1)
            val s = StringBuilder("$integerPart.")
            val length = decimalParts.length
            for (j in 0..7) {
                if (j == 3 || j == 6) {
                    s.append(" ")
                }
                s.append(if (j < length) decimalParts[j] else '0')
            }
            strAmount = s.toString()
        }
        return strAmount
    }

    val bchFormat: NumberFormat
    val decimalFormatSymbols: DecimalFormatSymbols
    val bchDecimalFormat = DecimalFormat("######0.0#######")
    val fiatDecimalFormat: DecimalFormat
        get() {
            val f = DecimalFormat("######0.00")
            f.decimalFormatSymbols = decimalFormatSymbols
            return f
        }

    companion object {
        val instance: MonetaryUtil by lazy { MonetaryUtil() }
    }

    init {
        bchFormat = NumberFormat.getInstance(Locale.getDefault())
        bchFormat.maximumFractionDigits = 8
        bchFormat.minimumFractionDigits = 1
        decimalFormatSymbols = DecimalFormatSymbols()
        bchDecimalFormat.decimalFormatSymbols = decimalFormatSymbols
    }
}