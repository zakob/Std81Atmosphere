"""Presentation of atmosphere parameters.

The model returns SI floats; this module knows how to turn them into strings
and which units and symbols to show.  Units are translated with
:mod:`std81.i18n`.  UI-specific markup (Kivy, HTML, ...) is applied by the
caller, not here.
"""

from dataclasses import dataclass
from math import floor, log10
from typing import Optional, Tuple

from . import i18n
from .atmosphere import AtmosphereState


@dataclass(frozen=True)
class Parameter:
    """A parameter shown to the user."""

    symbol: str
    attribute: str
    unit_key: str
    digits: int = 5


PARAMETERS: Tuple[Parameter, ...] = (
    Parameter('P', 'pressure', 'unit_pa', 6),
    Parameter('T', 'temperature', 'unit_k'),
    Parameter('rho', 'density', 'unit_kg_m3'),
    Parameter('M', 'molar_mass', 'unit_kg_mol'),
    Parameter('n', 'concentration', 'unit_m3'),
    Parameter('a', 'sound_speed', 'unit_m_s'),
    Parameter('l', 'mean_free_path', 'unit_m'),
    Parameter('mu', 'dynamic_viscosity', 'unit_pa_s'),
    Parameter('nu', 'kinematic_viscosity', 'unit_m2_s'),
    Parameter('W', 'thermal_conductivity', 'unit_w_mk'),
    Parameter('H', 'geopotential_height', 'unit_m'),
)

Row = Tuple[str, str, str]


def format_number(value: Optional[float], digits: int = 5) -> str:
    """Format ``value`` with ``digits`` significant digits.

    Values below 10^-3 and above 10^6 are shown in scientific notation, so that
    small pressures and the molar mass keep their significant digits.
    """
    if value is None:
        return '\u2014'
    if value == 0:
        return '0'
    exponent = floor(log10(abs(value)))
    if exponent < -3 or exponent >= 6:
        return f'{value:.{digits - 1}e}'
    decimals = max(0, digits - 1 - exponent)
    return f'{value:.{decimals}f}'


def format_state(state: AtmosphereState) -> Tuple[Row, ...]:
    """Turn a computed state into ``(symbol, value, unit)`` rows."""
    return tuple(
        (
            parameter.symbol,
            format_number(getattr(state, parameter.attribute), parameter.digits),
            i18n.tr(parameter.unit_key),
        )
        for parameter in PARAMETERS
    )
