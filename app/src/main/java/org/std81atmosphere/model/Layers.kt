package org.std81atmosphere.model

import kotlin.math.exp
import kotlin.math.pow

/**
 * One layer of the standard atmosphere: the lower boundary is given in
 * geopotential height, ``pressure`` is the base pressure at that boundary.
 */
data class Layer(
    val height: Double,
    val temperature: Double,
    val gradient: Double,
    val pressure: Double,
)

/**
 * Layer table of GOST 4401-81. A single source of truth for both pressure and
 * temperature, so the two can never drift apart.
 */
object Layers {
    /** Pressure inside [layer] at geopotential [height] (barometric formula). */
    fun pressureAt(layer: Layer, height: Double): Double {
        val delta = height - layer.height
        if (layer.gradient == 0.0) {
            return layer.pressure * exp(-Constants.G0 * Constants.M0 * delta /
                (Constants.R * layer.temperature))
        }
        val t = layer.temperature + layer.gradient * delta
        return layer.pressure * (t / layer.temperature).pow(
            -Constants.G0 * Constants.M0 / (Constants.R * layer.gradient)
        )
    }

    /** Molar temperature inside [layer] at geopotential [height]. */
    fun temperatureAt(layer: Layer, height: Double): Double =
        layer.temperature + layer.gradient * (height - layer.height)

    // (geopotential height, molar temperature, gradient, base pressure).
    // A null base pressure is derived from the previous layer (above 80 km).
    private data class Spec(
        val height: Double,
        val temperature: Double,
        val gradient: Double,
        val pressure: Double? = null,
    )

    private val specs = listOf(
        Spec(-2000.0, 301.15, -0.0065, 127774.0),
        Spec(0.0, 288.15, -0.0065, 101325.0),
        Spec(11000.0, 216.65, 0.0, 22632.0),
        Spec(20000.0, 216.65, 0.0010, 5474.87),
        Spec(32000.0, 228.65, 0.0028, 868.014),
        Spec(47000.0, 270.65, 0.0, 110.906),
        Spec(51000.0, 270.65, -0.0028, 66.9384),
        Spec(71000.0, 214.65, -0.0020, 3.95639),
        Spec(85000.0, 186.65, 0.0, null),
        Spec(94000.0, 186.65, 0.0030, null),
        Spec(102450.0, 212.00, 0.0110, null),
    )

    val layers: List<Layer> = buildList {
        for (spec in specs) {
            val pressure = spec.pressure ?: pressureAt(last(), spec.height)
            add(Layer(spec.height, spec.temperature, spec.gradient, pressure))
        }
    }

    private val heights = layers.map { it.height }

    /** Return the layer that contains the given geopotential height. */
    fun layerAt(geopotentialHeight: Double): Layer {
        var index = 0
        while (index < heights.size - 1 && geopotentialHeight >= heights[index + 1]) {
            index++
        }
        return layers[index]
    }
}