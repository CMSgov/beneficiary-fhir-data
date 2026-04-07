import argparse
from lambda_function import lambda_handler


parser = argparse.ArgumentParser(
    description="Utility to test the lambda_function() locally for development."
)

parser.add_argument("--region", "-r", default="us-east-1", type=str)
parser.add_argument("--workgroup", "-w", default="bb2", type=str)
parser.add_argument("--database", "-d", default="bb2", type=str)
parser.add_argument("--env", "-e", required=True, type=str)
parser.add_argument("--target-report-dates", "-t", default="", type=str)
parser.add_argument("--basename-main", "-m", default="global_state_copy1", type=str)
parser.add_argument("--basename-per-app", "-p", default="global_state_per_app_copy1", type=str)
parser.add_argument("--retry-sleep-seconds", "-s", default="60", type=str)


def run(args):
    REGION = args.region
    WORKGROUP = args.workgroup
    DATABASE = args.database
    ENV = args.env
    TARGET_DATES = args.target_report_dates or ""
    BASENAME_MAIN = args.basename_main
    BASENAME_PER_APP = args.basename_per_app
    RETRY_SLEEP_SECONDS = args.retry_sleep_seconds

    target_date_list = TARGET_DATES.split(",")

    print("--- Testing lambda_function.py")
    print("--- Using the following parameters:")
    print("---     TARGET_DATE_LIST:", target_date_list)
    print("---               REGION:", REGION)
    print("---            WORKGROUP:", WORKGROUP)
    print("---             DATABASE:", DATABASE)
    print("---                  ENV:", ENV)

    event = {
        "REGION": REGION,
        "WORKGROUP": WORKGROUP,
        "DATABASE": DATABASE,
        "ENV": ENV,
        "BASENAME_MAIN": BASENAME_MAIN,
        "BASENAME_PER_APP": BASENAME_PER_APP,
        "RETRY_SLEEP_SECONDS": RETRY_SLEEP_SECONDS,
    }

    for target_date in target_date_list:
        event["TARGET_DATE"] = target_date
        status = lambda_handler(event, None)
        print("STATUS:", status)


def main():
    args = parser.parse_args()
    run(args)


if __name__ == "__main__":
    main()
