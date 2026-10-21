# Generar e instalar el APK — Métodos Numéricos (Pentametod2)

Proyecto: `C:\CICLO VI\METODOS NUMERICOS\Programa\u2`, módulo de app `androidApp`, paquete `com.example.pentametod2`.
Todos los comandos se escriben en una terminal (PowerShell o CMD) **abierta en la carpeta del proyecto**.

---

## (a) APK de depuración para compartir

Sirve para probar y compartir con compañeros o el docente. Está firmado con la clave de depuración de Android Studio,
así que **no sirve para Google Play**.

### 1. Generarlo

```powershell
cd "C:\CICLO VI\METODOS NUMERICOS\Programa\u2"
.\gradlew.bat :androidApp:assembleDebug
```

El APK queda en:

```
androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

(Comprobado el 19/10/2026: `BUILD SUCCESSFUL`, archivo de unos 12 MB, nombre visible "Métodos Numéricos", minSdk 24 = Android 7.0 o superior.)

Para compartirlo conviene copiarlo con un nombre claro, p. ej.:

```powershell
Copy-Item androidApp\build\outputs\apk\debug\androidApp-debug.apk "$env:USERPROFILE\Desktop\MetodosNumericos-1.0-debug.apk"
```

### 2a. Instalar por USB (o en el emulador)

1. En el teléfono: Ajustes → Acerca del teléfono → tocar 7 veces "Número de compilación". Luego Ajustes → Sistema → Opciones de desarrollador → activar **Depuración por USB**.
2. Conectar el cable y aceptar en el teléfono el aviso "¿Permitir depuración USB?".
3. Instalar:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

`devices` debe listar el equipo como `device` (no `unauthorized`). `-r` reemplaza la versión instalada y conserva sus datos.
Debe responder `Success` (comprobado en `emulator-5554`).

Atajo: `.\gradlew.bat :androidApp:installDebug` genera e instala en un solo paso, en el dispositivo conectado.

### 2b. Instalar enviando el archivo

1. Enviar `MetodosNumericos-1.0-debug.apk` por Drive, correo o WhatsApp (como **documento**, para que no lo recompriman).
2. En el teléfono, abrirlo desde Descargas o desde el Gestor de archivos.
3. La primera vez Android pide permiso: "Instalar apps desconocidas" → permitir para esa app (Chrome, Archivos, Drive…).
4. Si sale "App no instalada" porque ya hay otra versión firmada con otra clave, hay que desinstalar la anterior primero.

---

## (b) APK / AAB de release firmado (Android Studio)

- **AAB** (`.aab`): es lo que pide Google Play.
- **APK** firmado: sirve para distribuirlo directamente, sin Play.

> **Antes de cada entrega nueva**, sube `versionCode` (y si quieres `versionName`) en `androidApp/build.gradle.kts`,
> bloque `defaultConfig`. Android no instala una versión con `versionCode` igual o menor encima de otra.

### Pasos

1. Abrir el proyecto en Android Studio y esperar a que termine el *Gradle sync*.
2. Menú **Build → Generate Signed App Bundle / APK…**
3. Elegir **Android App Bundle** (para Play) o **APK** (para compartir) → *Next*.
4. Módulo: **androidApp**.
5. **Key store path**:
   - La primera vez: **Create new…**
     - Guardar el `.jks` **fuera de la carpeta del proyecto**, p. ej. `C:\Users\<tu usuario>\claves\metodos-numericos.jks`.
     - Poner una contraseña del keystore.
     - Crear una *Key* con alias, p. ej. `metodos`, su propia contraseña y una validez de 25 años o más.
     - En *Certificate* basta con el nombre y la organización.
   - Las siguientes veces: **Choose existing…**, el mismo `.jks` y las mismas contraseñas.
6. **No** marcar "Remember passwords" en un equipo compartido → *Next*.
7. Build variant: **release** → *Create*.
8. Al terminar, Android Studio muestra un aviso con el enlace *locate*. Por defecto el archivo queda en:
   - AAB: `androidApp\release\androidApp-release.aab`
   - APK: `androidApp\release\androidApp-release.apk`

   La carpeta de destino se puede cambiar en el último paso del asistente.

### Qué hay que guardar (y dónde NO)

| Guardar | Dónde |
|---|---|
| El archivo keystore (`.jks`) | Fuera del repositorio, más una copia de respaldo (USB o nube personal). **Si se pierde, no se puede publicar una actualización de la misma app.** |
| Contraseña del keystore, alias de la clave y contraseña de la clave | Un gestor de contraseñas. **Nunca** en archivos del proyecto, en `build.gradle.kts` ni en el chat del grupo. |

Recomendado: agregar al `.gitignore` del proyecto estas líneas, para que un keystore copiado por error no llegue al repositorio:

```
*.jks
*.keystore
keystore.properties
androidApp/release/
```

(Este documento no crea keystores ni guarda contraseñas: todo lo anterior se hace a mano desde el asistente de Android Studio.)

---

## Nombre e ícono de la app

- **Nombre visible:** `androidApp/src/main/res/values/strings.xml` → `app_name`.
- **Ícono para Android 8 o superior** (adaptativo):
  - Color de fondo: `androidApp/src/main/res/drawable/ic_launcher_background.xml`.
  - Símbolo Σ: `androidApp/src/main/res/drawable/ic_launcher_foreground.xml`.
  - Se combinan en `mipmap-anydpi-v26/ic_launcher.xml` y en `ic_launcher_round.xml`.
- **Ícono para Android 7.x:** `androidApp/src/main/res/mipmap-anydpi/ic_launcher.xml` y `ic_launcher_round.xml`.
- **Para cambiarlo por una imagen:** en Android Studio, clic derecho en `androidApp/src/main/res` → **New → Image Asset** → *Launcher Icons (Adaptive and Legacy)*. Ese asistente reemplaza estos archivos.
