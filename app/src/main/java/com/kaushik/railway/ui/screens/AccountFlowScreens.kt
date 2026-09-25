package com.kaushik.railway.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.data.IrctcRules
import com.kaushik.railway.ui.components.LabeledField
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange
import com.kaushik.railway.ui.theme.Success
import kotlinx.coroutines.launch

@Composable
fun AccountRegisterScreen(vm: AppViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val steps = if (vm.regVisuallyImpaired) listOf(0, 1, 3, 4, 5) else listOf(0, 1, 2, 3, 4, 5)
    LaunchedEffect(vm.regVisuallyImpaired) {
        if (vm.regStep !in steps) vm.regStep = steps.first()
    }
    val pos = steps.indexOf(vm.regStep).let { if (it < 0) 0 else it }

    fun goNext() {
        if (pos < steps.lastIndex) vm.regStep = steps[pos + 1]
    }

    fun goBack() {
        if (pos == 0) onBack() else vm.regStep = steps[pos - 1]
    }

    FlowPage {
        when (vm.regStep) {
            0 -> BasicDetailsStep(vm, steps.size, pos + 1, onNext = { goNext() }, onBack = { goBack() })
            1 -> SecurityStep(vm, steps.size, pos + 1, onNext = { goNext() }, onBack = { goBack() })
            2 -> CaptchaStep(vm, steps.size, pos + 1, onNext = { goNext() }, onBack = { goBack() })
            3 -> ConfirmContactsStep(vm, steps.size, pos + 1, onNext = { goNext() }, onBack = { goBack() })
            4 -> OtpStep(vm, steps.size, pos + 1, onNext = { goNext() }, onBack = { goBack() })
            else -> AccountCreatedStep(vm, onDone = onDone)
        }
    }
}

