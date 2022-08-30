#!/usr/bin/env bash
set -eou pipefail

BFD_ENV=${BFD_ENV:="test"}
BFD_TEST_HOST=${BFD_TEST_HOST:="https://test.bfd.cms.gov"}

cluster_id="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/common/nonsensitive/rds_cluster_identifier --query Parameter.Value --region us-east-1 --output text)"
username="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/server/sensitive/vault_data_server_db_username --with-decryption --query Parameter.Value --region us-east-1 --output text)"
raw_password="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/server/sensitive/vault_data_server_db_password --with-decryption --query Parameter.Value --region us-east-1 --output text)"
password="$( python3 -c 'import sys;import urllib.parse;print(urllib.parse.quote(sys.argv[1]))' "$raw_password")"
cert_key="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/server/sensitive/test_client_key --with-decryption --query Parameter.Value --region us-east-1 --output text)"
cert="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/server/sensitive/test_client_cert --with-decryption --query Parameter.Value --region us-east-1 --output text)"
db_dsn="postgres://${username}:${password}@bfd-test-aurora-cluster.cluster-ro-clyryngdhnko.us-east-1.rds.amazonaws.com:5432/fhirdb"


cat <<EOF > "/tmp/${BFD_ENV}-cert.pem"
$cert_key
$cert
EOF

locust \
	--locustfile=high_volume_suite.py \
	--host="${BFD_TEST_HOST}" \
	--users=1 \
	--master \
	--master-bind-port=5557 \
	--client-cert-path="/tmp/${BFD_ENV}-cert.pem" \
	--database-uri="$db_dsn"\
	--enable-rebalancing \
	--spawn-rate=5 \
	--headless \
	--only-summary
