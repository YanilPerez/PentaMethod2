# Diseño "cuaderno de cálculo limpio": dónde se cambia cada token

Todo el tema vive en `shared/src/commonMain/kotlin/com/example/pentametod2/ui/tema/`. Los componentes de `ui/` no tienen colores ni medidas sueltas: los leen de aquí.

## Colores → `ui/tema/Color.kt`

| Token | Dónde |
|---|---|
| primary, onPrimary, primaryContainer, onPrimaryContainer | `EsquemaClaro` / `EsquemaOscuro` (ColorScheme de M3) |
| tertiary, tertiaryContainer, onTertiaryContainer | ídem |
| background, surface, onSurface, onSurfaceVariant | ídem |
| surfaceContainerLowest (tarjetas) | ídem |
| surfaceContainerHigh (cajas de fórmula, encabezado de tabla) | ídem |
| outline, outlineVariant | ídem |
| error, errorContainer, onErrorContainer | ídem |
| warning, warningContainer, borde/texto/botón del banner de aviso | `ExtendidosClaro` / `ExtendidosOscuro` (`ColoresExtendidos`) |
| success, successContainer | ídem |
| valorClave | ídem |
| filaAlterna, filaUltimaError | ídem |
| pistaR2 | ídem |
| chevronMenu | ídem |
| Gráfica: cuadrícula, ejes, datos, curva, predicción, serie4, serie5, errorSerie, tolerancia | `ExtendidosClaro.grafica` / `ExtendidosOscuro.grafica` (`ColoresGrafica`) |

- **Uso en el código:** `MaterialTheme.colorScheme.primary`, `MaterialTheme.extendidos.warning` y `MaterialTheme.extendidos.grafica.datos`.
- **Colores derivados:** los que no venían en el diseño llevan el comentario `// derivado` en el código.
- **Contraste:** el test `TemaTest.contrasteDeColoresDerivados` exige 4.5:1 en texto y 3:1 en bordes.

## Tipografía → `ui/tema/Tipografia.kt`

| Token | Dónde |
|---|---|
| Fuente de interfaz (IBM Plex Sans) | `fuenteInterfaz()` |
| Fuente de fórmulas y números (JetBrains Mono) | `fuenteMono()` (único punto) |
| headlineMedium 28/36·600, titleLarge 22/28·600, titleMedium 18/24·600, titleSmall 16/22·500, bodyLarge 16/24, bodyMedium 14/20, bodySmall 12/16, labelLarge 14/20·600 | `tipografia(sans)` |
| Etiqueta de sección 12/16·600 0.06em, marca "PENTAMETOD2" 0.08em, fórmula 15/24·500, tabla 13/18 tnum, valor clave 20/28·600, valor 15/22, gráfica 11 | `estilosMono(mono)` → `MaterialTheme.mono.*` |
| Texto de fórmula | `TextoFormula(...)`: todo texto de fórmula pasa por aquí |

Los archivos de fuente y su licencia están en `docs/FUENTES.md`.

## Formas → `ui/tema/Forma.kt`

| Radio | Uso | Objeto |
|---|---|---|
| 4 | campos | `Formas.campo` |
| 8 | chips, cajas de fórmula, tablas, caja informativa, campo compacto, pestañas, vista previa | `Formas.chip` |
| 12 | tile del menú, fila destacada, caja del vector a, píldora de conteo, línea de ayuda | `Formas.tile` |
| 16 | tarjetas, banners, estado vacío | `Formas.tarjeta` |
| 24 | botones, segmentado | `Formas.boton` |

`FormasMaterial` pasa los mismos radios a las Shapes de M3.

## Espaciado y medidas → `ui/tema/Espaciado.kt` (`object Dimens`)

