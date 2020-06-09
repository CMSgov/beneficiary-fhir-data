Restore Aurora Snapshot
-----------------------
This script will find all snapshots from an existing aurora cluster, and then create a restore cluster from the selected snapshot, restoring all settings/parameters/options from the existing cluster.

Installation:
- Ensure you have python 3.6 or greater installed.
- Ensure you have AWS credentials/profile set up for the correct environment
- Change directory to the script directory: `cd ops/ccs-ops-misc/restore-aurora-snapshot`
- Create a python virtual env: `python3 -m venv .venv`
- Activate the virtual env: `source .venv/bin/activate`
- Pip install the dependencies: `pip install -r requirements.txt`

Usage:
`python3 restore_aurora_snapshot.py`
