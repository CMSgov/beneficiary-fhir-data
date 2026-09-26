env_pattern := "(\\d+-)?(test|sandbox|prod)"

default:
    just --choose

bootstrap:
    ./hooks/install-java-format.sh
    ./hooks/install-fhir-validator.sh
    # Install dependencies used in our scripts
    brew install uv yq taplo prek tenv argc bash fd jq nvm terraform-docs
    # We overwrite the default prek hook with our own script
    # that will automatically re-add any formatting changes before committing
    # This is not possible out of the box at the time of writing this
    # See https://github.com/j178/prek/issues/1051
    prek install --force
    mv .git/hooks/pre-commit .git/hooks/pre-commit.prek
    cp -p ./hooks/run-pre-commit.sh .git/hooks/pre-commit
    cp -p ./hooks/rerun-changed.sh .git/hooks/pre-commit.rerun

remove-all-containers:
    docker stop $(docker ps -aq) && docker rm $(docker ps -aq)

remove-db:
    ./apps/utils/scripts/remove-local-db.sh

create-db:
    ./apps/utils/scripts/create-bfd-db.sh

create-mock-idr:
    ./apps/utils/scripts/run-sql-script.sh ./apps/bfd-pipeline-idr/mock-idr.sql

migrate-db: create-db
    BFD_ENV=local ./apps/bfd-db-migrator-ng/migrate.sh

pipeline csv_folder: migrate-db create-mock-idr
    LOGURU_COLORIZE=YES ./apps/bfd-pipeline-idr/run-pipeline.sh {{ csv_folder }}

# Run server-ng, optionally running the pipeline first if `csv_folder` is provided
server-ng csv_folder="":
    #!/usr/bin/env bash
    set -Eeuo pipefail

    if [ "{{ csv_folder }}" != "" ]; then
        just pipeline {{ csv_folder }};
    fi
    cd ./apps/bfd-server-ng && mvn clean spring-boot:run

[arg("env", long, pattern=env_pattern)]
migrate-synthetic env:
    BFD_ENV={{ env }} ./apps/bfd-db-migrator-synthetic/migrate.sh

[arg("env", long, pattern=env_pattern)]
[arg("load-mode", long, pattern="(local|synthetic)")]
[arg("truncate", long, value="1")]
load-synthetic load-mode env *truncate:
    #!/usr/bin/env bash
    set -Eeuo pipefail

    if [ "{{ load-mode }}" = "local" ]; then
        just migrate-db
    fi
    LOGURU_COLORIZE=YES BFD_ENV="{{ env }}" \
        ./apps/bfd-pipeline-idr/load-synthetic.sh \
        --load-mode "{{ load-mode }}" \
        {{ if truncate == "1" { "--truncate" } else { "" } }}

[arg("env", long, pattern=env_pattern)]
extract-idr env:
    cd ./apps/bfd-pipeline-idr && BFD_ENV="{{ env }}" ./extract-idr.sh

[arg("env", long, pattern=env_pattern)]
idr-bfd-validator env:
    cd ./apps/utils/idr-bfd-validator && BFD_ENV="{{ env }}" ./run-validator.sh

install-model-dependencies:
    cd ./apps/bfd-model-idr && npm install

sushi: install-model-dependencies
    cd ./apps/bfd-model-idr && npm run sushi-build

wait-for-matchbox: start-matchbox
    cd ./apps/bfd-model-idr && uv run wait_for_matchbox.py

upload-sushi: sushi wait-for-matchbox
    cd ./apps/bfd-model-idr && uv run upload_sushi.py

start-matchbox:
    cd ./apps/bfd-model-idr && docker-compose up -d

stop-matchbox:
    cd ./apps/bfd-model-idr && docker-compose down

matchbox-logs:
    cd ./apps/bfd-model-idr && docker-compose logs --follow

[arg("all", long, value="1")]
[arg("type", long)]
gen-structure-map type="" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./gen-structure-map.sh --type="{{ type }}" \
        {{ if all == "1" { "--all" } else { "" } }}

[arg("all", long, value="1")]
[arg("profile-type", long, pattern="Basis|Regular|CMS")]
[arg("resource", long)]
fhir-transform resource="" profile-type="CMS" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./fhir-transform.sh --resource "{{ resource }}" \
        --profile-type "{{ profile-type }}" {{ if all == "1" { "--all" } else { "" } }}

[arg("all", long, value="1")]
[arg("resource", long)]
conformance-test resource="" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./fhir-transform.sh --resource "{{ resource }}" \
        {{ if all == "1" { "--all" } else { "" } }}

[arg("output-directory", long)]
[arg("source-directory", long)]
[arg("utn", long)]
generate-prior-auth-sample utn source-directory="" output-directory="":
    cd apps/bfd-model-idr && uv run generate-prior-auth-sample \
        --utn "{{ utn }}" \
        {{ if source-directory != "" { f"--source-directory {{ source-directory }}" } else { "" } }} \
        {{ if output-directory != "" { f"--output-directory {{ output-directory }}" } else { "" } }}

[arg("clm-uniq-id", long)]
[arg("output-directory", long)]
[arg("source-directory", long)]
generate-eob-sample clm-uniq-id source-directory="" output-directory="":
    cd apps/bfd-model-idr && uv run generate-eob-sample \
        --clm-uniq-id "{{ clm-uniq-id }}" \
        {{ if source-directory != "" { f"--source-directory {{ source-directory }}" } else { "" } }} \
        {{ if output-directory != "" { f"--output-directory {{ output-directory }}" } else { "" } }}

[arg("bene-sk", long)]
[arg("output-directory", long)]
[arg("source-directory", long)]
generate-bene-sample bene-sk source-directory="" output-directory="":
    cd apps/bfd-model-idr && uv run generate-bene-sample \
        --bene-sk "{{ bene-sk }}" \
        {{ if source-directory != "" { f"--source-directory {{ source-directory }}" } else { "" } }} \
        {{ if output-directory != "" { f"--output-directory {{ output-directory }}" } else { "" } }}

[arg("paths")]
[arg("exclude-empty", long, value="1")]
[arg("force-ztm-static-rows", long, value="1")]
[arg("patients", long)]
[no-cd]
patient-generator patients="" exclude-empty="" force-ztm-static-rows="" *paths:
    #!/usr/bin/env bash
    set -Eeuo pipefail

    root="$(git rev-parse --show-toplevel)"
    paths=$($root/apps/utils/scripts/relative-to-absolute.sh "{{ paths }}")
    cd "$root/apps/bfd-model-idr" && uv run patient_generator.py $paths \
        {{ if patients != "" { f"--patients {{ patients }}" } else { "" } }} \
        {{ if exclude-empty == "1" { "--exclude-empty" } else { "" } }} \
        {{ if force-ztm-static-rows == "1" { "--force-ztm-static-rows" } else { "" } }}
