package com.github.osuasdt.groundstation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.time.TimeMark
import kotlin.time.TimeSource

sealed class ChannelConfig(val delay: Float) {
    data object DisabledChannel : ChannelConfig(0.0f)
    class ApogeeDeploy(delay: Float) : ChannelConfig(delay)
    class DescentDeploy(altitudeMeters: Float, delay: Float) : ChannelConfig(delay)
}

enum class ChannelState {
    OK, NO_CONTINUITY, DISABLED, FIRED;

    @Composable
    fun color() = when (this) {
        OK -> Color.Green
        NO_CONTINUITY -> Color.Red
        DISABLED -> Color.Gray
        FIRED -> MaterialTheme.colorScheme.primary
    }

    @Composable
    fun text() = when (this) {
        OK -> "Ready"
        NO_CONTINUITY -> "Fault"
        DISABLED -> "Disabled"
        FIRED -> "Fired"
    }
}

data class FullChannelInfo(val number: Int, val config: ChannelConfig, val state: ChannelState)

enum class ComputerState {
    PAD, BOOST, COAST, APOGEE, DESCENT, LAWNDART, LANDED
}

data class ComputerStatus(
    val name: String, val channels: List<FullChannelInfo>,
    val lat: Double, val lon: Double, val gpsAltMetersWGS84: Double,
    val sats: Int,
    val baroAltMetersAGL: Double, val verticalVelocityMetersPerSecond: Double,
    val state: ComputerState,
    val battery: Double,
    val timestamp: TimeMark,
    val rssi: Double
) {
    constructor() : this("", listOf(), 0.0, 0.0, 0.0, 0, 0.0, 0.0, ComputerState.PAD, 0.0, TimeSource.Monotonic.markNow(), 0.0)
}
