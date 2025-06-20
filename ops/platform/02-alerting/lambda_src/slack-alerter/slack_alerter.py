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
    os.environ.get("TOPIC_TO_WEBHOOK_MAP", default="{}")
)
TOPIC_TO_CHANNEL_MAP = TypeAdapter(dict[str, str]).validate_json(
    os.environ.get("TOPIC_TO_CHANNEL_MAP", default="{}")
)
logger = Logger()


def slack_escape_str(str_to_escape: str) -> str:
    return str_to_escape.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


class CloudWatchAlarmAlertTriggerModel(BaseModel):
    metric_name: str | None = Field(validation_alias="MetricName", default=None)


class CloudWatchAlarmAlertModel(BaseModel):
    alarm_name: str = Field(validation_alias="AlarmName")
    alarm_description: str = Field(validation_alias="AlarmDescription")
    new_state_reason: str = Field(validation_alias="NewStateReason")
    trigger: CloudWatchAlarmAlertTriggerModel = Field(validation_alias="Trigger")


class EventBridgeAlertModel(BaseModel):
    source: str
    detail_type: str = Field(alias="detail-type")
    detail: dict[str, Any]
    account: str
    region: str
    resources: list[str]


Alert = Union[CloudWatchAlarmAlertModel, EventBridgeAlertModel]


@singledispatch
def as_slack_message(alert: Alert) -> dict[str, Any]: ...


@as_slack_message.register
def handle_cloudwatch_alarm(alert: CloudWatchAlarmAlertModel) -> dict[str, Any]:
    alarm_message = alert.alarm_description.replace("&", "&amp;")
    alarm_name = slack_escape_str(alert.alarm_name)
    alarm_reason = slack_escape_str(alert.new_state_reason)
    alarm_metric = (
        slack_escape_str(alert.trigger.metric_name)
        if alert.trigger.metric_name is not None
        else slack_escape_str("N/A (computed metric)")
    )
    return {
        "blocks": [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f":warning: *Alarm Alert: `{alarm_name}`* :warning:\n\n{alarm_message}\n",
                },
            },
            {"type": "divider"},
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"_Alarm Reason:_ `{alarm_reason}`\n_Alarm Metric:_ `{alarm_metric}`\n",
                },
            },
        ]
    }


@as_slack_message.register
def handle_eventbridge_event(alert: EventBridgeAlertModel) -> dict[str, Any]:
    return {
        "blocks": [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f":satellite: *EventBridge Event*\n"
                        f"*Source:* `{alert.source}`\n"
                        f"*Type:* `{alert.detail_type}`\n"
                        f"*Region:* `{alert.region}`\n"
                        f"*Account:* `{alert.account}`\n"
                        f"*Resources:* {', '.join(alert.resources)}\n"
                        f"```{json.dumps(alert.detail, indent=2)}```"
                    ),
                },
            }
        ]
    }


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> None:
    sns_messages = (
        msg
        for msg in envelopes.SqsEnvelope().parse(data=event, model=SnsNotificationModel)
        if msg is not None
    )
    for sns_notif in sns_messages:
        topic_arn = sns_notif.TopicArn
        topic_name = topic_arn.split(":")[-1]
        raw_message = str(sns_notif.Message)
        alert = TypeAdapter(Alert).validate_json(raw_message)
        webhook_url = TOPIC_TO_WEBHOOK_MAP.get(topic_name)
        channel = TOPIC_TO_CHANNEL_MAP.get(topic_name)
        if not webhook_url or not channel:
            logger.warning("No webhook/channel mapping found for topic: %s", topic_name)
            continue
        slack_message = as_slack_message(alert)
        logger.info(
            "%s alert received; sending Slack message to #%s: %s",
            type(alert).__name__,
            channel,
            json.dumps(slack_message),
        )
        request = Request(webhook_url, method="POST")
        request.add_header("Content-Type", "application/json")
        try:
            with urlopen(request, data=json.dumps(slack_message).encode("utf-8")) as response:
                if response.status == 200:
                    logger.info("Message posted successfully")
                else:
                    logger.warning("Slack returned status: %s", response.status)
        except URLError:
            logger.exception("Unable to connect to Slack")
            raise
