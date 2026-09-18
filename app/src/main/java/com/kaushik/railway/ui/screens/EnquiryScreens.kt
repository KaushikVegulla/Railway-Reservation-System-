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
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.ui.components.KeyValue
import com.kaushik.railway.ui.components.LabeledField
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.components.RailTopBar
import com.kaushik.railway.ui.components.StatusChip
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange

@Composable
fun PnrScreen(vm: AppViewModel, onBack: () -> Unit) {
    var pnr by remember { mutableStateOf("") }
    Scaffold(topBar = { RailTopBar("PNR status", onBack) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(Color.Transparent).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            LabeledField("10-digit PNR", pnr, onValue = { value ->
                pnr = value.filter { ch -> ch.isDigit() }.take(10)
            })
            Spacer(Modifier.height(12.dp))
            OrangeButton("GET LIVE STATUS") { vm.lookupPnr(pnr) }
            if (vm.pnrLoading) Text("Fetching from RailRadar…", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            vm.pnrError?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            vm.pnrResult?.let { b ->
                Spacer(Modifier.height(16.dp))
                RailCard {
                    Column {
                        Text("PNR ${b.pnr}", fontWeight = FontWeight.Black, color = Orange, fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        StatusChip(b.chart.ifBlank { "Live" })
                        Spacer(Modifier.height(8.dp))
                        KeyValue("Train", "${b.trainNo} ${b.trainName}")
                        KeyValue("From", b.fromName)
                        KeyValue("To", b.toName)
                        KeyValue("Date", b.date)
                        KeyValue("Class / Quota", "${b.travelClass} / ${b.quota}")
                        KeyValue("Fare", "₹${b.fare}")
                        Spacer(Modifier.height(8.dp))
                        b.passengers.forEach { Text(it, fontSize = 13.sp, color = Navy) }
                    }
                }
            }
        }
    }
}

@Composable
fun RunningStatusScreen(vm: AppViewModel, onBack: () -> Unit) {
    var trainNo by remember { mutableStateOf("12904") }
    Scaffold(topBar = { RailTopBar("Live running status", onBack) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(Color.Transparent).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            LabeledField("Train number", trainNo, onValue = { trainNo = it.filter { ch -> ch.isDigit() }.take(5) })
            Spacer(Modifier.height(12.dp))
            OrangeButton("GET LIVE STATUS") { vm.loadRunning(trainNo) }
            if (vm.runningLoading) Text("Fetching live timeline…", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            vm.runningError?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            if (vm.runningNote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(vm.runningNote, fontWeight = FontWeight.Bold, color = Navy)
            }
            vm.runningStops.forEach { stop ->
                RailCard(Modifier.padding(bottom = 8.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("${stop.station} (${stop.code})", fontWeight = FontWeight.SemiBold, color = Navy)
                            Text("Arr ${stop.schArr}   Dep ${stop.schDep}", fontSize = 12.sp, color = Color.Gray)
                        }
                        StatusChip(stop.status)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit, userId: String, userName: String, email: String, mobile: String, onLogout: () -> Unit) {
    Scaffold(topBar = { RailTopBar("My profile", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color.Transparent).padding(16.dp)) {
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
    Column(Modifier.fillMaxSize().background(Color.Transparent).padding(16.dp)) {
        Text("More", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text("RailOne  •  CRIS services", color = Color.Gray, fontSize = 13.sp)
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
            "Inspired by RailOne (CRIS). Academic demo only — not an official Indian Railways app.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
