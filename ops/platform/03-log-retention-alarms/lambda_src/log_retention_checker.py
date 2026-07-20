import json
import logging
import os
from typing import TYPE_CHECKING, Protocol, cast

import boto3  # type: ignore[import-not-found]
from botocore.config import Config# type: ignore[import-not-found]

if TYPE_CHECKING:
	class CloudWatchLogsClient(Protocol):
		def get_paginator(self, operation_name: str): ...

		def put_retention_policy(self, *, logGroupName: str, retentionInDays: int) -> dict: ...
else:
    CloudWatchLogsClient = object


logger = Logger()

REQUIRED_RETENTION_DAYS = int(os.getenv("REQUIRED_RETENTION_DAYS", "2557"))
ALERT_SNS_TOPIC_ARN = os.getenv("ALERT_SNS_TOPIC_ARN")
REGION = os.environ.get("AWS_CURRENT_REGION", default="us-east-1")
BOTO_CONFIG = Config(
	region_name=REGION,
	# Instructs boto3 to retry upto 10 times using an exponential backoff
	retries={
		"total_max_attempts": 10,
		"mode": "adaptive",
	},
	# Double the read timeout for some extra safety when synchrnously invoking the run-locust Lambda
	read_timeout=120,
)


def _list_non_compliant_log_groups(logs_client: CloudWatchLogsClient) -> list[dict[str, object]]:
	"""Return log groups that are missing or not equal to required retention."""
	non_compliant = []
	paginator = logs_client.get_paginator("describe_log_groups")

	for page in paginator.paginate():
		for group in page.get("logGroups", []):
			log_group_name = group.get("logGroupName")
			configured_retention = group.get("retentionInDays")
			if configured_retention is not None and configured_retention < REQUIRED_RETENTION_DAYS:
				non_compliant.append(
					{
						"logGroupName": log_group_name,
						"retentionInDays": configured_retention,
						"requiredRetentionInDays": REQUIRED_RETENTION_DAYS,
					}
				)
				if log_group_name:
					logs_client.put_retention_policy(
						logGroupName=log_group_name,
						retentionInDays=REQUIRED_RETENTION_DAYS,
					)

	return non_compliant


def _publish_alert(sns_client, message: str) -> None:
	"""Send alert to SNS when topic ARN is configured."""
	if not ALERT_SNS_TOPIC_ARN:
		logger.warning(
			"ALERT_SNS_TOPIC_ARN not configured. Alert only logged to CloudWatch."
		)
		return

	sns_client.publish(
		TopicArn=ALERT_SNS_TOPIC_ARN,
		Message=message,
	)


@logger.inject_lambda_context(clear_state=True, log_event=True)
def lambda_handler(event: dict[str, Any], context: LambdaContext):

	logger.info(
		"Environment: REQUIRED_RETENTION_DAYS=%r, ALERT_SNS_TOPIC_ARN=%r",
		os.getenv("REQUIRED_RETENTION_DAYS"),
		os.getenv("ALERT_SNS_TOPIC_ARN"),
	)

	logs_client = cast(CloudWatchLogsClient, boto3.client("logs", config=BOTO_CONFIG))
	sns_client = boto3.client("sns", config=BOTO_CONFIG)

	non_compliant = _list_non_compliant_log_groups(logs_client)

	if not non_compliant:
		logger.info(
			"All CloudWatch log groups are compliant with retention policy (%s days).",
			REQUIRED_RETENTION_DAYS,
		)
		return {
			"statusCode": 200,
			"body": json.dumps(
				{
					"message": "All log groups are compliant.",
					"requiredRetentionInDays": REQUIRED_RETENTION_DAYS,
					"nonCompliantCount": 0,
				}
			),
		}

	payload = {
		"AlarmName": "log-retention-non-compliance",
		"AlarmDescription": f"Found {len(non_compliant)} log groups with non-compliant retention (required: {REQUIRED_RETENTION_DAYS} days)",
		"NewStateReason": json.dumps(non_compliant, indent=2),
		"Trigger": {"MetricName": None},
	}
	# log the non-compliant log groups and publish an alert to SNS
	logger.warning(
		"Invalid resource payload: %s",
		json.dumps(non_compliant, indent=2),
	)
	alert_message = json.dumps(payload)

	logger.warning(alert_message)
	_publish_alert(sns_client, alert_message)

	return {
		"statusCode": 200,
		"body": json.dumps({
			"message": "Found log groups with non-compliant retention.",
			"requiredRetentionInDays": REQUIRED_RETENTION_DAYS,
			"nonCompliantCount": len(non_compliant),
			"nonCompliantLogGroups": non_compliant,
		}),
	}
