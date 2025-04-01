#!/usr/bin/env bash
#######################################
# RDS Writer Identifier Script
#######################################
# This script identifies the writer node in an RDS Cluster, and returns it as part of a JSON object
# with a single property, "writer".
#
# This exists to accommodate the desire for placing write-intensive workloads in the same AZ as the
# writer node. As of April 2025, the data source for DB instances does not expose the `IsClusterWriter`
# field from the RDS API's DescribeDBInstances response that's leveraged below.
# See the data source attributes reference for more details:
# https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/db_instance#attributes-reference
#
# Arguments:
#   $1 maps to the CLUSTER_IDENTIFIER
#######################################
set -euo pipefail

CLUSTER_IDENTIFIER="$1"

aws rds describe-db-clusters --db-cluster-identifier "$CLUSTER_IDENTIFIER" |
  jq '.DBClusters[].DBClusterMembers | map(select(.IsClusterWriter) | .DBInstanceIdentifier) | { writer: .[0] }'
