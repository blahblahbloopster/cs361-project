package com.github.osuasdt.groundstation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeSource

@Singleton
class ComputerRepository @Inject constructor() {
    //private val _data = MutableStateFlow(ComputerStatus())
    //val data: Flow<ComputerStatus> = _data.asStateFlow()
    val data: Flow<ComputerStatus> = flow {
        while (true) {
            emit(ComputerStatus(
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
            ))

            delay(1500)
        }
    }
}
