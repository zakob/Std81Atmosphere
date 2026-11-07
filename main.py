"""Kivy application: GOST 4401-81 atmosphere calculator.

Screens: single-height calculation, range table (CSV export) and a plot.
All texts come from :mod:`std81.i18n` (Russian/English).
"""

import os
import re
import webbrowser
from math import log10

from kivy.animation import Animation
from kivy.app import App
from kivy.clock import Clock
from kivy.core.clipboard import Clipboard
from kivy.core.text import Label as CoreLabel
from kivy.core.window import Window
from kivy.graphics import Color, Line, Rectangle
from kivy.metrics import dp, sp
from kivy.properties import StringProperty
from kivy.storage.jsonstore import JsonStore
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.popup import Popup
from kivy.uix.spinner import Spinner  # noqa: F401  (used by atmo.kv)
from kivy.uix.togglebutton import ToggleButton
from kivy.uix.widget import Widget

from std81 import (
    HeightError,
    TableError,
    build_rows,
    compute,
    format_number,
    format_state,
    i18n,
    parse_request,
    rows_to_csv,
)

_SUPERSCRIPT = re.compile(r'\^(-?\d+)')


def markup(unit: str) -> str:
    """Turn a plain unit into Kivy markup: ``kg/m^3`` -> ``kg/m[sup]3[/sup]``."""
    return _SUPERSCRIPT.sub(r'[sup]\1[/sup]', unit)


def _toast(text: str) -> None:
    app = App.get_running_app()
    if app is not None:
        app.toast(text)


class Toast(Label):
    """Small transient message at the bottom of the window."""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.opacity = 0
        self.size_hint = (None, None)
        self.font_size = sp(14)
        with self.canvas.before:
            Color(0.13, 0.13, 0.16, 1)
            self._background = Rectangle(pos=self.pos, size=self.size)
        self.bind(pos=self._sync_background, size=self._sync_background)
        self.bind(texture_size=self._sync_size)
        Window.bind(size=lambda *_: self._place())

    def _sync_background(self, *_args):
        self._background.pos = self.pos
        self._background.size = self.size

    def _sync_size(self, *_args):
        self.size = (self.texture_size[0] + dp(28), self.texture_size[1] + dp(14))
        self._place()

    def _place(self):
        self.pos = (Window.width / 2 - self.width / 2, dp(72))

    def show(self, text: str):
        self.text = text
        self.opacity = 0.96
        Animation.cancel_all(self)
        Clock.schedule_once(self._hide, 1.7)

    def _hide(self, _dt):
        Animation(opacity=0, duration=0.45).start(self)


class CalcScreen(BoxLayout):
    highlight = StringProperty('')

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self._pending = None
        self.state = None

    def on_kv_post(self, base_widget):
        self.retranslate()

    def retranslate(self):
        self.ids.h_input.hint_text = i18n.tr('calc_hint')
        if not self.ids.h_input.text:
            self.ids.res_output.text = i18n.tr('calc_empty')

    def schedule_calc(self):
        if self._pending is not None:
            self._pending.cancel()
        self._pending = Clock.schedule_once(self._calculate, 0.25)

    def toggle_sign(self):
        text = self.ids.h_input.text.strip()
        if text.startswith('-'):
            self.ids.h_input.text = text[1:]
        elif text:
            self.ids.h_input.text = '-' + text

    def _calculate(self, _dt):
        self._pending = None
        text = self.ids.h_input.text.strip().replace(',', '.')
        if not text:
            self.state = None
            self.ids.res_output.text = i18n.tr('calc_empty')
            return
        try:
            self.state = compute(text)
        except HeightError as error:
            self.state = None
            self.ids.res_output.text = i18n.tr(error.code)
            return
        self.render()

    def render(self):
        if self.state is None:
            return
        rows = format_state(self.state)
        self.ids.res_output.text = ''.join(
            self._row(symbol, value, unit, symbol == self.highlight)
            for symbol, value, unit in rows
        )

    def _row(self, symbol, value, unit, selected):
        color = 'ff8a80' if selected else 'ffffff'
        return (
            f'{symbol} = [color={color}][ref={symbol} {value}]{value}[/ref][/color] '
            f'&bl;[color=9aa0a6]{markup(unit)}[/color]&br;\n'
        )

    def copy_value(self, ref):
        symbol, _, value = ref.partition(' ')
        Clipboard.copy(value)
        self.highlight = symbol
        self.render()
        _toast(i18n.tr('copied_value', value=value))


