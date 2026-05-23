# Módulo de Citas - IPS Centir

Backend del **módulo de gestión de citas médicas** de IPS Centir (Java 21, Spring Boot 3.2, PostgreSQL, DDD).

## Funcionalidades

- Disponibilidad, creación, confirmación, cancelación y reprogramación de citas
- Check-in, no-show y completado
- Flujos paciente vía **n8n** (`X-API-Key`) y formulario público
- Staff con JWT Supabase: `Medico`, `Admisiones`, **`Asesor`** (call center), `Administracion`, `Facturacion`
- Capacidad operativa por sede (horarios, inventario físico, cupos)
- Notificaciones (SMS/email), panel admin, auditoría n8n

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

API: `http://localhost:8080/api/v1/...`  
Swagger (solo dev): `http://localhost:8080/swagger-ui.html`

### Supabase

```bash
cp .env.example .env
# Editar credenciales
chmod +x scripts/run-with-env.sh
SPRING_PROFILES_ACTIVE=supabase SPRING_FLYWAY_ENABLED=true ./scripts/run-with-env.sh
```

Variables: ver [`.env.example`](.env.example).

### Producción

Perfil recomendado: `prod` (+ `supabase` si aplica).

```bash
SPRING_PROFILES_ACTIVE=prod,supabase SPRING_FLYWAY_ENABLED=true mvn spring-boot:run
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
| **Flujos API (referencia)** | [`docs/flujos-api.md`](docs/flujos-api.md) |
| Plan de fases | [`plan_ejecucion.md`](plan_ejecucion.md) |
| Capacidad por sede | [`plan_capacidad_operativa_sedes.md`](plan_capacidad_operativa_sedes.md) |

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

Métricas Micrometer:

- `appointments.created` (tag `channel`: N8N / STAFF)
- `security.unauthorized` / `security.forbidden`
- `notifications.failed`

## Roles y seguridad

| Rol | Auth | Citas (`/appointments`) | Panel (`/admin/**`) |
|-----|------|-------------------------|---------------------|
| **Asesor** | JWT | **Igual que Admisiones** (`/appointments` + `/admin/appointments`) | No (resto `/admin/**`) |
| Admisiones | JWT | Ciclo de vida + búsqueda admin citas | No (resto `/admin/**`) |
| Administracion | JWT | Operación + corte terapia grupal | Sí (exclusivo) |
| Medico | JWT | Lectura; check-in/completar propias | No |
| Facturacion | JWT | Solo lectura | No |
| Paciente | n8n / formulario | Integración y registro | — |

En Supabase, el rol debe existir en `core.roles` con nombre **`Asesor`** (migración `V28`).

## Integraciones paciente

- **n8n:** tipo de documento por **descripción** (ej. `Cédula de ciudadanía`); el backend resuelve al código DIAN (`13`).
- **Formulario:** `GET /forms/patients/config` expone catálogo `{ codigo, descripcion }` desde `ColombianIdentificationType`.

## Sedes

API admin: `/api/v1/admin/sedes` (no `/admin/facilities`). Códigos n8n: `BELEN`, `CONQUISTADORES`.

## Estructura

```text
src/main/java/com/ipscentir/appointments/
├── domain/
├── application/
├── infrastructure/
└── presentation/
docs/flujos-api.md              # Referencia API actualizada
src/main/resources/db/migration/   # Flyway V0…V28
```
