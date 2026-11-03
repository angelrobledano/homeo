# Guía de Desarrollo y Contribución

[English](../en/DEVELOPMENT.md) | [Español](DEVELOPMENT.md)

Esta guía detalla las instrucciones técnicas para configurar el entorno de desarrollo, parametrizar las credenciales locales de hardware, ejecutar la batería de pruebas automatizadas y compilar Homeo desde el código fuente.

---

## 1. Requisitos Previos y Herramientas

Para compilar y ejecutar Homeo, tu estación de trabajo debe contar con:

- **Sistema Operativo:** Windows, macOS o Linux.
- **JDK:** OpenJDK 17 (recomendado: Eclipse Temurin o el OpenJDK integrado en Android Studio).
- **Android Studio:** Hedgehog (2023.1.1) o Ladybug (2024.2+).
- **Android SDK:**
  - `compileSdk = 34` (Android 14)
  - `minSdk = 26` (Android 8.0 Oreo)
  - `targetSdk = 34`
  - Build Tools: `34.0.0`
- **Dispositivo de Prueba Físico:** Dispositivo con Android 14 y Bluetooth 5.0+ (Google Pixel 7a recomendado).

---

## 2. Configuración y Clonación del Proyecto

```bash
# Clonar el repositorio
git clone https://github.com/your-username/homeo.git
cd homeo

# Verificar integridad del wrapper de Gradle
./gradlew --version
```

---

## 3. Configuración de Credenciales de Hardware (`secrets.properties`)

La conexión directa Bluetooth LE con smartwatches Amazfit GTR 3 requiere una clave de autenticación criptográfica única de 16 bytes asociada a la dirección MAC del reloj.

1. En la raíz del repositorio, crea un archivo llamado `secrets.properties` (este archivo está excluido de Git mediante `.gitignore`):
   ```properties
   # Dirección MAC Bluetooth del Amazfit GTR 3
   WATCH_MAC=XX:XX:XX:XX:XX:XX

   # Clave de autenticación de 16 bytes (32 caracteres hexadecimales) obtenida de Zepp / Gadgetbridge
   WATCH_AUTH_KEY=0123456789abcdef0123456789abcdef
   ```

2. Si no dispones del archivo `secrets.properties`, la aplicación arrancará en modo de simulación de contingencia, permitiendo inspeccionar la interfaz, el tracking GPS y Health Connect sin necesidad de tener un reloj físico conectado.

---

## 4. Comandos de Compilación y Pruebas con Gradle

### Ejecución de Pruebas Unitarias

Homeo sigue una metodología estricta de Desarrollo Guiado por Pruebas (TDD). La batería de tests valida los modelos de dominio, las operaciones de Room DAO, los decodificadores de tramas BLE y los ViewModels:

```bash
# Ejecutar todas las pruebas unitarias en la JVM local
./gradlew testDebugUnitTest

# Consultar el informe HTML de resultados
# app/build/reports/tests/testDebugUnitTest/index.html
```

### Compilación del Binario Debug

```bash
# Compilar el APK debug
./gradlew assembleDebug

# Ubicación del archivo resultante:
# app/build/outputs/apk/debug/app-debug.apk
```

### Análisis Estático de Código

```bash
# Ejecutar Android Lint
./gradlew lintDebug
```

---

## 5. Despliegue en Dispositivo Físico (ADB)

```bash
# 1. Comprobar conexión ADB
adb devices

# 2. Instalar la versión debug con reemplazo (-r)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Iniciar Homeo directamente en MainActivity
adb shell am start -n com.salud.personal/.MainActivity

# 4. Monitorizar la salida logcat filtrada por Homeo
adb logcat -v time | grep "com.salud.personal"
```

---

## 6. Flujo de Trabajo y Directrices TDD

### Implementación de Nueva Lógica de Dominio
1. Escribe primero las pruebas unitarias en `app/src/test/java/com/salud/personal/domain/`.
2. Implementa las reglas de negocio deterministas en `app/src/main/java/com/salud/personal/domain/`.
3. Verifica que todos los tests pasen antes de conectar la lógica con los ViewModels.

### Modificación de Esquemas de Base de Datos Room
1. Añade los nuevos campos o tablas en las clases `@Entity` bajo `data/local/entity/`.
2. Incrementa el número de `databaseVersion` en `AppDatabase.kt`.
3. Declara una migración explícita `Migration(from, to)` para garantizar la integridad histórica de los datos.
4. Desarrolla tests de validación para la migración.

### Directrices de Interfaz Composable
1. Respeta la **Regla de Diseño 1**: Suprime textos explicativos redundantes o frases vacías.
2. Utiliza exclusivamente tokens del tema `WhoopColors` y `MaterialTheme.typography`.
3. Comprueba que todo elemento interactivo cuente con un área táctil mínima de 48x48dp.
4. No introduzcas textos fijos en código Kotlin; referencia siempre recursos mediante `stringResource(R.string.<id>)`.

---

## 7. Internacionalización y Cómo Añadir Idiomas (i18n)

Homeo está estructurado como un proyecto abierto internacional donde añadir cualquier nuevo idioma requiere **cero modificaciones en código Kotlin**:

1. **Idioma canónico base (Inglés):** Ubicado en `app/src/main/res/values/strings.xml`.
2. **Localización al Español:** Ubicada en `app/src/main/res/values-es/strings.xml`.
3. **Pasos para contribuir un nuevo idioma:**
   - Crea una carpeta `app/src/main/res/values-<locale>/` (ej: `values-fr/`, `values-de/`, `values-it/`, `values-pt/`, `values-ja/`).
   - Copia `app/src/main/res/values/strings.xml` dentro de tu directorio.
   - Traduce los textos entre las etiquetas `<string name="...">...</string>`.
   - Ejecuta `./gradlew assembleDebug` para verificar la corrección del XML.
   - ¡Envía tu Pull Request!
