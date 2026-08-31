package com.kaushik.railway.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.data.MockData
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onSearch: () -> Unit,
    onPnr: () -> Unit,
    onRunning: () -> Unit,
    onBookings: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFFF7F0))) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Navy)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Text("Namaste, ${vm.userName.split(" ").first()}", color = Color(0xFFFFCC80), fontSize = 13.sp)
                Text("Book your train", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            RailCard {
                Column {
                    StationRow("From", vm.fromCode) { vm.fromCode = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        IconButton(
                            onClick = { vm.swapStations() },
                            modifier = Modifier.clip(CircleShape).background(Orange.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.SwapVert, "Swap", tint = Orange)
                        }
                    }
                    StationRow("To", vm.toCode) { vm.toCode = it }
                    Spacer(Modifier.height(8.dp))
                    SimpleSelect("Journey date", vm.journeyDate, AppViewModel.upcomingDates()) {
                        vm.journeyDate = it
                    }
                    Spacer(Modifier.height(8.dp))
                    SimpleSelect("Class", vm.selectedClass, listOf("All Classes") + MockData.classes) {
                        vm.selectedClass = it
                    }
                    Spacer(Modifier.height(8.dp))
                    SimpleSelect("Quota", vm.selectedQuota, MockData.quotas) { vm.selectedQuota = it }
                    Spacer(Modifier.height(16.dp))
                    OrangeButton("SEARCH TRAINS") {
                        vm.searchTrainsLive()
                        onSearch()
                    }
                    Text("Live data via RailRadar", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Quick services", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("PNR Status", Icons.Default.ConfirmationNumber, Modifier.weight(1f), onPnr)
                QuickTile("Live Status", Icons.Default.Timeline, Modifier.weight(1f), onRunning)
                QuickTile("My Bookings", Icons.Default.Train, Modifier.weight(1f), onBookings)
            }
        }
    }
}

@Composable
private fun StationRow(label: String, code: String, onSelect: (String) -> Unit) {
    val st = MockData.stations.find { it.code == code }
    SimpleSelect(
        label,
        st?.let { "${it.name} (${it.code})" } ?: code,
        MockData.stations.map { "${it.name} (${it.code})" }
    ) { picked ->
        val c = picked.substringAfterLast("(").substringBefore(")")
        onSelect(c)
    }
}

@Composable
private fun SimpleSelect(label: String, value: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onPick(it); open = false })
            }
        }
    }
}

@Composable
private fun QuickTile(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Orange, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy)
    }
}
