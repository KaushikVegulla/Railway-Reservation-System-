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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.data.MockData
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.components.TricolorStrip
import com.kaushik.railway.ui.theme.LiquidGlassBackdrop
import com.kaushik.railway.ui.theme.Mute
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.NavyDark
import com.kaushik.railway.ui.theme.NavyMid
import com.kaushik.railway.ui.theme.Orange

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onSearch: () -> Unit,
    onPnr: () -> Unit,
    onRunning: () -> Unit,
    onBookings: () -> Unit,
    onNeedProfile: () -> Unit,
    onNeedAadhaar: () -> Unit
) {
    LiquidGlassBackdrop {
        Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Navy.copy(alpha = 0.82f))
                .statusBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Orange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Train, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("RailOne", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Indian Railways  |  CRIS", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
                Icon(Icons.Default.Notifications, null, tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Default.Person, null, tint = Color.White)
            }
            Text(
                "Namaste, ${vm.userName.split(" ").first()}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, bottom = 28.dp)
            )
            TricolorStrip()
        }
        Column(
            Modifier
                .weight(1f)
                .offset(y = (-18).dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            // One-tap rebook last journey
            vm.lastJourney?.let { j ->
                if (j.fromCode.isNotBlank() && j.toCode.isNotBlank()) {
                    RailCard(Modifier.padding(bottom = 10.dp)) {
                        Column {
                            Text("REBOOK LAST JOURNEY", fontWeight = FontWeight.Bold, color = Orange, fontSize = 12.sp, letterSpacing = 0.6.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${j.fromCode} → ${j.toCode}" + (if (j.trainName.isNotBlank()) "  ·  ${j.trainName}" else ""),
                                color = Navy,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            if (j.classCode.isNotBlank()) {
                                Text("${j.classCode}  ·  ${j.quota}", color = Mute, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            OrangeButton("USE THIS ROUTE") {
                                vm.applyLastJourney()
                            }
                        }
                    }
                }
            }
            RailCard {
                Column {
                    Text("BOOK TICKET", fontWeight = FontWeight.Bold, color = Navy, fontSize = 13.sp, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(8.dp))
                    StationSearch("From / प्रस्थान", vm.fromCode, vm) { vm.fromCode = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        IconButton(
                            onClick = { vm.swapStations() },
                            modifier = Modifier.clip(CircleShape).background(Navy.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.SwapVert, "Swap", tint = Navy)
                        }
                    }
                    StationSearch("To / गंतव्य", vm.toCode, vm) { vm.toCode = it }
                    Spacer(Modifier.height(4.dp))
                    SimpleSelect("Journey date / यात्रा तिथि", vm.journeyDate, AppViewModel.upcomingDates()) {
                        vm.journeyDate = it
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            vm.returnDate = if (vm.returnDate == null) AppViewModel.defaultDate() else null
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (vm.returnDate != null) "☑ Return journey" else "☐ Return journey (optional)",
                            color = Navy,
                            fontSize = 14.sp
                        )
                    }
                    if (vm.returnDate != null) {
                        Spacer(Modifier.height(8.dp))
                        SimpleSelect("Return date", vm.returnDate ?: AppViewModel.defaultDate(), AppViewModel.upcomingDates()) {
                            vm.returnDate = it
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            SimpleSelect("Class", vm.selectedClass, listOf("All Classes") + MockData.classes) {
                                vm.selectedClass = it
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            SimpleSelect("Quota", vm.selectedQuota, MockData.quotas) { vm.selectedQuota = it }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (!vm.profileComplete) {
                        Text("Complete your profile before searching trains.", color = Orange, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                    } else if (vm.needsTatkalAadhaar()) {
                        Text("Tatkal needs a local Aadhaar or VID link before search.", color = Orange, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                    } else if (vm.aadhaarLinked && com.kaushik.railway.data.IrctcRules.isTatkalQuota(vm.selectedQuota)) {
                        Text("Tatkal link on this device ••••${vm.aadhaarLast4}", color = Navy, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                    OrangeButton("SEARCH TRAINS") {
                        when {
                            !vm.profileComplete -> onNeedProfile()
                            vm.needsTatkalAadhaar() -> onNeedAadhaar()
                            else -> {
                                vm.searchTrainsLive()
                                onSearch()
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Services", fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp)
            Text("Enquiry & other CRIS services", color = Mute, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceTile("PNR", "Status", Icons.Default.ConfirmationNumber, Modifier.weight(1f), onPnr)
                ServiceTile("Live", "Train", Icons.Default.Timeline, Modifier.weight(1f), onRunning)
                ServiceTile("My", "Bookings", Icons.Default.Train, Modifier.weight(1f), onBookings)
                ServiceTile("Retiring", "Room", Icons.Default.Hotel, Modifier.weight(1f), onBookings)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceTile("e-Catering", "Meals", Icons.Default.Fastfood, Modifier.weight(1f), onBookings)
                ServiceTile("Alerts", "Notices", Icons.Default.Notifications, Modifier.weight(1f), onPnr)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
            Text(
                "Inspired by RailOne (CRIS). Not an official Indian Railways app.",
                color = Mute,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        }
    }
}

@Composable
private fun StationSearch(label: String, code: String, vm: AppViewModel, onSelect: (String) -> Unit) {
    var query by remember(code) { mutableStateOf(vm.stationName(code)) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.length >= 2) vm.searchStations(it)
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        val suggestions = vm.stationSuggestions
        if (suggestions.isNotEmpty() && query.length >= 2) {
            suggestions.take(8).forEach { st ->
                Text(
                    "${st.name} (${st.code})",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(st.code)
                            query = "${st.name} (${st.code})"
                            vm.stationSuggestions.clear()
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    color = Navy,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SimpleSelect(label: String, value: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 6.dp)) {
        Text(label.uppercase(), color = Mute, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = NavyDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onPick(it); open = false })
            }
        }
    }
}

@Composable
private fun ServiceTile(line1: String, line2: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.42f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(NavyMid.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Navy, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(line1, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyDark, textAlign = TextAlign.Center)
        Text(line2, fontSize = 10.sp, color = Mute, textAlign = TextAlign.Center)
    }
}
