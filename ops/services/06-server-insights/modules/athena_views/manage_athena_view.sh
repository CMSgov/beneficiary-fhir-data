#!/bin/bash

set -eou pipefail

region="${REGION}"
readonly region

database_name="${DATABASE_NAME}"
readonly database_name

view_name="${VIEW_NAME}"
readonly view_name

view_sql="${VIEW_SQL:-none}" # If no sql is specified, set the value of view_sql to "none"
readonly view_sql

operation_type="${OPERATION_TYPE}"
readonly operation_type

if [[ $operation_type == "CREATE_VIEW" ]]; then
  if [[ $view_sql == "none" ]]; then
    echo "VIEW_SQL was unspecified when creating $view_name" >&2
    exit 1
  fi

  exec_id=$(
    aws athena start-query-execution \
      --region "$region" \
      --query-string "$view_sql" \
      --query-execution-context "Database=$database_name" \
      --work-group "bfd" \
      --query "QueryExecutionId" \
      --output text
  )
elif [[ $operation_type == "DESTROY_VIEW" ]]; then
  exec_id=$(
    aws athena start-query-execution \
      --region "$region" \
      --query-string "DROP VIEW IF EXISTS $view_name" \
      --query-execution-context "Database=$database_name" \
      --work-group "bfd" \
      --query "QueryExecutionId" \
      --output text
  )
else
  echo "$operation_type is unsupported, exitting" >&2
  exit 1
fi

while true; do
  status=$(
    aws athena get-query-execution \
      --region "$region" \
      --query-execution-id "$exec_id" \
      --query "QueryExecution.Status.State" \
      --output text
  )

  if [[ $status == "FAILED" ]]; then
    # We need to double-check failures due to a quirk of Athena where it "automatically retries
    # your queries in cases of certain transient errors". We wait 10 seconds to give Athena
    # enough time to try, and only if the status is still FAILED do we exit the loop
    sleep 10

    status=$(
      aws athena get-query-execution \
        --region "$region" \
        --query-execution-id "$exec_id" \
        --query "QueryExecution.Status.State" \
        --output text
    )

    # If the status is not failed, that means that Athena retried automatically due to a transient
    # error and so we can continue the loop. But, if the status is _still_ FAILED that means it is
    # likely that the error is not automatically retriable and we should bubble it up back to
    # Terraform
    if [[ $status == "FAILED" ]]; then
      break
    fi
  elif [[ $status != "RUNNING" && $status != "QUEUED" ]]; then
    break
  fi
done

if [[ $status == "SUCCEEDED" ]]; then
  if [[ $operation_type == "CREATE_VIEW" ]]; then
    echo "$view_name created successfully in $database_name"
  else
    echo "$view_name deleted successfully from $database_name"
  fi
else
  status_reason=$(
    aws athena get-query-execution \
      --region "$region" \
      --query-execution-id "$exec_id" \
      --query "QueryExecution.Status.StateChangeReason" \
      --output text
  )
  echo "Operation $operation_type on $view_name for database $database_name failed due to: $status_reason" >&2
  exit 1
fi
