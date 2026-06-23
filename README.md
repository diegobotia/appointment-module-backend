# Módulo de Citas - IPS Centir

Backend del **módulo de gestión de citas médicas** de IPS Centir (Java 21, Spring Boot 3.2, PostgreSQL, DDD). Comparte esquemas con Supabase (`core`, `hc`, `appointments`).

## Funcionalidades

### Citas y operación diaria

- Ciclo de vida: crear, confirmar, cancelar, reprogramar, check-in, no-show y completar
- Canal de reserva `STAFF` (panel/mostrador) y `N8N` (chat automatizado)
- Terapia grupal: estado `PENDIENTE_CONFIRMACION_GRUPO` y corte de cupo mínimo
- Citas **administrativas** (`STAFF`): reuniones/bloqueos internos sin paciente, recurso `REUNION_STAFF`
- Listados enriquecidos: `medicoDisplayName`, `patientDisplayName`, flag `administrative`
- Capacidad por sede: horarios de operación, inventario físico (consultorios, salas) y cupos concurrentes

### Panel interno (JWT Supabase)

- Auth: configuración Supabase, perfil `/auth/me` (incluye `medicoId` para rol `Medico`)
- Roles: `Medico`, `Admisiones`, **`Asesor`** (call center, mismos permisos de citas que Admisiones), `Administracion`, `Facturacion`
- Búsqueda de pacientes: `/staff/patients` (documento → `core.pacientes`)
- Agendas y disponibilidad: `/schedules`, `/medicos`, vista consolidada `/me/schedule` (médico)
- Catálogos públicos: especialidades y tipos de servicio (`/catalogs`)
- Admin: dashboard KPIs, sedes, planificación trimestral de agendas, directorio de médicos, bandeja PQRS, notificaciones (reintento), auditoría n8n

### Paciente (sin login en este módulo)

- **Formulario público:** registro en `core.pacientes` (`/forms/patients`)
- **PQRS:** alta pública (`/forms/pqrs`) y gestión en panel (`/admin/pqrs`)
- **n8n** (`X-API-Key`): identificación, registro, disponibilidad, citas, recordatorios pendientes, webhooks de eventos
- Tipo de documento: n8n puede enviar **descripción** (ej. `Cédula de ciudadanía`); el backend resuelve al código DIAN (`13`)

### Notificaciones y observabilidad

- SMS (Twilio), email (Resend) y recordatorios configurables
- Métricas Micrometer: `appointments.created`, `security.unauthorized` / `security.forbidden`, `notifications.failed`
- Prometheus en producción (`/actuator/prometheus`, rol `Administracion`)

### Identidad médico

- `core.profiles.id` = JWT `sub` (usuario del panel)
- `hc.medicos.id` = **`medicoId`** en API de citas y agendas (`core.profiles.medico_id` enlaza ambos)
- El módulo **lee** médicos desde `hc.medicos`; no los crea ni edita

## Arquitectura

Monolito modular: `domain` → `application` → `infrastructure` → `presentation`. Eventos de dominio procesados en proceso (sin bus externo).

## Inicio rápido

### Requisitos

- Java 21, Maven 3.9+
- PostgreSQL 15 (Docker o Supabase)

### Local con Docker

```bash
docker-compose up -d
SPRING_FLYWAY_ENABLED=true mvn spring-boot:run
```

Incluye PostgreSQL, pgAdmin y **MailHog** (SMTP `1025`, UI `8025`) para probar notificaciones por email.

API: `http://localhost:8080/api/v1/...`  
Swagger (solo dev): `http://localhost:8080/swagger-ui.html`

### Ejecución local

```bash
cp .env.example .env
# Editar credenciales
chmod +x scripts/run-with-env.sh
SPRING_PROFILES_ACTIVE=prod SPRING_FLYWAY_ENABLED=true ./scripts/run-with-env.sh
```

Variables: ver [`.env.example`](.env.example).

### Producción

Perfil recomendado: `prod`.

```bash
SPRING_PROFILES_ACTIVE=prod SPRING_FLYWAY_ENABLED=true mvn spring-boot:run
```

| Aspecto | Comportamiento |
|---------|----------------|
| Flyway | Habilitado (`application-prod.yml`) |
| Hibernate | `ddl-auto: none` |
| Swagger / OpenAPI | Deshabilitado |
| Métricas | `/actuator/prometheus` (rol `ADMINISTRACION`) |

## API y documentación

| Recurso | Ruta |
|---------|------|
| REST | `/api/v1/**` |
| OpenAPI JSON | `/v3/api-docs` |
| Swagger UI | `/swagger-ui.html` (no prod) |
| Health | `/actuator/health` |
| **Referencia completa de flujos y endpoints** | [`docs/flujos-api.md`](docs/flujos-api.md) |

### Grupos de rutas (resumen)

| Consumidor | Auth | Prefijos principales |
|------------|------|----------------------|
| Panel interno | JWT Bearer | `/auth`, `/appointments`, `/schedules`, `/medicos`, `/staff/patients`, `/me/schedule`, `/admin/**` |
| Formularios web | Ninguna | `/forms/patients`, `/forms/pqrs` |
| Combos del panel | Ninguna | `/catalogs` |
| n8n / chat paciente | `X-API-Key` | `/integrations/n8n/patient`, `/integrations/n8n/webhooks` |

