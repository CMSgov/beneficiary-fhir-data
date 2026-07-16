import json
import logging
import os
from typing import Dict, List

import boto3
from botocore.config import Config# type: ignore[import-not-found]


logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

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


def _list_non_compliant_log_groups(logs_client) -> List[Dict[str, object]]:
	"""Return log groups that are missing or not equal to required retention."""
	non_compliant = []
	paginator = logs_client.get_paginator("describe_log_groups")

	for page in paginator.paginate():
		for group in page.get("logGroups", []):
			configured_retention = group.get("retentionInDays")
			if configured_retention is not None and configured_retention < REQUIRED_RETENTION_DAYS:
				non_compliant.append(
					{
						"logGroupName": group.get("logGroupName"),
						"retentionInDays": configured_retention,
						"requiredRetentionInDays": REQUIRED_RETENTION_DAYS,
					}
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


def lambda_handler(event, context):
	del event, context

	logger.info(
		"Environment: REQUIRED_RETENTION_DAYS=%r, ALERT_SNS_TOPIC_ARN=%r",
		os.getenv("REQUIRED_RETENTION_DAYS"),
		os.getenv("ALERT_SNS_TOPIC_ARN"),
	)

	logs_client = boto3.client("logs", config=BOTO_CONFIG)
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
