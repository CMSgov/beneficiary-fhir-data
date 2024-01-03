## Terraform configurations

### Overview

Infrastructure definitions are split up by environment and by modules. The `terraform/env` directory contains subdirectories for each of the deployment environments: `mgmt`, `test`, `prod-sbx` and `prod`. Shared components should be defined under the `terraform/env/mgmt` directory (e.g., S3 buckets, IAM roles or policies, etc. that are used across the AWS account).

Modules here are split into 2 parts: stateless and resources. Stateful terraservices (i.e. those resources under `ops/terraform/services`) and Stateless deployments are done
separately to avoid the possibility of stateful destroys when deploying stateless services. Previously, stateful resources were defined side-by-side to stateless resources.
In an attempt to move away from this design, all stateful resources have been migrated to the appropriate terraservice(s), with stateless soon to follow.

When it is necessary to reference infrastructure components across master scripts, naming conventions are used. 

- Use [terraform data sources](https://www.terraform.io/docs/configuration/data-sources.html) to lookup resources and reference them across service configurations
- If it is not possible to use data sources for lookup, stick with well-known text for naming resources, so that composing a resource name is predictable across configs (e.g., incorporate app, env and service into the resource name: `bfd-test-rds`). Use this convention to make it possible to refer to resources across configurations without need to hardcode values (e.g.,`bfd-${var.env}-rds`).
