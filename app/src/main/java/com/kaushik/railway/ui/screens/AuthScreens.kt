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
import com.kaushik.railway.ui.components.LabeledField
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, Color(0xFF1B3A6B)))),
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
            Text("RAIL CONNECT", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Indian Railways  •  e-Ticketing", color = Color(0xFFFFCC80), fontSize = 13.sp)
        }
    }
}

@Composable
fun LoginScreen(vm: AppViewModel, onLogin: () -> Unit, onRegister: () -> Unit) {
    var id by remember { mutableStateOf("demo_user") }
    var pass by remember { mutableStateOf("demo123") }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFFF7F0)).padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("IRCTC login", color = Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Use your Rail Connect user ID", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        LabeledField("User ID", id, onValue = { id = it })
        Spacer(Modifier.height(12.dp))
        LabeledField("Password", pass, onValue = { pass = it })
        Spacer(Modifier.height(20.dp))
        OrangeButton("LOGIN") {
            vm.userId = id.ifBlank { "demo_user" }
            vm.loggedIn = true
            onLogin()
        }
        TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
            Text("New user? Register here", color = Orange)
        }
        Text(
            "Demo credentials are pre-filled. This is a student project and is not affiliated with IRCTC.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun RegisterScreen(vm: AppViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFFF7F0)).padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Create account", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LabeledField("Full name", name, onValue = { name = it })
        Spacer(Modifier.height(10.dp))
        LabeledField("Email", email, onValue = { email = it })
        Spacer(Modifier.height(10.dp))
        LabeledField("Mobile", mobile, onValue = { mobile = it })
        Spacer(Modifier.height(10.dp))
        LabeledField("Choose user ID", user, onValue = { user = it })
        Spacer(Modifier.height(10.dp))
        LabeledField("Password", "••••••••", onValue = { })
        Spacer(Modifier.height(20.dp))
        OrangeButton("REGISTER & CONTINUE") {
            vm.userName = name.ifBlank { "Passenger" }
            vm.userId = user.ifBlank { "new_user" }
            vm.email = email
            vm.mobile = mobile
            vm.loggedIn = true
            onDone()
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to login", color = Navy) }
    }
}
