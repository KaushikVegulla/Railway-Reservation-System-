package com.kaushik.railway.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.data.AuthApi
import com.kaushik.railway.data.SessionStore
import com.kaushik.railway.RailApp
import com.kaushik.railway.data.UnverifiedException
import com.kaushik.railway.ui.components.LabeledField
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.theme.LiquidGlassBackdrop
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private fun friendlyNetError(msg: String?): String {
    val m = msg.orEmpty()
    if (m.contains("failed to connect", true) || m.contains("timed out", true) || m.contains("Unable to resolve", true)) {
        return "No internet. Turn on mobile data or Wi‑Fi and retry."
    }
    return m.ifBlank { "Request failed" }
}

@Composable
fun SplashScreen(ready: Boolean, onDone: () -> Unit) {
    val gate = remember { AtomicBoolean(false) }
    LaunchedEffect(ready) {
        if (!ready || gate.get()) return@LaunchedEffect
        delay(900)
        if (gate.compareAndSet(false, true)) onDone()
    }
    LaunchedEffect(Unit) {
        delay(2500)
        if (gate.compareAndSet(false, true)) onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF062A66), Navy))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(Orange),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Train, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("RailX", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Centre for Railway Information Systems", color = Color(0xFFFFCC80), fontSize = 12.sp)
            Text("Indian Railways  •  e-Ticketing", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
fun LoginScreen(vm: AppViewModel, onLogin: () -> Unit, onRegister: () -> Unit, onNeedVerify: (String) -> Unit) {
    var user by remember(vm.loginPrefill) { mutableStateOf(vm.loginPrefill.ifBlank { vm.userId }) }
    var pass by remember { mutableStateOf("") }
    var impaired by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var forgotOpen by remember { mutableStateOf(false) }
    var forgotKey by remember { mutableStateOf("") }
    var forgotInfo by remember { mutableStateOf<String?>(null) }
    LiquidGlassBackdrop {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        RailCard {
        Column {
        Text("SIGN IN", color = Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("User ID and password", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LabeledField("User ID", user, onValue = { user = it })
        Spacer(Modifier.height(12.dp))
        LabeledField("Password", pass, isPassword = true, onValue = { pass = it })
        TextButton(onClick = { forgotOpen = true; forgotInfo = null }, modifier = Modifier.fillMaxWidth()) {
            Text("Forgot account details?", color = Navy)
        }
        CheckRow(
            checked = impaired,
            text = "Visually impaired user: receive OTP instead of captcha",
            onChange = { impaired = it }
        )
        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        OrangeButton(if (busy) "PLEASE WAIT" else "SIGN IN", enabled = !busy) {
            if (user.isBlank() || pass.isBlank()) {
                error = "Enter the user ID and password"
                return@OrangeButton
            }
            busy = true
            error = null
            vm.signIn(
                idOrEmail = user.trim(),
                password = pass,
                onSuccess = {
                    busy = false
                    onLogin()
                },
                onNeedVerify = { email ->
                    busy = false
                    vm.email = email
                    onNeedVerify(email)
                },
                onError = {
                    busy = false
                    error = it
                }
            )
        }
        TextButton(onClick = {
            vm.startRegistration(impaired)
            onRegister()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Register user?", color = Orange)
        }
        }
        }
        Text(
            "Academic demo inspired by the IRCTC account flow. Not an official railway login.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    }
    if (forgotOpen) {
        AlertDialog(
            onDismissRequest = { forgotOpen = false },
            title = { Text("Forgot account details") },
            text = {
                Column {
                    Text("Enter the registered email or mobile. This demo only shows the user ID on this device.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    LabeledField("Email or mobile", forgotKey, keyboardType = KeyboardType.Email) { forgotKey = it }
                    forgotInfo?.let { Text(it, color = Navy, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.lookupUserId(forgotKey) { id ->
                        forgotInfo = if (id.isNullOrBlank()) "No account matched on this device."
                        else "Your user ID is $id"
                    }
                }) { Text("Find user ID", color = Orange) }
            },
            dismissButton = {
                TextButton(onClick = { forgotOpen = false }) { Text("Close", color = Navy) }
            }
        )
    }
}

@Composable
fun RegisterScreen(vm: AppViewModel, onNeedVerify: (String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LiquidGlassBackdrop {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        RailCard {
        Column {
        Text("Create account", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("We’ll send a 6-digit verification code", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        LabeledField("Full name", name, onValue = { name = it })
        Spacer(Modifier.height(10.dp))
        LabeledField("Email", email, onValue = { email = it })
        Spacer(Modifier.height(10.dp))
        LabeledField("Password (6+ characters)", pass, isPassword = true, onValue = { pass = it })
        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(20.dp))
        OrangeButton(if (busy) "SENDING CODE…" else "REGISTER", enabled = !busy) {
            busy = true
            error = null
            scope.launch {
                try {
                    val otp = withContext(Dispatchers.IO) {
                        AuthApi.register(name.trim(), email.trim(), pass)
                    }
                    vm.userName = name.trim()
                    vm.email = email.trim()
                    vm.otpEmailed = otp.emailed
                    vm.otpDisplayCode = if (otp.emailed) null else otp.code
                    onNeedVerify(email.trim())
                } catch (e: Exception) {
                    error = friendlyNetError(e.message)
                } finally {
                    busy = false
                }
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to login", color = Navy) }
        }
        }
    }
    }
}

@Composable
fun VerifyEmailScreen(vm: AppViewModel, email: String, onVerified: () -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf(vm.otpDisplayCode.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember {
        mutableStateOf(
            if (vm.otpEmailed) "Code sent to $email"
            else "Email delivery is blocked for this address. Use the in-app code below."
        )
    }

    DisposableEffect(email) {
        onDispose {
            info = if (vm.otpEmailed) "Code sent to $email" else "Email delivery is blocked for this address. Use the in-app code below."
        }
    }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(Color.Transparent).systemBarsPadding().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Verify email", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(info, color = Color.Gray, fontSize = 14.sp)
        vm.otpDisplayCode?.let { shown ->
            Spacer(Modifier.height(12.dp))
            Text("Your code", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                shown,
                color = Orange,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        LabeledField("6-digit code", code, onValue = { code = it.filter { ch -> ch.isDigit() }.take(6) })
        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(20.dp))
        OrangeButton(if (busy) "VERIFYING…" else "VERIFY & CONTINUE", enabled = !busy) {
            busy = true
            error = null
            vm.verifyEmail(code) { message ->
                busy = false
                if (message == null) onVerified() else error = message
            }
        }
        TextButton(
            onClick = {
                scope.launch {
                    vm.resendCode { message ->
                        if (message == null) {
                            info = if (vm.otpEmailed) "New code sent to $email" else "A new in-app code is shown below."
                            vm.otpDisplayCode?.let { code = it }
                        } else {
                            error = message
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Resend code", color = Orange) }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Navy) }
        Text(
            if (vm.otpEmailed) "Check inbox and spam. Codes expire in 10 minutes."
            else "Resend’s test sender cannot email this address. Codes expire in 10 minutes.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
