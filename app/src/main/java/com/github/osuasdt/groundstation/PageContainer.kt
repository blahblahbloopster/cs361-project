package com.github.osuasdt.groundstation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.osuasdt.groundstation.ui.theme.GroundstationTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageContainer(title: String, toMain: () -> Unit, toConfig: () -> Unit, availableGroundstations: List<GroundStation>, selectedGroundstation: Long, selectedComputer: Long?, selectionModified: (Long, Long?) -> Unit, fab: Pair<@Composable () -> Unit, FabPosition> = Pair({}, FabPosition.End), bottomBar: @Composable () -> Unit = {}, content: @Composable (Modifier) -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    GroundstationTheme {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    //Text(title, modifier = Modifier.padding(16.dp))
                    ComputerPicker(availableGroundstations, selectedGroundstation, selectedComputer, selectionModified, Modifier.padding(16.dp))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text("Home") },
                        selected = false,  // FIXME
                        onClick = {
                            toMain()
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Configure") },
                        selected = false,  // FIXME
                        onClick = {
                            toConfig()
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                }
            },
            drawerState = drawerState
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                floatingActionButton = fab.first,
                floatingActionButtonPosition = fab.second,
                bottomBar = bottomBar,
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) {
                                            drawerState.open()
                                        } else {
                                            drawerState.close()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(Modifier.padding(innerPadding).padding(16.dp)) {
                    content(Modifier)
                }
            }
        }
    }
}

@Composable
fun ComputerPicker(availableGroundstations: List<GroundStation>, selectedGroundstation: Long, selectedComputer: Long?, modified: (Long, Long?) -> Unit, modifier: Modifier) {
    var leftExpanded by remember { mutableStateOf(false) }
    var rightExpanded by remember { mutableStateOf(false) }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Box(Modifier.weight(1.0f)) {
            OutlinedButton({ leftExpanded = !leftExpanded }, Modifier.fillMaxWidth()) {
                Row {
                    Text("Groundstation ${availableGroundstations.find { it.id == selectedGroundstation }?.id}")
                    Icon(Icons.Default.ArrowDropDown, "Pick")
                }
            }
            DropdownMenu(leftExpanded, { leftExpanded = false }) {
                availableGroundstations.forEach {
                    // FIXME add name
                    DropdownMenuItem({ Text("Groundstation ${it.id}") }, {
                        modified(it.id, it.computers.firstOrNull()?.uuid)
                        leftExpanded = false
                    })
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Box(Modifier.weight(1.0f)) {
            OutlinedButton({ rightExpanded = !rightExpanded }, Modifier.fillMaxWidth()) {
                Row {
                    Text(availableGroundstations.find { it.id == selectedGroundstation }!!.computers.find { it.uuid == selectedComputer }?.name ?: "None")
                    Icon(Icons.Default.ArrowDropDown, "Pick")
                }
            }
            DropdownMenu(rightExpanded, { rightExpanded = false }) {
                availableGroundstations.find { it.id == selectedGroundstation }!!.computers.forEach {
                    DropdownMenuItem({ Text(it.name) }, {
                        modified(selectedGroundstation, it.uuid)
                        rightExpanded = false
                    })
                }
            }
        }
    }
}
