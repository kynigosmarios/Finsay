package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald700
import com.example.util.Localization

@Composable
fun LegalDocumentsDialog(
    initialTab: String = "TERMS", // "TERMS" or "PRIVACY"
    appLanguage: String = "EL",
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var selectedTab by remember { mutableIntStateOf(if (initialTab == "PRIVACY") 1 else 0) }
    LaunchedEffect(initialTab) {
        selectedTab = if (initialTab == "PRIVACY") 1 else 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("legal_documents_dialog")
    ) {
        // Page Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("legal_dialog_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (lang == Localization.Language.ENGLISH) "Back" else "Επιστροφή",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Emerald700.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Description else Icons.Default.Security,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedTab == 0) {
                            if (lang == Localization.Language.ENGLISH) "Terms & Conditions" else "Όροι & Προϋποθέσεις"
                        } else {
                            if (lang == Localization.Language.ENGLISH) "Privacy Policy" else "Πολιτική Απορρήτου"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == Localization.Language.ENGLISH)
                            "GDPR & Greek Law 4624/2019 Compliant"
                        else
                            "Συμβατό με GDPR & Ν. 4624/2019",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs: Terms of Service / Privacy Policy
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Emerald700,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .height(44.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Terms of Service" else "Όροι Χρήσης",
                        fontSize = 12.5.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Privacy Policy" else "Πολιτική Απορρήτου",
                        fontSize = 12.5.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Legal Text Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                TermsOfServiceText(lang = lang)
            } else {
                PrivacyPolicyText(lang = lang)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PrivacyPolicyText(lang: Localization.Language) {
    if (lang == Localization.Language.ENGLISH) {
        LegalSection(
            title = "1. Introduction",
            body = "This Privacy Policy describes how the Finsay (OikoNomos) application collects, uses, stores, and protects your personal and financial tracking data in compliance with the General Data Protection Regulation (EU) 2016/679 (GDPR), the Greek Law 4624/2019, and Google Play Developer Policies. By using Finsay, you agree to the data collection and practices described herein."
        )
        LegalSection(
            title = "2. Data We Collect",
            body = "• Account Information: Name, email address, profile picture provided via Google Authentication / Firebase Auth.\n• Expense & Financial Logs: Amounts, expense titles, categories, dates, payment sources, and beneficiary split shares that you intentionally register in the app.\n• Household & Family Members: Member nicknames, roles, and avatar identifiers created for shared household budgeting.\n• Device Preferences: Theme mode, selected language (EL/EN), and notification preferences."
        )
        LegalSection(
            title = "3. Purpose and Legal Basis of Processing",
            body = "We process your data strictly under the following legal bases:\n• Performance of Contract (Art. 6(1)(b) GDPR): To deliver real-time expense calculations, synchronization across your household devices, and data export features.\n• Consent (Art. 6(1)(a) GDPR): For optional voice recognition entry parsing and reminder notifications.\n• Legitimate Interests (Art. 6(1)(f) GDPR): To safeguard application security, prevent fraud, and maintain database reliability."
        )
        LegalSection(
            title = "4. Data Storage and Security",
            body = "• Security & Encryption: Your data is stored in isolated Google Cloud / Firebase Firestore databases with end-to-end security rules. Reads and writes are strictly restricted to authenticated household members.\n• Zero Third-Party Monetization: We DO NOT sell, rent, monetize, or transfer your personal or financial data to advertisers, brokers, or marketing networks."
        )
        LegalSection(
            title = "5. Your GDPR Rights",
            body = "Under GDPR and Greek Law 4624/2019, you have the full right to:\n• Access your data at any time via the application.\n• Rectify and edit any inaccurate records.\n• Erasure (\"Right to be Forgotten\"): Permanently delete your account and all associated cloud & local data via Menu (☰) > My Profile > Delete Account.\n• Data Portability: Export your entire budget and expense records to CSV / Excel directly from the app."
        )
        LegalSection(
            title = "6. Subscriptions & Third-Party Services",
            body = "• Google Play Billing: Premium Pro subscriptions are processed securely through Google Play. We do not store or process credit card numbers.\n• Contact & Inquiries: For questions regarding privacy or data rights, contact us at support@finsayapp.com."
        )
    } else {
        LegalSection(
            title = "1. Εισαγωγή & Υπεύθυνος Επεξεργασίας",
            body = "Η παρούσα Πολιτική Απορρήτου καθορίζει τον τρόπο με τον οποίο η εφαρμογή Finsay (OikoNomos) συλλέγει, αποθηκεύει και προστατεύει τα προσωπικά και οικονομικά σας δεδομένα σύμφωνα με τον Γενικό Κανονισμό Προστασίας Δεδομένων (ΕΕ) 2016/679 (GDPR), τον ελληνικό Νόμο 4624/2019 και τις πολιτικές της Google Play Console. Με τη χρήση της εφαρμογής, αποδέχεστε τις πρακτικές που περιγράφονται στο παρόν."
        )
        LegalSection(
            title = "2. Δεδομένα που Συλλέγουμε",
            body = "• Στοιχεία Λογαριασμού: Ονοματεπώνυμο, email και φωτογραφία προφίλ που παρέχονται μέσω του Google Sign-In ή του Firebase Authentication.\n• Εγγραφές Εξόδων & Εσόδων: Χρηματικά ποσά, τίτλοι/περιγραφές, κατηγορίες, ημερομηνίες, τρόποι πληρωμής και επιμερισμοί μελών που καταχωρείτε οικειοθελώς στην εφαρμογή.\n• Μέλη Εξοδολογίου: Ονόματα, ρόλοι και εικονίδια μελών για τον κοινό οικογενειακό/ομαδικό προϋπολογισμό.\n• Προτιμήσεις Συσκευής: Επιλογή γλώσσας (Ελληνικά/Αγγλικά), θέμα εμφάνισης και ώρα τοπικής υπενθύμισης."
        )
        LegalSection(
            title = "3. Σκοπός και Νομική Βάση Επεξεργασίας",
            body = "Η επεξεργασία πραγματοποιείται αποκλειστικά για:\n• Εκτέλεση Σύμβασης (Άρθρο 6(1)(β) GDPR): Παροχή υπολογισμών, στατιστικών αναλύσεων και συγχρονισμού εξοδολογίου μεταξύ των εξουσιοδοτημένων συσκευών σας.\n• Συγκατάθεση (Άρθρο 6(1)(α) GDPR): Για τη φωνητική καταχώρηση εντολών και την αποστολή τοπικών ειδοποιήσεων υπενθύμισης.\n• Έννομο Συμφέρον (Άρθρο 6(1)(στ) GDPR): Διασφάλιση ακεραιότητας και αποτροπή κακόβουλης χρήσης της εφαρμογής."
        )
        LegalSection(
            title = "4. Ασφάλεια & Μηδενική Κοινοποίηση σε Τρίτους",
            body = "• Κρυπτογράφηση & Απομόνωση: Τα δεδομένα αποθηκεύονται σε πιστοποιημένη υποδομή Google Cloud / Firebase Firestore με αυστηρούς κανόνες ασφαλείας. Κανένας μη εξουσιοδοτημένος χρήστης δεν έχει πρόσβαση στο ιδιωτικό σας εξοδολόγιο.\n• Μηδενική Πώληση Δεδομένων: Τα προσωπικά και οικονομικά σας στοιχεία ΔΕΝ πωλούνται, ΔΕΝ ενοικιάζονται και ΔΕΝ κοινοποιούνται σε διαφημιστικές εταιρείες ή διαμεσολαβητές."
        )
        LegalSection(
            title = "5. Τα Δικαιώματά σας βάσει GDPR & Ν. 4624/2019",
            body = "Έχετε πλήρη δικαιώματα:\n• Πρόσβασης και Διόρθωσης των εγγραφών σας.\n• Οριστικής Διαγραφής («Δικαίωμα στη Λήθη»): Μπορείτε να διαγράψετε άμεσα και οριστικά τον λογαριασμό σας καθώς και όλα τα τοπικά και cloud δεδομένα σας από το «Μενού (☰) > Το Προφίλ μου > Διαγραφή Λογαριασμού».\n• Φορητότητας Δεδομένων: Εξαγωγή του συνόλου των εξόδων σας σε αρχείο Excel / CSV οποτεδήποτε το επιθυμείτε."
        )
        LegalSection(
            title = "6. Συνδρομές & Επικοινωνία",
            body = "• Google Play Billing: Οι Pro συνδρομές διαχειρίζονται με ασφάλεια αποκλειστικά από την Google Play. Η εφαρμογή δεν αποθηκεύει αριθμούς τραπεζικών καρτών.\n• Επικοινωνία: Για οποιοδήποτε ερώτημα σχετικά με τα δεδομένα σας, επικοινωνήστε στο support@finsayapp.com."
        )
    }
}

@Composable
private fun TermsOfServiceText(lang: Localization.Language) {
    if (lang == Localization.Language.ENGLISH) {
        LegalSection(
            title = "1. Acceptance of Terms",
            body = "By downloading, installing, or using Finsay (OikoNomos), you agree to be bound by these Terms and Conditions. If you do not agree with any part of these terms, you must not use the application."
        )
        LegalSection(
            title = "2. Financial Disclaimer",
            body = "Finsay is an informative tracking and organizational tool designed for personal and household financial budgeting. Finsay DOES NOT provide certified financial, investment, accounting, or tax advice. You are solely responsible for reviewing and verifying the accuracy of your financial records."
        )
        LegalSection(
            title = "3. User Account and Responsibilities",
            body = "You are responsible for safeguarding your login credentials. You agree to use the application in accordance with all applicable laws and regulations. You may not use the app for any unauthorized, fraudulent, or harmful purposes."
        )
        LegalSection(
            title = "4. Pro Subscriptions & In-App Purchases",
            body = "• Certain features, such as multiple households and unlimited members, require a Finsay Pro subscription.\n• Subscriptions are billed through Google Play according to the terms stated at the time of purchase.\n• You can manage or cancel your subscription at any time via your Google Play account settings."
        )
        LegalSection(
            title = "5. Limitation of Liability",
            body = "To the maximum extent permitted by applicable law, Finsay and its developers shall not be liable for any indirect, incidental, or consequential damages resulting from the use or inability to use the application or any data loss."
        )
        LegalSection(
            title = "6. Governing Law & Jurisdiction",
            body = "These Terms are governed by and construed in accordance with the laws of the Hellenic Republic (Greece) and the European Union. Any disputes shall be subject to the exclusive jurisdiction of the competent courts of Athens, Greece."
        )
    } else {
        LegalSection(
            title = "1. Αποδοχή των Όρων",
            body = "Με τη λήψη, εγκατάσταση και χρήση της εφαρμογής Finsay (OikoNomos), συμφωνείτε ρητά με τους παρόντες Όρους και Προϋποθέσεις. Εάν δεν συμφωνείτε με οποιοδήποτε μέρος των όρων, παρακαλείστε να μην χρησιμοποιήσετε την εφαρμογή."
        )
        LegalSection(
            title = "2. Αποποίηση Οικονομικής & Φορολογικής Ευθύνης",
            body = "Το Finsay αποτελεί αποκλειστικά εργαλείο προσωπικής καταγραφής, οργάνωσης και παρακολούθησης οικιακού προϋπολογισμού. Το Finsay ΔΕΝ παρέχει πιστοποιημένες οικονομικές, επενδυτικές ή φορολογικές συμβουλές. Ο χρήστης φέρει την αποκλειστική ευθύνη για την ορθότητα των στοιχείων που καταχωρεί."
        )
        LegalSection(
            title = "3. Λογαριασμός Χρήστη & Υποχρεώσεις",
            body = "Ο χρήστης είναι υπεύθυνος για τη διατήρηση της εμπιστευτικότητας των στοιχείων εισόδου του. Απαγορεύεται ρητά η χρήση της εφαρμογής για οποιονδήποτε παράνομο, παραπλανητικό ή κακόβουλο σκοπό."
        )
        LegalSection(
            title = "4. Συνδρομή Pro & Αγορές μέσω Google Play",
            body = "• Ορισμένες προηγμένες λειτουργίες (π.χ. πολλαπλά εξοδολόγια, απεριόριστα μέλη) απαιτούν συνδρομή Finsay Pro.\n• Οι συνδρομές τιμολογούνται μέσω της Google Play σύμφωνα με τους όρους αγοράς.\n• Μπορείτε να διαχειριστείτε ή να ακυρώσετε τη συνδρομή σας οποτεδήποτε μέσω των ρυθμίσεων του λογαριασμού σας στο Google Play."
        )
        LegalSection(
            title = "5. Περιορισμός Ευθύνης",
            body = "Στο μέγιστο βαθμό που επιτρέπεται από την ισχύουσα νομοθεσία, το Finsay και οι δημιουργοί του δεν φέρουν ευθύνη για τυχόν έμμεσες ή αποθετικές ζημίες ή απώλεια δεδομένων που προκύπτουν από τη χρήση της εφαρμογής."
        )
        LegalSection(
            title = "6. Εφαρμοστέο Δίκαιο & Δικαιοδοσία",
            body = "Οι παρόντες Όροι διέπονται από το Ελληνικό Δίκαιο και το εφαρμοστέο δίκαιο της Ευρωπαϊκής Ένωσης. Αρμόδια για την επίλυση τυχόν διαφορών ορίζονται τα δικαστήρια των Αθηνών."
        )
    }
}

@Composable
private fun LegalSection(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
