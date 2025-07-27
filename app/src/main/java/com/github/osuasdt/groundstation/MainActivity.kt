package com.github.osuasdt.groundstation

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@HiltAndroidApp
class GroundstationApplication : Application() {
    @Inject lateinit var computer: DefaultComputerRepository

    override fun onCreate() {
        super.onCreate()

        Log.i("application", "$computer")
    }
}

interface ComputerRepository {
    fun getComputer(): ComputerStatus
}

@Singleton
class DefaultComputerRepository @Inject constructor() : ComputerRepository {
    override fun getComputer(): ComputerStatus {
        return ComputerStatus(
            "procket",
            listOf(
                FullChannelInfo(1, ChannelConfig.DescentDeploy(200.0f, 0.0f), ChannelState.OK),
                FullChannelInfo(2, ChannelConfig.ApogeeDeploy(0.0f), ChannelState.NO_CONTINUITY),
                FullChannelInfo(3, ChannelConfig.DisabledChannel, ChannelState.DISABLED),
                FullChannelInfo(4, ChannelConfig.ApogeeDeploy(1.0f), ChannelState.FIRED)
            ),
            45.0, -120.0, 0.0, 8,
            0.0, 0.0,
            ComputerState.PAD, 7.4, TimeSource.Monotonic.markNow(), -90.0
        )
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var computer: DefaultComputerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainView(computer.getComputer())
        }
    }
}

@Composable
fun MainView(computer: ComputerStatus) {
    PageContainer { modifier ->
        Column(verticalArrangement = Arrangement.Top) {
            DeploymentInfo(computer.channels, modifier.padding(0.dp, 8.dp))
            StatusInfo(computer, modifier.padding(0.dp, 8.dp))
            LastSeenIndicator(computer.timestamp, computer.rssi, modifier.padding(0.dp, 8.dp))
        }
    }
}

@Composable
fun SingleChannelInfo(info: FullChannelInfo, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${info.number}")
        Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
            Text("name", modifier = Modifier.padding(PaddingValues(8.dp, 0.dp)), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Row {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(info.state.color()).align(Alignment.CenterVertically))
            Spacer(Modifier.width(5.dp))
            Text(info.state.text())
        }
    }
}

@Composable
fun DeploymentInfo(channels: List<FullChannelInfo>, modifier: Modifier) {
    Row(modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp)), horizontalArrangement = Arrangement.SpaceEvenly) {
        channels.forEach {
            SingleChannelInfo(it, modifier)
        }
    }
}

@Composable
fun StatusInfo(computer: ComputerStatus, modifier: Modifier) {
    Column(modifier.border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp)).padding(6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("%.5f, %.5f".format(computer.lat, computer.lon), fontSize = 24.sp)
            // TODO: WGS84 altitude is not useful at all to the user, fix
            Text("${computer.gpsAltMetersWGS84.toInt()} m", fontSize = 24.sp)
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("${computer.sats} sats   ${"%.1f".format(computer.verticalVelocityMetersPerSecond.absoluteValue)} m/s")
            Text("state: ${computer.state.toString().lowercase()}")
        }
    }
}

@Composable
fun LastSeenIndicator(lastSeen: TimeMark, rssi: Double, modifier: Modifier) {
    val fraction = remember { Animatable(0.0f) }
    var elapsed by remember { mutableLongStateOf(0) }
    LaunchedEffect(lastSeen) {
        val expected = 5.seconds
        while (true) {
            val millis = lastSeen.elapsedNow().absoluteValue.inWholeMilliseconds
            val frac = (millis / expected.inWholeMilliseconds.toFloat()).coerceAtMost(1.0f)
            elapsed = millis
            fraction.animateTo(frac)
            if (elapsed > expected.inWholeMilliseconds) {
                delay(25)
            }
        }
    }

    Column(modifier.border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp)).padding(6.dp)) {
        Text("Last seen ${elapsed.milliseconds.humanReadableString()} ago")
        Box(modifier.fillMaxWidth().height(5.dp).drawBehind {
            drawRect(color = Color.Gray, size = this.size)
            drawRect(
                color = if (fraction.value == 1.0f) Color.Red else Color.White,
                size = Size(size.width * fraction.value, size.height)
            )
        })
        Text("RSSI = ${rssi.toInt()} dBm")
    }
}

fun Duration.humanReadableString() = when {
    this < 1.seconds -> "${this.inWholeMilliseconds} ms"
    this < 1.minutes -> "${this.inWholeSeconds} s"
    this < 1.hours -> { val s = this.inWholeSeconds; "%d:%02d".format(s / 60, s % 60) }
    else -> { val s = this.inWholeSeconds; "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) }
}

