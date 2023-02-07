# `trigger_glue_crawler` Submodule

This submodule defines a Lambda and its corresponding permissions and triggers that will run
whenever an S3 event notification from the API requests S3 bucket is received. When run, the Lambda
will trigger the corresponding API requests' Glue Crawler _if_ the S3 event notification encodes a
creation event for a partition that does not yet exist in the `bfd_insights_bfd_${env}_api_requests`
Glue Table.

This submodule is referenced (and used) in the parent module `api-requests`. The S3 event
notifications that trigger the aforementioned Lambda are defined in
`ops/terraform/env/mgmt/insights/s3.tf` due to [constraints on duplicate S3 event notification resources](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_notification)

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
| [aws_iam_policy.glue](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_lambda_function.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [archive_file.lambda_src](https://registry.terraform.io/providers/hashicorp/archive/2.2.0/docs/data-sources/file) | data source |
| [aws_kms_key.cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
<!-- END_TF_DOCS -->
