from math import exp, log, sqrt, pow
from typing import Union


def pressure(h: float) -> float:
    """Return pressure [Pa]."""
    Mc = 0.02896442  # molar weight of air at sea level
    gc = 9.80665     # acceleration of gravity
    R = 8.314        # universal gas constant
    # H1 - geopotential height of current layer
    # P1 - lower pressure of layer
    # T1m - lower molar temperature of layer
    # b - molar or thermodynamic temperature gradient

    H = geopotential_height(h)

    if h <= 120000: 
        if h <= -1999:
            return 127783
        if h > -1999 and h <= 0:
            H1, P1, T1m, b = -2000, 127774, 301.15, -0.0065
        if h > 0 and h <= 11019:
            H1, P1, T1m, b = 0, 101325, 288.15, -0.0065
        if h > 11019 and h <= 20063:
            H1, P1, T1m, b = 11000, 22632.0, 216.65, 0
        if h > 20063 and h <= 32162:
            H1, P1, T1m, b = 20000, 5474.87, 216.65, 0.0010
        if h > 32162 and h <= 47350:
            H1, P1, T1m, b = 32000, 868.014, 228.65, 0.0028
        if h > 47350 and h <= 51412:
            H1, P1, T1m, b = 47000, 110.906, 270.65,0
        if h > 51412 and h <= 71802:
            H1, P1, T1m, b = 51000, 66.9384, 270.65, -0.0028
        if h > 71802 and h <= 86152:
            H1, P1, T1m, b = 71000, 3.95639, 214.65, -0.0020
        if h > 86152 and h <= 95411:
            H1, P1, T1m, b = 85000, pressure(86152), 186.65, 0
        if h > 95411 and h <= 104128:
            H1, P1, T1m, b = 94000, pressure(95411), 186.65, 0.0030
        if h > 104128 and h <= 120000:
            H1, P1, T1m, b =102450, pressure(104128), 212.00, 0.0110

        if b != 0:
            return exp(log(P1) - gc *Mc * log( (T1m + b * (H - H1))/T1m ) / R /b)
        return exp(log(P1) - gc * Mc * (H - H1) / R / T1m)

    if h > 120000 and h <= 1200000:
        n = concentration(h)
        T = temperature(h)
        return 1 / 7.243611 * 1e-22 * n * T 

    return 0


def temperature(h: float) -> float:
    """Return temperature [K]."""
    Mc = 0.02896442  # molar weight of air at sea level
    # Tm - molar temperature
    # T1m - lower molar temperature of layer
    # T1 - lower thermodynamic temperature of layer
    # b - molar or thermodynamic temperature gradient
    # H1 - geopotential height of current layer
    # h1 - geometric height of current layer

    if h <= 120000: 
        H = geopotential_height(h)
        M = molar_weight(h)
        if H <= -2000:
            return 301.15
        if H > -2000 and H <= 0:
            H1, T1m, b = -2000, 301.15, -0.0065
        if H > 0 and H <= 11000:
            H1, T1m, b = 0, 288.15, -0.0065
        if H > 11000 and H <= 20000:
            H1, T1m, b = 11000, 216.65, 0
        if H > 20000 and H <= 32000:
            H1, T1m, b = 20000, 216.65, 0.0010
        if H > 32000 and H <= 47000:
            H1, T1m, b = 32000, 228.65, 0.0028
        if H > 47000 and H <= 51000:
            H1, T1m, b = 47000, 270.65, 0
        if H > 51000 and H <= 71000:
            H1, T1m, b = 51000, 270.65, -0.0028
        if H > 71000 and H <= 85000:
            H1, T1m, b = 71000, 214.65, -0.0020
        if H > 85000 and H <= 94000:
            H1, T1m, b = 85000, 186.65, 0
        if H > 94000 and H <= 102450:
            H1, T1m, b = 94000, 186.65, 0.0030
        if H > 102450 and h <= 120000:
            H1, T1m, b = 102450, 212.00, 0.0110
        Tm = T1m + b * (H - H1) 
        return Tm * M / Mc

    if h > 120000 and h <= 1200000:
        if h > 120000 and h <= 140000:
            h1, T1, b = 120000, 334.42, 0.011259
        if h > 140000 and h <= 160000:
            h1, T1, b = 140000, 559.60, 0.006800
        if h > 160000 and h <= 200000:
            h1, T1, b = 160000, 695.60, 0.003970
        if h > 200000 and h <= 250000:
            h1, T1, b = 200000, 854.40, 0.001750
        if h > 250000 and h <= 325000:
            h1, T1, b = 250000, 941.90, 0.000570
        if h > 325000 and h <= 400000:
            h1, T1, b = 325000, 984.65, 0.000150
        if h > 400000 and h <= 600000:
            h1, T1, b = 400000, 995.90, 0.000020
        if h > 600000 and h <= 800000:
            h1, T1, b = 600000, 999.90, 0.0000005
        if h > 800000 and h <= 1200000:
            h1, T1, b = 800000, 1000.00, 0
        return T1 + b * (h - h1)

    return 1000


