package com.example.ui.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.safety.DataSafetyGuard
import com.example.util.Localization
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

internal class OikoNomosAuthDelegate(
    private val vm: OikoNomosViewModel
) {
    fun acceptNotificationConsent() {
        val uid = vm.auth.currentUser?.uid ?: "guest"
        vm.prefs.edit().putBoolean("notification_consent_handled_$uid", true).apply()
        vm._notificationConsentCompleted.value = true
        vm.toggleDailyReminder(true)
        if (uid != "guest") {
            vm._userProfile.value = vm._userProfile.value?.copy(notificationConsentCompleted = true, reminderEnabled = true)
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.updateUserProfilePreferences(uid, notificationConsentCompleted = true, reminderEnabled = true)
            }
        }
        val user = vm.auth.currentUser ?: vm._currentUser.value
        if (user != null && isUserVerified(user)) {
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.loadUserProfileAndHousehold(user)
            }
        }
    }

    fun skipNotificationConsent() {
        val uid = vm.auth.currentUser?.uid ?: "guest"
        vm.prefs.edit().putBoolean("notification_consent_handled_$uid", true).apply()
        vm._notificationConsentCompleted.value = true
        if (uid != "guest") {
            vm._userProfile.value = vm._userProfile.value?.copy(notificationConsentCompleted = true)
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.firestoreSyncRepo.updateUserProfilePreferences(uid, notificationConsentCompleted = true)
            }
        }
        val user = vm.auth.currentUser ?: vm._currentUser.value
        if (user != null && isUserVerified(user)) {
            vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
                vm.loadUserProfileAndHousehold(user)
            }
        }
    }

    fun isUserVerified(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return user.isEmailVerified
    }

    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        val user = vm.auth.currentUser ?: vm._currentUser.value ?: run {
            onResult(false)
            return
        }
        vm._isAuthLoading.value = true
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                user.reload().await()
                val refreshedUser = vm.auth.currentUser ?: user
                if (!refreshedUser.isEmailVerified) {
                    vm._isAuthLoading.value = false
                    vm._isUserVerifiedState.value = false
                    onResult(false)
                    return@launch
                }

                vm.prefs.edit().remove("pending_google_verification_${refreshedUser.uid}").apply()
                try {
                    vm.firestoreSyncRepo.markEmailVerifiedConfirmed(refreshedUser.uid)
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "markEmailVerifiedConfirmed warning: ${e.message}")
                }
                vm._userProfile.value = vm._userProfile.value?.copy(emailVerifiedConfirmed = true)
                vm._currentUser.value = refreshedUser
                vm._isUserVerifiedState.value = true
                vm._isAuthLoading.value = false
                vm.showSnackbar(
                    if (vm._appLanguage.value == "EN") "Email verified successfully! Welcome!"
                    else "Το email επιβεβαιώθηκε επιτυχώς! Καλώς ήρθατε!"
                )
                vm.loadUserProfileAndHousehold(refreshedUser)
                onResult(true)
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.w("OikoNomosVM", "checkEmailVerification user invalid: ${e.message}")
                vm._isAuthLoading.value = false
                signOutUser()
                onResult(false)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("OikoNomosVM", "checkEmailVerification reload error: ${e.message}")
                vm._isAuthLoading.value = false
                onResult(false)
            }
        }
    }

    fun resendVerificationEmail(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = vm.auth.currentUser ?: vm._currentUser.value
        if (user == null) {
            onError(if (vm._appLanguage.value == "EN") "No signed-in user found" else "Δεν βρέθηκε συνδεδεμένος χρήστης")
            return
        }
        vm._isAuthLoading.value = true
        vm.auth.setLanguageCode(if (vm._appLanguage.value == "EN") "en" else "el")
        user.sendEmailVerification()
            .addOnSuccessListener {
                vm._isAuthLoading.value = false
                vm.showSnackbar(
                    if (vm._appLanguage.value == "EN") "A new verification email has been sent!"
                    else "Στάλθηκε νέο email επιβεβαίωσης!"
                )
                onSuccess()
            }
            .addOnFailureListener { exc ->
                vm._isAuthLoading.value = false
                val msg = if (exc is FirebaseTooManyRequestsException) {
                    if (vm._appLanguage.value == "EN") "Too many requests. Please wait a bit before resending."
                    else "Πολλές αποστολές email σε σύντομο χρόνο. Παρακαλούμε περιμένετε λίγο και προσπαθήστε ξανά."
                } else {
                    exc.localizedMessage ?: (if (vm._appLanguage.value == "EN") "Failed to send verification email" else "Σφάλμα αποστολής email")
                }
                onError(msg)
            }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val clean = email.trim()
        if (clean.isBlank()) {
            onError(if (vm._appLanguage.value == "EN") "Please enter your email" else "Παρακαλώ εισάγετε το email σας")
            return
        }
        vm._isAuthLoading.value = true
        vm.auth.setLanguageCode(if (vm._appLanguage.value == "EN") "en" else "el")
        vm.auth.sendPasswordResetEmail(clean)
            .addOnSuccessListener {
                vm._isAuthLoading.value = false
                vm.showSnackbar(if (vm._appLanguage.value == "EN") "Password reset link sent to $clean!" else "Στάλθηκε σύνδεσμος επαναφοράς κωδικού στο $clean!")
                onSuccess()
            }
            .addOnFailureListener { exc ->
                vm._isAuthLoading.value = false
                val msg = when {
                    exc.message?.contains("user-not-found") == true ->
                        if (vm._appLanguage.value == "EN") "No account found with this email" else "Δεν βρέθηκε λογαριασμός με αυτό το email"
                    exc.message?.contains("invalid-email") == true ->
                        if (vm._appLanguage.value == "EN") "Invalid email address" else "Μη έγκυρη μορφή email"
                    else -> exc.localizedMessage ?: (if (vm._appLanguage.value == "EN") "Error sending reset email" else "Σφάλμα αποστολής email")
                }
                onError(msg)
            }
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val cleanEmail = email.trim()
        val isEn = vm._appLanguage.value == "EN"
        if (cleanEmail.isBlank() || pass.isBlank()) {
            onError(if (isEn) "Please enter your email and password" else "Παρακαλώ συμπληρώστε email και κωδικό πρόσβασης")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onError(if (isEn) "Invalid email address format" else "Μη έγκυρη μορφή email")
            return
        }
        vm._isAuthLoading.value = true
        vm.suppressAuthStateListener = true
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                val result = vm.auth.signInWithEmailAndPassword(cleanEmail, pass).await()
                val rawUser = result.user ?: throw IllegalStateException("Authentication returned no user")
                try { rawUser.reload().await() } catch (_: Exception) {}
                val user = vm.auth.currentUser ?: rawUser
                val verified = isUserVerified(user)
                vm.prefs.edit().putString("cached_active_uid", user.uid).apply()
                vm._currentUser.value = user
                vm._isUserVerifiedState.value = verified
                vm._isAuthLoading.value = false
                if (verified) {
                    vm.showSnackbar(if (isEn) "Signed in successfully!" else "Συνδεθήκατε επιτυχώς!")
                    vm.loadUserProfileAndHousehold(user)
                } else {
                    vm.showSnackbar(
                        if (isEn) "Email verification is required. Please click the link sent to your email."
                        else "Απαιτείται επιβεβαίωση email. Πατήστε στον σύνδεσμο που στάλθηκε στο email σας."
                    )
                }
                onSuccess()
            } catch (exc: Exception) {
                if (exc is CancellationException) throw exc
                vm._isAuthLoading.value = false
                val code = (exc as? FirebaseAuthException)?.errorCode.orEmpty()
                val msg = when {
                    code == "ERROR_USER_NOT_FOUND" || exc.message?.contains("user-not-found") == true ->
                        if (isEn) "No registered account found with this email. Please Sign Up first."
                        else "Δεν βρέθηκε εγγεγραμμένος λογαριασμός με αυτό το email. Κάντε πρώτα Εγγραφή."
                    code == "ERROR_WRONG_PASSWORD" || code == "ERROR_INVALID_CREDENTIAL" ||
                        exc.message?.contains("wrong-password") == true || exc.message?.contains("invalid-credential") == true ->
                        if (isEn) "Incorrect email or password, or account is not registered."
                        else "Λανθασμένο email ή κωδικός πρόσβασης, ή ο λογαριασμός δεν είναι εγγεγραμμένος."
                    code == "ERROR_INVALID_EMAIL" || exc.message?.contains("invalid-email") == true ->
                        if (isEn) "Invalid email address format" else "Μη έγκυρη μορφή email"
                    code == "ERROR_USER_DISABLED" ->
                        if (isEn) "This account has been disabled." else "Αυτός ο λογαριασμός έχει απενεργοποιηθεί."
                    exc is FirebaseTooManyRequestsException || code == "ERROR_TOO_MANY_REQUESTS" ->
                        if (isEn) "Too many attempts. Please try again shortly."
                        else "Πολλές αποτυχημένες προσπάθειες. Δοκιμάστε ξανά σε λίγο."
                    else -> exc.localizedMessage ?: (if (isEn) "Sign in error" else "Σφάλμα σύνδεσης")
                }
                onError(msg)
            } finally {
                vm.suppressAuthStateListener = false
            }
        }
    }

    fun signUpWithEmail(
        firstName: String,
        lastName: String,
        birthDate: String,
        phoneNumber: String,
        occupation: String,
        city: String,
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val isEn = vm._appLanguage.value == "EN"
        val cleanEmail = email.trim()
        if (firstName.isBlank() || lastName.isBlank()) {
            onError(if (isEn) "Please enter First Name and Last Name" else "Παρακαλώ συμπληρώστε Όνομα και Επίθετο")
            return
        }
        if (birthDate.isBlank()) {
            onError(if (isEn) "Please select your Date of Birth" else "Παρακαλώ επιλέξτε την Ημερομηνία Γέννησής σας")
            return
        }
        if (phoneNumber.isBlank() || phoneNumber.length < 10) {
            onError(if (isEn) "Please enter a valid mobile phone (at least 10 digits)" else "Παρακαλώ εισάγετε έγκυρο κινητό τηλέφωνο (τουλάχιστον 10 ψηφία)")
            return
        }
        if (cleanEmail.isBlank() || pass.isBlank()) {
            onError(if (isEn) "Please enter email and password" else "Παρακαλώ συμπληρώστε email και κωδικό πρόσβασης")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onError(if (isEn) "Invalid email address format" else "Μη έγκυρη μορφή email")
            return
        }
        if (pass.length < 6) {
            onError(if (isEn) "Password must be at least 6 characters" else "Ο κωδικός πρέπει να έχει τουλάχιστον 6 χαρακτήρες")
            return
        }
        vm._isAuthLoading.value = true
        vm.suppressAuthStateListener = true
        val fullName = "${firstName.trim()} ${lastName.trim()}"
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                val result = vm.auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
                val user = result.user ?: throw IllegalStateException("User creation returned null")
                try {
                    val updateReq = UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
                    user.updateProfile(updateReq).await()
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "updateProfile warning: ${e.message}")
                }

                val pendingProfile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: cleanEmail,
                    displayName = fullName,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    birthDate = birthDate.trim(),
                    phoneNumber = phoneNumber.trim(),
                    occupation = occupation.trim(),
                    city = city.trim(),
                    authProvider = "password",
                    requiresEmailVerification = true,
                    emailVerifiedConfirmed = false,
                    appLanguage = vm._appLanguage.value,
                    createdAt = Timestamp.now()
                )
                try {
                    vm.firestoreSyncRepo.savePendingUserProfile(pendingProfile)
                    vm._userProfile.value = pendingProfile
                    saveUserProfileToPrefs(pendingProfile)
                } catch (e: Exception) {
                    Log.e("OikoNomosVM", "Error storing profile during sign up", e)
                }

                try {
                    vm.auth.setLanguageCode(if (isEn) "en" else "el")
                    user.sendEmailVerification().await()
                } catch (e: Exception) {
                    Log.e("OikoNomosVM", "sendEmailVerification error", e)
                    vm.showSnackbar(e.localizedMessage ?: (if (isEn) "Failed to send verification email" else "Σφάλμα αποστολής email επιβεβαίωσης"))
                }

                val verified = isUserVerified(user)
                vm.prefs.edit().putString("cached_active_uid", user.uid).apply()
                vm._currentUser.value = user
                vm._isUserVerifiedState.value = verified
                vm._isAuthLoading.value = false
                if (!verified) {
                    vm.showSnackbar(
                        if (isEn) "Verification link sent to ${user.email}!"
                        else "Στάλθηκε σύνδεσμος επιβεβαίωσης στο ${user.email}!"
                    )
                } else {
                    vm.loadUserProfileAndHousehold(user)
                }
                onSuccess()
            } catch (exc: Exception) {
                if (exc is CancellationException) throw exc
                vm._isAuthLoading.value = false
                val code = (exc as? FirebaseAuthException)?.errorCode.orEmpty()
                val msg = when {
                    code == "ERROR_EMAIL_ALREADY_IN_USE" || exc.message?.contains("email-already-in-use") == true ->
                        if (isEn) "This email is already registered. Please switch to Sign In."
                        else "Αυτό το email χρησιμοποιείται ήδη. Παρακαλούμε μεταβείτε στη Σύνδεση."
                    code == "ERROR_WEAK_PASSWORD" || exc.message?.contains("weak-password") == true ->
                        if (isEn) "Password is too weak" else "Ο κωδικός είναι πολύ αδύναμος"
                    code == "ERROR_INVALID_EMAIL" || exc.message?.contains("invalid-email") == true ->
                        if (isEn) "Invalid email address format" else "Μη έγκυρη μορφή email"
                    else -> exc.localizedMessage ?: (if (isEn) "Error creating account" else "Σφάλμα δημιουργίας λογαριασμού")
                }
                onError(msg)
            } finally {
                vm.suppressAuthStateListener = false
            }
        }
    }

    fun signInWithGoogleToken(
        idToken: String,
        isSignUpMode: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val isEn = vm._appLanguage.value == "EN"
        vm._isAuthLoading.value = true
        vm.suppressAuthStateListener = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                val result = vm.auth.signInWithCredential(credential).await()
                val user = result.user ?: throw IllegalStateException("Google authentication returned no user")
                val isNewAuthUser = result.additionalUserInfo?.isNewUser == true
                val existingProfile = try {
                    vm.firestoreSyncRepo.getUserProfileOnce(user.uid)
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "getUserProfileOnce error: ${e.message}")
                    if (isNewAuthUser) null else UserProfile(uid = user.uid, email = user.email.orEmpty())
                }

                val googleVerified = user.isEmailVerified
                if (isNewAuthUser || existingProfile == null) {
                    val displayName = user.displayName.orEmpty().trim()
                    val parts = displayName.split(" ").filter { it.isNotBlank() }
                    val newProfile = UserProfile(
                        uid = user.uid,
                        email = user.email.orEmpty().trim(),
                        displayName = displayName.ifBlank { "Χρήστης Finsay" },
                        firstName = parts.firstOrNull().orEmpty(),
                        lastName = if (parts.size > 1) parts.drop(1).joinToString(" ") else "",
                        photoUrl = user.photoUrl?.toString().orEmpty(),
                        authProvider = "google",
                        requiresEmailVerification = !googleVerified,
                        emailVerifiedConfirmed = googleVerified,
                        appLanguage = vm._appLanguage.value,
                        createdAt = Timestamp.now()
                    )
                    try {
                        vm.firestoreSyncRepo.savePendingUserProfile(newProfile)
                        saveUserProfileToPrefs(newProfile)
                    } catch (e: Exception) {
                        Log.w("OikoNomosVM", "savePendingUserProfile Google warning: ${e.message}")
                    }
                    vm.prefs.edit()
                        .putString("cached_active_uid", user.uid)
                        .remove("pending_google_verification_${user.uid}")
                        .apply()
                    if (!googleVerified) {
                        try {
                            vm.auth.setLanguageCode(if (isEn) "en" else "el")
                            user.sendEmailVerification().await()
                        } catch (e: Exception) {
                            Log.w("OikoNomosVM", "Google sendEmailVerification warning: ${e.message}")
                        }
                    }
                    vm._userProfile.value = newProfile
                    vm._currentUser.value = user
                    vm._isUserVerifiedState.value = googleVerified
                    vm._isAuthLoading.value = false
                    if (googleVerified) {
                        val welcomeMsg = if (isSignUpMode) {
                            if (isEn) "Signed up with Google!" else "Η εγγραφή μέσω Google ολοκληρώθηκε!"
                        } else {
                            if (isEn) "Signed in with Google!" else "Συνδεθήκατε μέσω Google!"
                        }
                        vm.showSnackbar(welcomeMsg)
                        vm.loadUserProfileAndHousehold(user)
                    } else {
                        vm.showSnackbar(
                            if (isEn) "Verification link sent to ${user.email}!"
                            else "Στάλθηκε σύνδεσμος επιβεβαίωσης στο ${user.email}!"
                        )
                    }
                    onSuccess()
                } else {
                    vm.prefs.edit()
                        .putString("cached_active_uid", user.uid)
                        .remove("pending_google_verification_${user.uid}")
                        .apply()
                    if (!googleVerified) {
                        vm._userProfile.value = existingProfile
                        vm._currentUser.value = user
                        vm._isUserVerifiedState.value = false
                        vm._isAuthLoading.value = false
                        vm.showSnackbar(
                            if (isEn) "Please verify your email via the link sent to ${user.email}."
                            else "Παρακαλούμε επιβεβαιώστε το email σας από τον σύνδεσμο στο ${user.email}."
                        )
                        onSuccess()
                    } else {
                        if (existingProfile.requiresEmailVerification && !existingProfile.emailVerifiedConfirmed) {
                            try { vm.firestoreSyncRepo.markEmailVerifiedConfirmed(user.uid) } catch (_: Exception) {}
                        }
                        vm._userProfile.value = existingProfile.copy(requiresEmailVerification = false, emailVerifiedConfirmed = true)
                        vm._currentUser.value = user
                        vm._isUserVerifiedState.value = true
                        vm._isAuthLoading.value = false
                        vm.showSnackbar(if (isEn) "Signed in with Google!" else "Συνδεθήκατε μέσω Google!")
                        vm.loadUserProfileAndHousehold(user)
                        onSuccess()
                    }
                }
            } catch (exc: Exception) {
                if (exc is CancellationException) throw exc
                vm._isAuthLoading.value = false
                onError(exc.localizedMessage ?: (if (isEn) "Google sign-in error" else "Σφάλμα σύνδεσης Google"))
            } finally {
                vm.suppressAuthStateListener = false
            }
        }
    }

    fun signOutUser() {
        val prevUid = vm.auth.currentUser?.uid ?: vm._currentUser.value?.uid
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            withContext(NonCancellable) {
                try {
                    CredentialManager.create(vm.getApplication()).clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "clearCredentialState warning: ${e.message}")
                } finally {
                    vm.auth.signOut()
                }
            }
        }
        vm._currentUser.value = null
        vm._isUserVerifiedState.value = false
        vm._userProfile.value = null
        vm._currentHousehold.value = null
        vm._userHouseholds.value = emptyList()
        vm._showOnboardingCarousel.value = false
        vm._showWelcomeCarousel.value = false
        vm.syncBankCatcherProEntitlement()
        vm._showUserProfileDialog.value = false
        vm._showExodologioDetailsDialog.value = false
        vm._showRemindersManagerDialog.value = false
        vm._showSavingsGoalsManagerDialog.value = false
        vm._showSettingsDialog.value = false
        vm._legalDocumentsInitialTab.value = null
        vm._notificationConsentCompleted.value = false
        vm._pendingUndoDeletion.value = null
        vm.undoCountdownJob?.cancel()
        vm._householdActivities.value = emptyList()
        clearUserProfileFromPrefs()
        vm.prefs.edit()
            .remove("last_household_id")
            .remove("cached_active_uid")
            .remove("has_entered_any_expense")
            .apply()
        vm.cancelAllSyncJobs()
        vm.lastObservedHouseholdId = null
        vm.lastHouseholdExpenses = emptyList()
        vm.lastUserExpenses = emptyList()
        vm.lastHouseholdIncomes = emptyList()
        vm.lastUserIncomes = emptyList()
        vm.lastHouseholdMembers = emptyList()
        vm.lastUserMembers = emptyList()
        vm.lastHouseholdCategories = emptyList()
        vm.lastUserCategories = emptyList()
        vm.lastHouseholdSavingsGoals = emptyList()
        vm.lastUserSavingsGoals = emptyList()
        vm.savingsGoalCloudIdByLocalId.clear()
        vm.pendingLocalSavingsGoals.clear()
        vm.pendingDeletedSavingsGoals.clear()
        vm.inFlightCreateGoalKeys.clear()
        vm._allExpenses.value = emptyList()
        vm._allIncomes.value = emptyList()
        vm._allMembers.value = emptyList()
        vm._allCategories.value = emptyList()
        vm._allSavingsGoals.value = emptyList()
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try { vm.repository.clearAllData() } catch (e: Exception) {
                Log.w("OikoNomosVM", "Error clearing Room on signOut: ${e.message}")
            }
        }
        if (prevUid != null) Log.d("OikoNomosVM", "Signed out user $prevUid")
        vm.showSnackbar(if (vm._appLanguage.value == "EN") "Signed out successfully" else "Αποσυνδεθήκατε επιτυχώς")
    }

    fun factoryResetAndSignOut() {
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            withContext(NonCancellable) {
                try {
                    CredentialManager.create(vm.getApplication()).clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {}
                vm.auth.signOut()
            }
        }
        vm._currentUser.value = null
        vm._isUserVerifiedState.value = false
        vm._userProfile.value = null
        vm._currentHousehold.value = null
        vm._userHouseholds.value = emptyList()
        vm._showOnboardingCarousel.value = false
        vm._showWelcomeCarousel.value = false
        vm.syncBankCatcherProEntitlement()
        vm._showUserProfileDialog.value = false
        vm._showExodologioDetailsDialog.value = false
        vm._showRemindersManagerDialog.value = false
        vm._showSavingsGoalsManagerDialog.value = false
        vm._showSettingsDialog.value = false
        vm._legalDocumentsInitialTab.value = null
        vm._notificationConsentCompleted.value = false
        vm._pendingUndoDeletion.value = null
        vm.undoCountdownJob?.cancel()
        vm._householdActivities.value = emptyList()
        vm.prefs.edit().clear().apply()
        vm.cancelAllSyncJobs()
        vm.lastObservedHouseholdId = null
        vm.lastHouseholdExpenses = emptyList()
        vm.lastUserExpenses = emptyList()
        vm.lastHouseholdIncomes = emptyList()
        vm.lastUserIncomes = emptyList()
        vm.lastHouseholdMembers = emptyList()
        vm.lastUserMembers = emptyList()
        vm.lastHouseholdCategories = emptyList()
        vm.lastUserCategories = emptyList()
        vm.lastHouseholdSavingsGoals = emptyList()
        vm.lastUserSavingsGoals = emptyList()
        vm.savingsGoalCloudIdByLocalId.clear()
        vm.pendingLocalSavingsGoals.clear()
        vm.pendingDeletedSavingsGoals.clear()
        vm.inFlightCreateGoalKeys.clear()
        vm._allExpenses.value = emptyList()
        vm._allIncomes.value = emptyList()
        vm._allMembers.value = emptyList()
        vm._allCategories.value = emptyList()
        vm._allSavingsGoals.value = emptyList()
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            if (DataSafetyGuard.validateDestructiveOperation(DataSafetyGuard.DestructionReason.EXPLICIT_FACTORY_RESET)) {
                vm.repository.clearAllData()
            }
            vm.showSnackbar("Η εφαρμογή επανήλθε σε αρχική καθαρή κατάσταση")
        }
    }

    fun deleteCurrentUserAccount() {
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            val user = vm.auth.currentUser ?: vm._currentUser.value
            val uid = user?.uid
            if (user == null || uid.isNullOrBlank()) {
                vm.showSnackbar(if (vm._appLanguage.value == "EN") "No active user session found" else "Δεν βρέθηκε ενεργός συνδεδεμένος χρήστης")
                return@launch
            }
            try {
                val hIds = vm._userHouseholds.value.map { it.id }
                vm.firestoreSyncRepo.deleteUserEntireData(uid, hIds)
                user.delete().await()
                withContext(NonCancellable) {
                    try {
                        CredentialManager.create(vm.getApplication()).clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: Exception) {}
                }
                vm.repository.clearAllData()
                clearUserProfileFromPrefs()
                vm.prefs.edit().clear().apply()
                vm.cancelAllSyncJobs()
                vm._currentUser.value = null
                vm._isUserVerifiedState.value = false
                vm._userProfile.value = null
                vm._currentHousehold.value = null
                vm._userHouseholds.value = emptyList()
                vm._showWelcomeCarousel.value = false
                vm.syncBankCatcherProEntitlement()
                vm._allExpenses.value = emptyList()
                vm._allIncomes.value = emptyList()
                vm._allMembers.value = emptyList()
                vm._allCategories.value = emptyList()
                vm._allSavingsGoals.value = emptyList()
                vm._householdActivities.value = emptyList()
                vm.lastHouseholdSavingsGoals = emptyList()
                vm.lastUserSavingsGoals = emptyList()
                vm.savingsGoalCloudIdByLocalId.clear()
                vm.pendingLocalSavingsGoals.clear()
                vm.pendingDeletedSavingsGoals.clear()
                vm.inFlightCreateGoalKeys.clear()
                vm.dismissAllSubPages()
                vm.showSnackbar(if (vm._appLanguage.value == "EN") "Your account and personal data have been completely deleted" else "Ο λογαριασμός και όλα τα δεδομένα σας διαγράφηκαν επιτυχώς")
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                vm.showSnackbar(
                    if (vm._appLanguage.value == "EN")
                        "For your security, please sign out, sign in again, and retry Account Deletion."
                    else
                        "Για λόγους ασφαλείας, παρακαλούμε αποσυνδεθείτε, συνδεθείτε ξανά και πατήστε πάλι Διαγραφή Λογαριασμού."
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Σφάλμα διαγραφής: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun updateUserDisplayName(newName: String) {
        val user = vm._currentUser.value ?: return
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm.firestoreSyncRepo.updateUserProfileName(user.uid, trimmed)
                vm._userProfile.value = vm._userProfile.value?.copy(displayName = trimmed)
                vm.showSnackbar("Το όνομά σας ενημερώθηκε σε «$trimmed»")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Σφάλμα ενημέρωσης: ${e.localizedMessage}")
            }
        }
    }

    fun updateUserFullProfile(
        firstName: String,
        lastName: String,
        birthDate: String,
        phoneNumber: String,
        occupation: String,
        city: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = vm.auth.currentUser ?: vm._currentUser.value ?: run {
            onError("Δεν βρέθηκε συνδεδεμένος χρήστης")
            return
        }
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val current = vm._userProfile.value ?: loadUserProfileFromPrefs() ?: UserProfile(uid = user.uid, email = user.email.orEmpty())
        val resolvedFirst = cleanFirst.ifBlank { current.firstName.ifBlank { user.displayName?.split(" ")?.firstOrNull() ?: "" } }
        val resolvedLast = cleanLast.ifBlank {
            current.lastName.ifBlank {
                val parts = user.displayName?.split(" ") ?: emptyList()
                if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            }
        }
        val fullName = "$resolvedFirst $resolvedLast".trim().ifBlank { user.displayName?.ifBlank { null } ?: "Χρήστης Finsay" }
        val optimisticProfile = current.copy(
            displayName = fullName,
            firstName = resolvedFirst,
            lastName = resolvedLast,
            birthDate = birthDate.trim().ifBlank { current.birthDate },
            phoneNumber = phoneNumber.trim().ifBlank { current.phoneNumber },
            occupation = occupation.trim().ifBlank { current.occupation },
            city = city.trim().ifBlank { current.city }
        )
        vm._userProfile.value = optimisticProfile
        saveUserProfileToPrefs(optimisticProfile)

        try {
            val changeReq = UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
            user.updateProfile(changeReq)
        } catch (_: Exception) {}

        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                val res = vm.firestoreSyncRepo.updateUserFullProfile(
                    uid = user.uid,
                    firstName = resolvedFirst,
                    lastName = resolvedLast,
                    birthDate = birthDate.trim().ifBlank { current.birthDate },
                    phoneNumber = phoneNumber.trim().ifBlank { current.phoneNumber },
                    occupation = occupation.trim().ifBlank { current.occupation },
                    city = city.trim().ifBlank { current.city }
                )
                res.onSuccess { updated ->
                    val merged = (vm._userProfile.value ?: optimisticProfile).copy(
                        displayName = updated.displayName.ifBlank { optimisticProfile.displayName },
                        firstName = updated.firstName.ifBlank { optimisticProfile.firstName },
                        lastName = updated.lastName.ifBlank { optimisticProfile.lastName },
                        birthDate = updated.birthDate.ifBlank { optimisticProfile.birthDate },
                        phoneNumber = updated.phoneNumber.ifBlank { optimisticProfile.phoneNumber },
                        occupation = updated.occupation.ifBlank { optimisticProfile.occupation },
                        city = updated.city.ifBlank { optimisticProfile.city },
                        email = updated.email.ifBlank { optimisticProfile.email },
                        householdId = updated.householdId.ifBlank { optimisticProfile.householdId },
                        householdIds = if (updated.householdIds.isNotEmpty()) updated.householdIds else optimisticProfile.householdIds,
                        photoUrl = updated.photoUrl.ifBlank { optimisticProfile.photoUrl },
                        isPremium = updated.isPremium || optimisticProfile.isPremium
                    )
                    vm._userProfile.value = merged
                    saveUserProfileToPrefs(merged)
                    val lang = Localization.Language.fromCode(vm._appLanguage.value)
                    vm.showSnackbar(if (lang == Localization.Language.ENGLISH) "Profile saved successfully!" else "Το προφίλ σας αποθηκεύτηκε επιτυχώς!")
                    onSuccess()
                }.onFailure {
                    val lang = Localization.Language.fromCode(vm._appLanguage.value)
                    vm.showSnackbar(if (lang == Localization.Language.ENGLISH) "Saved locally (syncing with cloud...)" else "Τα στοιχεία αποθηκεύτηκαν τοπικά (συγχρονισμός με cloud...)")
                    onSuccess()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Τα στοιχεία αποθηκεύτηκαν τοπικά")
                onSuccess()
            }
        }
    }

    fun updateUserProfilePhoto(photoUrl: String) {
        val user = vm._currentUser.value ?: return
        vm.viewModelScope.launch(vm.coroutineExceptionHandler) {
            try {
                vm.firestoreSyncRepo.updateUserProfilePhoto(user.uid, photoUrl)
                vm._userProfile.value = vm._userProfile.value?.copy(photoUrl = photoUrl)
                vm.showSnackbar("Η εικόνα προφίλ ενημερώθηκε επιτυχώς")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                vm.showSnackbar("Αποτυχία ενημέρωσης εικόνας προφίλ")
            }
        }
    }

    fun acceptPrivacyConsent() {
        vm._showPrivacyConsentDialog.value = false
        val uid = vm.auth.currentUser?.uid ?: vm._currentUser.value?.uid
        val editor = vm.prefs.edit().putBoolean("gdpr_privacy_consent_accepted", true)
        if (!uid.isNullOrBlank()) {
            editor.putBoolean("gdpr_privacy_consent_accepted_$uid", true)
        }
        editor.apply()
        vm.showSnackbar(if (vm._appLanguage.value == "EN") "Privacy settings saved" else "Η συγκατάθεση αποθηκεύτηκε επιτυχώς")
    }

    fun dismissOnboardingCarousel() {
        vm._showOnboardingCarousel.value = false
    }

    fun dismissWelcomeCarousel() {
        vm._showWelcomeCarousel.value = false
        val uid = vm.auth.currentUser?.uid ?: vm._currentUser.value?.uid
        val editor = vm.prefs.edit()
            .putBoolean("onboarding_shown_v3", true)
            .putBoolean("gdpr_privacy_consent_accepted", true)
        if (!uid.isNullOrBlank()) {
            editor.putBoolean("onboarding_shown_$uid", true)
            editor.putBoolean("gdpr_privacy_consent_accepted_$uid", true)
        }
        editor.apply()
    }

    fun saveUserProfileToPrefs(profile: UserProfile) {
        try {
            val cleanHouseholdIds = profile.householdIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
            vm.prefs.edit()
                .putString("cached_user_uid", profile.uid)
                .putString("cached_user_email", profile.email)
                .putString("cached_user_display_name", profile.displayName)
                .putString("cached_user_first_name", profile.firstName)
                .putString("cached_user_last_name", profile.lastName)
                .putString("cached_user_birth_date", profile.birthDate)
                .putString("cached_user_phone", profile.phoneNumber)
                .putString("cached_user_occupation", profile.occupation)
                .putString("cached_user_city", profile.city)
                .putString("cached_user_photo_url", profile.photoUrl)
                .putBoolean("cached_user_is_premium", profile.isPremium)
                .putString("cached_user_household_id", profile.householdId)
                .putStringSet("cached_user_household_ids", cleanHouseholdIds)
                .putString("cached_user_app_language", profile.appLanguage)
                .putString("cached_user_theme_mode", profile.themeMode)
                .putBoolean("cached_user_reminder_enabled", profile.reminderEnabled)
                .putInt("cached_user_reminder_hour", profile.reminderHour)
                .putInt("cached_user_reminder_minute", profile.reminderMinute)
                .putBoolean("cached_user_notification_consent_completed", profile.notificationConsentCompleted)
                .apply()
        } catch (t: Throwable) {
            Log.w("OikoNomosVM", "Failed to cache user profile to prefs: ${t.message}")
        }
    }

    fun loadUserProfileFromPrefs(): UserProfile? {
        return try {
            val uid = vm.prefs.getString("cached_user_uid", null) ?: return null
            if (uid.isBlank()) return null
            val cachedHouseholdIds = vm.prefs.getStringSet("cached_user_household_ids", emptySet())
                ?.map { it.trim() }?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
            UserProfile(
                uid = uid,
                email = vm.prefs.getString("cached_user_email", "").orEmpty(),
                displayName = vm.prefs.getString("cached_user_display_name", "").orEmpty(),
                firstName = vm.prefs.getString("cached_user_first_name", "").orEmpty(),
                lastName = vm.prefs.getString("cached_user_last_name", "").orEmpty(),
                birthDate = vm.prefs.getString("cached_user_birth_date", "").orEmpty(),
                phoneNumber = vm.prefs.getString("cached_user_phone", "").orEmpty(),
                occupation = vm.prefs.getString("cached_user_occupation", "").orEmpty(),
                city = vm.prefs.getString("cached_user_city", "").orEmpty(),
                photoUrl = vm.prefs.getString("cached_user_photo_url", "").orEmpty(),
                isPremium = vm.prefs.getBoolean("cached_user_is_premium", false),
                householdId = vm.prefs.getString("cached_user_household_id", "").orEmpty(),
                householdIds = cachedHouseholdIds,
                appLanguage = vm.prefs.getString("cached_user_app_language", "EL").orEmpty(),
                themeMode = vm.prefs.getString("cached_user_theme_mode", "SYSTEM").orEmpty(),
                reminderEnabled = vm.prefs.getBoolean("cached_user_reminder_enabled", false),
                reminderHour = vm.prefs.getInt("cached_user_reminder_hour", 21).coerceIn(0, 23),
                reminderMinute = vm.prefs.getInt("cached_user_reminder_minute", 30).coerceIn(0, 59),
                notificationConsentCompleted = vm.prefs.getBoolean("cached_user_notification_consent_completed", false)
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun clearUserProfileFromPrefs() {
        try {
            vm.prefs.edit()
                .remove("cached_user_uid")
                .remove("cached_user_email")
                .remove("cached_user_display_name")
                .remove("cached_user_first_name")
                .remove("cached_user_last_name")
                .remove("cached_user_birth_date")
                .remove("cached_user_phone")
                .remove("cached_user_occupation")
                .remove("cached_user_city")
                .remove("cached_user_photo_url")
                .remove("cached_user_is_premium")
                .remove("cached_user_household_id")
                .remove("cached_user_household_ids")
                .remove("cached_user_app_language")
                .remove("cached_user_theme_mode")
                .remove("cached_user_reminder_enabled")
                .remove("cached_user_reminder_hour")
                .remove("cached_user_reminder_minute")
                .remove("cached_user_notification_consent_completed")
                .apply()
        } catch (_: Throwable) {}
    }
}