class TableScreen(BoxLayout):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.rows = []

    def on_kv_post(self, base_widget):
        self.ids.from_input.text = '0'
        self.ids.to_input.text = '10000'
        self.ids.step_input.text = '1000'
        self.retranslate()

    def retranslate(self):
        ids = self.ids
        ids.from_label.text = i18n.tr('table_from')
        ids.to_label.text = i18n.tr('table_to')
        ids.step_label.text = i18n.tr('table_step')
        ids.build_button.text = i18n.tr('table_build')
        ids.export_button.text = i18n.tr('table_export')
        if self.rows:
            self.fill_grid()

    def build_table(self):
        ids = self.ids
        try:
            request = parse_request(ids.from_input.text, ids.to_input.text, ids.step_input.text)
            self.rows = build_rows(request)
        except (TableError, HeightError) as error:
            self.rows = []
            self.ids.grid.clear_widgets()
            _toast(i18n.tr(getattr(error, 'code', 'error_generic')))
            return
        self.fill_grid()

    def fill_grid(self):
        grid = self.ids.grid
        grid.clear_widgets()
        headers = (i18n.tr('table_h'), i18n.tr('table_p'), i18n.tr('table_t'), i18n.tr('table_rho'))
        widths = (dp(76), dp(94), dp(84), dp(94))
        for text, width in zip(headers, widths):
            grid.add_widget(self._cell(text, width, bold=True))
        for state in self.rows:
            values = (
                f'{state.height:.10g}',
                format_number(state.pressure, 6),
                format_number(state.temperature, 6),
                format_number(state.density, 5),
            )
            for value, width in zip(values, widths):
                grid.add_widget(self._cell(value, width))

    @staticmethod
    def _cell(text, width, bold=False):
        return Label(
            text=text,
            size_hint=(None, None),
            size=(width, dp(24)),
            font_size=sp(12),
            bold=bold,
            color=(0.92, 0.93, 0.95, 1),
        )

    def export_csv(self):
        if not self.rows:
            _toast(i18n.tr('table_empty'))
            return
        Clipboard.copy(rows_to_csv(self.rows))
        _toast(i18n.tr('table_copied', count=len(self.rows)))


# (symbol, attribute, logarithmic)
PLOT_PARAMETERS = (
    ('P', 'pressure', True),
    ('T', 'temperature', False),
    ('rho', 'density', True),
    ('a', 'sound_speed', False),
)
PLOT_SPANS = (20_000, 50_000, 100_000, 1_200_000)
PLOT_SAMPLES = 240


