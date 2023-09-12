#!/bin/bash

set -o pipefail
set -e

CERT_FILE_OR_DIR="$1"
MODE="${2:-prod}"
SHOW_SSM="${3:-false}"
SSM_DIR=""
SUBJ_CN=""

if [[ "${MODE}" == "prod" ]]; then
 SSM_DIR="/bfd/prod/server/client_certs"
elif [[ "${MODE}" == "test" ]]; then
 SSM_DIR="/bfd/test/server/client_certs"
elif [[ "${MODE}" == "prod-sbx" ]]; then
 SSM_DIR="/bfd/test/server/client_certs"
else
  echo "Invalid mode specified...must be either: [prod | prod-sbx | test]"
  exit ;
fi


function getCertCN () {
  cn_val=$(openssl x509 -noout -subject -nameopt multiline -in $1  | sed -n 's/ *commonName *= //p')
	if [[ -z "$cn_val" ]]; then
      echo "Failed to derive CN from provided file: $1...exiting!"
      exit ;
  fi
  # replace any spaces with underscores
  cn_val="${cn_val// /_}"
	echo $cn_val
}

function updateSsm () {
    SSM_KEY="${SSM_DIR}/${1}"
    aws ssm put-parameter --name "$SSM_KEY" --type "String" --overwrite --value "$(cat $2)"
    echo "Updated SSM Parameter Store using: $SSM_KEY"
    
    if [[ "${SHOW_SSM}" == true ]]; then
      aws ssm get-parameter --name $SSM_KEY
    fi
    echo "-------------------------------------------"
    echo " "
}

function checkFileFromDir {
  if grep -q -e "-----BEGIN CERTIFICATE-----" "$1" && grep -q -e "-----END CERTIFICATE-----" "$1"; then
    echo "+++++++++++++++++++++++++++++++++++++++++++"
    SUBJ_CN=$(getCertCN $1)
    updateSsm $SUBJ_CN $1
  else
    echo "***** Ignoring file that does not appear to be PEM-encoded cert: $1"
  fi
}

# are we dealing with a single file or directory of files?
if [[ -f "$CERT_FILE_OR_DIR" ]]; then
  echo "+++++++++++++++++++++++++++++++++++++++++++"
  SUBJ_CN=$(getCertCN $CERT_FILE_OR_DIR)
  updateSsm $SUBJ_CN $CERT_FILE_OR_DIR
elif [[ -d "$CERT_FILE_OR_DIR" ]]; then
  for fn in $CERT_FILE_OR_DIR/*;
  do checkFileFromDir $fn; done
else
  echo "$CERT_FILE_OR_DIR is neither a file or directory...ignoring"
fi

exit 0;