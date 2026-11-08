package org.std81atmosphere.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

/** Physical invariants that must hold for every height in the standard range. */
class InvariantsTest {

    private val breakpoints = listOf(
        0.0, 11019.0, 20063.0, 32162.0, 47350.0, 51412.0,
        71802.0, 86152.0, 95411.0, 104128.0, 120000.0,
    )

    private fun heights(step: Double, start: Double = -2000.0, stop: Double = 1_200_000.0) =
        generateSequence(start) { it + step }.takeWhile { it <= stop }

    @Test
    fun idealGasLaw() {
        for (h in heights(2500.0)) {
            val s = Atmosphere.compute(h)
            val expected = s.pressure * s.molarMass / Constants.R / s.temperature
            assertEquals(expected, s.density, abs(expected) * 1e-12 + 1e-30)
        }
    }

    @Test
    fun speedOfSoundFormula() {
        for (h in heights(5000.0)) {
            val s = Atmosphere.compute(h)
            val expected = kotlin.math.sqrt(
                Constants.GAMMA * Constants.R * s.temperature / s.molarMass
            )
            assertEquals(expected, s.soundSpeed, abs(expected) * 1e-9)
        }
    }

    @Test
    fun geopotentialFormula() {
        val radius = Constants.EARTH_RADIUS
        for (h in heights(10_000.0)) {
            val expected = radius * h / (radius + h)
            assertEquals(expected, Atmosphere.geopotentialHeight(h), abs(expected) * 1e-9 + 1e-6)
        }
    }

    @Test
    fun pressureDecreasesWithHeight() {
        var previous = Double.POSITIVE_INFINITY
        for (h in heights(500.0)) {
            val current = Atmosphere.pressure(h)
            assertTrue("pressure not decreasing at $h", current < previous)
            previous = current
        }
    }

    @Test
    fun quantitiesArePositive() {
        for (h in heights(5000.0)) {
            assertTrue(Atmosphere.temperature(h) > 0)
            assertTrue(Atmosphere.molarWeight(h) > 0)
            assertTrue(Atmosphere.pressure(h) > 0)
            assertTrue(Atmosphere.density(h) > 0)
            assertTrue(Atmosphere.soundSpeed(h) > 0)
        }
    }

    @Test
    fun temperatureAndMassRanges() {
        for (h in heights(5000.0)) {
            assertTrue(Atmosphere.temperature(h) in 150.0..1100.0)
            assertTrue(Atmosphere.molarWeight(h) in 0.002..0.030)
        }
    }

    @Test
    fun continuousAtLayerBoundaries() {
        val eps = 1e-3
        for (h in breakpoints) {
            if (h == 120000.0) continue // known ~0.7% joint, see below
            val low = Atmosphere.pressure(h - eps)
            val high = Atmosphere.pressure(h + eps)
            val jump = abs(high - low) / abs(low)
            assertTrue("pressure jumps at $h: $jump", jump < 5e-4)
        }
    }

    @Test
    fun pressureJointAt120KmIsSmall() {
        // The appendix polynomial starts ~0.75% below the barometric branch at
        // exactly 120 km; it vanishes by 121 km.  Keep the step under 2%.
        val eps = 1e-3
        val low = Atmosphere.pressure(120_000.0 - eps)
        val high = Atmosphere.pressure(120_000.0 + eps)
        val jump = abs(high - low) / abs(low)
        assertTrue("120 km joint is $jump", jump < 2e-2)
    }

    @Test
    fun transportPropertiesStopAbove90Km() {
        // The model (like the standard) defines them for h <= 90 km; above
        // that strictly they become null.
        for (h in heights(25_000.0, start = 90_000.0, stop = 1_200_000.0)) {
            if (h > Constants.TRANSPORT_MAX_HEIGHT) {
                assertTrue("dynamic viscosity at $h", Atmosphere.dynamicViscosity(h) == null)
                assertTrue("kinematic viscosity at $h", Atmosphere.kinematicViscosity(h) == null)
                assertTrue("thermal conductivity at $h", Atmosphere.thermalConductivity(h) == null)
            }
        }
    }

    @Test
    fun invalidHeightsAreRejected() {
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, -2001.0, 1_200_001.0)) {
            try {
                Atmosphere.compute(bad)
                fail("expected HeightError for $bad")
            } catch (error: HeightError) {
                // expected
            }
        }
    }

    @Test
    fun boundaryHeightsAreAccepted() {
        for (h in listOf(-2000.0, 0.0, 1_200_000.0)) {
            Atmosphere.compute(h)
        }
    }
}