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

Más detalles en: [Decisión de Monolito Modular](docs/architecture/adr/0001-modular-monolith.md)

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

## Próximos Pasos (ROADMAP 8 SEMANAS)

Consulte `plan.md` para visualizar los sprints programados para completar este desarrollo en 8 semanas.

## Integracion con n8n

Guia tecnica de conexion backend-n8n:

- `docs/n8n/GUIA_CONEXION_BACKEND_N8N.md`

## 🚀 Quick Start (Local)

*El inicio local se simplificará únicamente a levantar PostgreSQL e iniciar el proyecto Spring Boot directamente.*

```bash
# 1. Levantar PostgreSQL
docker-compose up -d

# 2. Iniciar la aplicación
mvn spring-boot:run
```