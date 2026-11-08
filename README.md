# Std81Atmosphere

Android app (Kotlin + Jetpack Compose) that calculates the parameters of the
standard atmosphere according to **GOST 4401-81** for a geometric height from
−2000 to 1 200 000 m.

The calculation core is pure Kotlin (no Android dependencies) and is verified
against 1410 reference rows extracted from the tables of the standard.

> The previous Python/Kivy implementation is preserved in the **`kivy-legacy`**
> branch. This `main` branch is the Kotlin/Jetpack Compose rewrite.

## Features

* calculation of 11 parameters for a single height (P, T, ρ, M, n, a, l, μ, ν,
  W, H), tap a value to copy it;
* parameter table over a height range with CSV export to the clipboard;
* plots of P (log), T, ρ (log) and a over spans 0–20/50/100/1200 km;
* Russian/English interface, the choice is remembered between launches;
* no permissions, no network access.

## Project layout

| Path | Purpose |
|---|---|
| `app/src/main/java/org/std81atmosphere/model/` | calculation core: constants, layers, atmosphere, table, formatting, translations |
| `app/src/main/java/org/std81atmosphere/ui/` | Jetpack Compose screens |
| `app/src/test/` | JUnit golden tests against the GOST tables + physical invariants |
| `app/src/test/resources/gost_4401_81.csv` | reference data (1410 rows, tables 1–4 of GOST 4401-81) |
| `.github/workflows/android.yml` | lint, unit tests, debug and release APK builds |

## The model

`Atmosphere.compute(height)` returns every parameter for one height. The model
reproduces the layer table of the standard: the layers are keyed by
geopotential height with pressures anchored at 101325 Pa at h = 0, the upper
atmosphere (120–1200 km) uses the appendix polynomials, and transport
properties return `null` above 90 km (the standard does not tabulate them
there).

Known behaviour, verified by tests:

* `P(0) = 101325 Pa` exactly;
* worst deviation from the tabulated values is < 1·10⁻³ across the range
  (T/a/μ are within 1·10⁻⁴);
* at exactly 120 km the appendix polynomial starts ≈0.75 % below the
  barometric branch; the step disappears by 121 km.

## Testing

```sh
gradle testDebugUnitTest
```

or in Android Studio: right-click `app` → Run all tests. The golden tests read
`gost_4401_81.csv` from the test resources; tolerances are stated per
parameter in `GostReferenceTest`.

## Building locally

You need JDK 17 and Android SDK (compileSdk 35). The project does not commit
the Gradle wrapper jar, so on a fresh machine run once:

```sh
gradle wrapper --gradle-version 8.10.2   # if you have gradle installed
./gradlew assembleDebug                  # or just: gradle assembleDebug
```

In Android Studio: open the project root, let it import, then Run. A local
`assembleRelease` requires the signing environment variables
(`RELEASE_KEYSTORE_FILE`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
`RELEASE_KEY_ALIAS_PASSWORD`); the debug build needs none.

## Continuous integration

See [docs/ci.md](docs/ci.md): on every push GitHub Actions runs lint and the
unit tests; a debug APK can be built manually; pushing a `v*` tag builds a
signed release APK and creates a GitHub Release.

## Reference data provenance

The CSV reference was extracted from the public text of GOST 4401-81 (tables
1–4: temperature, pressure, density, speed of sound, viscosities, thermal
conductivity, concentration, mean free path, molar mass, geopotential height).
Rows were accepted only after internal-consistency validation (p vs mmHg,
ρ = p·M/(R·T), n = p/(k·T), λ = 2.332376e-5·T/p, smoothness); 82 of 1492
parsed rows were rejected as OCR noise of the scanned source. See the
`kivy-legacy` branch `tests/README.md` for the full extraction notes.