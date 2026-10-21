# Fuentes de la app

| Archivo (Compose Resources) | Familia y peso | Versión | Origen |
|---|---|---|---|
| `ibm_plex_sans_regular.ttf` | IBM Plex Sans 400 | 3.005 | github.com/IBM/plex · `packages/plex-sans/fonts/complete/ttf/IBMPlexSans-Regular.ttf` |
| `ibm_plex_sans_medium.ttf` | IBM Plex Sans 500 | 3.005 | ídem, `IBMPlexSans-Medium.ttf` |
| `ibm_plex_sans_semibold.ttf` | IBM Plex Sans 600 | 3.005 | ídem, `IBMPlexSans-SemiBold.ttf` |
| `jetbrains_mono_regular.ttf` | JetBrains Mono 400 | 2.305 | github.com/JetBrains/JetBrainsMono · `fonts/ttf/JetBrainsMono-Regular.ttf` |
| `jetbrains_mono_medium.ttf` | JetBrains Mono 500 | 2.305 | ídem, `JetBrainsMono-Medium.ttf` |
| `jetbrains_mono_semibold.ttf` | JetBrains Mono 600 | 2.305 | ídem, `JetBrainsMono-SemiBold.ttf` |

- **Ubicación:** `shared/src/commonMain/composeResources/font/`.
- **Tipo:** TTF **estáticos**. Se comprobó con fontTools que no tienen la tabla `fvar`, así que no son fuentes variables.
- **Descarga:** 19/10/2026, desde las ramas `master` de los repositorios oficiales.
- **En el código:** se cargan con `org.jetbrains.compose.resources.Font(Res.font.…)` en `ui/tema/Tipografia.kt` (funciones `fuenteInterfaz()` y `fuenteMono()`).

## Licencia

Las dos familias se distribuyen con la **SIL Open Font License 1.1**. Los textos completos están en:

- `docs/licencias/IBM-Plex-OFL.txt` (Copyright © 2017 IBM Corp., con Reserved Font Name "Plex").
- `docs/licencias/JetBrains-Mono-OFL.txt` (Copyright 2020 The JetBrains Mono Project Authors).

La OFL permite incluir las fuentes dentro de una app, incluso una comercial, con estas condiciones:

1. No se venden las fuentes por separado.
2. Se acompaña el aviso de copyright y la licencia, como en este documento.
3. Si se **modifican** los archivos (por ejemplo, agregando glifos), la fuente resultante **no puede llamarse "Plex"**, porque es un Reserved Font Name.

Los archivos que usa la app no tienen ninguna modificación.

## Cobertura de glifos (fontTools)

Caracteres no ASCII de las fórmulas y de la interfaz que **no** están en cada fuente:

| Fuente | Glifos que faltan |
|---|---|
| JetBrains Mono (fórmulas, números) | ᵀ ᵏ ᵐ ᵢ ᵧ ⁱ ⁻ ⁽ ⁾ ₊ ₋ ₖ ₘ ₙ ⱼ (15) |
| IBM Plex Sans (interfaz) | ȳ ᵀ ᵏ ᵐ ᵢ ᵧ ‖ ⁱ ⁺ ⁻ ⁽ ⁾ ₊ ₋ ₖ ₘ ₙ ▸ ▾ ✕ ⱼ (21) |

Las dos tienen Σ Π Δ √ ∞ ≈ ≠ − · … ŷ ⁰–⁹ ₀–₉ ← • y las vocales con tilde.

En Android, Compose dibuja cada glifo ausente con la fuente del sistema. Se comprobó en el emulador (Pixel, imagen de sistema actual) sacando `/system/fonts` y revisándolo con fontTools:

- Todos los glifos de modificador están en **Roboto / DroidSans**.
- ▸ ▾ ✕ están en **NotoSansSymbols**.

Resultado: no aparece ningún "▯". El glifo prestado tiene otro grosor y otra anchura, así que en las fórmulas se nota un poco, sobre todo en x⁽ᵏ⁺¹⁾.

Sustituciones hechas: **Sᵧᵧ → S_yy** en `presentacion/PresentadorRegresion.kt`. "ᵧ" es una gamma griega subíndice usada como si fuera "y" y no está en ninguna de las dos fuentes. Las flechas ▸ ▾ ✕ de la interfaz anterior se reemplazaron por íconos vectoriales.

## Cómo cambiar una fuente

1. Copiar los TTF a `shared/src/commonMain/composeResources/font/`, con nombres en minúscula y guiones bajos.
2. Cambiar `fuenteInterfaz()` o `fuenteMono()` en `ui/tema/Tipografia.kt`. Es el único lugar: `TextoFormula` y todos los estilos mono salen de `fuenteMono()`.
3. Actualizar esta tabla y la licencia.
