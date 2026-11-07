"""Compatibility API used by the app and the command line interface."""

from typing import Tuple, Union

from .atmosphere import compute
from .formatting import format_state

CalcResult = Union[str, Tuple[Tuple[str, str, str], ...]]


def allcalc(height: Union[float, str]) -> CalcResult:
    """Return the 11 parameter rows for ``height`` or an error message.

    This keeps the original ``atmolib.allcalc`` contract (a string on invalid
    input instead of an exception) so that the UI stays simple.  New code
    should prefer :func:`std81.atmosphere.compute`.
    """
    try:
        state = compute(height)
    except ValueError as error:
        return str(error)
    return format_state(state)
