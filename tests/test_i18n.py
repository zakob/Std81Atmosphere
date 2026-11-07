"""Tests for localization (no Kivy required)."""

import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from std81 import PARAMETERS, compute, format_state, i18n  # noqa: E402


class TranslationTest(unittest.TestCase):
    def setUp(self):
        self.language = i18n.get_language()

    def tearDown(self):
        i18n.set_language(self.language)

    def test_languages_have_the_same_keys(self):
        russian = set(i18n.TRANSLATIONS['ru'])
        english = set(i18n.TRANSLATIONS['en'])
        self.assertEqual(russian, english)

    def test_every_string_is_not_empty(self):
        for code, strings in i18n.TRANSLATIONS.items():
            for key, value in strings.items():
                with self.subTest(language=code, key=key):
                    self.assertTrue(value.strip())

    def test_switch_language(self):
        i18n.set_language('en')
        self.assertEqual(i18n.tr('nav_calc'), 'Calc')
        i18n.set_language('ru')
        self.assertEqual(i18n.tr('nav_calc'), 'Расчёт')

    def test_unknown_language(self):
        with self.assertRaises(ValueError):
            i18n.set_language('de')

    def test_unknown_key_falls_back_to_the_key(self):
        self.assertEqual(i18n.tr('no.such.key'), 'no.such.key')

    def test_placeholders_are_substituted(self):
        i18n.set_language('ru')
        self.assertIn('7', i18n.tr('table_copied', count=7))
        i18n.set_language('en')
        self.assertIn('7', i18n.tr('table_copied', count=7))

    def test_available_languages(self):
        codes = [code for code, _ in i18n.available_languages()]
        self.assertIn('ru', codes)
        self.assertIn('en', codes)

    def test_unit_keys_exist_in_every_language(self):
        for parameter in PARAMETERS:
            for code, strings in i18n.TRANSLATIONS.items():
                with self.subTest(language=code, key=parameter.unit_key):
                    self.assertIn(parameter.unit_key, strings)

    def test_units_follow_language(self):
        state = compute(0)
        i18n.set_language('ru')
        russian = [row[2] for row in format_state(state)]
        self.assertEqual(russian[0], 'Па')
        self.assertIn('кг/м^3', russian)
        i18n.set_language('en')
        english = [row[2] for row in format_state(state)]
        self.assertEqual(english[0], 'Pa')
        self.assertIn('kg/m^3', english)


if __name__ == '__main__':
    unittest.main()
