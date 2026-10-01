env_pattern := "(\\d+-)?(test|sandbox|prod)"
env_local_pattern := f"({{ env_pattern }}|local)"
env_help := "BFD environment"

default:
    just --list

[doc("""Installs development dependencies in your local machine.
Should be ran once for new devs or when dependencies change""")]
bootstrap:
    #!/usr/bin/env bash
    set -Eeuo pipefail

    ./hooks/install-java-format.sh
    ./hooks/install-fhir-validator.sh
    # Install dependencies used in our scripts
    brew install uv yq taplo prek tenv argc bash fd jq nvm terraform-docs shfmt
    # Some people may not have installed VS Code from brew,
    # so skip if it already exists
    if !command -v code >/dev/null 2>&1
    then
        brew install --cask visual-studio-code
    fi
    # We overwrite the default prek hook with our own script
    # that will automatically re-add any formatting changes before committing
    # This is not possible out of the box at the time of writing this
    # See https://github.com/j178/prek/issues/1051
    prek install --force
    mv .git/hooks/pre-commit .git/hooks/pre-commit.prek
    cp -p ./hooks/run-pre-commit.sh .git/hooks/pre-commit
    cp -p ./hooks/rerun-changed.sh .git/hooks/pre-commit.rerun

[arg("env", long, pattern=env_pattern, help=env_help)]
[doc("""Extracts the certificate from the server.
Useful when new clients connect to BFD and need the cert for host verification.""")]
extract-server-cert env:
    curl -k -w "%{certs}" "https://{{ env }}.fhirv3.bfd.cmscloud.local"

[arg("query", help="Querystring to send with the request")]
[arg("samhsa", long, value="1", help="When set, uses a certificate that is allowed to see samhsa data")]
[arg("env", long, pattern=env_pattern, help=env_help)]
[arg("resource", pattern="(Patient|Coverage|ExplanationOfBenefit)", help="FHIR resource")]
[doc("Sends a request to BFD")]
bfd-request resource query env samhsa="":
    BFD_ENV="{{ env }}" ./apps/utils/scripts/bfd-request.sh "{{ resource }}" "{{ query }}" \
        {{ if samhsa == "1" { "--samhsa" } else { "" } }}

[doc("Rebuilds the entire maven project. This needs to be ran after a release.")]
java-build-all:
    cd ./apps && mvn clean install -DskipITs -DskipTests --threads=1C

[doc("Helper command to remove all containers")]
remove-all-containers:
    docker stop $(docker ps -aq) && docker rm $(docker ps -aq)

[doc("Removes the local db container")]
remove-db:
    ./apps/utils/scripts/remove-local-db.sh

[doc("Creates the local db container")]
create-db:
    ./apps/utils/scripts/create-bfd-db.sh

[arg("env", long, pattern=f"(local|{{ env_pattern }})", help=env_help)]
migrate-db env: create-db
    BFD_ENV="{{ env }}" ./apps/bfd-db-migrator-ng/migrate.sh

[doc("Creates the mock IDR schema in the local db container")]
create-mock-idr: (migrate-db "local")
    ./apps/utils/scripts/run-sql-script.sh ./apps/bfd-pipeline-idr/mock-idr.sql

[arg("csv-folder", help="Loads data from the folder into the local db before starting the server")]
[doc("Run server-ng, optionally running the pipeline first if `csv-folder` is provided")]
server-ng csv-folder="":
    #!/usr/bin/env bash
    set -Eeuo pipefail

    if [ "{{ csv-folder }}" != "" ]; then
        just pipeline {{ csv-folder }};
    fi
    cd ./apps/bfd-server-ng && mvn clean spring-boot:run

[arg("update-snapshots", long, value="1", help="Update snapshots when running tests")]
[doc("Run unit and integration tests for server-ng")]
server-ng-test update-snapshots="":
    cd ./apps/bfd-server-ng && mvn clean verify {{ if update-snapshots == "1" { "-DupdateSnapshot=" } else { "" } }}

