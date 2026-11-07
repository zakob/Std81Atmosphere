"""Tests for range tables and CSV export (no Kivy required)."""

import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from std81 import HeightError, TableError, build_rows, parse_request, rows_to_csv  # noqa: E402
from std81.table import MAX_ROWS, TableRequest  # noqa: E402


class ParseRequestTest(unittest.TestCase):
    def test_valid_request(self):
        request = parse_request('0', '1000', '100')
        self.assertEqual(request.count, 11)
        self.assertEqual((request.start, request.stop, request.step), (0.0, 1000.0, 100.0))

    def test_comma_decimals(self):
        request = parse_request('0', '1000,5', '500,25')
        self.assertEqual(request.step, 500.25)
        self.assertEqual(request.count, 3)

    def test_bad_number(self):
        with self.assertRaises(TableError) as context:
            parse_request('abc', '1000', '100')
        self.assertEqual(context.exception.code, 'table_bad_number')

    def test_bad_step(self):
        for step in ('0', '-10'):
            with self.subTest(step=step):
                with self.assertRaises(TableError) as context:
                    parse_request('0', '1000', step)
                self.assertEqual(context.exception.code, 'table_bad_step')

    def test_bad_order(self):
        with self.assertRaises(TableError) as context:
            parse_request('1000', '0', '100')
        self.assertEqual(context.exception.code, 'table_bad_order')

    def test_out_of_range(self):
        with self.assertRaises(HeightError) as context:
            parse_request('-2001', '0', '100')
        self.assertEqual(context.exception.code, 'out_of_range')

    def test_too_many_rows(self):
        with self.assertRaises(TableError) as context:
            parse_request('0', '1000000', '100')
        self.assertEqual(context.exception.code, 'table_too_many')


class BuildRowsTest(unittest.TestCase):
    def test_row_count_and_edges(self):
        rows = build_rows(parse_request('0', '1000', '100'))
        self.assertEqual(len(rows), 11)
        self.assertEqual(rows[0].height, 0.0)
        self.assertEqual(rows[-1].height, 1000.0)

    def test_pressure_decreases(self):
        rows = build_rows(parse_request('0', '5000', '500'))
        pressures = [row.pressure for row in rows]
        self.assertEqual(pressures, sorted(pressures, reverse=True))

    def test_large_step_is_allowed(self):
        rows = build_rows(TableRequest(0.0, 1200000.0, 1000000.0))
        self.assertEqual(len(rows), 2)


class CsvTest(unittest.TestCase):
    def test_header_and_rows(self):
        rows = build_rows(parse_request('0', '2000', '1000'))
        text = rows_to_csv(rows)
        lines = text.splitlines()
        self.assertEqual(len(lines), 4)  # header + 0, 1000, 2000 m
        self.assertEqual(len(lines[0].split(';')), 12)
        self.assertTrue(lines[0].startswith('h,m;T,K;p,Pa'))
        for line in lines[1:]:
            self.assertEqual(len(line.split(';')), 12)

    def test_missing_transport_values_are_empty(self):
        rows = build_rows(parse_request('100000', '102000', '1000'))
        line = rows_to_csv(rows).splitlines()[1]
        fields = line.split(';')
        self.assertEqual(fields[8], '')  # dynamic viscosity above 90 km
        self.assertEqual(fields[9], '')  # kinematic viscosity
        self.assertEqual(fields[10], '')  # thermal conductivity

    def test_max_rows_constant(self):
        self.assertGreaterEqual(MAX_ROWS, 10)


if __name__ == '__main__':
    unittest.main()
