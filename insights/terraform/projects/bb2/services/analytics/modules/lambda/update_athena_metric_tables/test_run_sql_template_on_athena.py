import argparse
import boto3

from utils.utils import (
    get_report_dates_from_target_date,
    run_athena_query_using_template,
    output_results_list_to_csv_file,
    update_or_create_metrics_table,
)

parser = argparse.ArgumentParser(
    description="Utility script to test out Athena SQL template for development."
)

parser.add_argument("--input-template-file", "-i", required=True, type=str)
parser.add_argument("--region", "-r", default="us-east-1", type=str)
parser.add_argument("--workgroup", "-w", default="bb2", type=str)
parser.add_argument("--database", "-d", default="bb2", type=str)
parser.add_argument("--env", "-e", required=True, type=str)
parser.add_argument("--target-report-date", "-t", required=True, type=str)
parser.add_argument("--results-output-file", "-o", default="", type=str)
parser.add_argument("--basename_per_app", "-b", default="global_state_per_app_testing1", type=str)
parser.add_argument("--append-sql", "-a", default="", type=str)
parser.add_argument("--update-per-app-table", action="store_true", default=False)


def run(args):
    INPUT_TEMPLATE_FILE = args.input_template_file
    REGION = args.region
    WORKGROUP = args.workgroup
    DATABASE = args.database
    ENV = args.env
    TARGET_DATE = args.target_report_date
    OUTPUT_FILE = args.results_output_file
    BASENAME_PER_APP = args.basename_per_app
    APPEND_SQL = args.append_sql
    UPDATE_PER_APP_TABLE = args.update_per_app_table

    print("--- Running SQL from template in Athena...")

    session = boto3.Session()

    report_dates = get_report_dates_from_target_date(TARGET_DATE)

    params = {
        "region": REGION,
        "workgroup": WORKGROUP,
        "database": DATABASE,
        "env": ENV,
        "basename_main": None,
        "basename_per_app": BASENAME_PER_APP,
        "append_sql": APPEND_SQL,
        "report_dates": report_dates,
    }

    if UPDATE_PER_APP_TABLE:
        success_flag = update_or_create_metrics_table(
            session, params, BASENAME_PER_APP, INPUT_TEMPLATE_FILE
        )

        if success_flag:
            print("SUCCESS: table updated/created")
        else:
            print("FAIL: table update/create unsuccessful")
    else:
        result_list = run_athena_query_using_template(
            session, params, INPUT_TEMPLATE_FILE
        )

        print("Results:", len(result_list))

        if OUTPUT_FILE:
            output_results_list_to_csv_file(result_list, OUTPUT_FILE)


def main():
    args = parser.parse_args()
    run(args)


if __name__ == "__main__":
    main()
