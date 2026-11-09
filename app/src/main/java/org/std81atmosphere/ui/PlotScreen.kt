package org.std81atmosphere.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withSave
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import org.std81atmosphere.model.Atmosphere
import org.std81atmosphere.model.I18n

private data class PlotParameter(
    val symbol: String,
    val extract: (Double) -> Double,
    val logarithmic: Boolean,
    val axisLabelKey: String,
)

private val PARAMETERS = listOf(
    PlotParameter("P", { h -> Atmosphere.pressure(h) }, true, "plot_axis_p"),
    PlotParameter("T", { h -> Atmosphere.temperature(h) }, false, "plot_axis_t"),
    PlotParameter("\u03C1", { h -> Atmosphere.density(h) }, true, "plot_axis_rho"),
    PlotParameter("a", { h -> Atmosphere.soundSpeed(h) }, false, "plot_axis_a"),
)

private val SPANS_KM = listOf(20, 50, 100, 1200)
private const val SAMPLES = 240

/** Plot screen: one parameter over a height range, drawn with Canvas. */
@Composable
fun PlotScreen(modifier: Modifier = Modifier) {
    var parameterIndex by remember { mutableStateOf(0) }
    var spanKm by remember { mutableStateOf(100) }

    val parameter = PARAMETERS[parameterIndex]
    val span = spanKm * 1000.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for ((index, item) in PARAMETERS.withIndex()) {
                ParameterButton(
                    label = item.symbol,
                    selected = index == parameterIndex,
                    onClick = { parameterIndex = index },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (km in SPANS_KM) {
                ParameterButton(
                    label = "$km",
                    selected = km == spanKm,
                    onClick = { spanKm = km },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            I18n.tr(parameter.axisLabelKey),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        AtmospherePlot(
            parameter = parameter,
            span = span,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ParameterButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun AtmospherePlot(parameter: PlotParameter, span: Double, modifier: Modifier) {
    val samples = remember(parameter.symbol, span) {
        List(SAMPLES) { index -> span * index / (SAMPLES - 1) }
    }
    val values = remember(parameter.symbol, span) { samples.map { parameter.extract(it) } }
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        if (values.size < 2) return@Canvas

        val left = 78.dp.toPx()
        val right = 14.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = 40.dp.toPx()
        val plotWidth = size.width - left - right
        val plotHeight = size.height - top - bottom
        if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

        val yValues = values.map { if (parameter.logarithmic) log10(it) else it }
        val yMin = yValues.minOrNull()!!
        val yMax = yValues.maxOrNull()!!
        val y0 = if (yMin == yMax) yMin - 1.0 else yMin
        val y1 = if (yMin == yMax) yMax + 1.0 else yMax

        fun px(x: Double): Float = (left + (x - 0.0) / span * plotWidth).toFloat()
        fun py(y: Double): Float = (top + (y1 - y) / (y1 - y0) * plotHeight).toFloat()

        val axisColor = Color(0xFF8B919C)
        val gridColor = Color(0xFF2A2E36)
        val labelStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        val titleStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace)

        // Ticks.
        val yTicks: List<Pair<Double, String>> = if (parameter.logarithmic) {
            val lo = 10.0.pow(y0)
            val hi = 10.0.pow(y1)
            buildList {
                var e = floor(log10(lo)).toInt()
                while (10.0.pow(e) <= hi * 1.001) {
                    val v = 10.0.pow(e)
                    if (v >= lo * 0.999) add(v to formatLogTick(v))
                    e++
                }
            }
        } else {
            niceTicks(y0, y1)
        }
        val xTicks = niceTicks(0.0, span)

        // Grid + axes + tick marks.
        for ((value, _) in yTicks) {
            val y = py(value)
            drawLine(gridColor, Offset(left, y), Offset(left + plotWidth, y), 1f)
            drawLine(axisColor, Offset(left - 4.dp.toPx(), y), Offset(left, y), 1.5f)
        }
        for (value in xTicks) {
            val x = px(value)
            drawLine(gridColor, Offset(x, top), Offset(x, top + plotHeight), 1f)
            drawLine(axisColor, Offset(x, top + plotHeight), Offset(x, top + plotHeight + 4.dp.toPx()), 1.5f)
        }
        drawLine(axisColor, Offset(left, top + plotHeight), Offset(left + plotWidth, top + plotHeight), 1.5f)
        drawLine(axisColor, Offset(left, top + plotHeight), Offset(left, top), 1.5f)

        // Curve.
        val path = Path()
        for (index in values.indices) {
            val x = px(samples[index])
            val y = py(yValues[index])
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFFF2B33D), style = Stroke(width = 3.dp.toPx()))

        // Y tick labels.
        for ((value, label) in yTicks) {
            val layout = textMeasurer.measure(AnnotatedString(label), style = labelStyle)
            val y = py(value)
            drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2))
        }

        // X tick labels (km).
        for (value in xTicks) {
            val km = (value / 1000.0).roundToInt()
            val layout = textMeasurer.measure(AnnotatedString(km.toString()), style = labelStyle)
            val x = px(value)
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
        withSave {
            drawContext.canvas.rotate(-90f, yTitleX, yTitleY)
            drawText(
                yTitleLayout,
                topLeft = Offset(yTitleX - yTitleLayout.size.width / 2, yTitleY - yTitleLayout.size.height / 2),
            )
        }
    }
}

private fun formatLogTick(value: Double): String {
    val exponent = log10(value).roundToInt()
    return "10^$exponent"
}

/** Choose ~[target] "nice" tick values spanning [min, max]. */
private fun niceTicks(min: Double, max: Double, target: Int = 5): List<Pair<Double, String>> {
    if (max <= min) return listOf(min to formatTick(min), max to formatTick(max))
    val raw = (max - min) / target
    val magnitude = 10.0.pow(floor(log10(raw)))
    val normalized = raw / magnitude
    val step = when {
        normalized >= 5.0 -> 5.0 * magnitude
        normalized >= 2.0 -> 2.0 * magnitude
        else -> magnitude
    }
    val start = floor(min / step) * step
    val result = mutableListOf<Pair<Double, String>>()
    var value = start
    while (value <= max + step * 1e-6) {
        if (value >= min - step * 1e-6) result.add(value to formatTick(value))
        value += step
    }
    return result
}

private fun formatTick(value: Double): String {
    val abs = kotlin.math.abs(value)
    val label = if (abs >= 100_000 || (abs < 0.001 && abs > 0)) {
        String.format(java.util.Locale.ROOT, "%.1e", value)
    } else if (abs >= 100) {
        String.format(java.util.Locale.ROOT, "%.0f", value)
    } else {
        String.format(java.util.Locale.ROOT, "%.2g", value)
    }
    return label
}