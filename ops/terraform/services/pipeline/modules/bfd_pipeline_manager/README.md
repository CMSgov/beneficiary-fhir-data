# `bfd_pipeline_manager` Sub-module

This submodule defines a Lambda and its corresponding infrastructure. This Lambda,
`bfd-${env}-pipeline-manager`, "manages" the BFD Pipeline Terraservice; it is invoked when new files
are created in the `env`'s corresponding S3 pipeline/ETL bucket at specific paths:

- `Synthetic/Incoming/`
- `Synthetic/Done/`
- `Incoming/`
- `Done/`

Objects uploaded to or moved to these paths in the ETL bucket will invoke this Lambda, which will
then determine if the BFD CCW Pipeline should be started or not. This depends on a few factors:

- If the file was uploaded to either of the `Incoming` folders, and there are no _ongoing_ or
  _queued_ data loads _at all_ (including other groups), the `bfd-deploy-pipeline-terraservice`
  Jenkins pipeline is started using the `bfd-job-broker` with the arguments set to _create_ the CCW
  Pipeline instance
- Else, if the file was uploaded to either of the `Done` folders, there are no files in the
  corresponding `Incoming` folder, all non-optional RIF files are present in the `Done` folder,
  _and_ there are no ongoing loads _at all_ (including other groups), the
  `bfd-deploy-pipeline-terraservice` Jenkins pipeline is started using the `bfd-job-broker` with the
  arguments set to _destroy_ the CCW Pipeline instance

In short, this Lambda indirectly invokes a Jenkins pipeline that will either _create_ or _destroy_
the CCW Pipeline instance for the current `env`, thus ensuring the CCW Pipeline instance is only
running when there is data queued up for load into that `env`. As a side effect of using Terraform
to do this management, the `pipeline` Terraservice will be _deployed_ whenever the CCW Pipeline
instance needs to start or stop.

Note that the branch from which this Lambda/module was deployed from remains consistent, such that
whatever branch this module was deployed from will be the branch variant of the
`bfd-deploy-pipeline-terraservice` that is invoked. For established environments, this will
generally remain `master`, but this allows for consistency when using ephemeral environments that
may be based upon non-`master` branches.

<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_archive"></a> [archive](#requirement\_archive) | 2.2.0 |

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_iam_policy.jenkins_sqs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.this_sqs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_lambda_function.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [aws_sqs_queue.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue) | resource |
| [archive_file.lambda_src](https://registry.terraform.io/providers/hashicorp/archive/2.2.0/docs/data-sources/file) | data source |
| [aws_kms_key.env_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_kms_key.mgmt_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_s3_bucket.etl](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/s3_bucket) | data source |
| [aws_sqs_queue.jenkins_job_queue](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sqs_queue) | data source |
<!-- END_TF_DOCS -->
