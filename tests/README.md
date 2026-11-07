# Reference data: GOST 4401-81

`data/gost_4401_81.csv` is a machine-readable extract of the tables of
**ГОСТ 4401-81 «Атмосфера стандартная. Параметры»** (Standard Atmosphere,
parameters, −2000 … 1 200 000 m).

It is used by `tests/test_gost_reference.py` as a golden reference: every
value produced by `atmolib.py` must match the standard within the tolerance
stated in that file.

## Source

The tables were extracted from the publicly available text of the standard:

<https://meganorm.ru/mega_doc/norm/gost_gosudarstvennyj-standart/24/gost_4401-81_mezhgosudarstvennyy_standart_atmosfera.html>

Tables used:

| GOST table | Content | Heights |
|---|---|---|
| 1 | T, p, ρ, g vs geometric and geopotential height | −2000 … 80 000 m |
| 2 | a, μ, ν, λ | −2000 … 80 000 m |
| 3 | n, mean free path, collision frequency, etc. | −2000 … 80 000 m |
| 4 | T, p, ρ, g, n, λ, M | 80 000 … 1 200 000 m |
| appendix, table 95 | layer boundaries: T, gradients, M | — |

## CSV columns

| Column | Meaning | Unit |
|---|---|---|
| `h` | geometric height | m |
| `H` | geopotential height | m |
| `T` | temperature | K |
| `p` | pressure | Pa |
| `rho` | density | kg/m³ |
| `a` | speed of sound | m/s |
| `mu` | dynamic viscosity | Pa·s |
| `nu` | kinematic viscosity | m²/s |
| `W` | thermal conductivity | W/(m·K) |
| `n` | particle concentration | m⁻³ |
| `lambda` | mean free path | m |
| `M` | molar mass (converted from the table's kg/kmol) | kg/mol |
| `g` | acceleration of gravity | m/s² |

Empty cells mean the parameter is not tabulated at that height.

## Validation performed on extraction

The scanned HTML contains OCR artefacts, so every row was checked for internal
consistency **before** being accepted:

* `p` ↔ `p` in mm Hg (`1 mmHg = 133.322387415 Pa`);
* `ρ` ↔ `p·M/(R·T)` (ideal gas, `R = 8.31432`);
* `n` ↔ `p/(k·T)`;
* `λ` ↔ `2.332376e-5·T/p`;
* `a` ↔ `√(1.4·R·T/M)`;
* `μ`, `W` ↔ Sutherland-type formulas;
* `H` ↔ `Rz·h/(Rz+h)`, `Rz = 6 356 767 m`;
* pressure must be non-increasing with height;
* every value must be consistent with its neighbours (local smoothness).

1492 candidate rows were parsed, **82 were rejected**, 1410 were kept.

Rejected rows (counts overlap, a row may fail several checks): temperature /
pressure / density inconsistencies 24, `λ` 12, `n` 9, `μ` 8, `W` 8, `p(mmHg)`
8, `a` 6, `H` 1, pressure non-monotonic 2, local outliers 2.

Known gaps:

* **400–1950 m**: the scanned source omits table 1 for this interval, so those
  rows have no temperature/pressure and cannot be cross-checked; they were
  excluded. Table 2/3 values for that interval (a, μ, ν, W, n, λ) are therefore
  not covered by the fixture.
* **above 80 000 m**: GOST table 2 ends there, so `a`, `μ`, `ν`, `W` have no
  reference values above 80 km. The app extrapolates to 90 km and returns
  `None` above (a deliberate limitation, not a GOST requirement).

## Current accuracy of `atmolib.py` against the tables

Measured with this fixture (worst relative deviation over all kept rows):

| Parameter | Worst deviation | Where |
|---|---|---|
| `T` | 5.6·10⁻⁵ | 165 km |
| `p` | 3.5·10⁻⁴ (8.1·10⁻³) | 80–120 km (120–150 km) |
| `rho` | 4.1·10⁻⁴ (8.1·10⁻³) | 80–120 km (120–150 km) |
| `a` | 2.6·10⁻⁵ | everywhere |
| `mu` | 3.5·10⁻⁵ | everywhere |
| `nu` | 8.9·10⁻⁴ | 67.4 km |
| `W` | 1.3·10⁻⁴ | 60.4 km |
| `n` | 3.4·10⁻⁴ (8.2·10⁻³) | 80–120 km (120–150 km) |
| `lambda` | 1.7·10⁻⁴ (8.2·10⁻³) | 80–120 km (120–150 km) |
| `M` | 1.0·10⁻³ | 1 100 km |
| `H` | 2.8·10⁻⁴ | −1 750 m |

The 120–150 km layer is the only systematic discrepancy: the standard defines
it through an approximation polynomial (see the appendix), and that polynomial
is itself up to ~0.8 % below the tabulated values at 150 km. Because the lower
(barometric) branch is exact there, pressure, density and concentration have a
~0.8 % step at the 120 km joint. This is documented by
`test_known_120_150_km_deviation` and the expected-failure test
`test_pressure_continuous_at_120_km`.

## Running the tests

No third-party packages are required:

```sh
python -m unittest discover -s tests -v
```

or, from the repository root:

```sh
python -m unittest tests.test_gost_reference tests.test_invariants -v
```

To extend the fixture, add rows to the CSV and keep the validation rules above
in mind; values taken from the standard must be given to at least 5 significant
digits.
