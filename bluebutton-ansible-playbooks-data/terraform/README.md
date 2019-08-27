## Terraform configurations

### Overview

Infrastructure definitions are split up by environment and by modules. The `terraform/env` directory contains sub directories for each of the deployment environments: `mgmt`, `test`, `prod-sbx` and `prod`. Shared components should be defined under the `terraform/env/global` directory (e.g., S3 buckets, IAM roles or policies, etc. that are used across the AWS account).

Modules have are split into 3 parts: stateless, stateful and resources. Stateless and Stateful are the master scripts for deployments. Stateful and Stateless deployments are done
seperately to avoid the possiblity of stateful destroys when deploying stateless services. Env scripts are thin layers that call into stateful and stateless master scripts. The master scripts allow Terraform to work out dependencies between resources.  

Compared to a big "global" configuration this structure a few advantages:

- Configurations tightly scoped to an individual service means it is less likely that changes will be inadvertently applied to other parts of the stack when running `terraform apply`.
- Good coding practices are used, including Don't Repeat Yourself, variable typing, 
- Separating services makes it very simple to move/migrate configurations within the terraform configuration stack using `terraform state` and `terraform import`.

When it is necessary to reference infrastructure components across master scripts, naming conventions are used. 

- Use [terraform data sources](https://www.terraform.io/docs/configuration/data-sources.html) to lookup resources and reference them across service configurations
- If it is not possible to use data sources for lookup, stick with well-known text for naming resources, so that composing a resource name is predictable across configs (e.g., incorporate app, env and service into the resource name: `bfd-test-rds`). Use this convention to make it possible to refer to resources across configurations without need to hardcode values (e.g.,`bfd-${var.env}-rds`).

### Usage

#### Deploy a new environment 

1. Only once setup the terraform backend

  ```
  cd env/global
  terraform init
  terraform apply
  ```

2. Once per environment, setup the stateful resources first

  ```
  cd env/test/stateful
  terraform init
  terrafrom apply
  ```

3. Next setup the stateless resources

  ```
  cd env/test/stateless
  terraform init
  terrafrom apply
  ```


#### Changing a configuration

1. Make the change to the resource and then apply it

  ```
  cd env/test/stateless
  terrafrom apply
  ```


#### Adding a new resource

1. Make a new resource module
2. Use the module in the stateful or stateless module
3. Apply it. Remember init is needed for new modules

  ```
  cd env/test/stateless
  terraform init
  terrafrom apply
  ```
