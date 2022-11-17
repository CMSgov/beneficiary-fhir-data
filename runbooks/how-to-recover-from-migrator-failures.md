# How to Recover from `migrator` Failures

Follow this runbook if the the deployment of `migrator` fails pre-deployment checks **or** results in a non-zero (0) exit status. The `migrator` only runs during the course of a deployment, and this runbook covers troubleshooting steps for application and non-application failures that we may encounter during a deployment.

**Table of Contents**

- [Failures](#failures)
    - [Undeployable State](#undeployable-state)
    - [Invalid App Configuration (1)](#invalid-app-configuration-1)
    - [Migration Failed (2)](#migration-failed-2)
    - [Validation Failed (3)](#validation-failed-3)
- [References](#references)

## Failures

### Undeployable State

**NOTE**: _The failure modes impacting the deployability of the migrator are numerous and can include problems with AWS IAM permissions, network access, other outages in the AWS environment, availability of the Cloudbees Jenkins deployment services, and lingering issues in the logic serving the various Jenkinsfile resources, and supporting global libraries. The troubleshooting steps below only deals with the most common error where errant messages persist in the SQS queue used for signaling between the BFD environments and Cloudbees Jenkins. For other areas of investigation, please see [the References section](#references) for a list of links to resources that might assist in investigating errors not covered here._

The CI process failed to even attempt deployment of the migrator, resulting in message like one or more of the following:
> Queue bfd-${env}-migrator has messages. Is there an old bfd-db-migrator instance running? Migrator deployment cannot proceed.

> Halting Migrator Deployment. Check the SQS Queue bfd-${env}-migrator.

Causes might include:
- resources from a previously failed deployment that wasn't _cleaned up_
- _untidy_ development practices involving a migrator deployment
- other out-of-band deployment activity

**NOTE**: **Operators must verify that there is no ongoing migration under way before troubleshooting or attempting re-deployment.** At the time of this writing, the EC2 instance should only exist for as long as it takes to execute the necessary migration(s) **unless** there is a failure. If a bfd-migrator EC2 instance exists at the time of deployment or persists _after_ the deployment, this is anomalous.

#### Performance Steps

<details><summary>More...</summary>

1. Identify the problematic AWS SQS Queue from the relevant Jenkins log message, e.g. `bfd-${env}-migrator` 
2. Fetch messages if they exist
    - <details><summary>through the console</summary>

        1. navigate to the [SQS panel](https://us-east-1.console.aws.amazon.com/sqs/v2/home?region=us-east-1#/queues)
        2. note the available messages count from this interface
        3. if there are more than 0 messages
            1. select the appropriate SQS queue `bfd-${env}-migrator`, e.g. `bfd-test-migrator`
            2. from the specific queue's interface, select `Send and receive messages`
            3. select `Poll for Messages`

        </details>

    - <details><summary>through the cli</summary>

        If the following returns nothing, there are no messages available:

        ```sh
        queue_name=bfd-test-migrator # CHANGE AS NECESSARY
        url="$(aws sqs get-queue-url --queue-name "$queue_name" --region us-east-1 --output text)"
        aws sqs receive-message --region us-east-1 --queue-url "$url" 
        ```

        </details>

3. Read messages
    - if there is a message that contains a normal exit with a zero (0) return code, continue with step 4
    - if there are messages that contains a non-zero return code, continue to troubleshoot with the instructions for the corresponding code:
        - [for return code 1](#invalid-app-configuration-1)
        - [for return code 2](#migration-failed-2)
        - [for return code 3](#validation-failed-3)
    - if there are no messages containing return codes **AND** a migrator instance exists, complete the following steps and continue to step 4:
        - <details><summary>Ensure the migrator is not running</summary>

            1. Identify the bfd-migrator instance for the environment
            2. Connect to the EC2 instance via SSH
            3. Inspect output of `sudo systemctl status bfd-db-migrator`
            4. Ensure there is no active migration `sudo systemctl stop bfd-db-migrator`
            5. _Optionally_ inspect logs: `view /bluebutton-data-pipeline/bluebutton-data-pipeline.log`

            </details>

4. Purge the AWS SQS Queue of **all** messages

    - <details><summary>through the console</summary>
   
        1. navigate to the [SQS panel](https://us-east-1.console.aws.amazon.com/sqs/v2/home?region=us-east-1#/queues)
        2. select the appropriate SQS queue `bfd-<env>-migrator`, e.g. `bfd-test-migrator`
        3. next, select the `Purge` action
        4. in the dialogue, enter `purge` and confirm with by selecting `Purge`

        </details>

    - <details><summary>through the cli</summary>
   
        ```sh
        queue_name=bfd-test-migrator # CHANGE AS NECESSARY
        url="$(aws sqs get-queue-url --queue-name "$queue_name" --region us-east-1 --output text)"
        aws sqs purge-queue --region us-east-1 --queue-url "$url" 
        ```

        </details>

5. Attempt to re-deploy from Jenkins
6. If failures persist
   - further scrutinize the Jenkins logs for errors leading up to the migrator deployment
   - verify AWS IAM permissions for the `cloudbees-jenkins` role and inspect the [AWS CloudTrails Errors Dashboard][splunk-cloudtrail-errors] in splunk
   - ensure AWS isn't reporting any open issues in the [health dashboard](https://health.aws.amazon.com/health/home#/account/dashboard/open-issues)

</details>

### Invalid App Configuration (1)

The application failed to resolve full, error-free configuration for the given environment and provides the message:
> Migrator completed with exit status 1

Common causes might include:
- permissions, connectivity problems in resolution of AWS SSM Parameter Store configuration values 
- permissions, connectivity problems in accessing the AWS Key Management Service (KMS) Customer Managed Key (CMK)
- erroneous or missing configuration from the `base` module
- erroneous [`ansible`][migrator-ansible-role] or [`terraform`][migrator-terraform] templates

#### Performance Steps

<details><summary>More...</summary>

1. Identify the bfd-migrator instance for the environment
2. Connect to the EC2 instance via SSH
3. Inspect the contents of the cloud-init `/var/lib/cloud/instance/user-data.txt` script
4. Attempt to re-apply: `sudo bash /var/lib/cloud/instance/user-data.txt`; Troubleshoot.
    - Search for errors in `/var/log/cloud-init.log` and `/var/log/cloud-init-output.log`
    - Inspect the resultant `/opt/bfd-db-migrator/bfd-db-migrator-service.sh` for format errors
    - Verify AWS SSM Parameter Store Access
        - Evaluate permission: `aws sts get-caller-identity`
        - Verify: `aws ssm get-parameters-by-path --path /bfd/${env}/pipeline`
        - Verify: `aws ssm get-parameters-by-path --path /bfd/${env}/common/nonsensitive`
    - Inspect resultant cloud-init derived extra variables files:
        - /beneficiary-fhir-data/ops/ansible/playbooks-ccs/common_vars.json
        - /beneficiary-fhir-data/ops/ansible/playbooks-ccs/extra_vars.json
        - /beneficiary-fhir-data/ops/ansible/playbooks-ccs/pipeline_vars.json
    - Inspect output of `sudo systemctl status bfd-db-migrator` and `sudo systemctl migrator-monitor` for errors
5. Make adjustments as necessary:
    - AWS IAM Policies attached to `bfd-<env>-migrator`
    - Likely Ansible Configuration Files:
        - /beneficiary-fhir-data/ops/ansible/playbooks-ccs/launch_bfd-db-migrator.yml
        - /beneficiary-fhir-data/ops/ansible/roles/bfd-db-migrator/templates/bfd-db-migrator.service.j2
        - /beneficiary-fhir-data/ops/ansible/roles/bfd-db-migrator/templates/bfd-db-migrator-service.sh.j2
        - /beneficiary-fhir-data/ops/ansible/roles/bfd-db-migrator/templates/migrator-monitor.service.j2
        - /beneficiary-fhir-data/ops/ansible/roles/bfd-db-migrator/templates/migrator-monitor.sh.j2
6. Record all adjustments in a new branch and seek PR feedback from BFD engineers as necessary.
7. Re-deploy with latest changes from Jenkins

</details>

### Migration Failed (2)

The application failed to apply the required flyway migration script or scripts to the appropriate database and provides the following message:
> Migrator completed with exit status 2

Causes might include:
- development version of a migration was _inappropriately_ applied to an environment's database
- other out-of-band changes were applied to the RDS cluster resulting in mismatching schema version or schema version hashes
- tests failed to identify an error only impacting AWS Aurora PostgreSQL, but
  - succeed against HSQLDB (uncommon)
  - success against non-Aurora PostgreSQL (rare)

#### Performance Steps

<details><summary>More...</summary>

1. Identify the bfd-migrator instance for the environment
2. Identify the error message(s) from the logs
    - <details><summary>through the instance</summary>

        1. navigate to the appropriate CloudWatch Panel

            - [test][migrator-cw-logs-test]
            - [prod-sbx][migrator-cw-logs-prod-sbx]
            - [prod][migrator-cw-logs-prod]
            - [all migrator log groups][migrator-cw-logs-all]

        2. select the appropriate instance (typically the top instance)

        </summary>

    - <details><summary>through the console</summary>

        1. SSH to the instance 
        2. `view /bluebutton-data-pipeline/bluebutton-data-pipeline.log`

        </details>

3. Support migration author in resolving the migration error with PR feedback, deployment support, etc
4. Purge SQS Message Queue as necessary before the next deployment (purge instructions [found here](#undeployable-state))

</details>

### Validation Failed (3)

**NOTE**: _This should be exceedingly rare. If you happen to experience this, please expand this section with any helpful details that assisted in your remediation efforts._

The application failed to validate the Hibernate models against the currently applied schema version and provides the following message:
> Migrator completed with exit status 3

#### Performance Steps

<details><summary>More...</summary>

1. Identify the bfd-migrator instance for the environment
2. Identify the error message(s) from the logs
    - <details><summary>through the instance</summary>

        1. navigate to the appropriate CloudWatch Panel

            - [test][migrator-cw-logs-test]
            - [prod-sbx][migrator-cw-logs-prod-sbx]
            - [prod][migrator-cw-logs-prod]
            - [all migrator log groups][migrator-cw-logs-all]

        2. select the appropriate instance (typically the top instance)

        </details>

    - <details><summary>through the console</summary>

        1. Connect to the EC2 instance via SSH
        2. `view /bluebutton-data-pipeline/bluebutton-data-pipeline.log`

        </details>

3. Support migration author in resolving the validation error with PR feedback, deployment support, etc
4. Purge SQS Message Queue as necessary before the next deployment (purge instructions [found here](#undeployable-state))

</details>

## References
- [Original Migrator RFC Document][migrator-rfc]
- [Migrator Java Artifact Source Code][migrator-java-source]
- [Monolithic Packer Manifest][migrator-packer-manifest]
- [Migrator Terraform][migrator-terraform]
    - [Migrator cloud-init user-data Template][migrator-cloud-init]
    - [Migrator-Specific Jenkins Deployment Logic][migrator-jenkins]
- [Migrator Ansible Role][migrator-ansible-role]
- [Migrator Ansible Playbook][migrator-ansible-playbook]
- [Jenkins Global Pipeline Libraries][jenkins-global-pipeline-libraries]
- [Migrator Cloud Watch Logs][migrator-cw-logs-all]
    - [test][migrator-cw-logs-test]
    - [prod-sbx][migrator-cw-logs-prod-sbx]
    - [prod][migrator-cw-logs-prod]
- [Splunk CloudTrail Errors][splunk-cloudtrail-errors] _(CMS VPN Required)_

<!-- links in reference style for those destinations that may change over time -->
[migrator-rfc]: https://github.com/CMSgov/beneficiary-fhir-data/blob/master/rfcs/0011-separate-flyway-from-pipeline.md "Original Migrator RFC Document"
[migrator-java-source]: https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-db-migrator "Migrator Java Artifact Source Code"
[migrator-packer-manifest]: https://github.com/CMSgov/beneficiary-fhir-data/blob/master/ops/packer/build_bfd-all.json "Monolithic Packer Manifest"
[migrator-terraform]: https://github.com/CMSgov/beneficiary-fhir-data/tree/master/ops/terraform/services/migrator "Migrator Terraform"
[migrator-cloud-init]: https://github.com/CMSgov/beneficiary-fhir-data/blob/master/ops/terraform/services/migrator/user-data.tftpl "Migrator cloud-init user-data Template"
[migrator-jenkins]: https://github.com/CMSgov/beneficiary-fhir-data/blob/master/ops/terraform/services/migrator/deploy.groovy "Migrator-Specific Jenkins Deployment Logic"
[migrator-ansible-role]: https://github.com/CMSgov/beneficiary-fhir-data/tree/master/ops/ansible/roles/bfd-db-migrator "Migrator Ansible Role"
[migrator-ansible-playbook]: https://github.com/CMSgov/beneficiary-fhir-data/blob/master/ops/ansible/playbooks-ccs/launch_bfd-db-migrator.yml "Migrator Ansible Playbook"
[jenkins-global-pipeline-libraries]: https://github.com/CMSgov/beneficiary-fhir-data/tree/master/ops/jenkins/global-pipeline-libraries "Jenkins Global Pipeline Libraries"

[migrator-cw-logs-test]: https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Fbfd$252Ftest$252Fbfd-db-migrator$252Fmigrator-log.json "TEST Migrator CloudWatch Logs"
[migrator-cw-logs-prod-sbx]: https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Fbfd$252Ftest$252Fbfd-db-migrator$252Fmigrator-log.json "PROD-SBX Migrator CloudWatch Logs"
[migrator-cw-logs-prod]: https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Fbfd$252Ftest$252Fbfd-db-migrator$252Fmigrator-log.json "PROD Migrator CloudWatch Logs"
[migrator-cw-logs-all]: https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups$3FlogGroupNameFilter$3Dmigrator "ALL Migrator CloudWatch Logs"

[splunk-cloudtrail-errors]: https://splunk.cloud.cms.gov/en-US/app/cms_oeda_bfd_landing_page/cloudtrailerrors0 "Splunk CloudTrail Errors"
