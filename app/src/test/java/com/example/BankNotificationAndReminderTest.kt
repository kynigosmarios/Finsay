package com.example

import com.example.data.entity.ExpenseCategory
import com.example.data.entity.RecurringExpense
import com.example.util.BankNotificationParser
import com.example.util.RecurringScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BankNotificationAndReminderTest {

    @Test
    fun `rejects Viber Temu promotional message and non-bank apps`() {
        val temuViber = BankNotificationParser.parse(
            packageName = "com.viber.voip",
            title = "Temu",
            text = "ΑΠΙΣΤΕΥΤΟ! Αγοράστε 3 προϊόντα ΜΟΝΟ με 10€!> https://app.temu.com/t/5pBfR4s4SXB Κατάργηση εγγραφής: https://www.temu.com/t/gDy3Pe3uMYB"
        )
        assertNull("Viber promo must never be detected as bank transaction", temuViber)

        val gmailPromo = BankNotificationParser.parse(
            packageName = "com.google.android.gm",
            title = "Skrill",
            text = "Ready for effortless payments? Get 10€ bonus today!"
        )
        assertNull("Gmail newsletter must never be detected", gmailPromo)

        val revolutPromo = BankNotificationParser.parse(
            packageName = "com.revolut.revolut",
            title = "Revolut",
            text = "Invite a friend and earn up to 50€! https://revolut.com/referral"
        )
        assertNull("Bank app promo with URL must be rejected", revolutPromo)

        val bankOtp = BankNotificationParser.parse(
            packageName = "gr.eurobank.mobile.app",
            title = "Eurobank",
            text = "Κωδικός επιβεβαίωσης (OTP) 482910 για συναλλαγή 45,00 €"
        )
        assertNull("OTP approval request must be rejected", bankOtp)

        val declinedTx = BankNotificationParser.parse(
            packageName = "com.revolut.revolut",
            title = "Revolut",
            text = "Payment of €25.00 at Starbucks was declined due to insufficient funds"
        )
        assertNull("Declined transaction must be rejected", declinedTx)
    }

    @Test
    fun `parses Greek and worldwide bank expenses accurately`() {
        val gpay = BankNotificationParser.parse(
            packageName = "com.google.android.apps.walletnfcrel",
            title = "RAPO KLONTIAN KAI",
            text = "2,10 € με την κάρτα Revolut Mastercard •••• 1234"
        )
        assertNotNull(gpay)
        assertFalse(gpay!!.isIncome)
        assertEquals("Google Pay", gpay.bankName)
        assertEquals("RAPO KLONTIAN KAI", gpay.merchantName)
        assertEquals(2.10, gpay.amount, 0.001)

        val chase = BankNotificationParser.parse(
            packageName = "com.chase.sig.android",
            title = "Chase",
            text = "You spent $18.75 at Starbucks with your Visa card"
        )
        assertNotNull(chase)
        assertFalse(chase!!.isIncome)
        assertEquals("Chase", chase.bankName)
        assertEquals(18.75, chase.amount, 0.001)

        val eurobank = BankNotificationParser.parse(
            packageName = "gr.eurobank.mobile.app",
            title = "Αγορά με κάρτα",
            text = "Πληρωμή 34,50 € στην επιχείρηση ΣΚΛΑΒΕΝΙΤΗΣ. Διαθέσιμο υπόλοιπο 850,00 €"
        )
        assertNotNull(eurobank)
        assertFalse(eurobank!!.isIncome)
        assertEquals(34.50, eurobank.amount, 0.001)
    }

    @Test
    fun `detects incoming credits and deposits as income`() {
        val salaryGreek = BankNotificationParser.parse(
            packageName = "gr.nbg.mobilebanking",
            title = "Εθνική Τράπεζα",
            text = "Πιστώθηκε στον λογαριασμό σας ποσό 1.450,00 € από ACME HELLAS (Μισθοδοσία)"
        )
        assertNotNull(salaryGreek)
        assertTrue("Incoming salary must be classified as income", salaryGreek!!.isIncome)
        assertEquals(1450.0, salaryGreek.amount, 0.001)

        val wiseIncoming = BankNotificationParser.parse(
            packageName = "com.transferwise.android",
            title = "Wise",
            text = "You received €250.00 from John Smith"
        )
        assertNotNull(wiseIncoming)
        assertTrue("Incoming Wise transfer must be classified as income", wiseIncoming!!.isIncome)
        assertEquals("Wise", wiseIncoming.bankName)
        assertEquals(250.0, wiseIncoming.amount, 0.001)

        val incomeCats = listOf(
            ExpenseCategory(101L, "Μισθός", "work", 0.0, 0xFF059669L, "μισθός,μισθοδοσία,salary", 0, 0L, "INCOME"),
            ExpenseCategory(107L, "Άλλο", "other", 0.0, 0xFF475569L, "άλλο", 6, 0L, "INCOME")
        )
        val matched = BankNotificationParser.categorizeIncomeMerchant(salaryGreek.merchantName, salaryGreek.rawText, incomeCats)
        assertEquals("Μισθός", matched?.name)
    }

    @Test
    fun `reminder exact window only triggers at scheduled day and time not hours later`() {
        val reminder = RecurringExpense(
            id = 55L,
            title = "ΔΕΗ",
            amount = 90.0,
            categoryId = 3L,
            categoryName = "Λογαριασμοί & Σπίτι",
            frequency = RecurringScheduleHelper.FREQ_MONTHLY,
            dayOfMonth = 15,
            reminderHour = 10,
            reminderMinute = 0,
            isActive = true
        )
        fun at(day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(reminder, at(15, 9, 59)))
        assertTrue(RecurringScheduleHelper.isReminderDueAtExactTime(reminder, at(15, 10, 0)))
        assertTrue(RecurringScheduleHelper.isReminderDueAtExactTime(reminder, at(15, 10, 5)))
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(reminder, at(15, 10, 6)))
        assertFalse(RecurringScheduleHelper.isReminderDueAtExactTime(reminder, at(15, 18, 0)))
        assertTrue(RecurringScheduleHelper.isReminderTimeAlreadyPassedToday(reminder, at(15, 10, 1)))
        assertFalse(RecurringScheduleHelper.isReminderTimeAlreadyPassedToday(reminder, at(15, 9, 55)))
    }
}
