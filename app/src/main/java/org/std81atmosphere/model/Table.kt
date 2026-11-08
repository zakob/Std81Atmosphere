package org.std81atmosphere.model

import kotlin.math.floor

/** Raised when a table request is invalid; [code] is a stable i18n key. */
class TableError(val code: String) : IllegalArgumentException(code)

/** A validated request for a range table. */
data class TableRequest(
    val start: Double,
    val stop: Double,
    val step: Double,
) {
    val count: Int
        get() = floor((stop - start) / step + 1e-9).toInt() + 1

    companion object {
        const val MAX_ROWS = 300
    }
}

/** Range tables and CSV export (pure Kotlin, JVM-testable). */
object Table {

    fun parse(start: String, stop: String, step: String): TableRequest {
        fun toDouble(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()
        val startValue = toDouble(start) ?: throw TableError("table_bad_number")
        val stopValue = toDouble(stop) ?: throw TableError("table_bad_number")
        val stepValue = toDouble(step) ?: throw TableError("table_bad_number")
        if (stepValue <= 0.0) throw TableError("table_bad_step")
        if (startValue >= stopValue) throw TableError("table_bad_order")
        Atmosphere.validateHeight(startValue)
        Atmosphere.validateHeight(stopValue)
        val request = TableRequest(startValue, stopValue, stepValue)
        if (request.count > TableRequest.MAX_ROWS) throw TableError("table_too_many")
        return request
    }

    /** Build the rows; the last row never exceeds [request.stop]. */
    fun buildRows(request: TableRequest): List<AtmosphereState> {
        val rows = mutableListOf<AtmosphereState>()
        for (index in 0 until request.count) {
            val height = request.start + index * request.step
            if (height > request.stop + 1e-9) break
            val rounded = kotlin.math.round(height * 1e6) / 1e6
            rows.add(Atmosphere.compute(rounded))
        }
        return rows
    }

    /** Serialize rows to CSV with ';' as the delimiter (Excel-friendly in ru locale). */
    fun toCsv(rows: List<AtmosphereState>, delimiter: Char = ';'): String {
        val header = listOf(
            "h,m", "T,K", "p,Pa", "rho,kg/m^3", "M,kg/mol", "n,m^-3",
            "a,m/s", "l,m", "mu,Pa*s", "nu,m^2/s", "W,W/(m*K)", "H,m",
        )
        val lines = mutableListOf(header.joinToString(delimiter.toString()))
        for (state in rows) {
            val values: List<Double?> = listOf(
                state.height, state.temperature, state.pressure, state.density,
                state.molarMass, state.concentration, state.soundSpeed, state.meanFreePath,
                state.dynamicViscosity, state.kinematicViscosity,
                state.thermalConductivity, state.geopotentialHeight,
            )
            lines.add(
                values.joinToString(delimiter.toString()) { value ->
                    if (value == null) "" else formatCell(value)
                }
            )
        }
        return lines.joinToString("\n")
    }

    private fun formatCell(value: Double): String =
        String.format(java.util.Locale.ROOT, "%.10g", value)
}