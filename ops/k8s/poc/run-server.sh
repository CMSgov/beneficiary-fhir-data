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

cd `dirname $0`

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
      value=`aws ssm get-parameter --name $from --output text --query Parameter.Value`
      echo setting $to
      aws ssm put-parameter --name $to --value "$value" --type String --overwrite
      shift 2
      from=$1
      to=$2
    done
}

function copy_secure_params() {
    from=$1
    to=$2
    while [[ x$1 != x ]] ; do
      value=`aws ssm get-parameter --name $from --with-decryption --output text --query Parameter.Value`
      echo setting $to
      aws ssm put-parameter --name $to --value "$value" --type SecureString --overwrite --key-id $EKS_SSM_KEY_ID
      shift 2
      from=$1
      to=$2
    done
}

base_path="${EKS_SSM_PREFIX}/server"
base_config_path="{base_path}/${EKS_SSM_CONFIG_ROOT}"

copy_params \
  "${base_path}/nonsensitive/data_server_new_relic_metric_path" "${base_config_path}/NEW_RELIC_METRIC_PATH" \
  "${base_path}/nonsensitive/data_server_new_relic_metric_host" "${base_config_path}/NEW_RELIC_METRIC_HOST" \
  "${base_path}/nonsensitive/pac_claim_source_types" "${base_config_path}/bfdServer.pac.claimSourceTypes" \
  "${base_path}/nonsensitive/pac_resources_enabled" "${base_config_path}/bfdServer.pac.enabled" \

copy_secure_params \
  "${base_path}/sensitive/data_server_db_username" "${base_config_path}/bfdServer.db.username" \
  "${base_path}/sensitive/data_server_db_password" "${base_config_path}/bfdServer.db.password" \
  "${base_path}/sensitive/data_server_appserver_https_port" "${base_config_path}/BFD_PORT" \
  "${base_path}/sensitive/data_server_new_relic_metric_key" "${base_config_path}/NEW_RELIC_METRIC_KEY" \

set_const_secure_params \
  "jdbc:postgresql://${EKS_RDS_WRITER_ENDPOINT}:5432/fhirdb?logServerErrorDetail=false" "${base_config_path}/bfdServer.db.url" \

set_const_params \
  "true" "${base_config_path}/bfdServer.v2.enabled" \
  "10" "${base_config_path}/bfdServer.db.connections.max" \

# Our docker image for bfd-server currently does not have support for New Relic agent jar.
# See ops/ansible/roles/bfd-server/templates/bfd-server.sh.j2
#   -javaagent:{{ data_server_dir }}/newrelic/newrelic.jar
#
# It is an open question whether we'll need the agent at all.  Micrometer supports JVM
# metric collection without an agent: https://micrometer.io/docs/ref/jvm

# Deliberately not set:
#   Only used by agent (see comment about agent):
#     NEW_RELIC_ENVIRONMENT
#     NEW_RELIC_LICENSE_KEY  ${base_path}/sensitive/data_server_new_relic_license_key
#     NEW_RELIC_PROXY_HOST
#     NEW_RELIC_PROXY_PORT
#     NEW_RELIC_HIGH_SECURITY
#     NEW_RELIC_EXTENSIONS_DIR
#     NEW_RELIC_LOG_FILE_PATH
#   Set in helm chart:
#     NEW_RELIC_APP_NAME
#   Not available in IaC:
#     NEW_RELIC_METRIC_PERIOD

# Currently or soon to be obsolete:
#   bfdServer.pac.enabled
#   bfdServer.pac.claimSourceTypes
#   bfdServer.v2.enabled

# Probably better handled outside of the server settings:
#   BFD_PORT: Pods could use fixed port. Load balancer would expose the actual port.

# For purposes of this poc we are going to use the local development key and trust store files.
# Otherwise we would need to access:
#
#  ${base_path}/sensitive/server_keystore_base64
#  ${base_path}/sensitive/test_client_cert
#  ${base_path}/sensitive/test_client_key
#
# Still an open question whether trust store should be constructed dynamically from certs in SSM.

# Values set in the helm chart:
#   bfdServer.logs.dir
#   java.io.tmpdir
#   org.jboss.logging.provider

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
      "InProcess" "${base_config_path}/RDA_GRPC_SERVER_TYPE" \
      "Random" "${base_config_path}/RDA_GRPC_INPROC_SERVER_MODE" \
      "42" "${base_config_path}/RDA_GRPC_INPROC_SERVER_RANDOM_SEED" \
      "2500" "${base_config_path}/RDA_GRPC_INPROC_SERVER_RANDOM_MAX_CLAIMS" \
      "true" "${rda_config_path}/RDA_JOB_ENABLED" \

    ;;
  rda)
    # RDA API Call mode
    set_const_params \
      "Remote" "${base_config_path}/RDA_GRPC_SERVER_TYPE" \
      "600" "${base_config_path}/RDA_GRPC_MAX_IDLE_SECONDS" \
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
  --set ssmHierarchies="{${base_config_path},${mode_config_path}}" \
  --set imageRegistry="${EKS_ECR_REGISTRY}/" \
  --values pipeline-values.yaml
