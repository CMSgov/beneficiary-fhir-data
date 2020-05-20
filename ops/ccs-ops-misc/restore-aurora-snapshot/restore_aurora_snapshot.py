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
            DBClusterIdentifier = restore_db_cluster_identifier,
            SnapshotIdentifier = source_db_cluster_snapshot_identifier,
            AvailabilityZones = source_db_cluster["AvailabilityZones"],
            CopyTagsToSnapshot = source_db_cluster["CopyTagsToSnapshot"],
            DBClusterParameterGroupName = source_db_cluster["DBClusterParameterGroup"],
            DBSubnetGroupName = source_db_cluster["DBSubnetGroup"],
            DeletionProtection = source_db_cluster["DeletionProtection"],
            EnableCloudwatchLogsExports = source_db_cluster["EnabledCloudwatchLogsExports"],
            Engine = source_db_cluster["Engine"],
            EngineMode = source_db_cluster["EngineMode"],
            EngineVersion = source_db_cluster["EngineVersion"],
            KmsKeyId = source_db_cluster["KmsKeyId"],
            Port = source_db_cluster["Port"],
            VpcSecurityGroupIds = [security_group["VpcSecurityGroupId"] for security_group in source_db_cluster["VpcSecurityGroups"]]
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
        print("\nCreating cluster instances...")
        for source_db_instance in source_db_cluster_instances:
            restore_db_instance = rds_client.create_db_instance(
                DBClusterIdentifier = restore_db_cluster_identifier,
                DBInstanceIdentifier = ,
                DBInstanceClass = source_db_instance["DBInstanceClass"],
                Engine = source_db_instance["Engine"],
                AvailabilityZone = source_db_instance["AvailabilityZone"],
                DBSubnetGroupName = source_db_instance["DBSubnetGroup"]["DBSubnetGroupName"],
                PreferredMaintenanceWindow = source_db_instance["PreferredMaintenanceWindow"],
                DBParameterGroupName = source_db_instance["DBParameterGroups"][0]["DBParameterGroupName"],
                Port = source_db_instance["Port"],
                AutoMinorVersionUpgrade = source_db_instance["AutoMinorVersionUpgrade"],
                LicenseModel = source_db_instance["LicenseModel"],
                OptionGroupName = source_db_instance["OptionGroupMemberships"][0]["OptionGroupName"],
                PubliclyAccessible = source_db_instance["PubliclyAccessible"],
                MonitoringInterval = source_db_instance["MonitoringInterval"],
                MonitoringRoleArn = source_db_instance["MonitoringRoleArn"],
                EnablePerformanceInsights = source_db_instance["PerformanceInsightsEnabled"],
                PerformanceInsightsKMSKeyId = source_db_instance["PerformanceInsightsKMSKeyId"],
                EnableCloudwatchLogsExports = source_db_instance["EnabledCloudwatchLogsExports"],
            )
    except botocore.exceptions.ClientError as client_err:
        sys.exit(client_err)

if __name__ == "__main__":
    main()
