package com.github.osuasdt.groundstation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeSource

@Singleton
class ComputerRepository @Inject constructor() {
    private val _data = MutableStateFlow(ComputerStatus())
    val data: Flow<ComputerStatus> = _data.asStateFlow()

    fun last(): ComputerStatus {
        return _data.value
    }

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                val st = ComputerStatus(
                    19348093824082390,
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
                _data.value = st
                delay(10000)
            }
        }
    }

    fun updateComputer(block: (ComputerStatus) -> ComputerStatus) {
        _data.value = block(_data.value)
    }
}
