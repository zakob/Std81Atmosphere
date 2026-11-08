package org.std81atmosphere.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Tests for the range table, CSV export and formatting (JVM, no Android). */
class ModelExtrasTest {

    @Test
    fun parseValidRequest() {
        val request = Table.parse("0", "1000", "100")
        assertEquals(11, request.count)
        assertEquals(0.0, request.start, 0.0)
        assertEquals(1000.0, request.stop, 0.0)
        assertEquals(100.0, request.step, 0.0)
    }

    @Test
    fun parseCommaDecimals() {
        val request = Table.parse("0", "1000,5", "500,25")
        assertEquals(500.25, request.step, 0.0)
        assertEquals(3, request.count)
    }

    @Test
    fun parseErrors() {
        expectError("table_bad_number") { Table.parse("abc", "1000", "100") }
        expectError("table_bad_step") { Table.parse("0", "1000", "0") }
        expectError("table_bad_order") { Table.parse("1000", "0", "100") }
        expectError("table_too_many") { Table.parse("0", "1000000", "100") }
    }

    @Test
    fun buildRowsEdges() {
        val rows = Table.buildRows(Table.parse("0", "1000", "100"))
        assertEquals(11, rows.size)
        assertEquals(0.0, rows.first().height, 0.0)
        assertEquals(1000.0, rows.last().height, 0.0)
    }

    @Test
    fun csvExport() {
        val rows = Table.buildRows(Table.parse("0", "2000", "1000"))
        val csv = Table.toCsv(rows)
        val lines = csv.split("\n")
        assertEquals(4, lines.size) // header + 0, 1000, 2000
        assertEquals(12, lines[0].split(";").size)
        assertTrue(lines[0].startsWith("h,m;T,K;p,Pa"))
        assertEquals(12, lines[1].split(";").size)
    }

    @Test
    fun csvTransportCellsAreEmptyAbove90Km() {
        val rows = Table.buildRows(Table.parse("100000", "102000", "1000"))
        val fields = Table.toCsv(rows).split("\n")[1].split(";")
        assertEquals("", fields[8]) // dynamic viscosity
        assertEquals("", fields[9]) // kinematic viscosity
        assertEquals("", fields[10]) // thermal conductivity
    }

    @Test
    fun formatNumber() {
        assertEquals("0.0289644", Format.number(0.02896442, 6))
        assertEquals("101325", Format.number(101325.0, 6))
        assertEquals("19399.4", Format.number(19399.4, 6))
        assertEquals("0.0319", Format.number(0.03185253, 3))
        assertEquals("0", Format.number(0.0))
        assertEquals("\u2014", Format.number(null))
    }

    @Test
    fun translationsAreConsistent() {
        val ru = I18n.strings[I18n.Language.RU].orEmpty()
        val en = I18n.strings[I18n.Language.EN].orEmpty()
        assertEquals(ru.keys, en.keys)
        for ((key, value) in ru) {
            assertTrue("empty ru string for $key", value.isNotBlank())
        }
    }

    @Test
    fun translationPlaceholders() {
        I18n.current = I18n.Language.RU
        assertTrue(I18n.tr("table_copied", "count" to 7).contains("7"))
        I18n.current = I18n.Language.EN
        assertTrue(I18n.tr("table_copied", "count" to 7).contains("7"))
    }

    private inline fun expectError(code: String, block: () -> Unit) {
        try {
            block()
            fail("expected TableError($code)")
        } catch (error: TableError) {
            assertEquals(code, error.code)
        }
    }
}