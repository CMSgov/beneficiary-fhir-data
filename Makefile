.PHONY: build run stop clean status logs generate-data

# ============================================================================
# BFD Local Development Environment
# ============================================================================
#
# Prerequisites:
#   - Docker and Docker Compose
#   - Java 25+
#   - Maven 3.9+
#
# Commands:
#   make build  - Compile Java apps (skip tests) and build Docker images
#   make run    - Start the full local BFD stack (DB + migrations + data + server)
#   make stop   - Stop all running containers
#   make clean  - Remove containers, volumes, and local images
#   make status - Show status of local BFD services
#   make logs   - Tail logs from all services
#	make generate-data - Generates the required synth data.
#
# After `make run`, the BFD FHIR server is at: http://localhost:8080
# ============================================================================

COMPOSE_FILE := local/docker-compose.yml
COMPOSE := docker compose -f $(COMPOSE_FILE)

# Build Java artifacts and Docker images
build:
	@echo "==> Building BFD Java applications (skipping tests)..."
	cd apps && mvn clean install \
		--threads 1C \
		--no-transfer-progress \
		-DskipTests \
		-DskipITs \
		-Dmaven.javadoc.skip=true \
		-Dapidocgen.skip=true \
		-Dmaven.build.cache.enabled=false \
		-pl bfd-model/bfd-model-rif,bfd-model/bfd-model-rif-samples,bfd-server-ng \
		-am
	@echo "==> Repackaging bfd-server-ng as executable Spring Boot jar..."
	cd apps/bfd-server-ng && mvn package spring-boot:repackage \
		--no-transfer-progress \
		-DskipTests \
		-Djib.skip=true
	@echo ""
	@echo "==> Building Docker images..."
	$(COMPOSE) build
	@echo ""
	@echo "==> Build complete! Run 'make run' to start the local BFD stack."

# Start the full local stack
run:
	@echo "==> Starting local BFD stack..."
	@echo "    PostgreSQL -> Schema Migrations -> Data Loading -> BFD Server"
	@echo ""
	$(COMPOSE) up -d
	@echo ""
	@echo "==> Waiting for BFD server to become healthy..."
	@timeout=180; \
	elapsed=0; \
	while ! curl -sf http://localhost:8080/actuator/metrics > /dev/null 2>&1; do \
		if [ $$elapsed -ge $$timeout ]; then \
			echo "ERROR: BFD server did not become healthy within $${timeout}s"; \
			echo "Check logs with: make logs"; \
			exit 1; \
		fi; \
		sleep 3; \
		elapsed=$$((elapsed + 3)); \
		printf "\r    Elapsed: %ds..." $$elapsed; \
	done
	@echo ""
	@echo ""
	@echo "==> BFD is running!"
	@echo "    FHIR Server:  http://localhost:8080"
	@echo "    Swagger UI:   http://localhost:8080/v3/fhir/swagger-ui"
	@echo "    Database:     postgresql://bfd:InsecureLocalDev@localhost:5432/fhirdb"
	@echo ""
	@echo "    Try:  curl http://localhost:8080/v3/fhir/metadata"
	@echo ""

# Stop all containers
stop:
	@echo "==> Stopping local BFD stack..."
	$(COMPOSE) down

# Remove everything including data volumes
clean:
	@echo "==> Removing local BFD stack (including data volumes)..."
	$(COMPOSE) down -v --rmi local --remove-orphans

# Show container status
status:
	$(COMPOSE) ps

# Tail logs from all services
logs:
	$(COMPOSE) logs -f

# Generate 10K synthetic IDR beneficiary + claims data
generate-data:
	@echo "==> Generating 10K synthetic IDR beneficiaries and claims..."
	@echo "    (requires Node.js for sushi FHIR compiler)"
	cd apps/bfd-model-idr && npx fsh-sushi sushi
	cd apps/bfd-model-idr && uv sync && uv run patient_generator.py --patients 10000 --claims
	@echo "==> Copying generated data to synthetic-data/..."
	cp apps/bfd-model-idr/out/SYNTHETIC_*.csv apps/bfd-model-idr/synthetic-data/
	@echo ""
	@echo "==> Data generation complete!"
	@echo "    Run 'make build && make run' to load it into the local stack."
