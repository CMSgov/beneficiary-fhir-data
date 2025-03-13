<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_archive"></a> [archive](#requirement\_archive) | 2.2.0 |

## Providers

| Name | Version |
|------|---------|
| <a name="provider_archive"></a> [archive](#provider\_archive) | 2.2.0 |
| <a name="provider_aws"></a> [aws](#provider\_aws) | n/a |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [aws_autoscaling_notification.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_notification) | resource |
| [aws_iam_policy.autoscaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.cloudwatch](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_lambda_function.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [aws_sns_topic.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic) | resource |
| [aws_sns_topic_subscription.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_subscription) | resource |
| [archive_file.this](https://registry.terraform.io/providers/hashicorp/archive/2.2.0/docs/data-sources/file) | data source |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_iam_policy.permissions_boundary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_kms_key.mgmt_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_sns_topic.alarms_action_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.alarms_ok_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_alarm_action_sns_override"></a> [alarm\_action\_sns\_override](#input\_alarm\_action\_sns\_override) | Overrides the SNS topic that the alarms created by this module's Lambda will post to when transitioning to the ALARM state | `string` | `null` | no |
| <a name="input_alarm_ok_sns_override"></a> [alarm\_ok\_sns\_override](#input\_alarm\_ok\_sns\_override) | Overrides the SNS topic that the alarms created by this module's Lambda will post to when transitioning to the OK state | `string` | `null` | no |
| <a name="input_asg_names"></a> [asg\_names](#input\_asg\_names) | Names of the ASG to attach notifications to | `list(string)` | `null` | no |
| <a name="input_cloudtamer_iam_path"></a> [cloudtamer\_iam\_path](#input\_cloudtamer\_iam\_path) | IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts | `string` | `"/delegatedadmin/developer/"` | no |

## Outputs

No outputs.
<!-- END_TF_DOCS -->
