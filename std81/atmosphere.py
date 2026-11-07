"""GOST 4401-81 standard atmosphere model.

All public functions take *geometric* height in metres and return SI units.
Heights outside [-2000, 1 200 000] m and non-finite values raise ``ValueError``.
"""

from dataclasses import dataclass
from math import isfinite, sqrt
from typing import Optional, Tuple

from .constants import (
    BOLTZMANN,
    CONCENTRATION_FACTOR,
    CONDUCTIVITY_A,
    CONDUCTIVITY_B,
    EARTH_RADIUS,
    FREE_PATH_FACTOR,
    GAMMA,
    M0,
    MAX_HEIGHT,
    MIN_HEIGHT,
    TRANSPORT_MAX_HEIGHT,
    UPPER_ATMOSPHERE_START,
    VISCOSITY_B,
    VISCOSITY_S,
    R,
)
from .layers import layer_at, pressure_at, temperature_at

MESSAGES = {
    'invalid_number': 'Please, enter height in meters.',
    'out_of_range': 'Valid height range is\n[-2000; 1200000]\nin meter.',
}
INVALID_NUMBER_MESSAGE = MESSAGES['invalid_number']
OUT_OF_RANGE_MESSAGE = MESSAGES['out_of_range']


class HeightError(ValueError):
    """Invalid height input.

    ``code`` is a stable identifier ("invalid_number" or "out_of_range") that
    the UI can translate.
    """

    def __init__(self, code: str) -> None:
        self.code = code
        super().__init__(MESSAGES[code])

# Upper atmosphere: (lower boundary, upper boundary, temperature, gradient).
_UPPER_TEMPERATURE: Tuple[Tuple[float, float, float, float], ...] = (
    (120000.0, 140000.0, 334.42, 0.011259),
    (140000.0, 160000.0, 559.60, 0.006800),
    (160000.0, 200000.0, 695.60, 0.003970),
    (200000.0, 250000.0, 854.40, 0.001750),
    (250000.0, 325000.0, 941.90, 0.000570),
    (325000.0, 400000.0, 984.65, 0.000150),
    (400000.0, 600000.0, 995.90, 0.000020),
    (600000.0, 800000.0, 999.90, 0.0000005),
    (800000.0, 1200000.0, 1000.00, 0.0),
)

# Upper atmosphere concentration polynomials (appendix, table 97):
# (upper boundary, (A0, A1, A2, A3, A4, decade)).
_UPPER_CONCENTRATION: Tuple[Tuple[float, Tuple[float, ...]], ...] = (
    (150000.0, (2.10005867e03, -5.618444757e-02, 5.663986231e-07,
                -2.547466858e-12, 4.309844119e-18, 17)),
    (200000.0, (1.016393700e03, -2.119530830e-02, 1.671627815e-07,
                -5.894237068e-13, 7.826684089e-19, 16)),
    (250000.0, (7.631575e02, -1.150600844e-02, 6.612598428e-08,
                -1.708736137e-13, 1.669823114e-19, 15)),
    (350000.0, (1.882203e02, -2.265999519e-03, 1.041726141e-08,
                -2.155574922e-14, 1.687430962e-20, 15)),
    (450000.0, (2.804823e02, -2.432231125e-03, 8.055024663e-09,
                -1.202418519e-14, 6.805101379e-21, 14)),
    (600000.0, (5.599362e02, -3.714141392e-03, 9.358870345e-09,
                -1.058591881e-14, 4.525531532e-21, 13)),
    (800000.0, (8.358756e02, -4.265393073e-03, 8.252842085e-09,
                -7.150127437e-15, 2.335744331e-21, 12)),
    (1000000.0, (8.364965e01, -3.162492458e-04, 4.602064246e-10,
                 -3.021858469e-16, 7.512304301e-23, 12)),
    (1200000.0, (3.83220e01, -5.0980e-05, 1.81e-11, 0.0, 0.0, 11)),
)

# Molar mass polynomials above 120 km (appendix, table 93):
# (upper boundary, (B0, B1, B2, B3)) in g/mol.
_MOLAR_MASS_POLYNOMIALS: Tuple[Tuple[float, Tuple[float, float, float, float]], ...] = (
    (250000.0, (46.9083, -29.71210e-05, 12.08693e-10, -1.85675e-15)),
    (400000.0, (40.4668, -15.52722e-05, 3.55735e-10, -3.02340e-16)),
    (650000.0, (6.3770, 6.25497e-05, -1.10144e-10, 3.36907e-17)),
    (900000.0, (75.6896, -17.61243e-05, 1.33603e-10, -2.87884e-17)),
    (1050000.0, (112.4838, -30.68086e-05, 2.90329e-10, -9.20616e-17)),
    (1200000.0, (9.89704838, -1.19732e-05, 7.78247e-12, -1.77541e-18)),
)

# Molar mass at the 97.5 km joint, g/mol (from the 97-97.5 km formula).
_MOLAR_MASS_97500_G = 28.85


def validate_height(height: float) -> float:
    """Return ``height`` as a float, checking that it is a valid height."""
    try:
        value = float(height)
    except (TypeError, ValueError):
        raise HeightError('invalid_number') from None
    if not isfinite(value):
        raise HeightError('invalid_number')
    if not MIN_HEIGHT <= value <= MAX_HEIGHT:
        raise HeightError('out_of_range')
    return value


