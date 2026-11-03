# Preguntas Frecuentes (FAQ)

[English](../en/FAQ.md) | [Español](FAQ.md)

Este documento responde a las preguntas técnicas, fisiológicas y operativas más habituales sobre Homeo.

---

## Preguntas Generales

### ¿Qué es Homeo y qué problema resuelve?
Homeo es una aplicación de seguimiento biométrico y fisiología deportiva para Android, privada y con funcionamiento prioritariamente local (offline-first). Inspirada en productos como WHOOP y MacroFactor, unifica los datos de smartwatches (Amazfit GTR 3) y básculas de bioimpedancia (Renpho) en un cockpit oscuro OLED. Sustituye las aplicaciones comerciales ruidosas por un control determinista de carga atlética basado en ciencia deportiva (ACWR de Gabbett, TRIMP de Banister, tendencia de peso EMA).

### ¿Es Homeo gratuita y de código abierto?
Sí. Homeo es software libre bajo la licencia Apache 2.0. No contiene suscripciones, muros de pago (*paywalls*), funciones premium bloqueadas ni compras integradas.

### ¿Requiere Homeo conexión a Internet?
No. Homeo está diseñada para funcionar al 100% de forma local. La comunicación Bluetooth LE, la sincronización con Health Connect, el tracking GPS de entrenamientos y el almacenamiento en Room SQLite operan de forma autónoma en tu dispositivo Android sin depender de servidores externos.

---

## Métricas Fisiológicas y Algoritmos

### ¿Qué es el ACWR (Ratio de Carga Aguda:Crónica)?
El ACWR, desarrollado por el científico deportivo Dr. Tim Gabbett, evalúa la relación entre la fatiga acumulada a corto plazo (el esfuerzo realizado en los últimos 7 días) y tu capacidad física crónica construida (la media semanal de esfuerzo en los últimos 28 días).
- **Ratio < 0.8 (Descarga / Volumen Bajo):** Estímulo sensiblemente inferior al habitual. Adecuado para recuperación activa o periodos de descarga planificada.
- **Ratio 0.8 – 1.3 (Rango Óptimo / Sweet Spot):** Punto fisiológico ideal donde el estado de forma progresa minimizando el riesgo de lesión.
- **Ratio 1.3 – 1.5 (Precaución / Carga Alta):** Incremento rápido del volumen de entrenamiento. Exige vigilar el descanso y la nutrición.
- **Ratio > 1.5 (Zona de Riesgo):** Pico agudo que supera en más de un 50% la capacidad media mensual, asociado estadísticamente a un incremento brusco del riesgo de sobreentrenamiento o lesión muscular.

### ¿Qué es el TRIMP y cómo se calcula?
El TRIMP (*Training Impulse*), formulado por el Dr. Eric Banister, cuantifica la carga del entrenamiento cardiovascular. A diferencia de contar calorías o minutos brutos, el TRIMP pondera:
1. La duración de la sesión en minutos.
2. La reserva de frecuencia cardíaca ($\Delta HR = \frac{HR_{media} - HR_{reposo}}{HR_{max} - HR_{reposo}}$).
3. Un factor exponencial de ponderación ($y = e^{1.92 \cdot \Delta HR}$ en hombres, $e^{1.67 \cdot \Delta HR}$ en mujeres) que refleja el impacto fisiológico no lineal de los esfuerzos a intensidades elevadas.

### ¿En qué se diferencia la Tendencia de Peso (EMA) del pesaje bruto de la báscula?
El peso diario en báscula fluctúa entre $\pm 1$ y $2$ kg debido a retención de líquidos, consumo de sodio y niveles de glucógeno muscular (1 gramo de glucógeno retiene de 3 a 4 gramos de agua). Homeo aplica una Media Móvil Exponencial (EMA) con factor de suavizado $\alpha = 0.10$. Este filtro absorbe el ruido hídrico transitorio, reflejando el cambio biológico real en tejido graso y muscular.

### ¿Qué es el "Filtro Biológico de Peso"?
Las básculas inteligentes registran en ocasiones taras accidentales (una pisada involuntaria de un niño o mascota, o un recalibrado por debajo de 35 kg). Homeo filtra automáticamente cualquier medición fuera del rango humano de 35 kg a 250 kg, evitando que un pesaje erróneo distorsione tu tarjeta principal de peso o los cálculos de evolución a 7 días.

### ¿Qué significa "En Rango" en la tarjeta de Constantes Vitales?
En lugar de comparar tus datos con una media poblacional genérica, Homeo evalúa si tu Frecuencia Cardíaca en Reposo, las horas de Sueño y el Peso se encuentran dentro de tu propia línea base individual (media móvil a 30 días $\pm$ margen de desviación típica).

---

## Hardware y Resolución de Problemas

### ¿Cómo obtengo la clave Auth Key de 16 bytes de mi Amazfit GTR 3?
La clave de autenticación puede obtenerse mediante:
1. **Gadgetbridge:** Sigue la [guía oficial de emparejamiento Huami en Gadgetbridge](https://gadgetbridge.org/basics/pairing/huami/) para emparejar el reloj y extraer la clave hexadecimal de 32 caracteres.
2. **Exportación de Zepp / Zepp modificado:** Extrayendo la clave de los registros de la base de datos local de la app oficial (`/data/data/com.huami.watch.hmwatchmanager/databases/`).
3. Guarda tus credenciales en el archivo `secrets.properties` en la raíz del proyecto:
   ```properties
   WATCH_MAC=XX:XX:XX:XX:XX:XX
   WATCH_AUTH_KEY=0123456789abcdef0123456789abcdef
   ```

### ¿Por qué no aparecen los pesajes de mi báscula Renpho?
1. Comprueba que tienes instalada la app **Health Connect** (integrada en el sistema en Android 14+ o descargable en Google Play en Android 9–13).
2. Entra en **Homeo > Ajustes > Health Connect** y otorga permisos de lectura para `Peso` y `Grasa corporal`.
3. Verifica que la app Renpho Health tiene permiso para escribir en Health Connect y que has realizado al menos un pesaje reciente.

### ¿Puedo utilizar Homeo sin reloj inteligente ni báscula?
Sí. Homeo admite el registro manual de entrenamientos (con estimación de esfuerzo RPE), el cuestionario matutino de sensaciones, el tracking GPS de caminatas y la consulta del historial sin necesidad de vincular accesorios BLE externos.
