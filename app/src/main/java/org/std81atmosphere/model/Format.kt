package org.std81atmosphere.model

import kotlin.math.floor
import kotlin.math.log10

/** Number formatting shared by the calculation and table screens. */
object Format {

    /**
     * Format [value] with [digits] significant digits.
     *
     * Values below 10^-3 and above 10^6 are shown in scientific notation so
     * that small pressures and the molar mass keep their significant digits.
     * A null value (transport properties above 90 km) becomes an em dash.
     */
    fun number(value: Double?, digits: Int = 5): String {
        if (value == null) return "\u2014"
        if (value == 0.0) return "0"
        val exponent = floor(log10(kotlin.math.abs(value)))
        if (exponent < -3 || exponent >= 6) {
            val precision = (digits - 1).coerceAtLeast(1)
            return String.format(java.util.Locale.ROOT, "%." + precision + "e", value)
        }
        val decimals = (digits - 1 - exponent.toInt()).coerceAtLeast(0)
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value)
    }
}