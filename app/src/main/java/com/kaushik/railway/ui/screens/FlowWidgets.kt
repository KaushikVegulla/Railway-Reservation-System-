package com.kaushik.railway.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.ui.theme.LiquidGlassBackdrop
import com.kaushik.railway.ui.theme.Mute
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.NavyDark
import com.kaushik.railway.ui.theme.Orange

@Composable
internal fun FlowPage(content: @Composable ColumnScope.() -> Unit) {
    LiquidGlassBackdrop {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
internal fun StepHeading(step: Int, total: Int, title: String, subtitle: String) {
    Text(
        "STEP $step OF $total",
        color = Orange,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
    )
    Text(title, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = Mute, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
}

@Composable
internal fun ChoiceField(label: String, value: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Mute, fontSize = 12.sp)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.55f))
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(value.ifBlank { "Select" }, color = NavyDark, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onPick(option); open = false })
                }
            }
        }
    }
}

@Composable
internal fun CheckRow(checked: Boolean, text: String, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(text, color = NavyDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun DemoNote(text: String) {
    Text(text, color = Mute, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
}
