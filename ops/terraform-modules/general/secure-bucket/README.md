# `secure-bucket` General Module

This Terraform Module defines a _secure-by-default_ S3 Bucket using a Customer-Managed Key (CMK) as a Bucket Key along with various Bucket Policies restricting insecure actions.

This module exports the full properties of the created `aws_s3_bucket` resource for use in other S3 resources, e.g. `aws_s3_bucket_notifications`, `aws_s3_bucket_lifecycle_configuration`, etc.

<!-- BEGIN_TF_DOCS -->
<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | ~> 1.10.0 |
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~>6 |
| <a name="requirement_external"></a> [external](#requirement\_external) | ~>2 |
| <a name="requirement_http"></a> [http](#requirement\_http) | ~>3 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_bucket_kms_key_arn"></a> [bucket\_kms\_key\_arn](#input\_bucket\_kms\_key\_arn) | ARN of the KMS Key that will be used as the Bucket Key. Objects must be uploaded using this key exclusively. | `string` | n/a | yes |
| <a name="input_additional_bucket_policy_docs"></a> [additional\_bucket\_policy\_docs](#input\_additional\_bucket\_policy\_docs) | List of JSON policy document strings that are combined with the default secure Bucket policy. | `list(string)` | `[]` | no |
| <a name="input_bucket_name"></a> [bucket\_name](#input\_bucket\_name) | Full Bucket name. Maps to `aws_s3_bucket.bucket`. Conflicts with 'bucket\_prefix'. | `string` | `null` | no |
| <a name="input_bucket_prefix"></a> [bucket\_prefix](#input\_bucket\_prefix) | Bucket name prefix. Maps to `aws_s3_bucket.bucket_prefix`. Conflicts with 'bucket\_name'. | `string` | `null` | no |
| <a name="input_force_destroy"></a> [force\_destroy](#input\_force\_destroy) | Enable bucket force destroy. Maps to `aws_s3_bucket.force_destroy`. | `bool` | `false` | no |
| <a name="input_ssm_param_name"></a> [ssm\_param\_name](#input\_ssm\_param\_name) | Name of SSM parameter storing the name of the created bucket. Useful in contexts where the bucket<br/>is created using a bucket prefix instead of a static name. If null, no parameter is created.<br/>Defaults to null. | `string` | `null` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | Additional tags to attach to bucket resource. | `map(string)` | `{}` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

No modules.

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_s3_bucket.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket) | resource |
| [aws_s3_bucket_policy.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_policy) | resource |
| [aws_s3_bucket_public_access_block.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_public_access_block) | resource |
| [aws_s3_bucket_server_side_encryption_configuration.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_server_side_encryption_configuration) | resource |
| [aws_ssm_parameter.bucket_name](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_iam_policy_document.combined](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

| Name | Description |
|------|-------------|
| <a name="output_bucket"></a> [bucket](#output\_bucket) | `aws_s3_bucket` resource created by this module. |
| <a name="output_ssm_bucket_name"></a> [ssm\_bucket\_name](#output\_ssm\_bucket\_name) | `aws_ssm_parameter` resource created by this module. Is null if var.ssm\_param\_name is unspecified/null. |
<!-- END_TF_DOCS -->
