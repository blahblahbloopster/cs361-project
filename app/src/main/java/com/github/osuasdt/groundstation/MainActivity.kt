package com.github.osuasdt.groundstation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Badge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.github.osuasdt.groundstation.ui.theme.GroundstationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PageContainer { modifier ->
                DeploymentInfo(modifier)
            }
        }
    }
}

@Composable
fun SingleChannelInfo(modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("1")
        Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
            Text("name", modifier = Modifier.padding(PaddingValues(8.dp, 0.dp)), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Row {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Green).align(Alignment.CenterVertically))
            Spacer(Modifier.width(5.dp))
            Text("status")
        }
    }
}

@Composable
fun DeploymentInfo(modifier: Modifier) {
    Row(modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp)), horizontalArrangement = Arrangement.SpaceEvenly) {
        SingleChannelInfo(modifier)
        SingleChannelInfo(modifier)
        SingleChannelInfo(modifier)
    }
}
