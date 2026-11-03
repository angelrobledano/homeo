# Guía de Contribución para Homeo

[English](CONTRIBUTING.md) | [Español](CONTRIBUTING.es.md)

¡Muchas gracias por tu interés en contribuir a **Homeo**! Damos la bienvenida a aportaciones de desarrolladores de todo el mundo. Nuestro objetivo es mantener una aplicación de rendimiento deportivo y salud personal con rigor científico, funcionamiento local (offline-first) y la máxima calidad técnica.

---

## Idioma y Comunicación

- **Comunidad Bilingüe:** Puedes abrir Issues, Pull Requests y participar en debates tanto en **inglés** como en **español**.
- **Identificadores en Código:** Todos los nombres técnicos en el código fuente (clases, métodos, variables, tablas de base de datos, comentarios de código y mensajes de commit de Git) deben escribirse en **inglés**.
- **Textos de Interfaz (UI):** Los textos visibles para el usuario en la aplicación se gestionan en español de España (`es_ES`) por defecto, con internacionalización mediante `strings.xml`.

---

## Política Obligatoria de Documentación Bilingüe

Para asegurar el alcance internacional del proyecto garantizando al mismo tiempo el acceso pleno a la comunidad hispanohablante, **toda la documentación pública es estrictamente bilingüe**:

1. **Paridad entre Idiomas:** Cada documento existe en inglés y en español:
   - En la raíz: `ARCHIVO.md` (inglés) y `ARCHIVO.es.md` (español).
   - Documentación técnica: `docs/en/ARCHIVO.md` y `docs/es/ARCHIVO.md`.
2. **Obligación de Sincronización:** Cada vez que se incorpore una funcionalidad, se modifique la arquitectura, se alteren esquemas de base de datos o cambien pasos de configuración:
   - Actualiza la documentación en inglés.
   - Actualiza la versión correspondiente en español.
   - Comprueba que ambas versiones ofrecen la misma información, estructura y nivel de detalle.
   - Comprueba que todos los enlaces internos de markdown funcionen correctamente.
3. **Validación en Pull Requests:** Aquellos Pull Requests que introduzcan o modifiquen funciones sin actualizar ambas versiones idiomáticas deberán completar la traducción pendiente antes de su fusión (*merge*).

---

## Principios de Diseño y Arquitectura

Toda aportación debe respetar los principios clave del proyecto:

1. **Equilibrio UX / DX / AX:** Toda decisión relevante debe mejorar la experiencia de usuario (claridad, honestidad de datos, fluidez), la experiencia del desarrollador (facilidad de prueba, código limpio) y la experiencia del agente (fuente de verdad identificable, modularidad).
2. **Regla de Diseño 1 — Eliminar Texto Innecesario:** Sin frases motivacionales vacías, eslóganes de marketing ni etiquetas secundarias redundantes. La jerarquía visual y los valores numéricos claros deben bastar por sí solos.
3. **Lógica de Dominio Determinista:** Los cálculos fisiológicos (ACWR, TRIMP, desviaciones de línea base, tendencia de peso EMA) deben ubicarse en la capa `domain/` con nula dependencia de la interfaz o del framework Android.
4. **Privacidad Local sin Filtraciones:** Nunca envíes métricas corporales, ubicaciones o claves privadas a servidores de terceros. Toda la persistencia en Room y la comunicación BLE residen en el dispositivo físico.

---

## Flujo de Trabajo y Desarrollo

### 1. Configuración del Entorno

- Instalar **Android Studio Hedgehog** (2023.1.1) o **Ladybug** (2024.2+).
- Utilizar **JDK 17**.
- Disponer de la plataforma Android SDK **API 34** (Android 14) y herramientas de compilación.

### 2. Ramas y Commits

- Convención de nombres para ramas:
  - `feat/nombre-funcionalidad`
  - `fix/descripcion-error`
  - `docs/actualizacion-documentacion`
  - `refactor/limpieza-codigo`
- Los mensajes de commit siguen el estándar Conventional Commits (en inglés):
  ```text
  feat(trends): implement 90-day workload comparative analysis
  fix(weight): filter anomalous bioimpedance scale taras
  docs(api): update BLE GATT packet decoding documentation
  ```

### 3. Desarrollo Guiado por Pruebas (TDD)

Todo algoritmo de dominio, decodificador de repositorio o transformación de datos debe contar con pruebas unitarias automatizadas antes de su entrega:

```bash
# Ejecutar la suite completa de pruebas unitarias
./gradlew testDebugUnitTest

# Compilar binario debug
./gradlew assembleDebug
```

Verifica que todas las pruebas pasen con éxito (`BUILD SUCCESSFUL`) sin advertencias ni regresiones.

---

## Cómo Enviar un Pull Request

1. Haz un fork del repositorio y crea tu rama a partir de `master`.
2. Asegúrate de que el proyecto compila limpiamente y pasa todas las pruebas automatizadas.
3. Comprueba que se han actualizado los documentos tanto en **inglés** como en **español** si el cambio lo requiere.
4. Abre tu Pull Request detallando:
   - Qué problema resuelve.
   - Evidencias de verificación (capturas o salida de tests).
   - Requisitos de hardware o permisos implicados.

---

## Código de Conducta

Se espera que todos los colaboradores y mantenedores respeten nuestro [Código de Conducta](CODE_OF_CONDUCT.es.md). Te rogamos informar de cualquier comportamiento inadecuado a los mantenedores del proyecto.
