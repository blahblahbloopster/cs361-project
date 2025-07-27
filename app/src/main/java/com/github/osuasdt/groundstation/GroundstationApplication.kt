package com.github.osuasdt.groundstation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GroundstationApplication : Application() {
    @Inject
    lateinit var computer: ComputerRepository
}
