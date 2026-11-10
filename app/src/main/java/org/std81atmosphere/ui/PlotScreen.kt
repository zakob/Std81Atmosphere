package org.std81atmosphere.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import org.std81atmosphere.model.Atmosphere
import org.std81atmosphere.model.I18n

/** A plottable quantity: symbol, value extractor, its preferred y scale. */
private data class PlotParameter(
    val symbol: String,
    val extract: (Double) -> Double,
    val defaultLog: Boolean,
    val axisLabelKey: String,
)

private val PARAMETERS = listOf(
    PlotParameter("P", { h -> Atmosphere.pressure(h) }, true, "plot_axis_p"),
    PlotParameter("T", { h -> Atmosphere.temperature(h) }, false, "plot_axis_t"),
    PlotParameter("\u03C1", { h -> Atmosphere.density(h) }, true, "plot_axis_rho"),
    PlotParameter("a", { h -> Atmosphere.soundSpeed(h) }, false, "plot_axis_a"),
)

private val SPANS_KM = listOf(20, 50, 100, 300, 1200)
private const val SAMPLES = 240

/** Plot screen: one parameter over a height range with a y-scale toggle. */
@Composable
fun PlotScreen(modifier: Modifier = Modifier) {
    var parameterIndex by remember { mutableStateOf(0) }
    var spanKm by remember { mutableStateOf(100) }
    // Reset to the parameter's preferred scale when the parameter changes;
    // the user can still toggle it.
    var logScale by remember(PARAMETERS[parameterIndex].symbol) {
        mutableStateOf(PARAMETERS[parameterIndex].defaultLog)
    }
    var rangeMenuOpen by remember { mutableStateOf(false) }

    val parameter = PARAMETERS[parameterIndex]
    val span = spanKm * 1000.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Parameter picker.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for ((index, item) in PARAMETERS.withIndex()) {
                ChipButton(
                    label = item.symbol,
                    selected = index == parameterIndex,
                    onClick = { parameterIndex = index },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Range dropdown + y-scale toggle.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                OutlinedButton(onClick = { rangeMenuOpen = true }) {
                    Text(I18n.tr("plot_range") + ": $spanKm ${I18n.tr("unit_km")}")
                }
                DropdownMenu(expanded = rangeMenuOpen, onDismissRequest = { rangeMenuOpen = false }) {
                    for (km in SPANS_KM) {
                        DropdownMenuItem(
                            text = { Text("$km ${I18n.tr("unit_km")}") },
                            onClick = {
                                spanKm = km
                                rangeMenuOpen = false
                            },
                        )
                    }
                }
            }
            ChipButton(
                label = I18n.tr("plot_scale_linear"),
                selected = !logScale,
                onClick = { logScale = false },
            )
            ChipButton(
                label = I18n.tr("plot_scale_log"),
                selected = logScale,
                onClick = { logScale = true },
            )
        }

        Text(
            I18n.tr(parameter.axisLabelKey),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        AtmospherePlot(
            parameter = parameter,
            span = span,
            logScale = logScale,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

/** Small selectable chip (also used by the table column picker). */
@Composable
internal fun ChipButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.height(36.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.height(36.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AtmospherePlot(
    parameter: PlotParameter,
    span: Double,
    logScale: Boolean,
    modifier: Modifier,
) {
    val samples = remember(parameter.symbol, span) {
        List(SAMPLES) { index -> span * index / (SAMPLES - 1) }
    }
    val values = remember(parameter.symbol, span) { samples.map { parameter.extract(it) } }
    val textMeasurer = rememberTextMeasurer()
    // Resolve theme colors in the composable scope: drawing text without an
    // explicit color uses black, which is invisible on the dark theme.
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val titleColor = MaterialTheme.colorScheme.onSurface
    val curveColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        drawRect(surfaceColor, Offset.Zero, size)

        val left = 82.dp.toPx()
        val right = 14.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = 40.dp.toPx()
        val plotWidth = size.width - left - right
        val plotHeight = size.height - top - bottom
        if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

        val yValues = values.map { if (logScale) log10(it) else it }
        val yMin = yValues.minOrNull()!!
        val yMax = yValues.maxOrNull()!!
        val y0 = if (yMin == yMax) yMin - 1.0 else yMin
        val y1 = if (yMin == yMax) yMax + 1.0 else yMax

        fun px(x: Double): Float = (left + x / span * plotWidth).toFloat()
        fun py(y: Double): Float = (top + (y1 - y) / (y1 - y0) * plotHeight).toFloat()

        val axisColor = Color(0xFF8B919C)
        val gridColor = Color(0xFF4A515C)
        val labelStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = labelColor)
        val titleStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = titleColor)

        // Tick values on the y axis (decimal decades for a log scale).
        val yTicks: List<Double> = if (logScale) {
            val lo = 10.0.pow(y0)
            val hi = 10.0.pow(y1)
            buildList {
                var e = floor(log10(lo)).toInt()
                while (10.0.pow(e) <= hi * 1.001) {
                    val v = 10.0.pow(e)
                    if (v >= lo * 0.999) add(v)
                    e++
                }
            }
        } else {
            niceTicks(y0, y1)
        }
        val xTicks = niceTicks(0.0, span)

        // Grid and axes.
        for (tickValue in yTicks) {
            val y = py(tickValue)
            drawLine(gridColor, Offset(left, y), Offset(left + plotWidth, y), 1f)
            drawLine(axisColor, Offset(left - 4.dp.toPx(), y), Offset(left, y), 1.5f)
        }
        for (tickValue in xTicks) {
            val x = px(tickValue)
            drawLine(gridColor, Offset(x, top), Offset(x, top + plotHeight), 1f)
            drawLine(axisColor, Offset(x, top + plotHeight), Offset(x, top + plotHeight + 4.dp.toPx()), 1.5f)
        }
        drawLine(axisColor, Offset(left, top + plotHeight), Offset(left + plotWidth, top + plotHeight), 1.5f)
        drawLine(axisColor, Offset(left, top + plotHeight), Offset(left, top), 1.5f)

        // Curve as short segments (robust across renderers).
        val strokeWidth = 4.dp.toPx()
        for (index in 1 until values.size) {
            drawLine(
                curveColor,
                Offset(px(samples[index - 1]), py(yValues[index - 1])),
                Offset(px(samples[index]), py(yValues[index])),
                strokeWidth,
            )
        }

        // Y tick labels.
        for (tickValue in yTicks) {
            val label = if (logScale) {
                "10^" + log10(tickValue).roundToInt()
            } else {
                formatTick(tickValue)
            }
            val layout = textMeasurer.measure(AnnotatedString(label), style = labelStyle)
            val y = py(tickValue)
            drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2))
        }

        // X tick labels (km).
        for (tickValue in xTicks) {
            val km = (tickValue / 1000.0).roundToInt()
            val layout = textMeasurer.measure(AnnotatedString(km.toString()), style = labelStyle)
            val x = px(tickValue)
            drawText(layout, topLeft = Offset(x - layout.size.width / 2, top + plotHeight + 6.dp.toPx()))
        }

        // Axis titles.
        val xTitleLayout = textMeasurer.measure(
            AnnotatedString(I18n.tr("plot_axis_h")),
            style = titleStyle,
        )
        drawText(
            xTitleLayout,
            topLeft = Offset(left + (plotWidth - xTitleLayout.size.width) / 2, size.height - xTitleLayout.size.height),
        )
        val yTitleLayout = textMeasurer.measure(
            AnnotatedString(I18n.tr(parameter.axisLabelKey)),
            style = titleStyle,
        )
        val yTitleX = 18.dp.toPx()
        val yTitleY = top + plotHeight / 2
        drawContext.canvas.save()
        drawContext.canvas.rotate(-90f, yTitleX, yTitleY)
        drawText(
            yTitleLayout,
            topLeft = Offset(yTitleX - yTitleLayout.size.width / 2, yTitleY - yTitleLayout.size.height / 2),
        )
        drawContext.canvas.restore()
    }
}

/** Choose ~[target] "nice" tick values spanning [min, max] (monotonic). */
private fun niceTicks(min: Double, max: Double, target: Int = 5): List<Double> {
    if (max <= min || !max.isFinite() || !min.isFinite()) return listOf(min)
    val raw = (max - min) / target
    val magnitude = 10.0.pow(floor(log10(raw)))
    val normalized = raw / magnitude
    val step = when {
        normalized >= 5.0 -> 5.0 * magnitude
        normalized >= 2.0 -> 2.0 * magnitude
        else -> magnitude
    }
    val start = floor(min / step) * step
    val result = mutableListOf<Double>()
    var value = start
    while (value <= max + step * 1e-6) {
        if (value >= min - step * 1e-6) result.add(value)
        value += step
    }
    return result
}

private fun formatTick(value: Double): String {
    val abs = kotlin.math.abs(value)
    return if (abs >= 100_000 || (abs < 0.001 && abs > 0)) {
        String.format(java.util.Locale.ROOT, "%.1e", value)
    } else if (abs >= 100) {
        String.format(java.util.Locale.ROOT, "%.0f", value)
    } else {
        String.format(java.util.Locale.ROOT, "%.2g", value)
    }
}