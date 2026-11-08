package org.std81atmosphere.model

/**
 * Physical constants of the GOST 4401-81 standard atmosphere.
 *
 * The values follow the standard itself: R = 287.05287 J/(kg*K), which
 * corresponds to R* = 8.31432 J/(mol*K) at M0 = 28.96442 g/mol.
 */
object Constants {
    /** Universal gas constant, J/(mol*K). */
    const val R = 8.31432

    /** Standard acceleration of gravity, m/s^2. */
    const val G0 = 9.80665

    /** Molar mass of dry air at sea level, kg/mol. */
    const val M0 = 0.02896442

    /** Conventional Earth radius used for the geopotential height, m. */
    const val EARTH_RADIUS = 6356767.0

    /** Ratio of specific heats used for the speed of sound. */
    const val GAMMA = 1.4

    /** Factor of the concentration formula n = 7.243611e22 * p / T. */
    const val CONCENTRATION_FACTOR = 7.243611e22

    /** Mean free path: l = 2.332376e-5 * T / p. */
    const val FREE_PATH_FACTOR = 2.332376e-05

    /** Sutherland coefficients for the dynamic viscosity. */
    const val VISCOSITY_B = 1.458e-06
    const val VISCOSITY_S = 110.4

    /** Sutherland coefficients for the thermal conductivity. */
    const val CONDUCTIVITY_A = 2.648151e-03
    const val CONDUCTIVITY_B = 245.4

    /** Valid height range of the standard, m. */
    const val MIN_HEIGHT = -2000.0
    const val MAX_HEIGHT = 1_200_000.0

    /** Above this geometric height the standard uses the upper-atmosphere model. */
    const val UPPER_ATMOSPHERE_START = 120_000.0

    /** Molecular transport properties are only defined below this height. */
    const val TRANSPORT_MAX_HEIGHT = 90_000.0

    /** Effective Boltzmann constant implied by the concentration formula, J/K. */
    val BOLTZMANN: Double = 1e-22 / 7.243611
}