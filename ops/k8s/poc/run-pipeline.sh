#!/usr/bin/env bash
# Sets up SSM parameter store hierarchy containing settings and then installs
# the pipeline's helm chart into the EKS POC cluster.

set -e

app_name=`basename $0`

mode=$1
if [[ x$mode = x ]] ; then
  echo "error: usage ${app_name} rda|ccw|random" 2>&1
  exit 1
fi

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
if [[ x$EKS_RDA_GRPC_HOST = x ]] ; then
  echo "error: Define the RDA server hostname in env var EKS_RDA_GRPC_HOST" 2>&1
  exit 1
fi
if [[ x$EKS_RDA_GRPC_PORT = x ]] ; then
  echo "error: Define the RDA server port in env var EKS_RDA_GRPC_PORT" 2>&1
  exit 1
fi
if [[ x$EKS_RDA_GRPC_AUTH_TOKEN = x ]] ; then
  echo "error: Define the RDA server authentication token in env var EKS_RDA_GRPC_AUTH_TOKEN" 2>&1
  exit 1
fi
if [[ x$EKS_S3_BUCKET_NAME = x ]] ; then
  echo "error: Define the S3 bucket name for CCW pipeline in env var EKS_S3_BUCKET_NAME" 2>&1
  exit 1
fi

cd `dirname $0`

UNDEFINED="--undefined--"

function set_const_params() {
    value=$1
    to=$2
    while [[ "x$1" != x ]] ; do
      echo setting $to
      aws ssm put-parameter --name $to --value "$value" --type String --overwrite
      shift 2
      value=$1
      to=$2
    done
}

function set_const_secure_params() {
    value=$1
    to=$2
    while [[ "x$1" != x ]] ; do
      echo setting $to
      aws ssm put-parameter --name $to --value "$value" --type SecureString --overwrite --key-id $EKS_SSM_KEY_ID
      shift 2
      value=$1
      to=$2
    done
}

function copy_params() {
    from=$1/$3
    to=$2/$3
    while [[ x$1 != x ]] ; do
      echo getting $from
      value=`aws ssm get-parameter --name $from --output text --query Parameter.Value` || value=$UNDEFINED
      if [[ "x$value" != x$UNDEFINED ]] ; then
        echo setting $to
        aws ssm put-parameter --name $to --value "$value" --type String --overwrite
      fi
      shift 3
      from=$1/$3
      to=$2/$3
    done
}

function copy_secure_params() {
    from=$1/$3
    to=$2/$3
    while [[ x$1 != x ]] ; do
      echo getting $from
      value=`aws ssm get-parameter --name $from --with-decryption --output text --query Parameter.Value` || value=$UNDEFINED
      if [[ "x$value" != x$UNDEFINED ]] ; then
        echo setting $to
        aws ssm put-parameter --name $to --value "$value" --type SecureString --overwrite --key-id $EKS_SSM_KEY_ID
      fi
      shift 3
      from=$1/$3
      to=$2/$3
    done
}

base_path="${EKS_SSM_PREFIX}/pipeline"
shared_path="${base_path}/shared"
ccw_path="${base_path}/ccw"
rda_path="${base_path}/rda"
base_config_path="${shared_path}/${EKS_SSM_CONFIG_ROOT}"
shared_config_path="${base_config_path}/common"
ccw_config_path="${base_config_path}/ccw"
rda_config_path="${base_config_path}/rda"

copy_params \
  "${ccw_path}" "${shared_config_path}" /nonsensitive/ccw/idempotency_enabled \
  "${ccw_path}" "${shared_config_path}" /nonsensitive/ccw/job/batch_size \
  "${ccw_path}" "${shared_config_path}" /nonsensitive/ccw/job/claims/batch_size \
  "${ccw_path}" "${shared_config_path}" /nonsensitive/ccw/job/queue_size_multiple \
  "${ccw_path}" "${shared_config_path}" /nonsensitive/ccw/job/claims/queue_size_multiple \
  "${ccw_path}" "${shared_config_path}" /nonsensitive/micrometer_cw/interval \
  "${ccw_path}" "${shared_config_path}" /nonsensitive/micrometer_cw/namespace \
  "${ccw_path}" "${shared_config_path}" /nonsensitive/micrometer_cw/enabled \

