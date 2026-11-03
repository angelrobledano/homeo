# Hoja de Ruta del Producto (Roadmap)

[English](ROADMAP.md) | [Español](ROADMAP.es.md)

Esta hoja de ruta describe las fases completadas y los próximos hitos de desarrollo para **Homeo**, nuestro sistema de seguimiento biométrico y fisiología deportiva offline-first.

---

## Hitos Completados

### Fase 1: Cimientos y Stack Android Moderno :white_check_mark:
- Arquitectura limpia con patrón MVI, Kotlin Coroutines y `StateFlow`.
- Persistencia reactiva local mediante Room SQLite 2.6 con esquemas indexados.
- Sistema de diseño visual oscuro OLED (`#0A0B0D`) conforme a los estándares de contraste WCAG AA.
- Grafo de navegación modular con barra inferior de 4 destinos (Hoy, Entreno, Progreso, Coach).

### Fase 2: Telemetría de Hardware y Health Connect :white_check_mark:
- Conexión Bluetooth Low Energy (BLE) directa con **Amazfit GTR 3** (protocolo Zepp OS 2021).
- Negociación criptográfica completa con curva elíptica ECDH B-163 y reto-respuesta AES-128.
- Extracción de telemetría GATT en tiempo real: frecuencia cardíaca, pasos diarios y nivel de batería.
- Integración con Android Health Connect (`androidx.health.connect:connect-client:1.1.0-alpha11`) para importar pesajes y masa grasa desde **Renpho Health**.

### Fase 3: Motores de Fisiología Deportiva Determinista :white_check_mark:
- **Ratio de Carga Aguda:Crónica (ACWR):** Medidor de 4 zonas de Tim Gabbett para prevención de fatiga y lesiones.
- **Impulso de Entrenamiento (TRIMP):** Cuantificación de carga de Eric Banister según reserva cardíaca y duración.
- **Tendencia de Peso (Filtro EMA):** Media móvil exponencial ($\alpha = 0.10$) que aísla fluctuaciones de agua y sodio.
- **Línea Base de FC en Reposo a 30 Días:** Seguimiento de desviaciones respecto a la media recortada individual.
- **Matriz de Correlación de Hábitos:** Evaluación multivariable del impacto de hábitos diarios sobre la recuperación.
- **Seguimiento GPS Exterior:** Servicio en primer plano con renderizado local de mapas en OpenStreetMap.

### Fase 4: Granularidad y Calidad de Datos :white_check_mark:
- **Selectores Temporales Interactivos:** Cambio reactivo entre ventanas de 7D (Semanal), 30D (Mensual) y 90D (Trimestral) con recálculo dinámico inmediato.
- **Filtro Biológico de Báscula:** Descarte automático de taras no humanas (< 35 kg o > 250 kg) para preservar la pureza de los datos.
- **Balances de Periodos Cerrados:** Retrospectivas estructuradas por Día, Semana, Mes y Año.
- **Portabilidad de Datos y Widgets:** Copia de seguridad local JSON y widget de escritorio para Android.
- **Identidad de Marca:** Transición de nombre e iconografía a **Homeo** (Homeostasis).

---

## Hitos Activos y Próximos

### Fase 5: Biometría Avanzada y Controladores Directos (En Planificación)
- [ ] **Controlador BLE Directo para Báscula Renpho Elis 1:** Decodificación de emisiones broadcast del protocolo QN-Scale para leer peso e impedancia sin necesidad de app compañera ni Health Connect.
- [ ] **Análisis de VFC (HRV) rMSSD:** Procesamiento de intervalos entre latidos (RR / IBI) nocturnos para calcular la raíz cuadrada del valor cuadrático medio y evaluar el sistema nervioso autónomo.
- [ ] **Exportación en Formatos FIT y GPX:** Exportación estandarizada de sesiones de entrenamiento compatible con GoldenCheetah, Strava y TrainingPeaks.
- [ ] **Precarga de Mapas Sin Conexión:** Descarga de mosaicos de mapas regionales para salidas de trail running en zonas sin cobertura móvil.

### Fase 6: Ecosistema Wear OS y Wearables Estándar (Backlog)
- [ ] **Compañero para Wear OS (Tiles):** Aplicación ligera para relojes Wear OS que muestra el estado de Recuperación y margen de Esfuerzo en la muñeca.
- [ ] **Perfil Bluetooth Smart HRM Estándar:** Conexión genérica a bandas pectorales de pulso (Polar H10, Garmin HRM-Pro).
- [ ] **Fórmulas Personalizadas de Bioimpedancia:** Ajuste de masa magra calibrado por el usuario mediante validación cruzada con pliegues cutáneos (Jackson-Pollock).
