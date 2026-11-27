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
    const val FREQ_ONCE = "ONCE"
    const val FREQ_WEEKLY = "WEEKLY"
    const val FREQ_BIWEEKLY = "BIWEEKLY"
    const val FREQ_MONTHLY = "MONTHLY"
    const val FREQ_QUARTERLY = "QUARTERLY"
    const val FREQ_YEARLY = "YEARLY"

    private const val ONCE_ANCHOR_MIN = 2000 * 12

    val ALL_FREQUENCIES = listOf(
        FREQ_ONCE,
        FREQ_WEEKLY,
        FREQ_BIWEEKLY,
        FREQ_MONTHLY,
        FREQ_QUARTERLY,
        FREQ_YEARLY
    )

    fun normalizeFrequency(raw: String?): String = when (raw?.trim()?.uppercase(Locale.ROOT)) {
        FREQ_ONCE -> FREQ_ONCE
        FREQ_WEEKLY -> FREQ_WEEKLY
        FREQ_BIWEEKLY -> FREQ_BIWEEKLY
        FREQ_QUARTERLY -> FREQ_QUARTERLY
        FREQ_YEARLY -> FREQ_YEARLY
        else -> FREQ_MONTHLY
    }

    fun isOnce(frequency: String?): Boolean = normalizeFrequency(frequency) == FREQ_ONCE

    fun encodeOnceAnchor(year: Int, monthZeroIndexed: Int): Int {
        val safeYear = year.coerceIn(2000, 2200)
        val safeMonth = monthZeroIndexed.coerceIn(0, 11)
        return safeYear * 12 + safeMonth
    }

    fun decodeOnceAnchor(anchorMonth: Int, fallbackNowMillis: Long = System.currentTimeMillis()): Pair<Int, Int> {
        if (anchorMonth >= ONCE_ANCHOR_MIN) {
            return Pair(anchorMonth / 12, anchorMonth % 12)
        }
        val cal = Calendar.getInstance().apply { timeInMillis = fallbackNowMillis }
        val month = if (anchorMonth in 0..11) anchorMonth else cal.get(Calendar.MONTH)
        return Pair(cal.get(Calendar.YEAR), month)
    }

    fun sanitizeAnchorMonth(
        frequency: String?,
        anchorMonth: Int,
        fallbackNowMillis: Long = System.currentTimeMillis()
    ): Int {
        return if (isOnce(frequency)) {
            val (y, m) = decodeOnceAnchor(anchorMonth, fallbackNowMillis)
            encodeOnceAnchor(y, m)
        } else {
            if (anchorMonth >= ONCE_ANCHOR_MIN) (anchorMonth % 12).coerceIn(0, 11)
            else anchorMonth.coerceIn(0, 11)
        }
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
            FREQ_ONCE -> {
                val (targetYear, targetMonth) = decodeOnceAnchor(recurring.anchorMonth, nowMillis)
                String.format(Locale.US, "ONCE-%04d-%02d-%02d", targetYear, targetMonth + 1, recurring.dayOfMonth.coerceIn(1, 31))
            }
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
            FREQ_ONCE -> {
                val (_, targetMonth) = decodeOnceAnchor(recurring.anchorMonth)
                monthZeroIndexed == targetMonth
            }
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
            FREQ_ONCE -> {
                val (targetYear, targetMonth) = decodeOnceAnchor(recurring.anchorMonth, nowMillis)
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, targetMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, recurring.dayOfMonth.coerceIn(1, maxDay))
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                nowMillis >= targetCal.timeInMillis
            }
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
     * AND within a tight 5-minute window starting at reminderHour:reminderMinute on that day,
     * provided the reminder has not already been logged or dismissed in the current period.
     * This ensures logging out/in or reinstalling later in the day never re-triggers old reminders.
     */
    fun isReminderDueAtExactTime(
        recurring: RecurringExpense,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!recurring.isActive) return false
        val currentKey = periodKeyFor(recurring, nowMillis)
        if (recurring.lastGeneratedMonth == currentKey) return false

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        if (!isExactScheduledDay(recurring, nowMillis, cal)) return false

        val currentMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val targetMinutesOfDay = recurring.reminderHour.coerceIn(0, 23) * 60 + recurring.reminderMinute.coerceIn(0, 59)
        return currentMinutesOfDay in targetMinutesOfDay..(targetMinutesOfDay + 5)
    }

    /**
     * Returns true if today is the exact scheduled day for this reminder and the configured
     * reminderHour:reminderMinute has already passed (strictly greater than target minute).
     */
    fun isReminderTimeAlreadyPassedToday(
        recurring: RecurringExpense,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        if (!isExactScheduledDay(recurring, nowMillis, cal)) return false
        val currentMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val targetMinutesOfDay = recurring.reminderHour.coerceIn(0, 23) * 60 + recurring.reminderMinute.coerceIn(0, 59)
        return currentMinutesOfDay > targetMinutesOfDay
    }

    private fun isExactScheduledDay(
        recurring: RecurringExpense,
        nowMillis: Long,
        cal: Calendar
    ): Boolean {
        val freq = normalizeFrequency(recurring.frequency)
        return when (freq) {
            FREQ_ONCE -> {
                val (targetYear, targetMonth) = decodeOnceAnchor(recurring.anchorMonth, nowMillis)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val effectiveDay = recurring.dayOfMonth.coerceIn(1, maxDay)
                cal.get(Calendar.YEAR) == targetYear &&
                    cal.get(Calendar.MONTH) == targetMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == effectiveDay
            }
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
    }

    fun calculateTargetTimestampForPeriod(recurring: RecurringExpense, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val freq = normalizeFrequency(recurring.frequency)
        when (freq) {
            FREQ_ONCE -> {
                val (targetYear, targetMonth) = decodeOnceAnchor(recurring.anchorMonth, nowMillis)
                cal.set(Calendar.YEAR, targetYear)
                cal.set(Calendar.MONTH, targetMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, recurring.dayOfMonth.coerceIn(1, maxDay))
            }
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
            FREQ_ONCE -> {
                val (targetYear, targetMonth) = decodeOnceAnchor(anchorMonth, nowMillis)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, targetMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
                    set(Calendar.HOUR_OF_DAY, safeHour)
                    set(Calendar.MINUTE, safeMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= nowMillis) -1L else cal.timeInMillis
            }
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
            FREQ_ONCE -> {
                val (targetYear, targetMonth) = decodeOnceAnchor(recurring.anchorMonth)
                if (targetYear != year || targetMonth != monthZeroIndexed) return 0
                val effectiveDay = recurring.dayOfMonth.coerceIn(1, maxDays)
                if (effectiveDay > afterDayOfMonth) 1 else 0
            }
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
        return when (normalizeFrequency(frequency)) {
            FREQ_ONCE -> Localization.tr(lang, "Once", "Μία φορά")
            FREQ_WEEKLY -> Localization.tr(lang, "Weekly", "Εβδομαδιαία")
            FREQ_BIWEEKLY -> Localization.tr(lang, "Biweekly", "Δεκαπενθήμερη")
            FREQ_QUARTERLY -> Localization.tr(lang, "Quarterly", "Τριμηνιαία")
            FREQ_YEARLY -> Localization.tr(lang, "Yearly", "Ετήσια")
            else -> Localization.tr(lang, "Monthly", "Μηνιαία")
        }
    }

    fun weekdayName(isoDow: Int, lang: Localization.Language): String {
        return when (isoDow.coerceIn(1, 7)) {
            1 -> Localization.tr(lang, "Mon", "Δευ")
            2 -> Localization.tr(lang, "Tue", "Τρί")
            3 -> Localization.tr(lang, "Wed", "Τετ")
            4 -> Localization.tr(lang, "Thu", "Πέμ")
            5 -> Localization.tr(lang, "Fri", "Παρ")
            6 -> Localization.tr(lang, "Sat", "Σάβ")
            else -> Localization.tr(lang, "Sun", "Κυρ")
        }
    }

    fun monthNameShort(monthZeroIndexed: Int, lang: Localization.Language): String {
        val en = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val el = listOf("Ιαν", "Φεβ", "Μαρ", "Απρ", "Μαΐ", "Ιουν", "Ιουλ", "Αυγ", "Σεπ", "Οκτ", "Νοε", "Δεκ")
        val idx = monthZeroIndexed.coerceIn(0, 11)
        return Localization.tr(lang, en[idx], el[idx])
    }

    fun scheduleSummary(recurring: RecurringExpense, lang: Localization.Language): String {
        return when (normalizeFrequency(recurring.frequency)) {
            FREQ_ONCE -> {
                val (y, m) = decodeOnceAnchor(recurring.anchorMonth)
                val formattedDate = String.format(Locale.US, "%02d/%02d/%04d", recurring.dayOfMonth.coerceIn(1, 31), m + 1, y)
                Localization.trFormat(lang, "Once (%1\$s)", "Μία φορά (%1\$s)", formattedDate)
            }
            FREQ_WEEKLY -> {
                val wd = weekdayName(recurring.dayOfMonth, lang)
                Localization.trFormat(lang, "Weekly (%1\$s)", "Κάθε εβδομάδα (%1\$s)", wd)
            }
            FREQ_BIWEEKLY -> {
                val wd = weekdayName(recurring.dayOfMonth, lang)
                Localization.trFormat(lang, "Biweekly (%1\$s)", "Ανά 15 ημέρες (%1\$s)", wd)
            }
            FREQ_QUARTERLY -> {
                val m = monthNameShort(recurring.anchorMonth, lang)
                Localization.trFormat(lang, "Quarterly (%1\$d, from %2\$s)", "Τριμηνιαία (%1\$dη, από %2\$s)", recurring.dayOfMonth, m)
            }
            FREQ_YEARLY -> {
                val m = monthNameShort(recurring.anchorMonth, lang)
                Localization.trFormat(lang, "Yearly (%1\$s %2\$d)", "Ετήσια (%2\$d %1\$s)", m, recurring.dayOfMonth)
            }
            else -> {
                Localization.everyDayOfMonth(lang, recurring.dayOfMonth)
            }
        }
    }
}
