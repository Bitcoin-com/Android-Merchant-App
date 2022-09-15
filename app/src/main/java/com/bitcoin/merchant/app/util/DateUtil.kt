package com.bitcoin.merchant.app.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DateUtil private constructor() {
    private val pastYearsFormat = SimpleDateFormat("E dd MMM @ HH:mm")
    private val currentYearFormat = SimpleDateFormat("dd MMM yyyy")
    private val previousDaysFormat = SimpleDateFormat("E dd MMM @ HH:mm")
    private val yesterdayFormat = SimpleDateFormat("HH:mm")
    private val todayFormat = SimpleDateFormat("HH:mm")
    private val hours24 = TimeUnit.HOURS.toMillis(24)
    fun format(timeInMillis: Long): String {
        if (timeInMillis == 0L) {
            return ""
        }
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.time = Date(now)
        val nowYear = cal[Calendar.YEAR]
        val nowDay = cal[Calendar.DAY_OF_MONTH]
        cal.time = Date(timeInMillis)
        return if (now - timeInMillis < hours24) {
            val thenDay = cal[Calendar.DAY_OF_MONTH]
            if (thenDay < nowDay) {
                "Yesterday @ " + yesterdayFormat.format(timeInMillis)
            } else {
                "Today @ " + todayFormat.format(timeInMillis)
            }
        } else if (now - timeInMillis < hours24 * 2) {
            previousDaysFormat.format(timeInMillis)
        } else {
            val thenYear = cal[Calendar.YEAR]
            if (thenYear < nowYear) {
                currentYearFormat.format(timeInMillis)
            } else {
                pastYearsFormat.format(timeInMillis)
            }
        }
    }

    fun formatHistorical(timeInMillis: Long): String {
        return pastYearsFormat.format(timeInMillis)
    }

    companion object {
        val instance: DateUtil by lazy { DateUtil() }
    }
}