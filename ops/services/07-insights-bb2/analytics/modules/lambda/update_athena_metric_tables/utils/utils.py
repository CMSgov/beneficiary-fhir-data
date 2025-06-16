import boto3
import csv
import re
import time

from datetime import datetime, timezone
from dateutil.relativedelta import relativedelta, MO
from io import StringIO
from string import Template

"""
Summary:

Utility functions shared by:
  - lambda_update_athena_metric_tables.py
  - test_run_sql_template_on_athena.py
"""


def athena_query(client, params):
    # NOTE: Output files will show up in the location configured for the workgroup.
    response = client.start_query_execution(
        QueryString=params["query"],
        QueryExecutionContext={"Database": params["database"]},
        WorkGroup=params["workgroup"],
    )
    return response


def run_athena_query_result_to_s3(session, params, max_execution=60):
    client = session.client("athena", region_name=params["region"])
    execution = athena_query(client, params)
    execution_id = execution["QueryExecutionId"]
    state = "RUNNING"
    print("## QueryExecutionId = ", execution_id)

    while max_execution > 0 and state in ["RUNNING", "QUEUED"]:
        max_execution = max_execution - 5
        response = client.get_query_execution(QueryExecutionId=execution_id)

        if (
            "QueryExecution" in response
            and "Status" in response["QueryExecution"]
            and "State" in response["QueryExecution"]["Status"]
        ):
            state = response["QueryExecution"]["Status"]["State"]
            if state == "FAILED":
                return False
            elif state == "SUCCEEDED":
                s3_path = response["QueryExecution"]["ResultConfiguration"][
                    "OutputLocation"
                ]
                return s3_path

        time.sleep(5)

    return False


def download_content_from_s3(s3_path, csv_format=True):
    """
    Returns results as a list of dict items if csv_format=True
      else return output value.
    """
    s3 = boto3.resource("s3")
    bucket_name = re.findall(r"^s3://([^/]+)", s3_path)[0]
    key = re.findall(r"^s3://[^/]+[/](.+)", s3_path)[0]
    try:
        response = s3.Object(bucket_name, key).get()
    except s3.meta.client.exceptions.NoSuchKey:
        return None

    f = StringIO(response["Body"].read().decode("utf-8"))
    if csv_format:
        # Load csv in to dictionary
        result_list = []
        for line in csv.DictReader(f):
            result_list.append(line)
        return result_list
    else:
        return f.getvalue()


def check_table_exists(session, params, table_basename):
    """
    Returns True if table for table_basename exists, else False.
    """
    params["query"] = (
        "SELECT count(*) FROM information_schema.tables "
        + "WHERE table_schema = '"
        + params["database"]
        + "' AND table_name = '"
        + params["env"]
        + "_"
        + table_basename
        + "'"
    )

    output_s3_path = run_athena_query_result_to_s3(session, params, 1000)
    result_list = download_content_from_s3(output_s3_path)
    count = result_list[0].get("_col0")

    if count == "0":
        return False
    else:
        return True


def check_table_for_report_date_entry(session, params, table_basename):
    """
    Returns True/False for check of an existing
    entry in the table for the report_date.
    """
    # Setup SQL
    params["query"] = (
        "SELECT count(*) "
        + "FROM "
        + params["database"]
        + "."
        + params["env"]
        + "_"
        + table_basename
        + " "
        + "WHERE report_date = CAST('"
        + params["report_dates"]["report_date"]
        + "' AS Date)"
    )

    output_s3_path = run_athena_query_result_to_s3(session, params, 1000)
    if output_s3_path:
        result_list = download_content_from_s3(output_s3_path)
    else:
        return False

    count = result_list[0].get("_col0")

    if count == "0":
        return False
    else:
        return True


def get_sql_from_template_file(filepath, params):
    f = open(filepath, "rt")
    # read file contents to template obj
    template = Template(f.read())
    f.close()

    ret = template.substitute(
        ENV=params["env"],
        BASENAME_PER_APP=params["basename_per_app"],
        START_DATE=params["report_dates"]["start_date"],
        END_DATE=params["report_dates"]["end_date"],
        REPORT_DATE=params["report_dates"]["report_date"],
        PARTITION_LIMIT_SQL=params["report_dates"]["partition_limit_sql"],
    )

    if params.get("append_sql", None):
        ret = ret + "\n" + params["append_sql"]

    return ret


def get_table_columns_select_list(session, params, table_basename):
    """
    Returns string with select list of columns for a given table.
    Ex:  "vpc, start_date, end_date, ..."
    """
    params["query"] = (
        "SHOW COLUMNS FROM "
        + params["database"]
        + "."
        + params["env"]
        + "_"
        + table_basename
    )

    output_s3_path = run_athena_query_result_to_s3(session, params, 1000)
    result_list = download_content_from_s3(output_s3_path, csv_format=False)

    items_list = result_list.split()
    return ",".join(items_list)


def run_athena_query_using_template(session, params, template_file):
    """
    Run the athena query using template for the report_date.
    """
    params["query"] = get_sql_from_template_file(template_file, params)

    output_s3_path = run_athena_query_result_to_s3(session, params, 1000)
    result_list = download_content_from_s3(output_s3_path)

    return result_list


