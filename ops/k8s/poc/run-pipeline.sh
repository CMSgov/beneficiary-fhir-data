#!/usr/bin/env bash
# Sets up SSM parameter store hierarchy containing settings and then installs
# the pipeline's helm chart into the EKS POC cluster.

set -e

app_name=`basename $0`

mode=$1
if [[ x$mode = x ]] ; then
  echo "error: usage ${app_name} rda|rif|random" 2>&1
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
    while [[ x$1 != x ]] ; do
      echo setting $to
      aws ssm put-parameter --name $to --value "$value" --type String --overwrite
      shift 2
      from=$1
      to=$2
    done
}

function set_const_secure_params() {
    value=$1
    to=$2
    while [[ x$1 != x ]] ; do
      echo setting $to
      aws ssm put-parameter --name $to --value "$value" --type SecureString --overwrite --key-id $EKS_SSM_KEY_ID
      shift 2
      from=$1
      to=$2
    done
}

function copy_params() {
    from=$1
    to=$2
    while [[ x$1 != x ]] ; do
      value=`aws ssm get-parameter --name $from --output text --query Parameter.Value` || value=$UNDEFINED
      if [[ x$value != x$UNDEFINED ]] ; then
        echo setting $to
        aws ssm put-parameter --name $to --value "$value" --type String --overwrite
      fi
      shift 2
      from=$1
      to=$2
    done
}

function copy_secure_params() {
    from=$1
    to=$2
    while [[ x$1 != x ]] ; do
      value=`aws ssm get-parameter --name $from --with-decryption --output text --query Parameter.Value` || value=$UNDEFINED
      if [[ x$value != x$UNDEFINED ]] ; then
        echo setting $to
        aws ssm put-parameter --name $to --value "$value" --type SecureString --overwrite --key-id $EKS_SSM_KEY_ID
      fi
      shift 2
      from=$1
      to=$2
    done
}

base_path="${EKS_SSM_PREFIX}/pipeline"
shared_path="${base_path}/${shared}"
ccw_path="${base_path}/ccw"
rda_path="${base_path}/rda"
base_config_path="{shared_path}/${EKS_SSM_CONFIG_ROOT}"
shared_config_path="{base_config_path}/common"
ccw_config_path="{base_config_path}/ccw"
rda_config_path="{base_config_path}/rda"

copy_params \
  "${ccw_path}/nonsensitive/data_pipeline_filtering_non_null_and_non_2023_benes" "${shared_config_path}/FILTERING_NON_NULL_AND_NON_2023_BENES" \
  "${ccw_path}/nonsensitive/data_pipeline_idempotency_required" "${shared_config_path}/IDEMPOTENCY_REQUIRED" \
  "${ccw_path}/nonsensitive/rif_job_batch_size" "${shared_config_path}/RIF_JOB_BATCH_SIZE" \
  "${ccw_path}/nonsensitive/rif_job_batch_size_claims" "${shared_config_path}/RIF_JOB_BATCH_SIZE_CLAIMS" \
  "${ccw_path}/nonsensitive/rif_job_queue_size_multiple" "${shared_config_path}/RIF_JOB_QUEUE_SIZE_MULTIPLE" \
  "${ccw_path}/nonsensitive/rif_job_queue_size_multiple_claims" "${shared_config_path}/RIF_JOB_QUEUE_SIZE_MULTIPLE_CLAIMS" \
  "${ccw_path}/nonsensitive/data_pipeline_micrometer_cw_interval" "${shared_config_path}/MICROMETER_CW_INTERVAL" \
  "${ccw_path}/nonsensitive/data_pipeline_micrometer_cw_namespace" "${shared_config_path}/MICROMETER_CW_NAMESPACE" \
  "${ccw_path}/nonsensitive/data_pipeline_micrometer_cw_enabled" "${shared_config_path}/MICROMETER_CW_ENABLED" \


