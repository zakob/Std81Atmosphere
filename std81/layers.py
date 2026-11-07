"""Layer table of GOST 4401-81.

The standard defines the atmosphere as a sequence of layers with a linear
temperature gradient.  Layer boundaries are given in *geopotential* height
(table 1 of the appendix); geometric heights are derived from them with
``EARTH_RADIUS``.

This module is the single source of truth for the layer structure: both
pressure and temperature are computed from it, so the two can no longer drift
apart.
"""

from bisect import bisect_right
from dataclasses import dataclass
from math import exp
from typing import Optional, Tuple

from .constants import G0, M0, R


@dataclass(frozen=True)
class Layer:
    """One layer of the standard atmosphere.

    ``height`` is the geopotential height of the lower boundary in metres,
    ``temperature`` is the molar temperature at that boundary in kelvin and
    ``gradient`` is the molar temperature gradient in K/m.  ``pressure`` is the
    pressure at the lower boundary in pascals.
    """

    height: float
    temperature: float
    gradient: float
    pressure: float


def pressure_at(layer: Layer, height: float) -> float:
    """Pressure inside ``layer`` at geopotential ``height`` (barometric formula)."""
    delta = height - layer.height
    if layer.gradient == 0.0:
        return layer.pressure * exp(-G0 * M0 * delta / (R * layer.temperature))
    temperature = layer.temperature + layer.gradient * delta
    return layer.pressure * (temperature / layer.temperature) ** (
        -G0 * M0 / (R * layer.gradient)
    )


def temperature_at(layer: Layer, height: float) -> float:
    """Molar temperature inside ``layer`` at geopotential ``height``."""
    return layer.temperature + layer.gradient * (height - layer.height)


# (geopotential height, molar temperature, gradient, pressure at the boundary).
# ``None`` means "derive from the previous layer", used above 80 km where the
# standard only tabulates values as a function of geometric height.
_SPECS: Tuple[Tuple[float, float, float, Optional[float]], ...] = (
    (-2000.0, 301.15, -0.0065, 127774.0),
    (0.0, 288.15, -0.0065, 101325.0),
    (11000.0, 216.65, 0.0, 22632.0),
    (20000.0, 216.65, 0.0010, 5474.87),
    (32000.0, 228.65, 0.0028, 868.014),
    (47000.0, 270.65, 0.0, 110.906),
    (51000.0, 270.65, -0.0028, 66.9384),
    (71000.0, 214.65, -0.0020, 3.95639),
    (85000.0, 186.65, 0.0, None),
    (94000.0, 186.65, 0.0030, None),
    (102450.0, 212.00, 0.0110, None),
)


def _build_layers() -> Tuple[Layer, ...]:
    layers = []
    for height, temperature, gradient, pressure in _SPECS:
        if pressure is None:
            pressure = pressure_at(layers[-1], height)
        layers.append(Layer(height, temperature, gradient, pressure))
    return tuple(layers)


LAYERS: Tuple[Layer, ...] = _build_layers()
_HEIGHTS: Tuple[float, ...] = tuple(layer.height for layer in LAYERS)


def layer_at(geopotential_height: float) -> Layer:
    """Return the layer that contains ``geopotential_height``."""
    return LAYERS[max(0, bisect_right(_HEIGHTS, geopotential_height) - 1)]
