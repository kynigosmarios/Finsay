package com.example.util

import com.example.data.entity.RecurringExpense
import java.util.Calendar
import java.util.Locale

/**
 * Unified schedule helper for standing orders / recurring expenses supporting:
 * WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY.
 *
 * Conventions:
 * - For WEEKLY / BIWEEKLY: `dayOfMonth` holds ISO day-of-week `1..7` (1=Monday .. 7=Sunday).
 * - For MONTHLY / QUARTERLY / YEARLY: `dayOfMonth` holds day-of-month `1..31`.
 * - For QUARTERLY / YEARLY: `anchorMonth` holds `0..11` (January..December anchor).
 */
object RecurringScheduleHelper {
    const val FREQ_WEEKLY = "WEEKLY"
    const val FREQ_BIWEEKLY = "BIWEEKLY"
    const val FREQ_MONTHLY = "MONTHLY"
    const val FREQ_QUARTERLY = "QUARTERLY"
    const val FREQ_YEARLY = "YEARLY"

    val ALL_FREQUENCIES = listOf(
        FREQ_WEEKLY,
        FREQ_BIWEEKLY,
        FREQ_MONTHLY,
        FREQ_QUARTERLY,
        FREQ_YEARLY
    )

    fun normalizeFrequency(raw: String?): String = when (raw?.trim()?.uppercase(Locale.ROOT)) {
        FREQ_WEEKLY -> FREQ_WEEKLY
        FREQ_BIWEEKLY -> FREQ_BIWEEKLY
        FREQ_QUARTERLY -> FREQ_QUARTERLY
        FREQ_YEARLY -> FREQ_YEARLY
        else -> FREQ_MONTHLY
    }