def update_or_create_table_for_report_date(
    session, params, table_basename, template_file, table_exists
):
    """
    Update the table with metrics for the report_date.
    """
    if table_exists:
        select_list = get_table_columns_select_list(session, params, table_basename)

        params["query"] = (
            "INSERT INTO "
            + params["database"]
            + "."
            + params["env"]
            + "_"
            + table_basename
            + "\n SELECT "
            + select_list
            + " FROM \n("
            + get_sql_from_template_file(template_file, params)
            + "\n)"
        )
    else:
        params["query"] = (
            "CREATE TABLE "
            + params["database"]
            + "."
            + params["env"]
            + "_"
            + table_basename
            + " AS "
            + get_sql_from_template_file(template_file, params)
        )

    output_s3_path = run_athena_query_result_to_s3(session, params, 1000)

    if output_s3_path:
        result_list = download_content_from_s3(output_s3_path)
    else:
        result_list = None

    return result_list


def update_or_create_metrics_table(session, params, table_basename, template_file):

    print("##")
    print(
        "## --- UPDATE/CREATE TABLE:  "
        + params["database"]
        + "."
        + params["env"]
        + "_"
        + table_basename
    )
    print("##")

    # Check if per_app table already exists
    table_exists = check_table_exists(session, params, table_basename)
    print("## table_exists:  ", table_exists)

    # Update the per_app table if an entry does not already exist.
    success_flag = False
    for attempt_count in range(3):
        # NOTE: Retry SQL run 3x for random Athena time-out issue.
        print("## SQL RUN ATTEMPT:  ", attempt_count + 1)
        if table_exists:
            if check_table_for_report_date_entry(session, params, table_basename):
                print("## TABLE already has entry for report_date. Skipping...")
                success_flag = True
                break
            else:
                print("## Updating TABLE...")
                # Update table
                update_or_create_table_for_report_date(
                    session, params, table_basename, template_file, table_exists
                )
        else:
            # Create table
            print("## Creating new TABLE...")
            update_or_create_table_for_report_date(
                session, params, table_basename, template_file, table_exists
            )

        # Checking if table was updated with SQL results
        if check_table_for_report_date_entry(session, params, table_basename):
            success_flag = True
            break

        # Sleep between retries.
        if not success_flag:
            retry_seconds = params.get("retry_sleep_seconds", "60")
            print("## RETRY SLEEPING FOR: ", retry_seconds)
            time.sleep(int(retry_seconds))

    return success_flag


def get_report_dates_from_target_date(target_date_str=""):
    """
    Given a target date string return dates for the
    report week to be used in queries.

    Use today's date if empty str.

    Returns:
       report_date
       start_date
       end_date
       dt
       partition_1
    """
    if target_date_str == "":
        target_date = datetime.now(timezone.utc)
    else:
        target_date = datetime.strptime(target_date_str, "%Y-%m-%d").astimezone(
            timezone.utc
        )

    # Get report_date (Monday) from target date
    report_date = target_date + relativedelta(weekday=MO(-1))

    # Get start date for report week
    start_date = report_date + relativedelta(weekday=MO(-2))

    # Get end date for report week
    end_date = report_date + relativedelta(days=-1)

    # Get Athena partition values from start/end dates
    partition_min_year = start_date.strftime("%Y")
    partition_max_year = report_date.strftime("%Y")
    partition_min_day = start_date.strftime("%d")
    partition_max_day = report_date.strftime("%d")

    if partition_max_year > partition_min_year:
        """
        TODO: Improve this with the Glue table partitioning.

        The partitioning should be setup using a format that can
        cross end of year dates. For example, dt = "2022-12-31".

        The current partitioning is split a dt = year, partition_1 = month,
        so this causes performance issues in the current setup and EOY.

        The current performance is acceptable, but should revisit
        for future needs.
        """
        partition_min_month = "01"
        partition_max_month = "12"
        partition_limit_sql = (
            "( (dt = '"
            + partition_min_year
            + "' AND partition_1 = '12' AND partition_2 >= '"
            + partition_min_day
            + "') OR (dt = '"
            + partition_max_year
            + "' AND partition_1 = '01' AND partition_2 <= '"
            + partition_max_day
            + "') )"
        )
    else:
        # Set min/max month for partition search.Speeds up query!
        partition_min_month = start_date.strftime("%m")
        partition_max_month = report_date.strftime("%m")
        if partition_max_month > partition_min_month:
            partition_limit_sql = (
                "(dt = '"
                + partition_min_year
                + "' AND partition_1 = '"
                + partition_min_month
                + "' AND partition_2 >= '"
                + partition_min_day
                + "') OR (dt = '"
                + partition_max_year
                + "' AND partition_1 = '"
                + partition_max_month
                + "' AND partition_2 <= '"
                + partition_max_day
                + "')"
            )
        else:
            partition_limit_sql = (
                "(dt = '"
                + partition_min_year
                + "' AND partition_1 = '"
                + partition_min_month
                + "' AND partition_2 >= '"
                + partition_min_day
                + "') AND (dt = '"
                + partition_max_year
                + "' AND partition_1 = '"
                + partition_max_month
                + "' AND partition_2 <= '"
                + partition_max_day
                + "')"
            )

    report_dates = {
        "report_date": report_date.strftime("%Y-%m-%d"),
        "start_date": start_date.strftime("%Y-%m-%d"),
        "end_date": end_date.strftime("%Y-%m-%d"),
        "partition_min_year": partition_min_year,
        "partition_min_month": partition_min_month,
        "partition_min_day": partition_min_day,
        "partition_max_year": partition_max_year,
        "partition_max_month": partition_max_month,
        "partition_max_day": partition_max_day,
        "partition_limit_sql": partition_limit_sql,
    }
    return report_dates


def output_results_list_to_csv_file(result_list, output_file, include_header=True):
    keys = result_list[0].keys()

    with open(output_file, "w", encoding="utf8", newline="") as f:
        dw = csv.DictWriter(f, keys)
        if include_header:
            dw.writeheader()
        dw.writerows(result_list)
