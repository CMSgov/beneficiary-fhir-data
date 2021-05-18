#!/bin/bash

set -o pipefail
set -e

YEAR="${1:-2020}"

if [[ "$YEAR" != "2019" && "$YEAR" != "2020" ]]; then
  echo "Invalid year specified...must be either 2019 or 2020!"
  exit ;
fi

# following must be passed in as either environment variables or as cmd-line args (default)
PGHOST="${DB_HOST:-$2}"
PGUSER="${DB_USER:-$3}"
PGPASSWORD="${DB_PSWD:-$4}"
# other vars that skirt security (a bit)
PGDATABASE="${DB_NAME:-fihrdb}"
export PGPORT="${DB_PORT:-5432}"

if [ -z "$PGHOST" ] || [ -z "$PGUSER" ] || [ -z "$PGPASSWORD" ]; then
    echo "*****  E r r o r - Missing required variables  *****"
    echo "$0 requires ENV variable(s) for: DB_HOST, DB_USER, DB_PSWD";
    echo "or";
    echo "$0 requires cmd-line args for: <year> <db host> <db username> <db password>";
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

echo "Begin processing year: $YEAR at: $(date +'%T.%31')"
SQL="select public.update_bene_monthly_with_delete('$YEAR');"

(( tot_rcds = 0 )) 
while :; do
  echo "Starting 20k transaction at: $(date +'%T.%31')"

  cnt=$(psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" --quiet --tuples-only -c "$SQL")
  ((cnt += 0))

  if [ "$cnt" -gt 0 ]; then
      tot_rcds+=$cnt
      echo "Current record count: $tot_rcds at: $(date +'%T.%31')"
  else
      break;
  fi
done

echo "All DONE at: $(date +'%T.%31')"
echo "TOTAL records processed: $tot_rcds"
exit 0;