#!/bin/bash 

# INSTRUCTIONS #
# 1.  Uncomment and run part 1 of the script locally.. Fill in the database credentials and run the script
# 2.  Comment out part 1 and uncomment out part 2 and copy the input.txt file as well as this script file over to the prod-sbx server



# Stop after any failure
set -e 



###### PART 1 ######
#psqlhost=""   # Database host
#psqluser=""   # Database username
#psqlport=5432  # Database port
#psqlpass=""  # Database password
#psqldb=""   # Database

#psql "host=$psqlhost port=$psqlport dbname=$psqldb user=$psqluser password=$psqlpass" -P "footer=off"  -t -c "SELECT \"beneficiaryId\" FROM public.\"Beneficiaries\" WHERE \"beneficiaryId\" LIKE '-%'" --output=input.txt

###### END OF PART 1 ######



###### PART 2 ######

#   host=localhost:7443
#   cert=/usr/local/bfd-server/bluebutton-backend-test-data-server-client-test-keypair.pem
#   #host=prod-sbx.bfdcloud.net:443
#   #cert=~/client_data_server_local_test_env_dpr_keypair.pem

# # Fetch EOBs 
# curl_eobs() {
#   sudo curl -sb -H "Accept: application/json" --insecure --cert-type pem --cert $cert 'https://'$host'/v1/fhir/ExplanationOfBenefit?patient='$1'&_format=application%2Fjson%2Bfhir'
# }

# # Fetch Patient 
# curl_patient() {
#   sudo curl -sb -H "Accept: application/json" --insecure --cert-type pem --cert $cert 'https://'$host'/v1/fhir/Patient/'$1'?_format=json'
# }

# # Fetch Coverage 
# curl_coverage() {
#   sudo curl -sb -H "Accept: application/json" --insecure --cert-type pem --cert $cert 'https://'$host'/v1/fhir/Coverage?beneficiary='$1'&_format=application%2Fjson%2Bfhir'
# }

# input="input.txt"
# echo "{\"responses\":[" >> eob.json
# echo "{\"responses\":[" >> coverage.json
# echo "{\"responses\":[" >> patient.json
# while IFS= read -r line
# do
#   curl_eobs $line >> eob.json
#   curl_coverage $line >> coverage.json
#   curl_patient $line >> patient.json
#   echo "," >> eob.json
# echo "," >> coverage.json
# echo "," >> patient.json
# done < "$input"

# echo "]}" >> eob.json
# echo "]}" >> coverage.json
# echo "]}" >> patient.json

###### END OF PART 2 ######


# echo "All done successfully"