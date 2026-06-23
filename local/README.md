# BFD Local Development Stack

A containerized local BFD environment for every team to use as part of their development stacks.

## Quick Start

```bash
# From the repo root:
make build    # Compile Java apps and build Docker images
make run      # Start PostgreSQL, run migrations, load data, start FHIR server
```

Once running, the BFD FHIR server is available at **http://localhost:8080**.

```bash
# Test it:
curl http://localhost:8080/v3/fhir/metadata

# Swagger UI:
open http://localhost:8080/v3/fhir/swagger-ui
```

## Commands

| Command       | Description                                               |
|---------------|-----------------------------------------------------------|
| `make build`  | Compile Java applications and build Docker images         |
| `make run`    | Start the full stack (DB + migrations + data + server)    |
| `make stop`   | Stop all containers (data is preserved)                   |
| `make clean`  | Remove all containers, volumes, and local images          |
| `make status` | Show running container status                             |
| `make logs`   | Tail logs from all services                               |

## Architecture

The local stack consists of four services:

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  PostgreSQL  │────▶│  Migrator   │────▶│ Data Loader  │────▶│ BFD Server  │
│  (fhirdb)   │     │  (Flyway)   │     │ (Synth Data) │     │  (port 8080)│
└─────────────┘     └─────────────┘     └──────────────┘     └─────────────┘
```

1. **PostgreSQL 16** - Database server hosting `fhirdb`
2. **Migrator** - Runs Flyway migrations from `bfd-model-rif` to set up the schema
3. **Data Loader** - Loads synthetic beneficiary data (RIF format sample data)
4. **BFD Server NG** - Spring Boot FHIR server with `local` profile (no mTLS)

## Prerequisites

- Docker and Docker Compose (v2)
- Java 25+
- Maven 3.9+

## Database Access

The PostgreSQL instance is available at:
- **Host:** `localhost`
- **Port:** `5432`
- **Database:** `fhirdb`
- **User:** `bfd`
- **Password:** `InsecureLocalDev`

```bash
psql postgresql://bfd:InsecureLocalDev@localhost:5432/fhirdb
```

## Notes

- This runs **bfd-server-ng** which serves the V3 FHIR API (`/v3/fhir/...`)
- The `local` Spring profile disables mTLS/SSL, so you can make plain HTTP requests
- Swagger UI is available at `http://localhost:8080/v3/fhir/swagger-ui`
- Data persists across `make stop` / `make run` cycles (use `make clean` to reset)
- The migrator and data-loader are one-shot containers that exit after completing
- If you modify migrations or server code, run `make build` again before `make run`
- To test SAMHSA-authorized requests locally, add the header:
  `X-Amzn-Mtls-Clientcert: samhsa_allowed`

## Synthetic Data

The data loader attempts to load sample synthetic RIF data bundled in the repository
(`apps/bfd-model/bfd-model-rif-samples/src/main/resources/rif-synthea/`).

For a full 10K synthetic beneficiary dataset, the pipeline can be configured to load
from an S3 bucket containing the complete synthetic data set. Contact the BFD team
for access to the synthetic data bucket if needed.

## Troubleshooting

**Server won't start:**
```bash
make logs  # Check for error messages
docker compose -f local/docker-compose.yml logs server
```

**Port conflicts:**
If port 5432 or 8080 is already in use, stop any local PostgreSQL or other services,
or modify the port mappings in `local/docker-compose.yml`.

**Rebuilding from scratch:**
```bash
make clean
make build
make run
```