copy_secure_params \
  "${shared_path}/sensitive/data_pipeline_hicn_hash_iterations" "${shared_config_path}/HICN_HASH_ITERATIONS" \
  "${shared_path}/sensitive/data_pipeline_hicn_hash_pepper" "${shared_config_path}/HICN_HASH_PEPPER" \
  "${shared_path}/sensitive/db_migrator_db_username" "${shared_config_path}/DATABASE_USERNAME" \
  "${shared_path}/sensitive/db_migrator_db_password" "${shared_config_path}/DATABASE_PASSWORD" \


set_const_secure_params \
  "jdbc:postgresql://${EKS_RDS_WRITER_ENDPOINT}:5432/fhirdb" "${shared_config_path}/DATABASE_URL" \


set_const_params \
  "100" "${shared_config_path}/HICN_HASH_CACHE_SIZE" \
  "180" "${shared_config_path}/RDA_JOB_ERROR_EXPIRE_DAYS" \
  "false" "${shared_config_path}/CCW_RIF_JOB_ENABLED" \
  "4" "${shared_config_path}/LOADER_THREADS" \
  "8" "${shared_config_path}/LOADER_THREADS_CLAIMS" \
  "false" "${shared_config_path}/RDA_JOB_ENABLED" \
  "3600" "${shared_config_path}/RDA_JOB_INTERVAL_SECONDS" \
  "20" "${shared_config_path}/RDA_JOB_BATCH_SIZE" \
  "5" "${shared_config_path}/RDA_JOB_WRITE_THREADS" \
  "true" "${shared_config_path}/RDA_JOB_PROCESS_DLQ" \
  "false" "${shared_config_path}/MICROMETER_CW_ENABLED" \

case $mode in
  rif)
    # RIF Mode
    set_const_params \
      "true" "${ccw_config_path}/CCW_RIF_JOB_ENABLED" \

    set_const_secure_params \
      "${EKS_S3_BUCKET_NAME}" "${rif_config_path}/S3_BUCKET_NAME" \

    ;;
  random)
    # Still an rda pipeline, just ussing random data.
    mode=rda

    # Random RDA Server Mode
    set_const_params \
      "InProcess" "${shared_config_path}/RDA_GRPC_SERVER_TYPE" \
      "Random" "${shared_config_path}/RDA_GRPC_INPROC_SERVER_MODE" \
      "42" "${shared_config_path}/RDA_GRPC_INPROC_SERVER_RANDOM_SEED" \
      "2500" "${shared_config_path}/RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS" \
      "true" "${rda_config_path}/RDA_JOB_ENABLED" \

    ;;
  rda)
    # RDA API Call mode
    set_const_params \
      "Remote" "${shared_config_path}/RDA_GRPC_SERVER_TYPE" \
      "600" "${shared_config_path}/RDA_GRPC_MAX_IDLE_SECONDS" \
      "true" "${rda_config_path}/RDA_JOB_ENABLED" \

    set_const_secure_params \
      "${EKS_RDA_GRPC_HOST}" "${rda_config_path}/RDA_GRPC_HOST" \
      "${EKS_RDA_GRPC_PORT}" "${rda_config_path}/RDA_GRPC_PORT" \
      "${EKS_RDA_GRPC_AUTH_TOKEN}" "${rda_config_path}/RDA_GRPC_AUTH_TOKEN" \

    ;;
  *)
    echo Invalid value for run_mode: $run_mode 1>&2
    exit 1
esac

namespace=eks-test
chart=../helm/pipeline
mode_config_path="{base_config_path}/${mode}"

helm -n $namespace uninstall "pipeline-${mode}" || true
helm -n $namespace install "pipeline-${mode}" $chart \
  --set ssmHierarchies="{${shared_config_path},${mode_config_path}}" \
  --set imageRegistry="${EKS_ECR_REGISTRY}/" \
  --values pipeline-values.yaml \
  --values "pipeline-values-${mode}.yaml"