## Calidad y CI

Pipeline GitHub Actions (`.github/workflows/ci.yml`):

```bash
mvn verify -Dspring.profiles.active=test
```

En cada **pull request hacia `main`** (y en push a `main`/`dev`):

| Control | Herramienta | Efecto si falla |
|---------|-------------|-----------------|
| Tests unitarios e integración | Maven Surefire | ❌ bloquea merge |
| Cobertura servicios críticos ≥80% | JaCoCo (`mvn verify`) | ❌ bloquea merge |
| Análisis estático bytecode | SpotBugs | ❌ bloquea merge (reporte en artifact) |
| CVEs en dependencias nuevas/modificadas | GitHub Dependency Review | ❌ si severidad **high+** |
| Visibilidad en PR | Comentario JaCoCo + check "Maven Tests" | informativo |

**Branch protection (pedir al owner del repo):** exigir que pase el check `Build, tests y calidad` antes de merge a `main`.

Artefactos descargables 14 días: `quality-reports` (JaCoCo HTML + SpotBugs).

## Roles y seguridad

| Rol | Auth | Citas (`/appointments`) | Panel (`/admin/**`) |
|-----|------|-------------------------|---------------------|
| **Asesor** | JWT | **Igual que Admisiones** (`/appointments` + `/admin/appointments`) | No (resto `/admin/**`) |
| Admisiones | JWT | Ciclo de vida + búsqueda admin citas | No (resto `/admin/**`) |
| Administracion | JWT | Operación + corte terapia grupal | Sí (exclusivo) |
| Medico | JWT | Lectura; check-in/completar propias | No |
| Facturacion | JWT | Solo lectura | No |
| Paciente | n8n / formulario | Integración y registro | — |

Rutas compartidas mostrador/call center: `/admin/appointments/**`, `/staff/patients/**` (`Admisiones`, `Asesor`, `Administracion`).

El rol debe existir en `core.roles` con nombre **`Asesor`**.

## Integraciones paciente

- **n8n:** tipo de documento por **descripción** (ej. `Cédula de ciudadanía`); el backend resuelve al código DIAN (`13`).
- **Formulario:** `GET /forms/patients/config` expone catálogo `{ codigo, descripcion }` desde `ColombianIdentificationType`.
- **Recordatorios:** `GET /integrations/n8n/patient/reminders/pending` para flujos automatizados.

## Sedes y planificación

- API admin de sedes: `/api/v1/admin/sedes` (no `/admin/facilities`). Códigos n8n: `BELEN`, `CONQUISTADORES`.
- Planes de agenda trimestrales: `/api/v1/admin/schedule-plans` (bloques por día, **consultorio** obligatorio por sede, publicación con validación de solapes).

## Migraciones (Flyway)

### Estrategia

El proyecto usa una **baseline única** (`V1__baseline.sql`) que refleja exactamente el esquema de la base de datos canónica (`centir_prod`). No hay migraciones incrementales históricas.

### ¿Qué hace Flyway según el estado de la BD?

| Estado de la BD | Comportamiento |
|----------------|----------------|
| **Vacía** (nueva instalación) | `baseline-on-migrate: true` + `baseline-version: 0` → Flyway aplica `V1__baseline.sql` completo |
| **Completa** (`centir_prod`) | Flyway detecta esquema no vacío, hace baseline en versión 0, aplica `V1` con `CREATE TABLE IF NOT EXISTS` (no produce cambios porque todo existe) |
| **Parcial** (faltan algunas tablas) | `V1` con `IF NOT EXISTS` crea solo lo que falte sin error |
| **Con datos existentes** | Flyway respeta los datos existentes; `IF NOT EXISTS` evita conflictos |

### CHECK constraints

Las siguientes constraints están sincronizadas con los enums de Java:

| Tabla | Constraint | Valores permitidos |
|-------|-----------|-------------------|
| `appointments.appointments` | `appointment_type` | `PRESENCIAL, JUNTA_MEDICA, TERAPIA_FISICA, TERAPIA_OCUPACIONAL, STAFF, BLOQUEO` |
| `appointments.appointments` | `status` | `SCHEDULED, PENDIENTE_CONFIRMACION_GRUPO, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW` |
| `appointments.appointments` | `booking_channel` | `N8N, STAFF` |
| `appointments.appointment_participants` | `participant_role` | `PRIMARY, SECONDARY, TERTIARY, QUATERNARY` |
| `appointments.appointment_participants` | `participant_order` | `1..4` |

### Desarrollo: agregar una migration

1. Crear `src/main/resources/db/migration/V<version>__<descripcion>.sql`
2. El número de versión debe ser incremental (V2, V3, etc.)
3. La migration se aplica automáticamente al iniciar la app

## Estructura

```text
src/main/java/com/ipscentir/appointments/
├── domain/
├── application/
├── infrastructure/
└── presentation/
docs/flujos-api.md                    # Referencia API (panel, formularios, n8n)
src/main/resources/db/migration/      # Flyway V1… (ver README sección Migraciones)
scripts/                              # run-with-env, seeds SQL
```
