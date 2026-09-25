#!/usr/bin/env bash
set -Eeuo pipefail

# argc doesn't work here because the arg parsing gets messed up
# when sourcing the script
load_mode="$1"
if [ "$load_mode" != "prod" ]; then
	is_synthetic=true
	creds_prefix="synthetic_env"
else
	is_synthetic=false
	creds_prefix="idr"
fi

IDR_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${creds_prefix}_username --with-decryption --query "Parameter.Value" --output text)"
export IDR_USERNAME
IDR_PRIVATE_KEY="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${creds_prefix}_private_key --with-decryption --query "Parameter.Value" --output text)"
export IDR_PRIVATE_KEY
IDR_ACCOUNT="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${creds_prefix}_account --with-decryption --query "Parameter.Value" --output text)"
export IDR_ACCOUNT
IDR_WAREHOUSE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${creds_prefix}_warehouse --with-decryption --query "Parameter.Value" --output text)"
export IDR_WAREHOUSE
IDR_DATABASE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${creds_prefix}_database --with-decryption --query "Parameter.Value" --output text)"
export IDR_DATABASE

if [ "$is_synthetic" = false ]; then
	IDR_EDP_DATABASE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/idr_edp_database --with-decryption --query "Parameter.Value" --output text)"
	export IDR_EDP_DATABASE
fi

# TODO: remove these at some point
# useful for testing the initial claim load
# export IDR_MIN_CLAIM_NCH_TRANSACTION_DATE=2014-06-30
# export IDR_MIN_CLAIM_SS_TRANSACTION_DATE=2021-03-01
# export IDR_LOAD_TYPE=initial
# export IDR_PARTITION_TYPE=day
# export IDR_LATEST_CLAIMS=0
# export IDR_ENABLE_DATE_PARTITIONS=0
# export IDR_MAX_TASKS=100
