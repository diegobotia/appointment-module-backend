# Makefile para gestión rápida del monorepo Centir IPS
# Uso: make <comando>

.PHONY: help start start-infra stop restart clean health logs test-db test-rabbitmq \
        start-patient start-appointment start-clinical

# Por defecto, mostrar ayuda
help:
	@echo "🏥 Centir IPS - Comandos del Monorepo"
	@echo "======================================"
	@echo ""
	@echo "Infra:"
	@echo "  make start          - Levantar infra + appointment-service (docker compose)"
	@echo "  make start-infra    - Levantar solo RabbitMQ, PostgreSQL y Redis"
	@echo "  make stop           - Detener todos los contenedores"
	@echo "  make restart        - Reiniciar todos los contenedores"
	@echo "  make clean          - Limpiar contenedores (mantiene datos)"
	@echo "  make clean-all      - Limpiar TODO incluyendo volúmenes"
	@echo "  make health         - Verificar salud de servicios"
	@echo "  make logs           - Ver logs de todos los servicios"
	@echo ""
	@echo "Desarrollo local (sin Docker para el servicio):"
	@echo "  make start-appointment  - Ejecutar appointment-service con mvn"
	@echo ""
	@echo "Debugging:"
	@echo "  make logs-rabbitmq  - Logs de RabbitMQ"
	@echo "  make logs-postgres  - Logs de PostgreSQL"
	@echo "  make psql           - CLI PostgreSQL"
	@echo "  make redis-cli      - CLI Redis"
	@echo "  make test-event     - Publicar evento de prueba"
	@echo ""

# Iniciar infra completa + appointment-service
start:
	@cd infra/local && ./start.sh

# Iniciar solo la infra base
start-infra:
	@cd infra/local && ./start-infra-only.sh

# Iniciar appointment-service localmente
start-appointment:
	@echo "▶ appointment-service en puerto 8081..."
	@cd services/core/appointment-service && mvn spring-boot:run

# Detener servicios
stop:
	@echo "⏹️  Deteniendo servicios..."
	@cd infra/local && docker-compose stop

# Reiniciar servicios
restart:
	@echo "🔄 Reiniciando servicios..."
	@cd infra/local && docker-compose restart

# Limpiar sin eliminar volúmenes
clean:
	@cd infra/local && ./cleanup.sh

# Limpiar TODO incluyendo datos
clean-all:
	@cd infra/local && ./cleanup.sh --volumes

# Health check
health:
	@cd infra/local && ./health-check.sh

# Ver logs de todos los servicios
logs:
	@cd infra/local && docker-compose logs -f

# Logs específicos de RabbitMQ
logs-rabbitmq:
	@cd infra/local && docker-compose logs -f rabbitmq

# Logs específicos de PostgreSQL
logs-postgres:
	@cd infra/local && docker-compose logs -f postgres

# Logs específicos de Redis
logs-redis:
	@cd infra/local && docker-compose logs -f redis

# Conectar a PostgreSQL
psql:
	@cd infra/local && ./psql.sh

# Conectar a Redis CLI
redis-cli:
	@cd infra/local && ./redis-cli.sh

# Publicar evento de prueba
test-event:
	@cd infra/local && ./publish-test-event.sh patient.registered
