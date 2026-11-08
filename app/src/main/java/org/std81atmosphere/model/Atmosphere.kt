package org.std81atmosphere.model

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * GOST 4401-81 standard atmosphere model.
 *
 * All functions take *geometric* height in metres (a valid range of
 * [-2000, 1_200_000]) and return SI units.
 */

/** Raised when the height is not a valid number or out of the standard range. */
class HeightError(val code: String) : IllegalArgumentException(code)

/** All standard-atmosphere parameters at one height. */
data class AtmosphereState(
    val height: Double,
    val geopotentialHeight: Double,
    val pressure: Double,
    val temperature: Double,
    val density: Double,
    val molarMass: Double,
    val concentration: Double,
    val soundSpeed: Double,
    val meanFreePath: Double,
    val dynamicViscosity: Double?,
    val kinematicViscosity: Double?,
    val thermalConductivity: Double?,
)

/** Upper atmosphere temperature: (lower, upper, base, gradient). */
private val UPPER_TEMPERATURE = arrayOf(
    doubleArrayOf(120000.0, 140000.0, 334.42, 0.011259),
    doubleArrayOf(140000.0, 160000.0, 559.60, 0.006800),
    doubleArrayOf(160000.0, 200000.0, 695.60, 0.003970),
    doubleArrayOf(200000.0, 250000.0, 854.40, 0.001750),
    doubleArrayOf(250000.0, 325000.0, 941.90, 0.000570),
    doubleArrayOf(325000.0, 400000.0, 984.65, 0.000150),
    doubleArrayOf(400000.0, 600000.0, 995.90, 0.000020),
    doubleArrayOf(600000.0, 800000.0, 999.90, 0.0000005),
    doubleArrayOf(800000.0, 1_200_000.0, 1000.00, 0.0),
)

/** Upper atmosphere molar mass polynomials: (upper, B0, B1, B2, B3) in g/mol. */
private val MOLAR_MASS_POLYNOMIALS = arrayOf(
    doubleArrayOf(250000.0, 46.9083, -29.71210e-05, 12.08693e-10, -1.85675e-15),
    doubleArrayOf(400000.0, 40.4668, -15.52722e-05, 3.55735e-10, -3.02340e-16),
    doubleArrayOf(650000.0, 6.3770, 6.25497e-05, -1.10144e-10, 3.36907e-17),
    doubleArrayOf(900000.0, 75.6896, -17.61243e-05, 1.33603e-10, -2.87884e-17),
    doubleArrayOf(1050000.0, 112.4838, -30.68086e-05, 2.90329e-10, -9.20616e-17),
    doubleArrayOf(1200000.0, 9.89704838, -1.19732e-05, 7.78247e-12, -1.77541e-18),
)

/** Upper atmosphere concentration: (upper, A0, A1, A2, A3, A4, decade). */
private val UPPER_CONCENTRATION = arrayOf(
    doubleArrayOf(150000.0, 2.10005867e03, -5.618444757e-02, 5.663986231e-07,
        -2.547466858e-12, 4.309844119e-18, 17),
    doubleArrayOf(200000.0, 1.016393700e03, -2.119530830e-02, 1.671627815e-07,
        -5.894237068e-13, 7.826684089e-19, 16),
    doubleArrayOf(250000.0, 7.631575e02, -1.150600844e-02, 6.612598428e-08,
        -1.708736137e-13, 1.669823114e-19, 15),
    doubleArrayOf(350000.0, 1.882203e02, -2.265999519e-03, 1.041726141e-08,
        -2.155574922e-14, 1.687430962e-20, 15),
    doubleArrayOf(450000.0, 2.804823e02, -2.432231125e-03, 8.055024663e-09,
        -1.202418519e-14, 6.805101379e-21, 14),
    doubleArrayOf(600000.0, 5.599362e02, -3.714141392e-03, 9.358870345e-09,
        -1.058591881e-14, 4.525531532e-21, 13),
    doubleArrayOf(800000.0, 8.358756e02, -4.265393073e-03, 8.252842085e-09,
        -7.150127437e-15, 2.335744331e-21, 12),
    doubleArrayOf(1000000.0, 8.364965e01, -3.162492458e-04, 4.602064246e-10,
        -3.021858469e-16, 7.512304301e-23, 12),
    doubleArrayOf(1200000.0, 3.83220e01, -5.0980e-05, 1.81e-11, 0.0, 0.0, 11),
)

/** Molar mass at the 97.5 km joint, g/mol. */
private const val MOLAR_MASS_97500_G = 28.85

object Atmosphere {

    /** Return [height] as a valid number within the standard range. */
    fun validateHeight(height: Double): Double {
        if (!height.isFinite()) throw HeightError("invalid_number")
        if (height < Constants.MIN_HEIGHT || height > Constants.MAX_HEIGHT) {
            throw HeightError("out_of_range")
        }
        return height
    }

