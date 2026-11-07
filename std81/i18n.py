"""User interface strings (Russian and English).

The module has no Kivy dependency so it can be tested and reused from scripts.
"""

from typing import Dict, Tuple

DEFAULT_LANGUAGE = 'ru'

LANGUAGES: Dict[str, str] = {
    'ru': 'Русский',
    'en': 'English',
}

TRANSLATIONS: Dict[str, Dict[str, str]] = {
    'ru': {
        'app_title': 'Стандартная атмосфера',
        'nav_calc': 'Расчёт',
        'nav_table': 'Таблица',
        'nav_plot': 'График',
        'nav_settings': 'Ещё',
        'calc_hint': 'Высота, м',
        'calc_empty': 'Введите высоту в метрах',
        'invalid_number': 'Введите высоту числом (метры).',
        'out_of_range': 'Допустимый диапазон:\nот -2000 до 1200000 м.',
        'copied_value': 'Скопировано: {value}',
        'table_from': 'От, м',
        'table_to': 'До, м',
        'table_step': 'Шаг, м',
        'table_build': 'Построить',
        'table_export': 'Копировать CSV',
        'table_h': 'h, м',
        'table_p': 'P, Па',
        'table_t': 'T, К',
        'table_rho': 'ρ, кг/м³',
        'table_empty': 'Таблица пуста — нажмите «Построить»',
        'table_bad_number': 'Введите числа в поля диапазона и шага.',
        'table_bad_step': 'Шаг должен быть больше нуля.',
        'table_bad_order': 'Начало должно быть меньше конца.',
        'table_too_many': 'Слишком много строк — увеличьте шаг.',
        'table_copied': 'CSV скопирован: строк {count}',
        'plot_parameter': 'Параметр',
        'plot_span': 'Диапазон',
        'plot_axis_h': 'h, км',
        'plot_axis_p': 'P, Па',
        'plot_axis_t': 'T, К',
        'plot_axis_rho': 'ρ, кг/м³',
        'plot_axis_a': 'a, м/с',
        'plot_empty': 'Нет данных',
        'settings_title': 'Настройки',
        'settings_language': 'Язык',
        'settings_about': 'О программе',
        'close': 'Закрыть',
        'about_title': 'О программе',
        'about_text': 'ГОСТ 4401-81\nСтандартная атмосфера\nпараметры',
        'about_note': 'Расчёт по таблицам стандарта',
        'about_contact': 'Связаться: ',
        'error_generic': 'Не удалось выполнить расчёт.',
    },
    'en': {
        'app_title': 'Standard Atmosphere',
        'nav_calc': 'Calc',
        'nav_table': 'Table',
        'nav_plot': 'Plot',
        'nav_settings': 'More',
        'calc_hint': 'Height, m',
        'calc_empty': 'Enter height in meters',
        'invalid_number': 'Enter the height as a number (meters).',
        'out_of_range': 'Valid range:\n-2000 to 1200000 m.',
        'copied_value': 'Copied: {value}',
        'table_from': 'From, m',
        'table_to': 'To, m',
        'table_step': 'Step, m',
        'table_build': 'Build',
        'table_export': 'Copy CSV',
        'table_h': 'h, m',
        'table_p': 'P, Pa',
        'table_t': 'T, K',
        'table_rho': 'ρ, kg/m³',
        'table_empty': 'The table is empty — press "Build"',
        'table_bad_number': 'Enter numbers for the range and the step.',
        'table_bad_step': 'The step must be greater than zero.',
        'table_bad_order': 'The start must be less than the end.',
        'table_too_many': 'Too many rows — increase the step.',
        'table_copied': 'CSV copied: {count} rows',
        'plot_parameter': 'Parameter',
        'plot_span': 'Range',
        'plot_axis_h': 'h, km',
        'plot_axis_p': 'P, Pa',
        'plot_axis_t': 'T, K',
        'plot_axis_rho': 'ρ, kg/m³',
        'plot_axis_a': 'a, m/s',
        'plot_empty': 'No data',
        'settings_title': 'Settings',
        'settings_language': 'Language',
        'settings_about': 'About',
        'close': 'Close',
        'about_title': 'About',
        'about_text': 'GOST 4401-81\nStandard atmosphere\nparameters',
        'about_note': 'Values follow the standard tables',
        'about_contact': 'Contact: ',
        'error_generic': 'The calculation failed.',
    },
}

_current = DEFAULT_LANGUAGE


def get_language() -> str:
    return _current


def set_language(code: str) -> None:
    global _current
    if code not in TRANSLATIONS:
        raise ValueError(f'unknown language: {code}')
    _current = code


def available_languages() -> Tuple[Tuple[str, str], ...]:
    return tuple((code, name) for code, name in LANGUAGES.items())


def tr(key: str, **kwargs) -> str:
    """Translate ``key`` into the current language, falling back to English."""
    text = TRANSLATIONS.get(_current, {}).get(key)
    if text is None:
        text = TRANSLATIONS['en'].get(key, key)
    if kwargs:
        try:
            return text.format(**kwargs)
        except (KeyError, IndexError):
            return text
    return text
