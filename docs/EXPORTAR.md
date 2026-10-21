# Exportar: Octave (.txt) y Excel (.xlsx)

Los **6 métodos** exportan desde la Fase 8. El menú se abre con el ícono de guardar (flecha a la bandeja) de la barra superior.

- Solo aparece cuando hay resultado.
- Si no se calculó nada (error de entrada), queda el "?" de ayuda.
- Si un sistema **no converge** o diverge, sí se exporta: la tabla de iteraciones sirve para ver qué pasó.

## Dónde está el código

| Qué | Dónde |
|---|---|
| Qué exporta cada método (registro) | `catalogo/Catalogo.kt`: `generador(id)`, `puedeExportarExcel(id)`, `puedeExportarOctave(id)` |
| Generadores por método | `exportar/GeneradoresMetodos.kt` (`GeneradorRegresionPolinomial`, `…Multiple`, `GeneradorLagrange`, `GeneradorNewtonInterpolacion`, `GeneradorPuntoFijo`, `GeneradorNewtonSistemas`) |
| Fachada para la interfaz (disponible, nombre, bytes) y piezas comunes de las hojas | `exportar/Exportaciones.kt` |
| Expresiones de la app → Octave | `exportar/TraductorOctave.kt` |
| Código Octave de la regresión polinomial | `exportar/GeneradorOctave.kt` |
| ZIP "stored" + CRC32 / .xlsx mínimo | `exportar/Zip.kt`, `exportar/Xlsx.kt` |
| Interfaz de guardado (`ExportadorArchivos`) y Android | `exportar/ExportadorArchivos.kt`, `androidApp/.../ExportadorAndroid.kt` |

Todo se genera desde la **misma `SalidaMetodo`** que dibuja la pantalla, más la entrada original (los textos del alumno, p. ej. las funciones de un sistema). Los comentarios numerados del .txt (`% 1. …`) salen de los títulos de los pasos de la pantalla. La única excepción es la regresión polinomial, cuyo texto se fijó en la Fase 7.

Para regenerar los .txt y .xlsx de los 6 ejemplos en `shared/build/exportados/`, junto con un volcado de los pasos (`pasos_<método>.txt`), ejecuta `gradlew.bat :shared:testAndroidHostTest`.

## Nombres de archivo

| Método | .txt / .xlsx |
|---|---|
| Punto fijo para sistemas | `punto_fijo_sistemas` |
| Newton-Raphson para sistemas | `newton_raphson_sistemas` |
| Regresión polinomial | `regresion_polinomial_grado{m}` |
| Regresión múltiple | `regresion_multiple_k{k}` |
| Polinomio de Lagrange | `lagrange_grado{g}` |
| Newton (diferencias divididas) | `newton_dif_divididas_grado{g}` |

## Hojas de cada .xlsx

| Método | Hojas |
|---|---|
| Regresión polinomial | Datos y sumatorias (Σ con =SUM) · Sistema normal · Resultados · Entrada |
| Regresión múltiple | Datos y sumatorias (Σ con =SUM) · Sistema normal (XᵀX, Xᵀy, (XᵀX)⁻¹, β) · Resultados · Entrada |
| Lagrange | Lagrange (i, xᵢ, yᵢ, Lᵢ, yᵢLᵢ; Σ con =SUM: Σ Lᵢ = 1 y Σ yᵢLᵢ = P(x)) · Resultados · Entrada |
| Newton (dif. divididas) | Diferencias divididas (triangular; 0.0000000 si los datos tienen 7 decimales) · Términos (k, coeficiente, factor, término, suma acumulada; Σ =SUM) · Resultados · Entrada |
| Punto fijo | Iteraciones (valores, no fórmulas) · Resultados · Entrada (funciones como texto, x⁽⁰⁾, tol, máx. iteraciones) |
| Newton-Raphson | Iteraciones · Primera iteración (F, J, Δ) · Resultados · Entrada |

La hoja **Resultados** tiene siempre las columnas Paso | Resultado | Fórmula | Valor, con una fila por resultado de cada paso.

**Validación:**
- Los 6 libros se abrieron con **openpyxl 3.1.5**: hojas, fórmulas `=SUM`, valores en caché y formatos `0.000000` / `0.0000000`.
- El zip pasó `zipfile.testzip()`.
- **No** se abrieron en Excel ni en LibreOffice (no están instalados).

## Octave: salidas esperadas de los ejemplos

**Ningún .txt se ejecutó en Octave**: no hay Octave instalado en el equipo de desarrollo. Para comprobarlos a mano:
1. Pega el .txt en Octave o en https://octave-online.net.
2. Compara lo que imprime con lo de abajo. La última cifra de los errores en notación científica puede variar.

