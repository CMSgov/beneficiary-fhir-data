import boto3
import argparse
from datetime import datetime, timezone

# filter on snapshot description
FILTER_STRING = 'Created*policy-0abe3036d255e9ae0*Daily*'

# older than 21 days
NUM_DAYS = 21


def main():
    # parse arguments
    parser = argparse.ArgumentParser(
        description='Batch delete daily snapshots older than 21 days')
    parser.add_argument(
        '--region', help='AWS region (defaults to us-east-1)', required=False)
    parser.add_argument(
        '--batch-size', help='Batch size (defaults to 250)', required=False, default=250)
    parser.add_argument('--dry-run', help='Dry run',
                        action='store_true', required=False, default=False)
    args = parser.parse_args()

    # load ec2 client
    region = args.region if args.region else 'us-east-1'
    ec2 = boto3.resource('ec2', region_name=region)

    # get all snapshots that match the filter
    snapshot_iterator = ec2.snapshots.filter(
        OwnerIds=['self'],
        Filters=[{'Name': 'description', 'Values': [FILTER_STRING]}],
        MaxResults=args.batch_size,
        DryRun=False
    )

    # iterate through and delete if older than NUM_DAYS
    for snapshot in snapshot_iterator:
        # find snapshot age in days
        snapshot_age = (datetime.now(timezone.utc) - snapshot.start_time).days

        # delete if older than NUM_DAYS (eg 21)
        if snapshot_age > NUM_DAYS and snapshot.state == 'completed':
            print('Delete ({} days old): {} - {} - {} '.format(snapshot_age,
                  snapshot.snapshot_id, snapshot.start_time, snapshot.description))
            snapshot.delete(DryRun=args.dry_run)


if __name__ == '__main__':
    main()
