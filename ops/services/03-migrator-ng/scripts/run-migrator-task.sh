#!/usr/bin/env bash

set -Eeou pipefail

#######################################
# DB Migrator ECS Run Task Script
#######################################
# This script runs the BFD Migrator during terraform apply's of the migrator service if there is a
# change to the migrator Task Definition (thus, the possibility of migrations to run).
#
# Intended to be ran in a "local_exec" provisioner of a null_resource resource.
#
# Environment Variables:
#   $ROLE_NAME: Name of the migrator IAM Role used in the Task Definition
#   $TASK_NAME: Name of the migrator Task, also the group name; typically just "migrator"
#   $CONTAINER_NAME: Name of the migrator container; typically just "migrator"
#   $CLUSTER_NAME: Name of the ECS Cluster to run in
#   $TASK_DEFINITION_ARN: ARN of the Task Definition revision to run a Task based on
#   $TASK_DEFINITION_FAMILY: Name of the Task Definition family for the Task
#   $NETWORK_CONFIG_JSON: JSON object of awsvpc network configuration (subnets, security group, etc.)
#   $LOG_GROUP_NAME: Name of the CloudWatch Log Group for Migrator standard output. Used only for
#                    assisting operators in observing the Migrator's behavior
#######################################

until aws iam get-role --role-name "$ROLE_NAME" &>/dev/null; do
  sleep 1
done

set +e
task_id="-1"
task_start_retries=3
while ((task_start_retries > 0)); do
  for migrator_task_arn in $(
    aws ecs list-tasks \
      --cluster "$CLUSTER_NAME" \
      --family "$TASK_DEFINITION_FAMILY" \
      --query "taskArns" \
      --output text
  ); do
    current_task_state="$(
      aws ecs describe-tasks \
        --cluster "$CLUSTER_NAME" \
        --tasks "$migrator_task_arn" \
        --query "tasks[0].lastStatus" \
        --output text || echo "n/a"
    )"
    if [[ $current_task_state == "PROVISIONING" ||
      $current_task_state == "PENDING" ||
      $current_task_state == "ACTIVATING" ||
      $current_task_state == "RUNNING" ]]; then
      echo "Task $migrator_task_arn is currently running"
      exit 1
    fi
  done

  task_id="$(
    aws ecs run-task \
      --group "$TASK_NAME" \
      --cluster "$CLUSTER_NAME" \
      --task-definition "$TASK_DEFINITION_ARN" \
      --enable-ecs-managed-tags \
      --propagate-tags TASK_DEFINITION \
      --count 1 \
      --platform-version "LATEST" \
      --capacity-provider-strategy "$CAPACITY_PROVIDER_STRATEGIES" \
      --network-configuration "$NETWORK_CONFIG_JSON" \
      --query "tasks[0].taskArn" \
      --output text || echo "-1"
  )"
  if [[ $task_id != "-1" ]]; then
    break
  fi
  ((task_start_retries--))
  sleep 5
done
set -e

if [[ $task_id == "-1" ]] || ((task_start_retries == 0)); then
  echo "Failed to start Task"
  exit 1
fi

echo "Started $TASK_NAME ($task_id) in $CLUSTER_NAME. Waiting until it has completed or failed..."

until [[ 
  $(aws ecs describe-tasks \
    --cluster "$CLUSTER_NAME" \
    --tasks "$task_id" \
    --query "tasks[0].lastStatus" \
    --output text) == "STOPPED" ]] \
  ; do
  echo "$TASK_NAME has not yet completed. Waiting 5 seconds before checking again..."
  sleep 5
done

task_exit_code=$(aws ecs describe-tasks \
  --cluster "$CLUSTER_NAME" \
  --tasks "$task_id" \
  --query "tasks[0].containers[?name=='$CONTAINER_NAME'].exitCode" \
  --output text)

if [[ $task_exit_code != "0" ]]; then
  echo "$TASK_NAME failed. Check CloudWatch Logs (aws logs tail '$LOG_GROUP_NAME') for more information"
  exit 1
fi

echo "$TASK_NAME ran successfully. Check CloudWatch Logs (aws logs tail '$LOG_GROUP_NAME') for more information"