**Regresión polinomial** (x = 1…5, grado 2, x a predecir 2.5):

```
a0 = 12.300000
a1 = -3.244286
a2 = 0.235714
Q = 0.905143
R2 = 0.974265
r = 0.987049
S = 0.672734
P(2.5) = 5.662500
```

**Regresión múltiple** (ejemplo del curso, punto (1, 1)):

```
beta0 = 5.000000
beta1 = 4.000000
beta2 = -3.000000
Q = 0.000000
R2 = 1.000000
r = 1.000000
S = 0.000000
Prediccion = 6.000000
```

**Lagrange** (2.2, 2.4, 2.6; x = 2.5). También abre la gráfica con los nodos, la curva y el punto evaluado.

```
L0(2.5) = -0.125000
L1(2.5) = 0.750000
L2(2.5) = 0.375000
Suma de Lk = 1.000000
P(2.5) = 0.498212
```

**Newton (diferencias divididas)** (2, 2.2, …, 2.8; x = 2.1). Primero muestra la matriz D y después imprime la evaluación por grado; al final abre la gráfica.

```
P0(2.1) = 0.510376   (termino 0.510376)
P1(2.1) = 0.515580   (termino 0.005204)
P2(2.1) = 0.518177   (termino 0.002597)
P3(2.1) = 0.518306   (termino 0.000129)
P4(2.1) = 0.518294   (termino -0.000013)
```

**Punto fijo** (G₁ = 0.5·cos(y), G₂ = 0.5·sin(x), x⁽⁰⁾ = (0, 0), tol 1e-8). Imprime una línea por iteración y, al final, la gráfica semilogarítmica del error.

```
Convergio en 13 iteraciones
x = 0.486405
y = 0.233725
Error final E = 5.063373e-09
```

**Newton-Raphson** (F₁ = x²+y²−4, F₂ = x−y, x⁽⁰⁾ = (1, 1), tol 1e-8). En la primera vuelta muestra F(x0) = [-2; 0], J(x0) = [2 2; 1 -1] y delta = [0.5; 0.5].

```
Convergio en 5 iteraciones
x = 1.414214
y = 1.414214
Error final E ≈ 1.6e-12
```

## Traducción de funciones a Octave (sistemas)

`TraductorOctave` lee la expresión con la **misma gramática** que el parser de la app:

| En la app | En Octave |
|---|---|
| `2x`, `3sin(x)`, `2(x+1)`, `(x+1)(x-1)` | `2*x`, `3*sin(x)`, `2*(x + 1)`, `(x + 1)*(x - 1)` |
| `xy` (variables de una letra) | `x*y` |
| `2^3^2` (asociativa a la derecha) | `2^(3^2)` (en Octave `^` va de izquierda a derecha) |
| `x^-1` | `x^(-1)` |
| `--x` | `-(-x)` (`--` sería un decremento en Octave) |
| `ln(x)` / `log(x)` (base 10) | `log(x)` / `log10(x)` |
| `e` / `pi` | `exp(1)` / `pi` |
| coma decimal `2,5` | `2.5` |
| sin, cos, tan, asin, acos, atan, exp, sqrt, abs | igual |

- **Cada ecuación** se convierte en una función anónima con parámetros con nombre, `g1 = @(x, y) 0.5*cos(y);`, y el bucle usa `G = @(v) [g1(v(1), v(2)); …]`. No se sustituye texto de variables a mano.
- **Si algo no se puede traducir** (una variable que es palabra reservada de Octave, como `end` o `if`, o una expresión inválida), se escribe `% ATENCIÓN: no se pudo traducir <token>` y la línea original comentada, en lugar de código inválido.
- **Prueba de ida y vuelta:** el test `TraductorOctaveTest.mismosValoresQueLaApp` convierte la traducción de nuevo a la sintaxis de la app y comprueba que da los mismos valores en varios puntos.

## Limitaciones conocidas

- **Rotar con el selector abierto:** el archivo se guarda igual (los bytes viven en un ViewModel) y el aviso sale como **Toast** en lugar del Snackbar.
- **Si Android cierra el proceso** con el selector abierto, no se guarda nada.
- **"ABRIR"** abre el archivo con la app que el sistema tenga para ese tipo. Si no hay ninguna, dice "No hay una app para abrir este archivo".
- **Octave sin probar:** los .txt usan solo funciones de Octave (endfor, endif, printf, polyval, norm, semilogy, linspace) y no se ejecutaron en Octave real.
