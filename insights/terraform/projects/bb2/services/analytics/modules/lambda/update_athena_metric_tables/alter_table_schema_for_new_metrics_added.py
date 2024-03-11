import argparse
import boto3
import re

from utils.utils import (
    run_athena_query_result_to_s3,
    download_content_from_s3,
)
"""
Summary:

Utility to ALTER a TARGET table with schema changes
from a SOURCE table (using the column differences).

This can be used when adding new metric columns/fields.

It will compare a SOURCE and TARGET table schema, then ALTER
the DESTINATION table with the new columns (difference).

See README.md for usage examples.
"""


def get_table_columns(session, params, table_name):
    """
    Returns COLUMNS list for given full table name.

    Returns a list of [column-name, type] pairs.
    """
    params["query"] = "DESCRIBE " + params["database"] + "." + table_name

    output_s3_path = run_athena_query_result_to_s3(session, params, 1000)
    result_list = download_content_from_s3(output_s3_path)

    # Get the first keyname from the very first list item.
    keyname = [a for a, b in result_list[0].items()][0]

    split_regex = re.compile(r"(\w+)\s*(date\s|int\s|bigint\s|string\s)\s+$")

    columns_list = []

    # Add [column_name, type] pairs to list
    for d in result_list:
        m = split_regex.match(d[keyname])
        if m:
            column_type_pair = []
            column_type_pair.append([m.group(1).strip(), m.group(2).strip()])
            columns_list.append(column_type_pair)

    return columns_list


def lists_diff(list1, list2):
    # Return list of items in list1 but not in list2
    diff_list = []
    for i1 in list1:
        found = False
        for i2 in list2:
            if i1 == i2:
                found = True
                break
        if not found:
            diff_list.append(i1)

    return diff_list


def alter_table_add_columns(session, params, table_name, alter_list):
    """
    ALTER the table to add columns from alter_list
    """
    if len(alter_list) == 0:
        print("---")
        print("--- ERROR: alter_list is empty!")
        print("---")
        return False

    alter_sql = (
        "ALTER TABLE " + params["database"] + "." + table_name + " ADD COLUMNS ("
    )

    cnt = 0
    for i in alter_list:
        if cnt != 0:
            alter_sql += ", "
        alter_sql += i[0][0] + " " + i[0][1]
        cnt += 1
    alter_sql += ")"

    params["query"] = alter_sql

    run_athena_query_result_to_s3(session, params, 1000)

    return True


parser = argparse.ArgumentParser(
    description="Utility to ALTER a TARGET table with schema changes from a SOURCE table (using the column differences)."
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
    "--source-table-name",
    "-s",
    help="The SOURCE table that has new columns/metrics.",
    required=True,
    type=str,
)
parser.add_argument(
    "--target-table-name",
    "-t",
    help="The TARGET table to be updated that is missing SOURCE table columns.",
    required=True,
    type=str,
)
parser.add_argument(
    "--alter-table",
    default=False,
    action='store_true',
    help="ALTER the destination table with schema differences.",
)

args = parser.parse_args()

REGION = args.region if args.region else None
WORKGROUP = args.workgroup if args.workgroup else None
DATABASE = args.database if args.database else None
ALTER_TABLE = args.alter_table if args.alter_table else None
SOURCE_TABLE = args.source_table_name if args.source_table_name else None
TARGET_TABLE = args.target_table_name if args.target_table_name else None

params = {
    "region": REGION,
    "workgroup": WORKGROUP,
    "database": DATABASE,
}

print("---")
print("---Using the following paramters:")
print("---")
print("---   AWS REGION: ", REGION)
print("---    WORKGROUP: ", WORKGROUP)
print("---     DATABASE: ", DATABASE)
print("---")
print("---   ALTER_TABLE: ", ALTER_TABLE)
print("---     (If False, just show changes to be made)")
print("---")
print("---  SOURCE_TABLE: ", SOURCE_TABLE)
print("---  TARGET_TABLE: ", TARGET_TABLE)
print("---")

session = boto3.Session()
# TODO: fix bug in get_table_columns that is unable to get boolean columns
source_columns_list = get_table_columns(session, params, SOURCE_TABLE)
target_columns_list = get_table_columns(session, params, TARGET_TABLE)

source_columns_count = len(source_columns_list)
target_columns_count = len(target_columns_list)

diff_columns_list = lists_diff(source_columns_list, target_columns_list)
print("---")
print("--- SOURCE TABLE HAS TOTAL COLUMNS = ", len(source_columns_list) + 1)
print("---")
print("--- TARGET TABLE HAS TOTAL COLUMNS = ", len(target_columns_list) + 1)
print("---")
print(
    "--- SOURCE has ",
    (source_columns_count - target_columns_count),
    " additional columns to add.",
)
print("---")
print("--- COLUMNS TO BE ADDED TO THE TARGET TABLE:")
print("---")
for i in diff_columns_list:
    print("  ADDING:  ", i[0])
print("---")
print("--- Verify columns to be add and use the --alter-table option to apply!")

if ALTER_TABLE:
    if alter_table_add_columns(session, params, TARGET_TABLE, diff_columns_list):
        print("---")
        print("--- ALTER was successful!!!")
        print("---")
    else:
        print("---")
        print("--- ALTER failed!!!!")
        print("---")
