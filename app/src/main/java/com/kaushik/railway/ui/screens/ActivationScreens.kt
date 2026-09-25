package com.kaushik.railway.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.kaushik.railway.AppViewModel
import com.kaushik.railway.ui.BiometricHelper
import com.kaushik.railway.data.IrctcRules
import com.kaushik.railway.data.ProfileDraft
import com.kaushik.railway.ui.components.LabeledField
import com.kaushik.railway.ui.components.OrangeButton
import com.kaushik.railway.ui.components.RailCard
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Success

@Composable
fun ProfileActivationScreen(vm: AppViewModel, onDone: () -> Unit, onLater: () -> Unit) {
    val existing = vm.currentProfile
    var step by remember { mutableIntStateOf(0) }
    var gender by remember(existing?.userId) { mutableStateOf(existing?.gender.orEmpty()) }
    var dob by remember(existing?.userId) { mutableStateOf(existing?.dob.orEmpty()) }
    var occupation by remember(existing?.userId) { mutableStateOf(existing?.occupation.orEmpty()) }
    var marital by remember(existing?.userId) { mutableStateOf(existing?.maritalStatus.orEmpty()) }
    var nationality by remember(existing?.userId) { mutableStateOf(existing?.nationality?.ifBlank { "India" } ?: "India") }
    var address by remember(existing?.userId) { mutableStateOf(existing?.addressLine.orEmpty()) }
    var city by remember(existing?.userId) { mutableStateOf(existing?.city.orEmpty()) }
    var state by remember(existing?.userId) { mutableStateOf(existing?.state.orEmpty()) }
    var country by remember(existing?.userId) { mutableStateOf(existing?.country?.ifBlank { "India" } ?: "India") }
    var pin by remember(existing?.userId) { mutableStateOf(existing?.pinCode.orEmpty()) }
    var terms by remember(existing?.userId) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    FlowPage {
        when (step) {
            0 -> {
                StepHeading(1, 3, "Complete your profile", "Gender, date of birth, and occupation")
                RailCard {
                    ColumnBlock {
                        ChoiceField("Gender", gender, IrctcRules.genders) { gender = it }
                        Spacer(Modifier.height(8.dp))
                        LabeledField("Date of birth (DD-MM-YYYY)", dob) { dob = it.take(10) }
                        Spacer(Modifier.height(8.dp))
                        ChoiceField("Occupation", occupation, IrctcRules.occupations) { occupation = it }
                        Spacer(Modifier.height(8.dp))
                        ChoiceField("Marital status", marital, IrctcRules.maritalStatuses) { marital = it }
                        Spacer(Modifier.height(8.dp))
                        LabeledField("Nationality", nationality) { nationality = it }
                        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                        Spacer(Modifier.height(16.dp))
                        OrangeButton("CONTINUE") {
                            error = when {
                                gender.isBlank() -> "Select gender"
                                IrctcRules.validateDob(dob) != null -> IrctcRules.validateDob(dob)
                                occupation.isBlank() -> "Select occupation"
                                marital.isBlank() -> "Select marital status"
                                nationality.isBlank() -> "Enter nationality"
                                else -> null
                            }
                            if (error == null) step = 1
                        }
                        TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                            Text("I'll do this later", color = Navy)
                        }
                    }
                }
            }
            1 -> {
                StepHeading(2, 3, "Address and terms", "City, state, and PIN code")
                RailCard {
                    ColumnBlock {
                        LabeledField("Address", address) { address = it }
                        Spacer(Modifier.height(8.dp))
                        LabeledField("City", city) { city = it }
                        Spacer(Modifier.height(8.dp))
                        ChoiceField("State", state, IrctcRules.indianStates) { state = it }
                        Spacer(Modifier.height(8.dp))
                        LabeledField("Country", country) { country = it }
                        Spacer(Modifier.height(8.dp))
                        LabeledField("PIN code", pin, keyboardType = KeyboardType.Number) {
                            pin = it.filter { ch -> ch.isDigit() }.take(6)
                        }
                        CheckRow(
                            checked = terms,
                            text = "I accept the RailX demo terms and confirm these details match my Govt. ID.",
                            onChange = { terms = it }
                        )
                        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                        Spacer(Modifier.height(16.dp))
                        OrangeButton(if (busy) "PLEASE WAIT" else "SUBMIT", enabled = !busy) {
                            val draft = ProfileDraft(
                                gender = gender,
                                dob = dob,
                                occupation = occupation,
                                maritalStatus = marital,
                                nationality = nationality,
                                addressLine = address,
                                city = city,
                                state = state,
                                country = country,
                                pinCode = pin,
                                acceptedTerms = terms
                            )
                            val problem = IrctcRules.validateProfile(draft)
                            if (problem != null) {
                                error = problem
                                return@OrangeButton
                            }
                            busy = true
                            error = null
                            vm.completeProfile(draft) { message ->
                                busy = false
                                if (message == null) step = 2 else error = message
                            }
                        }
                        TextButton(onClick = { step = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Navy) }
                    }
                }
            }
            else -> {
                StepHeading(3, 3, "Profile complete", "You can use ticketing services in this demo")
                RailCard {
                    ColumnBlock {
                        Text(
                            "Your profile is saved on this device. Tatkal search still asks for a local Aadhaar or VID check.",
                            color = Navy,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        OrangeButton("OK") { onDone() }
                    }
                }
            }
        }
        DemoNote("Academic demo. This is not an IRCTC profile and it is not sent to Indian Railways.")
    }
}

