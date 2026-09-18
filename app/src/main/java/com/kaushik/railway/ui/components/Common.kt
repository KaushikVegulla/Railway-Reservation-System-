package com.kaushik.railway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.ui.theme.Mute
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.NavyDark
import com.kaushik.railway.ui.theme.Orange
import com.kaushik.railway.ui.theme.Rac
import com.kaushik.railway.ui.theme.Success
import com.kaushik.railway.ui.theme.TricolorGreen
import com.kaushik.railway.ui.theme.TricolorSaffron
import com.kaushik.railway.ui.theme.Waitlist

@Composable
fun TricolorStrip() {
    Row(Modifier.fillMaxWidth().height(3.dp)) {
        Box(Modifier.weight(1f).height(3.dp).background(TricolorSaffron))
        Box(Modifier.weight(1f).height(3.dp).background(Color.White))
        Box(Modifier.weight(1f).height(3.dp).background(TricolorGreen))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RailTopBar(title: String, onBack: (() -> Unit)? = null) {
    Column {
        TopAppBar(
            title = {
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text("Centre for Railway Information Systems", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Navy,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
        TricolorStrip()
    }
}

@Composable
fun OrangeButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.4.sp)
    }
}

@Composable
fun RailCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when {
        status.contains("AVAILABLE", true) || status.contains("CNF", true) -> Success
        status.contains("RAC", true) -> Rac
        status.contains("CANCEL", true) -> Waitlist
        else -> Waitlist
    }
    Text(
        text = status,
        color = if (color == Rac) NavyDark else Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    onValue: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun KeyValue(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(key, color = Mute, fontSize = 13.sp, modifier = Modifier.width(120.dp))
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = NavyDark)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
}
