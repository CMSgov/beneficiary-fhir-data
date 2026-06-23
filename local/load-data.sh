#!/bin/sh
set -e

echo "=== BFD Data Loader ==="
echo "Loading synthetic beneficiary data into the database..."

# Wait for DB to be ready
until pg_isready -h db -p 5432 -U bfd -d fhirdb; do
  echo "Waiting for database..."
  sleep 2
done

# Load the sample beneficiary data using COPY commands
# The rif-synthea directory contains CSV files that map to BFD tables
loaded=0
for csv_file in /app/rif-data/*.csv; do
  if [ -f "$csv_file" ]; then
    table_name=$(basename "$csv_file" .csv)
    echo "Loading $table_name from $csv_file..."
    psql "postgresql://${DB_USER}:${DB_PASSWORD}@db:5432/fhirdb" \
      -c "\COPY \"${table_name}\" FROM '${csv_file}' WITH (FORMAT csv, HEADER true)" \
      2>/dev/null && loaded=$((loaded + 1)) || echo "  Skipped ${table_name} (table may not match CSV structure)"
  fi
done

echo ""
echo "=== Data loading finished ($loaded files loaded) ==="