@Composable
private fun BasicDetailsStep(
    vm: AppViewModel,
    total: Int,
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var error by remember { mutableStateOf<String?>(null) }
    StepHeading(step, total, "Create your account", "Basic details, as per your Govt. ID")
    RailCard {
        ColumnBlock {
            LabeledField("User ID", vm.regUserId) {
                vm.regUserId = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(35)
                vm.regIdMessage = ""
                vm.regIdChecked = ""
            }
            TextButton(onClick = { vm.checkUserId() }, modifier = Modifier.fillMaxWidth()) {
                Text("Check availability", color = Orange)
            }
            if (vm.regIdMessage.isNotBlank()) {
                val ok = vm.regIdMessage == "User ID is available"
                Text(vm.regIdMessage, color = if (ok) Success else Color.Red, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            LabeledField("Full name (as per Govt. ID)", vm.regFullName) { vm.regFullName = it }
            error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            OrangeButton("CONTINUE") {
                val idError = IrctcRules.validateUserId(vm.regUserId)
                val nameError = IrctcRules.validateFullName(vm.regFullName)
                error = when {
                    idError != null -> idError
                    !vm.regUserId.trim().equals(vm.regIdChecked, ignoreCase = true) ->
                        "Check that this user ID is available"
                    nameError != null -> nameError
                    else -> null
                }
                if (error == null) onNext()
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to sign in", color = Navy) }
        }
    }
}

@Composable
private fun SecurityStep(
    vm: AppViewModel,
    total: Int,
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    StepHeading(step, total, "Password, mobile and email", "These are used to verify the new user ID")
    RailCard {
        ColumnBlock {
            LabeledField("Password", vm.regPassword, isPassword = true) { vm.regPassword = it.take(15) }
            Spacer(Modifier.height(8.dp))
            LabeledField("Confirm password", confirm, isPassword = true) { confirm = it.take(15) }
            Spacer(Modifier.height(8.dp))
            IrctcRules.passwordRules.forEach { rule ->
                Text("• $rule", color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            LabeledField("Mobile number (+91)", vm.regMobile, keyboardType = KeyboardType.Number) {
                vm.regMobile = it.filter { ch -> ch.isDigit() }.take(10)
            }
            Spacer(Modifier.height(8.dp))
            LabeledField("Email", vm.regEmail, keyboardType = KeyboardType.Email) { vm.regEmail = it }
            Spacer(Modifier.height(8.dp))
            ChoiceField("Preferred language", vm.regLanguage, IrctcRules.languages) { vm.regLanguage = it }
            error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            OrangeButton(if (busy) "PLEASE WAIT" else "CONTINUE", enabled = !busy) {
                val problem = IrctcRules.validatePassword(vm.regPassword)
                    ?: (if (vm.regPassword != confirm) "Password and confirm password do not match" else null)
                    ?: IrctcRules.validateMobile(vm.regMobile)
                    ?: IrctcRules.validateEmail(vm.regEmail)
                if (problem != null) {
                    error = problem
                    return@OrangeButton
                }
                busy = true
                error = null
                scope.launch {
                    vm.validateNewContact { message ->
                        busy = false
                        error = message
                        if (message == null) onNext()
                    }
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Navy) }
        }
    }
    DemoNote("Academic demo. The password is stored only as a hash on this device.")
}

@Composable
private fun CaptchaStep(
    vm: AppViewModel,
    total: Int,
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var typed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (vm.regCaptcha.isBlank()) vm.refreshCaptcha()
    }
    StepHeading(step, total, "Captcha", "Type the characters shown below")
    RailCard {
        ColumnBlock {
            Text(
                vm.regCaptcha,
                color = Navy,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
            TextButton(onClick = { vm.refreshCaptcha(); typed = "" }, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh captcha", color = Orange)
            }
            LabeledField("Enter captcha", typed) { typed = it.take(5) }
            error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            OrangeButton("SUBMIT") {
                if (IrctcRules.captchaMatches(vm.regCaptcha, typed)) {
                    if (vm.regEmailOtp.isBlank()) vm.issueOtps()
                    onNext()
                } else {
                    error = "Captcha does not match"
                    vm.refreshCaptcha()
                    typed = ""
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Navy) }
        }
    }
}

@Composable
private fun ConfirmContactsStep(
    vm: AppViewModel,
    total: Int,
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (vm.regEmailOtp.isBlank() || vm.regMobileOtp.isBlank()) vm.issueOtps()
    }
    StepHeading(step, total, "Confirm email and mobile", "Check the user ID and masked contacts")
    RailCard {
        ColumnBlock {
            Text("User ID", color = Color.Gray, fontSize = 12.sp)
            Text(vm.regUserId, color = Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("Email  ${IrctcRules.maskEmail(vm.regEmail)}", color = Navy, fontSize = 14.sp)
            Text("Mobile  +91 ${IrctcRules.maskMobile(vm.regMobile)}", color = Navy, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("Demo codes — not sent to a real inbox or phone", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Email OTP  ${vm.regEmailOtp}", color = Navy, fontWeight = FontWeight.Bold)
            Text("Mobile OTP  ${vm.regMobileOtp}", color = Navy, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OrangeButton("CONTINUE") { onNext() }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Navy) }
        }
    }
}

@Composable
private fun OtpStep(
    vm: AppViewModel,
    total: Int,
    step: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var emailCode by remember { mutableStateOf("") }
    var mobileCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    StepHeading(step, total, "Enter email and mobile OTPs", "Use the demo codes from the previous step")
    RailCard {
        ColumnBlock {
            LabeledField("Email OTP", emailCode, keyboardType = KeyboardType.Number) {
                emailCode = it.filter { ch -> ch.isDigit() }.take(6)
            }
            Spacer(Modifier.height(8.dp))
            LabeledField("Mobile OTP", mobileCode, keyboardType = KeyboardType.Number) {
                mobileCode = it.filter { ch -> ch.isDigit() }.take(6)
            }
            Text(
                "Email ${vm.regEmailOtp}   ·   Mobile ${vm.regMobileOtp}",
                color = Orange,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            OrangeButton(if (busy) "PLEASE WAIT" else "VERIFY", enabled = !busy) {
                error = when {
                    !IrctcRules.otpMatches(vm.regEmailOtp, emailCode) -> "Email OTP does not match"
                    !IrctcRules.otpMatches(vm.regMobileOtp, mobileCode) -> "Mobile OTP does not match"
                    else -> null
                }
                if (error != null) return@OrangeButton
                busy = true
                vm.finishRegistration { message ->
                    busy = false
                    if (message == null) onNext() else error = message
                }
            }
            TextButton(
                onClick = {
                    vm.issueOtps()
                    emailCode = ""
                    mobileCode = ""
                    error = null
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Resend demo codes", color = Orange) }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Navy) }
        }
    }
}

@Composable
private fun AccountCreatedStep(vm: AppViewModel, onDone: () -> Unit) {
    StepHeading(6, 6, "User ID created", "Sign in next to complete the profile")
    RailCard {
        ColumnBlock {
            Text(
                "Welcome ${vm.regFullName}. Your user ID ${vm.loginPrefill.ifBlank { vm.regUserId }} has been created.",
                color = Navy,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Continue to sign in. You can fill gender, date of birth, and address after that.",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            OrangeButton("CONTINUE") { onDone() }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("I'll do it later", color = Navy)
            }
        }
    }
    DemoNote("This account lives on this device. It is not an IRCTC user ID.")
}

@Composable
private fun ColumnBlock(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(content = { content() })
}
