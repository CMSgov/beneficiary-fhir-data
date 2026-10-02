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
[group('utilities')]
extract-server-cert env:
    curl -k -w "%{certs}" "https://{{ env }}.fhirv3.bfd.cmscloud.local"

[arg("query", help="Querystring to send with the request")]
[arg("samhsa", long, value="1", help="When set, uses a certificate that is allowed to see samhsa data")]
[arg("env", long, pattern=env_pattern, help=env_help)]
[arg("resource", pattern="(Patient|Coverage|ExplanationOfBenefit)", help="FHIR resource")]
[doc("Sends a request to BFD")]
[group('server')]
bfd-request resource query env samhsa="":
    BFD_ENV="{{ env }}" ./apps/utils/scripts/bfd-request.sh "{{ resource }}" "{{ query }}" \
        {{ if samhsa == "1" { "--samhsa" } else { "" } }}

[doc("Rebuilds the entire maven project. This needs to be ran after a release.")]
[group('server')]
java-build-all:
    cd ./apps && mvn clean install -DskipITs -DskipTests --threads=1C

[doc("Helper command to remove all containers")]
[group('utilities')]
remove-all-containers:
    docker stop $(docker ps -aq) && docker rm $(docker ps -aq)

[doc("Removes the local db container")]
[group('database')]
remove-db:
    ./apps/utils/scripts/remove-local-db.sh

[doc("Creates the local db container")]
[group('database')]
create-db:
    ./apps/utils/scripts/create-bfd-db.sh

[arg("env", long, pattern=env_local_pattern, help=env_help)]
[doc("Migrate the v3 database using the specified env")]
[group('database')]
migrate-db env:
    #!/usr/bin/env bash
    set -Eeuo pipefail

    if [ "{{ env }}" = "local" ]; then
    	just create-db
    fi
    BFD_ENV="{{ env }}" ./apps/bfd-db-migrator-ng/migrate.sh

[doc("Creates the mock IDR schema in the local db container")]
[group('database')]
create-mock-idr: (migrate-db "local")
    ./apps/utils/scripts/run-sql-script.sh ./apps/bfd-pipeline-idr/mock-idr.sql

[arg("seed-from", long, help="Loads data from the folder into the local db before starting the server")]
[doc("Run server-ng, optionally running the pipeline first if `csv-folder` is provided")]
[group('server')]
server-ng seed-from="":
    #!/usr/bin/env bash
    set -Eeuo pipefail

    if [ "{{ seed-from }}" != "" ]; then
        just pipeline --seed-from "{{ seed-from }}";
    fi
    cd ./apps/bfd-server-ng && mvn clean spring-boot:run

[arg("update-snapshots", long, value="1", help="Update snapshots when running tests")]
[doc("Run unit and integration tests for server-ng")]
[group('server')]
server-ng-test update-snapshots="":
    cd ./apps/bfd-server-ng && mvn clean verify {{ if update-snapshots == "1" { "-DupdateSnapshot=" } else { "" } }}

[doc("""Open server-ng integration test logs.
These are useful to troubleshoot integration test failures.""")]
[group('server')]
server-ng-test-logs:
    code ./apps/bfd-server-ng/target/failsafe-reports/logs

[doc("Open server-ng swagger page. Local server must be running first.")]
[group('server')]
swagger:
    open http://localhost:8080/v3/fhir/swagger-ui/

[arg("env", long, pattern=env_pattern, help=env_help)]
[doc("Run the synthetic data migrator")]
[group('database')]
migrate-synthetic env:
    BFD_ENV="{{ env }}" ./apps/bfd-db-migrator-synthetic/migrate.sh

[arg("seed-from", long, help="directory to load synthetic CSV files from")]
[arg("truncate", long, value="1", help="When enabled, truncates the source env before loading new data")]
[arg("env", long, pattern=env_local_pattern, help=env_help)]
[arg("source-env", long, pattern=f"{{ env_pattern }}?", help="""Override the source environment to load data from.
Data from the `seed-from` param will be loaded here.""")]
[doc("""Runs the IDR pipeline. Optionally seeding new synthetic data into the environment
when the `seed-from` param is supplied. When `--env` is "local", `--source-env` can be used
to load data from a different env into your local db.
""")]
[group('pipeline')]
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

[arg("env", long, pattern=env_pattern, help=env_help)]
[doc("Dump all data from the Snowflake database into local CSVs")]
[group('pipeline')]
extract-snowflake env:
    cd ./apps/bfd-pipeline-idr && BFD_ENV="{{ env }}" ./extract-snowflake.sh

