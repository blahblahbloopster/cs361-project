package com.github.osuasdt.groundstation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.time.TimeMark

sealed class ChannelConfig(val delay: Float) {
    data object DisabledChannel : ChannelConfig(0.0f)
    class ApogeeDeploy(delay: Float) : ChannelConfig(delay)
    class DescentDeploy(altitudeMeters: Float, delay: Float) : ChannelConfig(delay)
}

enum class ChannelState {
    OK, NO_CONTINUITY, DISABLED;

    @Composable
    fun color() = when (this) {
        OK -> Color.Green
        NO_CONTINUITY -> Color.Red
        DISABLED -> Color.Gray
    }

    @Composable
    fun text() = when (this) {
        OK -> "Ready"
        NO_CONTINUITY -> "Fault"
        DISABLED -> "Disabled"
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
    val timestamp: TimeMark
)
