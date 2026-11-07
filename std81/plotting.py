"""Axis ticks for the plot (no Kivy dependency)."""

from math import log10
from typing import List, Tuple


def scale_ticks(
    low: float,
    high: float,
    count: int = 5,
    logarithmic: bool = False,
) -> Tuple[List[float], List[float]]:
    """Return tick positions and the values they represent.

    Positions are in the coordinate system used for drawing (``log10`` values
    for a logarithmic axis).  The first and last ticks are exactly ``low`` and
    ``high``, so the axis labels depend on the data only, never on the padding
    around the curve.
    """
    if count < 2:
        raise ValueError('count must be at least 2')
    if logarithmic:
        if low <= 0 or high <= 0:
            raise ValueError('logarithmic ticks require positive values')
        start, stop = log10(low), log10(high)
    else:
        start, stop = float(low), float(high)
    positions = [start + (stop - start) * index / (count - 1) for index in range(count)]
    values = [10.0 ** position for position in positions] if logarithmic else positions
    return positions, values