| Medida | Valor |
|---|---|
| Escala de espaciado (`e4` … `e48`) | 4, 8, 12, 16, 20, 24, 32, 48 |
| Margen lateral, padding de tarjeta | 16 |
| Separación entre tarjetas | 12 |
| Separación entre campos / entre ecuaciones | 20 / 16 |
| Ancho máximo del contenido | 720 |
| Barra superior | 64 |
| Fila del menú | ≥ 72 (tile de 44) |
| Área táctil, botón | 48 |
| Campo de texto | 56 (compacto: 44; celda de tabla: 36) |
| Área táctil compacta (× de la tabla, filas de la tabla de puntos) | 44 |
| Chip de estado / de categoría | 28 / 32 |
| Filas de tabla | 40 (encabezado 40, Σ 44, tabla de iteraciones 36), columna mínima 64 |
| Filas de resumen | 40 (destacada ≥ 52) |
| Encabezado de la explicación | ≥ 64 |
| Bordes | 1 (foco 2) |
| Íconos | 24 (18 en botones, 14 en chips, 16 en la caja informativa) |

## Íconos → `ui/tema/Iconos.kt`

- **Definición:** `Iconos.<nombre>` es un `DefIcono` con los paths SVG del diseño (viewBox 24, trazo 1.75 o 1.9 en botones, caps y joins redondeados).
- **Uso:** se dibujan con `Icono(def, descripcion, ...)`.
- **Otros dibujos:**
  - El marcador de predicción está en `ui/componentes/Resumen.kt` (`marcadorPrediccion`).
  - La ilustración del estado vacío se quitó en la Fase 6 (ahora es la línea de ayuda).

## Componentes → `ui/componentes/`

| Archivo | Contenido |
|---|---|
| `Contenedores.kt` | `TarjetaContorno`, `EtiquetaSeccion`, `TituloTarjeta`, `CajaInfo`, `ContenidoCentrado` |
| `Controles.kt` | `CampoTexto` (prefijo "=" y multilínea), `CampoCompacto`, `PildoraConteo`, `PestanasChip`, `BotonPrimario`, `BotonSecundario`, `BotonTexto`, `BotonTonal`, `Segmentado` (con `habilitado`), `FilaDoble` |
| `Estado.kt` | `ChipEstado`, `BannerEstado` |
| `Resumen.kt` | `FilaResumen` (regla de apilar: `debeApilar`, > 24 caracteres), `FilaDestacada`, `MarcadorPrediccion` |
| `Barra.kt` | `BarraSuperior` (≥ 64dp, atrás, subtítulo opcional 13/18, ayuda, barra de progreso de 4dp) |

### Componentes de la Fase 6

- **`CampoCompacto`**: etiqueta de 12 (bodySmall·500) **arriba** y, a su derecha, un accesorio opcional (la píldora). Caja de 44 de alto (o `alto`), borde 1dp outline (2dp primary con foco; 2dp error si `error`), radio 8, valor en mono 14 (`mono.dato`). Opciones: `ayuda`, `focusRequester`, `seleccionarTodoAlEnfocar`. Sin `etiqueta` es una celda de tabla (`descripcion` la nombra para TalkBack).
- **`PildoraConteo`**: alto 24, radio 12, texto 12/600. Válida: successContainer/success (con ✓ si `conCheck`). Error: errorContainer/onErrorContainer. Neutra: surfaceContainerHigh. Es región viva para TalkBack.
- **`PestanasChip`**: chips de 32, radio 8. La activa va en primaryContainer y las otras con borde outlineVariant (rol Tab).
- **`DockAcciones`** (`ui/formularios/Formularios.kt`): barra fija inferior de la pantalla de método en edición. Borde superior de 1dp outlineVariant, fondo `background`, padding 16/8, insets `safeDrawing` (barra de navegación y teclado). Lleva "Cargar ejemplo" (`BotonTexto`; sin ícono con fuente > 1.15×) y "Calcular" (`BotonPrimario`, flex 1).
- **Línea de ayuda** (`LineaAyuda` en `MetodoScreen.kt`): sustituye al estado vacío. Borde punteado 1dp, radio 12, ⓘ de 16 y "Verás fórmula, resumen, gráfica y tabla de pasos.". Desaparece cuando hay resultado.
- **Íconos nuevos**: `Iconos.cerrar` (×) y `Iconos.pegar` (portapapeles).

