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

The script will prompt for all nessecary information.

Database Failure Scenario Walkthrough
-------------------------------------
Lets say some data corruption has been introduced to the database after the latest ETL run and you want to get the FHIR servers on yesterdays copy of the data while you troubleshoot the issue.  That scenario might go like this:
- Run the restore script to create a temporary restore cluster:
  - `cd ops/ccs-ops-misc/restore-aurora-snapshot`
  - `source .venv/bin/activate`
  - `python3 restore_aurora_snapshot.py`
- Create a new branch with updated FHIR server databse config:
  - `git checkout -b myname/myhotfixbranch`
  - `cd ops/ansible/playbooks-ccs`
  - `source .venv/bin/activate`
  - `ansible-vault edit vars/(env)/group_vars/all/vault.yml`
  - `git commit -a -m 'Emergency procedure configure FHIR servers in (env) to use temporary aurora cluster'`
- Push your branch to github:
  - `git push -u origin myname/myhotfixbranch`
- There are two options at this point:
  - Run through a whole deployment pipeline using `deploy from non-master` option in Jenkins
  - Review and merge the branch into master, then detach existing instances from the ASG while checking the option to replace with new instances.  The new instances will come online with updated configuration.

  Once the issue is resolved and the original database is in a desired state, revert your changes either by deploying from master again if you deployed from a non master branch, or reverting the hotfix commit on master and recycling ASG instances as noted above.
  