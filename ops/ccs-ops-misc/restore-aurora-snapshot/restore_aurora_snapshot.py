import boto3
import botocore.exceptions
import sys
import time

def main():
    print("--------------------------------------")
    print("| Aurora snapshot restoration script |")
    print("--------------------------------------")

    rds_client = boto3.client("rds")

    # Prompt for a cluster id until a vaild one is input
    while True:
        try:
            source_db_cluster_identifier = input("\nCluster identifier: ")
            source_db_cluster = rds_client.describe_db_clusters(DBClusterIdentifier = source_db_cluster_identifier)['DBClusters'][0]
            source_db_cluster_instances = rds_client.describe_db_instances(Filters = [{"Name":"db-cluster-id", "Values":[source_db_cluster_identifier]}])['DBInstances']
            source_db_cluster_snapshot_all = rds_client.describe_db_cluster_snapshots(DBClusterIdentifier = source_db_cluster_identifier)['DBClusterSnapshots']
            break
        except botocore.exceptions.ClientError as client_err:
            print(client_err)

    print("\nAvailable snapshots:")
    for snapshot in source_db_cluster_snapshot_all:
        print(snapshot['DBClusterSnapshotIdentifier'])

    # Prompt for a snapshot id obtained from selected cluster id until a valid one is input
    while True:
        try:
            source_db_cluster_snapshot_identifier = input("\nCluster snapshot identifier: ")
            source_db_cluster_snapshot = rds_client.describe_db_cluster_snapshots(DBClusterSnapshotIdentifier = source_db_cluster_snapshot_identifier)['DBClusterSnapshots'][0]
            break
        except botocore.exceptions.ClientError as client_err:
            print(client_err)

    restore_db_cluster_identifier = source_db_cluster_snapshot['DBClusterSnapshotIdentifier'][4:] # Remove "rds:" from snapshot identifier for use as new cluster name

    print(f"\nSource aurora cluster identifier: {source_db_cluster_identifier}")
    print(f"Source aurora cluster snapshot identifier: {source_db_cluster_snapshot_identifier}")
    print(f"Restore aurora cluster identifier: {restore_db_cluster_identifier}")

    verify_restore = input("\nAre the above details correct (y/n)? ")
    if verify_restore.lower() != "y":
        sys.exit("\nExiting script")

    # Create a new aurora cluster from the selected snapshot, copying all settings from the selected source cluster
    try:
        print(f"\nRestoring snapshot {source_db_cluster_snapshot_identifier} to aurora cluster {restore_db_cluster_identifier}")
        restore_db_cluster = rds_client.restore_db_cluster_from_snapshot(
            DBClusterIdentifier = restore_db_cluster_identifier,
            SnapshotIdentifier = source_db_cluster_snapshot_identifier,
            AvailabilityZones = source_db_cluster['AvailabilityZones'],
            CopyTagsToSnapshot = source_db_cluster['CopyTagsToSnapshot'],
            DBClusterParameterGroupName = source_db_cluster['DBClusterParameterGroup'],
            DBSubnetGroupName = source_db_cluster['DBSubnetGroup'],
            DeletionProtection = source_db_cluster['DeletionProtection'],
            EnableCloudwatchLogsExports = source_db_cluster['EnabledCloudwatchLogsExports'],
            Engine = source_db_cluster['Engine'],
            EngineMode = source_db_cluster['EngineMode'],
            EngineVersion = source_db_cluster['EngineVersion'],
            KmsKeyId = source_db_cluster['KmsKeyId'],
            Port = source_db_cluster['Port'],
            VpcSecurityGroupIds = [security_group['VpcSecurityGroupId'] for security_group in source_db_cluster['VpcSecurityGroups']]
        )
    except botocore.exceptions.ClientError as client_err:
        sys.exit(client_err)

    # Poll AWS every 15 seconds until the cluster is finished creating
    while True:
        restore_db_cluster = rds_client.describe_db_clusters(DBClusterIdentifier = restore_db_cluster_identifier)['DBClusters'][0]
        print(f"Cluster status: {restore_db_cluster['Status']}")
        if restore_db_cluster['Status'] == "available":
            break
        time.sleep(15)

    # Create the same number of cluster nodes the original source cluster had, copying all settings from each node
    try:
        source_db_instance_count = 0
        for source_db_instance in source_db_cluster_instances:
            print(f"Creating cluster instance {restore_db_cluster_identifier}-node-{source_db_instance_count}")
            restore_db_instance = rds_client.create_db_instance(
                DBClusterIdentifier = restore_db_cluster_identifier,
                DBInstanceIdentifier = f"{restore_db_cluster_identifier}-node-{source_db_instance_count}",
                AutoMinorVersionUpgrade = source_db_instance['AutoMinorVersionUpgrade'],
                AvailabilityZone = source_db_instance['AvailabilityZone'],
                DBInstanceClass = source_db_instance['DBInstanceClass'],
                DBParameterGroupName = source_db_instance['DBParameterGroups'][0]['DBParameterGroupName'],
                DBSubnetGroupName = source_db_instance['DBSubnetGroup']['DBSubnetGroupName'],
                EnablePerformanceInsights = source_db_instance['PerformanceInsightsEnabled'],
                Engine = source_db_instance['Engine'],
                LicenseModel = source_db_instance['LicenseModel'],
                MonitoringInterval = source_db_instance['MonitoringInterval'],
                MonitoringRoleArn = source_db_instance['MonitoringRoleArn'],
                OptionGroupName = source_db_instance['OptionGroupMemberships'][0]['OptionGroupName'],
                PerformanceInsightsKMSKeyId = source_db_instance['PerformanceInsightsKMSKeyId'],
                PreferredMaintenanceWindow = source_db_instance['PreferredMaintenanceWindow'],
                PromotionTier = source_db_instance['PromotionTier'],
                PubliclyAccessible = source_db_instance['PubliclyAccessible']
            )
            source_db_instance_count += 1
    except botocore.exceptions.ClientError as client_err:
        sys.exit(client_err)

    # Poll AWS every 15 seconds until all cluster nodes are finished creating
    while True:
        restore_db_instances = rds_client.describe_db_instances(Filters = [{"Name":"db-cluster-id", "Values":[restore_db_cluster_identifier]}])['DBInstances']
        print(f"Instance status: {[instance['DBInstanceStatus'] for instance in restore_db_instances]}")
        if [instance['DBInstanceStatus'] for instance in restore_db_instances].count("available") == len(restore_db_instances):
            break
        time.sleep(15)

if __name__ == "__main__":
    main()
