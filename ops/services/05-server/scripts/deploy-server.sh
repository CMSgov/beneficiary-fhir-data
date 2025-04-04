#!/bin/bash
#######################################
# CodeDeploy ECS Server Script
#######################################
# Not intended for manual usage. Strictly used as part of a `null_resource` `local_exec` provisioner
#
# Deploys the Server ECS Service and waits for the deployment to complete or fail, outputting the
# status of the deployment at 5 second intervals.
#
# Requires jq and awscli
#
# Environment:
#   SERVICE_NAME: Name of the ECS Service; likely "server"
#   CLUSTER_NAME: Name of the ECS Cluster
#   APPSPEC_YAML: The templated YAML of the AppSpec revision to deploy
#######################################

deploy_id="$(aws deploy create-deployment \
  --cli-input-yaml "$APPSPEC_YAML" \
  --query "[deploymentId]" \
  --output text)"

echo "Deployment $deploy_id started."
while true; do
  deployment_status="$(
    aws deploy get-deployment \
      --deployment-id "$deploy_id" \
      --query "[deploymentInfo.status]" \
      --output text
  )"
  deployment_lifecycles="$(
    aws deploy get-deployment-target \
      --deployment-id "$deploy_id" \
      --target-id "$CLUSTER_NAME:$SERVICE_NAME" |
      jq -r '.deploymentTarget.ecsTarget.lifecycleEvents'
  )"
  successful_lifecycles="$(
    jq -r '
      map(select(.status == "Succeeded") |
        .lifecycleEventName) |
        join(", ")
    ' <<<"$deployment_lifecycles"
  )"
  in_progress_lifecycles="$(
    jq -r '
      map(select(.status == "InProgress") |
        .lifecycleEventName) |
        join(", ")
    ' <<<"$deployment_lifecycles"
  )"
  pending_lifecycles="$(
    jq -r '
      map(select(.status == "Pending") |
        .lifecycleEventName) |
        join(", ")
    ' <<<"$deployment_lifecycles"
  )"

  echo "$deploy_id status: $deployment_status"
  echo "$deploy_id successful lifecycles: $successful_lifecycles"
  echo "$deploy_id in progress lifecycles: $in_progress_lifecycles"
  echo "$deploy_id pending lifecycles: $pending_lifecycles"
  if [[ $deployment_status == "Succeeded" ]]; then
    echo
    echo "Deployment $deploy_id succeeded."
    break
  elif [[ $deployment_status == "Failed" || $deployment_status == "Stopped" ]]; then
    echo
    echo "Deployment $deploy_id failed!"
    exit 1
  fi

  echo "Sleeping for 5 seconds..."
  sleep 5
done
