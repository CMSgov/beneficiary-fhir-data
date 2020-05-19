import boto3
import botocore.exceptions
import sys

def main():
    print("--------------------------------------")
    print("| Aurora snapshot restoration script |")
    print("--------------------------------------")

    rds_client = boto3.client("rds")

    while True:
        try:
            source_db_cluster_identifier = input("\nCluster identifier: ")
            source_db_cluster = rds_client.describe_db_clusters(DBClusterIdentifier = source_db_cluster_identifier)["DBClusters"][0]
            source_db_cluster_snapshot_all = rds_client.describe_db_cluster_snapshots(DBClusterIdentifier = source_db_cluster_identifier)["DBClusterSnapshots"]
            break
        except botocore.exceptions.ClientError:
            print("Invalid cluster identifier, try again")

    print("\nAvailable snapshots:")
    for snapshot in source_db_cluster_snapshot_all:
        print(snapshot["DBClusterSnapshotIdentifier"])

    while True:
        try:
            source_db_cluster_snapshot_identifier = input("\nCluster snapshot identifier: ")
            source_db_cluster_snapshot = rds_client.describe_db_cluster_snapshots(DBClusterSnapshotIdentifier = source_db_cluster_snapshot_identifier)["DBClusterSnapshots"][0]
            restore_db_cluster_identifier = source_db_cluster_snapshot["DBClusterSnapshotIdentifier"][4:] # Remove "rds:" from snapshot identifier for use as new cluster name
            break
        except botocore.exceptions.ClientError:
            print("Invalid cluster snapshot identifier, try again")

    print(f"\nSource aurora cluster identifier: {source_db_cluster_identifier}")
    print(f"Source aurora cluster snapshot identifier: {source_db_cluster_snapshot_identifier}")
    print(f"Restore aurora cluster identifier: {restore_db_cluster_identifier}")

    verify_restore = input("\nAre the above details correct (y/n)? ")
    if verify_restore.lower() != "y":
        sys.exit("\nExiting script")
    else:
        print("\nProceeding with restore")

    # try:
    #     restore_db_cluster = rds_client.restore_db_cluster_from_snapshot(

    #     )
    # except botocore.exceptions.ClientError as client_err:
    #     sys.exit(client_err)

if __name__ == "__main__":
    main()
