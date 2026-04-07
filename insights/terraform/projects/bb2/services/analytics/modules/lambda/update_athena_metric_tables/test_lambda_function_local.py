import argparse
from lambda_function import lambda_handler

"""
Call / Test lambda handler with params for local development.

Edit and use the following params when testing and developing locally.

NOTE: These params are not used when launched via AWS Lambda.

When launching via AWS Lambda you must pass the parameters dictionary
via the "event". This can be included with the Lambda TEST parameters or
via the EventBridge caller.
"""
parser = argparse.ArgumentParser(
    description="Utility to test the lambda_function() locally for development."
)
parser.add_argument(
    "--region",
    "-r",
    help="The Athena AWS region.",
    default="us-east-1",
    type=str,
)
parser.add_argument(
    "--workgroup",
    "-w",
    help="The AWS Glue table workgroup.",
    default="bb2",
    type=str,
)
parser.add_argument(
    "--database",
    "-d",
    help="The AWS Glue table database.",
    default="bb2",
    type=str,
)
parser.add_argument(
    "--env",
    "-e",
    help="The BB2 environment (prod/impl/test).",
    required=True,
    type=str,
)
parser.add_argument(
    "--target-report-dates",
    "-t",
    help="The target report dates. EX: 2023-02-27, 2023-03-06",
    default="",
    type=str,
)

parser.add_argument(
    "--basename-main",
    "-m",
    help="The basename for the top-level/main metric table. EX: global_state_copy1",
    default="global_state_copy1",
    type=str,
)

parser.add_argument(
    "--basename-per-app",
    "-p",
    help="The basename for the per-application metric table. EX: global_state_per_app_copy1",
    default="global_state_per_app_copy1",
    type=str,
)
parser.add_argument(
    "--retry-sleep-seconds",
    "-s",
    help="The time in seconds between query retries. EX: 30",
    default="60",
    type=str,
)

args = parser.parse_args()

REGION = args.region if args.region else None
WORKGROUP = args.workgroup if args.workgroup else None
DATABASE = args.database if args.database else None
ENV = args.env if args.env else None
TARGET_DATES = args.target_report_dates if args.target_report_dates else ""
BASENAME_MAIN = args.basename_main if args.basename_main else None
BASENAME_PER_APP = args.basename_per_app if args.basename_per_app else None
RETRY_SLEEP_SECONDS = args.retry_sleep_seconds if args.retry_sleep_seconds else None

target_date_list = TARGET_DATES.split(",")

print("--- Testing lambda_function.py")
print("---")
print("--- Using the following parameters:")
print("---")
print("---     TARGET_DATE_LIST: ", str(target_date_list))
print("---               REGION: ", REGION)
print("---            WORKGROUP: ", WORKGROUP)
print("---             DATABASE: ", DATABASE)
print("---                  ENV: ", ENV)
print("---        BASENAME_MAIN: ", BASENAME_MAIN)
print("---     BASENAME_PER_APP: ", BASENAME_PER_APP)
print("---  RETRY_SLEEP_SECONDS: ", RETRY_SLEEP_SECONDS)
print("---")


event = {
    "REGION": REGION,
    "WORKGROUP": WORKGROUP,
    "DATABASE": DATABASE,
    "ENV": ENV,
    "BASENAME_MAIN": BASENAME_MAIN,
    "BASENAME_PER_APP": BASENAME_PER_APP,
    "RETRY_SLEEP_SECONDS": RETRY_SLEEP_SECONDS
}


for target_date in target_date_list:
    event["TARGET_DATE"] = target_date
    context = None
    status = lambda_handler(event, context)
    print("##")
    print("## STATUS:  ", status)
    print("##")
    print("##")
    print("## -------------------------------")
    print("##")
    print("##")
