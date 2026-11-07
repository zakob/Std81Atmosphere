"""Physical constants of the GOST 4401-81 standard atmosphere.

The values follow the standard itself (appendix "Основные положения и формулы"):
the specific gas constant is R = 287.05287 J/(kg*K), which corresponds to
R* = 8.31432 J/(mol*K) at M0 = 28.96442 g/mol.
"""

# Universal gas constant, J/(mol*K).
R = 8.31432
# Standard acceleration of gravity, m/s^2.
G0 = 9.80665
# Molar mass of dry air at sea level, kg/mol.
M0 = 0.02896442
# Conventional Earth radius used for geopotential height, m.
EARTH_RADIUS = 6356767.0
# Ratio of specific heats used for the speed of sound.
GAMMA = 1.4

# 1 mm Hg in Pa.
MMHG = 133.322387415

# Factor of the concentration formula n = 7.243611e22 * p / T, 1/(m^3*K/Pa).
CONCENTRATION_FACTOR = 7.243611e22
# Effective Boltzmann constant implied by the formula above, J/K.
BOLTZMANN = 1e-22 / 7.243611
# Mean free path: l = 2.332376e-5 * T / p, m*Pa/K.
FREE_PATH_FACTOR = 2.332376e-05

# Sutherland coefficients for dynamic viscosity: mu = b*T^1.5/(T + s).
VISCOSITY_B = 1.458e-06
VISCOSITY_S = 110.4
# Sutherland coefficients for thermal conductivity.
CONDUCTIVITY_A = 2.648151e-03
CONDUCTIVITY_B = 245.4

# Valid height range of the standard, m.
MIN_HEIGHT = -2000.0
MAX_HEIGHT = 1_200_000.0
# Above this geometric height the standard uses the upper-atmosphere model.
UPPER_ATMOSPHERE_START = 120_000.0
# Molecular transport properties are only defined below this height.
TRANSPORT_MAX_HEIGHT = 90_000.0