@Composable
fun MpinScreen(vm: AppViewModel, onDone: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(vm.biometricOn) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? FragmentActivity
    val helper = remember(activity) { activity?.let { BiometricHelper(it) } }
    val canBio = helper?.canAuthenticate() == true
    FlowPage {
        StepHeading(1, 1, "Generate PIN", "4-digit MPIN. Biometric unlock is optional.")
        RailCard {
            ColumnBlock {
                LabeledField("Enter PIN", pin, isPassword = true, keyboardType = KeyboardType.NumberPassword) {
                    pin = it.filter { ch -> ch.isDigit() }.take(4)
                }
                Spacer(Modifier.height(8.dp))
                LabeledField("Confirm PIN", confirm, isPassword = true, keyboardType = KeyboardType.NumberPassword) {
                    confirm = it.filter { ch -> ch.isDigit() }.take(4)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Unlock with biometric", color = Navy, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (canBio) "Fingerprint or face. MPIN stays the fallback."
                            else "This device has no fingerprint or face unlock.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = biometric && canBio,
                        onCheckedChange = { biometric = it && canBio },
                        enabled = canBio
                    )
                }
                error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(16.dp))
                OrangeButton(if (busy) "PLEASE WAIT" else "SUBMIT", enabled = !busy) {
                    busy = true
                    error = null
                    val save: (Boolean) -> Unit = { enabled ->
                        vm.saveMpin(pin, confirm, enabled) { message ->
                            busy = false
                            if (message == null) {
                                vm.markUnlocked()
                                onDone()
                            } else {
                                error = message
                            }
                        }
                    }
                    if (biometric && helper != null) {
                        helper.authenticate(
                            title = "Enable biometric unlock",
                            subtitle = "Confirm fingerprint or face for RailX",
                            onSuccess = { save(true) },
                            onError = {
                                busy = false
                                error = it.ifBlank { "Biometric was cancelled. Submit again or leave it off." }
                            }
                        )
                    } else {
                        save(false)
                    }
                }
                TextButton(onClick = { vm.deferMpin(onDone) }, modifier = Modifier.fillMaxWidth()) {
                    Text("I'll do this later", color = Navy)
                }
            }
        }
        DemoNote("The PIN is stored as a hash on this device. It is not sent to Cognito.")
    }
}

