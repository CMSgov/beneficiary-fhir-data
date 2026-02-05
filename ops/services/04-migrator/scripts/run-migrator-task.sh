#!/usr/bin/env bash

#######################################
# DB Migrator ECS Run Task Script
#######################################
# This script runs the BFD Migrator during terraform apply's of the migrator service if there is a
# change to the migrator Task Definition (thus, the possibility of migrations to run).
#
# Intended to be ran in a "local_exec" provisioner of a null_resource resource.
#
# Environment Variables:
#   $TASK_NAME: Name of the migrator Task, also the group name; typically just "migrator"
#   $CONTAINER_NAME: Name of the migrator container; typically just "migrator"
#   $CLUSTER_NAME: Name of the ECS Cluster to run in
#   $TASK_DEFINITION_ARN: ARN of the Task Definition revision to run a Task based on
#   $NETWORK_CONFIG_JSON: JSON object of awsvpc network configuration (subnets, security group, etc.)
#   $TASK_TAGS_JSON: JSON list of { "key": "<key>", "value": "<value>" } tag objects
#   $LOG_GROUP_NAME: Name of the CloudWatch Log Group for Migrator standard output. Used only for
#                    assisting operators in observing the Migrator's behavior
#######################################

task_id="$(
  aws ecs run-task \
    --group "$TASK_NAME" \
    --cluster "$CLUSTER_NAME" \
    --task-definition "$TASK_DEFINITION_ARN" \
    --count 1 \
    --platform-version "LATEST" \
    --capacity-provider-strategy "$CAPACITY_PROVIDER_STRATEGIES" \
    --network-configuration "$NETWORK_CONFIG_JSON" \
    --tags "$TASK_TAGS_JSON" \
    --query "tasks[0].taskArn" \
    --output text
)"

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
