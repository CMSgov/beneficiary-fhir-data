# `ccw-pipeline-alarms` Service

This subfolder contains the Terraform configuration for CloudWatch Alarms and alarming Lambdas related to the CCW Pipeline.

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

| Terraservice | Required for Established? | Required for Ephemeral? | Details |
|---|---|---|---|
| `base` | Yes | Yes | N/A |
| `config` | Yes | Yes | N/A |
| `ccw-pipeline` | Yes | Yes | N/A |
| `ccw-pipeline-metrics` | Yes | Yes | N/A |

<!-- BEGIN_TF_DOCS -->
<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 6 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_greenfield"></a> [greenfield](#input\_greenfield) | Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed. | `bool` | `true` | no |
| <a name="input_manifests_verifier_repository_override"></a> [manifests\_verifier\_repository\_override](#input\_manifests\_verifier\_repository\_override) | Overrides the ECR repository for the manifests-verifier container image. If not provided, the default will be used | `string` | `null` | no |
| <a name="input_manifests_verifier_version_override"></a> [manifests\_verifier\_version\_override](#input\_manifests\_verifier\_version\_override) | Overrides the version for manifests-verifier container image resolution. If not provided, the latest BFD version will be used | `string` | `null` | no |
| <a name="input_parent_env"></a> [parent\_env](#input\_parent\_env) | The parent environment of the current solution. Will correspond with `terraform.workspace`".<br/>Necessary on `tofu init` and `tofu workspace select` \_only\_. In all other situations, parent env<br/>will be divined from `terraform.workspace`. | `string` | `null` | no |
| <a name="input_region"></a> [region](#input\_region) | n/a | `string` | `"us-east-1"` | no |
| <a name="input_secondary_region"></a> [secondary\_region](#input\_secondary\_region) | n/a | `string` | `"us-west-2"` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_terraservice"></a> [terraservice](#module\_terraservice) | ../../terraform-modules/bfd/bfd-terraservice | n/a |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_cloudwatch_log_group.verifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_cloudwatch_metric_alarm.dataset_failed](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.errors](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_data_load_ingestion_time](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_iam_policy.verifier_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.verifier_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.verifier_rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.verifier_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.verifier_scheduler_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.verifier_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.verifier_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.verifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.verifier_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.verifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.verifier_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_function.verifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_scheduler_schedule.verifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/scheduler_schedule) | resource |
| [aws_scheduler_schedule_group.verifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/scheduler_schedule_group) | resource |
| [aws_security_group.verifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_vpc_security_group_egress_rule.verifier_allow_all_traffic_ipv4](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_egress_rule) | resource |
| [aws_vpc_security_group_ingress_rule.verifier_allow_db_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_ingress_rule) | resource |
| [aws_cloudwatch_log_group.messages](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/cloudwatch_log_group) | data source |
| [aws_ecr_image.verifier](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_ecs_cluster.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecs_cluster) | data source |
| [aws_iam_policy.lambda_vpc_execution](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_iam_policy_document.service_assume_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.verifier_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.verifier_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.verifier_rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.verifier_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.verifier_scheduler_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.verifier_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.verifier_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_rds_cluster.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/rds_cluster) | data source |
| [aws_s3_bucket.ccw_pipeline](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/s3_bucket) | data source |
| [aws_security_group.aurora_cluster](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_sns_topic.msgs_alert_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.msgs_warning_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.slos_alert_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.slos_warning_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.verifier_alert_topic](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_ssm_parameter.ccw_pipeline_bucket_name](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->