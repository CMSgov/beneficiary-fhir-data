# How to Clone an RDS Cluster with Terraform

**NOTE**: This runbook **DOES NOT** explicitly cover creating production clones for use as replacements to primary environment data stores.

Follow this runbook to create a clone of an RDS cluster for development purposes.
This runbook assumes you have sufficient access required to manipulate the remote terraform state stored in AWS S3, the state lock stored in AWS DynamoDB, and general read and write privileges to access the various AWS resources at issue, e.g. AWS SSM Parameter Store, AWS IAM, and AWS RDS among others.

## Software Requirements
- [tfenv](https://github.com/tfutils/tfenv#installation)
- [awscli](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html#getting-started-install-instructions)

## Configuring an Ephemeral Environment
All steps below assume you have the latest `master` in a local working copy of the BFD repository

### Initial Setup
Define the ephemeral environment (`ephemeralEnv`) and seed environment (`TF_VAR_ephemeral_seed_environment`) environment variables. Optionally, define `TF_VAR_ephemeral_rds_snapshot_id_override`.
- the `ephemeralEnv`
    - should be meaningful, easily traceable
    - is used as the workspace name, as `$env` in e.g. resource identifiers `bfd-${env}-fhir`
    - could be the numeric portion of the JIRA key associated with your work, e.g. BFD-2090 becomes `2090`
- `TF_VAR_ephemeral_seed_environment` seed environment is the targeted, _existing_ environment you intend to copy, e.g. `prod-sbx`
- `TF_VAR_ephemeral_rds_snapshot_id_override` is optional. If not specified, the latest snapshot associated with the seed environment's RDS cluster is selected.

**Example**
```sh
export ephemeralEnv=2090                          # environment to be named 2090
export TF_VAR_ephemeral_seed_environment=prod-sbx # clones prod-sbx environment
# export TF_VAR_rds_snapshot_id_override=         # selects latest when omitted
```

### Deploy the `base` terraservice
Define an ephemeral environment through a standard collection of AWS SSM Parameters using the base terraservice.

- [`base` terraservice README](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/ops/terraform/services/base#readme)
- <2 minutes to `terraform apply`

**Example**
```sh
# From the root of the BFD repository
cd ops/terraform/services/base
terraform init
terraform workspace new "$ephemeralEnv"
terraform apply
```

### Optional: Make any necessary adjustments to the environment definition via SSM
In addition to defaulting to the latest snapshot from the seed environment, the `base` terraservice copies select configuration values from the targeted seed environment into the ephemeral environment to create a more faithful clone.
Depending upon your use case, this may not be desirable.
At this time, use the [console to access values stored in AWS SSM Parameter Store](https://us-east-1.console.aws.amazon.com/systems-manager/parameters).

Some common values that you might consider adjusting include (but are not limited to) the following:
- `/bfd/${ephemeralEnv}/common/nonsensitive/rds_instance_class`
- `/bfd/${ephemeralEnv}/common/nonsensitive/rds_instance_count`

### Deploy the `common` terraservice
Create the common resources, including the AWS RDS Cluster and related components using the `common` terraservice.

- [`common` terraservice README](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/ops/terraform/services/common#readme)
- \>60 minutes to `terraform apply`
    - creation of the `aws_rds_cluster.aurora_cluster` resource, typically 60-80 minutes
    - creation of the `aws_rds_cluster_instance.nodes` resource(s), typically 10-20 minutes

**NOTE**: Be sure that your established AWS session has at least 120 minutes remaining before attempting to clone an RDS cluster.

**Example**
```sh
# From the root of the BFD repository
cd ops/terraform/services/common
terraform init
terraform workspace new "$ephemeralEnv"
terraform apply
```

### Using your cloned RDS Cluster

The common terraservice module produces the `rds_cluster_config` map as a terraform output.
This can be reproduced using the command `terraform output`.
`rds_cluster_config` represents a selective, _augmented_ [DescribeDBClusters RDS API response](https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/API_DescribeDBClusters.html). Most importantly, this includes the `ReaderEndpoint` and (writer) `Endpoint` fields:
- use the `ReaderEndpoint` address when connecting to the cluster for _safer_ **read-only** operations
- use the `Endpoint` address when connecting to the cluster to execute **write** operations