    /**
     * Converts Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to ISO day of week (Mon=1..Sun=7).
     */
    fun calendarDowToIso(calendarDow: Int): Int = when (calendarDow) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }

    /**
     * Converts ISO day of week (1=Mon..7=Sun) to Calendar.DAY_OF_WEEK.
     */
    fun isoToCalendarDow(isoDow: Int): Int = when (isoDow.coerceIn(1, 7)) {
        1 -> Calendar.MONDAY
        2 -> Calendar.TUESDAY
        3 -> Calendar.WEDNESDAY
        4 -> Calendar.THURSDAY
        5 -> Calendar.FRIDAY
        6 -> Calendar.SATURDAY
        else -> Calendar.SUNDAY
    }

    /**
     * Produces an unambiguous period key for `lastGeneratedMonth` so deduplication works cleanly
     * across WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, and YEARLY frequencies.
     */
    fun periodKeyFor(recurring: RecurringExpense, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = nowMillis
        }
        val year = cal.get(Calendar.YEAR)
        val monthZero = cal.get(Calendar.MONTH)
        return when (normalizeFrequency(recurring.frequency)) {
            FREQ_WEEKLY -> {
                val weekYear = cal.weekYear
                val week = cal.get(Calendar.WEEK_OF_YEAR)
                String.format(Locale.US, "%04d-W%02d", weekYear, week)
            }
            FREQ_BIWEEKLY -> {
                // Anchor epoch Mondays since 2024-01-01 so every 14-day period has a stable biweek index
                val dayStart = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val epochMonday = Calendar.getInstance().apply {
                    set(2024, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffDays = ((dayStart.timeInMillis - epochMonday.timeInMillis) / 86_400_000L).coerceAtLeast(0L)
                val biweekIndex = diffDays / 14L
                String.format(Locale.US, "%04d-BW%d", year, biweekIndex)
            }
            FREQ_QUARTERLY -> {
                val quarter = (monthZero / 3) + 1
                String.format(Locale.US, "%04d-Q%d", year, quarter)
            }
            FREQ_YEARLY -> String.format(Locale.US, "%04d", year)
            else -> String.format(Locale.US, "%04d-%02d", year, monthZero + 1)
        }
    }

    fun isMatchingScheduledMonth(recurring: RecurringExpense, monthZeroIndexed: Int): Boolean {
        return when (normalizeFrequency(recurring.frequency)) {
            FREQ_MONTHLY, FREQ_WEEKLY, FREQ_BIWEEKLY -> true
            FREQ_QUARTERLY -> {
                val anchor = recurring.anchorMonth.coerceIn(0, 11)
                ((monthZeroIndexed - anchor) % 3 + 3) % 3 == 0
            }
            FREQ_YEARLY -> monthZeroIndexed == recurring.anchorMonth.coerceIn(0, 11)
            else -> true
        }
    }

    fun isDueNow(recurring: RecurringExpense, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!recurring.isActive) return false
        val currentKey = periodKeyFor(recurring, nowMillis)
        if (recurring.lastGeneratedMonth == currentKey) return false

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val freq = normalizeFrequency(recurring.frequency)
        return when (freq) {
            FREQ_WEEKLY -> {
                val targetIso = recurring.dayOfMonth.coerceIn(1, 7)
                val currentIso = calendarDowToIso(cal.get(Calendar.DAY_OF_WEEK))
                currentIso >= targetIso
            }
            FREQ_BIWEEKLY -> {
                val targetIso = recurring.dayOfMonth.coerceIn(1, 7)
                val currentIso = calendarDowToIso(cal.get(Calendar.DAY_OF_WEEK))
                // Trigger in even biweek buckets starting on target day of week
                val dayStart = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val epochMonday = Calendar.getInstance().apply {
                    set(2024, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffWeeks = ((dayStart.timeInMillis - epochMonday.timeInMillis) / (7L * 86_400_000L)).coerceAtLeast(0L)
                val isEvenWeek = (diffWeeks % 2L) == 0L
                isEvenWeek && currentIso >= targetIso
            }
            FREQ_QUARTERLY, FREQ_YEARLY -> {
                val currentMonth = cal.get(Calendar.MONTH)
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val effectiveDay = recurring.dayOfMonth.coerceIn(1, maxDay)
                isMatchingScheduledMonth(recurring, currentMonth) && currentDay >= effectiveDay
            }
            else -> {
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val effectiveDay = recurring.dayOfMonth.coerceIn(1, maxDay)
                cal.get(Calendar.DAY_OF_MONTH) >= effectiveDay
            }
        }
    }

    /**
     * Strict schedule check for in-app reminder banners:
     * Returns true ONLY on the exact scheduled day (accounting for frequency & anchorMonth)
     * AND once current local time reaches or passes reminderHour:reminderMinute on that day,
     * provided the reminder has not already been logged in the current period.
     */
    fun isReminderDueAtExactTime(
        recurring: RecurringExpense,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!recurring.isActive) return false
        val currentKey = periodKeyFor(recurring, nowMillis)
        if (recurring.lastGeneratedMonth == currentKey) return false

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val freq = normalizeFrequency(recurring.frequency)
        val isExactDayMatch = when (freq) {
            FREQ_WEEKLY -> {
                val targetIso = recurring.dayOfMonth.coerceIn(1, 7)
                val currentIso = calendarDowToIso(cal.get(Calendar.DAY_OF_WEEK))
                currentIso == targetIso
            }
            FREQ_BIWEEKLY -> {
                val targetIso = recurring.dayOfMonth.coerceIn(1, 7)
                val currentIso = calendarDowToIso(cal.get(Calendar.DAY_OF_WEEK))
                val dayStart = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val epochMonday = Calendar.getInstance().apply {
                    set(2024, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffWeeks = ((dayStart.timeInMillis - epochMonday.timeInMillis) / (7L * 86_400_000L)).coerceAtLeast(0L)
                val isEvenWeek = (diffWeeks % 2L) == 0L
                isEvenWeek && currentIso == targetIso
            }
            FREQ_QUARTERLY, FREQ_YEARLY -> {
                val currentMonth = cal.get(Calendar.MONTH)
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val effectiveDay = recurring.dayOfMonth.coerceIn(1, maxDay)
                isMatchingScheduledMonth(recurring, currentMonth) && currentDay == effectiveDay
            }
            else -> {
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val effectiveDay = recurring.dayOfMonth.coerceIn(1, maxDay)
                cal.get(Calendar.DAY_OF_MONTH) == effectiveDay
            }
        }
        if (!isExactDayMatch) return false

        val currentMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val targetMinutesOfDay = recurring.reminderHour.coerceIn(0, 23) * 60 + recurring.reminderMinute.coerceIn(0, 59)
        return currentMinutesOfDay >= targetMinutesOfDay
    }

    fun calculateTargetTimestampForPeriod(recurring: RecurringExpense, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val freq = normalizeFrequency(recurring.frequency)
        when (freq) {
            FREQ_WEEKLY, FREQ_BIWEEKLY -> {
                val targetCalDow = isoToCalendarDow(recurring.dayOfMonth.coerceIn(1, 7))
                cal.set(Calendar.DAY_OF_WEEK, targetCalDow)
            }
            else -> {
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, recurring.dayOfMonth.coerceIn(1, maxDay))
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, recurring.reminderHour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, recurring.reminderMinute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun calculateNextTriggerTimeMillis(
        frequency: String,
        dayOfMonth: Int,
        anchorMonth: Int,
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val freq = normalizeFrequency(frequency)

        return when (freq) {
            FREQ_WEEKLY -> {
                val targetCalDow = isoToCalendarDow(dayOfMonth.coerceIn(1, 7))
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, targetCalDow)
                    set(Calendar.HOUR_OF_DAY, safeHour)
                    set(Calendar.MINUTE, safeMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= nowMillis) {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            FREQ_BIWEEKLY -> {
                val targetCalDow = isoToCalendarDow(dayOfMonth.coerceIn(1, 7))
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, targetCalDow)
                    set(Calendar.HOUR_OF_DAY, safeHour)
                    set(Calendar.MINUTE, safeMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= nowMillis) {
                    cal.add(Calendar.WEEK_OF_YEAR, 2)
                }
                cal.timeInMillis
            }
            FREQ_QUARTERLY -> {
                val safeAnchor = anchorMonth.coerceIn(0, 11)
                val safeDay = dayOfMonth.coerceIn(1, 31)
                val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                for (offset in 0..12) {
                    val probe = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        add(Calendar.MONTH, offset)
                        set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                        set(Calendar.HOUR_OF_DAY, safeHour)
                        set(Calendar.MINUTE, safeMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val m = probe.get(Calendar.MONTH)
                    val isQuarterMatch = ((m - safeAnchor) % 3 + 3) % 3 == 0
                    if (isQuarterMatch && probe.timeInMillis > nowMillis) {
                        return probe.timeInMillis
                    }
                }
                cal.timeInMillis + 90L * 86_400_000L
            }
            FREQ_YEARLY -> {
                val safeAnchor = anchorMonth.coerceIn(0, 11)
                val safeDay = dayOfMonth.coerceIn(1, 31)
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.MONTH, safeAnchor)
                    set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                    set(Calendar.HOUR_OF_DAY, safeHour)
                    set(Calendar.MINUTE, safeMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= nowMillis) {
                    cal.add(Calendar.YEAR, 1)
                    cal.set(Calendar.MONTH, safeAnchor)
                    cal.set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }
                cal.timeInMillis
            }
            else -> {
                val safeDay = dayOfMonth.coerceIn(1, 31)
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                    set(Calendar.HOUR_OF_DAY, safeHour)
                    set(Calendar.MINUTE, safeMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= nowMillis) {
                    cal.add(Calendar.MONTH, 1)
                    cal.set(Calendar.DAY_OF_MONTH, safeDay.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
                }
                cal.timeInMillis
            }
        }
    }

    /**
     * Estimates remaining scheduled occurrences of this recurring rule inside a given year/month
     * AFTER the reference day (used for Analytics End-of-Month forecast).
     */
    fun remainingOccurrencesInMonth(
        recurring: RecurringExpense,
        year: Int,
        monthZeroIndexed: Int,
        afterDayOfMonth: Int
    ): Int {
        if (!recurring.isActive) return 0
        val currentKey = periodKeyFor(
            recurring,
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroIndexed)
                set(Calendar.DAY_OF_MONTH, afterDayOfMonth.coerceIn(1, 28))
            }.timeInMillis
        )
        if (recurring.lastGeneratedMonth == currentKey) return 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val freq = normalizeFrequency(recurring.frequency)

        return when (freq) {
            FREQ_WEEKLY -> {
                val targetCalDow = isoToCalendarDow(recurring.dayOfMonth.coerceIn(1, 7))
                var count = 0
                for (d in (afterDayOfMonth + 1)..maxDays) {
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    if (cal.get(Calendar.DAY_OF_WEEK) == targetCalDow) count++
                }
                count
            }
            FREQ_BIWEEKLY -> {
                val targetCalDow = isoToCalendarDow(recurring.dayOfMonth.coerceIn(1, 7))
                var count = 0
                for (d in (afterDayOfMonth + 1)..maxDays) {
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    if (cal.get(Calendar.DAY_OF_WEEK) == targetCalDow) {
                        count++
                        if (count >= 1) {
                            // At most one more every 14 days from a single week index
                            break
                        }
                    }
                }
                count
            }
            FREQ_QUARTERLY, FREQ_YEARLY -> {
                if (!isMatchingScheduledMonth(recurring, monthZeroIndexed)) return 0
                val effectiveDay = recurring.dayOfMonth.coerceIn(1, maxDays)
                if (effectiveDay > afterDayOfMonth) 1 else 0
            }
            else -> {
                val effectiveDay = recurring.dayOfMonth.coerceIn(1, maxDays)
                if (effectiveDay > afterDayOfMonth) 1 else 0
            }
        }
    }

    fun frequencyLabel(frequency: String, lang: Localization.Language): String {
        val isEn = lang == Localization.Language.ENGLISH
        return when (normalizeFrequency(frequency)) {
            FREQ_WEEKLY -> if (isEn) "Weekly" else "Εβδομαδιαία"
            FREQ_BIWEEKLY -> if (isEn) "Biweekly" else "Δεκαπενθήμερη"
            FREQ_QUARTERLY -> if (isEn) "Quarterly" else "Τριμηνιαία"
            FREQ_YEARLY -> if (isEn) "Yearly" else "Ετήσια"
            else -> if (isEn) "Monthly" else "Μηνιαία"
        }
    }

    fun weekdayName(isoDow: Int, lang: Localization.Language): String {
        val isEn = lang == Localization.Language.ENGLISH
        return when (isoDow.coerceIn(1, 7)) {
            1 -> if (isEn) "Mon" else "Δευ"
            2 -> if (isEn) "Tue" else "Τρί"
            3 -> if (isEn) "Wed" else "Τετ"
            4 -> if (isEn) "Thu" else "Πέμ"
            5 -> if (isEn) "Fri" else "Παρ"
            6 -> if (isEn) "Sat" else "Σάβ"
            else -> if (isEn) "Sun" else "Κυρ"
        }
    }

    fun monthNameShort(monthZeroIndexed: Int, lang: Localization.Language): String {
        val isEn = lang == Localization.Language.ENGLISH
        val en = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val el = listOf("Ιαν", "Φεβ", "Μαρ", "Απρ", "Μαΐ", "Ιουν", "Ιουλ", "Αυγ", "Σεπ", "Οκτ", "Νοε", "Δεκ")
        val idx = monthZeroIndexed.coerceIn(0, 11)
        return if (isEn) en[idx] else el[idx]
    }

    fun scheduleSummary(recurring: RecurringExpense, lang: Localization.Language): String {
        val isEn = lang == Localization.Language.ENGLISH
        return when (normalizeFrequency(recurring.frequency)) {
            FREQ_WEEKLY -> {
                val wd = weekdayName(recurring.dayOfMonth, lang)
                if (isEn) "Weekly ($wd)" else "Κάθε εβδομάδα ($wd)"
            }
            FREQ_BIWEEKLY -> {
                val wd = weekdayName(recurring.dayOfMonth, lang)
                if (isEn) "Biweekly ($wd)" else "Ανά 15 ημέρες ($wd)"
            }
            FREQ_QUARTERLY -> {
                val m = monthNameShort(recurring.anchorMonth, lang)
                if (isEn) "Quarterly (${recurring.dayOfMonth}, from $m)" else "Τριμηνιαία (${recurring.dayOfMonth}η, από $m)"
            }
            FREQ_YEARLY -> {
                val m = monthNameShort(recurring.anchorMonth, lang)
                if (isEn) "Yearly ($m ${recurring.dayOfMonth})" else "Ετήσια (${recurring.dayOfMonth} $m)"
            }
            else -> {
                if (isEn) "Every ${recurring.dayOfMonth} of month" else "Κάθε ${recurring.dayOfMonth} του μήνα"
            }
        }
    }
}