class PlotWidget(Widget):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self._xs = []
        self._ys = []
        self._log = False
        self._title = ''
        self.bind(pos=self._redraw, size=self._redraw)

    def set_data(self, xs, ys, log=False, title=''):
        self._xs = list(xs)
        self._ys = list(ys)
        self._log = log
        self._title = title
        self._redraw()

    def _redraw(self, *_args):
        self.canvas.clear()
        if len(self._xs) < 2 or len(self._ys) < 2:
            return
        left, bottom, top, right = dp(58), dp(26), dp(22), dp(10)
        width = self.width - left - right
        height = self.height - bottom - top
        if width <= 0 or height <= 0:
            return

        xs = self._xs
        values = [self._log10(v) for v in self._ys]
        x0, x1 = xs[0], xs[-1]
        y0, y1 = min(values), max(values)
        if y0 == y1:
            y0, y1 = y0 - 1.0, y1 + 1.0
        pad = (y1 - y0) * 0.06
        y0 -= pad
        y1 += pad

        def px(x):
            return left + (x - x0) / (x1 - x0) * width

        def py(y):
            return bottom + (y - y0) / (y1 - y0) * height

        with self.canvas:
            Color(0.18, 0.19, 0.23, 1)
            for index in (1, 2, 3):
                y = bottom + height * index / 4
                Line(points=[left, y, left + width, y], width=1)
                x = left + width * index / 4
                Line(points=[x, bottom, x, bottom + height], width=1)

            Color(0.55, 0.57, 0.62, 1)
            Line(points=[left, bottom, left + width, bottom], width=1.2)
            Line(points=[left, bottom, left, bottom + height], width=1.2)

            Color(1.0, 0.72, 0.25, 1)
            points = []
            for x, y in zip(xs, values):
                points.extend((px(x), py(y)))
            Line(points=points, width=1.6)

        self._text(self._title, left, self.height - dp(18), size=12, color=(0.95, 0.95, 0.95, 1))
        self._text('h, km', left + width, self.height - dp(18), size=10, anchor='right')
        for index in range(5):
            value = y0 + (y1 - y0) * index / 4
            shown = 10 ** value if self._log else value
            self._text(f'{shown:.2g}', left - dp(6), bottom + height * index / 4 - dp(6),
                       size=9, anchor='right')
            x = x0 + (x1 - x0) * index / 4
            self._text(f'{x / 1000:.0f}', left + width * index / 4, dp(8),
                       size=9, anchor='center')

    @staticmethod
    def _log10(value):
        return log10(value) if value > 0 else -12.0

    def _text(self, text, x, y, size=10, anchor='left', color=(0.72, 0.74, 0.78, 1)):
        core = CoreLabel(text=text, font_size=sp(size))
        core.refresh()
        texture = core.texture
        if anchor == 'center':
            x -= texture.size[0] / 2
        elif anchor == 'right':
            x -= texture.size[0]
        with self.canvas:
            Color(*color)
            Rectangle(texture=texture, pos=(x, y), size=texture.size)


class PlotScreen(BoxLayout):
    def on_kv_post(self, base_widget):
        spinner = self.ids.parameter_spinner
        spinner.values = [symbol for symbol, _, _ in PLOT_PARAMETERS]
        spinner.text = 'P'
        self.ids.span_spinner.values = [f'0 – {span // 1000} km' for span in PLOT_SPANS]
        self.ids.span_spinner.text = self.ids.span_spinner.values[2]
        Clock.schedule_once(lambda _dt: self.refresh(), 0.2)

    def refresh(self):
        symbol = self.ids.parameter_spinner.text or 'P'
        parameter = next(
            (item for item in PLOT_PARAMETERS if item[0] == symbol), PLOT_PARAMETERS[0])
        span_text = self.ids.span_spinner.text
        try:
            span = PLOT_SPANS[self.ids.span_spinner.values.index(span_text)]
        except ValueError:
            span = PLOT_SPANS[2]
        xs = [span * index / (PLOT_SAMPLES - 1) for index in range(PLOT_SAMPLES)]
        states = [compute(height) for height in xs]
        ys = [getattr(state, parameter[1]) for state in states]
        self.ids.plot.set_data(xs, ys, log=parameter[2], title=symbol)


class Root(BoxLayout):
    def retranslate(self):
        ids = self.ids
        ids.nav_calc.text = i18n.tr('nav_calc')
        ids.nav_table.text = i18n.tr('nav_table')
        ids.nav_plot.text = i18n.tr('nav_plot')
        ids.nav_settings.text = i18n.tr('nav_settings')
        ids.calc_screen.retranslate()
        ids.table_screen.retranslate()

    def show(self, name):
        self.ids.manager.current = name
        if name == 'table' and not self.ids.table_screen.rows:
            self.ids.table_screen.build_table()


