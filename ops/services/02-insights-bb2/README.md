# BB2 Setup for BFD Insights

## Overview

The BB2 BFD-Insights AWS components are setup under this directory.

The configurations for the base components are located under `base_config`. This utilizes the original modules shared with other projects under `../../../modules/`.

The Terraform workspaces feature is used for switching between `test/impl/prod/allenv` environments. The `allenv` workspace is for components that are shared between `test/impl/prod` environments.

The BB2 specific components are setup under the following service areas:

- `services/analytics`:
  - Includes components for the lambda and event scheduler that update the intermediate reporting tables via Athena and for usage in QuickSight.
    - For more details, see this module [README.md](services/analytics/modules/lambda/update_athena_metric_tables/README.md).
    - When adding new metrics,except for calculated fields, this service would need to be updated.

- `services/common`:
  - Includes components that are currently common to `test/impl/prod` environments. Uses the `allenv` workspace.

- `services/log_steam`:
  - Includes components for streaming application log events from BB2 CloudWatch through to the related Glue tables.
  - Requires a `terraform.tfvars` file for sensitive vars.
    - File is located in CMS BOX under Engineering -> BFD-Insights-Terraform.

- `services/quicksight`:
  - Includes components for updating Quicksight DataSets & Analyses.
    - The `test` workspace is used for development and testing.
    - The `prod` workspace is used for the production versions of analyses that are used to publish a corresponding dashboard.
    - Requires a `terraform.tfvars` file for sensitive vars.
      - File is located in CMS BOX under Engineering -> BFD-Insights-Terraform.
  - When adding new metrics,including calculated fields, this service would need to be updated.

## Terraform Usage

To initially work with in a service area, use the following commands. For this example, the `log_stream` service is used:

```
cd services/log_stream
tfenv install 1.6.5 # install tf version needed
tfenv use 1.6.5 # select tf version to use
terraform init # locally initialize 
```

There are `terraform.tfvars` files that contain sensitive variables. They are located in BB2 CMS Box.

Copy the file related to the target service in to the current working directory, if the service requires one.

The Terraform workspaces feature is used for switching between `test/impl/prod/allenv` environments.

The following is an example of working with in the `test` environment for this service:

```
terraform workspace list # to see the list of workspaces available
terraform workspace select test # to switch to test workspace

terraform plan # to see the plan for changes
terraform apply # to apply changes after review

terraform plan # Review the changes still showing after the APPLY.
```

**WARNING**: The plans **must only** show resources to be changed!  If you are seeing any to DELETE or CREATE, something is wrong. For example, being in the default workspace will show all resources to be created.

## Special Note About Quicksight Analyses 

Normally when terraforming resources, the `terraform plan` after a `terraform apply`, will show the following message: "No changes. Your infrastructure matches the configuration."

However, due to several issues when terraforming Quicksight Analyses, these will always show some types of expected changes. These are described in the following README document: [services/quicksight/README.md](services/quicksight/README.md).


## Documentation for Athena & Quicksight Metrics

There is more detailed documention located under the `update_athena_metric_tables` lambda module directory. This contains documentation related to updating the intermediate tables utilized by the datasets. The datasets are used by the analyses that are used to publish the dashboards.

For more details, see this [services/analytics/modules/lambda/update_athena_metric_tables/README.md](services/analytics/modules/lambda/update_athena_metric_tables/README.md).
