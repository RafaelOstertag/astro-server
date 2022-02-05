package ch.guengel.astro.server

import ch.guengel.astro.coordinates.Angle
import ch.guengel.astro.coordinates.RightAscension
import org.jeasy.random.EasyRandomParameters
import kotlin.random.Random

internal val easyRandomParameters = EasyRandomParameters()
    .randomize(RightAscension::class.java) {
        RightAscension(Random.nextInt(0, 24),
            Random.nextInt(0, 60),
            Random.nextDouble(0.0, 60.0))
    }
    .randomize(Angle::class.java) {
        Angle(Random.nextInt(-89, 90),
            Random.nextInt(0, 60),
            Random.nextDouble(0.0, 60.0))
    }