[arg("env", long, pattern=env_pattern, help=env_help)]
[doc("Compares claims in BFD and IDR and verifies the data matches. Used for fuzz testing the pipeline.")]
[group('pipeline')]
idr-bfd-validator env:
    cd ./apps/utils/idr-bfd-validator && BFD_ENV="{{ env }}" ./run-validator.sh

[doc("installs npm dependencies for the model project")]
[group('model')]
install-model-dependencies:
    cd ./apps/bfd-model-idr && npm install

[doc("Compiles sushi files")]
[group('model')]
sushi: install-model-dependencies
    cd ./apps/bfd-model-idr && npm run sushi-build

[doc("Starts matchbox using docker compose")]
[group('model')]
start-matchbox:
    @echo "Starting matchbox. Run 'just matchbox-logs' to see log output\n"
    cd ./apps/bfd-model-idr && docker-compose up -d

[doc("Waits for matchbox to be ready")]
[group('model')]
_wait-for-matchbox: start-matchbox
    cd ./apps/bfd-model-idr && uv run wait_for_matchbox.py

[doc("Uploads sushi files to matchbox")]
[group('model')]
upload-sushi: sushi _wait-for-matchbox
    cd ./apps/bfd-model-idr && uv run upload_sushi.py

[doc("Stops the matchbox docker compose project")]
[group('model')]
stop-matchbox:
    cd ./apps/bfd-model-idr && docker-compose down

[doc("Shows logs from matchbox")]
[group('model')]
matchbox-logs:
    cd ./apps/bfd-model-idr && docker-compose logs --follow

[arg("type", long, help="Resource type to generate")]
[arg("all", long, value="1", help="Generate all resources")]
[doc("""Generates FHIR structure maps for a given resource.
If `--all` is supplied, all resources will be generated.
If no arguments are supplied, you can pick the resource interactively.""")]
[group('model')]
gen-structure-map type="" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./gen-structure-map.sh --type="{{ type }}" \
        {{ if all == "1" { "--all" } else { "" } }}

[arg("resource", long, help="Resource to transform")]
[arg("all", long, value="1", help="Transform all resources")]
[arg("profile-type", long, pattern="Basis|Regular|CMS", help="""Profile to transform with.
Profiles affect the elements returned in the resource""")]
[doc("""Runs the FHIR transform for a given resource.
If `--all` is supplied, all resources will be generated.
If no arguments are supplied, you can pick the resource interactively.""")]
[group('model')]
fhir-transform resource="" profile-type="CMS" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./fhir-transform.sh --resource "{{ resource }}" \
        --profile-type "{{ profile-type }}" {{ if all == "1" { "--all" } else { "" } }}

[arg("resource", long, help="Resource to test")]
[arg("all", long, value="1", help="Test all resources")]
[doc("""Runs the FHIR conformance test for a given resource.
If `--all` is supplied, all resources will be generated.
If no arguments are supplied, you can pick the resource interactively.""")]
[group('model')]
conformance-test resource="" *all: upload-sushi
    cd ./apps/bfd-model-idr && ./fhir-transform.sh --resource "{{ resource }}" \
        {{ if all == "1" { "--all" } else { "" } }}

[doc("Generates the v3 data dictionary")]
[group('model')]
generate-data-dictionary:
    cd ./apps/bfd-model-idr && uv run gen-dd

[arg("utn", long, help="UTN to generate sample with")]
[arg("source-directory", long, help="Directory to find prior auth CSV inputs")]
[arg("output-directory", long, help="Directory to save the output")]
[doc("Generates sample prior auth data to feed into the FHIR transform scripts")]
[group('model')]
generate-prior-auth-sample utn source-directory="" output-directory="":
    cd apps/bfd-model-idr && uv run generate-prior-auth-sample \
        --utn "{{ utn }}" \
        {{ if source-directory != "" { f"--source-directory {{ source-directory }}" } else { "" } }} \
        {{ if output-directory != "" { f"--output-directory {{ output-directory }}" } else { "" } }}

[arg("clm-uniq-id", long, help="Claim ID to generate sample with")]
[arg("source-directory", long, help="Directory to find claim CSV inputs")]
[arg("output-directory", long, help="Directory to save the output")]
[doc("Generates sample EOB data to feed into the FHIR transform scripts")]
[group('model')]
generate-eob-sample clm-uniq-id source-directory="" output-directory="":
    cd apps/bfd-model-idr && uv run generate-eob-sample \
        --clm-uniq-id "{{ clm-uniq-id }}" \
        {{ if source-directory != "" { f"--source-directory {{ source-directory }}" } else { "" } }} \
        {{ if output-directory != "" { f"--output-directory {{ output-directory }}" } else { "" } }}

