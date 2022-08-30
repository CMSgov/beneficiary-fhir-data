# Delete daily snapshots older than 21 days

Our DLM lifecycle policy snapshots existing volumes tagged with `snapshot:true` daily and is *supposed* to delete snapshots older than 21 days. 

However, for reasons currently unknown (maybe warm pools?), only a small fraction of the snapshots have actually been getting deleted, leaving a very large surplus of old snapshots that need to be manually removed.

**This script will find and delete these snapshots.**

## Snapshots

Our DLM policy creates snapshots of all active Volumes daily. These snapshots contain the following description:

```
Created for policy: policy-0abe3036d255e9ae0 schedule: Daily snapshots of EC2 volumes which are marked for snapshots
```

This script finds all snapshots older than 21 days old that match the following wildcard string:

  `Created*policy-0abe3036d255e9ae0*Daily*`

It does this in batches and may take a very long time (at least initially) to complete.

## Instructions

!!! *The credentials used to run this script must have sufficient perms to find and delete EC2 Snapshots.*

1. Install

```sh
python3 -m venv .venv
. .venv/bin/activate
pip3 install -r requirements.txt
```

2. Run the script (Try a `--dry-run` first!)

```sh
# do a quick dry run to test things out (ctl+c to stop)
python3 delete-aged-snapshots.py --dry-run

# if all looks ok
python3 delete-aged-snapshots.py
```

### References

https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/ec2.html#snapshot
