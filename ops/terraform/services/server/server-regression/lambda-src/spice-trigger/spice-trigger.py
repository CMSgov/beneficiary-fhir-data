import boto3
from datetime import datetime
import os

ENV = os.environ.get("ENV", "")


def lambda_handler(event, context):
    if not ENV:
        print("ENV was not defined, exiting...")
        return

    client = boto3.client("quicksight")
    account_id = boto3.client("sts").get_caller_identity().get("Account")
    data_set_id = get_data_set_id(client, account_id)
    refresh_data_set(client, account_id, data_set_id)


def get_data_set_id(client, account_id):
    data_sets = client.search_data_sets(
        AwsAccountId=account_id,
        Filters=[
            {
                "Operator": "StringLike",
                "Name": "DATASET_NAME",
                "Value": f"bfd-{ENV}-server-regression",
            },
        ],
        MaxResults=100,
    )

    for data_set in data_sets["DataSetSummaries"]:
        if data_set["Name"] == f"bfd-{ENV}-server-regression" and data_set["ImportMode"] == "SPICE":
            return data_set["DataSetId"]
    raise RuntimeError("QuickSight Dataset not found.")


def refresh_data_set(client, account_id, data_set_id):
    print("Refreshing SPICE data")
    time_stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    ingestion_id = time_stamp + "-" + data_set_id

    response = client.create_ingestion(
        AwsAccountId=account_id,
        DataSetId=data_set_id,
        IngestionId=ingestion_id,
        IngestionType="INCREMENTAL_REFRESH",
    )
    ingestion_status = response["IngestionStatus"]
    print(
        f"Refresh status of dataset {data_set_id} with an ingestion id of {ingestion_id} is {ingestion_status}"
    )