class AboutPopup(Popup):
    def __init__(self, **kwargs):
        content = BoxLayout(orientation='vertical', spacing=dp(8), padding=dp(12))
        title = Label(
            text=i18n.tr('about_text'),
            font_size=sp(20),
            halign='center',
            valign='middle',
            size_hint_y=0.45,
        )
        title.bind(size=lambda widget, size: setattr(widget, 'text_size', size))
        content.add_widget(title)
        content.add_widget(Label(text=i18n.tr('about_note'), font_size=sp(13), size_hint_y=0.15))
        contact = Label(
            text=f'{i18n.tr("about_contact")}[ref=mailto:bad_zako@hotmail.com]'
                 'bad_zako@hotmail.com[/ref]',
            markup=True,
            font_size=sp(13),
            size_hint_y=0.15,
        )
        contact.bind(on_ref_press=lambda _widget, ref: webbrowser.open(ref))
        content.add_widget(contact)
        close = Button(text=i18n.tr('close'), size_hint_y=0.2, font_size=sp(14))
        close.bind(on_release=lambda *_: self.dismiss())
        content.add_widget(close)
        super().__init__(title=i18n.tr('about_title'), content=content,
                         size_hint=(0.9, None), height=dp(380), **kwargs)


class SettingsPopup(Popup):
    def __init__(self, **kwargs):
        content = BoxLayout(orientation='vertical', spacing=dp(10), padding=dp(12))
        content.add_widget(
            Label(text=i18n.tr('settings_language'), size_hint_y=None, height=dp(24)))
        languages = BoxLayout(size_hint_y=None, height=dp(44), spacing=dp(8))
        for code, name in i18n.available_languages():
            button = ToggleButton(
                text=name,
                group='language',
                state='down' if code == i18n.get_language() else 'normal',
                font_size=sp(14),
            )
            button.bind(on_release=lambda _widget, selected=code: self._choose(selected))
            languages.add_widget(button)
        content.add_widget(languages)

        about = Button(
            text=i18n.tr('settings_about'), size_hint_y=None, height=dp(44), font_size=sp(14))
        about.bind(on_release=lambda *_: AboutPopup().open())
        content.add_widget(about)

        close = Button(text=i18n.tr('close'), size_hint_y=None, height=dp(44), font_size=sp(14))
        close.bind(on_release=lambda *_: self.dismiss())
        content.add_widget(close)
        super().__init__(title=i18n.tr('settings_title'), content=content,
                         size_hint=(0.85, None), height=dp(300), **kwargs)

    def _choose(self, code):
        App.get_running_app().change_language(code)
        self.dismiss()


class AtmoApp(App):
    def build(self):
        Window.clearcolor = (0.07, 0.08, 0.10, 1)
        Window.softinput_mode = 'below_target'
        self.store = self._open_store()
        if self.store is not None and self.store.exists('language'):
            try:
                i18n.set_language(self.store.get('language')['value'])
            except ValueError:
                pass
        self.title = i18n.tr('app_title')
        self.toast_widget = Toast()
        self.root_widget = Root()
        self.root_widget.retranslate()
        self.root_widget.ids.calc_screen.ids.h_input.text = '10000'
        return self.root_widget

    def on_start(self):
        Window.add_widget(self.toast_widget)

    def _open_store(self):
        try:
            base = os.environ.get('STD81_DATA_DIR') or self.user_data_dir
            os.makedirs(base, exist_ok=True)
            return JsonStore(os.path.join(base, 'settings.json'))
        except OSError:
            return None

    def toast(self, text):
        self.toast_widget.show(text)

    def show_screen(self, name):
        self.root_widget.show(name)

    def open_settings(self):
        SettingsPopup().open()

    def change_language(self, code):
        i18n.set_language(code)
        self.title = i18n.tr('app_title')
        self.root_widget.retranslate()
        if self.store is not None:
            try:
                self.store.put('language', value=code)
            except OSError:
                pass


if __name__ == '__main__':
    AtmoApp().run()
