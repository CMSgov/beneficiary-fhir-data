#!/usr/bin/env bash
# Sets up SSM parameter store hierarchy containing settings and then installs
# the migrator's helm chart into the EKS POC cluster.

set -e

if [[ x$EKS_SSM_KEY_ID = x ]] ; then
  echo "error: Define the SSM path prefix in env var EKS_SSM_KEY_ID" 2>&1
  exit 1
fi
if [[ x$EKS_SSM_PREFIX = x ]] ; then
  echo "error: Define the SSM path prefix in env var EKS_SSM_PREFIX" 2>&1
  exit 1
fi
if [[ x$EKS_SSM_CONFIG_ROOT = x ]] ; then
  echo "error: Define the relative SSM config prefix in env var EKS_SSM_CONFIG_ROOT" 2>&1
  echo "       This should be relative to the EKS_SSM_PREFIX." 2>&1
  exit 1
fi
if [[ x$EKS_RDS_WRITER_ENDPOINT = x ]] ; then
  echo "error: Define the RDS writer node endpoint in env var EKS_RDS_WRITER_ENDPOINT" 2>&1
  exit 1
fi
if [[ x$EKS_ECR_REGISTRY = x ]] ; then
  echo "error: Define the ECR registry path in env var EKS_ECR_REGISTRY" 2>&1
  exit 1
fi

cd `dirname $0`

function set_const_param() {
    value=$1
    to=$2
    echo setting $to
    aws ssm put-parameter --name $to --value "$value" --type String --overwrite
}

function set_const_secure_param() {
    value=$1
    to=$2
    echo setting $to
    aws ssm put-parameter --name $to --value "$value" --type SecureString --overwrite --key-id $EKS_SSM_KEY_ID
}

function copy_param() {
    from=$1
    to=$2
    value=`aws ssm get-parameter --name $from --output text --query Parameter.Value`
    echo setting $to
    aws ssm put-parameter --name $to --value "$value" --type String --overwrite
}

function copy_secure_param() {
    from=$1
    to=$2
    value=`aws ssm get-parameter --name $from --with-decryption --output text --query Parameter.Value`
    echo setting $to
    aws ssm put-parameter --name $to --value "$value" --type SecureString --overwrite --key-id $EKS_SSM_KEY_ID
}

config_path="${EKS_SSM_PREFIX}/migrator/${EKS_SSM_CONFIG_ROOT}"

set_const_param \
  "5" \
  "$config_path/DATABASE_MAX_POOL_SIZE"

copy_param \
  "$EKS_SSM_PREFIX/migrator/nonsensitive/sqs_queue_name" \
  "$config_path/DB_MIGRATOR_SQS_QUEUE"

set_const_secure_param \
  "jdbc:postgresql://${EKS_RDS_WRITER_ENDPOINT}:5432/fhirdb" \
  "$config_path/DATABASE_URL"

copy_secure_param \
  "$EKS_SSM_PREFIX/migrator/sensitive/db_migrator_db_username" \
  "$config_path/DATABASE_USERNAME"

copy_secure_param \
  "$EKS_SSM_PREFIX/migrator/sensitive/db_migrator_db_password" \
  "$config_path/DATABASE_PASSWORD"

namespace=eks-test
chart=../helm/migrator

helm -n $namespace uninstall migrator || true
helm -n $namespace install migrator $chart \
  --set ssmHierarchies="{${config_path}}" \
  --set imageRegistry="${EKS_ECR_REGISTRY}/" \
  --values migrator-values.yaml
