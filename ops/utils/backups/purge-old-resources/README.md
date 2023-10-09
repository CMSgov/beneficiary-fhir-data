# Purge old/aged snapshots and ami's

This is a script that can be used to manually purge resources that fall out of their retention periods as defined in [BFD Data Retention Backups and Recover Table](https://confluence.cms.gov/display/~W118/BFD+-+Data+Retention%2C+Backups%2C+and+Recovery+Table).

## Usage

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python purge.py
```

It's a good idea to do a dry run first to see what will be deleted.

```bash
DRY_RUN=true python3 purge.py # DEBUG=true too if you want to see more verbose logging
```

## Resources

### EC2 EBS Snapshots

This script will purge all unassociated EC2 EBS snapshots (created by the EC2 DLM service) older than 1 day.

Unassociated snapshots are those that are not tied to an existing EC2 Volume.

### Platinum AMI's

This script will purge all Platinum AMI's older than 365 days.

### Application AMI's

This script will purge all unassociated application ami's older than 1 day- but keeps the most recent 3 (just in case).

Unassociated snapshots are those that are not tied to an existing EC2 Volume.

### RDS Daily Snapshots

This script logs any daily automatic snapshot it finds older than 3 days. These snapshots are only logged as they are
managed by RDS and cannot be deleted.

### RDS Weekly CPM Snapshots

This script retains the last 5 CPM created "Weekly" snapshots.

### RDS Monthly CPM Snapshots

This script retains the last 2 CPM created "Monthly" snapshots.

### RDS us-west-2 replicated snapshots

This script retains the most recent CPM replicated snapshot in us-west-2 (failover region).
