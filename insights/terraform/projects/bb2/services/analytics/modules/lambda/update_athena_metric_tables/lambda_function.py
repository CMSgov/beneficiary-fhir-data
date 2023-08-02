import boto3
import datetime

from utils.utils import (
    get_report_dates_from_target_date,
    update_or_create_metrics_table,
)


"""
Summary:

This lambda supports the updating (or creation) of metric
tables via Athena used for BB2 Insights dashboards in QuickSight.

It does the following:

- Accepts a lambda parameters dictionary in the following format:
    {
      "REGION": "<AWS region>",
      "WORKGROUP": "bb2",
      "DATABASE": "bb2",
      "ENV": "<prod/impl/test>"
      "BASENAME_MAIN": "<basename of main table>" Ex: "global_state"
      "BASENAME_PER_APP": "<basename of per-app table>" Ex: "global_state_per_app"
      "TARGET_DATE": <target report date week> EX: "2022-09-19",
      "RETRY_SLEEP_SECONDS": "30", Default is 60, if ommitted.
    }

- Computes the report_date, start_date, and end_date based on the TARGET_DATE param.

  - If the TARGET_DATE is blank, the current date will be used for the TARGET_DATE.

- Updates (or creates) the per-applications table for the target report_date,
  ENV and BASENAME_PER_APP params.

- Updates (or creates) the main (top-level) table for the target report_date,
  ENV and BASENAME_MAIN params.

For each of the tables, it does the following:

- Check if the table already exists. Setup to create it, if not.

- Check if an entry for the report_date already exists to prevent duplicate
  entries.

- Execute the relatate SQL from the corresponding template file and
  update (or create) the table with the results.

- Retry running the SQL if there is a time-out up to 3-times.
  Will sleep between retries.

"""


TEMPLATE_FILE_PER_APP = (
    "./sql_templates/template_generate_per_app_metrics_for_report_date.sql"
)

TEMPLATE_FILE_MAIN = "./sql_templates/template_generate_metrics_for_report_date.sql"


def lambda_handler(event, context):
    session = boto3.Session()

    target_week_date = event["TARGET_DATE"]
    report_dates = get_report_dates_from_target_date(target_week_date)
    print("##")
    print("## -------------------------")
    print("## UPDATING FOR TARGET_DATE (if blank, today):  ", target_week_date)
    print("##")
    print("## report_dates DICT: ", report_dates)
    print("##")

    params = {
        "region": event["REGION"],
        "workgroup": event["WORKGROUP"],
        "database": event["DATABASE"],
        "env": event["ENV"],
        "basename_main": event["BASENAME_MAIN"],
        "basename_per_app": event["BASENAME_PER_APP"],
        "report_dates": report_dates,
        "retry_sleep_seconds": event.get("RETRY_SLEEP_SECONDS", "300")
    }

    lambda_start_time = datetime.datetime.now()
    print("##")
    print("## EVENT: " + str(event))
    print("##")
    print("## Lambda Start Time: ", lambda_start_time)
    print("##")

    # Update/create PER_APP table
    query_start_time = datetime.datetime.now()
    success_flag = update_or_create_metrics_table(
        session, params, params["basename_per_app"], TEMPLATE_FILE_PER_APP
    )
    query_end_time = datetime.datetime.now()
    query_duration_time = query_end_time - query_start_time
    print("## Query Duration Time: ", query_duration_time)

    if success_flag:
        print("## SUCCESS: PER_APP TABLE was updated/created!")
    else:
        print("## FAIL: PER_APP TABLE update/create un-successful after retries!")
        return {
            "STATUS": "FAIL",
            "DETAIL": "PER_APP table create/update was un-successful after retries!",
        }

    # Update/create MAIN table
    query_start_time = datetime.datetime.now()
    success_flag = update_or_create_metrics_table(
        session, params, params["basename_main"], TEMPLATE_FILE_MAIN
    )
    query_end_time = datetime.datetime.now()
    query_duration_time = query_end_time - query_start_time
    print("## Query Duration Time: ", query_duration_time)

    if success_flag:
        print("## SUCCESS: MAIN TABLE was updated/created!")

        lambda_end_time = datetime.datetime.now()
        lambda_duration_time = lambda_end_time - lambda_start_time
        print("##")
        print("## Lambda End Time: ", lambda_end_time)
        print("## Lambda Duration Time: ", lambda_duration_time)
        print("##")
    else:
        print("## FAIL: MAIN TABLE update/create un-successful after retries!")
        return {
            "STATUS": "FAIL",
            "DETAIL": "MAIN table create/update was un-successful after retries!",
        }

    return {
        "STATUS": "SUCCESS",
        "DETAIL": "Metric tables are ready for refresh in QuickSight!",
    }
