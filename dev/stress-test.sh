#!/bin/bash

# During our first Beneficiary FHIR Server game day, this script was run in
# our TEST environment, as follows:
#
#    $ sudo no_proxy=localhost /u01/jboss/stress-test.sh \
#        -a 'https://localhost:7443/v1/fhir/ExplanationOfBenefit?_format=application%2Fjson%2Bfhir&patient=5303418' \
#        -c 200 -r 2000
#
# This script was derived from this very handy gist:
# https://gist.github.com/cirocosta/de576304f1432fad5b3a


#### Default Configuration
CONCURRENCY=4
REQUESTS=100
ADDRESS="http://localhost:8080/"

show_help() {
cat << EOF
Naive Stress Test with cURL.
Usage: ./stress-test.sh [-a ADDRESS] [-c CONCURRENCY] [-r REQUESTS]
Params:
  -a  address to be tested.
      Defaults to localhost:8080
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

while getopts ":a:c:r:h" opt; do
  case $opt in
    a)
      ADDRESS=$OPTARG
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

queryRepeatedly() {
  echo "Job $1: started."
  for j in `seq 1 $REQUESTS`; do
    curl --silent --output /dev/null --insecure --cert-type pem --cert /u01/jboss/bluebutton-backend-test-data-server-client-test-keypair.pem "$ADDRESS"
    echo "  Query $1.$j done."
  done
  echo "Job $1: done."
}

for i in `seq 1 $CONCURRENCY`; do
   queryRepeatedly $i & pidlist="$pidlist $!"
done

# Execute and wait
FAIL=0
for job in $pidlist; do
  echo $job
  wait $job || let "FAIL += 1"
done

# Verify if any failed
if [ "$FAIL" -eq 0 ]; then
  echo "SUCCESS!"
else
  echo "Failed Requests: ($FAIL)"
fi
