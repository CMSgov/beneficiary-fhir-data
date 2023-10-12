# Purge old/aged snapshots and ami's

This is a script that can be used to manually purge resources that fall out of their retention periods as defined in [BFD Data Retention Backups and Recover Table](https://confluence.cms.gov/display/~W118/BFD+-+Data+Retention%2C+Backups%2C+and+Recovery+Table).

The script does the following:

- Ensures we are only retaining the last N daily/weekly CPM generated RDS snapshots
- Ensures we are only retaining the last N us-west-2 replicated RDS snapshots
- Deletes Platinum AMI's older than 365 days
- Deletes all unassociated Application AMI's (fhir, migrator, etc) older than 1 day
- Deletes all unassociated EC2 EBS snapshots older than 1 day

Notes:

- app/platinum ami's are deregistered and any ebs snapshots associated with the ami is deleted
- some safety logic is built in to the script which may prevent pruning in certain circumstances. For example, we don't delete monthly RDS snapshots if there are not enough weeklies

## Usage

Ensure you have the proper AWS credentials in your environment and a recent version of python3 installed. Then:

```bash
cd ops/utils/backups/purge-old-resources
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python purge.py
```

It's a good idea to do a dry run first to see what will be deleted.

```bash
DRY_RUN=true python3 purge.py # add a DEBUG=true too if you want to see more verbose logging
```