[arg("bene-sk", long, help="Bene key to generate sample with")]
[arg("source-directory", long, help="Directory to find bene CSV inputs")]
[arg("output-directory", long, help="Directory to save the output")]
[doc("Generates sample bene data to feed into the FHIR transform scripts")]
[group('model')]
generate-bene-sample bene-sk source-directory="" output-directory="":
    cd apps/bfd-model-idr && uv run generate-bene-sample \
        --bene-sk "{{ bene-sk }}" \
        {{ if source-directory != "" { f"--source-directory {{ source-directory }}" } else { "" } }} \
        {{ if output-directory != "" { f"--output-directory {{ output-directory }}" } else { "" } }}

[arg("paths", help="""
Paths to CSVs or directories including CSVs that will be regenerated/updated with new
        columns. Updates are idempotent, meaning that passing in an existing table/CSV without
        any new columns being added to the synthetic data generation will result in a
        byte-identical output file. Take care to avoid providing a partial set of tables with
        foreign key constraints (e.g. BENE_SK) without providing the root table as this could
        result in broken output data""")]
[arg("patients", long, pattern="\\d*", help="""Number of NEW patients to generate.
Does not affect patients regenerated when a bene_htry file is provided""")]
[arg("exclude-empty", long, value="1", help="""Treat empty column values as non-existent
                        and allow the generator to generate new values""")]
[arg("force-ztm-static-rows", long, value="1", help="""
Allow \"zero-to-many\" rows (e.g. BENE_ENTLMT, c/d data, etc.) for a patient loaded from
a file to be generated. This will introduce new rows for patients that previously had
none. Useful if not all tables for a patient have been generated yet.""")]
[doc("""Generates patient data using the IDR schema to feed into the BFD pipeline.
The `paths` argument can be used to add columns to existing files.
Alternatively, use `--patients` to generate new patients.""")]
[group('model')]
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

[arg("paths", help="Paths to a directory or specific CSV files to generate data from")]
[arg("enable-samhsa", long, value="1", help="Enables generation of SAMHSA-related data")]
[arg("min-claims", long, pattern="\\d+", help="Minimum number of claims to generate per person")]
[arg("max-claims", long, pattern="\\d+", help="Maximum number of claims to generate per person")]
[arg("pac-gen", long, pattern="(no|if_none|always)", help="""
"Generate new partially-adjudicated claims data based on choice. `no` will never generate
pac data, `if_none`` will generate if the input claims data has no pac CLMs, and
`always` will force the generation always""")]
[arg("bene-sk-mode", long, pattern="(bene_hstry|clm|both)", help="""
Sets the mode for which input files from which distinct BENE_SKs are read. 'bene_hstry'
indicates that BENE_SKs are only loaded from BENE_HSTRY, 'clm' indicates loading from
only from CLM. 'both' indicates loading from both
""")]
[doc("""Generates claims data using the IDR schema to feed into the BFD pipeline.
The `paths` argument can be used to add columns to existing files.
Alternatively, if no claims files are provided in `paths`, new ones will be generated from scratch.""")]
[group('model')]
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

[arg("tablesample", long, help="Percent of table to sample")]
[arg("headless", long, value="1", help="Run in headless mode (outputs in the terminal instead of the web UI)")]
[arg("env", long, pattern=env_pattern, help=env_help)]
[doc("Runs locust regression tests against the specified env")]
[group('regression')]
regression-test env tablesample="0.25" headless="":
    BFD_ENV="{{ env }}" apps/utils/locust_tests/regression.sh \
        --table-sample-percent "{{ tablesample }}" \
        {{ if headless != "" { "--headless" } else { "" } }}

[arg("tablesample", long, help="Percent of the table to sample")]
[arg("limit", long, pattern="\\d+", help="Limit of unique claim IDs (not necessarily beneficiaries) to return from queries")]
[arg("env", long, pattern=env_pattern, help=env_help)]
[arg("concurrency", long, pattern="\\d+", help="Number of concurrent requests to make against the v3 Server")]
[doc("Runs SAMHSA regression tests against the specified env")]
[group('regression')]
samhsa-regression-test env tablesample="10" limit="300" concurrency="10":
    BFD_ENV="{{ env }}" apps/utils/samhsa-regression-tests/regression.sh \
        --tablesample "{{ tablesample }}" \
        --limit "{{ limit }}" \
        --concurrency "{{ concurrency }}"
