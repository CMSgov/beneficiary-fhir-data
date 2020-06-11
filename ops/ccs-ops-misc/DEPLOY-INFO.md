Deployment Info
---------------

### General Info

- Single, multibranch Jenkinsfile provides all stage orchestration.
  - App build logic located here: `apps/build.groovy`
  - Deploy logic located here: `ops/deploy-ccs.groovy`
- Platinum AMI is an optional stage controlled by `build_platinum` var.  Recommend building at least once a month.
- Only the master branch can deploy to production environments unless `deploy_prod_from_non_master` var is set to true.
- Deployment stages use locks to prevent multiple deployments from occuring at once to the same environment.
- There is a gate (manual approval stage) between TEST and PROD-SBX deployment, but not between PROD-SBX and PROD deployment.  Recommend locking the PROD environment in order to gate the PROD-SBX deployment for additional testing.
- Our merge strategy in Jenkins is to merge the current deploying branch with master, which creates a unique commit ID in Jenkins that is appended to deployed AMIs.  To look up the commit ID of an AMI, you'll need to look up the merge commit in Jenkins and then find the originating commit from the BFD repo.

### Failed Deployment Troubleshooting

ELB health checks and smoke testing embedded in the BFD startup script prevent completely broken deployments.  Additionally we use a create-before-destroy apply method in terraform when creating a new deployment autoscaling group.  If a new deployment fails these checks, it will time out the terraform deployment, leaving the original autoscaling group in tact, thus preventing an outage.

Non-outage deployment timeout is the #1 issue encountered when deploying BFD, and unfortunately can happen for many reasons:
- Startup routines in the gold image defined stage of cloud-init are failing or slow
- Startup routines in our user data defined stage of cloud-init are failing or slow
  - Can occur in our actual user data bash script
  - Can occur in ansible launch playbook
- BFD service itself fails or is slow to start
- AWS operations called by terraform are slow

For insight into the cloud-init stage look at /var/log/cloud-init.log and /var/log/cloud-init-output.log
For insight into the BFD FHIR application servers look in /usr/local/bfd-server/
In circumstances where the old deployment autoscaling group is online and no outage has occured, the best option is to roll forward and attempt another deployment with any associated fixes.

In circumstances where the old deployment autoscaling group is no longer online, but performance or functionality of the new autoscaling group is degraded, a determiniation should be made whether to perform a roll forward or a roll back:
- If a small subset of requests are affected, a roll forward (new deployment with fix) may be an acceptable strategy
- If a large subset of requests are affected, a roll forward may be less acceptable and a roll back should be attempted

There is no safe way to perform a roll back in Jenkins as all stages are dependent on variables acquired from previous stages at runtime, so the safe way to perform a roll back so to extract the terraform apply command from a previous deploy and run it locally:
- Change to the appropriate terraform environment directory. Example: `cd ops/terraform/env/test/stateless/`
- Extract and run the terraform plan command from Jenkins. Example `terraform plan -var=fhir_ami=ami-034084a3946310dc4 -var=etl_ami=ami-0dba7ffeecdd9b8be -var=ssh_key_name=bfd-test -var=git_branch_name=master -var=git_commit_id=ec883a06fbdc7fdc72daa1655eefe26643aac008 -no-color -out=tfplan`
- Extract and run the terraform apply command from Jenkins. Example: `terraform apply -no-color -input=false tfplan`
