# BB2 Setup for BFD Insights

## Overview

The BB2 BFD-Insights AWS components are setup under this directory.

The configurations for the base components are located under `base_config`. This utilizes the original modules shared with other projects under `../../../modules/`.

The Terraform workspaces feature is used for switching between `test/impl/prod/allenv` environments. The `allenv` workspace is for components that are shared between `test/impl/prod` environments.

The BB2 specific components are setup under the following service areas:

- `services/analytics`:
  - Includes components used for reporting via QuickSight.

- `services/common`:
  - Includes components that are currently common to `test/impl/prod` environments. Uses the `allenv` workspace.

- `services/log_steam`:
  - Includes components for streaming application log events from BB2 CloudWatch through to the related Glue tables.

## Usage

To initially work with in a service area, use the following commands. For this example, the `log_stream` service is used:

```
cd services/log_stream
tfenv install 1.5.0 # install tf version needed
tfenv use 1.5.0 # select tf version to use
terraform init # locally initialize 
```

There is a `terraform.tfvars` file that contains sensitive variables. It is located under the default state path for the service. In this example, it is located in the main S3 bucket under this path:  `bfd-insights/bb2/services/log_stream/terraform.tfvars`.

Copy this file in to the current working directory.

The Terraform workspaces feature is used for switching between `test/impl/prod` environments.

The following is an example of working with in the `test` environment:

```
terraform workspace list # to see the list of envs available
terraform workspace select test # to switch to test env

terraform plan # to see the plan for changes
terraform apply # to apply changes after review
```
