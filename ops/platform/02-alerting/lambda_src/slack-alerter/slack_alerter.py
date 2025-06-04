import json
import os
from functools import singledispatch
from typing import Any, Union
from urllib.error import URLError
from urllib.request import Request, urlopen

from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parser import envelopes
from aws_lambda_powertools.utilities.parser.models import SnsNotificationModel
from aws_lambda_powertools.utilities.typing import LambdaContext
from pydantic import BaseModel, Field, TypeAdapter

TOPIC_TO_WEBHOOK_MAP = TypeAdapter(dict[str, str]).validate_json(
    os.environ.get("TOPIC_TO_WEBHOOK_MAP", default="")
)
TOPIC_TO_CHANNEL_MAP = TypeAdapter(dict[str, str]).validate_json(
    os.environ.get("TOPIC_TO_CHANNEL_MAP", default="")
)

logger = Logger()


def slack_escape_str(str_to_escape: str) -> str:
    """Escapes a string such that Slack can properly display it.

    See https://api.slack.com/reference/surfaces/formatting#escaping

    Args:
        str_to_escape (str): The string to escape

    Returns:
        str: A string escaped such that Slack can properly display it
    """
    return str_to_escape.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


class CloudWatchAlarmAlertTriggerModel(BaseModel):
    metric_name: str = Field(validation_alias="MetricName")


class CloudWatchAlarmAlertModel(BaseModel):
    alarm_name: str = Field(validation_alias="AlarmName")
    alarm_description: str = Field(validation_alias="AlarmDescription")
    new_state_reason: str = Field(validation_alias="NewStateReason")
    trigger: CloudWatchAlarmAlertTriggerModel | None = Field(
        validation_alias="Trigger", default=None
    )


# Add additional models with a registered singledispatch function for converting to a Slack message
# to support multiple types of alerts
Alert = Union[CloudWatchAlarmAlertModel]


@singledispatch
def as_slack_message(alert: Alert) -> dict[str, Any]: ...


@as_slack_message.register
def handle_cloudwatch_alarm(alert: CloudWatchAlarmAlertModel) -> dict[str, Any]:
    # We do not escape "<" or ">" to allow for links to be embedded in the Alarm message
    alarm_message = alert.alarm_description.replace("&", "&amp;")

    alarm_name = slack_escape_str(alert.alarm_name)
    alarm_reason = slack_escape_str(alert.new_state_reason)

    # For computed metric alarms (that is, alarms that alarm on a metric math expression), there
    # is no corresponding "MetricName" property. If it does, we specify the metric name as "N/A"
    alarm_metric = (
        slack_escape_str(alert.trigger.metric_name)
        if alert.trigger is not None
        else slack_escape_str("N/A (computed metric)")
    )

    return {
        "blocks": [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f":warning: *Alarm Alert: `{alarm_name}`* :warning:\n\n{alarm_message}\n"
                    ),
                },
            },
            {"type": "divider"},
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f"_Alarm Reason:_ `{alarm_reason}`\n_Alarm Metric:_ `{alarm_metric}`\n"
                    ),
                },
            },
        ]
    }


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> None:
    sns_notifs = [
        sns_notif
        for sns_notif in envelopes.SqsEnvelope().parse(data=event, model=SnsNotificationModel)
        if sns_notif is not None
    ]
    alerts = [
        (sns_notif.TopicArn, TypeAdapter(Alert).validate_json(str(sns_notif.Message)))
        for sns_notif in sns_notifs
    ]

    for topic_arn, alert in alerts:
        topic_name = topic_arn.split(":")[-1]
        webhook_url = TOPIC_TO_WEBHOOK_MAP[topic_name]
        slack_message = as_slack_message(alert)
        logger.info(
            "%s alert received; sending Slack message to #%s: %s",
            type(alert).__name__,
            TOPIC_TO_CHANNEL_MAP[topic_name],
            json.dumps(slack_message),
        )

        request = Request(webhook_url, method="POST")
        request.add_header("Content-Type", "application/json")
        try:
            with urlopen(request, data=json.dumps(slack_message).encode("utf-8")) as response:
                if response.status == 200:
                    logger.info("Message posted successfully")
                else:
                    logger.info("%s response received from Slack", response.status)
        except URLError:
            logger.exception("Unable to connect to Slack")
            raise
