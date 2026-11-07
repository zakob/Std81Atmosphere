"""GOST 4401-81 standard atmosphere.

The public functions take a geometric height in metres and return SI units:

>>> from std81 import pressure, temperature
>>> round(pressure(0), 1)
101325.0
>>> round(temperature(0), 2)
288.15

Use :func:`compute` to get every parameter in one call and
:func:`std81.formatting.format_state` to present them.
"""

from .api import CalcResult, allcalc
from .atmosphere import (
    AtmosphereState,
    concentration,
    compute,
    density,
    dynamic_viscosity,
    free_path,
    geopotential_height,
    kinematic_viscosity,
    molar_weight,
    pressure,
    sound_speed,
    temperature,
    thermal_conductivity,
    validate_height,
)
from .formatting import PARAMETERS, Parameter, format_number, format_state

__version__ = '0.9'

__all__ = [
    'PARAMETERS',
    'AtmosphereState',
    'CalcResult',
    'Parameter',
    'allcalc',
    'compute',
    'concentration',
    'density',
    'dynamic_viscosity',
    'format_number',
    'format_state',
    'free_path',
    'geopotential_height',
    'kinematic_viscosity',
    'molar_weight',
    'pressure',
    'sound_speed',
    'temperature',
    'thermal_conductivity',
    'validate_height',
]
