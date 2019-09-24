#!/bin/bash

# During our first Beneficiary FHIR Server game day, this script was run in
# our TEST environment, as follows:
#
#    $ sudo no_proxy=localhost /u01/jboss/stress-test.sh \
#        -u 'https://localhost:7443/v1/fhir' \
#        -f bene-ids.csv \
#        -c 200 -r 2000
#
# The beneficiary ID files should have one valid beneficiary ID per line, with no header row.
# It can be generated in `psql` by running:
#
#     > \copy (SELECT "beneficiaryId" FROM "Beneficiaries") to bene-ds.csv with csv
#
# This script was derived from this very handy gist:
# https://gist.github.com/cirocosta/de576304f1432fad5b3a


#### Default Configuration
URL_BASE='https://localhost:7443/v1/fhir'
BENE_IDS_FILE='bene-ids.csv'
CONCURRENCY=4
REQUESTS=100

show_help() {
cat << EOF
Naive Stress Test with cURL.
Usage: ./stress-test.sh [-a ADDRESS] [-c CONCURRENCY] [-r REQUESTS]
Params:
  -u  address to be tested.
      Defaults to localhost:8080
  -f  file with beneficiary IDs to query.
      Defaults to bene-ids.csv
  -c  conccurency: how many process to spawn
      Defaults to 1
  -r  number of requests per process
      Defaults to 10
  -h  show this help text
Example:
  $ ./stress-test.sh -c 4 -p 100 (400 requests to localhost:8080)
EOF
}


#### CLI

while getopts ":u:f:c:r:h" opt; do
  case $opt in
    u)
      URL_BASE=$OPTARG
      ;;
    f)
      BENE_IDS_FILE=$OPTARG
      ;;
    c)
      CONCURRENCY=$OPTARG
      ;;
    r)
      REQUESTS=$OPTARG
      ;;
    h)
      show_help
      exit 0
      ;;
    \?)
      show_help >&2
      echo "Invalid argument: $OPTARG" &2
      exit 1
      ;;
  esac
done

shift $((OPTIND-1))

#### Main

trap "kill 0" SIGINT

# Prints out the specified message.
#
# Params:
# * $1: the message to log
log() {
  echo "$(date --iso-8601=s): $1"
}

queryRepeatedly() {
  set -e
  set -o pipefail
  log "Job $1: started."
  for j in `seq 1 $REQUESTS`; do
    BENE_ID="${BENE_IDS[${RANDOM} % ${#BENE_IDS[@]}]}"
    log "  Query $1.$j (BENE_ID='${BENE_ID}'): starting..."
    SECONDS_START="$(date +%s)"
    PATIENT_BYTES="$(sudo curl --silent --insecure --cert-type pem --cert /jboss/bluebutton-backend-test-data-server-client-test-keypair.pem "${URL_BASE}/Patient/${BENE_ID}?_format=application%2Fjson%2Bfhir" | wc -c)"
    COVERAGE_BYTES="$(sudo curl --silent --insecure --cert-type pem --cert /jboss/bluebutton-backend-test-data-server-client-test-keypair.pem "${URL_BASE}/Coverage?beneficiary=${BENE_ID}&_format=application%2Fjson%2Bfhir" | wc -c)"
    EOB_BYTES="$(sudo curl --silent --insecure --cert-type pem --cert /jboss/bluebutton-backend-test-data-server-client-test-keypair.pem "${URL_BASE}/ExplanationOfBenefit?patient=${BENE_ID}&_format=application%2Fjson%2Bfhir" | wc -c)"
    SECONDS_END="$(date +%s)"
    SECONDS=$((SECONDS_END-SECONDS_START))
    log "  Query $1.$j (BENE_ID='${BENE_ID}'): complete (SECONDS='${SECONDS}', PATIENT_BYTES='${PATIENT_BYTES}', COVERAGE_BYTES='${COVERAGE_BYTES}', EOB_BYTES='${EOB_BYTES}')."
  done
  log "Job $1: done."
}

# Log startup.
log "Starting stress test: URL_BASE='${URL_BASE}', BENE_IDS_FILE='${BENE_IDS_FILE}', CONCURRENCY='${CONCURRENCY}', REQUESTS='${REQUESTS}'."

# Parse the bene IDs file to an inmemory array.
log 'Beneficiary IDs: parsing...'
readarray -t BENE_IDS < "${BENE_IDS_FILE}"
log 'Beneficiary IDs: parsed.'

# Start up the concurrent workers.
for i in `seq 1 $CONCURRENCY`; do
   queryRepeatedly $i & pidlist="$pidlist $!"
done

# Wait for the concurrent workers to all complete.
FAIL=0
for job in $pidlist; do
  #log $job
  wait $job || let "FAIL += 1"
done

# Verify if any failed
if [ "$FAIL" -eq 0 ]; then
  log "SUCCESS!"
else
  log "Failed Requests: ($FAIL)"
fi
