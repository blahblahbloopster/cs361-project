package com.github.osuasdt.groundstation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.osuasdt.groundstation.ui.theme.GroundstationTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageContainer(fab: Pair<@Composable () -> Unit, FabPosition> = Pair({}, FabPosition.End), content: @Composable (Modifier) -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    GroundstationTheme {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Text("bar title", modifier = Modifier.padding(16.dp))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text("blah") },
                        selected = false,
                        onClick = {}
                    )
                }
            },
            drawerState = drawerState
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                floatingActionButton = fab.first,
                floatingActionButtonPosition = fab.second,
                topBar = {
                    TopAppBar(
                        title = { Text("title goes here") },
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
