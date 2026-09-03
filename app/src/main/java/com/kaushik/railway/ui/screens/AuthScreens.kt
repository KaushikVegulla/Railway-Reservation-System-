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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.kaushik.railway.data.UnverifiedException
import com.kaushik.railway.ui.components.LabeledField
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun friendlyNetError(msg: String?): String {
    val m = msg.orEmpty()
    if (m.contains("failed to connect", true) || m.contains("timed out", true) || m.contains("Unable to resolve", true)) {
        return "No internet. Turn on mobile data or Wi‑Fi and retry."
    }
    return m.ifBlank { "Request failed" }
}

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        onDone()
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
            Text("RailOne", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Centre for Railway Information Systems", color = Color(0xFFFFCC80), fontSize = 12.sp)
            Text("Indian Railways  •  e-Ticketing", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
fun LoginScreen(vm: AppViewModel, onLogin: () -> Unit, onRegister: () -> Unit, onNeedVerify: (String) -> Unit) {
    var email by remember { mutableStateOf(vm.email.ifBlank { "" }) }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(Color(0xFFEEF2F7)).padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("RailOne login", color = Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Email and password", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        LabeledField("Email", email, onValue = { email = it })
        Spacer(Modifier.height(12.dp))
        LabeledField("Password", pass, isPassword = true, onValue = { pass = it })
        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(20.dp))
        OrangeButton(if (busy) "PLEASE WAIT" else "LOGIN", enabled = !busy) {
            busy = true
            error = null
            scope.launch {
                try {
                    val user = withContext(Dispatchers.IO) { AuthApi.login(email.trim(), pass) }
                    vm.userName = user.name
                    vm.email = user.email
                    vm.userId = user.email
                    vm.loggedIn = true
                    onLogin()
                } catch (e: UnverifiedException) {
                    vm.email = e.email
                    onNeedVerify(e.email)
                } catch (e: Exception) {
                    error = friendlyNetError(e.message)
                } finally {
                    busy = false
                }
            }
        }
        TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
            Text("New user? Register here", color = Orange)
        }
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
    Column(
        Modifier.fillMaxSize().background(Color(0xFFEEF2F7)).padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Create account", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("We’ll email a 6-digit verification code", color = Color.Gray, fontSize = 14.sp)
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
                    withContext(Dispatchers.IO) { AuthApi.register(name.trim(), email.trim(), pass) }
                    vm.userName = name.trim()
                    vm.email = email.trim()
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

@Composable
fun VerifyEmailScreen(vm: AppViewModel, email: String, onVerified: () -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf("Code sent to $email") }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(Color(0xFFEEF2F7)).padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Verify email", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(info, color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        LabeledField("6-digit code", code, onValue = { code = it.filter { ch -> ch.isDigit() }.take(6) })
        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(20.dp))
        OrangeButton(if (busy) "VERIFYING…" else "VERIFY & CONTINUE", enabled = !busy) {
            busy = true
            error = null
            scope.launch {
                try {
                    val user = withContext(Dispatchers.IO) { AuthApi.verifyEmail(email, code) }
                    vm.userName = user.name.ifBlank { vm.userName }
                    vm.email = user.email
                    vm.userId = user.email
                    vm.loggedIn = true
                    onVerified()
                } catch (e: Exception) {
                    error = e.message
                } finally {
                    busy = false
                }
            }
        }
        TextButton(
            onClick = {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { AuthApi.resendCode(email) }
                        info = "New code sent to $email"
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Resend code", color = Orange) }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Navy) }
        Text(
            "Check inbox and spam. Codes expire in 10 minutes.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
