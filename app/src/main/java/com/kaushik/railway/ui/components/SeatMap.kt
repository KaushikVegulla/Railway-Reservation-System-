package com.kaushik.railway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange

enum class SeatStatus { Available, Booked, Selected, Ladies }

data class SeatCell(
    val id: String,
    val berth: String, // LB, MB, UB, SL, SU
    val status: SeatStatus
)

/**
 * Simplified coach seat map (demo).
 * 3-tier layout: Lower / Middle / Upper + side berths.
 */
@Composable
fun SeatMapGrid(
    seats: List<SeatCell>,
    selectedIds: Set<String>,
    onToggle: (SeatCell) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text("Select berth (demo map)", fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            LegendDot(Color(0xFF4CAF50), "Free")
            LegendDot(Orange, "Selected")
            LegendDot(Color(0xFFBDBDBD), "Booked")
            LegendDot(Color(0xFFE91E63), "Ladies")
        }
        Spacer(Modifier.height(12.dp))
        // Group into bays of 6 (LB,MB,UB x2 side)
        seats.chunked(8).forEachIndexed { bay, baySeats ->
            Text("Bay ${bay + 1}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                baySeats.forEach { seat ->
                    val isSelected = seat.id in selectedIds
                    val bg = when {
                        isSelected -> Orange
                        seat.status == SeatStatus.Booked -> Color(0xFFBDBDBD)
                        seat.status == SeatStatus.Ladies -> Color(0xFFF8BBD0)
                        else -> Color(0xFFE8F5E9)
                    }
                    val enabled = seat.status != SeatStatus.Booked
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .border(1.dp, if (isSelected) Navy else Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                            .then(if (enabled) Modifier.clickable { onToggle(seat) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(seat.id, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy)
                            Text(seat.berth, fontSize = 9.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

/** Generate a demo seat map for a coach class */
fun demoSeatMap(coachCode: String, seed: Int = 42): List<SeatCell> {
    val berths = when (coachCode) {
        "SL", "3A", "3E" -> listOf("LB", "MB", "UB", "LB", "MB", "UB", "SL", "SU")
        "2A" -> listOf("LB", "UB", "LB", "UB", "SL", "SU", "LB", "UB")
        "1A" -> listOf("LB", "UB", "LB", "UB", "LB", "UB", "LB", "UB")
        else -> listOf("WS", "MS", "AS", "WS", "MS", "AS", "WS", "MS")
    }
    val rnd = java.util.Random(seed.toLong())
    return (1..24).map { i ->
        val bay = (i - 1) / 8 + 1
        val pos = (i - 1) % 8
        val id = "${coachCode.take(2)}${bay}${('A' + pos)}"
        val status = when {
            rnd.nextFloat() < 0.25f -> SeatStatus.Booked
            rnd.nextFloat() < 0.08f -> SeatStatus.Ladies
            else -> SeatStatus.Available
        }
        SeatCell(id = id, berth = berths[pos % berths.size], status = status)
    }
}
