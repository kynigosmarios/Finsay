# Changelog

All notable changes to **Finsay** (`com.kynigos.oikonomos`) are documented in this file.

## [1.0.6] - Build 7

### Added
- **Multi-Language Support (10 Languages):** Full localization added for Spanish (`es`), French (`fr`), German (`de`), Italian (`it`), Portuguese (`pt`), Dutch (`nl`), Polish (`pl`), and Romanian (`ro`) alongside Greek (`el`) and English (`en`), including UI labels, Firebase Authentication emails, notifications, reminders, and Quick Settings Tile labels.
- **Dynamic Google Play Pricing:** Pro subscription and lifetime plans now display localized prices dynamically retrieved from Google Play Billing (`ProductDetails`) with graceful fallbacks.

### Security & Privacy
- **Firestore Rules Hardening (`firestore.rules`):**
  - Added scoped `isSelfLeavingHousehold()` rule so non-owner members can safely leave a household during account deletion without modifying other members' fields.
  - Permitted household owners to clean up `/activity` subcollection entries during complete GDPR account/household deletion.
- **Receipt QR / AADE Redirect Validation:** Disabled automatic OkHttp redirects in `ReceiptAmountAndQrParser` and enforced strict allowlist checks (`isAllowedAadeOrInvoiceHost`) on every HTTPS redirect hop (max 2 hops).
- **Bank Notification Draft Privacy:** `BankNotificationHolder` no longer persists raw notification text (`rawText`) to disk and automatically prunes unprocessed drafts older than 7 days.
- **Release Hardening & Compliance:**
  - Explicitly blocked `com.google.android.gms.permission.AD_ID` via `tools:node="remove"` in `AndroidManifest.xml`.
  - Removed test-only Play Billing override query from the main manifest and stripped `Log.w` calls in release builds via `proguard-rules.pro`.
  - Removed stray development script (`check_firestore.js`) containing a hardcoded Firebase Web API key.
  - Synchronized all Privacy Policy HTML documents with the expanded language list and `PBKDF2-HMAC-SHA256` PIN hashing details.

### Data Integrity & Stability
- **Payer & Attribution Protection:** Prevented automatic/background mutations from overwriting user-selected payers or member attributions on historical expenses and incomes.
- **Cold-Start Local Cache Safety:** Guarded `clearAllData()` on app launch so local Room cache and offline recurring reminders are only cleared on sign-out or an actual user switch, never during normal same-user launches.
- **GDPR Account Deletion Completeness:** Fixed activity subcollection path (`"activity"`) and clean self-removal of non-owner household membership entries (`memberUids`, `memberRoles`, `memberEmails`, `memberNames`, `memberDefaultPayers`).
- **Reminder & Reboot Reliability:**
  - Unified `SharedPreferences` storage (`oikonomos_app_prefs`) across `BillReminderManager` and `DailyReminderManager`.
  - Updated `BootReceiver` to reschedule reminders after app updates (`ACTION_MY_PACKAGE_REPLACED`) and quick reboots (`QUICKBOOT_POWERON`) in addition to standard boot.
- **Swipe Gesture Sensitivity:** Raised horizontal swipe thresholds on expense/income cards to prevent accidental deletion or duplication while vertically scrolling.

### Billing & FinOps Optimization
- **Google Play Billing Safety (`BillingManager`):**
  - Eliminated fallback purchase routing in `launchPurchaseFlow()` (strict `productId` matching).
  - Prevented false Pro revocations on partial/transient Play Billing query errors (`subsQueryOk && inappQueryOk` check + query generation guard).
  - Added bounded retries for `acknowledgePurchase()` to prevent automatic refunds due to transient network failures.
  - Removed unused legacy client-side Pro activation/revocation helper methods.
- **Firestore Read/Write Cost Reduction:**
  - Added a 5-minute cooldown to `onAppResumed()` household refreshes.
  - Eliminated duplicate personal mirror Firestore listeners when an active household is being observed.
  - Cached verified household existence per session (`ensuredHouseholdsCache`) and skipped redundant `users/{uid}` `arrayUnion` writes when household IDs are already present.
- **APK Size Optimization:** Removed unused dependencies (`retrofit`, `moshi`, `logging-interceptor`, `firebase-ai`, unused App Check debug/reCAPTCHA artifacts).
