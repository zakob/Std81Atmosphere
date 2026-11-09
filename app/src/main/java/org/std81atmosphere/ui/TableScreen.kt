package org.std81atmosphere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import org.std81atmosphere.model.AtmosphereState
import org.std81atmosphere.model.Format
import org.std81atmosphere.model.I18n
import org.std81atmosphere.model.Table

/** Range table screen: build a parameter table and copy it as CSV. */
@Composable
fun TableScreen(modifier: Modifier = Modifier, onMessage: (String) -> Unit = {}) {
    var from by remember { mutableStateOf("0") }
    var to by remember { mutableStateOf("10000") }
    var step by remember { mutableStateOf("1000") }
    var rows by remember { mutableStateOf<List<AtmosphereState>>(emptyList()) }
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
        ResultTable(rows)
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

@Composable
private fun ResultTable(rows: List<AtmosphereState>) {
    if (rows.isEmpty()) {
        Text(I18n.tr("table_empty"), style = MaterialTheme.typography.bodyMedium)
        return
    }
    // Fixed column widths shared by the header and the data rows, so values
    // stay aligned under their column titles no matter the content.
    val columns = listOf(
        ColumnSpec(76.dp, Alignment.CenterHorizontally, I18n.tr("table_h")),
        ColumnSpec(128.dp, Alignment.CenterEnd, I18n.tr("table_p")),
        ColumnSpec(76.dp, Alignment.CenterEnd, I18n.tr("table_t")),
        ColumnSpec(104.dp, Alignment.CenterEnd, I18n.tr("table_rho")),
    )
    val cellStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    val grid = MaterialTheme.colorScheme.outlineVariant

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth()) {
                for (column in columns) {
                    Text(
                        text = column.title,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(column.width)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(0.5.dp, grid)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }
        }
        items(rows.size) { index ->
            val state = rows[index]
            val rowBackground = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
            Row(Modifier.fillMaxWidth().background(rowBackground)) {
                cell(columns[0], Format.number(state.height, 6), cellStyle, grid)
                cell(columns[1], Format.number(state.pressure, 5), cellStyle, grid)
                cell(columns[2], Format.number(state.temperature, 5), cellStyle, grid)
                cell(columns[3], Format.number(state.density, 4), cellStyle, grid)
            }
        }
    }
}

private data class ColumnSpec(val width: Dp, val align: Alignment, val title: String)

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