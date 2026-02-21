#!/usr/bin/env bash

set -e

username="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/migrator/sensitive/db/username --with-decryption --query "Parameter.Value" --output text)"
password="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/migrator/sensitive/db/password --with-decryption --query "Parameter.Value" --output text)"
db_cluster="bfd-${BFD_ENV}-aurora-cluster"
db_endpoint="$(aws rds describe-db-clusters --db-cluster-identifier $db_cluster --query "DBClusters[0].Endpoint" --output text)"

script_dir=$(path=$(realpath "$0") && dirname "$path")
(
    cd "$script_dir"
    mvn flyway:migrate -Dflyway.url=jdbc:postgresql://$db_endpoint:5432/fhirdb "-Dflyway.user=$username" "-Dflyway.password=$password"

    echo "Applying grants to 'idr' schema..."
    export PGPASSWORD="$password"
    psql -h "$db_endpoint" -d fhirdb -U "$username" -c "GRANT SELECT ON ALL TABLES IN SCHEMA idr TO rds_iam;
                        GRANT SELECT ON ALL TABLES IN SCHEMA idr TO svc_bfd_server_1;
                        GRANT SELECT ON ALL TABLES IN SCHEMA idr TO svc_bfd_pipeline_1;
                        GRANT INSERT ON ALL TABLES IN SCHEMA idr TO svc_bfd_pipeline_1;
                        GRANT UPDATE ON ALL TABLES IN SCHEMA idr TO svc_bfd_pipeline_1;
                        GRANT DELETE ON ALL TABLES IN SCHEMA idr TO svc_bfd_pipeline_1;"

)
