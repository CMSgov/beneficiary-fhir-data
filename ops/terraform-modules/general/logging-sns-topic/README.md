# `logging-sns-topic` General Module

This Terraform Module defines an _encrypted_ SNS Topic with logging enabled including the necessary IAM resources and encrypted CloudWatch Log Groups.

This module exports the full properties of the created `aws_sns_topic` resource.

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
| <a name="input_iam_path"></a> [iam\_path](#input\_iam\_path) | Kion-compliant IAM path | `string` | n/a | yes |
| <a name="input_kms_key_arn"></a> [kms\_key\_arn](#input\_kms\_key\_arn) | ARN of the KMS Key that will be used to encrypt the topic and the logs it emits. | `string` | n/a | yes |
| <a name="input_permissions_boundary_arn"></a> [permissions\_boundary\_arn](#input\_permissions\_boundary\_arn) | Kion-compliant Permissions Boundary ARN | `string` | n/a | yes |
| <a name="input_topic_name"></a> [topic\_name](#input\_topic\_name) | SNS Topic name. Maps to `aws_sns_topic.name`. | `string` | n/a | yes |
| <a name="input_additional_topic_policy_docs"></a> [additional\_topic\_policy\_docs](#input\_additional\_topic\_policy\_docs) | List of JSON policy document strings that are combined with the default SNS Topic policy. Use the placeholder string "%TOPIC\_ARN%" in policies to represent the topic ARN. | `list(string)` | `[]` | no |
| <a name="input_application_sample_rate"></a> [application\_sample\_rate](#input\_application\_sample\_rate) | Sample rate, in percent, of application events to log. Defaults to 0% | `number` | `0` | no |
| <a name="input_firehose_sample_rate"></a> [firehose\_sample\_rate](#input\_firehose\_sample\_rate) | Sample rate, in percent, of Firehose events to log. Defaults to 0% | `number` | `0` | no |
| <a name="input_http_sample_rate"></a> [http\_sample\_rate](#input\_http\_sample\_rate) | Sample rate, in percent, of HTTP events to log. Defaults to 0% | `number` | `0` | no |
| <a name="input_lambda_sample_rate"></a> [lambda\_sample\_rate](#input\_lambda\_sample\_rate) | Sample rate, in percent, of Lambda events to log. Defaults to 0% | `number` | `0` | no |
| <a name="input_sqs_sample_rate"></a> [sqs\_sample\_rate](#input\_sqs\_sample\_rate) | Sample rate, in percent, of SQS events to log. Defaults to 0% | `number` | `0` | no |
| <a name="input_tags"></a> [tags](#input\_tags) | Additional tags to attach to resources. | `map(string)` | `{}` | no |
| <a name="input_topic_description"></a> [topic\_description](#input\_topic\_description) | SNS Topic description. Maps to `aws_sns_topic.display_name`. | `string` | `null` | no |

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
| [aws_cloudwatch_log_group.failure](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_cloudwatch_log_group.success](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_iam_policy.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_sns_topic.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic) | resource |
| [aws_sns_topic_policy.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_policy) | resource |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_iam_policy_document.combined](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.sns_assume_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.topic_template](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

| Name | Description |
|------|-------------|
| <a name="output_topic"></a> [topic](#output\_topic) | `aws_sns_topic` resource created by this module. |
<!-- END_TF_DOCS -->
