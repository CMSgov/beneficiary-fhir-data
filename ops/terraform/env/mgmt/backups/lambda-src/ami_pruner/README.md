# AMI Pruner

## Overview

This lambda is responsible for finding pruning Platinum and Application AMI's that fall outside of their designated
retention policies. It is scheduled to run once a day.

- Platinum AMI's are time based (_retain the last n days_)
- Application AMI's are version based (_retain previous n versions_)

The lambda starts by building pools of candidate AMI's based on a set of resource Filters defined near the top of the
script. It then iterates through the pools and removes any candidates we wish to retain according to our retention
policies. However, we **always** retain:

- Any AMI or EC2 Snapshot tagged with `keep` or `do-not-delete`
- Any EC2 Snapshot associated with an existing EC2 Volume
- Any AMI referenced by the $Latest or $Default version of an existing Launch Template

Any remaining candidates are then deregistered and their associated snapshots deleted.

## Running Locally

_Note: Use caution when testing locally. The script will delete AMI's and snapshots if the `DRY_RUN` environment variable is
not set to `True`._

```sh
cd lambda-src/ami-pruner
python3 -m venv .venv
. .venv/bin/activate
pip3 install -r requirements.txt
export DRY_RUN=True # for testing
export LOG_LEVEL=10 # for more verbose output
export AMI_RETENTION_POLICIES_SSM_PATH="/bfd/mgmt/common/sensitive/backups/ami"
python3 ami_pruner.py
```
