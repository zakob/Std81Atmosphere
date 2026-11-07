"""Tests for plot axis ticks (no Kivy required)."""

import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from std81.plotting import scale_ticks  # noqa: E402


class ScaleTicksTest(unittest.TestCase):
    def test_linear(self):
        positions, values = scale_ticks(186.0, 1000.0, 5)
        self.assertEqual(len(positions), 5)
        self.assertAlmostEqual(positions[0], 186.0)
        self.assertAlmostEqual(positions[-1], 1000.0)
        self.assertEqual(values, positions)

    def test_logarithmic(self):
        positions, values = scale_ticks(0.032, 101325.0, 5, logarithmic=True)
        self.assertAlmostEqual(values[0], 0.032)
        self.assertAlmostEqual(values[-1], 101325.0)
        self.assertGreater(positions[-1], positions[0])
        self.assertTrue(all(value > 0 for value in values))

    def test_labels_are_independent_of_padding(self):
        # The first/last labels must be the data extremes for any range.
        for low, high in ((0.032, 101325.0), (0.002, 101325.0), (0.0003, 101325.0)):
            _, values = scale_ticks(low, high, 5, logarithmic=True)
            self.assertAlmostEqual(values[0], low)
            self.assertAlmostEqual(values[-1], high)

    def test_invalid(self):
        with self.assertRaises(ValueError):
            scale_ticks(1.0, 2.0, 1)
        with self.assertRaises(ValueError):
            scale_ticks(0.0, 2.0, 5, logarithmic=True)


if __name__ == '__main__':
    unittest.main()
