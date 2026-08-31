package com.kaushik.railway.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.data.Booking
import com.kaushik.railway.data.MockData
import com.kaushik.railway.ui.components.KeyValue
import com.kaushik.railway.ui.components.LabeledField
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.components.RailTopBar
import com.kaushik.railway.ui.components.StatusChip
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange

@Composable
fun PnrScreen(onBack: () -> Unit) {
    var pnr by remember { mutableStateOf("4521987630") }
    var result by remember { mutableStateOf<Booking?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { RailTopBar("PNR status", onBack) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(Color(0xFFFFF7F0)).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            LabeledField("10-digit PNR", pnr, onValue = { value ->
                pnr = value.filter { ch -> ch.isDigit() }.take(10)
            })
            Spacer(Modifier.height(12.dp))
            OrangeButton("GET STATUS") {
                result = MockData.pnrLookup(pnr)
                error = if (result == null) "Enter a valid 10-digit PNR (demo accepts any 10 digits)." else null
            }
            error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            result?.let { b ->
                Spacer(Modifier.height(16.dp))
                RailCard {
                    Column {
                        Text("PNR ${b.pnr}", fontWeight = FontWeight.Black, color = Orange, fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        StatusChip(b.status)
                        Spacer(Modifier.height(8.dp))
                        KeyValue("Train", "${b.train.number} ${b.train.name}")
                        KeyValue("From", b.fromName)
                        KeyValue("To", b.toName)
                        KeyValue("Date", b.date)
                        KeyValue("Class", b.travelClass.code)
                        Spacer(Modifier.height(8.dp))
                        b.passengers.forEachIndexed { i, p ->
                            Text("Passenger ${i + 1}: ${p.name} — ${b.status}", fontSize = 13.sp, color = Navy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RunningStatusScreen(onBack: () -> Unit) {
    var trainNo by remember { mutableStateOf("12952") }
    var show by remember { mutableStateOf(true) }
    Scaffold(topBar = { RailTopBar("Live running status", onBack) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(Color(0xFFFFF7F0)).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            LabeledField("Train number", trainNo, onValue = { trainNo = it })
            Spacer(Modifier.height(12.dp))
            OrangeButton("GET STATUS") { show = true }
            if (show) {
                Spacer(Modifier.height(12.dp))
                Text("Mumbai Rajdhani  •  delayed by 18 min", fontWeight = FontWeight.Bold, color = Navy)
                Spacer(Modifier.height(8.dp))
                MockData.runningStops(trainNo).forEach { stop ->
                    RailCard(Modifier.padding(bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("${stop.station} (${stop.code})", fontWeight = FontWeight.SemiBold, color = Navy)
                                Text("Arr ${stop.schArr}   Dep ${stop.schDep}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Column {
                                StatusChip(stop.status)
                                if (stop.delayMin > 0) Text("+${stop.delayMin} min", color = Orange, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit, userId: String, userName: String, email: String, mobile: String, onLogout: () -> Unit) {
    Scaffold(topBar = { RailTopBar("My profile", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color(0xFFFFF7F0)).padding(16.dp)) {
            RailCard {
                Column {
                    KeyValue("Name", userName)
                    KeyValue("User ID", userId)
                    KeyValue("Email", email)
                    KeyValue("Mobile", mobile)
                }
            }
            Spacer(Modifier.height(16.dp))
            OrangeButton("LOGOUT", onClick = onLogout)
        }
    }
}

@Composable
fun MoreScreen(onPnr: () -> Unit, onRunning: () -> Unit, onProfile: () -> Unit, onBookings: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFFF7F0)).padding(16.dp)) {
        Text("More", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
        Spacer(Modifier.height(12.dp))
        listOf(
            "PNR enquiry" to onPnr,
            "Live train status" to onRunning,
            "My bookings" to onBookings,
            "Profile & logout" to onProfile
        ).forEach { (label, action) ->
            RailCard(Modifier.padding(bottom = 8.dp)) {
                androidx.compose.material3.TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) {
                    Text(label, color = Navy, fontWeight = FontWeight.Medium)
                }
            }
        }
        Text(
            "Inspired by IRCTC Rail Connect. Academic / demo project only — not an official IRCTC app.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
