package com.x.player.util

import java.util.Calendar

/** Start-of-period timestamps (local time) for the stats screen. Weeks start on Monday. */
data class StatsWindows(val day: Long, val week: Long, val month: Long, val year: Long) {
    companion object {
        fun now(): StatsWindows {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val week = (today.clone() as Calendar).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val month = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            val year = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_YEAR, 1) }
            return StatsWindows(today.timeInMillis, week.timeInMillis, month.timeInMillis, year.timeInMillis)
        }
    }
}
