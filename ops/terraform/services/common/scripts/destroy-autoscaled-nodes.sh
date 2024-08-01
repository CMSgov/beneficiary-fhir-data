#!/bin/bash
# Terraform helper script called as a local-exec provisioner at destroy-time of the Aurora cluster
# resource. This script will enumerate the autoscaled reader nodes within the given Aurora cluster
# (provided by environment variable $DB_CLUSTER_ID), mark them all for deletion, and then wait
# indefinitely for all nodes to be successfully deleted. This script is intended to run ONLY in
# ephemeral environments and will immediately fail if it is run in an established environment.
# Environment variables:
#   DB_CLUSTER_ID: The cluster identifier of the ephemeral database cluster that is being destroyed
# . BFD_ENVIRONMENT: The name of the environment that this script is being run against

set -euo pipefail

# Ensure that the BFD_ENVIRONMENT environment var is specified and not empty
trimmed_bfd_env="$(echo "$BFD_ENVIRONMENT" | tr -d '[:space:]')"
if [[ -z $trimmed_bfd_env ]]; then
  echo "BFD_ENVIRONMENT must not be an empty string or whitespace"
  exit 1
fi

# Ensure that whatever environment this script is running in is NOT an established environment. We
# never want to destroy nodes in our established environments
if [[ $trimmed_bfd_env == "prod" || $trimmed_bfd_env == "prod-sbx" || $trimmed_bfd_env == "test" ]]; then
  echo "Cannot destroy nodes in established environment $BFD_ENVIRONMENT"
  exit 1
fi

# Be _extra_ careful about the cluster ID given by the $DB_CLUSTER_ID environment variable.
# Specifically, remove all whitespace (which is invalid for a cluster ID) and ensure that the
# resulting, trimmed ID is non-empty before even making any deletion attempts as the AWS CLI will
# treat an empty cluster ID as equivalent to "give me all clusters" when calling
# describe-db-clusters
trimmed_cluster_id="$(echo "$DB_CLUSTER_ID" | tr -d '[:space:]')"
if [[ -z $trimmed_cluster_id ]]; then
  echo "DB_CLUSTER_ID must not be an empty string or whitespace"
  exit 1
fi

# jq expression returns the first element in the returned array as a precaution against a scenario
# where multiple clusters are erroneously returned. Otherwise, the nodes in all clusters would be
# marked for deletion. Additionally, we select for only the nodes with names containing
# "autoscaling" to avoid possibly deleting the writer node which is managed by Terraform directly
nodes_in_cluster="$(
  aws rds describe-db-clusters --db-cluster-identifier "$trimmed_cluster_id" |
    jq -r '.DBClusters[0].DBClusterMembers[].DBInstanceIdentifier | select(. | contains("autoscaling"))'
)"

# If there are no autoscaling nodes to delete, immediately exit
if [[ -z $nodes_in_cluster ]]; then
  echo "No nodes in $trimmed_cluster_id to delete"
  exit
fi

for node in $nodes_in_cluster; do
  # stderr is redirected to /dev/null to ensure that if there are any errors this variable is empty
  # and no operation is done
  instance_details_json="$(
    aws rds describe-db-instances --db-instance-identifier "$node" 2>/dev/null || echo ""
  )"

  # If the instance is empty, then don't do anything
  if [[ -z $instance_details_json ]]; then
    continue
  fi

  instance_status="$(
    echo "$instance_details_json" |
      jq --arg trimmed_cluster_id "$trimmed_cluster_id" \
        -r '.DBInstances[0] | select(.DBClusterIdentifier == $trimmed_cluster_id) | .DBInstanceStatus'
  )"
  if [[ $instance_status != "deleting" ]]; then
    echo "Marking $node for deletion"
    # Redirect stdout to /dev/null as the JSON response is unnecessary
    aws rds delete-db-instance --db-instance-identifier "$node" > /dev/null
  fi
done

echo "All autoscaling nodes in $trimmed_cluster_id marked for deletion"

while true; do
  echo "Verifying if all autoscaling nodes in $trimmed_cluster_id have been deleted..."

  deleting_nodes=0
  for node in $nodes_in_cluster; do
    instance_details_json="$(
      aws rds describe-db-instances --db-instance-identifier "$node" 2>/dev/null || echo ""
    )"

    # If the instance is empty, then don't do anything
    if [[ -z $instance_details_json ]]; then
      continue
    fi

    instance_status="$(
      echo "$instance_details_json" |
        jq --arg trimmed_cluster_id "$trimmed_cluster_id" \
          -r '.DBInstances[0] | select(.DBClusterIdentifier == $trimmed_cluster_id) | .DBInstanceStatus'
    )"
    if [[ $instance_status == "deleting" ]]; then
      echo "Deletion of $node is still in progress"
      ((deleting_nodes += 1))
    fi
  done

  if ((deleting_nodes == 0)); then
    echo "All autoscaling nodes in $trimmed_cluster_id have been deleted"
    exit
  fi

  echo "$deleting_nodes autoscaling node(s) have not yet been deleted. Sleeping for 5 seconds before retrying"
  sleep 5
done
