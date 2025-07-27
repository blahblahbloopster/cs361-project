package com.github.osuasdt.groundstation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun Duration.humanReadableString() = when {
    this < 1.seconds -> "$inWholeMilliseconds ms"
    this < 10.seconds -> "%d.%03d s".format(inWholeMilliseconds / 1000, inWholeMilliseconds % 1000)
    this < 1.minutes -> "$inWholeSeconds s"
    this < 1.hours -> { val s = inWholeSeconds; "%d:%02d".format(s / 60, s % 60) }
    else -> { val s = inWholeSeconds; "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) }
}
