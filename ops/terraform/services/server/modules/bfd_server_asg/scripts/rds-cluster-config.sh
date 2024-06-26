#!/usr/bin/env bash
#######################################
# Results in two calls to the AWS RDS APIs for describe-db-clusters and describe-db-instances.
# Returns a well-formatted json object including the following keys:
# "DBClusterIdentifier", "Endpoint", "ReaderEndpoint", "WriterAZ", and "WriterNode".
#
# This exists to accommodate the desire for placing write-intensive workloads in the same AZ as the
# writer node. As of May 2022, the data source for DB instances does not expose the `IsClusterWriter`
# field from the RDS API's DescribeDBInstances response that's leveraged below.
# See the data source attributes reference for more details:
# https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/db_instance#attributes-reference
#
# Globals:
#   CLUSTER_IDENTIFIER mapped to the "$1" positional argument
#   CLUSTER a modified json object from the aws rds describe-db-clusters command
#   WRITER_NODE a string representing the writer node's db-instance-identifier
#   WRITER_AZ a string representing the writer node's availability zone
#   WRITER_CONFIG a json object (as a string) including writer-specific keys
#
# Arguments:
#   $1 maps to the CLUSTER_IDENTIFIER
#######################################
set -euo pipefail

CLUSTER_IDENTIFIER="$1"

CLUSTER="$(aws rds describe-db-clusters \
  --query 'DBClusters[].{DBClusterIdentifier:DBClusterIdentifier,Endpoint:Endpoint,ReaderEndpoint:ReaderEndpoint,Members:DBClusterMembers}[0]' \
  --db-cluster-identifier "$CLUSTER_IDENTIFIER")"

WRITER_NODE="$(jq -r '.Members[] | select(.IsClusterWriter == true) | .DBInstanceIdentifier' <<<"$CLUSTER")"

WRITER_AZ="$(aws rds describe-db-instances --db-instance-identifier "$WRITER_NODE" --query 'DBInstances[0].AvailabilityZone' --output text)"

WRITER_CONFIG="$(jq --null-input --arg writer_node "$WRITER_NODE" --arg writer_az "$WRITER_AZ" '{ WriterNode: $writer_node, WriterAZ: $writer_az }')"

MERGED_READER_WRITER_CONFIG="$(jq --argjson obj "$WRITER_CONFIG" '. += $obj | del(.Members)' <<<"$CLUSTER")"

# TODO: Don't hardcode the proxy name; rewrite script to remove unnecessary fields for BFD Server
PROXY_READER_ENDPOINT="$(aws rds describe-db-proxy-endpoints --db-proxy-name bfd-3495-prod | jq -r '.DBProxyEndpoints[] | select(.TargetRole | contains("READ_ONLY")) | .Endpoint')"

jq --arg proxy_reader "$PROXY_READER_ENDPOINT" '. += { ProxyReaderEndpoint: $proxy_reader }' <<<"$MERGED_READER_WRITER_CONFIG"
