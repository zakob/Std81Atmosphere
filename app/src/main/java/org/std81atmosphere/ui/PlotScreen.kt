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

    Canvas(modifier = modifier.fillMaxSize()) {
        if (values.size < 2) return@Canvas

        val left = 52.dp.toPx()
        val bottom = 26.dp.toPx()
        val topPad = 22.dp.toPx()
        val rightPad = 10.dp.toPx()
        val plotWidth = size.width - left - rightPad
        val plotHeight = size.height - bottom - topPad
        if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

        val yValues = values.map { if (parameter.logarithmic) log10(it) else it }
        val yMin = yValues.minOrNull()!!
        val yMax = yValues.maxOrNull()!!
        val y0 = if (yMin == yMax) yMin - 1.0 else yMin
        val y1 = if (yMin == yMax) yMax + 1.0 else yMax

        fun px(x: Double): Float = (left + (x - 0.0) / span * plotWidth).toFloat()
        fun py(y: Double): Float = (size.height - bottom - (y - y0) / (y1 - y0) * plotHeight).toFloat()

        // Grid.
        val gridColor = Color(0xFF2A2E36)
        for (index in 1..3) {
            val y = py(y0 + (y1 - y0) * index / 4)
            drawLine(gridColor, Offset(left, y), Offset(left + plotWidth, y), 1f)
            val x = px(span * index / 4)
            drawLine(gridColor, Offset(x, size.height - bottom), Offset(x, py(y0)), 1f)
        }

        // Axes.
        val axisColor = Color(0xFF8B919C)
        drawLine(axisColor, Offset(left, size.height - bottom), Offset(left + plotWidth, size.height - bottom), 1.5f)
        drawLine(axisColor, Offset(left, size.height - bottom), Offset(left, py(y0)), 1.5f)

        // Curve.
        val path = Path()
        for (index in values.indices) {
            val x = px(samples[index])
            val y = py(yValues[index])
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFFF2B33D), style = Stroke(width = 6f))

        // Y axis labels (5 ticks).
        val tickLabel = rememberTextMeasurer()
        for (index in 0..4) {
            val value = y0 + (y1 - y0) * index / 4
            val shown = if (parameter.logarithmic) 10.0.pow(value) else value
            val label = if (parameter.logarithmic && shown < 1e-3) {
                String.format(java.util.Locale.ROOT, "%1.1e", shown)
            } else {
                String.format(java.util.Locale.ROOT, "%.2g", shown)
            }
            val layout = tickLabel.measure(
                AnnotatedString(label),
                style = TextStyle(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
            )
            val y = py(value)
            drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2))
        }

        // X axis labels.
        val hLabel = I18n.tr("plot_axis_h")
        val hLayout = tickLabel.measure(
            AnnotatedString(hLabel),
            style = TextStyle(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
        )
        drawText(hLayout, topLeft = Offset(left + plotWidth - hLayout.size.width, 2.dp.toPx()))
        for (index in 0..4) {
            val km = (span * index / 4 / 1000).roundToInt()
            val label = km.toString()
            val layout = tickLabel.measure(
                AnnotatedString(label),
                style = TextStyle(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
            )
            drawText(layout, topLeft = Offset(px(span * index / 4) - layout.size.width / 2, size.height - 20.dp.toPx()))
        }
    }
}