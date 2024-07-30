#!/usr/bin/env bash
# Sets up SSM parameter store hierarchy containing settings and then installs
# the pipeline's helm chart into the EKS POC cluster.

set -e

app_name=`basename $0`

data_dir=$1
if [[ x$data_dir = x ]] ; then
  echo "error: usage ${app_name} working-directory-path" 2>&1
  exit 1
fi

if [[ ! -d $data_dir ]] ; then
  echo "error: not a directory: $data_dir" 2>&1
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

base_path="${EKS_SSM_PREFIX}/server"
base_config_path="${base_path}/${EKS_SSM_CONFIG_ROOT}"

copy_params \
  "${base_path}" "${base_config_path}" /nonsensitive/new_relic/metrics/path \
  "${base_path}" "${base_config_path}" /nonsensitive/new_relic/metrics/host \
  "${base_path}" "${base_config_path}" /nonsensitive/pac/claim_source_types \
  "${base_path}" "${base_config_path}" /nonsensitive/pac/enabled \

copy_secure_params \
  "${base_path}" "${base_config_path}" /sensitive/db/username \
  "${base_path}" "${base_config_path}" /sensitive/db/password \
  "${base_path}" "${base_config_path}" /sensitive/port \
  "${base_path}" "${base_config_path}" /sensitive/new_relic/metrics/license_key \

set_const_secure_params \
  "jdbc:postgresql://${EKS_RDS_WRITER_ENDPOINT}:5432/fhirdb?logServerErrorDetail=false" "${base_config_path}/sensitive/db/url" \

set_const_params \
  "10" "${base_config_path}/nonsensitive/db/hikari/max_pool_size" \
  "true" "${base_config_path}/nonsensitive/pac/enabled" \
  "fiss,mcs" "${base_config_path}/nonsensitive/pac/claim_source_types" \

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

truststore_name=truststore.pfx
truststore_file="${data_dir}/${truststore_name}"
[[ -f $truststore_file ]] && rm $truststore_file
keytool -genkeypair -alias fake -dname cn=fake -storetype PKCS12 -keyalg RSA -keypass changeit -keystore $truststore_file -storepass changeit
for cert_path in `aws ssm get-parameters-by-path --path "${base_path}/nonsensitive/client_certificates" --output text --query 'Parameters[*].Name'` ; do
  cert_name=`basename $cert_path`
  cert_file="${data_dir}/${cert_name}.cert"
  echo downloading $cert_name to $cert_file
  aws ssm get-parameter --name $cert_path --output text --query Parameter.Value > $cert_file
  echo importing $cert_file
  keytool -importcert -file $cert_file -alias $cert_name -keypass changeit -keystore $truststore_file -storepass changeit -noprompt
  rm $cert_file
done
keytool -delete -alias fake -keystore $truststore_file -storepass changeit -keypass changeit

keystore_name=keystore.pfx
keystore_file="${data_dir}/${keystore_name}"
aws ssm get-parameter --name "${base_path}/sensitive/server_keystore_base64" --with-decryption --output text --query Parameter.Value \
  | base64 -d \
  > $keystore_file

ssl_volume_path=/app/ssl
set_const_secure_params \
  "${ssl_volume_path}/${keystore_name}" "${base_config_path}/sensitive/paths/files/keystore" \
  "${ssl_volume_path}/${truststore_name}" "${base_config_path}/sensitive/paths/files/truststore" \

namespace=eks-test
chart=../helm/server

kubectl -n $namespace delete secret bfd-server-ssl-files || true
kubectl -n $namespace create secret generic bfd-server-ssl-files \
    --from-file ${truststore_file} \
    --from-file ${keystore_file}
rm ${truststore_file} ${keystore_file}

helm -n $namespace uninstall "server" || true
helm -n $namespace install "server" $chart \
  --set ssmHierarchies="{${base_config_path}/sensitive,${base_config_path}/nonsensitive}" \
  --set imageRegistry="${EKS_ECR_REGISTRY}/" \
  --values server-values.yaml