def geopotential_height(height: float) -> float:
    """Geopotential height [m] for a geometric height [m]."""
    height = validate_height(height)
    return EARTH_RADIUS * height / (EARTH_RADIUS + height)


def pressure(height: float) -> float:
    """Pressure [Pa] at a geometric height [m]."""
    height = validate_height(height)
    if height > UPPER_ATMOSPHERE_START:
        return BOLTZMANN * concentration(height) * temperature(height)
    geopotential = geopotential_height(height)
    return pressure_at(layer_at(geopotential), geopotential)


def temperature(height: float) -> float:
    """Temperature [K] at a geometric height [m]."""
    height = validate_height(height)
    if height > UPPER_ATMOSPHERE_START:
        for lower, upper, base, gradient in _UPPER_TEMPERATURE:
            if lower < height <= upper:
                return base + gradient * (height - lower)
        return 1000.0  # pragma: no cover - unreachable within the valid range
    geopotential = geopotential_height(height)
    molar = temperature_at(layer_at(geopotential), geopotential)
    return molar * molar_weight(height) / M0


def molar_weight(height: float) -> float:
    """Molar mass of air [kg/mol] at a geometric height [m]."""
    height = validate_height(height)
    if height <= 94000.0:
        return M0
    if height <= 97000.0:
        moles = (
            28.82
            + 0.158 * sqrt(1.0 - 7.5e-08 * (height - 94000.0) ** 2)
            - 2.479e-04 * sqrt(97000.0 - height)
        )
    elif height <= 97500.0:
        moles = 28.91 - 0.00012 * (height - 97000.0)
    elif height <= UPPER_ATMOSPHERE_START:
        moles = _MOLAR_MASS_97500_G - 0.0001511 * (height - 97500.0)
    else:
        polynomials = _MOLAR_MASS_POLYNOMIALS
        for upper, (b0, b1, b2, b3) in polynomials:
            if height <= upper:
                break
        else:
            b0, b1, b2, b3 = polynomials[-1][1]
        moles = b0 + b1 * height + b2 * height ** 2 + b3 * height ** 3
    return moles * 1e-3


def concentration(height: float) -> float:
    """Particle concentration [m^-3] at a geometric height [m]."""
    height = validate_height(height)
    if height <= UPPER_ATMOSPHERE_START:
        return CONCENTRATION_FACTOR * pressure(height) / temperature(height)
    for upper, coefficients in _UPPER_CONCENTRATION:
        if height <= upper:
            a0, a1, a2, a3, a4, decade = coefficients
            value = a0 + a1 * height + a2 * height ** 2 + a3 * height ** 3 + a4 * height ** 4
            return value * 10.0 ** decade
    raise ValueError(OUT_OF_RANGE_MESSAGE)  # pragma: no cover - guarded by validate_height


def density(height: float) -> float:
    """Density [kg/m^3] at a geometric height [m]."""
    return pressure(height) * molar_weight(height) / R / temperature(height)


def sound_speed(height: float) -> float:
    """Speed of sound [m/s] at a geometric height [m]."""
    return sqrt(GAMMA * R * temperature(height) / molar_weight(height))


def free_path(height: float) -> float:
    """Mean free path of air molecules [m] at a geometric height [m]."""
    return FREE_PATH_FACTOR * temperature(height) / pressure(height)


def dynamic_viscosity(height: float) -> Optional[float]:
    """Dynamic viscosity [Pa*s] or ``None`` above 90 km."""
    height = validate_height(height)
    if height > TRANSPORT_MAX_HEIGHT:
        return None
    temperature_value = temperature(height)
    return VISCOSITY_B * temperature_value ** 1.5 / (temperature_value + VISCOSITY_S)


def kinematic_viscosity(height: float) -> Optional[float]:
    """Kinematic viscosity [m^2/s] or ``None`` above 90 km."""
    viscosity = dynamic_viscosity(height)
    if viscosity is None:
        return None
    return viscosity / density(height)


def thermal_conductivity(height: float) -> Optional[float]:
    """Thermal conductivity [W/(m*K)] or ``None`` above 90 km."""
    height = validate_height(height)
    if height > TRANSPORT_MAX_HEIGHT:
        return None
    temperature_value = temperature(height)
    return CONDUCTIVITY_A * temperature_value ** 1.5 / (
        temperature_value + CONDUCTIVITY_B * 10.0 ** (-12.0 / temperature_value)
    )


@dataclass(frozen=True)
class AtmosphereState:
    """All standard-atmosphere parameters at one height."""

    height: float
    geopotential_height: float
    pressure: float
    temperature: float
    density: float
    molar_mass: float
    concentration: float
    sound_speed: float
    mean_free_path: float
    dynamic_viscosity: Optional[float]
    kinematic_viscosity: Optional[float]
    thermal_conductivity: Optional[float]


def compute(height: float) -> AtmosphereState:
    """Compute all parameters at ``height`` in one call."""
    value = validate_height(height)
    return AtmosphereState(
        height=value,
        geopotential_height=geopotential_height(value),
        pressure=pressure(value),
        temperature=temperature(value),
        density=density(value),
        molar_mass=molar_weight(value),
        concentration=concentration(value),
        sound_speed=sound_speed(value),
        mean_free_path=free_path(value),
        dynamic_viscosity=dynamic_viscosity(value),
        kinematic_viscosity=kinematic_viscosity(value),
        thermal_conductivity=thermal_conductivity(value),
    )
