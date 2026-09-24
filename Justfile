set unstable
set lists

export BFD_LOCAL_DB_USERNAME:="bfd"
export BFD_LOCAL_DB_PASSWORD:="InsecureLocalDev"
export BFD_LOCAL_DB_CONTAINER:="bfd-db"

bootstrap:
    ./hooks/install-java-format.sh
    ./hooks/install-fhir-validator.sh
    # Install dependencies used in our scripts
    brew install yq taplo prek tenv argc bash fd jq
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
    docker stop {{BFD_LOCAL_DB_CONTAINER}} && docker rm {{BFD_LOCAL_DB_CONTAINER}}

create-db:
    ./apps/utils/scripts/create-bfd-db.sh

create-mock-idr:
    ./apps/utils/scripts/run-sql-script.sh ./apps/bfd-pipeline-idr/mock-idr.sql

migrate-db: create-db
    BFD_ENV=local ./apps/bfd-db-migrator-ng/migrate.sh

pipeline csv_folder: migrate-db create-mock-idr
    LOGURU_COLORIZE=YES ./apps/bfd-pipeline-idr/run-pipeline.sh {{csv_folder}}

# Run server-ng, optionally running the pipeline first if `csv_folder` is provided
server-ng csv_folder="":
    #!/usr/bin/env bash
    set -euo pipefail

    if [ "{{csv_folder}}" != "" ]; then
        just pipeline {{csv_folder}};
    fi
    cd ./apps/bfd-server-ng && mvn clean spring-boot:run

[arg("load-mode", long, pattern="(local|synthetic)")]
[arg("env", long, pattern="(\\d+-)?(test|sandbox|prod)")]
[arg("truncate", long, value="1")]
load-synthetic load-mode env *truncate:
    #!/usr/bin/env bash
    set -euo pipefail

    if [ "{{load-mode}}" = "local" ]; then
        just migrate-db
    fi
    args=("--load-mode" "{{load-mode}}")
    echo "$args"
    if [ "{{truncate}}" = "1" ]; then
        args+=("--truncate")
    fi
    BFD_ENV="{{env}}" ./apps/bfd-pipeline-idr/load-synthetic.sh "${args[@]}"

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

[arg("type", long)]
[arg("all", long, value="1")]
gen-structure-map type="" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./gen-structure-map.sh --type="{{type}}" \
    {{ if all == "1" { "--all" } else { "" } }}

[arg("resource", long)]
[arg("profile-type", long, pattern="Basis|Regular|CMS")]
[arg("all", long, value="1")]
fhir-transform resource="" profile-type="CMS" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./fhir-transform.sh --resource "{{resource}}" \
    --profile-type "{{profile-type}}" {{ if all == "1" { "--all" } else { "" } }}

[arg("resource", long)]
[arg("all", long, value="1")]
conformance-test resource="" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./fhir-transform.sh --resource "{{resource}}" \
    {{ if all == "1" { "--all" } else { "" } }}
