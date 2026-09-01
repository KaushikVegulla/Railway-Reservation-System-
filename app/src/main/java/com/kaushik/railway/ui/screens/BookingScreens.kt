package com.kaushik.railway.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.data.MockData
import com.kaushik.railway.data.Passenger
import com.kaushik.railway.ui.components.KeyValue
import com.kaushik.railway.ui.components.LabeledField
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.components.RailTopBar
import com.kaushik.railway.ui.components.SectionTitle
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange

@Composable
fun PassengerScreen(vm: AppViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    Scaffold(topBar = { RailTopBar("Passenger details", onBack) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(Color(0xFFEEF2F7)).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            vm.passengers.forEachIndexed { i, p ->
                RailCard(Modifier.padding(bottom = 12.dp)) {
                    Column {
                        Text("Passenger ${i + 1}", fontWeight = FontWeight.Bold, color = Navy)
                        Spacer(Modifier.height(8.dp))
                        LabeledField("Name (as per ID)", p.name, onValue = { value -> vm.passengers[i] = p.copy(name = value.uppercase()) })
                        Spacer(Modifier.height(8.dp))
                        LabeledField("Age", p.age, onValue = { value ->
                            vm.passengers[i] = p.copy(age = value.filter { ch -> ch.isDigit() }.take(3))
                        })
                        Spacer(Modifier.height(8.dp))
                        LabeledField("Gender (Male/Female/Other)", p.gender, onValue = { value -> vm.passengers[i] = p.copy(gender = value) })
                        Spacer(Modifier.height(8.dp))
                        LabeledField("Berth preference", p.berth, onValue = { value -> vm.passengers[i] = p.copy(berth = value) })
                        Text("Options: ${MockData.berths.joinToString()}", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (vm.passengers.size < 6) {
                OutlinedButton(onClick = { vm.passengers.add(Passenger()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add passenger")
                }
            }
            Spacer(Modifier.height(12.dp))
            LabeledField("Mobile", vm.mobile, onValue = { vm.mobile = it })
            Spacer(Modifier.height(8.dp))
            LabeledField("Email", vm.email, onValue = { vm.email = it })
            Spacer(Modifier.height(16.dp))
            OrangeButton("CONTINUE") { onContinue() }
        }
    }
}

@Composable
fun ReviewScreen(vm: AppViewModel, onBack: () -> Unit, onPay: () -> Unit) {
    val train = vm.selectedTrain ?: return
    val cls = vm.selectedTravelClass ?: return
    val ins = if (vm.insurance) vm.passengers.size * 15 else 0
    val total = cls.fare * vm.passengers.size + ins
    Scaffold(topBar = { RailTopBar("Review booking", onBack) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(Color(0xFFEEF2F7)).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            RailCard {
                Column {
                    SectionTitle("${train.number} ${train.name}")
                    KeyValue("From", vm.stationName(vm.fromCode))
                    KeyValue("To", vm.stationName(vm.toCode))
                    KeyValue("Date", vm.journeyDate)
                    KeyValue("Class / Quota", "${cls.code} / ${vm.selectedQuota.take(2)}")
                    KeyValue("Dep / Arr", "${train.depart} / ${train.arrive}")
                }
            }
            Spacer(Modifier.height(12.dp))
            RailCard {
                Column {
                    SectionTitle("Passengers")
                    vm.passengers.forEachIndexed { i, p ->
                        Text("${i + 1}. ${p.name.ifBlank { "—" }}  •  ${p.age}  •  ${p.gender}  •  ${p.berth}", fontSize = 13.sp, color = Navy)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            RailCard {
                Column {
                    SectionTitle("Fare")
                    KeyValue("Ticket", "₹${cls.fare} × ${vm.passengers.size}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(vm.insurance, { vm.insurance = it })
                        Text("Travel insurance ₹15 / passenger")
                    }
                    KeyValue("Total payable", "₹$total")
                }
            }
            Spacer(Modifier.height(16.dp))
            OrangeButton("PROCEED TO PAY ₹$total") { onPay() }
        }
    }
}

@Composable
fun PaymentScreen(vm: AppViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    val cls = vm.selectedTravelClass ?: return
    val ins = if (vm.insurance) vm.passengers.size * 15 else 0
    val total = cls.fare * vm.passengers.size + ins
    Scaffold(topBar = { RailTopBar("Payment", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color(0xFFEEF2F7)).padding(16.dp)) {
            Text("Amount payable", color = Color.Gray)
            Text("₹$total", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Navy)
            Spacer(Modifier.height(16.dp))
            listOf("UPI (GPay / PhonePe / Paytm)", "IRCTC eWallet", "Debit / Credit card", "Net banking", "Wallets").forEach { method ->
                RailCard(Modifier.padding(bottom = 8.dp)) {
                    Text(method, fontWeight = FontWeight.Medium, color = Navy)
                }
            }
            Spacer(Modifier.height(12.dp))
            OrangeButton("PAY SECURELY (DEMO)") {
                vm.confirmBooking()
                onSuccess()
            }
            Text("Live fare from RailRadar. Payment is local only — RailRadar cannot book IRCTC tickets.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun TicketScreen(vm: AppViewModel, onHome: () -> Unit) {
    val b = vm.lastBooking
    Scaffold(topBar = { RailTopBar("e-Ticket") }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(Color(0xFFEEF2F7)).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (b == null) {
                Text("No ticket")
            } else {
                RailCard {
                    Column {
                        Text("PNR  ${b.pnr}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Orange)
                        Text(b.status, fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        KeyValue("Train", "${b.train.number} ${b.train.name}")
                        KeyValue("From", b.fromName)
                        KeyValue("To", b.toName)
                        KeyValue("Date", b.date)
                        KeyValue("Class", b.travelClass.code)
                        KeyValue("Quota", b.quota)
                        KeyValue("Amount", "₹${b.amount}")
                        Spacer(Modifier.height(8.dp))
                        b.passengers.forEachIndexed { i, p ->
                            Text("${i + 1}. ${p.name} (${p.age}, ${p.gender})", fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OrangeButton("BACK TO HOME", onClick = onHome)
            }
        }
    }
}

@Composable
fun BookingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    Scaffold(topBar = { RailTopBar("My bookings", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color(0xFFEEF2F7)).padding(16.dp)) {
            if (vm.bookings.isEmpty()) {
                Text("No bookings yet. Search trains to book a ticket.", color = Color.Gray)
            } else {
                vm.bookings.forEach { b ->
                    RailCard(Modifier.padding(bottom = 10.dp)) {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("PNR ${b.pnr}", fontWeight = FontWeight.Bold, color = Orange)
                                Text(b.status, fontWeight = FontWeight.Bold, color = Navy)
                            }
                            Text("${b.train.number} ${b.train.name}", color = Navy)
                            Text("${b.fromName} → ${b.toName}", fontSize = 13.sp, color = Color.Gray)
                            Text("${b.date}  •  ${b.travelClass.code}  •  ₹${b.amount}", fontSize = 13.sp)
                            if (b.status != "CANCELLED") {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { vm.cancel(b.pnr) }) { Text("Cancel ticket") }
                            }
                        }
                    }
                }
            }
        }
    }
}
