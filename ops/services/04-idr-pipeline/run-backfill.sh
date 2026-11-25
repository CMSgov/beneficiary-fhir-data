#!/usr/bin/env bash
set -Eeuo pipefail

read -p "Are you sure you want to run the backfill pipeline? [yn] " -n 1 -r
echo
if ! [[ $REPLY =~ ^[Yy]$ ]]; then
	echo 'exiting'
	exit 0
fi

infile="$(mktemp)"
outfile="$(mktemp)"
job_id="2"
cleanup() {
	rm -f "$infile" "$outfile"
	exit
}

trap 'cleanup' SIGTERM
trap 'cleanup' SIGINT

cat >"$infile" <<EOL
{
  "cpu": 16384,
  "memory": 114688,
  "env": {
    "IDR_ENABLE_NPI_TYPE_BACKFILL": "1",
    "IDR_ENABLE_DATE_PARTITIONS": "1",
    "IDR_PARTITION_TYPE": "day",
    "IDR_LATEST_CLAIMS": "1",
    "IDR_LOAD_MODE": "prod",
    "IDR_LOAD_TYPE": "initial",
    "IDR_JOB_ID": "$job_id",
    "IDR_MAX_TASKS_PER_CHILD": "1",
	"IDR_PER_BATCH_MIN_CONNECTIONS": "10",
    "IDR_PER_BATCH_MAX_CONNECTIONS": "10"
  }
}
EOL

cluster=bfd-prod-cluster
for task in $(aws ecs list-tasks --cluster "$cluster" --family bfd-prod-idr-pipeline --query 'taskArns' --output text); do
	found_id="$(aws ecs describe-tasks --cluster "$cluster" --tasks "$task" --query 'tasks[0].overrides.containerOverrides[].environment[]' |
		jq -r '.[] | select(.name == "IDR_JOB_ID").value')"

	if [[ "$found_id" == "$job_id" ]]; then
		echo "found existing task. stopping"
		aws ecs stop-task --cluster "$cluster" --task "$task"
	fi
done

aws lambda invoke --function-name "bfd-prod-idr-pipeline-run-idr-pipeline" --cli-binary-format raw-in-base64-out --payload "file://$infile" "$outfile"
cat "$outfile"
cleanup
