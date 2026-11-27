package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.LegalConstants
import com.example.ui.components.LegalDocumentsDialog
import com.example.ui.components.OikoNomosLogo
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.ui.theme.LogoGreen
import com.example.ui.theme.ModernFontFamily
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.Localization
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

@Composable
fun AuthScreen(
    viewModel: OikoNomosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val lang = Localization.Language.fromCode(appLanguage)
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    var isSignUp by remember { mutableStateOf(false) }
    var isForgotPassword by remember { mutableStateOf(false) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+30") }
    var showCountryDropdown by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }

    // Date Picker state
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val birthDateInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(birthDateInteractionSource) {
        birthDateInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                showDatePickerDialog = true
            }
        }
    }

    // Intercept back button when on Forgot Password screen
    BackHandler(enabled = isForgotPassword) {
        isForgotPassword = false
        errorMessage = null
        resetSuccessMessage = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo & App Name
            OikoNomosLogo(size = 60.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Finsay",
                fontFamily = ModernFontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = LogoGreen
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Auth Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isForgotPassword) {
                        // ============================================
                        // Password Reset Mode (Direct Firebase Auth Email Link)
                        // ============================================
                        Surface(
                            shape = CircleShape,
                            color = Emerald100,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = Localization.tr(lang, "Reset Password", "Επαναφορά Κωδικού"),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = Localization.tr(
                                lang,
                                "Enter your registered email address to receive a secure password reset link.",
                                "Εισάγετε το email του λογαριασμού σας για να λάβετε σύνδεσμο επαναφοράς κωδικού."
                            ),
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (resetSuccessMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = resetSuccessMessage ?: "",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF047857),
                                        lineHeight = 17.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    isForgotPassword = false
                                    errorMessage = null
                                    resetSuccessMessage = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = Localization.tr(lang, "Back to Sign In", "Επιστροφή στη Σύνδεση"),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                label = { Text("Email") },
                                placeholder = { Text("example@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Emerald700)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_email_field")
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    errorMessage = null
                                    viewModel.sendPasswordResetEmail(
                                        email = email,
                                        onSuccess = {
                                            resetSuccessMessage = Localization.trFormat(
                                                lang,
                                                "A password reset link has been sent to %1\$s.",
                                                "Στάλθηκε σύνδεσμος επαναφοράς στο %1\$s.",
                                                email
                                            )
                                        },
                                        onError = { errorMessage = it }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald700,
                                    contentColor = Color.White
                                ),
                                enabled = !isAuthLoading && !isGoogleSigningIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reset_submit_button")
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = Localization.tr(lang, "Send Reset Email", "Αποστολή Email Επαναφοράς"),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (resetSuccessMessage == null) {
                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = {
                                    isForgotPassword = false
                                    errorMessage = null
                                    resetSuccessMessage = null
                                },
                                modifier = Modifier.testTag("back_to_login_button")
                            ) {
                                Text(
                                    text = "← ${Localization.tr(lang, "Back to Sign In", "Επιστροφή στη Σύνδεση")}",
                                    color = Emerald700,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // ============================================
                        // Normal Sign In / Sign Up Mode
                        // ============================================

                        // Segmented Tab: Sign In / Sign Up
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isSignUp) Emerald700 else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable(enabled = !isAuthLoading && !isGoogleSigningIn) {
                                        isSignUp = false
                                        errorMessage = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = Localization.tr(lang, "Sign In", "Σύνδεση"),
                                        fontSize = 14.sp,
                                        fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isSignUp) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSignUp) Emerald700 else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable(enabled = !isAuthLoading && !isGoogleSigningIn) {
                                        isSignUp = true
                                        errorMessage = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = Localization.tr(lang, "Sign Up", "Εγγραφή"),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSignUp) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Google Sign-In / Sign-Up Button
                        OutlinedButton(
                            onClick = {
                                val serverClientId = try {
                                    context.getString(R.string.default_web_client_id)
                                } catch (e: Exception) {
                                    errorMessage = Localization.tr(
                                        lang,
                                        "Google Sign-In configuration missing",
                                        "Λείπει η ρύθμιση σύνδεσης Google"
                                    )
                                    return@OutlinedButton
                                }

                                isGoogleSigningIn = true
                                errorMessage = null
                                val credentialManager = CredentialManager.create(context)
                                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()
                                val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                                coroutineScope.launch {
                                    try {
                                        val activity = context as? Activity
                                            ?: throw IllegalStateException("Activity context unavailable")
                                        val result = credentialManager.getCredential(activity, request)
                                        val credential = result.credential
                                        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                            viewModel.signInWithGoogleToken(
                                                idToken = googleIdToken,
                                                isSignUpMode = isSignUp,
                                                onSuccess = { isGoogleSigningIn = false },
                                                onError = { err ->
                                                    isGoogleSigningIn = false
                                                    errorMessage = err
                                                }
                                            )
                                        } else {
                                            errorMessage = Localization.tr(lang, "Invalid Google credential", "Μη έγκυρο διαπιστευτήριο Google")
                                            isGoogleSigningIn = false
                                        }
                                    } catch (e: GetCredentialCancellationException) {
                                        Log.d("Auth", "Google sign-in was cancelled")
                                        isGoogleSigningIn = false
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Google Sign-In failed", e)
                                        val fallbackErr = Localization.tr(lang, "Please try again", "Προσπαθήστε ξανά")
                                        errorMessage = Localization.trFormat(
                                            lang,
                                            "Google Sign-In error: %1\$s",
                                            "Σφάλμα Google Sign-In: %1\$s",
                                            e.localizedMessage ?: fallbackErr
                                        )
                                        isGoogleSigningIn = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_auth_button"),
                            enabled = !isGoogleSigningIn && !isAuthLoading
                        ) {
                            if (isGoogleSigningIn) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_google_logo),
                                        contentDescription = "Google",
                                        tint = Emerald700,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isSignUp) {
                                            Localization.tr(lang, "Sign up with Google", "Εγγραφή με Google")
                                        } else {
                                            Localization.tr(lang, "Sign in with Google", "Σύνδεση με Google")
                                        },
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "  ${Localization.tr(lang, "or with Email", "ή με Email")}  ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Form Fields
                        if (isSignUp) {
                            // 1. Email (email type)
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                label = { Text("Email *") },
                                placeholder = { Text("example@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Emerald700)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. Κωδικός Πρόσβασης (κρυφός)
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                label = { Text("${Localization.tr(lang, "Password", "Κωδικός Πρόσβασης")} *") },
                                placeholder = { Text(Localization.tr(lang, "At least 6 characters", "Τουλάχιστον 6 χαρακτήρες")) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald700)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = Localization.tr(lang, "Show password", "Εμφάνιση κωδικού")
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. Όνομα (κείμενο)
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = {
                                    firstName = it
                                    errorMessage = null
                                },
                                label = { Text("${Localization.tr(lang, "First Name", "Όνομα")} *") },
                                placeholder = { Text(Localization.tr(lang, "e.g. John", "π.χ. Μάριος")) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Emerald700)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_first_name_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4. Επίθετο (κείμενο)
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = {
                                    lastName = it
                                    errorMessage = null
                                },
                                label = { Text("${Localization.tr(lang, "Last Name", "Επίθετο")} *") },
                                placeholder = { Text(Localization.tr(lang, "e.g. Doe", "π.χ. Παπαδόπουλος")) },
                                leadingIcon = {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = Emerald700)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_last_name_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 5. Ημερομηνία Γέννησης (ημερολόγιο)
                            OutlinedTextField(
                                value = birthDate,
                                onValueChange = { birthDate = it },
                                readOnly = true,
                                interactionSource = birthDateInteractionSource,
                                label = { Text("${Localization.tr(lang, "Date of Birth", "Ημερομηνία Γέννησης")} *") },
                                placeholder = { Text(Localization.tr(lang, "DD/MM/YYYY", "ΗΗ/ΜΜ/ΕΕΕΕ")) },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePickerDialog = true }) {
                                        Icon(Icons.Default.EditCalendar, contentDescription = Localization.tr(lang, "Pick Date", "Επιλογή Ημερομηνίας"), tint = Emerald700)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_birthdate_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 6. Κινητό Τηλέφωνο (αριθμός, με πρόθεμα χώρας μπροστά)
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    val digitsOnly = it.filter { ch -> ch.isDigit() }
                                    phoneNumber = digitsOnly
                                    errorMessage = null
                                },
                                label = { Text("${Localization.tr(lang, "Mobile Phone", "Κινητό Τηλέφωνο")} *") },
                                placeholder = { Text("69XXXXXXXX") },
                                leadingIcon = {
                                    Box {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showCountryDropdown = true }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = when (countryCode) {
                                                    "+30" -> "🇬🇷 +30"
                                                    "+357" -> "🇨🇾 +357"
                                                    "+44" -> "🇬🇧 +44"
                                                    "+1" -> "🇺🇸 +1"
                                                    "+34" -> "🇪🇸 +34"
                                                    "+33" -> "🇫🇷 +33"
                                                    "+49" -> "🇩🇪 +49"
                                                    "+39" -> "🇮🇹 +39"
                                                    "+351" -> "🇵🇹 +351"
                                                    "+31" -> "🇳🇱 +31"
                                                    "+48" -> "🇵🇱 +48"
                                                    "+40" -> "🇷🇴 +40"
                                                    else -> countryCode
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald700
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = Localization.tr(lang, "Select country prefix", "Επιλογή κωδικού χώρας"),
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showCountryDropdown,
                                            onDismissRequest = { showCountryDropdown = false }
                                        ) {
                                            listOf(
                                                "+30" to "🇬🇷 Greece (+30)",
                                                "+357" to "🇨🇾 Cyprus (+357)",
                                                "+44" to "🇬🇧 UK (+44)",
                                                "+1" to "🇺🇸 USA (+1)",
                                                "+34" to "🇪🇸 España (+34)",
                                                "+33" to "🇫🇷 France (+33)",
                                                "+49" to "🇩🇪 Deutschland (+49)",
                                                "+39" to "🇮🇹 Italia (+39)",
                                                "+351" to "🇵🇹 Portugal (+351)",
                                                "+31" to "🇳🇱 Nederland (+31)",
                                                "+48" to "🇵🇱 Polska (+48)",
                                                "+40" to "🇷🇴 România (+40)"
                                            ).forEach { (code, label) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = label,
                                                            fontSize = 13.5.sp,
                                                            fontWeight = if (code == countryCode) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (code == countryCode) Emerald700 else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        countryCode = code
                                                        showCountryDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_phone_field")
                            )
                        } else {
                            // Sign In Mode: Email & Password
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                label = { Text("Email *") },
                                placeholder = { Text("example@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Emerald700)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                label = { Text("${Localization.tr(lang, "Password", "Κωδικός Πρόσβασης")} *") },
                                placeholder = { Text(Localization.tr(lang, "At least 6 characters", "Τουλάχιστον 6 χαρακτήρες")) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald700)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = Localization.tr(lang, "Show password", "Εμφάνιση κωδικού")
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_field")
                            )
                        }

                        // Error message
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                errorMessage = null
                                if (isSignUp) {
                                    val cleanDigits = phoneNumber.trim().removePrefix("0")
                                    val fullPhone = if (phoneNumber.trim().startsWith("+")) {
                                        phoneNumber.trim()
                                    } else {
                                        "$countryCode$cleanDigits"
                                    }
                                    viewModel.signUpWithEmail(
                                        firstName = firstName,
                                        lastName = lastName,
                                        birthDate = birthDate,
                                        phoneNumber = fullPhone,
                                        occupation = "",
                                        city = "",
                                        email = email,
                                        pass = password,
                                        onSuccess = {},
                                        onError = { errorMessage = it }
                                    )
                                } else {
                                    viewModel.signInWithEmail(
                                        email = email,
                                        pass = password,
                                        onSuccess = {},
                                        onError = { errorMessage = it }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald700,
                                contentColor = Color.White
                            ),
                            enabled = !isAuthLoading && !isGoogleSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auth_submit_button")
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignUp) {
                                        Localization.tr(lang, "Create Account", "Δημιουργία Λογαριασμού")
                                    } else {
                                        Localization.tr(lang, "Sign In", "Σύνδεση")
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Requirement 4: Subtle "Forgot Password" link directly below main Sign In button
                        if (!isSignUp) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    isForgotPassword = true
                                    errorMessage = null
                                    resetSuccessMessage = null
                                },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = Localization.tr(lang, "Forgot password?", "Ξεχάσατε τον κωδικό πρόσβασης;"),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Language Selector supporting all 10 languages cleanly
            com.example.ui.components.LanguageSelectorPill(
                currentLanguageCode = appLanguage,
                onSelectLanguage = { viewModel.setAppLanguage(it) },
                modifier = Modifier
                    .widthIn(min = 210.dp, max = 300.dp)
                    .testTag("auth_language_selector")
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog for Date of Birth
    if (showDatePickerDialog) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val birthParts = birthDate.split("/").map { it.trim() }
        var dayText by remember(showDatePickerDialog) {
            mutableStateOf(if (birthParts.isNotEmpty() && birthParts[0].isNotBlank()) birthParts[0] else "")
        }
        var monthText by remember(showDatePickerDialog) {
            mutableStateOf(if (birthParts.size > 1 && birthParts[1].isNotBlank()) birthParts[1] else "")
        }
        var yearText by remember(showDatePickerDialog) {
            mutableStateOf(if (birthParts.size > 2 && birthParts[2].isNotBlank()) birthParts[2] else "")
        }

        Dialog(
            onDismissRequest = { showDatePickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald100,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = Localization.tr(lang, "Select Date of Birth", "Επιλογή Ημερομηνίας Γέννησης"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Day / Month / Year Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day Input
                        OutlinedTextField(
                            value = dayText,
                            onValueChange = { input ->
                                dayText = input.filter { ch -> ch.isDigit() }.take(2)
                            },
                            label = { Text(Localization.tr(lang, "Day", "Ημέρα")) },
                            placeholder = { Text(Localization.tr(lang, "DD", "ΗΗ")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Month Input
                        OutlinedTextField(
                            value = monthText,
                            onValueChange = { input ->
                                monthText = input.filter { ch -> ch.isDigit() }.take(2)
                            },
                            label = { Text(Localization.tr(lang, "Month", "Μήνας")) },
                            placeholder = { Text(Localization.tr(lang, "MM", "ΜΜ")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Year Input
                        OutlinedTextField(
                            value = yearText,
                            onValueChange = { input ->
                                yearText = input.filter { ch -> ch.isDigit() }.take(4)
                            },
                            label = { Text(Localization.tr(lang, "Year", "Έτος")) },
                            placeholder = { Text(Localization.tr(lang, "YYYY", "ΕΕΕΕ")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDatePickerDialog = false }) {
                            Text(text = Localization.cancel(lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val d = dayText.toIntOrNull()?.coerceIn(1, 31) ?: 1
                                val m = monthText.toIntOrNull()?.coerceIn(1, 12) ?: 1
                                val y = yearText.toIntOrNull()?.coerceIn(1920, currentYear) ?: 1995
                                val dayStr = d.toString().padStart(2, '0')
                                val monthStr = m.toString().padStart(2, '0')
                                birthDate = "$dayStr/$monthStr/$y"
                                showDatePickerDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Text(text = Localization.tr(lang, "Confirm", "Επιβεβαίωση"), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