    fun geopotentialHeight(height: Double): Double {
        val h = validateHeight(height)
        return Constants.EARTH_RADIUS * h / (Constants.EARTH_RADIUS + h)
    }

    fun pressure(height: Double): Double {
        val h = validateHeight(height)
        if (h > Constants.UPPER_ATMOSPHERE_START) {
            return Constants.BOLTZMANN * concentration(h) * temperature(h)
        }
        val geopotential = geopotentialHeight(h)
        return Layers.pressureAt(Layers.layerAt(geopotential), geopotential)
    }

    fun temperature(height: Double): Double {
        val h = validateHeight(height)
        if (h > Constants.UPPER_ATMOSPHERE_START) {
            for (row in UPPER_TEMPERATURE) {
                if (row[0] < h && h <= row[1]) {
                    return row[2] + row[3] * (h - row[0])
                }
            }
            return 1000.0 // unreachable within the valid range
        }
        val geopotential = geopotentialHeight(h)
        val molar = Layers.temperatureAt(Layers.layerAt(geopotential), geopotential)
        return molar * molarWeight(h) / Constants.M0
    }

    fun molarWeight(height: Double): Double {
        val h = validateHeight(height)
        val grams = when {
            h <= 94000.0 -> 28.964420
            h <= 97000.0 -> {
                28.82 +
                    0.158 * sqrt(1.0 - 7.5e-08 * (h - 94000.0) * (h - 94000.0)) -
                    2.479e-04 * sqrt(97000.0 - h)
            }
            h <= 97500.0 -> 28.91 - 0.00012 * (h - 97000.0)
            h <= Constants.UPPER_ATMOSPHERE_START -> {
                MOLAR_MASS_97500_G - 0.0001511 * (h - 97500.0)
            }
            else -> {
                var b0 = MOLAR_MASS_POLYNOMIALS.last()[1]
                var b1 = MOLAR_MASS_POLYNOMIALS.last()[2]
                var b2 = MOLAR_MASS_POLYNOMIALS.last()[3]
                var b3 = MOLAR_MASS_POLYNOMIALS.last()[4]
                for (row in MOLAR_MASS_POLYNOMIALS) {
                    if (h <= row[0]) {
                        b0 = row[1]; b1 = row[2]; b2 = row[3]; b3 = row[4]
                        break
                    }
                }
                b0 + b1 * h + b2 * h * h + b3 * h * h * h
            }
        }
        return grams * 1e-3
    }

    fun concentration(height: Double): Double {
        val h = validateHeight(height)
        if (h <= Constants.UPPER_ATMOSPHERE_START) {
            return Constants.CONCENTRATION_FACTOR * pressure(h) / temperature(h)
        }
        for (row in UPPER_CONCENTRATION) {
            if (h <= row[0]) {
                val value = row[1] + row[2] * h + row[3] * h * h +
                    row[4] * h * h * h + row[5] * h * h * h * h
                return value * 10.0.pow(row[6])
            }
        }
        throw HeightError("out_of_range")
    }

    fun density(height: Double): Double =
        pressure(height) * molarWeight(height) / Constants.R / temperature(height)

    fun soundSpeed(height: Double): Double =
        sqrt(Constants.GAMMA * Constants.R * temperature(height) / molarWeight(height))

    fun freePath(height: Double): Double =
        Constants.FREE_PATH_FACTOR * temperature(height) / pressure(height)

    fun dynamicViscosity(height: Double): Double? {
        val h = validateHeight(height)
        if (h > Constants.TRANSPORT_MAX_HEIGHT) return null
        val t = temperature(h)
        return Constants.VISCOSITY_B * t.pow(1.5) / (t + Constants.VISCOSITY_S)
    }

    fun kinematicViscosity(height: Double): Double? {
        val viscosity = dynamicViscosity(height) ?: return null
        return viscosity / density(height)
    }

    fun thermalConductivity(height: Double): Double? {
        val h = validateHeight(height)
        if (h > Constants.TRANSPORT_MAX_HEIGHT) return null
        val t = temperature(h)
        return Constants.CONDUCTIVITY_A * t.pow(1.5) / (
            t + Constants.CONDUCTIVITY_B * 10.0.pow(-12.0 / t)
            )
    }

    /** Compute every parameter at [height] in one call. */
    fun compute(height: Double): AtmosphereState {
        val h = validateHeight(height)
        return AtmosphereState(
            height = h,
            geopotentialHeight = geopotentialHeight(h),
            pressure = pressure(h),
            temperature = temperature(h),
            density = density(h),
            molarMass = molarWeight(h),
            concentration = concentration(h),
            soundSpeed = soundSpeed(h),
            meanFreePath = freePath(h),
            dynamicViscosity = dynamicViscosity(h),
            kinematicViscosity = kinematicViscosity(h),
            thermalConductivity = thermalConductivity(h),
        )
    }
}