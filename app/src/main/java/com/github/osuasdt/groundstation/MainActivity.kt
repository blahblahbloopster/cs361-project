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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    MainView({}, { navController.navigate(Configure) }, computer.data)
                }
                composable<Configure> {
                    ConfigureView({ navController.navigate(Home) }, {}, computer)
                }
            }
        }
    }
}

@Composable
fun ConfigureView(toMain: () -> Unit, toConfig: () -> Unit, repository: ComputerRepository) {
    val computer by repository.data.collectAsState(initial = ComputerStatus())
    PageContainer(toMain, toConfig, bottomBar = {
        BottomAppBar {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton({}, modifier = Modifier.fillMaxHeight()) {
                    Icon(Icons.Default.Close, "Cancel")
                }
                IconButton({}, modifier = Modifier.fillMaxHeight()) {
                    Icon(Icons.Default.Refresh, "Reset")
                }
                IconButton({}, modifier = Modifier.fillMaxHeight()) {
                    Icon(Icons.Default.Check, "Apply")
                }
            }
        }
    }) { modifier ->
        Column(verticalArrangement = Arrangement.Top, modifier = modifier.fillMaxWidth()) {
            Text("Pyro", fontSize = 24.sp, modifier = modifier.align(Alignment.CenterHorizontally))

            computer.channels.forEach { channel ->
                var newConfig by remember { mutableStateOf(channel.config) }
                var expanded by remember { mutableStateOf(false) }
                Row(Modifier.padding(0.dp, 8.dp).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape).padding(12.dp, 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${channel.number}", fontSize = 24.sp)
                    when (val c = newConfig) {
                        is ChannelConfig.ApogeeDeploy -> Text("Apogee")
                        is ChannelConfig.DescentDeploy -> {
                            //val state = rememberTextFieldState()
                            TextField(rememberTextFieldState())
                        }
                        ChannelConfig.DisabledChannel -> Text("Disabled")
                    }
                    Box {
                        IconButton({ expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, "Edit")
                        }
                        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Apogee") }, onClick = {
                                newConfig = ChannelConfig.ApogeeDeploy(0.0f)
                                expanded = false
                            })
                            DropdownMenuItem(text = { Text("Altitude") }, onClick = {
                                newConfig = ChannelConfig.DescentDeploy(200.0f, 0.0f)
                                expanded = false
                            })
                            DropdownMenuItem(text = { Text("Disabled") }, onClick = {
                                newConfig = ChannelConfig.DisabledChannel
                                expanded = false
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainView(toMain: () -> Unit, toConfig: () -> Unit, flow: Flow<ComputerStatus>) {
    val computer by flow.collectAsState(initial = ComputerStatus())

    PageContainer(toMain, toConfig, Pair({ FloatingActionButton(toConfig) { Icon(Icons.Default.Edit, contentDescription = "Edit") } }, FabPosition.End)) { modifier ->
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
