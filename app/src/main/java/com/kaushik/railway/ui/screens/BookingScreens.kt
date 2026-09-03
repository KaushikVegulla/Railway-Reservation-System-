package com.kaushik.railway.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.data.PaymentApi
import com.kaushik.railway.data.PaymentBridge
import com.razorpay.Checkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
    val total = vm.payableAmount()
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf("all") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { RailTopBar("Payment", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(Color(0xFFEEF2F7)).padding(16.dp)) {
            Text("Amount payable", color = Color.Gray)
            Text("₹$total", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Navy)
            Spacer(Modifier.height(16.dp))
            Text("Pay using Razorpay (test)", fontWeight = FontWeight.SemiBold, color = Navy)
            Spacer(Modifier.height(8.dp))
            listOf(
                "all" to "All methods (UPI + Cards + Netbanking)",
                "upi" to "UPI (GPay / PhonePe / Paytm)",
                "card" to "Credit / Debit card",
                "netbanking" to "Net banking"
            ).forEach { (id, label) ->
                val selected = method == id
                RailCard(
                    Modifier
                        .padding(bottom = 8.dp)
                        .then(
                            if (selected) Modifier.border(2.dp, Orange, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { method = id }
                ) {
                    Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = Navy)
                }
            }
            error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp)) }
            if (busy) Text("Contacting payment server…", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            OrangeButton(if (busy) "PLEASE WAIT" else "PAY ₹$total", enabled = !busy) {
                busy = true
                error = null
                PaymentBridge.onSuccess = { payId, orderId, sig ->
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { PaymentApi.verifyPayment(payId, orderId, sig) }
                            vm.confirmBooking(payId, orderId)
                            busy = false
                            onSuccess()
                        } catch (e: Exception) {
                            busy = false
                            error = e.message ?: "Verify failed"
                        }
                    }
                }
                PaymentBridge.onError = { msg ->
                    busy = false
                    error = msg
                }
                scope.launch {
                    try {
                        val order = withContext(Dispatchers.IO) {
                            PaymentApi.createOrder(total, "PNR-${vm.fromCode}-${vm.toCode}")
                        }
                        val options = JSONObject().apply {
                            put("key", order.keyId)
                            put("amount", order.amountPaise)
                            put("currency", order.currency)
                            put("name", "RailOne")
                            put("description", "Train ticket")
                            put("order_id", order.orderId)
                            put("theme", JSONObject().put("color", "#0A3D91"))
                            put("prefill", JSONObject().put("email", vm.email).put("contact", vm.mobile))
                            put(
                                "method",
                                JSONObject().apply {
                                    put("upi", method == "all" || method == "upi")
                                    put("card", method == "all" || method == "card")
                                    put("netbanking", method == "all" || method == "netbanking")
                                    put("wallet", method == "all")
                                }
                            )
                        }
                        Checkout().open(activity, options)
                    } catch (e: Exception) {
                        busy = false
                        error = e.message ?: "Could not create order. Is backend running on :8088?"
                    }
                }
            }
            Text(
                "Razorpay test mode. Card 4111 1111 1111 1111, any future expiry/CVV. Works on any internet connection.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
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
                        if (b.paymentId.isNotBlank()) KeyValue("Payment", b.paymentId)
                        if (b.orderId.isNotBlank()) KeyValue("Order", b.orderId)
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
