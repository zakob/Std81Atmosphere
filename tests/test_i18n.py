"""Tests for localization (no Kivy required)."""

import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, ROOT)

from std81 import i18n  # noqa: E402


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


if __name__ == '__main__':
    unittest.main()
