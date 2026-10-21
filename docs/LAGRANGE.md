# Módulo de Lagrange: puntos, grados y comparación

El formulario de Lagrange es independiente del de Newton. Newton y las llamadas anteriores con `EntradaInterpolacion` conservan su comportamiento. La pantalla utiliza `EntradaLagrange` y migra el ejemplo anterior al formulario nuevo.

## Flujo

1. Registrar parejas X–Y mediante Lista, la vista inicial. El acceso Lista / Tabla cambia la presentación sin reemplazar los puntos ni sus selecciones. En Tabla se editan celdas y se agregan y eliminan filas; el icono de portapapeles permite pegar exactamente dos columnas.
2. Ingresar x a evaluar y seleccionar uno o varios grados, desde 1 hasta min(12, cantidad de puntos distintos − 1). Al completar los puntos se selecciona por defecto el mayor grado disponible; sigue ajustándose al agregar o eliminar puntos hasta que el usuario elige sus propios grados o personaliza los puntos. Cargar ejemplo inicia de nuevo con el máximo. La selección explícita se conserva al editar y restaurar la pantalla.
3. Se eligen los g + 1 puntos más cercanos para cada grado, resolviendo empates por el menor X. Elegir puntos abre un panel con sugerencias editables, un conjunto independiente por grado y acciones para aplicar, cancelar o usar los puntos cercanos. La pantalla principal no muestra un selector Automático / Manual ni repite los puntos sugeridos.
4. Activar opcionalmente la función original, con sintaxis del evaluador existente (trigonometría en radianes).
5. Calcular una comparación común y seleccionar el procedimiento de un grado mediante el mismo estilo segmentado. Antes de calcular admite varios grados; al consultar el procedimiento admite uno.

## Reglas

- Cada punto conserva su identidad al editar o quitar otras filas. Eliminar un punto limpia sus referencias, pero no cambia los grados solicitados ni sustituye el punto silenciosamente.
- Reemplazar toda la tabla reinicia sus identificadores y vacía las selecciones manuales para revisarlas. Los huecos en las listas se conservan para no desplazar parejas.
- Las celdas admiten punto o coma decimal y notación científica. En listas, la coma separa valores y se utiliza punto decimal. El pegado acepta tabulación, espacios o punto y coma entre columnas y un encabezado opcional X Y; rechaza columnas extra.
- Se admiten hasta 200 filas de datos. Se ignoran solo las filas completamente vacías del final; los huecos intermedios, las filas incompletas, X repetidos y números fuera del rango válido deben corregirse.
- Un grado g requiere exactamente g + 1 puntos de X distintos; el polinomio puede tener grado efectivo menor. Calcular se desactiva mientras los datos o una selección sean inválidos.
- Cada grado comprueba extrapolación respecto a sus propios puntos. Si x coincide con uno utilizado, se explica que reproducir su Y no demuestra precisión en otros puntos.
- Error absoluto = abs(f(x) − P(x)). Error relativo porcentual = error absoluto / abs(f(x)) × 100. Si f(x) = 0, el relativo está indefinido. Se avisa cuando f(x) está cerca de cero.
- Una función inválida o indefinida en x no impide interpolar: se explica por qué no hay errores. Las discrepancias entre Y y f(X) se avisan sin reemplazar los datos; se tolera una diferencia de 1e-6 × max(1, abs(Y), abs(f(X))).
- El redondeo es de presentación. Los valores pequeños se muestran en notación científica. Se conserva la conclusión final de cada grado, después de su tabla; se retira la conclusión comparativa general.
- El formulario y las selecciones se conservan al volver al menú o restaurar la pantalla. Editar datos oculta resultados anteriores y su menú de exportación hasta recalcular.

## Resultados y exportación

La tarjeta Entrada / Datos conserva su presentación. El Resumen contiene solo f(x) de referencia cuando hay un valor válido. La comparación contiene grado, puntos y P(x); con referencia válida añade únicamente error absoluto. Los pasos conservan sus fórmulas y resultados, incluido su detalle de errores. El grado elegido en resultados no cambia los cálculos ni limita las exportaciones.

El orden de cada detalle es: Pasos → Gráfica → Nodos y polinomios de Lagrange → Conclusión final. La gráfica usa el mismo generador y el mismo código de dibujo; solo cambia su ubicación. Los avisos comunes y del grado elegido se reúnen junto a PASOS, sin duplicarlos en el encabezado RESULTADOS.

Excel incluye comparación, entrada, notas y tablas y pasos de todos los grados. El código Octave recalcula cada conjunto de puntos y, cuando corresponde, la función de referencia y sus errores, utilizando los Double completos. Copiar resultados incluye todos los puntos, grados, procedimientos y advertencias.

## Verificación

Ejecutar desde la raíz del proyecto:

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug --offline
```

Se agregaron pruebas de integración en `LagrangeComparacionTest`: resultados independientes para grados 1–3 sobre x³, selección manual por grado, extrapolación, función inválida o fuera de dominio, referencia cero, conservación de Y, validaciones, identidades y restauración, importación y exportaciones. También se verifica la compatibilidad de Newton y la migración del ejemplo.

Se revisó en el emulador Android la tabla de entrada, el ejemplo, la selección de varios grados, el reemplazo manual de puntos de grado 1 conservando grado 2, la desactivación de Calcular con selección incompleta, la entrada de función y la comparación con errores. La ejecución de pruebas iOS requiere macOS y queda fuera de la verificación realizada en Windows.
