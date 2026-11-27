package com.example

import com.example.data.entity.RecurringExpense
import com.example.data.repository.FirestoreTransactionsDelegate
import com.example.ui.viewmodel.AdvancedExpenseFilter
import com.example.util.Localization
import com.example.util.QrCodeHelper
import com.example.util.RecurringScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NewFeaturesUnitTest {

    // ---------------------------------------------------------
    // Feature #1: Advanced Expense Filter
    // ---------------------------------------------------------

    @Test
    fun advancedExpenseFilter_tracksActiveStateAndCountAccurately() {
        val empty = AdvancedExpenseFilter()
        assertFalse(empty.isActive)
        assertEquals(0, empty.activeCount)

        val amountOnly = AdvancedExpenseFilter(minAmount = 50.0)
        assertTrue(amountOnly.isActive)
        assertEquals(1, amountOnly.activeCount)

        val amountRange = AdvancedExpenseFilter(minAmount = 50.0, maxAmount = 200.0)
        assertTrue(amountRange.isActive)
        assertEquals(1, amountRange.activeCount)

        val combined = AdvancedExpenseFilter(
            minAmount = 50.0,
            startDateMs = 1_700_000_000_000L,
            endDateMs = 1_705_000_000_000L,
            payerMemberId = 2L
        )
        assertTrue(combined.isActive)
        assertEquals(3, combined.activeCount)
    }

    // ---------------------------------------------------------
    // Feature #2: Household Invite Code & Deep-Link Parsing
    // ---------------------------------------------------------

    @Test
    fun qrCodeHelper_generatesSecureInviteCodeWithExpectedFormat() {
        val code1 = QrCodeHelper.generateSecureInviteCode()
        val code2 = QrCodeHelper.generateSecureInviteCode()
        assertTrue(code1.startsWith("FSY-"))
        assertEquals(12, code1.length)
        assertTrue(code1 != code2)
    }

    @Test
    fun qrCodeHelper_extractsInviteCodeFromWebLinkAppSchemeTextAndRawCode() {
        assertEquals(
            "FSY-ABCD1234",
            QrCodeHelper.extractInviteCode("https://oikonomos-af58b.web.app/join?code=fsy-abcd1234")
        )
        assertEquals(
            "FSY-ZX98QP23",
            QrCodeHelper.extractInviteCode("finsay://join?code=FSY-ZX98QP23&ref=qr")
        )
        assertEquals(
            "FSY-7788AABB",
            QrCodeHelper.extractInviteCode("Join my Finsay household! Code: FSY-7788AABB")
        )
        assertEquals(
            "OIKO-1234",
            QrCodeHelper.extractInviteCode("  oiko-1234  ")
        )
        assertNull(QrCodeHelper.extractInviteCode(""))
        assertNull(QrCodeHelper.extractInviteCode("ab"))
        assertNull(QrCodeHelper.extractInviteCode("https://example.com/no-code-here"))
    }

    // ---------------------------------------------------------
    // Feature #3: One-Time Reminders (FREQ_ONCE)
    // ---------------------------------------------------------

    @Test
    fun recurringScheduleHelper_encodesDecodesAndSanitizesOnceAnchor() {
        val encoded = RecurringScheduleHelper.encodeOnceAnchor(2027, Calendar.MARCH)
        val (year, month) = RecurringScheduleHelper.decodeOnceAnchor(encoded)
        assertEquals(2027, year)
        assertEquals(Calendar.MARCH, month)

        val sanitizedOnce = RecurringScheduleHelper.sanitizeAnchorMonth(
            RecurringScheduleHelper.FREQ_ONCE,
            encoded
        )
        assertEquals(encoded, sanitizedOnce)

        val sanitizedMonthly = RecurringScheduleHelper.sanitizeAnchorMonth(
            RecurringScheduleHelper.FREQ_MONTHLY,
            encoded
        )
        assertEquals(Calendar.MARCH, sanitizedMonthly)
    }

    @Test
    fun recurringScheduleHelper_onceReminderTriggersOnceAndNeverReschedulesAfterPast() {
        val targetCal = Calendar.getInstance().apply {
            set(2027, Calendar.MAY, 15, 10, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayBeforeCal = Calendar.getInstance().apply {
            set(2027, Calendar.MAY, 14, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val beforeTimeCal = Calendar.getInstance().apply {
            set(2027, Calendar.MAY, 15, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val exactWindowCal = Calendar.getInstance().apply {
            set(2027, Calendar.MAY, 15, 10, 32, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val afterCal = Calendar.getInstance().apply {
            set(2027, Calendar.MAY, 15, 11, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val onceReminder = RecurringExpense(
            id = 42L,
            title = "Car Insurance",
            amount = 220.0,
            categoryId = 1L,
            categoryName = "Bills",
            dayOfMonth = 15,
            reminderHour = 10,
            reminderMinute = 30,
            frequency = RecurringScheduleHelper.FREQ_ONCE,
            anchorMonth = RecurringScheduleHelper.encodeOnceAnchor(2027, Calendar.MAY),
            isActive = true,
            lastGeneratedMonth = ""
        )

        // Day before -> not due at all
        assertFalse(RecurringScheduleHelper.isDueNow(onceReminder, dayBeforeCal.timeInMillis))
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(onceReminder, dayBeforeCal.timeInMillis))

        // Same day before exact hour -> scheduled at exact target millis, exact reminder banner not yet due
        val nextTrigger = RecurringScheduleHelper.calculateNextTriggerTimeMillis(
            frequency = onceReminder.frequency,
            dayOfMonth = onceReminder.dayOfMonth,
            anchorMonth = onceReminder.anchorMonth,
            hour = onceReminder.reminderHour,
            minute = onceReminder.reminderMinute,
            nowMillis = beforeTimeCal.timeInMillis
        )
        assertEquals(targetCal.timeInMillis, nextTrigger)
        assertTrue(RecurringScheduleHelper.isDueNow(onceReminder, beforeTimeCal.timeInMillis))
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(onceReminder, beforeTimeCal.timeInMillis))

        // Inside 5-minute reminder window -> exact banner due, never reschedules (-1L)
        assertTrue(RecurringScheduleHelper.isReminderDueAtExactTime(onceReminder, exactWindowCal.timeInMillis))
        assertEquals(
            -1L,
            RecurringScheduleHelper.calculateNextTriggerTimeMillis(
                frequency = onceReminder.frequency,
                dayOfMonth = onceReminder.dayOfMonth,
                anchorMonth = onceReminder.anchorMonth,
                hour = onceReminder.reminderHour,
                minute = onceReminder.reminderMinute,
                nowMillis = afterCal.timeInMillis
            )
        )
        // Hours later -> exact reminder banner no longer triggers
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(onceReminder, afterCal.timeInMillis))

        // Once logged/dismissed (lastGeneratedMonth matches period key) -> no longer due
        val completed = onceReminder.copy(
            lastGeneratedMonth = RecurringScheduleHelper.periodKeyFor(onceReminder, exactWindowCal.timeInMillis)
        )
        assertFalse(RecurringScheduleHelper.isDueNow(completed, exactWindowCal.timeInMillis))
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(completed, exactWindowCal.timeInMillis))

        // Summary formatting works across EN & EL
        val summaryEn = RecurringScheduleHelper.scheduleSummary(onceReminder, Localization.Language.ENGLISH)
        val summaryEl = RecurringScheduleHelper.scheduleSummary(onceReminder, Localization.Language.GREEK)
        assertTrue(summaryEn.contains("15/05/2027"))
        assertTrue(summaryEl.contains("15/05/2027"))
    }

    // ---------------------------------------------------------
    // Feature #4: Time-Windowed Firestore Cutoff (12 Months)
    // ---------------------------------------------------------

    @Test
    fun firestoreLiveWindowCutoff_computesStartOfMonthTwelveMonthsAgo() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 27, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cutoffMs = FirestoreTransactionsDelegate.defaultLiveWindowCutoffMs(now.timeInMillis)
        val cutoffCal = Calendar.getInstance().apply { timeInMillis = cutoffMs }

        assertEquals(2025, cutoffCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cutoffCal.get(Calendar.MONTH))
        assertEquals(1, cutoffCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, cutoffCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cutoffCal.get(Calendar.MINUTE))
        assertTrue(cutoffMs < now.timeInMillis)
    }

    // ---------------------------------------------------------
    // Localization Check for New Feature Keys (All 10 Languages)
    // ---------------------------------------------------------

    @Test
    fun newFeatureStrings_haveTranslationsInAllTenLanguages() {
        val sampleKeys = listOf(
            "Advanced Filters",
            "Invite Link & QR",
            "Join via Link or QR",
            "Once",
            "Load older history"
        )
        val nonDefaultLangs = Localization.Language.entries.filter {
            it != Localization.Language.ENGLISH && it != Localization.Language.GREEK
        }
        for (lang in nonDefaultLangs) {
            for (key in sampleKeys) {
                val translated = Localization.tr(lang, key, key)
                assertNotNull(translated)
                assertTrue("Expected non-blank translation for '$key' in $lang", translated.isNotBlank())
                assertTrue("Expected localized string for '$key' in $lang (got fallback '$translated')", translated != key)
            }
        }
    }
}
