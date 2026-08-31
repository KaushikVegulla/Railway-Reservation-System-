package com.kaushik.railway.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.data.Train
import com.kaushik.railway.data.TrainClassAvail
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.components.RailTopBar
import com.kaushik.railway.ui.components.StatusChip
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange

@Composable
fun TrainListScreen(vm: AppViewModel, onBack: () -> Unit, onSelect: (Train) -> Unit) {
    val trains = vm.trains
    Scaffold(topBar = { RailTopBar("${vm.fromCode} → ${vm.toCode}", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color(0xFFFFF7F0))) {
            Text(
                "${if (vm.searchLoading) "Searching…" else "${trains.size} trains"}  •  ${vm.journeyDate}  •  ${vm.quotaCode()}",
                modifier = Modifier.padding(16.dp),
                color = Color.Gray,
                fontSize = 13.sp
            )
            if (vm.searchLoading) {
                CircularProgressIndicator(Modifier.padding(24.dp), color = Orange)
            }
            vm.searchError?.let { Text(it, color = Color.Red, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp) }
            LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(trains, key = { it.number + it.depart }) { train ->
                    RailCard(Modifier.clickable { onSelect(train) }) {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${train.number}  ${train.name}", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                                Text(train.days, color = Color.Gray, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(train.depart, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
                                    Text(train.from, color = Color.Gray, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(train.duration, color = Orange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("────────", color = Color.LightGray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(train.arrive, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
                                    Text(train.to, color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                train.classes.forEach { cls ->
                                    Column(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFF1E6))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(cls.code, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy)
                                        Text("₹${cls.fare}", fontSize = 11.sp, color = Orange)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun AvailabilityScreen(vm: AppViewModel, onBack: () -> Unit, onVacancy: (TrainClassAvail) -> Unit) {
    val train = vm.selectedTrain ?: return
    LaunchedEffect(train.number) { vm.loadAvailability(train) }
    Scaffold(topBar = { RailTopBar(train.name, onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color(0xFFFFF7F0)).padding(16.dp)) {
            Text("${train.number}  •  ${train.fromCode} → ${train.toCode}  •  ${vm.journeyDate}", color = Color.Gray, fontSize = 13.sp)
            Text("Live seat availability (RailKit)", color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            if (vm.availLoading) CircularProgressIndicator(color = Orange)
            vm.availError?.let { Text(it, color = Color.Red, fontSize = 13.sp) }
            vm.classRows.forEach { cls ->
                RailCard(Modifier.padding(bottom = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${cls.code}  ${cls.name}", fontWeight = FontWeight.Bold, color = Navy)
                            Text("₹${cls.fare}  per adult", color = Orange, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            StatusChip(cls.status)
                        }
                        Spacer(Modifier.width(8.dp))
                        OrangeButton("CHART", modifier = Modifier.width(110.dp)) { onVacancy(cls) }
                    }
                }
            }
        }
    }
}

@Composable
fun VacancyChartScreen(vm: AppViewModel, onBack: () -> Unit, onBook: () -> Unit) {
    val train = vm.selectedTrain ?: return
    val cls = vm.selectedTravelClass ?: return
    LaunchedEffect(cls.code) { vm.loadVacancyChart(cls) }
    Scaffold(topBar = { RailTopBar("Vacancy chart • ${cls.code}", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color(0xFFFFF7F0)).padding(16.dp)) {
            Text("${train.number} ${train.name}", fontWeight = FontWeight.Bold, color = Navy)
            Text("Quota ${vm.quotaCode()}  •  fare ₹${vm.vacancy?.fare ?: cls.fare}", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            val days = vm.vacancy?.days.orEmpty()
            if (days.isEmpty()) {
                CircularProgressIndicator(color = Orange)
            } else {
                days.forEach { day ->
                    RailCard(Modifier.padding(bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(day.date, fontWeight = FontWeight.Bold, color = Navy)
                                Text(day.prediction, fontSize = 12.sp, color = Color.Gray)
                            }
                            StatusChip(day.text.ifBlank { day.status })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OrangeButton("CONTINUE TO BOOK") { onBook() }
            Text(
                "RailKit does not issue IRCTC tickets. Booking here saves a local e-ticket using live fare/availability.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
