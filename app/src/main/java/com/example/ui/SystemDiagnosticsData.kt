package com.example.ui

data class SystemDiagnosticsData(
    val cpuUsage: Double,
    val activeCores: Int,
    val jvmThreads: Int,
    val usedRamGb: Double,
    val totalRamGb: Double,
    val ramPercentage: Double,
    val jvmHeapUsedMb: Double,
    val jvmHeapMaxMb: Double,
    val batteryPercentage: Double,
    val batteryStatus: String,
    val batteryTemperature: Float,
    val batteryVoltage: Float,
    val usedStorageGb: Double,
    val totalStorageGb: Double,
    val storagePercentage: Double,
    val uptimeFormatted: String
)
