"""Golden tests against the published tables of GOST 4401-81.

The reference data lives in ``tests/data/gost_4401_81.csv`` and was extracted
from tables 1-4 of the standard (see ``tests/README.md`` for provenance).

Tolerances reflect the accuracy of the source tables (5-7 significant digits)
and the constants used by the model, not the model's own precision.
"""

import csv
import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

import std81 as A  # noqa: E402

DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'data', 'gost_4401_81.csv')

# The approximations of the appendix are accurate over the tabulated heights.
# The only known artefact is the joint at 120 km, where the polynomial for the
# 120-150 km layer starts ~0.75 % below the barometric branch; it is covered by
# a dedicated test in test_invariants.py.


def load_rows():
    with open(DATA, encoding='utf-8') as stream:
        for raw in csv.DictReader(stream):
            yield {key: (float(value) if value else None) for key, value in raw.items()}


ROWS = list(load_rows())


class GostReferenceTest(unittest.TestCase):
    """Every parameter must match the standard within the stated tolerance."""

    maxDiff = None

    def check(self, key, function, rtol):
        checked = 0
        for row in ROWS:
            expected = row.get(key)
            if expected is None:
                continue
            height = row['h']
            actual = function(height)
            self.assertIsNotNone(
                actual, f'{key}({height}) is None, GOST expects {expected!r}')
            if expected == 0:
                # e.g. geopotential height at sea level
                with self.subTest(parameter=key, height=height):
                    self.assertLessEqual(abs(actual), 1e-6)
                checked += 1
                continue
            relative = abs(actual - expected) / abs(expected)
            with self.subTest(parameter=key, height=height):
                self.assertLessEqual(
                    relative, rtol,
                    f'{key}({height}) = {actual!r}, GOST {expected!r}, '
                    f'relative error {relative:.2e} > {rtol:.0e}')
            checked += 1
        self.assertGreater(checked, 0, f'no reference values for {key}')

    def test_temperature(self):
        self.check('T', A.temperature, rtol=1e-4)

    def test_pressure(self):
        self.check('p', A.pressure, rtol=1e-3)

    def test_density(self):
        self.check('rho', A.density, rtol=1e-3)

    def test_sound_speed(self):
        self.check('a', A.sound_speed, rtol=1e-4)

    def test_dynamic_viscosity(self):
        self.check('mu', A.dynamic_viscosity, rtol=1e-4)

    def test_kinematic_viscosity(self):
        self.check('nu', A.kinematic_viscosity, rtol=2e-3)

    def test_thermal_conductivity(self):
        self.check('W', A.thermal_conductivity, rtol=5e-4)

    def test_concentration(self):
        self.check('n', A.concentration, rtol=1e-3)

    def test_mean_free_path(self):
        self.check('lambda', A.free_path, rtol=1e-3)

    def test_molar_weight(self):
        self.check('M', A.molar_weight, rtol=2e-3)

    def test_geopotential_height(self):
        self.check('H', A.geopotential_height, rtol=5e-4)


class LayerBoundaryTest(unittest.TestCase):
    """Values at the layer boundaries of table 1 of the appendix (table 95)."""

    # geometric height, geopotential height, kinetic temperature, molar mass [kg/mol]
    BOUNDARIES = [
        (0, 0, 288.15, None),
        (11019, 11000, 216.65, None),
        (20063, 20000, 216.65, None),
        (32162, 32000, 228.65, None),
        (47350, 47000, 270.65, None),
        (51412, 51000, 270.65, None),
        (71802, 71000, 214.65, None),
        (86152, 85000, 186.65, None),
        (95411, 94000, 186.65, None),
        (104128, 102450, 203.81, 0.027846),
        (120000, 117777, 334.417, 0.025450),
    ]

    def test_geopotential_height(self):
        for height, expected, _, _ in self.BOUNDARIES:
            with self.subTest(height=height):
                self.assertAlmostEqual(A.geopotential_height(height), expected, delta=1.0)

    def test_temperature(self):
        for height, _, expected, _ in self.BOUNDARIES:
            with self.subTest(height=height):
                actual = A.temperature(height)
                self.assertLessEqual(abs(actual - expected) / expected, 1e-3,
                                     f'temperature({height}) = {actual}, GOST {expected}')

    def test_molar_weight(self):
        for height, _, _, expected in self.BOUNDARIES:
            if expected is None:
                continue
            with self.subTest(height=height):
                actual = A.molar_weight(height)
                self.assertLessEqual(abs(actual - expected) / expected, 1e-3,
                                     f'molar_weight({height}) = {actual}, GOST {expected}')


class FixtureCoverageTest(unittest.TestCase):
    def test_heights_span_the_full_range(self):
        heights = [row['h'] for row in ROWS]
        self.assertLessEqual(min(heights), -2000)
        self.assertGreaterEqual(max(heights), 1_200_000)

    def test_every_parameter_is_covered(self):
        for key in ('T', 'p', 'rho', 'a', 'mu', 'nu', 'W', 'n', 'lambda', 'M', 'H'):
            values = [row[key] for row in ROWS if row.get(key) is not None]
            self.assertGreater(len(values), 100, f'too few reference values for {key}')

    def test_low_and_high_atmosphere_are_both_covered(self):
        low = [row for row in ROWS if row['h'] <= 11_000]
        high = [row for row in ROWS if row['h'] >= 120_000]
        self.assertGreater(len(low), 100)
        self.assertGreater(len(high), 300)


if __name__ == '__main__':
    unittest.main()
