# Política de Seguridad

[English](SECURITY.md) | [Español](SECURITY.es.md)

Homeo es una aplicación diseñada para procesar datos sensibles de biometría, actividad física y salud personal. La seguridad y la confidencialidad forman parte intrínseca de la arquitectura del sistema desde sus cimientos.

---

## Versiones Soportadas

Las actualizaciones de seguridad se aplican activamente sobre la rama principal de desarrollo.

| Versión / Rama | Con Soporte |
| :--- | :--- |
| `master` (Última versión) | :white_check_mark: Sí |
| Versiones anteriores | :x: No |

---

## Arquitectura de Seguridad y Tratamiento de Datos

1. **Cero Exfiltración Remota:** Homeo es una solución totalmente local. Carece de backend propio, no integra SDKs de analítica y no realiza seguimiento de telemetría. La información de salud nunca abandona el dispositivo Android.
2. **Protección de Credenciales Locales:** Las claves de comunicación de los wearables (como la clave de autenticación BLE Huami de 16 bytes) se aíslan en el archivo `secrets.properties` y en el almacenamiento local de Room, quedando excluidas del control de versiones mediante `.gitignore`.
3. **Aislamiento y Permisos de Android:** Homeo solicita exclusivamente el conjunto mínimo de permisos de ejecución necesarios para la conexión Bluetooth LE, el seguimiento de entrenamientos en primer plano y el acceso a Health Connect. Todos los permisos pueden revocarse en cualquier momento desde los Ajustes del sistema.
4. **Custodia y Portabilidad:** Las copias de seguridad se exportan en formato JSON legible mediante el `FileProvider` seguro de Android, permitiendo al usuario auditar y conservar el control absoluto sobre sus datos.

---

## Notificación de Vulnerabilidades

Nos tomamos muy en serio la seguridad de los datos personales. Si descubres alguna vulnerabilidad de seguridad, agradecemos tu colaboración notificándola de forma responsable.

### Procedimiento de Notificación

- **NO abras una issue pública en GitHub** para reportar vulnerabilidades no parcheadas.
- Envía un mensaje directo a los mantenedores del proyecto o utiliza la herramienta de reporte privado de vulnerabilidades de GitHub.
- Por favor, incluye en tu informe:
  - Tipo y descripción detallada de la vulnerabilidad.
  - Pasos exactos o prueba de concepto reproducible.
  - Versiones de Android o dispositivos afectados.
  - Impacto potencial sobre la integridad de datos locales o límites de autorización.

### Protocolo de Respuesta

1. Acusaremos recibo de tu informe en un plazo máximo de 48 horas.
2. Validaremos el problema y coordinaremos la corrección en una rama privada.
3. Una vez verificada y publicada la solución, reconoceremos públicamente tu contribución (salvo que solicites permanecer en el anonimato).
