#!/usr/bin/env bash
set -eo pipefail
export MAX_JOBS="${MAX_JOBS:-1}" # defaults to one, probably max around 4
export PGHOST="${PGHOST:-}"
export PGUSER="${PGUSER:-}"
export PGPASSWORD="${PGPASSWORD:-}"
export PGPORT="${PGPORT:-5432}"
export PGDATABASE="${PGDATABASE:-}"

# we will process these one by one in order (I do not know which tables are parents, just guessing)
parent_tables=(beneficiaries beneficiaries_history)

# these we will run in background jobs, up to $MAX_JOBS at a time
child_tables=(
  beneficiary_monthly
  beneficiaries_history_invalid
  loaded_batches
  medicare_beneficiaryid_history
  medicare_beneficiaryid_history_invalid
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

setup(){
  if ! [[ -f .env ]]; then
    printf "Generating .env file.. "
    echo -e "export PGHOST=\nexport PGPORT=5432\nexport PGUSER=\nexport PGPASSWORD=\nexport PGDATABASE=" > .env
    echo -e "export MAX_JOBS=1\n" >> .env
    echo "OK"
    echo "Please update $(PWD)/.env with the appropriate database credentials and run the script again."
    exit
  else
    # shellcheck disable=SC1091 # tell shellcheck not to worry about checking this .env file
    source .env
    if ! psql --quiet --tuples-only -c "select NOW();" >/dev/null 2>&1; then
      echo "Failed to connect to the database. Did you update the $(PWD)/.env file?"
      exit 1
    fi
  fi
}

# load the given table
# $1 == table name to process - this assumes all tables have a matching ./insert_${tbl_name}.sql file
load_file(){
  # I'm not sure how noisy the output is, but if it's a lot, displaying it will slow down the operation
  # due to console paging, if it does, either redirect the output to /dev/null or to a file if you want
  # to review the output later. If you do output to files, and there is a lot of output, be mindful of
  # the amount of freespace on the host. 
  tbl_name="$1"
  psql_cmd="echo psql --quiet --tuples-only -f ./insert_${tbl_name}.sql"
  #if $psql_cmd; then                            # show the output on the console
  #if $psql_cmd >/dev/null; then                 # hide the output
  if $psql_cmd > "insert_${tbl_name}.log"; then  # redirect output to a file
    # sleep "$(shuf -i 0-1 -n 1)"  # randomly sleep (used for testing background jobs)
    echo "  -> successfully loaded $tbl_name"
  else
    echo "  -x failed to load $tbl_name. Aborting"
    exit 1
  fi
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

# parent tables
echo "LOADING PARENT TABLES..."
parent_start=$SECONDS
for t in "${parent_tables[@]}"; do
  load_file "$t"
done
parent_end=$SECONDS; duration=$(( parent_end - parent_start ))
echo "parent tables loaded after ~$((duration / 60)) minutes"
echo

# process child tables
echo "LOADING CHILD TABLES..."; child_start=$SECONDS
process_tables "${child_tables[@]}" # processes all child_tables
child_end=$SECONDS; duration=$(( child_end - child_start ))
echo "child tables loaded after ~$((duration / 60)) minutes"

# done
total_end=$SECONDS; duration=$(( total_end - total_start ))
echo -e "\nAll DONE"
echo "Total Time: ~$((duration / 60)) minutes"
