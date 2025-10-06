#!/usr/bin/env bash

set -e

script_dir=$(path=$(realpath "$0") && dirname "$path")
(
    cd "$script_dir"
    mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/fhirdb -Dflyway.user=bfd -Dflyway.password=InsecureLocalDev
)
