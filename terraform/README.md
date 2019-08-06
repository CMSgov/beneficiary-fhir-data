## Terraform configurations

### Overview

Infrastructure definitions are split up by environment and by service.

Shared components should be defined under the `terraform/common` directory (e.g., S3 buckets, IAM roles or policies, etc. that are used across the AWS account).

The `terraform/env` directory contains sub directories for each of the deployment environments: `mgmt`, `test`, `prod-sbx` and `prod`. Within each environment, configurations are divided by service (e.g., KMS, IAM, RDS, ELB, etc.).

Within an environment directory, each service has its own backend configuration defined for terraform state storage.

Keeping the services separated vs included in one big "global" configuration has a few advantages:

- Configurations tightly scoped to an individual service means it is less likely that changes will be inadvertently applied to other parts of the stack when running `terraform apply`.
- Separating services makes it very simple to move/migrate configurations within the terraform configuration stack using `terraform state` and `terraform import`.

When it is necessary to reference infrastructure components across modules:

- Use [terraform data sources](https://www.terraform.io/docs/configuration/data-sources.html) to lookup resources and reference them across service configurations
- If it is not possible to use data sources for lookup, stick with well-known text for naming resources, so that composing a resource name is predictable across configs (e.g., incorporate app, env and service into the resource name: `bfd-test-rds`). Use this convention to make it possible to refer to resources across configurations without need to hardcode values (e.g.,`bfd-${var.env}-rds`).

### Usage

#### Changes to existing infrastructure

1. Change directories to the environment and service you wish to change:

    ```sh
    $ cd env/test/iam
    ```

2. Make changes and run `terraform plan` to validate before applying

3. If the output of `terraform plan` looks correct, run `terraform apply`

#### Adding new configurations

1. Create a new directory within the environment dir where the new resource(s) will be used. For example:

    ```sh
    $ mkdir terraform/test/sqs
    ```

2. Add a `main.tf` and `backend.tf` file to the dir, at minimum.

    ```sh
    $ touch terraform/test/sqs/main.tf
    $ touch terraform/test/sqs/backend.tf
    ```

3. Add a terraform backend definition for state storage (i.e., in`env/test/sqs/backend.tf`):


```
terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "test/sqs/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
}
```

**NOTE:** the value of `key` (in this case:`"test/sqs/terraform.tfstate"`) includes the environment (`test`) and name/abbreviation for the service (`sqs`). Change the environment and service values to something appropriate for your use case. Be sure to use a unique `key` to avoid namespace collisions with other terraform state files.

4. Add you configurations to `main.tf`
5. Run `terraform init`
6. Run `terraform plan`, verify the plan output
7. If the plan output is correct, run `terraform apply`
