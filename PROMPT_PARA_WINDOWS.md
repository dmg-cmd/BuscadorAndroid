# Prompt para compilar BuscadorAndroid en Windows + Android Studio

Copia y pega este prompt en tu IA de la máquina grande (ChatGPT, Claude, opencode, etc.):

---

**Prompt:**

Eres un asistente Android experto. Ayúdame a compilar e instalar el proyecto **BuscadorAndroid**.

**Contexto del proyecto:**
- Ubicación tras copiar: `C:\Users\TuUsuario\Documents\BuscadorAndroid` (o donde lo descomprimas)
- App nativa Android Kotlin + Jetpack Compose (Material 3) + Hilt + Room + Coroutines
- Paquete: `com.buscadorandroid.app` | `compileSdk 35` | `minSdk 26` | `targetSdk 35` | AGP 8.4.2 + Gradle 8.6 + Kotlin 1.9.22
- Funcionalidad clave: buscador de archivos en todo el celular, con filtro por **tipo** (Imágenes, Videos, Música, Documentos, APKs, Comprimidos) y por **CUALQUIER extensión** (ej: `log`, `tmp`, `bak`, `dwg`, `db`, `xyz` - no solo las comunes). Ver `app/src/main/java/com/buscadorandroid/app/domain/model/TipoArchivo.kt:12` y `FiltroBusqueda.kt:30`

**Estado actual:**
- Proyecto recién generado, ya compila. Se corrigió: se agregaron `app/src/main/res/mipmap-*/ic_launcher.png` y `android.suppressUnsupportedCompileSdk=35` en `gradle.properties` para el warning de AGP 8.4.2 con compileSdk 35.
- Tiene `gradlew.bat` y `gradle/wrapper/` listos. No necesitas crearlos.

**Lo que necesito:**
1. Abrir el proyecto en Android Studio (Hedgehog o superior, JDK 17) y hacer Sync Gradle.
2. Conectar un celular Android por USB con Depuración USB activada y `adb devices` visible, o usar emulador.
3. Compilar e instalar: `Run ▶` o por terminal `gradlew.bat assembleDebug` y luego `adb install -r app\build\outputs\apk\debug\app-debug.apk`
4. Probar la app: abrir BuscadorAndroid, conceder permisos `READ_MEDIA_*`, buscar por nombre y tocar chips de tipo (ej: Imágenes) o botón `+ Extensión` para agregar `log`/`tmp`/`dwg`, o escribir directamente `.log` / `*.tmp` en la barra para filtrar.

**Requisitos a verificar en Windows:**
- Android Studio con SDK Platforms 35 y Build-Tools 35 instalados (SDK Manager)
- JDK 17 (embebido en Android Studio)
- Si falla Sync, hacer `File > Invalidate Caches / Restart` y `gradlew.bat --stop` luego Sync de nuevo.

**Regla:** Comunícate exclusivamente en español y piensa en español. Código y comentarios en español salvo keywords técnicas.

Ayúdame paso a paso y si hay error de compilación, muéstrame el `tail` del log y corrígelo.

---

## Cómo llevar el proyecto a Windows

**Opción A - ZIP (recomendada):**
```bash
# En tu máquina Linux actual:
cd /home/delm/Documentos
zip -r BuscadorAndroid.zip BuscadorAndroid -x "BuscadorAndroid/app/build/*" -x "BuscadorAndroid/.gradle/*" -x "BuscadorAndroid/.idea/*"
# Copia el ZIP a USB o compártelo por Drive/Telegram a tu PC Windows y descomprime
```

**Opción B - Git (si usas GitHub):**
```bash
cd /home/delm/Documentos/BuscadorAndroid
git init
git add .
git commit -m "BuscadorAndroid inicial con filtro por cualquier extensión"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/BuscadorAndroid.git
git push -u origin main
# Luego en Windows: git clone https://github.com/TU_USUARIO/BuscadorAndroid.git
```

**Opción C - Carpeta compartida / USB:**
Copia toda la carpeta `BuscadorAndroid` tal cual a un pendrive, excluyendo `app/build` y `.gradle` si quieres que pese menos (Android Studio los regenera).

## Verificación rápida en Windows tras abrir

```powershell
# En PowerShell dentro de la carpeta del proyecto:
.\gradlew.bat --version
.\gradlew.bat assembleDebug
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.buscadorandroid.app/.presentation.ui.MainActivity
```
