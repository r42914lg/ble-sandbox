package com.r42914lg.blesandbox.tempgraph

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

private val TealAccent = Color(0xFF0B7A85)
private val NeutralLine = Color(0xFFD3DBE2)

@Composable
fun TempGraphWidget(
    modifier: Modifier = Modifier,
    viewModel: TempGraphViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TempGraphWidgetContent(
        uiState = uiState,
        onSelectWindow = { viewModel.selectTimeWindow(it) },
        modifier = modifier
    )
}

@Composable
fun TempGraphWidgetContent(
    uiState: TempGraphUiState,
    onSelectWindow: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, NeutralLine),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeWindow.entries.forEach { window ->
                        val isSelected = uiState.timeWindow == window
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = if (isSelected) TealAccent else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, NeutralLine),
                            modifier = Modifier.clickable { onSelectWindow(window) }
                        ) {
                            Text(
                                text = window.label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.readings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Collecting temperature data...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val points = uiState.readings
                    val minT = uiState.minTemp
                    val maxT = uiState.maxTemp
                    val tempRange = (maxT - minT).coerceAtLeast(1.0f)

                    val width = size.width
                    val height = size.height

                    val linePath = Path()
                    val fillPath = Path()

                    val stepX = if (points.size > 1) width / (points.size - 1) else width

                    points.forEachIndexed { i, record ->
                        val x = i * stepX
                        val normalizedY = (record.temperatureCelsius - minT) / tempRange
                        val y = height - (normalizedY * height)

                        if (i == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    fillPath.lineTo(width, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TealAccent.copy(alpha = 0.35f),
                                TealAccent.copy(alpha = 0.02f)
                            )
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = TealAccent,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