def density(h: float) -> float:
    """Return density [kg/m^3]."""
    R = 8.314  # universal gas constant
    P = pressure(h)
    M = molar_weight(h)
    T = temperature(h)
    return P * M / R / T


def molar_weight(h: float) -> float:
    """Return molar mass [kg/mole]."""
    Mc = 0.02896442  # molar weight of air at sea level
    if h <= 94000:
        return Mc
    if h > 1200000:
        return molar_weight(1200000)
    if h > 94000 and h <= 97000:
        M = 28.82 + 0.158 * sqrt(1 - 7.5e-08 * (h - 94000) * (h - 94000)) - 2.479e-04 * sqrt(97000 - h)
    if h > 97000 and h <= 97500:
        M = 28.91 - 0.00012 * (h - 97000)
    if h > 97500 and h <= 120000:
        M = 1000 * molar_weight(97500) - 0.0001511 * (h - 97500)
    if h > 120000 and h <= 1200000:
        if h > 120000 and h <= 250000:
            B0, B1, B2, B3 = 46.9083, -29.71210e-05, 12.08693e-10, -1.85675e-15
        if h > 250000 and h <= 400000:
            B0, B1, B2, B3 = 40.4668, -15.52722e-05, 3.55735e-10, -3.02340e-16
        if h > 400000 and h <= 650000:
            B0, B1, B2, B3 = 6.3770, 6.25497e-05, -1.10144e-10, 3.36907e-17
        if h > 650000 and h <= 900000:
            B0, B1, B2, B3 = 75.6896, -17.61243e-05, 1.33603e-10, -2.87884e-17
        if h > 900000 and h <= 1050000:
            B0, B1, B2, B3 = 112.4838, -30.68086e-05, 2.90329e-10, -9.20616e-17
        if h > 1050000 and h <= 1200000:
            B0, B1, B2, B3 = 9.89704838, -1.19732e-05, 7.78247e-12, -1.77541e-18
        M = B0 + (B1 * h) + (B2 * pow(h, 2)) + (B3 * pow(h, 3))
    return M * 0.001


def concentration(h: float) -> float:
    """Return concentration [m^-3]."""
    if h <= 120000:
        P = pressure(h)
        T = temperature(h)
        return 7.243611e+22 * P / T

    if h > 120000 and h <= 1200000:
        if h > 120000 and h <= 150000:
            A0, A1, A2, A3, A4, m = \
            0.210005867e+04, -0.561844757e-01, 0.5663986231e-06, -0.2547466858e-11, 0.4309844119e-17, 17
        if h > 150000 and h <= 200000:
            A0, A1, A2, A3, A4, m = \
            0.10163937e+04, -0.2119530830e-01, 0.1671627815e-06, -0.5894237068e-12, 0.7826684089e-18, 16
        if h > 200000 and h <= 250000:
            A0, A1, A2, A3, A4, m = \
            0.7631575e+03, -0.1150600844e-01, 0.6612598428e-07, -0.1708736137e-12, 0.1669823114e-18, 15
        if h > 250000 and h <= 350000:
            A0, A1, A2, A3, A4, m = \
            0.1882203e+03, -0.2265999519e-02, 0.1041726141e-07, -0.2155574922e-13, 0.1687430962e-19, 15
        if h > 350000 and h <= 450000:
            A0, A1, A2, A3, A4, m = \
            0.2804823e+03, -0.2432231125e-02, 0.8055024663e-08, -0.1202418519e-13, 0.6805101379e-20, 14
        if h > 450000 and h <= 600000:
            A0, A1, A2, A3, A4, m = \
            0.5599362e+03, -0.3714141392e-02, 0.9358870345e-08, -0.1058591881e-13, 0.4525531532e-20, 13
        if h > 600000 and h <= 800000:
            A0, A1, A2, A3, A4, m = \
            0.8358756e+03, -0.4265393073e-02, 0.8252842085e-08, -0.7150127437e-14, 0.2335744331e-20, 12
        if h > 800000 and h <= 1000000:
            A0, A1, A2, A3, A4, m = \
            0.8364965e+02, -0.3162492458e-03, 0.4602064246e-09, -0.3021858469e-15, 0.7512304301e-22, 12
        if h > 1000000 and h <= 1200000:
            A0, A1, A2, A3, A4, m = \
            0.383220e+02, -0.50980e-04, 0.181e-10, 0, 0, 11

        return (A0 + A1 * h + A2 * pow(h, 2) + A3 * pow(h, 3) + A4 * pow(h, 4)) * pow(10, m)

    return concentration(1200000)


