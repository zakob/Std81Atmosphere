"""Range tables and CSV export.

The UI builds a table of parameters over a height range and copies it as CSV;
the same code can be used from scripts.
"""

from dataclasses import dataclass
from math import floor
from typing import List, Tuple

from .atmosphere import AtmosphereState, compute, validate_height

MAX_ROWS = 300

# (symbol, unit) for the CSV header; the order matches the row values below.
CSV_COLUMNS: Tuple[Tuple[str, str], ...] = (
    ('h', 'm'),
    ('T', 'K'),
    ('p', 'Pa'),
    ('rho', 'kg/m^3'),
    ('M', 'kg/mol'),
    ('n', 'm^-3'),
    ('a', 'm/s'),
    ('l', 'm'),
    ('mu', 'Pa*s'),
    ('nu', 'm^2/s'),
    ('W', 'W/(m*K)'),
    ('H', 'm'),
)

_FIELDS = (
    'height',
    'temperature',
    'pressure',
    'density',
    'molar_mass',
    'concentration',
    'sound_speed',
    'mean_free_path',
    'dynamic_viscosity',
    'kinematic_viscosity',
    'thermal_conductivity',
    'geopotential_height',
)


class TableError(ValueError):
    """Invalid table request; ``code`` is a stable identifier for the UI."""

    def __init__(self, code: str) -> None:
        self.code = code
        super().__init__(code)


@dataclass(frozen=True)
class TableRequest:
    start: float
    stop: float
    step: float

    @property
    def count(self) -> int:
        return floor((self.stop - self.start) / self.step + 1e-9) + 1


def _parse_number(text: str) -> float:
    try:
        return float(str(text).strip().replace(',', '.'))
    except (TypeError, ValueError):
        raise TableError('table_bad_number') from None


def parse_request(start: str, stop: str, step: str) -> TableRequest:
    """Validate user input and build a :class:`TableRequest`."""
    start_value = _parse_number(start)
    stop_value = _parse_number(stop)
    step_value = _parse_number(step)
    if step_value <= 0:
        raise TableError('table_bad_step')
    if start_value >= stop_value:
        raise TableError('table_bad_order')
    validate_height(start_value)
    validate_height(stop_value)
    request = TableRequest(start_value, stop_value, step_value)
    if request.count > MAX_ROWS:
        raise TableError('table_too_many')
    return request


def build_rows(request: TableRequest) -> List[AtmosphereState]:
    """Compute all table rows; the last row never exceeds ``request.stop``."""
    rows = []
    for index in range(request.count):
        height = request.start + index * request.step
        if height > request.stop + 1e-9:
            break
        rows.append(compute(round(height, 6)))
    return rows


def _format(value: float) -> str:
    if value is None:
        return ''
    return f'{value:.10g}'


def rows_to_csv(rows: List[AtmosphereState], delimiter: str = ';') -> str:
    """Serialize rows to CSV (``;`` by default, which Excel expects in ru locale)."""
    lines = [delimiter.join(f'{name},{unit}' for name, unit in CSV_COLUMNS)]
    for state in rows:
        lines.append(delimiter.join(_format(getattr(state, field)) for field in _FIELDS))
    return '\n'.join(lines)
