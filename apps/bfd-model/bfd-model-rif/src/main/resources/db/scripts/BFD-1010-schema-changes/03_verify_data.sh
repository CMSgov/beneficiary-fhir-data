#!/usr/bin/env bash
set -eo pipefail
export MAX_JOBS="${MAX_JOBS:-1}" # defaults to one, probably max around 4
export PGHOST="${PGHOST:-}"
export PGUSER="${PGUSER:-}"
export PGPASSWORD="${PGPASSWORD:-}"
export PGPORT="${PGPORT:-5432}"
export PGDATABASE="${PGDATABASE:-}"

# when DRY_RUN=true (the default), the script will echo the commands it would run against the db
export DRY_RUN="${DRY_RUN:-false}"

# boolean to limit table data verification to a subset of tables
# defaults to false which runs a complete verification
# boolean to limit table processing to a subset of tables
export MIN_TABLES=${1:-false}

# these will be run in background jobs, up to $MAX_JOBS at a time
# not necessarily in order
if ! [ "${MIN_TABLES}" = true ] ; then
verify_tables=(
beneficiaries
beneficiaries_history
beneficiaries_history_invalid_beneficiaries
beneficiary_monthly
medicare_beneficiaryid_history
medicare_beneficiaryid_history_invalid_beneficiaries
carrier_claims
carrier_claim_lines
dme_claims
dme_claim_lines
hha_claims
hha_claim_lines
hospice_claims
hospice_claim_lines
inpatient_claims
inpatient_claim_lines
outpatient_claims
outpatient_claim_lines
partd_events
snf_claims
snf_claim_lines
)
else
verify_tables=(
beneficiaries
beneficiary_monthly
carrier_claims
carrier_claim_lines
)
echo "Will be running using a subset of db tables..."
fi

setupPg() {
  # have 2 choices on how psql gets password:
  #  1) use a connection URL
  #  2) use a ~/.pgpass file; format: hostname:port:database:username:password
  #
  # we'll be using .pgpass
  # if .pgpass exists, move it to side for now
  if [[ -f ~/.pgpass ]]; then
    mv ~/.pgpass ~/.pgpass.orig
  fi
  # create and protect .pgpass
  echo "$PGHOST:$PGPORT:$PGDATABASE:$PGUSER:$PGPASSWORD" > ~/.pgpass
  chmod go-rwx ~/.pgpass
}

restorePg() {
  if [[ -f ~/.pgpass.orig ]]; then
    mv ~/.pgpass.orig ~/.pgpass
  fi
}

testDbConnection() {
  echo "Testing db connectivity..."
  now=$(psql -h $PGHOST -U $PGUSER -d $PGDATABASE --quiet --tuples-only -c "select NOW();")
  if [[ "$now" == *"20"* ]]; then
    echo "db connectivity: OK"
  else
    echo "Failed to connect to the database. Did you update the ${PWD}/.env file?"
    exit 1
  fi
}

# generates/loads .env file and tests db connection
setup(){
  if ! [[ -f .env ]]; then
    printf "Generating .env file.. "
    echo -e "export PGHOST=" > .env
    echo -e "export PGPORT=5432" >> .env
    echo -e "export PGUSER=" >> .env
    echo -e "export PGPASSWORD=" >> .env
    echo -e "export PGDATABASE=" >> .env
    echo -e "export MAX_JOBS=1" >> .env
    echo -e "export DRY_RUN=true" >> .env
    echo "OK"
    echo "Please update ${PWD}/.env with the appropriate database credentials and run the script again."
    exit
  else
    if $DRY_RUN; then
      echo "DRY_RUN=true.. skipping db connection check"
    else
      # shellcheck disable=SC1091 # tell shellcheck not to worry about checking this .env file
      source .env
      setupPg
      testDbConnection
    fi
  fi
}

# load the given table
# $1 == table name to process - this assumes all tables have a matching ./insert_${tbl_name}.sql file
load_file(){
  local start end tbl_name psql_cmd
  start=$SECONDS
  # I'm not sure how noisy the output is, but if it's a lot, displaying it will slow down the operation
  # due to console paging, if it does, either redirect the output to /dev/null or to a file if you want
  # to review the output later. If you do output to files, and there is a lot of output, be mindful of
  # the amount of freespace on the host. 
  tbl_name="$1"
  psql_cmd="psql -h $PGHOST -U $PGUSER -d $PGDATABASE --quiet --tuples-only -f ./verify_${tbl_name}.sql"
  $DRY_RUN && psql_cmd="echo $psql_cmd"

  echo "  -> start processing table ${tbl_name} at: $(date +'%T')"
  #if $psql_cmd; then                            # show the output on the console
  #if $psql_cmd >/dev/null; then                 # hide the output
  if $psql_cmd > "verify_${tbl_name}.log"; then  # redirect command output to a file
    $DRY_RUN && sleep "$(shuf -i 0-2 -n 1)"      # randomly sleep to simulate background jobs
    end=$SECONDS; duration=$(( end - start ))
    echo "  -> successfully verifed $tbl_name at: $(date +'%T'); (took $((duration / 60)) minutes)"
  else
    end=$SECONDS; duration=$(( end - start ))
    echo "  -x failed to verify $tbl_name at: $(date +'%T'); (after $((duration / 60)) minutes)"
    exit 1
  fi
}

# load the given table
# $1 == table name to process - this assumes all tables have a matching ./insert_${tbl_name}.sql file
create_verification_table(){
  psql_cmd="psql -h $PGHOST -U $PGUSER -d $PGDATABASE --quiet --tuples-only -f ./migration_errors_table.sql"
}

# processes $MAX_JOBS number of tables (files) in parallel
# $1 a list of tables ex: "process_tables snf_claims snf_claim_lines"
process_tables(){
  while [[ -n "$1" ]]; do
    # always try to keep the max number of jobs running the background
    while [[ $(jobs -r | wc -l | tr -d " ") < $((MAX_JOBS+1)) ]]; do
      load_file "$1" &
      shift
    done
  done
  wait # waits until all background jobs are finished
}

# load and parse .env file
setup

# track overall time
total_start=$SECONDS

# migration_errors table 
echo "CREATING MIGRATION_ERRORS TABLE..."
create_verification_table
echo "migration_errors table created"
echo

# parent tables
echo "VERIFY TABLES..."
parent_start=$SECONDS
for t in "${verify_tables[@]}"; do
  load_file "$t"
done
parent_end=$SECONDS; duration=$(( parent_end - parent_start ))
echo "tables verified after ~$((duration / 60)) minutes"
echo

# done
total_end=$SECONDS; duration=$(( total_end - total_start ))
echo "All DONE"
restorePg
echo "Total Time: ~$((duration / 60)) minutes"
