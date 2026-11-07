"""Kivy application: a single-screen GOST 4401-81 calculator."""

import re

from kivy.app import App
from kivy.properties import ObjectProperty
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.popup import Popup

from std81 import allcalc

_SUPERSCRIPT = re.compile(r'\^(-?\d+)')


def markup(unit: str) -> str:
    """Turn a plain unit into Kivy markup: ``kg/m^3`` -> ``kg/m[sup]3[/sup]``."""
    return _SUPERSCRIPT.sub(r'[sup]\1[/sup]', unit)


class About(Popup):
    pass


class AtmoCalc(BoxLayout):
    h_input = ObjectProperty(None)
    res_output = ObjectProperty(None)

    calc_res = ()

    def get_about(self):
        About().open()

    def format_row(self, symbol, value, unit, highlight=False):
        color = 'ff0000' if highlight else 'ffffff'
        return (
            f'{symbol} = [color={color}][ref={symbol} {value}]{value}[/ref][/color] '
            f'&bl;{markup(unit)}&br;\n'
        )

    def render(self, highlight=None):
        self.res_output.text = ''.join(
            self.format_row(symbol, value, unit, highlight=(symbol == highlight))
            for symbol, value, unit in self.calc_res
        )

    def change_color(self, ref):
        self.render(highlight=ref.split()[0])

    def calc(self):
        result = allcalc(self.h_input.text)
        if isinstance(result, str):
            self.calc_res = ()
            self.res_output.text = result
            return
        self.calc_res = result
        self.render()


class AtmoApp(App):
    def build(self):
        return AtmoCalc()


if __name__ == '__main__':
    AtmoApp().run()
