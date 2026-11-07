"""Physical invariants and API contracts that must hold independently of the
reference tables: identities, monotonicity, continuity and input handling."""

import math
import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

import std81 as A  # noqa: E402
from std81.constants import GAMMA, R  # noqa: E402

# Geometric heights of the layer boundaries (table 1 of the appendix).
BREAKPOINTS = [0, 11019, 20063, 32162, 47350, 51412, 71802, 86152, 95411, 104128, 120000]

# The 120 km joint is not closed exactly: the lower branch is barometric, the
# upper one starts with the approximation polynomial for 120-150 km, which is
# ~0.75 % below it at the boundary (the error vanishes by 121 km).
JOINT_120_KM = 120000


def heights(step=1000, start=-2000, stop=1200000):
    h = start
    while h <= stop:
        yield h
        h += step


class IdentityTest(unittest.TestCase):
    def test_ideal_gas(self):
        for h in heights(2500):
            with self.subTest(height=h):
                expected = A.pressure(h) * A.molar_weight(h) / R / A.temperature(h)
                self.assertAlmostEqual(A.density(h) / expected, 1.0, places=12)

    def test_sound_speed(self):
        for h in heights(5000):
            expected = math.sqrt(GAMMA * R * A.temperature(h) / A.molar_weight(h))
            with self.subTest(height=h):
                self.assertAlmostEqual(A.sound_speed(h) / expected, 1.0, places=12)

    def test_kinematic_viscosity(self):
        for h in heights(2500):
            mu = A.dynamic_viscosity(h)
            if mu is None:
                self.assertIsNone(A.kinematic_viscosity(h))
                continue
            with self.subTest(height=h):
                self.assertAlmostEqual(
                    A.kinematic_viscosity(h) / (mu / A.density(h)), 1.0, places=12)

    def test_geopotential_height(self):
        radius = 6356767.0
        for h in heights(10000):
            with self.subTest(height=h):
                self.assertAlmostEqual(
                    A.geopotential_height(h), radius * h / (radius + h), places=6)


class SanityTest(unittest.TestCase):
    def test_pressure_decreases_with_height(self):
        previous = math.inf
        for h in heights(500):
            current = A.pressure(h)
            with self.subTest(height=h):
                self.assertLess(current, previous)
            previous = current

    def test_positive_quantities(self):
        for h in heights(5000):
            with self.subTest(height=h):
                self.assertGreater(A.temperature(h), 0.0)
                self.assertGreater(A.molar_weight(h), 0.0)
                self.assertGreater(A.pressure(h), 0.0)
                self.assertGreater(A.density(h), 0.0)
                self.assertGreater(A.sound_speed(h), 0.0)

    def test_temperature_range(self):
        for h in heights(5000):
            temperature = A.temperature(h)
            with self.subTest(height=h):
                self.assertGreater(temperature, 150.0)
                self.assertLess(temperature, 1100.0)

    def test_molar_mass_range(self):
        for h in heights(5000):
            molar = A.molar_weight(h)
            with self.subTest(height=h):
                self.assertGreater(molar, 0.002)
                self.assertLess(molar, 0.030)


class ContinuityTest(unittest.TestCase):
    EPS = 1e-3

    def assert_continuous(self, function, name, tolerance=5e-4):
        for h in BREAKPOINTS:
            if h == JOINT_120_KM:
                continue
            low = function(h - self.EPS)
            high = function(h + self.EPS)
            with self.subTest(parameter=name, height=h):
                self.assertLessEqual(
                    abs(high - low) / abs(low), tolerance,
                    f'{name} jumps at h={h}: {low!r} -> {high!r}')

    def test_temperature_continuous(self):
        self.assert_continuous(A.temperature, 'temperature')

    def test_pressure_continuous(self):
        self.assert_continuous(A.pressure, 'pressure')

    def test_density_continuous(self):
        self.assert_continuous(A.density, 'density')

    def test_concentration_continuous(self):
        self.assert_continuous(A.concentration, 'concentration')

    def test_molar_weight_continuous(self):
        self.assert_continuous(A.molar_weight, 'molar_weight')

    @unittest.expectedFailure
    def test_pressure_continuous_at_120_km(self):
        """Known issue: the 120 km joint is not closed (~0.75 % step).

        The appendix approximates the 120-150 km layer with a polynomial whose
        value at the lower endpoint differs from the barometric branch.
        Remove this decorator once the branch is anchored (e.g. by
        interpolating the tabulated values instead of the polynomial); an
        unexpected success means the issue is fixed.
        """
        low = A.pressure(JOINT_120_KM - self.EPS)
        high = A.pressure(JOINT_120_KM + self.EPS)
        self.assertLessEqual(abs(high - low) / abs(low), 1e-4)


class AllCalcTest(unittest.TestCase):
    def test_returns_all_parameters(self):
        result = A.allcalc('1000')
        self.assertIsInstance(result, tuple)
        self.assertEqual(len(result), 11)
        self.assertEqual([row[0] for row in result],
                         ['P', 'T', 'ρ', 'M', 'n', 'a', 'l', 'μ', 'ν', 'W', 'H'])

    def test_invalid_text(self):
        self.assertIn('height', A.allcalc('abc'))

    def test_out_of_range(self):
        self.assertIn('range', A.allcalc('-2001'))
        self.assertIn('range', A.allcalc('1200001'))

    def test_boundary_values_accepted(self):
        for height in ('-2000', '0', '1200000'):
            with self.subTest(height=height):
                self.assertIsInstance(A.allcalc(height), tuple)


if __name__ == '__main__':
    unittest.main()
