package com.github.osuasdt.groundstation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SnapSpec
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark

@Serializable
object Home
@Serializable
object Configure

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var computer: ComputerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = Home) {
                composable<Home> {
                    MainView(navController, computer.data)
                }
                composable<Configure> {
                    ConfigureView(navController)
                }
            }
        }
    }
}

@Composable
fun DrawerContent(controller: NavController) {
    ModalDrawerSheet {
        Text("bar title", modifier = Modifier.padding(16.dp))
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text("Home") },
            selected = false,  // FIXME
            onClick = {
                controller.navigate(route = Home)
            }
        )
        NavigationDrawerItem(
            label = { Text("Configure") },
            selected = false,  // FIXME
            onClick = {
                controller.navigate(route = Configure)
            }
        )
    }
}

@Composable
fun ConfigureView(controller: NavController) {
    PageContainer({ DrawerContent(controller) }) { modifier ->
        LazyColumn(verticalArrangement = Arrangement.Top, modifier = modifier.fillMaxWidth()) {
            item {
                Text("Pyro", fontSize = 24.sp/*, modifier = modifier.align(Alignment.CenterHorizontally)*/)
            }
        }
    }
}

@Composable
fun MainView(controller: NavController, flow: Flow<ComputerStatus>) {
    val computer by flow.collectAsState(initial = ComputerStatus())

    PageContainer({ DrawerContent(controller) }, Pair({ FloatingActionButton({}) { Icon(Icons.Default.Edit, contentDescription = "Edit") } }, FabPosition.End)) { modifier ->
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
        val snap = SnapSpec<Float>()
        while (true) {
            val millis = lastSeen.elapsedNow().absoluteValue.inWholeMilliseconds
            val frac = (millis / expected.inWholeMilliseconds.toFloat()).coerceAtMost(1.0f)
            // snap bar to zero if it went down
            if (millis < elapsed) {
                fraction.animateTo(frac, animationSpec = snap)
            } else {
                fraction.animateTo(frac)
            }
            elapsed = millis

            if (elapsed > expected.inWholeMilliseconds) {
                delay(25)
            } else {
                delay(10)
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
