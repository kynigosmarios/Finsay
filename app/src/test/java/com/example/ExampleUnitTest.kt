package com.example

import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.data.entity.getPayerShares
import com.example.data.entity.hasMultiplePayers
import com.example.util.ExpenseReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testExpenseReportExporterCsvGeneration() {
        val expenses = listOf(
            Expense(
                id = 1,
                title = "Σούπερ μάρκετ",
                amount = 45.50,
                categoryId = 1,
                categoryName = "Σούπερ μάρκετ",
                timestamp = 1727800000000L,
                splitType = "ALL_FAMILY",
                memberNamesDisplay = "Κοινό Οικογένειας"
            ),
            Expense(
                id = 2,
                title = "Καφέδες",
                amount = 4.70,
                categoryId = 4,
                categoryName = "Καφέδες",
                timestamp = 1727805000000L,
                splitType = "SPECIFIC_MEMBERS",
                memberNamesDisplay = "Μάριος & Βασιλική"
            )
        )

        val csv = ExpenseReportExporter.generateCsv(expenses, "Οκτώβριος 2026")
        assertTrue(csv.contains("FINSAY"))
        assertTrue(csv.contains("Σούπερ μάρκετ"))
        assertTrue(csv.contains("45.50"))
        assertTrue(csv.contains("Καφέδες"))
        assertTrue(csv.contains("4.70"))
        assertTrue(csv.contains("50.20")) // Total
    }

    @Test
    fun testGreekVoiceParserAmounts() {
        val parser = com.example.util.GreekVoiceParser

        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4 ευρώ και 30 λεπτά"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4 ευρώ 30 λεπτά"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4 ευρώ και 30"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("τέσσερα ευρώ και τριάντα λεπτά"))!!, 0.01)
        assertEquals(5.30, parser.extractAmount(parser.normalizeGreek("πέντε τριάντα"))!!, 0.01)
        assertEquals(5.30, parser.extractAmount(parser.normalizeGreek("πέντε και τριάντα"))!!, 0.01)
        assertEquals(5.30, parser.extractAmount(parser.normalizeGreek("5 και 30"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4 και 30"))!!, 0.01)
        assertEquals(5.30, parser.extractAmount(parser.normalizeGreek("5:30"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4,30"))!!, 0.01)
        assertEquals(0.30, parser.extractAmount(parser.normalizeGreek("30 λεπτά"))!!, 0.01)
        assertEquals(0.50, parser.extractAmount(parser.normalizeGreek("μισό ευρώ"))!!, 0.01)
        assertEquals(0.50, parser.extractAmount(parser.normalizeGreek("πενήντα λεπτά"))!!, 0.01)
        assertEquals(540.0, parser.extractAmount(parser.normalizeGreek("πεντακόσια σαράντα"))!!, 0.01)
        assertEquals(540.0, parser.extractAmount(parser.normalizeGreek("πεντακόσια σαράντα ευρώ"))!!, 0.01)
        assertEquals(12.50, parser.extractAmount(parser.normalizeGreek("δώδεκα πενήντα"))!!, 0.01)
        assertEquals(400.0, parser.extractAmount(parser.normalizeGreek("τετρακόσια ευρώ"))!!, 0.01)
        assertEquals(377.40, parser.extractAmount(parser.normalizeGreek("τριακόσια εβδομήντα εφτά ευρώ και σαράντα λεπτά"))!!, 0.01)
        assertEquals(14726.70, parser.extractAmount(parser.normalizeGreek("δεκατέσσερις χιλιάδες επτακόσια εικοσιέξι ευρώ και εβδομήντα λεπτά"))!!, 0.01)
    }

    @Test
    fun testPayerBeneficiariesAndDescription() {
        val parser = com.example.util.GreekVoiceParser
        val categories = listOf(
            com.example.data.entity.ExpenseCategory(id = 1, name = "Καφέδες", voiceKeywords = "καφέδες, καφές"),
            com.example.data.entity.ExpenseCategory(id = 2, name = "gadgets", voiceKeywords = "gadgets, τεχνολογία")
        )
        val members = listOf(
            com.example.data.entity.FamilyMember(id = 1, name = "Μάριος", role = "Γονέας"),
            com.example.data.entity.FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας")
        )

        // Case: "Έξι ευρώ καφέδες Μάριος για Μάριο και Βασιλική"
        // Rule 6.1: "καφέδες" is the category name, so it is NOT added to title/description!
        val p1 = parser.parse("Έξι ευρώ καφέδες Μάριος για Μάριο και Βασιλική", categories, members)
        assertEquals(6.0, p1.amount, 0.01)
        assertEquals(1L, p1.payerMemberId)
        assertEquals("Μάριος", p1.payerMemberName)
        assertEquals(2, p1.memberIds.size)
        assertEquals("", p1.title)

        // Case: "Δεκατέσσερα ευρώ, πληκτρολόγιο, gadgets, Μάριος Για Βασιλική"
        val p2 = parser.parse("Δεκατέσσερα ευρώ, πληκτρολόγιο, gadgets, Μάριος Για Βασιλική", categories, members)
        assertEquals(14.0, p2.amount, 0.01)
        assertEquals(1L, p2.payerMemberId)
        assertEquals("Μάριος", p2.payerMemberName)
        assertEquals(listOf(2L), p2.memberIds)
        assertEquals(2L, p2.categoryId)
        assertEquals("gadgets", p2.categoryName)
        assertEquals("Πληκτρολόγιο", p2.title)

        // User case: "εικοσιέξι ευρώ και εβδομήντα λεπτά, σούπερ μάρκετ, στραγάλια και σταφίδες, Μάριος και Βασιλική για Μάριος Βασιλική"
        val fullCategories = categories + listOf(
            com.example.data.entity.ExpenseCategory(id = 3, name = "Σούπερ μάρκετ", voiceKeywords = "σούπερ μάρκετ, σουπερ μαρκετ, μάρκετ")
        )
        val pUser = parser.parse("εικοσιέξι ευρώ και εβδομήντα λεπτά, σούπερ μάρκετ, στραγάλια και σταφίδες, Μάριος και Βασιλική για Μάριος Βασιλική", fullCategories, members)
        assertEquals(26.70, pUser.amount, 0.01)
        assertEquals(3L, pUser.categoryId)
        assertEquals("Σούπερ μάρκετ", pUser.categoryName)
        assertEquals("Στραγάλια σταφίδες", pUser.title)
        assertEquals("Μάριος & Βασιλική", pUser.payerMemberName)
        assertEquals("1,2", pUser.payerMemberIdsCsv)
        assertEquals(listOf(1L, 2L), pUser.memberIds)
        assertEquals("Μάριος & Βασιλική", pUser.memberNamesDisplay)
    }

    @Test
    fun testRequirement61CategoryWordOmittedFromDescription() {
        val parser = com.example.util.GreekVoiceParser
        val categories = listOf(
            com.example.data.entity.ExpenseCategory(id = 1, name = "Καφέδες", voiceKeywords = "καφέδες, καφές"),
            com.example.data.entity.ExpenseCategory(id = 2, name = "Σούπερ μάρκετ", voiceKeywords = "σούπερ μάρκετ, σουπερ μαρκετ"),
            com.example.data.entity.ExpenseCategory(id = 3, name = "Ρεύμα", voiceKeywords = "ρεύμα, ρευμα, δεη")
        )
        val members = listOf(
            com.example.data.entity.FamilyMember(id = 1, name = "Μάριος", role = "Γονέας")
        )

        // Only category word spoken -> description field MUST be empty
        val resCoffee = parser.parse("15 ευρώ καφέδες", categories, members)
        assertEquals(1L, resCoffee.categoryId)
        assertEquals("", resCoffee.title)

        val resMarket = parser.parse("45 ευρώ σούπερ μάρκετ", categories, members)
        assertEquals(2L, resMarket.categoryId)
        assertEquals("", resMarket.title)

        val resPower = parser.parse("60 ευρώ ρεύμα", categories, members)
        assertEquals(3L, resPower.categoryId)
        assertEquals("", resPower.title)

        // Category word with custom description -> category word stripped, custom description kept
        val resCoffeeCustom = parser.parse("12 ευρώ καφέδες starbucks", categories, members)
        assertEquals(1L, resCoffeeCustom.categoryId)
        assertEquals("Starbucks", resCoffeeCustom.title)
    }

    @Test
    fun testRequirement62PayerPrefixes() {
        val parser = com.example.util.GreekVoiceParser
        val categories = listOf(
            com.example.data.entity.ExpenseCategory(id = 1, name = "Σούπερ μάρκετ", voiceKeywords = "σούπερ μάρκετ")
        )
        val members = listOf(
            com.example.data.entity.FamilyMember(id = 1, name = "Μάριος", role = "Γονέας"),
            com.example.data.entity.FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας")
        )

        // 1. "πληρώθηκε από"
        val r1 = parser.parse("30 ευρώ σούπερ μάρκετ πληρώθηκε από τη Βασιλική", categories, members)
        assertEquals(2L, r1.payerMemberId)
        assertEquals("Βασιλική", r1.payerMemberName)

        // 2. "πλήρωσε ο"
        val r2 = parser.parse("50 ευρώ σούπερ μάρκετ πλήρωσε ο Μάριος", categories, members)
        assertEquals(1L, r2.payerMemberId)
        assertEquals("Μάριος", r2.payerMemberName)

        // 3. "πλήρωσε η"
        val r3 = parser.parse("25 ευρώ σούπερ μάρκετ πλήρωσε η Βασιλική", categories, members)
        assertEquals(2L, r3.payerMemberId)
        assertEquals("Βασιλική", r3.payerMemberName)

        // 4. "πλήρωσαν οι"
        val r4 = parser.parse("100 ευρώ σούπερ μάρκετ πλήρωσαν οι Μάριος και Βασιλική", categories, members)
        assertEquals("Μάριος & Βασιλική", r4.payerMemberName)

        // 5. "paid by"
        val r5 = parser.parse("40 euros paid by Marios", categories, members)
        assertEquals(1L, r5.payerMemberId)
        assertEquals("Μάριος", r5.payerMemberName)
    }

    @Test
    fun testRequirement63BeneficiaryPrefixes() {
        val parser = com.example.util.GreekVoiceParser
        val categories = listOf(
            com.example.data.entity.ExpenseCategory(id = 1, name = "Δραστηριότητες", voiceKeywords = "δραστηριότητες")
        )
        val members = listOf(
            com.example.data.entity.FamilyMember(id = 1, name = "Μάριος", role = "Γονέας"),
            com.example.data.entity.FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας"),
            com.example.data.entity.FamilyMember(id = 3, name = "Μελίνα", role = "Παιδί")
        )

        // 1. "για"
        val r1 = parser.parse("20 ευρώ δραστηριότητες για Μελίνα", categories, members)
        assertEquals(listOf(3L), r1.memberIds)
        assertEquals("Μελίνα", r1.memberNamesDisplay)

        // 2. "για τον"
        val r2 = parser.parse("35 ευρώ δραστηριότητες για τον Μάριο", categories, members)
        assertEquals(listOf(1L), r2.memberIds)
        assertEquals("Μάριος", r2.memberNamesDisplay)

        // 3. "για την"
        val r3 = parser.parse("40 ευρώ δραστηριότητες για την Βασιλική", categories, members)
        assertEquals(listOf(2L), r3.memberIds)
        assertEquals("Βασιλική", r3.memberNamesDisplay)

        // 4. "για τους"
        val r4 = parser.parse("50 ευρώ δραστηριότητες για τους Μάριο και Βασιλική", categories, members)
        assertEquals(listOf(1L, 2L), r4.memberIds)
        assertEquals("Μάριος & Βασιλική", r4.memberNamesDisplay)

        // 5. "αφορά τον"
        val r5 = parser.parse("25 ευρώ δραστηριότητες αφορά τον Μάριο", categories, members)
        assertEquals(listOf(1L), r5.memberIds)
        assertEquals("Μάριος", r5.memberNamesDisplay)

        // 6. "αφορά την"
        val r6 = parser.parse("30 ευρώ δραστηριότητες αφορά την Βασιλική", categories, members)
        assertEquals(listOf(2L), r6.memberIds)
        assertEquals("Βασιλική", r6.memberNamesDisplay)

        // 7. "αφορά τους"
        val r7 = parser.parse("60 ευρώ δραστηριότητες αφορά τους Μάριο και Βασιλική", categories, members)
        assertEquals(listOf(1L, 2L), r7.memberIds)
        assertEquals("Μάριος & Βασιλική", r7.memberNamesDisplay)
    }

    @Test
    fun testRequirement3BalanceIgnoredInBankNotifications() {
        val parser = com.example.util.BankNotificationParser

        // Push notification with "υπόλοιπο" after transaction
        val tx1 = parser.parse(
            packageName = "gr.eurobank.mobile",
            title = "Eurobank",
            text = "Αγορά 15.00€ στο LIDL. Νέο υπόλοιπο: 250.00€"
        )
        org.junit.Assert.assertNotNull(tx1)
        assertEquals(15.00, tx1!!.amount, 0.01)

        // Push notification with "Υπόλοιπο" capitalized
        val tx2 = parser.parse(
            packageName = "gr.winbank.mobile",
            title = "winbank",
            text = "Εγκρίθηκε συναλλαγή 34,50€ στο IKEA. Υπόλοιπο λογαριασμού 890,20€"
        )
        org.junit.Assert.assertNotNull(tx2)
        assertEquals(34.50, tx2!!.amount, 0.01)

        // Push notification with "balance"
        val tx3 = parser.parse(
            packageName = "com.revolut.revolut",
            title = "Revolut",
            text = "You paid €18.20 at Shell. Balance: €450.00"
        )
        org.junit.Assert.assertNotNull(tx3)
        assertEquals(18.20, tx3!!.amount, 0.01)

        // Notification that ONLY contains balance inquiry -> NOT an expense!
        val txBalanceOnly = parser.parse(
            packageName = "gr.alphabank.mobilebanking",
            title = "Alpha Bank",
            text = "Το διαθέσιμο υπόλοιπο της κάρτας σας είναι 350.00 EUR"
        )
        org.junit.Assert.assertNull(txBalanceOnly)
    }

    @Test
    fun testRequirement5BankNotificationCategorization() {
        val parser = com.example.util.BankNotificationParser
        val categories = listOf(
            com.example.data.entity.ExpenseCategory(id = 10, name = "Σούπερ μάρκετ", voiceKeywords = "σούπερ μάρκετ"),
            com.example.data.entity.ExpenseCategory(id = 20, name = "Έξοδα σπιτιού", voiceKeywords = "έπιπλα, σπίτι"),
            com.example.data.entity.ExpenseCategory(id = 30, name = "Μετακινήσεις & Καύσιμα", voiceKeywords = "βενζίνη, καύσιμα")
        )

        // IKEA -> should categorize to "Έξοδα σπιτιού"
        val catIkea = parser.categorizeMerchant("IKEA", "Αγορά 80.00€ στο IKEA", categories)
        org.junit.Assert.assertNotNull(catIkea)
        assertEquals(20L, catIkea!!.id)

        // Lidl -> should categorize to "Σούπερ μάρκετ"
        val catLidl = parser.categorizeMerchant("LIDL", "Αγορά 25.00€ στο κατάστημα LIDL", categories)
        org.junit.Assert.assertNotNull(catLidl)
        assertEquals(10L, catLidl!!.id)

        // Shell -> should categorize to "Μετακινήσεις & Καύσιμα"
        val catShell = parser.categorizeMerchant("Shell", "Πληρωμή 40.00€ Shell", categories)
        org.junit.Assert.assertNotNull(catShell)
        assertEquals(30L, catShell!!.id)

        // Unknown merchant that cannot be categorized -> returns null!
        val catUnknown = parser.categorizeMerchant("XYZ Unknown", "Συναλλαγή 15.00€", categories)
        org.junit.Assert.assertNull(catUnknown)
    }

    @Test
    fun testMultiPayerSharesCalculation() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μπαμπάς", role = "Γονέας"),
            FamilyMember(id = 2, name = "Μαμά", role = "Γονέας")
        )

        // Equal split test (40 € split equally between 2 members)
        val expEqual = Expense(
            id = 10,
            title = "Παιχνίδι",
            amount = 40.0,
            categoryId = 1,
            categoryName = "Παιδιά",
            splitType = "ALL_FAMILY",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "",
            payerMemberName = "Μπαμπάς & Μαμά"
        )
        val sharesEqual = expEqual.getPayerShares(members)
        assertEquals(2, sharesEqual.size)
        assertEquals(20.0, sharesEqual[0].amount, 0.01)
        assertEquals(20.0, sharesEqual[1].amount, 0.01)
        assertEquals("Μπαμπάς", sharesEqual[0].memberName)
        assertEquals("Μαμά", sharesEqual[1].memberName)

        // Custom split test (40 € with 30 € from dad and 10 € from mom)
        val expCustom = Expense(
            id = 11,
            title = "Ρούχα παιδιού",
            amount = 40.0,
            categoryId = 1,
            categoryName = "Παιδιά",
            splitType = "SPECIFIC_MEMBERS",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:30.0,2:10.0",
            payerMemberName = "Μπαμπάς (30 €) & Μαμά (10 €)"
        )
        val sharesCustom = expCustom.getPayerShares(members)
        assertEquals(2, sharesCustom.size)
        assertEquals(30.0, sharesCustom[0].amount, 0.01)
        assertEquals(10.0, sharesCustom[1].amount, 0.01)
        assertTrue(expCustom.hasMultiplePayers())

        // Single payer backward-compatibility test
        val expSingle = Expense(
            id = 12,
            title = "Βενζίνη",
            amount = 50.0,
            categoryId = 2,
            categoryName = "Μετακινήσεις",
            splitType = "ALL_FAMILY",
            payerMemberId = 1,
            payerMemberName = "Μπαμπάς"
        )
        val sharesSingle = expSingle.getPayerShares(members)
        assertEquals(1, sharesSingle.size)
        assertEquals(50.0, sharesSingle[0].amount, 0.01)
        assertEquals("Μπαμπάς", sharesSingle[0].memberName)
    }

    @Test
    fun testSettleUpCalculator() {
        val marios = FamilyMember(id = 1, name = "Μάριος", role = "Γονέας")
        val vasiliki = FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας")
        val members = listOf(marios, vasiliki)

        // Marios pays 100€ for an ALL_FAMILY expense (shared 50-50)
        val exp1 = Expense(
            id = 1,
            title = "Σούπερ Μάρκετ",
            amount = 100.0,
            categoryId = 1,
            categoryName = "Σούπερ Μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1,
            payerMemberName = "Μάριος"
        )

        // Vasiliki pays 20€ for an ALL_FAMILY expense (shared 50-50)
        val exp2 = Expense(
            id = 2,
            title = "Φούρνος",
            amount = 20.0,
            categoryId = 1,
            categoryName = "Φούρνος",
            splitType = "ALL_FAMILY",
            payerMemberId = 2,
            payerMemberName = "Βασιλική"
        )

        val result = com.example.util.SettleUpCalculator.calculate(listOf(exp1, exp2), members)
        assertEquals(120.0, result.totalHouseholdExpenses, 0.01)

        val mariosSummary = result.memberSummaries.find { it.member.id == 1L }!!
        val vasilikiSummary = result.memberSummaries.find { it.member.id == 2L }!!

        // Total expenses 120€, each person's fair share is 60€
        // Marios paid 100€ -> net = +40€
        // Vasiliki paid 20€ -> net = -40€
        assertEquals(100.0, mariosSummary.totalPaid, 0.01)
        assertEquals(60.0, mariosSummary.totalShare, 0.01)
        assertEquals(40.0, mariosSummary.netBalance, 0.01)

        assertEquals(20.0, vasilikiSummary.totalPaid, 0.01)
        assertEquals(60.0, vasilikiSummary.totalShare, 0.01)
        assertEquals(-40.0, vasilikiSummary.netBalance, 0.01)

        // Settlement transfer: Vasiliki -> Marios 40€
        assertEquals(1, result.transfers.size)
        assertEquals(2L, result.transfers[0].fromMember.id)
        assertEquals(1L, result.transfers[0].toMember.id)
        assertEquals(40.0, result.transfers[0].amount, 0.01)

        val summaryText = com.example.util.SettleUpCalculator.formatSummaryText(result, "Σπίτι", true)
        assertTrue(summaryText.contains("Βασιλική ➜ Μάριος"))
        assertTrue(summaryText.contains("40,00 €"))
    }

    @Test
    fun testReceiptParserOcrAndQr() {
        // Greek supermarket receipt OCR mock
        val sampleReceiptText = """
            ΣΚΛΑΒΕΝΙΤΗΣ
            Ι. & Σ. ΣΚΛΑΒΕΝΙΤΗΣ Α.Ε.Ε.
            ΑΦΜ: 094056252
            ΗΜ/ΝΙΑ: 14/03/2025  12:45
            ΓΑΛΑ ΦΡΕΣΚΟ          1,85
            ΨΩΜΙ ΤΟΣΤ            2,10
            ΤΥΡΙ ΦΕΤΑ            7,50
            ΣΥΝΟΛΟ ΕΥΡΩ         11,45
            ΚΑΡΤΑ VISA          11,45
        """.trimIndent()

        val parsedOcr = com.example.util.ReceiptParser.parseOcrText(sampleReceiptText)
        assertEquals("Σκλαβενίτης", parsedOcr.title)
        assertEquals(11.45, parsedOcr.amount, 0.01)
        assertEquals("Σούπερ Μάρκετ", parsedOcr.suggestedCategoryName)

        // AADE QR code string
        val aadeQr = "https://mydatainfo.aade.gr/receipt?tin=094056252&amount=24.80&date=20250314"
        val parsedQr = com.example.util.ReceiptParser.parseAadeQr(aadeQr)
        assertEquals(24.80, parsedQr.amount, 0.01)
        assertEquals("094056252", parsedQr.merchantAfm)

        // Test 1: Store name at top of receipt with receipt header before it
        val bakeryReceipt = """
            ΑΠΟΔΕΙΞΗ ΛΙΑΝΙΚΗΣ ΠΩΛΗΣΗΣ
            ΦΟΥΡΝΟΣ ΒΕΝΕΤΗ
            ΠΑΝΕΠΙΣΤΗΜΙΟΥ 52, ΑΘΗΝΑ
            ΑΦΜ: 094112233  ΔΟΥ: Δ' ΑΘΗΝΩΝ
            ΤΗΛ: 210-3612345
            2x ΚΟΥΛΟΥΡΙ         1,40
            1x ΕΣΠΡΕΣΣΟ         2,20
            ΣΥΝΟΛΟ:             3,60
            ΜΕΤΡΗΤΑ:           10,00
            ΡΕΣΤΑ:              6,40
        """.trimIndent()
        val parsedBakery = com.example.util.ReceiptParser.parseOcrText(bakeryReceipt)
        assertEquals("Φούρνος Βενέτη", parsedBakery.title)
        assertEquals(3.60, parsedBakery.amount, 0.01) // ONLY right of ΣΥΝΟΛΟ, never 10.00 or 6.40

        // Test 2: Unnamed shop with generic Greek store name at top
        val tavernReceipt = """
            ΨΗΤΟΠΩΛΕΙΟ Ο ΜΠΑΜΠΗΣ
            ΠΑΠΑΔΟΠΟΥΛΟΣ ΧΑΡΑΛΑΜΠΟΣ
            ΑΓ. ΜΑΡΙΝΑΣ 14, ΙΛΙΟΝ
            ΑΦΜ: 123456789
            ΣΥΝΟΛΟ 28,50 €
        """.trimIndent()
        val parsedTavern = com.example.util.ReceiptParser.parseOcrText(tavernReceipt)
        assertTrue(parsedTavern.title.contains("Ψητοπωλείο", ignoreCase = true))
        assertEquals(28.50, parsedTavern.amount, 0.01)

        // Test 3: No ΣΥΝΟΛΟ keyword -> must return 0.0 (NO random fallback numbers)
        val receiptWithoutSynolo = """
            ΒΙΒΛΙΟΠΩΛΕΙΟ ΕΣΤΙΑ
            ΣΟΛΩΝΟΣ 60, ΑΘΗΝΑ
            ΑΦΜ 091234567
            ΤΗΛ: 2103600000
            ΒΙΒΛΙΟ                18,00
        """.trimIndent()
        val parsedNoSynolo = com.example.util.ReceiptParser.parseOcrText(receiptWithoutSynolo)
        assertEquals(0.0, parsedNoSynolo.amount, 0.01) // STRICT: 0.0 because no ΣΥΝΟΛΟ

        // Test 4: Real user Lidl receipt with LGDL logo OCR, ΛΙΝΤΛ name, and ΣΥΝΟΛΟ 47,98
        val userLidlReceipt = """
            LGDL
            ΛΙΝΤΛ ΕΛΛΑΣ ΚΑΙ ΣΙΑ ΟΕ, ΥΠΕΡΑΓΟΡΑ
            ΚΟΥΝΤΟΥΡΙΩΤΗ 8
            55535, ΠΥΛΑΙΑ
            ΑΦΜ 093683423

            ΦΟΡΟΛΟΓΙΚΗ ΑΠΟΔΕΙΞΗ ΕΝΑΡΞΗ
            ΞΥΛ.ΣΕΤ ΠΑΙΧ-0524746         7,99  24.00%
            Π.ΞΥΛΙΝΟ ΜΠΑ-0486017       49,99  24.00%
                   έκπτωση Lidl Plus   -10,00

            ΣΥΝΟΛΟ                      47,98
            ΚΑΡΤΑ                       47,98

            ΦΠΑ%        ΚΑΘΑΡΟ   +   ΦΠΑ   =   ΜΙΚΤΟ
            C 24.00%     38,69      9,29      47,98
            ΣΥΝΟΛΟ       38,69      9,29      47,98

            Συνολική εξοικονόμηση
                 10,00 ευρώ

            Με το Lidl Plus εξοικονομήθηκαν
                 10,00 ευρώ

            ΣΕΙΡΑ: 000482            ΑΡΙΘΜΟΣ: 32775
        """.trimIndent()

        val parsedLidl = com.example.util.ReceiptParser.parseOcrText(userLidlReceipt)
        assertEquals("Λιντλ", parsedLidl.title) // NEVER "LGDL"
        assertEquals(47.98, parsedLidl.amount, 0.01) // Correctly extracts 47.98 from ΣΥΝΟΛΟ
        assertEquals("Σούπερ Μάρκετ", parsedLidl.suggestedCategoryName)
        assertEquals("093683423", parsedLidl.merchantAfm)

        // Test 5: Real user receipt when ML Kit Latin OCR reads ΣΥΝΟΛΟ as EYNOAO or ZYNOAO
        val latinOcrLidlReceipt = """
            LGDL
            AINTA EAAAE KAI EIA OE, YNEPAEOPA
            KOYNTOYPIQTH 8
            55535, NYAAIA
            AQM 093683423

            EYNOAO                      47,98
            KAPTA                       47,98
        """.trimIndent()

        val parsedLatinLidl = com.example.util.ReceiptParser.parseOcrText(latinOcrLidlReceipt)
        assertEquals("Λιντλ", parsedLatinLidl.title)
        assertEquals(47.98, parsedLatinLidl.amount, 0.01) // Robust across EYNOAO / ZYNOAO

        // Test 6: Small neighborhood grocery with logo noise at top
        val groceryReceipt = """
            &&&
            ΠΑΝΤΟΠΩΛΕΙΟ Η ΓΩΝΙΑ
            ΕΜΠΟΡΙΟ ΤΡΟΦΙΜΩΝ ΚΑΙ ΠΟΤΩΝ
            ΟΔΟΣ ΣΟΛΩΝΟΣ 20
            ΑΦΜ: 082345678
            ΣΥΝΟΛΟ : 16,30
        """.trimIndent()
        val parsedGrocery = com.example.util.ReceiptParser.parseOcrText(groceryReceipt)
        assertTrue("Expected title to contain 'Παντοπωλείο Η Γωνία', got '${parsedGrocery.title}'",
            parsedGrocery.title.contains("Παντοπωλείο", ignoreCase = true))
        assertEquals(16.30, parsedGrocery.amount, 0.01)

        // Test 7: Pharmacy receipt
        val pharmacyReceipt = """
            ΦΑΡΜΑΚΕΙΟ ΚΑΡΑΓΙΑΝΝΗ ΜΑΡΙΑ
            ΑΧΑΡΝΩΝ 120, ΑΘΗΝΑ
            ΑΦΜ 045678912
            ΣΥΝΟΛΟ 8,50 €
        """.trimIndent()
        val parsedPharmacy = com.example.util.ReceiptParser.parseOcrText(pharmacyReceipt)
        assertTrue(parsedPharmacy.title.contains("Φαρμακείο", ignoreCase = true))
        assertEquals(8.50, parsedPharmacy.amount, 0.01)
        assertEquals("Υγεία & Φαρμακείο", parsedPharmacy.suggestedCategoryName)

        // Test 8: Gas station with spaced 'Σ Υ Ν Ο Λ Ο'
        val gasReceipt = """
            ΠΡΑΤΗΡΙΟ ΥΓΡΩΝ ΚΑΥΣΙΜΩΝ
            ΠΑΠΑΔΟΠΟΥΛΟΣ ΓΕΩΡΓΙΟΣ ΚΑΙ ΣΙΑ Ε.Ε.
            ΛΕΩΦ. ΒΟΥΛΙΑΓΜΕΝΗΣ 210
            ΑΦΜ 091827364
            ΑΜΟΛΥΒΔΗ 95 30,00
            Σ Υ Ν Ο Λ Ο 30,00
        """.trimIndent()
        val parsedGas = com.example.util.ReceiptParser.parseOcrText(gasReceipt)
        assertTrue(parsedGas.title.contains("Πρατήριο", ignoreCase = true))
        assertEquals(30.00, parsedGas.amount, 0.01)
        assertEquals("Μετακινήσεις", parsedGas.suggestedCategoryName)

        // Test 9: Cafe with English 'TOTAL'
        val cafeReceipt = """
            COFFEE LAB
            MITROPOLEOS 14
            FREDDO ESPRESSO 2,20
            TOTAL EUR 2.20
        """.trimIndent()
        val parsedCafe = com.example.util.ReceiptParser.parseOcrText(cafeReceipt)
        assertEquals("Coffee Lab", parsedCafe.title)
        assertEquals(2.20, parsedCafe.amount, 0.01)
        assertEquals("Καφές & Έξω", parsedCafe.suggestedCategoryName)

        // Test 10: Real Sklavenitis receipt with register metadata "ΤΑΜΕΙΟ 02 ΚΑΤ: 3624 ΧΕΙΡΙΣΤΗΣ 29900"
        val sklavenitisReceipt = """
            ΕΛΛΗΝΙΚΕΣ ΥΠΕΡΑΓΟΡΕΣ
            ΣΚΛΑΒΕΝΙΤΗΣ Α.Ε.Ε.
            SUPER MARKETS
            Υ/Α ΤΣΑΛΔΑΡΗ & ΒΑΣΙΛΙΚΗΣ ΤΑΒΑΚΗ 25
            ΘΕΡΜΗ ΤΗΛ:2310487590
            ΑΦΜ 800764388
            ΤΑΜΕΙΟ 02 ΚΑΤ: 3624       ΧΕΙΡΙΣΤΗΣ 29900
            ΑΠΟΔΕΙΞΗ ΛΙΑΝΙΚΗΣ ΠΩΛΗΣΗΣ
            5201321933830
            ROL ΥΓΡΟ BLACK 30ΠΛΥΣΕΙΣ      4,65 24,00%
            ΣΥΝΟΛΟ                   28,16 ΕΥΡΩ
            ΜΕΤΡΗΤΑ                  28,20
            ΡΕΣΤΑ                    -0,04
            ΠΩΛΗΘΕΝΤΑ ΕΙΔΗ :         13
        """.trimIndent()
        val parsedSklavenitis = com.example.util.ReceiptParser.parseOcrText(sklavenitisReceipt)
        assertEquals("Σκλαβενίτης", parsedSklavenitis.title)
        assertEquals(28.16, parsedSklavenitis.amount, 0.01)
        assertEquals("Σούπερ Μάρκετ", parsedSklavenitis.suggestedCategoryName)

        // Test 11: Real Sklavenitis receipt with Latin OCR artifacts
        val sklavenitisOcrLatin = """
            EAAHNIKEC YTEPATOPEL
            EKAABENITHC A.E.E.
            SUPER MARKETS
            Y/A TSAADARH & BASIAIKHS TABAKH 25
            AFM 800764388
            TAMEIO 02 KAT 3624
            XEIRISTIS 29900
            APODEIXH AIANIKHS PWAHSHS
            EYNOAO                   28,16 EYRW
            METRHTA                  28,20
            RESTA                    -0,04
        """.trimIndent()
        val parsedSklavenitisLatin = com.example.util.ReceiptParser.parseOcrText(sklavenitisOcrLatin)
        assertEquals("Σκλαβενίτης", parsedSklavenitisLatin.title)
        assertEquals(28.16, parsedSklavenitisLatin.amount, 0.01)

        // Test 12: Real Dedeman / Praktiker receipt with 'ΣΥΝΟΛΟ ΤΕΜΑΧΙΩΝ 3' and 'Σύνολο 11,70 €'
        val praktikerReceipt = """
            DEDEMAN
            DEDEMAN HELLAS A.E.
            YP/MA: MARINOY ANTYPA, 551 34 OESNIKH
            EDRA: PEIRAIWS 176 - 17778 TAYROS
            AFM: 094317955
            THL: 2310 489550 - WWW.PRAKTIKER.GR
            ALLAGES ME APODEIXH ENTOS 14 HMERWN
            Eidos Parastatikoy
            APODEIXH TAMEIAKHS MHXANHS
            Ar. Parastatikoy: ATM305006-00443034
            Hmeromhnia: 07/11/26 15:28:42
            Xeirisths: 20100950 - Termatiko: 305006
            4048962257243          2  3,90 €   7,80 €  24,00 %
            399918-BYSMA OYPAT 6X50MM FISCHER DUOPO
            4048962237061          1  3,90 €   3,90 €  24,00 %
            399916-BYSMA OYPAT 6X30MM FISCHER DUOPO
            ΣΥΝΟΛΟ ΤΕΜΑΧΙΩΝ
            3
            ΦΠΑ    Καθαρή Αξία    Αξία ΦΠΑ    Συνολική Αξία
            24        8,43 €        2,27 €       11,70 €
            ----------------------------------------
            Σύνολο                               11,70 €
            ----------------------------------------
            ΜΕΤΡΗΤΑ                              20,00 €
            ΡΕΣΤΑ                                -8,30 €
        """.trimIndent()
        val parsedPraktiker = com.example.util.ReceiptParser.parseOcrText(praktikerReceipt)
        assertTrue(parsedPraktiker.title.contains("Praktiker", ignoreCase = true) || parsedPraktiker.title.contains("Dedeman", ignoreCase = true))
        assertEquals(11.70, parsedPraktiker.amount, 0.01)

        // Test 13: Dedeman / Praktiker with OCR lowercase 'Evvolo 11,70 €' and 'EYNOAO TEMAXIWN'
        val praktikerOcrLatin = """
            DEDEMAN HELLAS A.E.
            AFM: 094317955
            WWW.PRAKTIKER.GR
            APODEIXH TAMEIAKHS MHXANHS
            EYNOAO TEMAXIWN
            3
            FPA    Ka0arh Axia    Axia FPA    Synolikh Axia
            24        8,43 €        2,27 €       11,70 €
            ----------------------------------------
            Evvolo                               11,70 €
            ----------------------------------------
            METRHTA                              20,00 €
            RESTA                                -8,30 €
        """.trimIndent()
        val parsedPraktikerLatin = com.example.util.ReceiptParser.parseOcrText(praktikerOcrLatin)
        assertEquals(11.70, parsedPraktikerLatin.amount, 0.01)

        // Test 14: Dedeman / Praktiker with 2-line separation (Evvolo on line 1, 11,70 € on line 2)
        val praktikerTwoLines = """
            DEDEMAN HELLAS A.E.
            AFM: 094317955
            WWW.PRAKTIKER.GR
            EYNOAO TEMAXIWN
            3
            ----------------------------------------
            Evvolo
            11,70 €
            ----------------------------------------
            METRHTA 20,00 €
        """.trimIndent()
        val parsedPraktikerTwoLines = com.example.util.ReceiptParser.parseOcrText(praktikerTwoLines)
        assertEquals(11.70, parsedPraktikerTwoLines.amount, 0.01)

        // Test 15: Exact Sklavenitis receipt from user photo (disqualifies Tameio 02 Kat 3624, identifies Sklavenitis & 28.16)
        val sklavenitisWithCashierReceipt = """
            ΕΛΛΗΝΙΚΕΣ ΥΠΕΡΑΓΟΡΕΣ
            ΣΚΛΑΒΕΝΙΤΗΣ Α.Ε.Ε.
            SUPER MARKETS
            Υ/Α ΤΣΑΛΔΑΡΗ & ΒΑΣΙΛΙΚΗΣ ΤΑΒΑΚΗ 25
            ΘΕΡΜΗ ΤΗΛ:2310487590
            ΑΦΜ 800764388

            ΤΑΜΕΙΟ 02 ΚΑΤ: 3624          ΧΕΙΡΙΣΤΗΣ 29900

            ΑΠΟΔΕΙΞΗ ΛΙΑΝΙΚΗΣ ΠΩΛΗΣΗΣ
            5201321933830
            ROL ΥΓΡΟ BLACK 30ΠΛΥΣΕΙΣ      4,65 24,00%
            ΕΚΠΤΩΣΗ ROL ΥΓΡΟ BLACK 30ΠΛ  -0,70 24,00%
            ----------------------------------------
            ΣΥΝΟΛΟ                        28,16 ΕΥΡΩ
            ΜΕΤΡΗΤΑ                       28,20
            ΡΕΣΤΑ                         -0,04
            ΠΩΛΗΘΕΝΤΑ ΕΙΔΗ : 13
        """.trimIndent()
        val parsedSklavenitisWithCashier = com.example.util.ReceiptParser.parseOcrText(sklavenitisWithCashierReceipt)
        assertEquals("Σκλαβενίτης", parsedSklavenitisWithCashier.title)
        assertEquals(28.16, parsedSklavenitisWithCashier.amount, 0.01)
        assertEquals("800764388", parsedSklavenitisWithCashier.merchantAfm)

        // Test 16: Independent local bakery (generic scoring + clean title + total)
        val localBakeryExtraReceipt = """
            ΑΡΤΟΠΟΙΕΙΟ Ο ΦΟΥΡΝΟΣ ΤΗΣ ΓΙΑΓΙΑΣ Ο.Ε.
            ΕΔΡΑ: ΑΓΙΟΥ ΔΗΜΗΤΡΙΟΥ 45, ΘΕΣΣΑΛΟΝΙΚΗ
            ΑΦΜ: 123456789 - ΔΟΥ: Δ' ΘΕΣΣΑΛΟΝΙΚΗΣ
            ΑΠΟΔΕΙΞΗ ΛΙΑΝΙΚΗΣ ΠΩΛΗΣΗΣ
            ΚΟΥΛΟΥΡΙ ΘΕΣΣΑΛΟΝΙΚΗΣ 2 X 0,80    1,60 €
            ΤΥΡΟΠΙΤΑ ΤΑΨΙΟΥ 1 X 2,40           2,40 €
            ΚΑΦΕΣ CAPPUCCINO 2 X 2,20          4,40 €
            ----------------------------------------
            ΣΥΝΟΛΟ                             8,40 €
            ΜΕΤΡΗΤΑ                           10,00 €
            ΡΕΣΤΑ                             -1,60 €
        """.trimIndent()
        val parsedBakeryExtra = com.example.util.ReceiptParser.parseOcrText(localBakeryExtraReceipt)
        assertTrue(parsedBakeryExtra.title.contains("Φούρνος", ignoreCase = true) && parsedBakeryExtra.title.contains("Γιαγια", ignoreCase = true))
        assertEquals(8.40, parsedBakeryExtra.amount, 0.01)
        assertEquals("123456789", parsedBakeryExtra.merchantAfm)

        // Test 17: Independent local pharmacy (generic health categorization + total)
        val localPharmacyExtraReceipt = """
            ΦΑΡΜΑΚΕΙΟ ΓΕΩΡΓΙΟΣ ΠΑΠΑΔΟΠΟΥΛΟΣ
            ΤΣΙΜΙΣΚΗ 88, ΤΚ 54622
            ΑΦΜ: 987654321
            ΑΠΟΔΕΙΞΗ ΛΙΑΝΙΚΗΣ
            DEPON MAXIMUM 1000MG              3,50 €
            VITAMIN C 1000MG EFFERV           6,35 €
            ΑΝΤΙΗΛΙΑΚΗ ΚΡΕΜΑ 50+             10,00 €
            ----------------------------------------
            ΣΥΝΟΛΟ                           19,85 €
            ΠΛΗΡΩΜΗ ΜΕ ΚΑΡΤΑ                 19,85 €
        """.trimIndent()
        val parsedPharmacyExtra = com.example.util.ReceiptParser.parseOcrText(localPharmacyExtraReceipt)
        assertTrue(parsedPharmacyExtra.title.contains("Φαρμακείο", ignoreCase = true) && parsedPharmacyExtra.title.contains("Παπαδοπουλος", ignoreCase = true))
        assertEquals(19.85, parsedPharmacyExtra.amount, 0.01)

        // Test 18: Independent grill house / tavern
        val localTavernExtraReceipt = """
            ΨΗΤΟΠΩΛΕΙΟ Ο ΜΠΑΜΠΗΣ
            ΑΦΜ 112233445
            ΜΕΡΙΔΑ ΓΥΡΟΣ ΧΟΙΡΙΝΟΣ             8,50 €
            ΣΟΥΒΛΑΚΙ ΚΑΛΑΜΑΚΙ 4 X 2,20        8,80 €
            ΠΑΤΑΤΕΣ ΤΗΓΑΝΗΤΕΣ 1 X 3,80        3,80 €
            ΧΩΡΙΑΤΙΚΗ ΣΑΛΑΤΑ 1 X 7,50         7,50 €
            ΑΝΑΨΥΚΤΙΚΑ 2 X 2,80               5,60 €
            ----------------------------------------
            Σύνολο                           34,20 €
            ΜΕΤΡΗΤΑ                          50,00 €
            ΡΕΣΤΑ                           -15,80 €
        """.trimIndent()
        val parsedTavernExtra = com.example.util.ReceiptParser.parseOcrText(localTavernExtraReceipt)
        assertTrue(parsedTavernExtra.title.contains("Ψητοπωλείο", ignoreCase = true) && parsedTavernExtra.title.contains("Μπαμπ", ignoreCase = true))
        assertEquals(34.20, parsedTavernExtra.amount, 0.01)
    }

    @Test
    fun testBankNotificationParser() {
        val parser = com.example.util.BankNotificationParser

        // Eurobank notification
        val eurobankTx = parser.parse(
            packageName = "gr.eurobank.mobile",
            title = "Eurobank Cards",
            text = "Συναλλαγή 14,50€ με κάρτα στο SKLAVENITIS"
        )
        assertTrue(eurobankTx != null)
        assertEquals(14.50, eurobankTx!!.amount, 0.01)
        assertEquals("SKLAVENITIS", eurobankTx.merchantName)
        assertEquals("Eurobank", eurobankTx.bankName)

        // Piraeus Bank notification
        val piraeusTx = parser.parse(
            packageName = "gr.winbank.mobile",
            title = "winbank",
            text = "Αγορά 25,00€ με την κάρτα σας στο κατάστημα LIDL"
        )
        assertTrue(piraeusTx != null)
        assertEquals(25.00, piraeusTx!!.amount, 0.01)
        assertEquals("LIDL", piraeusTx.merchantName)
        assertEquals("Τράπεζα Πειραιώς", piraeusTx.bankName)

        // Revolut notification
        val revolutTx = parser.parse(
            packageName = "com.revolut.revolut",
            title = "Revolut",
            text = "Ξοδέψατε 4,80 € στο Coffee Island"
        )
        assertTrue(revolutTx != null)
        assertEquals(4.80, revolutTx!!.amount, 0.01)
        assertEquals("Coffee Island", revolutTx.merchantName)
        assertEquals("Revolut", revolutTx.bankName)

        // Incoming funds should NOT be parsed as expenses
        val incomingRevolut = parser.parse(
            packageName = "com.revolut.revolut",
            title = "Revolut",
            text = "You received 50.00 € from John"
        )
        org.junit.Assert.assertNull(incomingRevolut)

        val incomingEurobank = parser.parse(
            packageName = "gr.eurobank.mobile",
            title = "Eurobank",
            text = "Λάβατε μεταφορά 120,00€ από ΓΕΩΡΓΙΟΣ"
        )
        org.junit.Assert.assertNull(incomingEurobank)

        // Exact Eurobank e-commerce notification from user screenshot:
        // Package: gr.eurobank.mobile.app
        // Title: "Αγορά/Πληρωμή e-commerce"
        // Text: "Κάνατε αγορά/πληρωμή ηλεκτρονικά 12,49 EUR με την κάρτα 5167 32** **** 8483 στην εταιρεία PAYPAL *NETFLIX COM DE 11/11/2026, 04:11"
        val eurobankEcommerceTx = parser.parse(
            packageName = "gr.eurobank.mobile.app",
            title = "Αγορά/Πληρωμή e-commerce",
            text = "Κάνατε αγορά/πληρωμή ηλεκτρονικά 12,49 EUR με την κάρτα 5167 32** **** 8483 στην εταιρεία PAYPAL *NETFLIX COM DE 11/11/2026, 04:11"
        )
        org.junit.Assert.assertNotNull(eurobankEcommerceTx)
        assertEquals(12.49, eurobankEcommerceTx!!.amount, 0.01)
        assertTrue(eurobankEcommerceTx.merchantName.equals("Netflix", ignoreCase = true))
        assertEquals("Eurobank", eurobankEcommerceTx.bankName)

        // Categorization for Netflix into user's categories
        val userCategories = listOf(
            com.example.data.entity.ExpenseCategory(id = 1, name = "Σούπερ μάρκετ"),
            com.example.data.entity.ExpenseCategory(id = 2, name = "Φαγητό από έξω"),
            com.example.data.entity.ExpenseCategory(id = 3, name = "Καφέδες"),
            com.example.data.entity.ExpenseCategory(id = 4, name = "Ψιλικά"),
            com.example.data.entity.ExpenseCategory(id = 5, name = "Μετακινήσεις & Καύσιμα"),
            com.example.data.entity.ExpenseCategory(id = 6, name = "Γιατρός & Φαρμακείο"),
            com.example.data.entity.ExpenseCategory(id = 7, name = "Διασκέδαση & Ψυχαγωγία"),
            com.example.data.entity.ExpenseCategory(id = 8, name = "Στοίχημα")
        )
        val cat = parser.categorizeMerchant(eurobankEcommerceTx.merchantName, eurobankEcommerceTx.rawText, userCategories)
        org.junit.Assert.assertNotNull(cat)
        assertEquals(7L, cat!!.id)
        assertEquals("Διασκέδαση & Ψυχαγωγία", cat.name)
    }

    @Test
    fun testVoiceInputKeywordDescriptionPreserved() {
        val parser = com.example.util.GreekVoiceParser
        val categories = listOf(
            com.example.data.entity.ExpenseCategory(
                id = 1,
                name = "Σούπερ μάρκετ",
                voiceKeywords = "σουπερ μαρκετ, κρεοπωλείο, κρεοπωλειο, μαναβικο, σκλαβενιτης, lidl"
            ),
            com.example.data.entity.ExpenseCategory(
                id = 2,
                name = "Φαγητό έξω",
                voiceKeywords = "φαγητο, ταβερνα, εστιατοριο"
            )
        )
        val members = listOf(
            com.example.data.entity.FamilyMember(id = 1, name = "Μάριος", role = "Γονέας")
        )

        // "43 ευρώ κρεοπωλείο"
        val parsed = parser.parse("43 ευρώ κρεοπωλείο", categories, members)
        assertEquals(43.0, parsed.amount, 0.01)
        assertEquals(1L, parsed.categoryId)
        assertTrue("Expected title to contain 'Κρεοπωλείο', got: '${parsed.title}'", parsed.title.contains("κρεοπωλείο", ignoreCase = true))

        // "25 ευρώ lidl"
        val parsedLidl = parser.parse("25 ευρώ lidl", categories, members)
        assertEquals(25.0, parsedLidl.amount, 0.01)
        assertTrue("Expected title to contain 'Lidl', got: '${parsedLidl.title}'", parsedLidl.title.contains("lidl", ignoreCase = true))
    }

    @Test
    fun testGooglePayAndEurobankPosNotificationsAndDeduplication() {
        val parser = com.example.util.BankNotificationParser

        // 1. Google Wallet / Πορτοφόλι Google notifications from user screenshot
        val shellWallet = parser.parse(
            packageName = "com.google.android.apps.walletnfcrel",
            title = "SHELL MYRTEA THESSALON",
            text = "18,00 € με την κάρτα Eurobank Classic Mastercard •••• 8483",
            timestamp = 1_700_000_000_000L
        )
        org.junit.Assert.assertNotNull(shellWallet)
        assertEquals(18.00, shellWallet!!.amount, 0.01)
        assertEquals("SHELL MYRTEA THESSALON", shellWallet.merchantName)
        assertEquals("Google Pay", shellWallet.bankName)

        val lidlGms = parser.parse(
            packageName = "com.google.android.gms",
            title = "LIDL HELLAS & SIA O.E.",
            text = "79,60 € με την κάρτα Eurobank Classic Mastercard",
            timestamp = 1_700_000_010_000L
        )
        org.junit.Assert.assertNotNull(lidlGms)
        assertEquals(79.60, lidlGms!!.amount, 0.01)
        assertEquals("LIDL HELLAS & SIA O.E.", lidlGms.merchantName)

        val sklavenitisWallet = parser.parse(
            packageName = "com.google.android.apps.walletnfcrel",
            title = "SKLAVENITIS 3040 MAKED",
            text = "77,78 € με την κάρτα Eurobank Classic Mastercard",
            timestamp = 1_700_000_020_000L
        )
        org.junit.Assert.assertNotNull(sklavenitisWallet)
        assertEquals(77.78, sklavenitisWallet!!.amount, 0.01)
        assertEquals("SKLAVENITIS 3040 MAKED", sklavenitisWallet.merchantName)

        // 2. Non-wallet GMS notifications must still be rejected
        val nonWalletGms = parser.parse(
            packageName = "com.google.android.gms",
            title = "Google Play Services",
            text = "Backup completed: 120 MB"
        )
        org.junit.Assert.assertNull(nonWalletGms)

        // 3. Official Eurobank app ("com.EurobankEFG") POS notifications ("Αγορά/Πληρωμή σε POS")
        val eurobankPosShell = parser.parse(
            packageName = "com.EurobankEFG",
            title = "Αγορά/Πληρωμή σε POS",
            text = "Κάνατε αγορά/πληρωμή 18,00 EUR στην εταιρεία SHELL MYRTEA THESSALON με την κάρτα 5167 32** **** 8483 στις 14/11/2026",
            timestamp = 1_700_000_005_000L
        )
        org.junit.Assert.assertNotNull(eurobankPosShell)
        assertEquals(18.00, eurobankPosShell!!.amount, 0.01)
        assertEquals("Eurobank", eurobankPosShell.bankName)
        assertEquals("SHELL MYRTEA THESSALON", eurobankPosShell.merchantName)

        val eurobankPosLidl = parser.parse(
            packageName = "com.EurobankEFG",
            title = "Αγορά/Πληρωμή σε POS",
            text = "Κάνατε αγορά/πληρωμή 79,60 EUR στην εταιρεία LIDL HELLAS & SIA O.E. με την κάρτα 5167 32** **** 8483",
            timestamp = 1_700_000_015_000L
        )
        org.junit.Assert.assertNotNull(eurobankPosLidl)
        assertEquals(79.60, eurobankPosLidl!!.amount, 0.01)
        assertEquals("LIDL HELLAS & SIA O.E.", eurobankPosLidl.merchantName)

        // 4. Cross-app deduplication in BankNotificationHolder (Google Pay + Eurobank within 120s)
        val holder = com.example.util.BankNotificationHolder
        holder.clearAll()
        val firstAdded = holder.addTransaction(shellWallet)
        val secondAdded = holder.addTransaction(eurobankPosShell)
        assertTrue(firstAdded)
        org.junit.Assert.assertFalse(secondAdded)
        assertEquals(1, holder.pendingTransactions.value.size)
        holder.clearAll()
    }

    @Test
    fun testBillReminderNextTriggerCalculation() {
        val nowCal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 14, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        // Same day, later hour -> triggers today
        val laterToday = com.example.reminder.BillReminderManager.calculateNextTriggerTimeMillis(
            dayOfMonth = 14,
            hour = 18,
            minute = 30,
            nowMillis = nowCal.timeInMillis
        )
        val laterCal = java.util.Calendar.getInstance().apply { timeInMillis = laterToday }
        assertEquals(2026, laterCal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.NOVEMBER, laterCal.get(java.util.Calendar.MONTH))
        assertEquals(14, laterCal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(18, laterCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(30, laterCal.get(java.util.Calendar.MINUTE))

        // Same day, earlier hour -> rolls over to next month (Dec 14)
        val nextMonth = com.example.reminder.BillReminderManager.calculateNextTriggerTimeMillis(
            dayOfMonth = 14,
            hour = 8,
            minute = 0,
            nowMillis = nowCal.timeInMillis
        )
        val nextCal = java.util.Calendar.getInstance().apply { timeInMillis = nextMonth }
        assertEquals(2026, nextCal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.DECEMBER, nextCal.get(java.util.Calendar.MONTH))
        assertEquals(14, nextCal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(8, nextCal.get(java.util.Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testRecurringIdResolutionAndDeduplication() {
        val processor = com.example.util.RecurringExpenseAutoProcessor

        // Numeric string IDs and legacy rec_ IDs must resolve back to their Room numeric ID (not hashCode)
        assertEquals(1L, processor.resolveCloudEntityId("1"))
        assertEquals(5L, processor.resolveCloudEntityId("5"))
        assertEquals(5L, processor.resolveCloudEntityId("rec_5"))
        assertTrue(processor.resolveCloudEntityId("uuid-abc-123") >= 1L)

        // Duplicate reminders created from Room ID vs hashCode sync must collapse cleanly to 1
        val dup1 = com.example.data.entity.RecurringExpense(
            id = 5L,
            title = "Ενοίκιο",
            amount = 450.0,
            categoryId = 1L,
            categoryName = "Σπίτι",
            dayOfMonth = 1,
            reminderHour = 9,
            reminderMinute = 0
        )
        val dup2 = dup1.copy(id = 53L, title = "ενοικιο ")
        val other = dup1.copy(id = 6L, title = "ΔΕΗ", amount = 80.0, dayOfMonth = 15)

        val deduped = processor.deduplicateRecurringExpenses(listOf(dup1, dup2, other))
        assertEquals(2, deduped.size)
        assertEquals(5L, deduped[0].id)
        assertEquals(6L, deduped[1].id)
    }
}
