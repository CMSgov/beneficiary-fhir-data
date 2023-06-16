# BB2 Setup for BFD Insights

## Overview

The BB2 BFD-Insights AWS components are setup under this directory.

The configurations for the base components are located under [base_config](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/insights/terraform/projects/bb2/base_config). This utilizes the original modules shared with other projects under [../../../modules/](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/insights/terraform/modules).

The Terraform workspaces feature is used for switching between `test/impl/prod/allenv` environments. The `allenv` workspace is for components that are shared between `test/impl/prod` environments.

The BB2 custom components are setup under the following service areas:

- `services/log_steam`:
  - Includes components for streaming application log events from BB2 CloudWatch through to the related Glue tables. Uses `test/impl/prod` workspaces.

- `services/common`:
  - Includes components that are currently common to `test/impl/prod` enviornments. Uses the `allenv` workspace.

- `services/analytics`:
  - Includes components used for reporting via QuickSight. Uses the `allenv` workspace.

## Usage

To initially work with in a service area, use the following commands. For this example, the `log_stream` service is used:

```
cd services/log_stream
tfenv install 0.13.7 # install tf version needed
tfenv use 0.13.7 # select tf version to use
terraform init # locally initialize 
```

There is a `terraform.tfvars` file that contains sensitive variables. It is only needed for the `log_stream` service. It is located in the main S3 bucket under this path:  `bfd-insights/bb2/services/log_stream/terraform.tfvars`. Copy this file in to the `log_stream` directory.

The following is an example of working with the `test` environment workspace:

```
terraform workspace list # to see the list of envs available
terraform workspace select test # to switch to test env

terraform plan # to review the plan for changes.
terraform apply # to apply changes after review.
```