### Componentes de la Fase 7

- **`ListaPasos`** (`ui/resultados/ListaPasos.kt`): lista desplegable de `BloquePaso`.
  - **Contenedor:** borde 1dp outlineVariant, radio 16, filas separadas por una línea de 1dp.
  - **Fila (≥ 52, `Dimens.filaDestacada`):**
    - círculo de 28 en primaryContainer con el número (`mono.tile`);
    - título 15/20 (500 cerrada, 600 abierta);
    - `resumenCorto` en mono 14/600 (onSurfaceVariant, o `extendidos.valorClave` si el paso es destacado), con un máximo de 140 de ancho;
    - chevron › de 18, que gira 90° al abrirse.
  - **Fila abierta:** fondo `extendidos.filaAlterna`.
  - **Animación:** `AnimatedVisibility` con expand/shrink vertical y fundido de 200 ms. Con "Quitar animaciones" del sistema, Compose la omite.
  - **Estado:** los pasos abiertos se guardan con rememberSaveable por cálculo (texto "1,3"; funciones puras `pasosIniciales`, `alternarPaso` y `pasosAbiertos` en `VistaSalida.kt`).
  - **Contenido**, en orden:
    - explicación (bodyMedium);
    - `EtiquetaSeccion("Fórmula")` con una caja mono;
    - `EtiquetaSeccion("Con tus datos")` con una caja mono y/o `ContenidoMatrices`;
    - franja de resultado: primaryContainer, o tertiaryContainer con valorClave si es destacado; radio 8 y mono 14/600.
- **`TextoFormula(..., ajustar = true)`**: la fórmula pasa a la línea siguiente en lugar de desplazarse en horizontal. Lo usan las cajas de los pasos.
- **`MenuResultados`** (`ui/resultados/MenuResultados.kt`):
  - Va en la barra superior mediante el nuevo parámetro `accion` de `BarraSuperior`, en el lugar del "?". Solo aparece con resultado.
  - Botón `Iconos.guardar`, con descripción "Guardar o exportar".
  - `DropdownMenu` de M3:
    - 312 de ancho, radio 12 (`Formas.tile`), fondo surfaceContainerLowest, borde 1dp outlineVariant y sombra de 3.
    - Ítems de ≥ 56 con ícono a la izquierda (`hoja`, `codigo`, `copiar`, `ayuda`) y separador antes de "Ayuda del método".
  - La lista de ítems y si están habilitados sale de `itemsMenu(salida)`, que es pura.
- **Snackbar**: `SnackbarHost` del Scaffold de `MetodoScreen`, con los colores M3 por defecto del tema.
  - Exportar: "Guardado: <nombre>" con la acción "ABRIR" (duración larga).
  - Copiar: "Copiado: resumen y tablas, separados por tabulaciones.".
  - Error: "No se pudo guardar el archivo".
- **Íconos nuevos**: `Iconos.guardar` (M12 4v11 · M7.5 10.5L12 15l4.5-4.5 · M5 19h14), `Iconos.hoja`, `Iconos.codigo` y `Iconos.copiar`.

### Fase 8: la misma estructura en los 6 métodos

