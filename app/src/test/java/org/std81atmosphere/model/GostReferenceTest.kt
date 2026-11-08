package org.std81atmosphere.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Golden tests against the tables of GOST 4401-81.
 *
 * The reference data lives in `resources/gost_4401_81.csv` (1410 rows) and was
 * extracted from tables 1-4 of the standard (see the repository README for
 * provenance: meganorm.ru text of GOST 4401-81, rows validated for internal
 * consistency).  Tolerances reflect the accuracy of the source tables, not the
 * model.
 */
class GostReferenceTest {

    private val reference: List<GostRow> by lazy { loadRows() }

    private class GostRow(
        val h: Double,
        val t: Double?, val p: Double?, val rho: Double?, val a: Double?,
        val mu: Double?, val nu: Double?, val w: Double?, val n: Double?,
        val lambda: Double?, val m: Double?, val hg: Double?,
    )

    private fun loadRows(): List<GostRow> {
        val stream = javaClass.classLoader!!.getResourceAsStream("gost_4401_81.csv")
            ?: error("gost_4401_81.csv not found in test resources")
        val text = stream.bufferedReader().readText()
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .drop(1) // header
            .map { line ->
                val fields = line.split(',')
                fun cell(index: Int): Double? =
                    fields.getOrNull(index)?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                GostRow(
                    h = fields[0].toDouble(),
                    t = cell(2), p = cell(3), rho = cell(4), a = cell(5),
                    mu = cell(6), nu = cell(7), w = cell(8), n = cell(9),
                    lambda = cell(10), m = cell(11), hg = cell(1),
                )
            }
            .toList()
    }

    private fun assertWithin(field: String, height: Double, actual: Double, expected: Double, rtol: Double) {
        val relative = abs(actual - expected) / abs(expected)
        assertTrue(
            "$field($height): actual=$actual expected=$expected relative=$relative > $rtol",
            relative <= rtol,
        )
    }

    private fun check(key: String, fn: (Double) -> Double?, rtol: Double) {
        var checked = 0
        for (row in reference) {
            val expected = when (key) {
                "T" -> row.t
                "p" -> row.p
                "rho" -> row.rho
                "a" -> row.a
                "mu" -> row.mu
                "nu" -> row.nu
                "W" -> row.w
                "n" -> row.n
                "lambda" -> row.lambda
                "M" -> row.m
                "H" -> row.hg
                else -> error("unknown key $key")
            } ?: continue
            if (key == "H" && expected == 0.0) {
                assertTrue("H(0) should be ~0", abs(fn(row.h)!!) < 1e-6)
                checked++
                continue
            }
            val actual = fn(row.h)
            assertTrue("$key(${row.h}) is null", actual != null)
            assertWithin(key, row.h, actual!!, expected, rtol)
            checked++
        }
        assertTrue("no reference values for $key", checked > 0)
    }

    @Test
    fun temperature() = check("T", { Atmosphere.temperature(it) }, 1e-4)

    @Test
    fun pressure() = check("p", { Atmosphere.pressure(it) }, 1e-3)

    @Test
    fun density() = check("rho", { Atmosphere.density(it) }, 1e-3)

    @Test
    fun soundSpeed() = check("a", { Atmosphere.soundSpeed(it) }, 1e-4)

    @Test
    fun dynamicViscosity() = check("mu", { Atmosphere.dynamicViscosity(it) }, 1e-4)

    @Test
    fun kinematicViscosity() = check("nu", { Atmosphere.kinematicViscosity(it) }, 2e-3)

    @Test
    fun thermalConductivity() = check("W", { Atmosphere.thermalConductivity(it) }, 5e-4)

    @Test
    fun concentration() = check("n", { Atmosphere.concentration(it) }, 1e-3)

    @Test
    fun meanFreePath() = check("lambda", { Atmosphere.freePath(it) }, 1e-3)

    @Test
    fun molarWeight() = check("M", { Atmosphere.molarWeight(it) }, 2e-3)

    @Test
    fun geopotentialHeight() = check("H", { Atmosphere.geopotentialHeight(it) }, 5e-4)

    @Test
    fun heightRangeCoverage() {
        val heights = reference.map { it.h }
        assertTrue(heights.minOrNull()!! <= -2000.0)
        assertTrue(heights.maxOrNull()!! >= 1_200_000.0)
    }

    @Test
    fun seaLevelPressureIsExactlyTabulated() {
        // The model anchors the first layer at 101325 Pa.
        assertEquals(101325.0, Atmosphere.pressure(0.0), 1e-9)
    }
}