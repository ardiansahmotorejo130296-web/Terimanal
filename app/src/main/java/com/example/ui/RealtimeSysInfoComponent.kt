package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RealtimeSysInfoComponent(
    data: SystemDiagnosticsData?,
    theme: TerminalTheme,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = theme.background.copy(alpha = 0.96f)
        ),
        border = BorderStroke(1.5.dp, theme.successColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("sysinfo_diagnostic_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = "System Info Diagnostics",
                        tint = theme.successColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SYSINFO REAL-TIME DIAGNOSTICS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = theme.foreground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Real-time Pulsing Active indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "blink"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(theme.successColor, RoundedCornerShape(4.dp))
                            .alpha(pulseAlpha)
                    )
                    Text(
                        text = "REALTIME",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.successColor
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "[DISMISS]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.errorColor,
                        modifier = Modifier
                            .clickable(
                                onClickLabel = "Close system info",
                                role = Role.Button
                            ) { onClose() }
                            .padding(horizontal = 4.dp)
                            .testTag("sysinfo_close_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = theme.successColor.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(10.dp))

            if (data == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = theme.successColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Awaiting real-time telemetry feed...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = theme.foreground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // CPU USAGE
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CPU LOAD UTILIZATION",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.promptColor
                            )
                            Text(
                                text = "${String.format("%.1f", data.cpuUsage)}% [${data.activeCores} Cores]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.foreground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        SysInfoProgressBar(
                            progress = data.cpuUsage / 100.0,
                            theme = theme
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Thread pool count: ${data.jvmThreads} threads",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = theme.foreground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Tick: nominal",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = theme.successColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // MEMORY AVAILABILITY
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SYSTEM RAM AVAILABILITY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.promptColor
                            )
                            val availRamGb = data.totalRamGb - data.usedRamGb
                            Text(
                                text = "${String.format("%.2f", availRamGb)}G Avail / ${String.format("%.2f", data.totalRamGb)}G [${String.format("%.1f", 100.0 - data.ramPercentage)}% Free]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.foreground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        SysInfoProgressBar(
                            progress = data.ramPercentage / 100.0,
                            theme = theme
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "JVM Runtime Heap: ${String.format("%.1f", data.jvmHeapUsedMb)}MB / ${String.format("%.1f", data.jvmHeapMaxMb)}MB",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = theme.foreground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // BATTERY STATUS
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "BATTERY STATUS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.promptColor
                            )
                            Text(
                                text = "${String.format("%.1f", data.batteryPercentage)}% [${data.batteryStatus}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.foreground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        SysInfoProgressBar(
                            progress = data.batteryPercentage / 100.0,
                            theme = theme
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Temp: ${String.format("%.1f", data.batteryTemperature)}°C  |  Voltage: ${String.format("%.2f", data.batteryVoltage)}V",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = theme.foreground.copy(alpha = 0.5f)
                            )
                            if (data.batteryStatus.equals("Charging", ignoreCase = true)) {
                                Text(
                                    text = "⚡ PLUGGED AC/USB",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.successColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = theme.successColor.copy(alpha = 0.15f))
                    
                    // Uptime footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM UPTIME: ${data.uptimeFormatted}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.foreground.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "STATUS: ONLINE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.successColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SysInfoProgressBar(
    progress: Double,
    theme: TerminalTheme,
    modifier: Modifier = Modifier
) {
    val coercedProgress = progress.coerceIn(0.0, 1.0)
    val totalTicks = 24
    val activeTicks = (coercedProgress * totalTicks).toInt()
    
    val barText = buildString {
        append("[")
        for (i in 0 until totalTicks) {
            if (i < activeTicks) {
                append("█")
            } else {
                append("░")
            }
        }
        append("]")
    }

    Text(
        text = barText,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = when {
            coercedProgress > 0.85 -> theme.errorColor
            coercedProgress > 0.60 -> theme.infoColor
            else -> theme.successColor
        },
        modifier = modifier.fillMaxWidth()
    )
}
