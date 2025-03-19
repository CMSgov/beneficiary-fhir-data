#!/usr/bin/env bash

IDR_USERNAME="$(aws ssm get-parameter --name /bfd/3913-prod/pipeline/sensitive/idr_username --with-decryption --query "Parameter.Value")"
export IDR_USERNAME
IDR_PASSWORD="$(aws ssm get-parameter --name /bfd/3913-prod/pipeline/sensitive/idr_password --with-decryption --query "Parameter.Value")"
export IDR_PASSWORD
IDR_ACCOUNT="$(aws ssm get-parameter --name /bfd/3913-prod/pipeline/sensitive/idr_account --with-decryption --query "Parameter.Value")"
export IDR_ACCOUNT
IDR_WAREHOUSE="$(aws ssm get-parameter --name /bfd/3913-prod/pipeline/sensitive/idr_warehouse --with-decryption --query "Parameter.Value")"
export IDR_WAREHOUSE
IDR_DATABASE="$(aws ssm get-parameter --name /bfd/3913-prod/pipeline/sensitive/idr_database --with-decryption --query "Parameter.Value")"
export IDR_DATABASE
IDR_SCHEMA="$(aws ssm get-parameter --name /bfd/3913-prod/pipeline/sensitive/idr_schema --with-decryption --query "Parameter.Value")"
export IDR_SCHEMA