copy_secure_params \
  "${shared_path}" "${shared_config_path}" /sensitive/hicn_hash/iterations \
  "${shared_path}" "${shared_config_path}" /sensitive/hicn_hash/pepper \
  "${shared_path}" "${shared_config_path}" /sensitive/db/username \
  "${shared_path}" "${shared_config_path}" /sensitive/db/password \


set_const_secure_params \
  "jdbc:postgresql://${EKS_RDS_WRITER_ENDPOINT}:5432/fhirdb" "${shared_config_path}/sensitive/db/url" \
  "100" "${shared_config_path}/sensitive/hicn_hash/cache_size" \

set_const_params \
  "180" "${shared_config_path}/nonsensitive/rda/job/error_expire_days" \
  "false" "${shared_config_path}/nonsensitive/ccw/job/enabled" \
  "4" "${shared_config_path}/nonsensitive/loader_thread_count" \
  "8" "${shared_config_path}/nonsensitive/ccw/job/claims/loader_thread_count" \
  "false" "${shared_config_path}/nonsensitive/rda/job/enabled" \
  "3600" "${shared_config_path}/nonsensitive/rda/job/interval_seconds" \
  "20" "${shared_config_path}/nonsensitive/rda/job/batch_size" \
  "5" "${shared_config_path}/nonsensitive/rda/job/write_thread_count" \
  "true" "${shared_config_path}/nonsensitive/rda/job/process_dlq" \
  "false" "${shared_config_path}/nonsensitive/micrometer_cw/enabled" \

case $mode in
  ccw)
    # CCW Mode
    set_const_params \
      "true" "${ccw_config_path}/nonsensitive/ccw/job/enabled" \

    set_const_secure_params \
      "${EKS_S3_BUCKET_NAME}" "${ccw_config_path}/sensitive/ccw/s3_bucket_name" \

    ;;
  random)
    # Still an rda pipeline, just ussing random data.
    mode=rda

    # Random RDA Server Mode
    set_const_params \
      "InProcess" "${shared_config_path}/nonsensitive/rda/grpc/server_type" \
      "Random" "${shared_config_path}/nonsensitive/rda/grpc/inprocess_server/mode" \
      "42" "${shared_config_path}/nonsensitive/rda/grpc/inprocess_server/random/seed" \
      "2500" "${shared_config_path}/nonsensitive/rda/grpc/inprocess_server/random/max_claims" \
      "true" "${rda_config_path}/nonsensitive/rda/job/enabled" \

    ;;
  rda)
    # RDA API Call mode
    set_const_params \
      "Remote" "${shared_config_path}/nonsensitive/rda/grpc/server_type" \
      "600" "${shared_config_path}/nonsensitive/rda/grpc/max_idle_seconds" \
      "true" "${rda_config_path}/nonsensitive/rda/job/enabled" \

    set_const_secure_params \
      "${EKS_RDA_GRPC_HOST}" "${rda_config_path}/sensitive/rda/grpc/host" \
      "${EKS_RDA_GRPC_PORT}" "${rda_config_path}/sensitive/rda/grpc/port" \
      "${EKS_RDA_GRPC_AUTH_TOKEN}" "${rda_config_path}/sensitive/rda/grpc/auth_token" \

    ;;
  *)
    echo Invalid value for run_mode: $run_mode 1>&2
    exit 1
esac

namespace=eks-test
chart=../helm/pipeline
mode_config_path="${base_config_path}/${mode}"

helm -n $namespace uninstall "pipeline-${mode}" || true
helm -n $namespace install "pipeline-${mode}" $chart \
  --set ssmHierarchies="{${shared_config_path}/sensitive,${shared_config_path}/nonsensitive,${mode_config_path}/sensitive,${mode_config_path}/nonsensitive}" \
  --set imageRegistry="${EKS_ECR_REGISTRY}/" \
  --values pipeline-values.yaml \
  --values "pipeline-values-${mode}.yaml"
