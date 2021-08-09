#!/bin/bash

set -o pipefail
set -e

# following must be passed in as either environment variables or as cmd-line args (default)
PGHOST="${DB_HOST:-$1}"
PGUSER="${DB_USER:-$2}"
PGPASSWORD="${DB_PSWD:-$3}"

# other vars that skirt security (a bit)
PGDATABASE="${DB_NAME:-fihr}"
export PGPORT="${DB_PORT:-5432}"

if [ -z "$PGHOST" ] || [ -z "$PGUSER" ] || [ -z "$PGPASSWORD" ]; then
    echo "*****  E r r o r - Missing required variables  *****"
    echo "$0 requires ENV variable(s) for: DB_HOST, DB_USER, DB_PSWD";
    echo "or";
    echo "$0 requires cmd-line args for: <db host> <db username> <db password>";
    exit 1;
fi

echo "Testing db connectivity..."
now=$(psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" --quiet --tuples-only -c "select NOW();")
if [[ "$now" == *"2021"* ]]; then
  echo "db connectivity: OK"
else
  echo "db connectivity: FAILED...exiting"
  exit 1;
fi

sql_files=(
"./verify_beneficiaries.sql" \
"./verify_beneficiaries_history.sql" \
"./verify_beneficiaries_history_invalid_beneficiaries.sql" \
"./verify_beneficiary_monthly.sql" \
#"./verify_loaded_batches.sql" \
"./verify_medicare_beneficiaryid_history.sql" \
"./verify_medicare_beneficiaryid_history_invalid_beneficiaries.sql" \
"./verify_carrier_claims.sql" \
"./verify_carrier_claim_lines.sql" \
"./verify_dme_claims.sql" \
"./verify_dme_claim_lines.sql" \
"./verify_hha_claims.sql" \
"./verify_hha_claim_lines.sql" \
"./verify_hospice_claims.sql" \
"./verify_hospice_claim_lines.sql" \
"./verify_inpatient_claims.sql" \
"./verify_inpatient_claim_lines.sql" \
"./verify_outpatient_claims.sql" \
"./verify_outpatient_claim_lines.sql" \
"./verify_partd_events.sql" \
"./verify_snf_claims.sql" \
"./verify_snf_claim_lines.sql" )

echo "Begin verifying database..."
for SQL in "${sql_files[@]}"
do
   : 
   # do whatever on $i
   echo "begin processing ${SQL} : $(date +'%T.%31')"
   psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" --quiet --tuples-only -f "$SQL"
   echo "finished processing ${SQL} : $(date +'%T.%31')"
done

echo "All DONE at: $(date +'%T.%31')"
exit 0;