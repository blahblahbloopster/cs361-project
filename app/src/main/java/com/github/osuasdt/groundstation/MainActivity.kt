package com.github.osuasdt.groundstation

import android.content.ClipData
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SnapSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
            var selectedGroundstation: Long? by remember { mutableStateOf(null) }
            var lastGroundstationPacket: GroundStation? by remember { mutableStateOf(null) }
            var selectedComputer: Long? by remember { mutableStateOf(null) }
            var lastComputerPacket: ComputerStatus? by remember { mutableStateOf(null) }
            var modified by remember { mutableStateOf(lastComputerPacket) }
            val l by computer.data.collectAsState(emptyList())
            l.find { it.id == selectedGroundstation }?.let { lastGroundstationPacket = it; it.computers.find { c -> c.uuid == selectedComputer }?.let { c -> lastComputerPacket = c } }
            if (l.isNotEmpty() && selectedGroundstation == null) {
                selectedGroundstation = l.first().id
            }
            if (lastGroundstationPacket?.computers?.isNotEmpty() == true && selectedComputer == null) {
                selectedComputer = lastGroundstationPacket!!.computers.first().uuid
            }
            val m = { g: Long, c: Long? ->
                selectedGroundstation = g
                selectedComputer = c
            }

            NavHost(navController = navController, startDestination = Home) {
                composable<Home> {
                    if (lastComputerPacket != null) {
                        // FIXME
                        MainView({}, { modified = lastComputerPacket; navController.navigate(Configure) }, lastComputerPacket!!, computer.last(), selectedGroundstation!!, selectedComputer, m)
                    } else {
                        Text("Waiting for groundstation...")
                    }
                }
                composable<Configure> {
                    modified?.let { mod ->
                        lastComputerPacket?.let { c ->
                            ConfigureView({ navController.navigate(Home) }, {}, mod, { modified = it }, { computer.updateComputer(it.uuid) { _ -> it } }, c, computer.last(), selectedGroundstation!!, selectedComputer, m)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigureView(toMain: () -> Unit, toConfig: () -> Unit, modified: ComputerStatus, modifiedModified: (ComputerStatus) -> Unit, saved: (ComputerStatus) -> Unit, computer: ComputerStatus, availableGroundstations: List<GroundStation>, selectedGroundstation: Long, selectedComputer: Long?, selectionModified: (Long, Long?) -> Unit) {
    PageContainer(computer.name, toMain, toConfig, availableGroundstations, selectedGroundstation, selectedComputer, selectionModified, bottomBar = {
        BottomAppBar {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(toMain, modifier = Modifier.fillMaxHeight().aspectRatio(1.0f, true)) {
                    Icon(Icons.Default.Close, "Cancel")
                }
                IconButton({ modifiedModified(computer) }, modifier = Modifier.fillMaxHeight().aspectRatio(1.0f, true)) {
                    Icon(Icons.Default.Refresh, "Reset")
                }
                IconButton({ saved(modified); toMain() }, modifier = Modifier.fillMaxHeight().aspectRatio(1.0f, true)) {
                    Icon(Icons.Default.Check, "Apply")
                }
            }
        }
    }) { modifier ->
        Column(verticalArrangement = Arrangement.Top, modifier = modifier.fillMaxWidth()) {
            Text("Pyro", fontSize = 24.sp, modifier = modifier.align(Alignment.CenterHorizontally))

            computer.channels.forEachIndexed { idx, _ ->
                var expanded by remember { mutableStateOf(false) }

                val hue: Float
                val saturation: Float
                when (modified.channels[idx].config) {
                    is ChannelConfig.ApogeeDeploy -> { hue = 200.0f; saturation = 0.7f }
                    is ChannelConfig.DescentDeploy -> { hue = 0.0f; saturation = 0.7f }
                    ChannelConfig.DisabledChannel -> { hue = 0.0f; saturation = 0.0f }
                }
                val background = Color.hsl(hue, saturation, if (isSystemInDarkTheme()) 0.1f else 0.9f)
                Row(Modifier.padding(0.dp, 8.dp).fillMaxWidth().height(80.dp).clip(CircleShape).background(background).border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape).padding(12.dp, 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${modified.channels[idx].number}", fontSize = 24.sp)
                    Box(Modifier.weight(1.0f)) {
                        when (val c = modified.channels[idx].config) {
                            is ChannelConfig.ApogeeDeploy -> Button({ expanded = !expanded }, colors = ButtonColors(Color.Transparent, MaterialTheme.colorScheme.onBackground, Color.White, Color.Black), modifier = Modifier.fillMaxSize()) { Text("Apogee") }
                            is ChannelConfig.DescentDeploy -> {
                                TextField(rememberTextFieldState("${c.altitudeMeters}"), prefix = { Text("At", modifier.padding(end = 8.dp)) }, suffix = { Text("m") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), colors = TextFieldDefaults.colors(unfocusedContainerColor = background, focusedContainerColor = background))
                            }
                            ChannelConfig.DisabledChannel -> Button({ expanded = !expanded }, colors = ButtonColors(Color.Transparent, MaterialTheme.colorScheme.onBackground, Color.White, Color.Black), modifier = Modifier.fillMaxSize()) { Text("Disabled") }
                        }
                    }
                    Box {
                        IconButton({ expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, "Edit")
                        }
                        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Apogee") }, onClick = {
                                modifiedModified(modified.replaceChannelConfig(idx, ChannelConfig.ApogeeDeploy(0.0f)))
                                expanded = false
                            })
                            DropdownMenuItem(text = { Text("Altitude") }, onClick = {
                                modifiedModified(modified.replaceChannelConfig(idx, ChannelConfig.DescentDeploy(200.0f, 0.0f)))
                                expanded = false
                            })
                            DropdownMenuItem(text = { Text("Disabled") }, onClick = {
                                modifiedModified(modified.replaceChannelConfig(idx, ChannelConfig.DisabledChannel))
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
fun MainView(toMain: () -> Unit, toConfig: () -> Unit, computer: ComputerStatus, availableGroundstations: List<GroundStation>, selectedGroundstation: Long, selectedComputer: Long?, selectionModified: (Long, Long?) -> Unit) {
    PageContainer(computer.name, toMain, toConfig, availableGroundstations, selectedGroundstation, selectedComputer, selectionModified, Pair({ FloatingActionButton(toConfig, modifier = Modifier.size(80.dp), shape = CircleShape) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(30.dp)) } }, FabPosition.End)) { modifier ->
        Column(verticalArrangement = Arrangement.Top) {
            DeploymentInfo(computer.channels, modifier.padding(0.dp, 8.dp))
            StatusInfo(computer, modifier.padding(0.dp, 8.dp))
            LastSeenIndicator(computer.timestamp, computer.rssi, modifier.padding(0.dp, 8.dp))
        }
    }
}

@Composable
fun SingleChannelInfo(info: FullChannelInfo) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${info.number}")
        val hue: Float
        val saturation: Float
        when (info.config) {
            is ChannelConfig.ApogeeDeploy -> { hue = 200.0f; saturation = 0.7f }
            is ChannelConfig.DescentDeploy -> { hue = 0.0f; saturation = 0.7f }
            ChannelConfig.DisabledChannel -> { hue = 0.0f; saturation = 0.0f }
        }
        val background = Color.hsl(hue, saturation, if (isSystemInDarkTheme()) 0.7f else 0.3f)
        Box(Modifier.clip(CircleShape).background(background)) {
            Text(info.config.summary(), modifier = Modifier.padding(PaddingValues(8.dp, 0.dp)), color = MaterialTheme.colorScheme.background)
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
            SingleChannelInfo(it)
        }
    }
}

@Composable
fun StatusInfo(computer: ComputerStatus, modifier: Modifier) {
    Column(modifier.border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp)).padding(6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val background = if (isSystemInDarkTheme()) Color(0xFF202020) else Color(0xFFF0F0F0)
            val stroke = if (isSystemInDarkTheme()) Color(0xFF888888) else Color(0xFF888888)
            val manager = LocalClipboard.current
            val scope = rememberCoroutineScope()

            Button({
                scope.launch {
                    manager.setClipEntry(
                        ClipEntry(
                            ClipData.newPlainText(
                                "${computer.name} location",
                                "%.5f, %.5f".format(computer.lat, computer.lon)
                            )
                        )
                    )
                }
            }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, stroke), contentPadding = PaddingValues(2.dp), colors = ButtonColors(background, MaterialTheme.colorScheme.onBackground, Color.Red, Color.Red)) {
                Text("%.5f, %.5f".format(computer.lat, computer.lon), fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ContentCopy, "Share")
            }
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
