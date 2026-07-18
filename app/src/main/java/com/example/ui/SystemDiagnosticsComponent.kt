package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TerminalTheme

@Composable
fun SystemDiagnosticsComponent(
    data: SystemDiagnosticsData?,
    theme: TerminalTheme,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = theme.background.copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.dp, theme.promptColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("system_diagnostics_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = "System Telemetry Icon",
                        tint = theme.successColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SYSTEM TELEMETRY MONITOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = theme.foreground
                    )
                }

                // Close / Pulse dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Blinking Online pulse dot
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "blink"
                    )

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(theme.successColor, RoundedCornerShape(3.dp))
                            .alpha(pulseAlpha)
                    )
                    Text(
                        text = "LIVE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.successColor
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "[X]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.errorColor,
                        modifier = Modifier
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Close System Diagnostics"
                            ) { onClose() }
                            .padding(horizontal = 4.dp)
                            .testTag("diagnostics_close_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = theme.foreground.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            if (data == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = theme.promptColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Querying system diagnostics...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = theme.foreground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // CPU Section
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "CPU LOAD",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.promptColor
                            )
                            Text(
                                text = "${String.format("%.1f", data.cpuUsage)}% [${data.activeCores} Cores]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = theme.foreground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        DiagnosticsProgressBar(
                            progress = data.cpuUsage / 100.0,
                            theme = theme
                        )
                    }

                    // RAM Section
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SYSTEM RAM",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.promptColor
                            )
                            Text(
                                text = "${String.format("%.2f", data.usedRamGb)}G / ${String.format("%.2f", data.totalRamGb)}G [${String.format("%.1f", data.ramPercentage)}%]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = theme.foreground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        DiagnosticsProgressBar(
                            progress = data.ramPercentage / 100.0,
                            theme = theme
                        )
                    }

                    // Storage Section
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "STORAGE (filesDir)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.promptColor
                            )
                            Text(
                                text = "${String.format("%.2f", data.usedStorageGb)}G / ${String.format("%.2f", data.totalStorageGb)}G [${String.format("%.1f", data.storagePercentage)}%]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = theme.foreground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        DiagnosticsProgressBar(
                            progress = data.storagePercentage / 100.0,
                            theme = theme
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = theme.foreground.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(4.dp))

                    // Secondary telemetry details grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TelemetryLine(label = "HEAP USED", value = "${String.format("%.1f", data.jvmHeapUsedMb)} MB", theme = theme)
                            TelemetryLine(label = "HEAP MAX", value = "${String.format("%.1f", data.jvmHeapMaxMb)} MB", theme = theme)
                            TelemetryLine(label = "JVM THREADS", value = "${data.jvmThreads}", theme = theme)
                        }

                        Column(
                            modifier = Modifier.weight(1.5f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TelemetryLine(label = "BATTERY", value = "${String.format("%.1f", data.batteryPercentage)}% (${data.batteryStatus})", theme = theme)
                            TelemetryLine(label = "BAT TEMP/VOLT", value = "${String.format("%.1f", data.batteryTemperature)}°C / ${String.format("%.2f", data.batteryVoltage)}V", theme = theme)
                            TelemetryLine(label = "UPTIME", value = data.uptimeFormatted, theme = theme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryLine(
    label: String,
    value: String,
    theme: TerminalTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = theme.foreground.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = theme.foreground,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun DiagnosticsProgressBar(
    progress: Double,
    theme: TerminalTheme,
    modifier: Modifier = Modifier
) {
    val coercedProgress = progress.coerceIn(0.0, 1.0)
    val totalTicks = 20
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
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = when {
            coercedProgress > 0.85 -> theme.errorColor
            coercedProgress > 0.60 -> theme.infoColor
            else -> theme.successColor
        },
        modifier = modifier.fillMaxWidth()
    )
}