@Composable
fun UnlockScreen(vm: AppViewModel, onUnlocked: () -> Unit) {
    val activity = LocalContext.current as? FragmentActivity
    var showMpin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var prompted by remember { mutableStateOf(false) }
    LaunchedEffect(activity, prompted) {
        if (prompted) return@LaunchedEffect
        val helper = activity?.let { BiometricHelper(it) }
        if (helper != null && helper.canAuthenticate()) {
            prompted = true
            helper.authenticate(
                title = "Unlock RailX",
                subtitle = "Use fingerprint or face. MPIN is the fallback.",
                onSuccess = {
                    vm.markUnlocked()
                    onUnlocked()
                },
                onError = {
                    showMpin = true
                    if (it.isNotBlank()) error = it
                }
            )
        } else {
            prompted = true
            showMpin = true
        }
    }
    if (showMpin) {
        FlowPage {
            StepHeading(1, 1, "Enter MPIN", "Biometric unlock was skipped")
            RailCard {
                ColumnBlock {
                    LabeledField("MPIN", pin, isPassword = true, keyboardType = KeyboardType.NumberPassword) {
                        pin = it.filter { ch -> ch.isDigit() }.take(4)
                    }
                    error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                    Spacer(Modifier.height(16.dp))
                    OrangeButton("UNLOCK") {
                        if (vm.verifyMpin(pin)) {
                            vm.markUnlocked()
                            onUnlocked()
                        } else {
                            error = "Incorrect MPIN"
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AadhaarEnableScreen(vm: AppViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var kind by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var savedLast4 by remember { mutableStateOf("") }

    FlowPage {
        when (step) {
            0 -> {
                StepHeading(1, 3, "Aadhaar authentication", "Required in this demo before Tatkal search")
                RailCard {
                    ColumnBlock {
                        Text(
                            "Aadhaar or VID is mandatory here before a Tatkal or Premium Tatkal search.",
                            color = Navy,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The full number stays in this form until you submit. Only the last 4 digits are saved, and UIDAI is not contacted.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        OrangeButton("CONTINUE") { step = 1 }
                        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Not now", color = Navy) }
                    }
                }
            }
            1 -> {
                StepHeading(2, 3, "Authenticate user", "Choose Aadhaar Card / VID")
                RailCard {
                    ColumnBlock {
                        ChoiceField("Authentication type", kind, IrctcRules.authTypes) { kind = it }
                        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                        Spacer(Modifier.height(16.dp))
                        OrangeButton("CONTINUE") {
                            error = when (kind) {
                                "" -> "Select an authentication type"
                                "PAN CARD" -> "PAN does not enable Tatkal. Choose Aadhaar Card / VID."
                                else -> null
                            }
                            if (error == null) step = 2
                        }
                        TextButton(onClick = { step = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Back", color = Navy) }
                    }
                }
            }
            2 -> {
                StepHeading(3, 3, "Aadhaar Card / VID", "Enter the number and confirm consent")
                RailCard {
                    ColumnBlock {
                        LabeledField("Aadhaar (12) or VID (16)", number, keyboardType = KeyboardType.Number) {
                            number = it.filter { ch -> ch.isDigit() }.take(16)
                        }
                        CheckRow(
                            checked = consent,
                            text = "I confirm this number is mine and I agree to link it in this academic demo so Tatkal search can continue.",
                            onChange = { consent = it }
                        )
                        error?.let { Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                        Spacer(Modifier.height(16.dp))
                        OrangeButton(if (busy) "PLEASE WAIT" else "SUBMIT", enabled = !busy) {
                            val check = IrctcRules.checkIdentity(kind, number, consent)
                            if (!check.ok) {
                                error = check.error
                                return@OrangeButton
                            }
                            busy = true
                            error = null
                            val last4 = check.last4
                            vm.saveAadhaarLink(check.kind, last4) { message ->
                                busy = false
                                if (message == null) {
                                    number = ""
                                    savedLast4 = last4
                                    step = 3
                                } else {
                                    error = message
                                }
                            }
                        }
                        TextButton(onClick = { number = ""; step = 1 }, modifier = Modifier.fillMaxWidth()) {
                            Text("Back", color = Navy)
                        }
                    }
                }
            }
            else -> {
                StepHeading(3, 3, "Tatkal link saved", "Last 4 digits only")
                RailCard {
                    ColumnBlock {
                        Text("Linked on this device", color = Success, fontWeight = FontWeight.Bold)
                        Text(
                            "${vm.aadhaarKind.ifBlank { "ID" }} ••••${savedLast4.ifBlank { vm.aadhaarLast4 }}",
                            color = Navy,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "UIDAI was not contacted. Train search in this app stays a local demo.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        OrangeButton("OK") { onDone() }
                    }
                }
            }
        }
        DemoNote("Do not enter a real Aadhaar number. Any well-formed demo number is accepted locally.")
    }
}

@Composable
private fun ColumnBlock(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth(), content = { content() })
}
