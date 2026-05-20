# Módulo de Citas - IPS Centir

Repositorio para el desarrollo del **Módulo de Gestión de Citas Médicas** de IPS Centir.

## Alcance del Proyecto

1. **Objetivo:** Modernización del sistema de gestión de citas médicas.
2. **Funcionalidades principales:**
   - Consulta de disponibilidad de agendas y bloqueo de horarios.
   - Creación, confirmación, cancelación y reprogramación de citas.
   - Check-in, No-Show y completado de citas.
   - Notificaciones automáticas por Email, SMS y WhatsApp.
3. **Integraciones:**
   - Opcional validación de pacientes con módulo externo.
   - Afiliación, Historia Clínica, Facturación (vía adapatadores y eventos locales).

## Arquitectura

Se ha definido una arquitectura basada en un **Monolito Modular** puro:

- **Tecnología:** Java 21 + Spring Boot 3.2
- **Base de Datos:** PostgreSQL 15 (Única base de datos para citas, agendas, notificaciones y event log)
- **Patrones de Diseño:** Domain-Driven Design (DDD), Eventos de Dominio (Spring Events, procesados localmente sin bus de mensajería externo como RabbitMQ).
- **Justificación:** Simplicidad operacional para el equipo de 3 personas, un solo endpoint de despliegue, y eliminación de complejidad de red innecesaria para el alcance acotado actual.

## Estructura Base Ajustada

```text
appointments-module/
├── src/
│   ├── main/
│   │   ├── java/com/ipscentir/appointments/
│   │   │   ├── domain/        # Modelos (Appointment, Schedule), Eventos, Interfaces
│   │   │   ├── application/   # Use cases y DTOs
│   │   │   ├── infrastructure/# Adaptadores de persistencia, integración, config
│   │   │   ├── presentation/  # Controladores REST API
│   │   │   └── shared/        # Utilidades, Base Exceptions
│   │   └── resources/
│   │       ├── db/migration/  # Flyway scripts
│   │       └── application.yml
├── docker-compose.yml         # Solo PostgreSQL 15 (opcionalmente PgAdmin o MailHog)
└── pom.xml
```

## 🚀 Guía de Inicio (Quick Start)

El proyecto puede ejecutarse en dos modalidades: **Local** (usando base de datos Docker) o integrado con **Supabase** (usando variables de entorno).

### Opción 1: Ejecución Local (con base de datos local y Flyway)

1. **Levantar base de datos local:**
   Inicie el contenedor de base de datos y herramientas auxiliares usando Docker:
   ```bash
   docker-compose up -d
   ```
   *(Nota: Por defecto, `docker-compose.yml` crea la base de datos `ipscentir_appointments`. Asegúrese de que coincida con la URL en su `application.yml` o use variables de entorno para adaptarla)*

2. **Iniciar la aplicación con Flyway:**
   Por defecto, la ejecución automática de Flyway está desactivada en la configuración para prevenir mutaciones involuntarias. Para habilitar Flyway y aplicar las migraciones locales en el arranque, ejecute:
   ```bash
   SPRING_FLYWAY_ENABLED=true mvn spring-boot:run
   ```

---

### Opción 2: Ejecución con Supabase

1. **Configurar variables de entorno:**
   Cree su archivo `.env` a partir de la plantilla y configure sus credenciales de Supabase:
   ```bash
   cp .env.example .env
   # Edite el archivo .env con sus credenciales reales de Supabase
   ```

2. **Iniciar la aplicación:**
   Para arrancar la aplicación cargando las variables del `.env` (incluyendo la activación del perfil `supabase` y la habilitación de Flyway si requiere aplicar cambios sobre su instancia), ejecute:
   ```bash
   # Habilitar permisos de ejecución si es necesario
   chmod +x scripts/run-with-env.sh

   # Ejecutar cargando perfil y habilitando Flyway
   SPRING_PROFILES_ACTIVE=supabase SPRING_FLYWAY_ENABLED=true ./scripts/run-with-env.sh
   ```

En entornos de integración continua (CI/CD), configure estas mismas variables de entorno como secretos de su repositorio.