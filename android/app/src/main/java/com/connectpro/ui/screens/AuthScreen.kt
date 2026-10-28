package com.connectpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.connectpro.data.auth.AuthRepository
import com.connectpro.data.network.ApiException
import kotlinx.coroutines.launch

private enum class AuthMode {
    LOGIN,
    REGISTER,
    VERIFY,
    RESET_REQUEST,
    RESET_CONFIRM
}

@Composable
fun AuthScreen(
    repository: AuthRepository,
    onAuthenticated: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var phoneCode by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var resetIdentifier by remember { mutableStateOf("") }

    var phoneVerified by remember { mutableStateOf(false) }
    var emailVerified by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun launchAction(
        successMessage: String,
        action: suspend () -> Unit,
        onSuccess: () -> Unit = {}
    ) {
        if (busy) return

        scope.launch {
            busy = true
            message = null
            isError = false

            try {
                action()
                message = successMessage
                onSuccess()
            } catch (exception: kotlinx.coroutines.CancellationException) {
                throw exception
            } catch (exception: Exception) {
                message = when (exception) {
                    is ApiException -> exception.message
                    else -> "Unable to complete the request. Check your connection and try again."
                }
                isError = true
            } finally {
                busy = false
            }
        }
    }

    fun validPin(value: String): Boolean =
        value.length in 6..64 &&
                value.all { it.isLetterOrDigit() }

    fun validPhone(value: String): Boolean =
        Regex("^\\+[1-9]\\d{7,14}$").matches(value.trim())

    fun validEmail(value: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS
            .matcher(value.trim())
            .matches() &&
                value.trim().length <= 254

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ConnectPro",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = when (mode) {
                AuthMode.LOGIN -> "Sign in to your account"
                AuthMode.REGISTER -> "Create your account"
                AuthMode.VERIFY -> "Verify your contact details"
                AuthMode.RESET_REQUEST -> "Reset your PIN"
                AuthMode.RESET_CONFIRM -> "Enter your reset code"
            },
            style = MaterialTheme.typography.titleLarge
        )

        when (mode) {
            AuthMode.LOGIN -> {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone number (+country code)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        when {
                            !validPhone(phoneNumber) ->
                                message = "Enter a valid international phone number."
                            !validPin(pin) ->
                                message = "PIN must be 6–64 letters or numbers."
                            else -> launchAction(
                                successMessage = "Signed in successfully.",
                                action = {
                                    repository.login(phoneNumber, pin)
                                },
                                onSuccess = onAuthenticated
                            )
                        }
                        if (!validPhone(phoneNumber) || !validPin(pin)) {
                            isError = true
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            message = null
                            mode = AuthMode.REGISTER
                        }
                    ) {
                        Text("Create account")
                    }

                    TextButton(
                        onClick = {
                            resetIdentifier = phoneNumber
                            message = null
                            mode = AuthMode.RESET_REQUEST
                        }
                    ) {
                        Text("Forgot PIN?")
                    }
                }
            }

            AuthMode.REGISTER -> {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone number (+country code)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Create PIN (6–64 letters/numbers)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        when {
                            !validPhone(phoneNumber) ->
                                message = "Enter a valid international phone number."
                            !validEmail(email) ->
                                message = "Enter a valid email address."
                            !validPin(pin) ->
                                message = "PIN must be 6–64 letters or numbers."
                            else -> launchAction(
                                successMessage = "Account created. Verify both contact methods.",
                                action = {
                                    repository.register(
                                        phoneNumber,
                                        email,
                                        pin
                                    )
                                },
                                onSuccess = {
                                    phoneVerified = false
                                    emailVerified = false
                                    phoneCode = ""
                                    emailCode = ""
                                    mode = AuthMode.VERIFY
                                }
                            )
                        }

                        if (!validPhone(phoneNumber) ||
                            !validEmail(email) ||
                            !validPin(pin)
                        ) {
                            isError = true
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Register")
                }

                TextButton(onClick = {
                    message = null
                    mode = AuthMode.LOGIN
                }) {
                    Text("Back to sign in")
                }
            }

            AuthMode.VERIFY -> {
                Text(
                    text = "Phone: $phoneNumber\nEmail: $email",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = phoneCode,
                    onValueChange = { phoneCode = it.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit phone verification code") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (phoneCode.length != 6) {
                            message = "Enter the 6-digit phone verification code."
                            isError = true
                        } else {
                            launchAction(
                                successMessage = "Phone verified.",
                                action = {
                                    repository.verifyPhone(
                                        phoneNumber,
                                        phoneCode
                                    )
                                },
                                onSuccess = { phoneVerified = true }
                            )
                        }
                    },
                    enabled = !busy && !phoneVerified,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (phoneVerified) "Phone verified" else "Verify phone")
                }

                OutlinedTextField(
                    value = emailCode,
                    onValueChange = { emailCode = it.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit email verification code") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (emailCode.length != 6) {
                            message = "Enter the 6-digit email verification code."
                            isError = true
                        } else {
                            launchAction(
                                successMessage = "Email verified.",
                                action = {
                                    repository.verifyEmail(
                                        email,
                                        emailCode
                                    )
                                },
                                onSuccess = { emailVerified = true }
                            )
                        }
                    },
                    enabled = !busy && !emailVerified,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (emailVerified) "Email verified" else "Verify email")
                }

                if (phoneVerified && emailVerified) {
                    Button(
                        onClick = {
                            pin = ""
                            message = "Both contact methods verified. You can now sign in."
                            isError = false
                            mode = AuthMode.LOGIN
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue to sign in")
                    }
                }

                TextButton(onClick = {
                    message = null
                    mode = AuthMode.LOGIN
                }) {
                    Text("Back to sign in")
                }
            }

            AuthMode.RESET_REQUEST -> {
                OutlinedTextField(
                    value = resetIdentifier,
                    onValueChange = { resetIdentifier = it },
                    label = { Text("Registered phone number or email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (resetIdentifier.isBlank()) {
                            message = "Enter your registered phone number or email."
                            isError = true
                        } else {
                            launchAction(
                                successMessage = "If the account exists, a reset code will be sent.",
                                action = {
                                    repository.requestPinReset(resetIdentifier)
                                },
                                onSuccess = {
                                    resetCode = ""
                                    newPin = ""
                                    mode = AuthMode.RESET_CONFIRM
                                }
                            )
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request reset code")
                }

                TextButton(onClick = {
                    message = null
                    mode = AuthMode.LOGIN
                }) {
                    Text("Back to sign in")
                }
            }

            AuthMode.RESET_CONFIRM -> {
                OutlinedTextField(
                    value = resetIdentifier,
                    onValueChange = { resetIdentifier = it },
                    label = { Text("Registered phone number or email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = resetCode,
                    onValueChange = { resetCode = it.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit reset code") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN (6–64 letters/numbers)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        when {
                            resetCode.length != 6 -> {
                                message = "Enter the 6-digit reset code."
                                isError = true
                            }
                            !validPin(newPin) -> {
                                message = "PIN must be 6–64 letters or numbers."
                                isError = true
                            }
                            else -> launchAction(
                                successMessage = "PIN reset. Please sign in with your new PIN.",
                                action = {
                                    repository.resetPin(
                                        resetIdentifier,
                                        resetCode,
                                        newPin
                                    )
                                },
                                onSuccess = {
                                    pin = ""
                                    mode = AuthMode.LOGIN
                                }
                            )
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset PIN")
                }

                TextButton(onClick = {
                    message = null
                    mode = AuthMode.RESET_REQUEST
                }) {
                    Text("Request another reset")
                }

                TextButton(onClick = {
                    message = null
                    mode = AuthMode.LOGIN
                }) {
                    Text("Back to sign in")
                }
            }
        }

        if (busy) {
            CircularProgressIndicator()
        }

        message?.let { currentMessage ->
            Text(
                text = currentMessage,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
