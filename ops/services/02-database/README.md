# `database` Service

This subfolder contains the Terraform configuration for the RDS Aurora Postgres database.

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

| Terraservice | Required for Established? | Required for Ephemeral? | Details |
|---|---|---|---|
| `base` | Yes | Yes | N/A |
| `config` | Yes | Yes | N/A |

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
| <a name="input_disable_rds_scheduling_override"></a> [disable\_rds\_scheduling\_override](#input\_disable\_rds\_scheduling\_override) | If true, RDS off hours scheduled scale-in actions will be disabled for this environment. Defaults<br/>  to false. | `bool` | `false` | no |
| <a name="input_ephemeral_rds_snapshot_id_override"></a> [ephemeral\_rds\_snapshot\_id\_override](#input\_ephemeral\_rds\_snapshot\_id\_override) | Specify DB Cluster Snapshot ID from `seed_env`. Will cause the Cluster to be created from a<br/>Snapshot. Defaults to null which, for ephemeral environments, indicates latest snapshot from the<br/>seed cluster on initial definition, falling back to previously specified snapshot on subsequent<br/>execution. | `string` | `null` | no |
| <a name="input_greenfield"></a> [greenfield](#input\_greenfield) | Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed. | `bool` | `true` | no |
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
| [aws_appautoscaling_policy.replicas_cpu_scaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/appautoscaling_policy) | resource |
| [aws_appautoscaling_scheduled_action.off_hours_scale_in](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/appautoscaling_scheduled_action) | resource |
| [aws_appautoscaling_scheduled_action.work_hours_scale_out](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/appautoscaling_scheduled_action) | resource |
| [aws_appautoscaling_target.dynamic_replicas](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/appautoscaling_target) | resource |
| [aws_appautoscaling_target.static_replicas](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/appautoscaling_target) | resource |
| [aws_db_parameter_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_parameter_group) | resource |
| [aws_db_subnet_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/db_subnet_group) | resource |
| [aws_iam_role.db_monitoring](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.db_monitoring](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_rds_cluster.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/rds_cluster) | resource |
| [aws_rds_cluster_instance.writer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/rds_cluster_instance) | resource |
| [aws_rds_cluster_parameter_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/rds_cluster_parameter_group) | resource |
| [aws_security_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_vpc_security_group_egress_rule.allow_all_traffic_ipv4](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_egress_rule) | resource |
| [aws_db_cluster_snapshot.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/db_cluster_snapshot) | data source |
| [aws_iam_policy.rds_monitoring](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_iam_policy_document.rds_monitoring_assume](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->