[doc("""Open server-ng integration test logs.
These are useful to troubleshoot integration test failures.""")]
server-ng-test-logs:
    code ./apps/bfd-server-ng/target/failsafe-reports/logs

[doc("Open server-ng swagger page. Local server must be running first.")]
swagger:
    open http://localhost:8080/v3/fhir/swagger-ui/

[arg("env", long, pattern=env_pattern, help=env_help)]
[doc("Run the synthetic data migrator")]
migrate-synthetic env:
    BFD_ENV="{{ env }}" ./apps/bfd-db-migrator-synthetic/migrate.sh

[arg("seed-from", long)]
[arg("source-env", long, pattern=f"{{ env_pattern }}?")]
[arg("truncate", long, value="1")]
[arg("env", long, pattern=env_local_pattern, help=env_help)]
[no-cd]
pipeline env="local" source-env="" seed-from="" *truncate:
    #!/usr/bin/env bash
    set -Eeuo pipefail

    if [ "{{ env }}" = "local" ]; then
        just create-mock-idr
    fi
    root="$(git rev-parse --show-toplevel)"
    seed_from={{ if seed-from == "" { "" } else { f"$($root/apps/utils/scripts/relative-to-absolute.sh {{ seed-from }})" } }}
    BFD_ENV="{{ env }}" "$root/apps/bfd-pipeline-idr/run-pipeline.sh" \
        {{ if source-env != "" { f"--source-env {{ source-env }}" } else { "" } }} \
        {{ if seed-from != "" { "--seed-from \"$seed_from\"" } else { "" } }}
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
    paths={{ if paths == "" { "" } else { f"$($root/apps/utils/scripts/relative-to-absolute.sh {{ paths }} )" } }}
    cd "$root/apps/bfd-model-idr" && uv run patient_generator.py $paths \
        {{ if patients != "" { f"--patients {{ patients }}" } else { "" } }} \
        {{ if exclude-empty == "1" { "--exclude-empty" } else { "" } }} \
        {{ if force-ztm-static-rows == "1" { "--force-ztm-static-rows" } else { "" } }}

[arg("paths")]
[arg("bene-sk-mode", long, pattern="(bene_hstry|clm|both)")]
[arg("enable-samhsa", long, value="1")]
[arg("max-claims", long, pattern="\\d+")]
[arg("min-claims", long, pattern="\\d+")]
[arg("pac-gen", long, pattern="(no|if_none|always)")]
[no-cd]
claims-generator min-claims="5" max-claims="10" enable-samhsa="" pac-gen="if_none" bene-sk-mode="both" *paths:
    #!/usr/bin/env bash
    set -Eeuo pipefail

    root="$(git rev-parse --show-toplevel)"
    paths={{ if paths == "" { "" } else { f"$($root/apps/utils/scripts/relative-to-absolute.sh {{ paths }} )" } }}
    cd "$root/apps/bfd-model-idr" && uv run claims_generator.py $paths \
        --bene-sk-mode "{{ bene-sk-mode }}" \
        {{ if enable-samhsa == "1" { "--enable-samhsa" } else { "" } }} \
        --max-claims "{{ max-claims }}" \
        --min-claims "{{ min-claims }}" \
        --pac-gen "{{ pac-gen }}"

[arg("env", long, pattern=env_pattern)]
[arg("headless", long, value="1")]
regression-test env headless="":
    BFD_ENV="{{ env }}" apps/utils/locust_tests/regression.sh \
        {{ if headless != "" { "--headless" } else { "" } }}

[arg("concurrency", long, pattern="\\d+")]
[arg("env", long, pattern=env_pattern)]
[arg("limit", long, pattern="\\d+")]
[arg("tablesample", long, pattern="\\d+")]
samhsa-regression-test env tablesample="10" limit="300" concurrency="10":
    BFD_ENV="{{ env }}" apps/utils/samhsa-regression-tests/regression.sh \
        --tablesample "{{ tablesample }}" \
        --limit "{{ limit }}" \
        --concurrency "{{ concurrency }}"
