# `ccw-pipeline` Service

This subfolder contains the Terraform configuration (Task Definitions, ECS Service definitions, etc.) for the BFD CCW Pipeline ECS Service and all related infrastructure.

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

| Terraservice | Required for Established? | Required for Ephemeral? | Details |
|---|---|---|---|
| `base` | Yes | Yes | N/A |
| `config` | Yes | Yes | N/A |
| `cluster` | Yes | Yes | N/A |
| `database` | Yes | No | This Terraservice may be skipped in **ephemeral environments** by specifying the `db_environment_override` in ephemeral environments to an existing cluster in the same seed environment |
| `migrator` | Yes | No | This Terraservice may be skipped in **ephemeral environments**, if is known that there are no new migrations to run |

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
| <a name="input_ccw_runner_repository_override"></a> [ccw\_runner\_repository\_override](#input\_ccw\_runner\_repository\_override) | Overrides the ECR repository for the ccw-runner container image. If not provided, the default will be used | `string` | `null` | no |
| <a name="input_ccw_runner_version_override"></a> [ccw\_runner\_version\_override](#input\_ccw\_runner\_version\_override) | Overrides the version for ccw-runner container image resolution. If not provided, the latest BFD version will be used | `string` | `null` | no |
| <a name="input_db_environment_override"></a> [db\_environment\_override](#input\_db\_environment\_override) | For use in database maintenance contexts or in ephemeral environments only | `string` | `null` | no |
| <a name="input_greenfield"></a> [greenfield](#input\_greenfield) | Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed. | `bool` | `true` | no |
| <a name="input_parent_env"></a> [parent\_env](#input\_parent\_env) | The parent environment of the current solution. Will correspond with `terraform.workspace`".<br/>Necessary on `tofu init` and `tofu workspace select` \_only\_. In all other situations, parent env<br/>will be divined from `terraform.workspace`. | `string` | `null` | no |
| <a name="input_pipeline_repository_override"></a> [pipeline\_repository\_override](#input\_pipeline\_repository\_override) | Overrides the ECR repository for the pipeline container image. If not provided, the default will be used | `string` | `null` | no |
| <a name="input_pipeline_version_override"></a> [pipeline\_version\_override](#input\_pipeline\_version\_override) | Overrides the version for pipeline container image resolution. If not provided, the latest BFD version will be used | `string` | `null` | no |
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
| <a name="module_bucket_ccw"></a> [bucket\_ccw](#module\_bucket\_ccw) | ../../terraform-modules/general/secure-bucket | n/a |
| <a name="module_data_db_writer_instance"></a> [data\_db\_writer\_instance](#module\_data\_db\_writer\_instance) | ../../terraform-modules/general/data-db-writer-instance | n/a |
| <a name="module_data_strategies"></a> [data\_strategies](#module\_data\_strategies) | ../../terraform-modules/bfd/bfd-data-ecs-strategies | n/a |
| <a name="module_terraservice"></a> [terraservice](#module\_terraservice) | ../../terraform-modules/bfd/bfd-terraservice | n/a |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_cloudwatch_log_group.ccw_messages](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_cloudwatch_log_group.ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_ecs_task_definition.ccw](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ecs_task_definition) | resource |
| [aws_iam_policy.ccw_execution_ecr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_execution_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_metrics](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_rif_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_rif_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_runner_ecs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_runner_iam](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_runner_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_runner_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_runner_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_runner_scheduler_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_runner_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ccw_ssm_params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.ccw_execution](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.ccw_rif](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.ccw_runner_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.ccw_task](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.ccw_execution](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.ccw_rif](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.ccw_runner_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.ccw_task](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_function.ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.ccw_runner_allow_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [aws_s3_bucket_notification.ccw_bucket_invoke_ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_notification) | resource |
| [aws_scheduler_schedule.ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/scheduler_schedule) | resource |
| [aws_scheduler_schedule_group.ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/scheduler_schedule_group) | resource |
| [aws_security_group.ccw](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group.ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_vpc_security_group_egress_rule.ccw_allow_all_traffic_ipv4](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_egress_rule) | resource |
| [aws_vpc_security_group_egress_rule.ccw_runner_allow_all_traffic_ipv4](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_egress_rule) | resource |
| [aws_vpc_security_group_ingress_rule.allow_ccw_runner_rds_cluster](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_ingress_rule) | resource |
| [aws_vpc_security_group_ingress_rule.ccw_allow_db_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_ingress_rule) | resource |
| [aws_ecr_image.ccw_runner](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_ecr_image.pipeline](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_ecr_repository.pipeline](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_repository) | data source |
| [aws_ecs_cluster.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecs_cluster) | data source |
| [aws_iam_policy_document.ccw_execution_ecr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_execution_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_metrics](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_rif_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_rif_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_rif_trust](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_runner_ecs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_runner_iam](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_runner_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_runner_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_runner_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_runner_scheduler_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_runner_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.ccw_ssm_params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.service_assume_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_rds_cluster.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/rds_cluster) | data source |
| [aws_security_group.aurora_cluster](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->
