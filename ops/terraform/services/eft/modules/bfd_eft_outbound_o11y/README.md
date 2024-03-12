# `bft_eft_outbound_o11y` Submodule

This Submodule contains the resources and Lambda source code for several Alarms and a Lambda that sends BFD EFT Outbound status notifications to a Slack channel configured in `base` configuration.

<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

No requirements.

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_kms_key_arn"></a> [kms\_key\_arn](#input\_kms\_key\_arn) | The current environment's default KMS key ARN | `string` | n/a | yes |
| <a name="input_outbound_bfd_sns_topic_arn"></a> [outbound\_bfd\_sns\_topic\_arn](#input\_outbound\_bfd\_sns\_topic\_arn) | The ARN of the catch-all/BFD BFD EFT Outbound SNS Topic | `string` | n/a | yes |
| <a name="input_outbound_bfd_sns_topic_name"></a> [outbound\_bfd\_sns\_topic\_name](#input\_outbound\_bfd\_sns\_topic\_name) | The name of the catch-all/BFD BFD EFT Outbound SNS Topic | `string` | n/a | yes |
| <a name="input_outbound_lambda_dlq_name"></a> [outbound\_lambda\_dlq\_name](#input\_outbound\_lambda\_dlq\_name) | Name of the BFD EFT SFTP Outbound Lambda's DLQ | `string` | n/a | yes |
| <a name="input_outbound_lambda_name"></a> [outbound\_lambda\_name](#input\_outbound\_lambda\_name) | Name of BFD EFT SFTP Outbound Lambda | `string` | n/a | yes |
| <a name="input_outbound_partner_sns_topic_names"></a> [outbound\_partner\_sns\_topic\_names](#input\_outbound\_partner\_sns\_topic\_names) | List of names of the partner-specific BFD EFT Outbound SNS Topics | `list(string)` | n/a | yes |
| <a name="input_ssm_config"></a> [ssm\_config](#input\_ssm\_config) | SSM config retrieved by parent Terraservice | `map(string)` | n/a | yes |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->



<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Outputs

No outputs.

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_cloudwatch_metric_alarm.lambda_errors](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.sns_failures](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_iam_policy.slack_notifier_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.slack_notifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.slack_notifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_function.slack_notifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.slack_notifier_allow_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [aws_sns_topic_subscription.sns_to_slack_notifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_subscription) | resource |
| [archive_file.slack_notifier_src](https://registry.terraform.io/providers/hashicorp/archive/latest/docs/data-sources/file) | data source |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_sns_topic.breach_topics](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.ok_topics](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sqs_queue.outbound_lambda_dlq](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sqs_queue) | data source |
| [aws_ssm_parameter.slack_webhook](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
<!-- END_TF_DOCS -->
