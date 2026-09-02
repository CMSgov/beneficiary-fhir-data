#!/usr/bin/env bash

#set -e

IDR_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_username --with-decryption --query "Parameter.Value" --output text)"
export IDR_USERNAME
readonly IDR_USERNAME
IDR_PRIVATE_KEY="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_private_key --with-decryption --query "Parameter.Value" --output text)"
export IDR_PRIVATE_KEY
export IDR_PRIVATE_KEY
IDR_ACCOUNT="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_account --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_ACCOUNT
export IDR_ACCOUNT
IDR_WAREHOUSE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_warehouse --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_WAREHOUSE
export IDR_WAREHOUSE
IDR_DATABASE="BFD_TEMP"
readonly IDR_DATABASE
export IDR_DATABASE
IDR_SCHEMA="CMS_VDM_VIEW_MDCR_PRD"
readonly IDR_SCHEMA
export IDR_SCHEMA
