"""Command line interface: ``python -m std81 1000``."""

import sys
from typing import List, Optional

from .api import allcalc


def main(argv: Optional[List[str]] = None) -> int:
    arguments = sys.argv[1:] if argv is None else argv
    if len(arguments) != 1:
        print('You must specify height in meters.')
        return 1
    result = allcalc(arguments[0])
    if isinstance(result, str):
        print(result)
        return 2
    for symbol, value, unit in result:
        print(f'{symbol} = {value} {unit}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
