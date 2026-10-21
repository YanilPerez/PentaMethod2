# QA manual — Pentametod2 (emulador Pixel 7) · diseño "cuaderno de cálculo limpio"

Instalar: `gradlew.bat :androidApp:installDebug`.

Las medidas están en dp y los colores son los tokens de `docs/DISENO.md`. Entre paréntesis va el valor en claro / oscuro.

> Para medir: Opciones de desarrollador → "Mostrar límites de diseño". Para el oscuro: `adb shell cmd uimode night yes`. Para fuente grande: `adb shell settings put system font_scale 1.3`.

## 0. Generales (todas las pantallas)
- [ ] No hay sombras: las tarjetas tienen borde de 1dp outlineVariant (#C3CBCD / #3F484B), fondo surfaceContainerLowest (#FFFFFF / #171D1F) y radio 16.
- [ ] El fondo de pantalla es #F8FAFA / #0F1416.
- [ ] El texto de la interfaz está en **IBM Plex Sans**, y los números y fórmulas en **JetBrains Mono**.
- [ ] Ningún texto dice "Semana".
- [ ] El contenido no queda debajo de la barra de estado ni de la barra de navegación.
- [ ] No aparece ningún "▯". Algunos subíndices y superíndices (ᵢ ₖ ⁽ᵏ⁾) se ven en la fuente del sistema, un poco distintos. Es esperado (ver `docs/FUENTES.md`).

## 1. Menú
- [ ] Arriba, "PENTAMETOD2" en mono 12/600, espaciado 0.08em, color primary (#0E5E6F / #84D2E3). A la derecha, el botón ⓘ (48dp) abre "Acerca de" con el nombre, una frase y "Versión 1.0".
- [ ] El título "Métodos numéricos" es de 28/36 en 600. Debajo, "Elige un método para empezar a calcular." en 14/20 onSurfaceVariant.
- [ ] Hay tres secciones de 16/24 en 600: **Sistemas no lineales**, **Ajuste de curvas** e **Interpolación**, con 20 de separación.
- [ ] Cada tarjeta mide ≥ 72 de alto, tiene radio 16 y borde de 1dp, y van separadas 8.
- [ ] El tile es de 44×44, radio 12, fondo primaryContainer (#CDE7EC / #004E5C) y glifo mono 13/600: G(x), J⁻¹, Pₘ, βₖ, Lₖ, f[ ].
- [ ] El título de la tarjeta es de 16/22 en 500. La descripción ocupa **una línea** con "…" y el chevron › es de 24, color #6F7B7F / #9AA5A9.

## 2. Pantalla de método · edición (los 6 métodos)
- [ ] La barra superior mide ≥ 64: ← (48, circular), el título en 22/28 (600) y el botón ? (48), que abre la ayuda del método.
- [ ] Bajo el título va una línea 13/18 onSurfaceVariant: "<sección> · <frase corta>". Por ejemplo, "Ajuste de curvas · mínimos cuadrados" o "Sistemas no lineales · itera x = G(x)". Ya **no** hay chip de categoría.
- [ ] Las tarjetas de entrada no llevan "ENTRADA / Datos" (en sistemas sigue el título "Ecuaciones" con su contador).
- [ ] Los campos de interpolación, regresión múltiple y sistemas siguen como antes: 56 de alto, radio 4, label flotante y mono 16.
- [ ] **Dock inferior fijo:**
  - [ ] Tiene un borde superior de 1dp outlineVariant, fondo de pantalla y padding 16 a los lados.
  - [ ] "Cargar ejemplo" es un botón de texto (sin borde, primary) y "Calcular" un botón relleno de 48 con flex 1.
  - [ ] El contenido se desplaza por encima del dock y la última tarjeta no queda tapada.
  - [ ] Con el teclado abierto, el dock sube sobre el teclado y sigue visible.
  - [ ] Queda por encima de la barra de navegación (gestos y 3 botones).
  - [ ] En modo resultado (entrada comprimida) el dock no aparece.
- [ ] Sin resultado aparece una línea de ayuda de una fila: borde punteado de 1dp, radio 12, ⓘ y "Verás fórmula, resumen, gráfica y tabla de pasos.". Desaparece al haber resultado.

## 3. Formularios (los 4 tipos)
- [ ] **Regresión polinomial (propuesta C):**
  - [ ] **Tarjeta de datos:**
    - [ ] Pestañas "Lista | Tabla" en chips de 32; la activa va en primaryContainer.
    - [ ] Campos compactos: etiqueta de 12 arriba, caja de 44, radio 8, borde de 1dp y valor mono 14.
  - [ ] **Lista:**
    - [ ] Junto a "Valores x" y "Valores y" va la píldora "N valores", verde si es válida.
    - [ ] x = `1,2,3,4,5` con y = `9,7.5,4.2,3` pone ambas píldoras en rojo. Debajo aparece "x tiene 5 valores y y tiene 4. Deben ser iguales.".
    - [ ] y = `9,abc` hace que la píldora diga "Revisa el valor 2".
    - [ ] Bajo y va la ayuda "Separa con comas y usa punto decimal.".
    - [ ] Al teclear rápido no se pierden ni se reordenan caracteres.
  - [ ] **Tabla:**
    - [ ] Columnas #, x, y, con el encabezado en primaryContainer y una × por fila (área de 44).
    - [ ] Arriba a la derecha aparece la píldora "✓ N puntos".
    - [ ] "+ Agregar punto" añade una fila vacía.
    - [ ] "Pegar de Excel" reemplaza la tabla con dos columnas copiadas (tab, espacios o ';'; un encabezado "x y" se ignora).
    - [ ] Con un portapapeles inválido (una sola columna o vacío) aparece un mensaje rojo breve y no se toca la tabla.
  - [ ] **Lista ↔ Tabla:**
    - [ ] Cambiar de pestaña no pierde datos.
    - [ ] Con longitudes distintas, la tabla rellena con celdas vacías.
    - [ ] Al volver a Lista, las celdas vacías se descartan.
  - [ ] **Tarjeta de ajuste:**
    - [ ] Grado segmentado 1…G con G = min(5, n−1). Con n < 2 solo aparece "1", deshabilitado.
    - [ ] Debajo dice "Grado máximo permitido con n puntos: G.".
    - [ ] Grado 4 con 5 puntos: al quitar 2 filas en Tabla, el grado baja solo a 2.
    - [ ] Campo "x a predecir (opcional)"; al entrar en él, el texto queda seleccionado.
    - [ ] Recuadro primaryContainer "SE AJUSTARÁ" con la fórmula en mono. Grado 2 da `y = a₀ + a₁x + a₂x²`, y la fórmula cambia con el grado.
  - [ ] **Calcular con errores:**
    - [ ] No calcula y enfoca el primer campo (o celda) con error.
    - [ ] Muestra el mensaje: vacío, "Revisa el valor N de …" o "Se necesitan al menos 2 puntos…".
    - [ ] Los errores del motor (p. ej. x a predecir `abc`) salen como antes, en el banner.
  - [ ] "Cargar ejemplo" conserva la pestaña (Lista o Tabla) en la que estaba el alumno.
- [ ] **Interpolación** (Lagrange y Newton):
  - [ ] Campos "Nodos x", "Valores y" y "x a evaluar".
  - [ ] Segmentado Manual | Automático: 48 de alto, radio 24, borde de 1dp; el seleccionado va en primaryContainer con un check de 16.
  - [ ] "Grado" solo aparece en Automático.
- [ ] **Regresión múltiple:**
  - [ ] Segmentado k = 1…4.
  - [ ] Cuadrícula con encabezados x₁…xₖ, y (mono 13/600) y celdas de 96 con el número alineado a la derecha.
  - [ ] ⊖ (48) quita una fila sin bajar de 3; "Agregar fila" es un botón tonal (fondo primaryContainer).
  - [ ] Campo "Valores a predecir (opcional)".
- [ ] **Sistemas** (punto fijo y Newton):
  - [ ] Tarjeta "Ecuaciones" con "F(x) = 0" en mono (en punto fijo, "G(x)") y el contador "2 de 2" (mono 12) a la derecha.
  - [ ] Cada ecuación es un campo con label "F₁(x, y)" y prefijo "=" (en punto fijo, label "G₁(x, y)" y prefijo "x =") más ⊖ de 48. Las ecuaciones van separadas 16.
  - [ ] "Agregar ecuación" es tonal; con 4 ecuaciones se deshabilita.
  - [ ] Tarjeta "Parámetros": Variables | Valores iniciales x₀, luego Tolerancia | Máx. iteraciones.
  - [ ] La línea "Error: norma infinito ‖·‖∞" va en 12/16.
  - [ ] Caja informativa: fondo surfaceContainerHigh, radio 8, ícono ⓘ de 16 y texto 12/18.

## 4. Resultado · Regresión polinomial (Cargar ejemplo → Calcular)
- [ ] Mientras calcula aparece una barra de progreso fina (4dp, primary) bajo la barra superior; el dock desaparece al pasar a la vista de resultado.
- [ ] La tarjeta de datos se comprime:
  - [ ] Muestra "ENTRADA", "Datos" y el botón de texto "✎ Editar".
  - [ ] Debajo, la lista mono 14: `x 1, 2, 3, 4, 5`, `y 9, 7.5, 4.2, 3, 2.1`, `Grado 2 · x a predecir 2.5`.
  - [ ] "Editar" vuelve al formulario con lo escrito.
- [ ] Fila "PASOS" (mono 12/600, primary) con una línea de 1dp y el chip "✓ Calculado" a la derecha.
- [ ] Ya **no** están la tarjeta "EXPLICACIÓN / Fórmula" ni la tarjeta "Resumen".
- [ ] **Lista de pasos:**
  - [ ] Borde de 1dp outlineVariant, radio 16, filas separadas por una línea.
  - [ ] Cada fila mide ≥ 52 y lleva un círculo de 28 en primaryContainer con el número, el título (15/500), el resumen a la derecha (mono 600) y el chevron ›.
  - [ ] Orden y resúmenes:
    1. Sistema normal: `C·a = b`.
    2. Ecuación del polinomio: `a₀, a₁, a₂`.
    3. Suma de cuadrados de residuos: `0.905143`.
    4. Media y variación de y: `ȳ = 5.160000`.
    5. Coeficiente de determinación: `0.974265`.
    6. Error estándar: `0.672734`.
    7. Predicción: `5.662500`, en terracota (valorClave).
  - [ ] Al entrar, solo el paso 1 está abierto.
  - [ ] Una fila abierta lleva fondo gris claro (filaAlterna), el título en 600 y el chevron hacia abajo.
  - [ ] Tocar una fila la abre o la cierra con una animación de altura de ~200 ms. Con "Quitar animaciones" en Accesibilidad, cambia sin animación.
  - [ ] Se pueden tener varios pasos abiertos a la vez. Al rotar, siguen abiertos los mismos.
  - [ ] TalkBack lee cada fila como botón, con "Expandido" o "Contraído", y el chevron como "Abrir …" o "Cerrar …".
  - [ ] **Contenido de un paso abierto**, siempre en este orden:
    - una frase de explicación;
    - "FÓRMULA" con una caja mono;
    - "CON TUS DATOS" con una caja mono y/o matrices;
    - la franja de resultado (primaryContainer; en la predicción, tertiaryContainer con texto terracota).
  - [ ] Las fórmulas largas pasan a la línea siguiente: nunca se cortan ni se desplazan en horizontal. Las matrices sí se desplazan si no caben.
  - [ ] **Paso 1:** C, b, C⁻¹ con corchetes y la caja de "a" con "← a₀ ← a₁ ← a₂". Franja: `P₂(x) = 12.300000 − 3.244286x + 0.235714x²`.
  - [ ] **Paso 3:** una línea por punto, `(P(1) − 9)² = (… − 9)² = …`, y luego la suma; Q = 0.905143.
  - [ ] **Paso 5:** franja con R² = 0.974265 y r = 0.987049.
  - [ ] **Paso 7:**
    - `P(2.5) = 12.300000 + (-3.244286)(2.5) + 0.235714(2.5)²`
    - `= 12.300000 − 8.110714 + 1.473214`
    - `= 5.662500`
  - [ ] Sin "x a predecir" solo hay 6 pasos (no está "Predicción").
- [ ] **Después de los pasos, en este orden:** Gráfica → Datos y sumatorias → Conclusión.
- [ ] **Gráfica:**
  - [ ] Título "Gráfica" y debajo "Ajuste polinomial" en 12.
  - [ ] Cuadrícula de 1dp (#E1E7E8 / #2A3235). Solo hay eje X abajo y eje Y a la izquierda (#5F6B6F / #899295).
  - [ ] Números mono 11, nombre del eje "x" en cursiva al final del eje.
  - [ ] Datos: círculos azules (#0072B2 / #56B4E9) de radio 5 con halo.
  - [ ] Curva verde (#009E73 / #3DCB9E) de 2dp.
  - [ ] Predicción: anillo de radio 11, punto de 6.5, guías punteadas a los dos ejes y la etiqueta "(2.5, 5.6625)" mono 11/600 en valorClave.
  - [ ] La leyenda va centrada: ● Datos, — P(x), ◎ Predicción.
  - [ ] Al tocar aparece el recuadro con el punto más cercano.
- [ ] **Datos y sumatorias:**
  - [ ] Título y "11 columnas" (12).
  - [ ] Caja con borde de 1dp y radio 8. El encabezado mide 40, fondo surfaceContainerHigh, mono 13/600 a la derecha.
  - [ ] La **columna "i" queda fija** al desplazar y tiene borde derecho.
  - [ ] Filas de 40 con zebra (#F5F8F8 / #1B2224).
  - [ ] La fila **Σ** mide 44, fondo primaryContainer, texto 600 y borde superior de 2dp; muestra Σxᵢ = 15 y Σxᵢ² = 55.
  - [ ] Debajo, la barra de 4dp (pista de 120) y "Desliza para ver las 11 columnas →".
- [ ] **Conclusión:**
  - [ ] Tarjeta sin borde, fondo primaryContainer, radio 16, padding 20/16.
  - [ ] Ícono ✓ en círculo, "Conclusión" y el texto 16/24 con **97.43%** en negrita.
  - [ ] Barra de R² de 6dp (pista #A6CDD5 / #1E6B7A) con "R² 0 … 1" en mono 12.
- [ ] Ya no hay botón "Copiar resultados" abajo: está en el menú (4b).

## 4b. Menú de guardar / exportar (todos los métodos)
- [ ] Sin resultado, la barra muestra el "?" de ayuda.
- [ ] Con resultado, el "?" se cambia por el ícono de guardar (flecha hacia una bandeja). TalkBack lo lee "Guardar o exportar".
- [ ] **El menú:**
  - [ ] Mide ~312 de ancho, radio 12, borde de 1dp y sombra suave.
  - [ ] Cada ítem mide ≥ 56 y lleva ícono a la izquierda.
  - [ ] Ítems:
    - "Exportar a Excel" (".xlsx con datos, sumatorias y resultados")
    - "Exportar código Octave" (".txt listo para pegar en Octave")
    - "Copiar resultados" ("Resumen y tablas, separados por tabulaciones")
    - un separador
    - "Ayuda del método"
- [ ] Desde la Fase 8, los 6 métodos tienen las 4 opciones habilitadas. Los nombres de archivo y las hojas de cada uno están en la sección 5 y en `docs/EXPORTAR.md`.
- [ ] Con un error de entrada no hay ícono de guardar (queda el "?").
- [ ] "Copiar resultados" copia al portapapeles y muestra el Snackbar "Copiado: resumen y tablas, separados por tabulaciones.".
- [ ] "Ayuda del método" abre el mismo diálogo que antes daba el "?".
- [ ] **Exportar código Octave:**
  - [ ] Se abre el selector "Guardar" del sistema con el nombre `regresion_polinomial_grado2.txt`.
  - [ ] Al guardar aparece el Snackbar "Guardado: regresion_polinomial_grado2.txt" con la acción "ABRIR".
  - [ ] En `Download/` (o la carpeta elegida) está el archivo.
- [ ] **Exportar a Excel:** igual, con `regresion_polinomial_grado2.xlsx`. Sin visor de .xlsx instalado, "ABRIR" dice "No hay una app para abrir este archivo".
- [ ] **Cancelar** con atrás en el selector: no aparece ningún aviso.
- [ ] **Rotar con el selector abierto y guardar:** el archivo se guarda y aparece un Toast "Guardado: …" en lugar del Snackbar (limitación documentada en `docs/EXPORTAR.md`).
- [ ] Sin "x a predecir", el .txt no trae la sección "% 6. Predicción" y el .xlsx no tiene la fila P(x) en "Resultados".
- [ ] **Comprobación manual de los archivos** (ver `docs/EXPORTAR.md`):
  - [ ] Pegar el .txt en Octave u Octave Online: imprime Q = 0.905143, R2 = 0.974265, S = 0.672734 y P(2.5) = 5.662500.
  - [ ] Abrir el .xlsx en Excel o LibreOffice:
    - aparecen las 4 hojas;
    - la fila Σ es fórmula (=SUM);
    - los valores coinciden con la pantalla.

## 5. Los otros 5 métodos con lista de pasos (Fase 8)

Mismas reglas que la sección 4 en los 6 métodos:
- La pantalla va en este orden: ENTRADA compacta → PASOS + chip → banners → lista (solo el paso 1 abierto) → Gráfica → tabla principal → Conclusión.
- No aparecen las tarjetas "EXPLICACIÓN / Fórmula" ni "Resumen".
- Cada paso lleva explicación, FÓRMULA, CON TUS DATOS y la franja de resultado. Las fórmulas largas pasan de línea.
- El último paso va destacado, con el resumen en terracota.
- En el menú, las 4 opciones están habilitadas.

### 5a. Regresión múltiple (ejemplo del curso, predicción (1, 1))
- [ ] **Pasos:**
  1. Modelo y sistema normal: `(XᵀX)·β = Xᵀy`.
  2. Ecuación del modelo: `β₀, β₁, β₂`.
  3. Suma de cuadrados de residuos: `0.000000`.
  4. Media y variación de y: `ȳ = 9.000000`.
  5. Coeficiente de determinación: `1.000000`.
  6. Error estándar.
  7. Predicción: `6.000000`.
- [ ] El paso 1 muestra XᵀX, Xᵀy y (XᵀX)⁻¹ con corchetes, y β en su caja con "← β₀ ← β₁ ← β₂".
- [ ] Sin punto a predecir solo hay 6 pasos.
- [ ] Gráfica "Real vs predicho". Debajo, la tabla "Datos y residuos" con fila Σ, y al final la conclusión con la barra de R².
- [ ] **Exportar:**
  - [ ] Nombres `regresion_multiple_k2.txt` / `.xlsx`.
  - [ ] Hojas: Datos y sumatorias, Sistema normal, Resultados, Entrada.

### 5b. Polinomio de Lagrange (2.2, 2.4, 2.6; x = 2.5)
- [ ] **Pasos:**
  1. Nodos usados (`grado 2`), con una tabla k | xₖ | yₖ que muestra los 7 decimales de los datos.
  2. Polinomio de Lagrange.
  3. Cada Lₖ(x) con tus datos: L₀ = −0.125, L₁ = 0.75, L₂ = 0.375.
  4. Evaluación: `0.498212`, con la línea "Comprobación: Σ Lₖ(2.5) = 1.000000".
- [ ] En modo Automático, el paso 1 dice "se eligieron los n nodos más cercanos a x = … (de los N que diste)".
- [ ] Debajo, la gráfica, la tabla "Nodos y polinomios de Lagrange" con Σ (Σ Lᵢ = 1) y la conclusión "El polinomio de grado 2 que pasa por los 3 nodos da P(2.5) = 0.498212.".
- [ ] **Exportar:**
  - [ ] `lagrange_grado2.txt` / `.xlsx`.
  - [ ] Hojas: Lagrange, Resultados, Entrada.
  - [ ] El .txt dibuja la curva con `plot`.

### 5c. Newton · diferencias divididas (2 … 2.8; x = 2.1)
- [ ] **Pasos:**
  1. Nodos usados.
  2. Tabla de diferencias divididas (`5×5`):
     - lleva dentro la tabla triangular, con celdas vacías bajo la diagonal y la columna k fija;
     - muestra el cálculo de la primera fila, con los negativos entre paréntesis.
  3. Polinomio de Newton.
  4. Términos: una línea por k con su factor.
  5. Evaluación por grado: P₀ … P₄ = 0.510376, 0.515580, 0.518177, 0.518306, **0.518294**.
- [ ] La tabla triangular no se repite debajo de la lista; debajo va la tabla "Términos de Newton" con Σ.
- [ ] **Exportar:**
  - [ ] `newton_dif_divididas_grado4.txt` / `.xlsx`.
  - [ ] Hojas: Diferencias divididas (7 decimales), Términos (con suma acumulada y Σ =SUM), Resultados, Entrada.

### 5d. Newton-Raphson para sistemas (ejemplo)
- [ ] La entrada comprimida muestra `F₁ x^2+y^2-4 = 0`, `F₂ x-y = 0`, `x₀ (1, 1)` y `Criterio tol 1e-8 · máx 50`.
- [ ] **Pasos:**
  1. Sistema F(x) = 0.
  2. Jacobiano: aviso de diferencias centrales, "hⱼ = 6·10⁻⁶ · max(1, |xⱼ|)" y J(x⁽⁰⁾).
  3. Método de Newton.
  4. Primera iteración: `0.500000`, con F(x⁽⁰⁾), J(x⁽⁰⁾) y Δ con corchetes, y x⁽¹⁾ = (1.5, 1.5).
  5. Resultado: `k = 5`, con x* = y* = 1.414214.
- [ ] **Gráfica de error:**
  - [ ] Subtítulo "Error por iteración · Escala logarítmica".
  - [ ] El eje Y se marca con superíndices (10⁰, 10⁻³, …).
  - [ ] La serie E(k) es roja, línea con puntos, con la etiqueta "E₅ = 1.59e-12".
  - [ ] La tolerancia es una línea verde discontinua con el rótulo "tolerancia 1e-8".
- [ ] En la tabla "Iteraciones", las filas miden 36 y la columna k queda fija. Debajo están los 5 acordeones "Detalle de la iteración k", contraídos.
- [ ] **Exportar:**
  - [ ] `newton_raphson_sistemas.txt` / `.xlsx`.
  - [ ] Hojas: Iteraciones, Primera iteración (F, J, Δ), Resultados, Entrada.
  - [ ] El .txt define `F1 = @(x, y) x^2 + y^2 - 4;` y usa `delta = J\(-Fx);`.

### 5e. Punto fijo para sistemas (ejemplo)
- [ ] **Pasos:**
  1. Sistema y función de iteración: `x = 0.5*cos(y)`, `y = 0.5*sin(x)`.
  2. Punto inicial y criterio: `tol = 1e-8`, con x⁽⁰⁾ = (0, 0) y "máximo de iteraciones = 50".
  3. Primera iteración: `0.500000`.
  4. Resultado: `k = 13`, con "Convergió en 13 iteraciones (E < 1e-8)", x* = 0.486405 e y* = 0.233725.
- [ ] **Exportar:**
  - [ ] `punto_fijo_sistemas.txt` / `.xlsx`.
  - [ ] Hojas: Iteraciones (valores), Resultados, Entrada (funciones como texto).
  - [ ] El .txt define `g1 = @(x, y) 0.5*cos(y);` y termina con `semilogy`.

## 6. Punto fijo divergente (G₁ = `2x+y`, G₂ = `2y+x`, x₀ = `1,1`)
- [ ] El chip del separador dice "No convergió" (errorContainer). El estado sigue siendo el de antes de la Fase 8.
- [ ] **Banner de error:**
  - [ ] Fondo errorContainer (#F9DEDC / #8C1D18), sin borde, radio 16 e ícono octágono en color error.
  - [ ] Título "El método diverge o no convergió" (16/22·600) y el mensaje del método.
  - [ ] Acciones a la derecha: **Cambiar despeje** vuelve a la edición y **Probar Newton-Raphson** abre ese método.
- [ ] La lista de pasos sigue ahí. El paso 4 "Resultado" dice "No convergió", repite el aviso del método y muestra el último valor.
- [ ] La gráfica muestra el error creciendo hasta 10¹² y la etiqueta "E₂₆ = 1.69e12".
- [ ] La **última fila** de la tabla de iteraciones tiene fondo filaUltimaError (#FCEEEE / #3B2224) y la columna E en rojo 600 (desliza a la derecha para verla).
- [ ] El ícono de guardar sí aparece y la exportación funciona: hay iteraciones que revisar.
- [ ] Error de entrada (p. ej. la ecuación `x^2+`):
  - chip "Con errores";
  - banner "No se pudo calcular" con "Ecuación 1: …" y la acción "Editar datos";
  - **no** hay ícono de guardar: queda el "?".

## 7. Aviso de mal condicionamiento (hoja "Resolver")
x = `176,168,202,138,213,159,193,122,185,153`, y = `448,556,844,427,811,398,447,154,534,313`, Grado `2` (ya viene elegido por defecto con 10 puntos).
- [ ] El chip dice "Con avisos" (warningContainer #FFEFC7, texto warning).
- [ ] **Banner de aviso:**
  - [ ] Fondo warningContainer, borde de 1dp (#E5C26B / #B08A2E), radio 16 y triángulo en color warning.
  - [ ] Título "El sistema está mal condicionado (número de condición ≈ 1.37e12)…".
  - [ ] La acción **Entendido** (#6B4600 / #F5C35B) lo oculta.
- [ ] Los resultados se ven igual debajo.

## 8. Modo oscuro (repetir 1–7, incluidas las subsecciones 5a–5e)
- [ ] Todos los tokens cambian a su valor oscuro.
- [ ] Los textos se leen bien sobre las tarjetas (#171D1F).
- [ ] Los chips y banners mantienen su significado (verde, ámbar, rojo).
- [ ] La gráfica usa la paleta oscura (#56B4E9, #3DCB9E, #FFB68F, error #F2B8B5, tolerancia #9BD3A6).
- [ ] (Conocido) La pantalla de arranque del sistema es clara aunque el teléfono esté en oscuro (tema del manifiesto, ver el reporte).

## 9. Fuente grande (1.3×)
- [ ] Todo el texto crece y nada se superpone.
- [ ] En el dock, "Cargar ejemplo" pierde su ícono y ambos botones caben en una fila sin recortar el texto.
- [ ] La barra superior crece para el subtítulo y no recorta el título ni el subtítulo en vertical.
- [ ] El segmentado de grado (hasta 5 botones) no corta los números.
- [ ] Las tablas ensanchan sus columnas y mantienen la columna fija y la barra de desplazamiento.
- [ ] Las etiquetas de la gráfica no se cortan.
- [ ] En la lista de pasos, los títulos largos pasan a dos líneas y los resúmenes cortos ("k = 13", "0.500000") se leen enteros.
- [ ] (Conocido, anterior a la Fase 8) En la gráfica de error de punto fijo, el rótulo "E₁₃ = …" se encima con "tolerancia 1e-8". En la entrada comprimida, "Criterio" y "Variables" se parten en dos líneas.
- [ ] El título largo de la barra ("Newton-Raphson para sistemas") se recorta con "…" en una línea; es esperado.

## 10. Rotación
- [ ] Con el formulario escrito, al rotar se conserva todo. En regresión polinomial se conservan la pestaña (Lista o Tabla), las celdas vacías, las longitudes distintas, el grado y el texto tal como se tecleó.
- [ ] Con resultados, al rotar se vuelve a la vista de resultados con la entrada comprimida, recalculada en segundo plano.
- [ ] Se conserva el estado de la explicación (abierta o cerrada) y, en regresión polinomial, qué pasos están abiertos.
- [ ] En horizontal, el contenido queda centrado con un máximo de 720 y respeta el recorte de pantalla.
- [ ] Al volver al menú y entrar otra vez al método, se conserva su estado.
