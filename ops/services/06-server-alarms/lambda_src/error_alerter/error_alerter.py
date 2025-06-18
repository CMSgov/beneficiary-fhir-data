import calendar
import json
import logging
import os
import sys
import time
from datetime import UTC, datetime, timedelta
from typing import Any, TypedDict
from urllib.error import URLError
from urllib.parse import quote_plus
from urllib.request import Request, urlopen

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
ACCESS_JSON_LOG_GROUP = os.environ.get("ACCESS_JSON_LOG_GROUP", "")
MESSAGES_JSON_LOG_GROUP = os.environ.get("MESSAGES_JSON_LOG_GROUP", "")
LOG_LOOKBACK_SECONDS = int(os.environ.get("LOG_LOOKBACK_SECONDS", "330"))
SLACK_WEBHOOK = os.environ.get("SLACK_WEBHOOK", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

LOG_INSIGHTS_NOTIF_QUERY = """
fields
mdc.http_access_response_status as status,
mdc.http_access_request_clientSSL_DN as partner,
mdc.http_access_request_operation as op
| filter status == 500
| stats count(status) by partner, op
""".strip()
LOG_INSIGHTS_ACCESS_JSON_ERRORS_QUERY = """
fields @message,
mdc.http_access_response_status as status
| filter status == 500
| sort @timestamp desc
""".strip()
LOG_INSIGHTS_MESSAGES_JSON_ERRORS_QUERY = """
fields @message
| filter ispresent(exception) and ispresent(mdc.http_access_request_uri) and level = 'ERROR'
| sort @timestamp desc
""".strip()


class LogInsightsQueryResultTypeDef(TypedDict, total=False):
    field: str
    value: str


logging.basicConfig(level=logging.INFO, force=True)
logger = logging.getLogger()
try:
    logs_client = boto3.client("logs", config=BOTO_CONFIG)  # type: ignore
except Exception:
    logger.error(
        "Unrecoverable exception occurred when attempting to create boto3 clients/resources:",
        exc_info=True,
    )
    sys.exit(0)


def __get_value_by_key(cell_key: str, row_list: list[LogInsightsQueryResultTypeDef]) -> str:
    return next(
        result_cell
        for result_cell in row_list
        if "field" in result_cell and "value" in result_cell and result_cell["field"] == cell_key
    )["value"]


def __escape_log_insights_url_str(unescaped_str: str) -> str:
    return quote_plus(unescaped_str).replace("%", "*")


def __gen_log_insights_url(
    start_time: datetime, end_time: datetime, editor_string: str, source_log_group: str
) -> str:
    # Log Insights uses a _strange_ escape and query string scheme that fully and properly
    # implementing would be too much effort. Instead, some shortcuts are made here with the
    # assumption that these URLs will have _very_ little variation in what data is used to generate
    # them.

    url_prefix = f"https://{REGION}.console.aws.amazon.com/cloudwatch/home?region={REGION}#logsV2:logs-insights$3FqueryDetail$3D~"
    start_time_iso = (
        f"{__escape_log_insights_url_str(start_time.isoformat(timespec='seconds'))}.000Z"
    )
    end_time_iso = f"{__escape_log_insights_url_str(end_time.isoformat(timespec='seconds'))}.000Z"
    editor_string = __escape_log_insights_url_str(editor_string)
    query_params = {
        "end": end_time_iso,
        "start": start_time_iso,
        "timeType": "ABSOLUTE",
        "tz": "UTC",
        "editorString": editor_string,
    }
    # For whatever reason, '=' is escaped as "~'"
    query_param_fragments = [f"{k}~'{v}" for k, v in query_params.items()]
    query_params_str = f"({'~'.join(query_param_fragments)}~source~(~'{__escape_log_insights_url_str(source_log_group)}))"

    return f"{url_prefix}{query_params_str}"


def handler(event: dict[str, str], context: Any):
    if not all([REGION, BFD_ENVIRONMENT, LOG_LOOKBACK_SECONDS]):
        logger.error("Not all necessary environment variables were defined, exiting...")
        return

    utc_now = datetime.now(UTC)
    start_time = utc_now - timedelta(seconds=LOG_LOOKBACK_SECONDS)
    start_time_timestamp = calendar.timegm(start_time.utctimetuple())
    start_time_iso = start_time.isoformat(timespec="seconds")
    end_time = utc_now
    end_time_timestamp = calendar.timegm(end_time.utctimetuple())
    end_time_iso = end_time.isoformat(timespec="seconds")
    logger.info(
        "Querying %s log group for 500 responses per-partner, per-operation between %s"
        " UTC and %s UTC (%s second(s) timespan)...",
        ACCESS_JSON_LOG_GROUP,
        start_time_iso,
        end_time_iso,
        LOG_LOOKBACK_SECONDS,
    )
    try:
        start_query_response = logs_client.start_query(
            logGroupName=ACCESS_JSON_LOG_GROUP,
            startTime=start_time_timestamp,
            endTime=end_time_timestamp,
            queryString=LOG_INSIGHTS_NOTIF_QUERY,
        )
    except logs_client.exceptions.ClientError:
        logger.error(
            "An unrecoverable error occurred when trying to query for 500s in %s:",
            ACCESS_JSON_LOG_GROUP,
            exc_info=True,
        )
        return

    try:
        response = logs_client.get_query_results(queryId=start_query_response["queryId"])
        while response["status"] == "Running" or response["status"] == "Scheduled":
            logger.info(
                'Query "%s" has not finished running (current status: %s), waiting 1 second...',
                start_query_response["queryId"],
                response["status"],
            )
            time.sleep(1)
            response = logs_client.get_query_results(queryId=start_query_response["queryId"])
    except logs_client.exceptions.ClientError:
        logger.error(
            'Retrieving results for query ID "%s" has failed due to an unrecoverable error:',
            exc_info=True,
        )
        return

    logger.info(
        "Query (ID: %s) has completed with status %s",
        start_query_response["queryId"],
        response["status"],
    )

    if not response["results"]:
        logger.info(
            "No results found. It is likely there have been no errors in the past %s"
            " second(s). Stopping...",
            LOG_LOOKBACK_SECONDS,
        )
        return

    # Response has the following format:
    # [
    #     [
    #         {"field": "partner", "value": "..."},
    #         {"field": "op", "value": "..."},
    #         {"field": "count(status)", "value": "..."},
    #     ],
    #     [...],
    # ]
    # Parse the response to create a dict of partner to error count with the following format:
    # {
    #     "partner1": {
    #         "op1": ...,
    #         "op2": ...,
    #     },
    #     "...": {...}
    # }
    partner_to_errors: dict[str, dict[str, int]] = {}
    for result_row_list in response["results"]:
        partner = __get_value_by_key("partner", result_row_list)
        op_to_count = {
            __get_value_by_key("op", result_row_list): int(
                __get_value_by_key("count(status)", result_row_list)
            )
        }
        partner_to_errors[partner] = (
            partner_to_errors[partner] | op_to_count
            if partner in partner_to_errors
            else op_to_count
        )

    total_errors = sum(sum(x.values()) for x in partner_to_errors.values())
    logger.info(
        "Per-partner error results: %s, total errors: %s in past %s second(s)",
        partner_to_errors,
        total_errors,
        LOG_LOOKBACK_SECONDS,
    )

    per_partner_tables: dict[str, str] = {}
    for partner, op_counts in partner_to_errors.items():
        longest_op_chars_count = max(len(x) for x in op_counts.keys())
        longest_err_chars_count = max(len(str(x)) for x in op_counts.values())
        table_block_str = f"```\n{'Operation':<{longest_op_chars_count}} {'Error Count':<{longest_err_chars_count}}\n"
        for op, count in op_counts.items():
            table_block_str += (
                f"{op:<{longest_op_chars_count}} {count:<{longest_err_chars_count}}\n"
            )
        table_block_str += "```"

        per_partner_tables[partner] = table_block_str

    log_insights_access_json_url = __gen_log_insights_url(
        start_time=start_time,
        end_time=end_time,
        editor_string=LOG_INSIGHTS_ACCESS_JSON_ERRORS_QUERY,
        source_log_group=ACCESS_JSON_LOG_GROUP,
    )
    log_insights_messages_json_errors_url = __gen_log_insights_url(
        start_time=start_time,
        end_time=end_time,
        editor_string=LOG_INSIGHTS_MESSAGES_JSON_ERRORS_QUERY,
        source_log_group=MESSAGES_JSON_LOG_GROUP,
    )
    slack_message = {
        "blocks": [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f":warning: *Error Alert for `{BFD_ENVIRONMENT}` BFD Server* :warning:\n"
                    ),
                },
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f"*{total_errors}* `500` errors occurred in"
                        f" `{BFD_ENVIRONMENT}` BFD Server from"
                        f" `{start_time_iso} UTC` to"
                        f" `{end_time_iso} UTC` (approx. the past"
                        f" {LOG_LOOKBACK_SECONDS} second(s))"
                    ),
                },
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": "\n_*Per-partner, per-operation breakdown(s)*_:\n\n"
                    + "\n".join(
                        f"*{partner}*\n{table}" for partner, table in per_partner_tables.items()
                    ),
                },
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f"\n_For the time period of `{start_time_iso} UTC` to"
                        f" `{end_time_iso} UTC`_:\n- <{log_insights_access_json_url}|Open Log"
                        " Insights query for all 500s in access.json>\n-"
                        f" <{log_insights_messages_json_errors_url}|Open Log Insights query for all"
                        " HTTP request errors with exceptions in messages.json>"
                    ),
                },
            },
        ]
    }

    if SLACK_WEBHOOK:
        logger.info("POSTing %s to Slack Webhook...", slack_message)

        request = Request(SLACK_WEBHOOK, method="POST")
        request.add_header("Content-Type", "application/json")
        try:
            with urlopen(request, data=json.dumps(slack_message).encode("utf-8")) as response:
                if response.status == 200:
                    logger.info("Message posted successfully")
                else:
                    logger.error("%s response received from Slack", response.status)
        except URLError:
            logger.error(
                "An unrecoverable error occurred attempting to post Slack message: ",
                exc_info=True,
            )
    else:
        logger.info("No Slack webhook defined, logging Slack message instead")
        logger.info(slack_message)
