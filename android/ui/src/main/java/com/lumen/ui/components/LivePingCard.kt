package com.lumen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
                pingHistory = (pingHistory + ping.toFloat()).takeLast(14)
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

    // پالت رنگ هوشمند بر اساس کیفیت پینگ
    val targetColor = when {
        currentPing in 1..75 -> Color(0xFF00E676)   // سبز نئونی گیمینگ
        currentPing in 76..120 -> Color(0xFFFFB300) // زرد/طلایی سایبری
        else -> Color(0xFFFF3366)                   // سرخابی نئونی پینگ بالا
    }

    val chartColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 600),
        label = "chartColorAnimation"
    )

    val badgeText = when {
        currentPing in 1..75 -> "مناسب برای بازی"
        currentPing in 76..120 -> "متوسط"
        else -> "نامناسب برای بازی"
    }

    // کادر اصلی با فریم نئونی و گرادیان متالیک
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF161922),
                        Color(0xFF0F1117)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        chartColor.copy(alpha = 0.40f),
                        Color(0xFF252A3A).copy(alpha = 0.5f),
                        Color(0xFF151821).copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            // ردیف بالایی: بج کیفیت شبکه و عنوان پینگ لایو
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(chartColor.copy(alpha = 0.12f))
                        .border(1.dp, chartColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(chartColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = badgeText,
                            color = chartColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                        color = Color(0xFF8A92A6),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ردیف وسط: نمودار نوری امواج و عدد درشت پینگ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PingWaveChart(
                    history = pingHistory,
                    chartColor = chartColor,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(end = 14.dp)
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "ms",
                        color = chartColor.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                    )
                    Text(
                        text = if (currentPing > 0) "$currentPing" else "--",
                        color = chartColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // خط فاصل محو و متقارن
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF262B3B),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ردیف پایین: اطلاعات نوسان و میانگین
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(chartColor.copy(alpha = 0.8f))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "اسکنر بلادرنگ",
                        color = Color(0xFF6B7280),
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "میانگین ${avgPing}ms  •  نوسان ${jitter}ms",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// کامپوننت نمودار امواج نرم با هاله نور پرکننده زیر نمودار
@Composable
private fun PingWaveChart(
    history: List<Float>,
    chartColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas

        // خطوط بسیار ظریف اسکنر در پس‌زمینه کارت
        val gridAlpha = 0.08f
        for (i in 1..2) {
            val gridY = size.height * (i / 3f)
            drawLine(
                color = Color.White.copy(alpha = gridAlpha),
                start = Offset(0f, gridY),
                end = Offset(size.width, gridY),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (history.size < 2) {
            val y = size.height / 2f
            drawLine(
                color = chartColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val maxPing = (history.maxOrNull() ?: 200f).coerceAtLeast(100f)
        val minPing = (history.minOrNull() ?: 20f).coerceAtMost(50f)
        val range = (maxPing - minPing).coerceAtLeast(1f)

        val stepX = size.width / (history.size - 1)
        val path = Path()
        val fillPath = Path()

        val points = history.mapIndexed { index, ping ->
            val x = index * stepX
            val normalizedY = 1f - ((ping - minPing) / range)
            val y = (normalizedY * (size.height - 16f)) + 8f
            Offset(x, y)
        }

        // ایجاد خطوط نرم با فرمول منحنی Bezier (به جای خطوط شکسته تیز)
        path.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, size.height)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val cx = (prev.x + curr.x) / 2f

            path.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
            fillPath.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
        }

        fillPath.lineTo(points.last().x, size.height)
        fillPath.close()

        // پر کردن زیر نمودار با گرادیان شفق نوری
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    chartColor.copy(alpha = 0.35f),
                    chartColor.copy(alpha = 0.08f),
                    Color.Transparent
                )
            )
        )

        // رسم خط اصلی نئونی
        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // نقطه درخشان در آخرین مختصات پینگ
        val tip = points.last()
        drawCircle(
            color = chartColor.copy(alpha = 0.30f),
            radius = 6.dp.toPx(),
            center = tip
        )
        drawCircle(
            color = chartColor,
            radius = 3.dp.toPx(),
            center = tip
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