def sound_speed(h: float) -> float:
    """Return sound of speed [m/s]."""
    R = 8.314  # universal gas constant
    M = molar_weight(h)
    T = temperature(h)
    return sqrt(1.4 * R * T / M)


def free_path(h: float) -> Union[float, None]:
    """Return mean free path [m]."""
    P = pressure(h)
    T = temperature(h)
    if P > 0:
        return 2.332376e-05 * T / P
    return None


def dynamic_viscosity(h: float) -> Union[float, None]:
    """Return dynamic viscosity [Pa*s]."""
    S = 110.4     # Sutherland coefficient for calculating dynamic viscosity
    b = 1.458e-6  # another Sutherland coefficient for calculating dynamic viscosity
    T = temperature(h)
    if h > 90000:
        return None
    return b * pow(T, 1.5) / (T + S)


def kinematic_viscosity(h: float) -> Union[float, None]:
    """Return kinematic viscosity [m^2/s]."""
    mu = dynamic_viscosity(h)
    if mu:
        return mu / density(h)
    return None


def thermal_conductivity(h: float) -> Union[float, None]:
    """Return thermal conductivity [W/(m*K)]."""
    if h > 90000:
        return None
    T = temperature(h)
    p = 12 / T
    return 2.648151e-03 * pow(T, 1.5) / (T + 245.4 * pow(10, -p))


def geopotential_height(h: float) -> float:
    """Return geopotential height [m]."""
    Rz = 6356767.0  # conventional radius of the Earth in meters
    if h == -Rz:
        return -Rz
    return Rz * h / (Rz + h)


def to_print(result: Union[float, None]) -> str:
    """Prepares the result for print."""
    if not result:
        return 'None'
    return f'{result:.3e}' if abs(result) < 1e-3 or abs(result) > 1e6 else f'{result:.3f}' 


def allcalc(h: str) -> Union[str, tuple]:
    try:
        h = float(h)
    except ValueError:
        return 'Please, enter height in meters.'
    if h < -2000 or h > 1200000:
        return 'Valid height range is\n[-2000; 1200000]\nin meter.'
    return (
        ('P', to_print(pressure(h)), 'Pa'),
        ('T', to_print(temperature(h)), 'K'),
        ('rho', to_print(density(h)), 'kg/m^3'),
        ('M', to_print(molar_weight(h)), 'kg/mole'),
        ('n', to_print(concentration(h)), 'm^-3'),
        ('a', to_print(sound_speed(h)), 'm/s'),
        ('l', to_print(free_path(h)), 'm'),
        ('mu', to_print(dynamic_viscosity(h)), 'Pa*s'),
        ('nu', to_print(kinematic_viscosity(h)), 'm^2/s'),
        ('W', to_print(thermal_conductivity(h)), 'W/(m*K)'),
        ('H', to_print(geopotential_height(h)), 'm')
    )


if __name__ == '__main__':
    import sys

    if len(sys.argv) != 2:
        print('You must specify height in meter.')
        sys.exit(1)

    print(allcalc(sys.argv[1]))