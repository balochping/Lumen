package com.lumen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

@Composable
fun LivePingCard(
    modifier: Modifier = Modifier,
    targetHost: String = "8.8.8.8"
) {
    var currentPing by remember { mutableLongStateOf(0L) }
    var pingHistory by remember { mutableStateOf(listOf<Float>()) }

    LaunchedEffect(Unit) {
        while (true) {
            val ping = measureLatency(targetHost)
            if (ping > 0) {
                currentPing = ping
                pingHistory = (pingHistory + ping.toFloat()).takeLast(12)
            }
            delay(2000)
        }
    }

    val avgPing = remember(pingHistory) {
        if (pingHistory.isNotEmpty()) pingHistory.average().toInt() else 0
    }
    val jitter = remember(pingHistory) {
        if (pingHistory.size >= 2) {
            var diffSum = 0f
            for (i in 1 until pingHistory.size) {
                diffSum += abs(pingHistory[i] - pingHistory[i - 1])
            }
            (diffSum / (pingHistory.size - 1)).toInt()
        } else 0
    }

    val (badgeText, badgeBg, badgeTextColor) = when {
        currentPing in 1..75 -> Triple("مناسب برای بازی", Color(0xFF143823), Color(0xFF34D399))
        currentPing in 76..120 -> Triple("متوسط", Color(0xFF382F14), Color(0xFFFBBF24))
        else -> Triple("نامناسب برای بازی", Color(0xFF3A181E), Color(0xFFF87171))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF12141C))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "پینگ لایو",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "پینگ اینترنت شما",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PingWaveChart(
                    history = pingHistory,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(end = 16.dp)
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "ms",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                    )
                    Text(
                        text = if (currentPing > 0) "$currentPing" else "--",
                        color = if (currentPing > 120 || currentPing == 0L) Color(0xFFFF4B55) else Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF1E2230))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "میانگین ${avgPing}ms  •  نوسان ${jitter}ms",
                    color = Color(0xFF8A92A6),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PingWaveChart(history: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas

        val maxPing = (history.maxOrNull() ?: 200f).coerceAtLeast(100f)
        val minPing = (history.minOrNull() ?: 20f).coerceAtMost(50f)
        val range = (maxPing - minPing).coerceAtLeast(1f)

        val stepX = size.width / (history.size - 1)
        val path = Path()
        val fillPath = Path()

        history.forEachIndexed { index, ping ->
            val x = index * stepX
            val normalizedY = 1f - ((ping - minPing) / range)
            val y = (normalizedY * (size.height - 10f)) + 5f

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x40FF4B55), Color.Transparent)
            )
        )

        drawPath(
            path = path,
            color = Color(0xFFFF4B55),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

private suspend fun measureLatency(host: String): Long = withContext(Dispatchers.IO) {
    try {
        val start = System.currentTimeMillis()
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, 53), 1500)
        }
        System.currentTimeMillis() - start
    } catch (_: Exception) {
        -1L
    }
}
