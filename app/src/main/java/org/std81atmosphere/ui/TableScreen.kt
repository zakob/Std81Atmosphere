package org.std81atmosphere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.std81atmosphere.model.AtmosphereState
import org.std81atmosphere.model.Format
import org.std81atmosphere.model.I18n
import org.std81atmosphere.model.Table

/**
 * One selectable table column: how its header and cells are rendered.
 * Widths are fixed so values always align under their titles.
 */
private data class ColumnSpec(
    val key: String,
    val headerKey: String,
    val width: Dp,
    val align: Alignment,
    val format: (AtmosphereState) -> String,
)

/** Every quantity the model provides, in display order. */
private val ALL_COLUMNS = listOf(
    ColumnSpec("h", "table_h", 84.dp, Alignment.Center, { Format.number(it.height, 6) }),
    ColumnSpec("p", "table_p", 140.dp, Alignment.CenterEnd, { Format.number(it.pressure, 5) }),
    ColumnSpec("T", "table_t", 84.dp, Alignment.CenterEnd, { Format.number(it.temperature, 5) }),
    ColumnSpec("rho", "table_rho", 108.dp, Alignment.CenterEnd, { Format.number(it.density, 4) }),
    ColumnSpec("M", "table_M", 100.dp, Alignment.CenterEnd, { Format.number(it.molarMass, 5) }),
    ColumnSpec("n", "table_n", 130.dp, Alignment.CenterEnd, { Format.number(it.concentration, 5) }),
    ColumnSpec("a", "table_a", 92.dp, Alignment.CenterEnd, { Format.number(it.soundSpeed, 5) }),
    ColumnSpec("l", "table_l", 104.dp, Alignment.CenterEnd, { Format.number(it.meanFreePath, 4) }),
    ColumnSpec("mu", "table_mu", 96.dp, Alignment.CenterEnd, { Format.number(it.dynamicViscosity, 4) }),
    ColumnSpec("nu", "table_nu", 96.dp, Alignment.CenterEnd, { Format.number(it.kinematicViscosity, 3) }),
    ColumnSpec("W", "table_W", 112.dp, Alignment.CenterEnd, { Format.number(it.thermalConductivity, 4) }),
    ColumnSpec("H", "table_H", 96.dp, Alignment.CenterEnd, { Format.number(it.geopotentialHeight, 5) }),
)

private val DEFAULT_COLUMNS = setOf("h", "p", "T", "rho")

/** Range table screen: build a parameter table and copy it as CSV. */
@Composable
fun TableScreen(modifier: Modifier = Modifier, onMessage: (String) -> Unit = {}) {
    var from by remember { mutableStateOf("0") }
    var to by remember { mutableStateOf("10000") }
    var step by remember { mutableStateOf("1000") }
    var rows by remember { mutableStateOf<List<AtmosphereState>>(emptyList()) }
    var selectedKeys by remember { mutableStateOf(DEFAULT_COLUMNS) }
    val clipboard = LocalClipboardManager.current

    fun build() {
        try {
            rows = Table.buildRows(Table.parse(from, to, step))
        } catch (error: Exception) {
            rows = emptyList()
            onMessage(I18n.tr((error as? org.std81atmosphere.model.TableError)?.code ?: "error_generic"))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(I18n.tr("table_from"), from, { from = it }, Modifier.weight(1f))
            NumberField(I18n.tr("table_to"), to, { to = it }, Modifier.weight(1f))
            NumberField(I18n.tr("table_step"), step, { step = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { build() }, modifier = Modifier.weight(1f)) {
                Text(I18n.tr("table_build"))
            }
            OutlinedButton(
                onClick = {
                    if (rows.isEmpty()) {
                        onMessage(I18n.tr("table_empty"))
                    } else {
                        clipboard.setText(AnnotatedString(Table.toCsv(rows)))
                        onMessage(I18n.tr("table_copied", "count" to rows.size))
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(I18n.tr("table_export"))
            }
        }

        // Column picker: a horizontally scrollable row of toggles.
        Text(I18n.tr("table_columns"), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (column in ALL_COLUMNS) {
                ChipButton(
                    label = I18n.tr(column.headerKey).substringBefore(","),
                    selected = column.key in selectedKeys,
                    onClick = {
                        selectedKeys = if (column.key in selectedKeys) {
                            selectedKeys - column.key
                        } else {
                            selectedKeys + column.key
                        }
                    },
                )
            }
        }

        ResultTable(rows, ALL_COLUMNS.filter { it.key in selectedKeys }, Modifier.weight(1f))
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

/** Small selectable chip used for the column picker (shared with PlotScreen). */

/**
 * The table itself: a fixed-width grid inside a horizontal scroll, with a
 * vertically scrolling body, so many (or wide) columns never overflow.
 */
@Composable
private fun ResultTable(rows: List<AtmosphereState>, columns: List<ColumnSpec>, modifier: Modifier) {
    if (rows.isEmpty()) {
        Text(I18n.tr("table_empty"), style = MaterialTheme.typography.bodyMedium)
        return
    }
    if (columns.isEmpty()) {
        Text(I18n.tr("table_none"), style = MaterialTheme.typography.bodyMedium)
        return
    }

    val totalWidth = columns.fold(0.dp) { sum, column -> sum + column.width }
    val cellStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val hScroll = rememberScrollState()

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(hScroll)
                .width(totalWidth),
        ) {
            // Header (fixed, does not scroll vertically with the rows).
            Row(Modifier.fillMaxWidth()) {
                for (column in columns) {
                    Text(
                        text = I18n.tr(column.headerKey),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .width(column.width)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(0.5.dp, grid)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }
            // Rows (vertically scrollable).
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows.size) { index ->
                    val state = rows[index]
                    val rowBackground = if (index % 2 == 0) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    Row(Modifier.fillMaxWidth().background(rowBackground)) {
                        for (column in columns) {
                            cell(column, column.format(state), cellStyle, grid)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun cell(column: ColumnSpec, value: String, style: TextStyle, grid: Color) {
    Box(
        modifier = Modifier
            .width(column.width)
            .border(0.5.dp, grid)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        contentAlignment = column.align,
    ) {
        Text(text = value, style = style)
    }
}