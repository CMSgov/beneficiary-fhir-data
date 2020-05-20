import boto3
import botocore.exceptions
import sys
import time

def main():
    print("--------------------------------------")
    print("| Aurora snapshot restoration script |")
    print("--------------------------------------")

    rds_client = boto3.client("rds")

    while True:
        try:
            source_db_cluster_identifier = input("\nCluster identifier: ")
            source_db_cluster = rds_client.describe_db_clusters(DBClusterIdentifier = source_db_cluster_identifier)["DBClusters"][0]
            source_db_cluster_instances = rds_client.describe_db_instances(Filters = [{"Name":"db-cluster-id", "Values":[source_db_cluster_identifier]}])["DBInstances"]
            source_db_cluster_snapshot_all = rds_client.describe_db_cluster_snapshots(DBClusterIdentifier = source_db_cluster_identifier)["DBClusterSnapshots"]
            break
        except botocore.exceptions.ClientError as client_err:
            print(client_err)

    print("\nAvailable snapshots:")
    for snapshot in source_db_cluster_snapshot_all:
        print(snapshot["DBClusterSnapshotIdentifier"])

    while True:
        try:
            source_db_cluster_snapshot_identifier = input("\nCluster snapshot identifier: ")
            source_db_cluster_snapshot = rds_client.describe_db_cluster_snapshots(DBClusterSnapshotIdentifier = source_db_cluster_snapshot_identifier)["DBClusterSnapshots"][0]
            restore_db_cluster_identifier = source_db_cluster_snapshot["DBClusterSnapshotIdentifier"][4:] # Remove "rds:" from snapshot identifier for use as new cluster name
            break
        except botocore.exceptions.ClientError as client_err:
            print(client_err)

    print(f"\nSource aurora cluster identifier: {source_db_cluster_identifier}")
    print(f"Source aurora cluster snapshot identifier: {source_db_cluster_snapshot_identifier}")
    print(f"Restore aurora cluster identifier: {restore_db_cluster_identifier}")

    verify_restore = input("\nAre the above details correct (y/n)? ")
    if verify_restore.lower() != "y":
        sys.exit("\nExiting script")

    try:
        print("\nRestoring aurora cluster...")
        restore_db_cluster = rds_client.restore_db_cluster_from_snapshot(
            AvailabilityZones = source_db_cluster["AvailabilityZones"],
            DBClusterIdentifier = restore_db_cluster_identifier,
            SnapshotIdentifier = source_db_cluster_snapshot_identifier,
            Engine = source_db_cluster["Engine"],
            EngineVersion = source_db_cluster["EngineVersion"],
            Port = source_db_cluster["Port"],
            DBSubnetGroupName = source_db_cluster["DBSubnetGroup"],
            VpcSecurityGroupIds = [security_group["VpcSecurityGroupId"] for security_group in source_db_cluster["VpcSecurityGroups"]],
            KmsKeyId = source_db_cluster["KmsKeyId"],
            EnableCloudwatchLogsExports = source_db_cluster["EnabledCloudwatchLogsExports"],
            EngineMode = source_db_cluster["EngineMode"],
            DBClusterParameterGroupName = source_db_cluster["DBClusterParameterGroup"],
            DeletionProtection = source_db_cluster["DeletionProtection"],
            CopyTagsToSnapshot = source_db_cluster["CopyTagsToSnapshot"]
        )
    except botocore.exceptions.ClientError as client_err:
        sys.exit(client_err)

    while True:
        restore_db_cluster = rds_client.describe_db_clusters(DBClusterIdentifier = restore_db_cluster_identifier)["DBClusters"][0]
        if restore_db_cluster["Status"].lower() == "available":
            break
        time.sleep(30)
        print(".", end = "")

    try:
        print("Restore of aurora cluster complete!")
        print("Creating cluster instances...")

    except botocore.exceptions.ClientError as client_err:
        sys.exit(client_err)

if __name__ == "__main__":
    main()
