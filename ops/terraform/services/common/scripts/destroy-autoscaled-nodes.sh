#!/bin/bash

set -x

nodes_in_cluster=$(
  aws rds describe-db-clusters --db-cluster-identifier "$DB_CLUSTER_ID" |
    jq -r '.DBClusters[].DBClusterMembers[].DBInstanceIdentifier'
)

for node in $nodes_in_cluster; do
  instance_status="$(aws rds describe-db-instances --db-instance-identifier "$node" | jq -r '.DBInstances[].DBInstanceStatus')"
  if [[ -n $instance_status && $instance_status != "deleting" ]]; then
    aws rds delete-db-instance --db-instance-identifier "$node" > /dev/null
  fi
done

while true; do
  deleting_nodes=0
  for node in $nodes_in_cluster; do
    instance_status="$(aws rds describe-db-instances --db-instance-identifier "$node" | jq -r '.DBInstances[].DBInstanceStatus')"
    if [[ $instance_status == "deleting" ]]; then
      ((deleting_nodes += 1))
    fi
  done

  if (( deleting_nodes == 0)); then
    exit
  fi
  sleep 5
done