- **Pantalla de resultados:** los 6 métodos usan el formato de pasos: PASOS → `ListaPasos` → gráfica → tabla principal → conclusión. El formato anterior (EXPLICACIÓN + Resumen) sigue en el código para salidas sin pasos, pero hoy ningún método lo usa.
- **`BloquePaso.tablas`:** un paso puede llevar tablas en "Con tus datos", dibujadas con el mismo `TablaView` que las de abajo (columna fija y aviso "Desliza para ver las N columnas →"). Si esa tabla también está entre los bloques de la salida, no se repite debajo de la lista (`yaEnUnPaso`). Ejemplo: la tabla triangular de Newton va en el paso 2.
- **Rótulos de coeficientes:** `rotulosCoeficientes` acepta "a" y "β" (← β₀, ← β₁…); el vector β va en la caja surfaceContainerHigh como "a".
- **Resúmenes cortos:** se escriben pensando en ≤ 140dp a 1.3× ("grado 2", "k = 13", "0.500000"). Si no caben, se cortan con "…".
- **Ícono de guardar:** está en los 6 métodos cuando hay resultado. Se oculta solo si no se calculó nada (`Exportaciones.sinCalculo`). Un sistema que no converge sí exporta.
- **Exportación:** cada método registra su generador en `Catalogo` (`generador`, `puedeExportarExcel`, `puedeExportarOctave`). La interfaz solo pregunta a `Exportaciones.disponible`, sin `when (metodo)` en `ui/`. Las hojas y los nombres están en `docs/EXPORTAR.md`.
- **Estilos del .xlsx:** se agregó `EstiloX.DECIMAL7` (`0.0000000`), que usa la hoja de diferencias divididas cuando los datos tienen 7 decimales.

## Pantallas

| Pantalla | Archivo |
|---|---|
| Menú | `ui/navegacion/MenuScreen.kt` (secciones, glifos `glifoMetodo`, "Acerca de") |
| Pantalla de método (Edición / Resultado, estado vacío, ayuda) | `ui/resultados/MetodoScreen.kt` |
| Formularios | `ui/formularios/Formularios.kt` (y la regresión polinomial en `FormularioPolinomial.kt`) |
| Reglas puras de la entrada de puntos (conteo, validación, grado, Lista ↔ Tabla, pegado, vista previa) | `ui/formularios/ValidacionPuntos.kt` |
| Subtítulo de la barra ("Ajuste de curvas · mínimos cuadrados") | `subtituloMetodo` en `ui/resultados/MetodoScreen.kt` |
| Entrada comprimida | `ui/formularios/ResumenEntrada.kt` |
| Resultados (explicación, separador, banners, resumen, tablas, matrices, conclusión, grupos) | `ui/resultados/SalidaView.kt` (con pasos: PASOS → lista → gráfica → tablas → conclusión) |
| Lista de pasos y menú de exportar | `ui/resultados/ListaPasos.kt`, `ui/resultados/MenuResultados.kt` |
| Exportación (Octave, ZIP, XLSX, guardado) | `exportar/` (ver `docs/EXPORTAR.md`) |
| Reglas puras de los resultados | `ui/resultados/VistaSalida.kt` |
| Gráfica | `ui/grafica/GraficaView.kt` (dibujo) y `ui/grafica/Escalas.kt` (escalas, colores por rol `rolesSeries`, etiquetas) |

## Fuera de `ui/` (cambios mínimos de la Fase 4B)

| Cambio | Dónde |
|---|---|
| `ItemResumen.destacado`, `BloqueTexto.progreso`, `Serie.discontinua` | `model/Salida.kt` |
| `BloquePaso`, `ResultadoPaso`, `SalidaMetodo.exportable` (`DatosExportables`) (Fase 7); `BloquePaso.tablas` y los `DatosExportables` de los otros 5 métodos (Fase 8) | `model/Salida.kt` |
| Registro de exportadores; se pasan `maxIter` y el modo de nodos (automático/manual) a los presentadores (Fase 8) | `catalogo/Catalogo.kt` |
| `MetodoInfo.seccion` y las secciones del menú | `catalogo/Entradas.kt` (`object Secciones`) |
| Descripciones del menú | `catalogo/Catalogo.kt` |
| Ícono de la app | `androidApp/src/main/res/` (ver `docs/APK.md`) |
