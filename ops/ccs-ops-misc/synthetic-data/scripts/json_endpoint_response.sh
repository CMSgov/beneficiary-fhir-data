#!/bin/bash 

# This simple script is meant to test requesting EOBs while in an SSH session to a FHIR host in production
# It is useful as a smoke test after a data load 

# Stop after any failure
set -e 

  #host=localhost:7443 
  #cert=/usr/local/bfd-server/bluebutton-backend-test-data-server-client-test-keypair.pem
  host=prod-sbx.bfdcloud.net:443
  cert=~/client_data_server_local_test_env_dpr_keypair.pem

# Fetch EOBs 
curl_eobs() {
  #echo $1
  sudo curl --fail --silent -o /dev/null --insecure --cert-type pem --cert $cert 'https://'$host'/v1/fhir/ExplanationOfBenefit?patient='$1'&_format=application%2Fjson%2Bfhir'
}

# Fetch Patient 
curl_patient() {
  #echo $1
  sudo curl --fail --silent -o /dev/null --insecure --cert-type pem --cert $cert 'https://'$host'/v1/fhir/Patient/'$1'&_format=application%2Fjson%2Bfhir'
}

# Fetch Coverage 
curl_coverage() {
  #echo $1
  sudo curl --fail --silent -o /dev/null --insecure --cert-type pem --cert $cert 'https://'$host'/v1/fhir/Coverage?beneficiary='$1'&_format=application%2Fjson%2Bfhir'
}



psqlhost=""   # Database host
psqluser=""   # Database username
psqlport=5432  # Database port
psqlpass=""  # Database password
psqldb=""   # Database

psql "host=$psqlhost port=$psqlport dbname=$psqldb user=$psqluser password=$psqlpass" -P "footer=off"  -t -c "SELECT \"beneficiaryId\" FROM public.\"Beneficiaries\" WHERE \"beneficiaryId\" LIKE '-%'  limit 1" --output=input.txt

input="input.txt"
while IFS= read -r line
do
  curl_eobs $line >> results.txt
done < "$input"

rm $input

# echo 'Testing EOBs that failed before'
# curl_eobs -19990000010000
# curl_eobs -19990000009997
# curl_eobs -19990000000161
# echo 'Passed'

# echo 'About to fetch EOBs for each synthetic benficiary'

# for i in $(seq -f "%05g" 1 10000); 
# do
#   echo 'Fetching EOBs for '$i' of 10000'
#   curl_eobs -199900000$i
#   curl_eobs -200000000$i
#   curl_eobs -201400000$i
# done

# echo "All done successfully"