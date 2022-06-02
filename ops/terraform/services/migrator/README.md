# BFD DB Migrator

This module implements the migrator service (`bfd-db-migrator`) with a distinct,
[terraform workspaces-enabled](https://www.terraform.io/language/state/workspaces) state. **If you're manipulating this state manually, you must verify that you're operating in the appropriate workspace for the targeted environment.**

## Prerequisites
In addition to the [Requirements (below)](#requirements) below, an included [external data source](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/data_source) depends on [`jq`](https://stedolan.github.io/jq/), [`awscli`](https://aws.amazon.com/cli/), and sufficient AWS IAM privileges for the AWS provider [Resources and Date Sources (below)](#resources), remote [AWS S3 Backend](https://www.terraform.io/language/settings/backends/s3#s3-bucket-permissions), and [AWS DynamoDB Table  ](https://www.terraform.io/language/settings/backends/s3#dynamodb-table-permissions).

<!-- TODO: Add reference to a forthcoming contributing document -->

<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 4.12 |
| <a name="requirement_external"></a> [external](#requirement\_external) | ~> 2.2 |

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_ami_id"></a> [ami\_id](#input\_ami\_id) | Provided AMI ID for the migrator. | `string` | n/a | yes |
| <a name="input_git_branch_name"></a> [git\_branch\_name](#input\_git\_branch\_name) | Source branch for this migrator deployment | `string` | n/a | yes |
| <a name="input_create_migrator_instance"></a> [create\_migrator\_instance](#input\_create\_migrator\_instance) | When true, create the migrator instance | `bool` | `false` | no |
| <a name="input_instance_type_override"></a> [instance\_type\_override](#input\_instance\_type\_override) | Valid instance type. See [ec2 instance types](https://aws.amazon.com/ec2/instance-types/) | `string` | `null` | no |
| <a name="input_migrator_monitor_enabled_override"></a> [migrator\_monitor\_enabled\_override](#input\_migrator\_monitor\_enabled\_override) | When true, migrator system emits signals to SQS. Defaults to `true` | `bool` | `null` | no |
| <a name="input_migrator_monitor_heartbeat_interval_seconds_override"></a> [migrator\_monitor\_heartbeat\_interval\_seconds\_override](#input\_migrator\_monitor\_heartbeat\_interval\_seconds\_override) | Sets interval for migrator monitor heartbeat in seconds. Defaults to `300` | `number` | `null` | no |
| <a name="input_rds_cluster_identifier_override"></a> [rds\_cluster\_identifier\_override](#input\_rds\_cluster\_identifier\_override) | RDS Cluster identifier. Defaults to environment-specific RDS cluster. | `string` | `null` | no |
| <a name="input_security_group_ids_extra"></a> [security\_group\_ids\_extra](#input\_security\_group\_ids\_extra) | Extra security group IDs | `list(string)` | `[]` | no |
| <a name="input_sqs_queue_name_override"></a> [sqs\_queue\_name\_override](#input\_sqs\_queue\_name\_override) | SQS Queue Name. Defaults to environment-specific SQS Queue. | `string` | `null` | no |
| <a name="input_volume_size_override"></a> [volume\_size\_override](#input\_volume\_size\_override) | Root volume size override. | `number` | `null` | no |

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

| Name | Description |
|------|-------------|
| <a name="output_rds_cluster_config"></a> [rds\_cluster\_config](#output\_rds\_cluster\_config) | Abbreviated JSON representation of RDS cluster for diagnostic purposes. |

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_iam_instance_profile.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_instance_profile) | resource |
| [aws_iam_policy.sqs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_instance.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/instance) | resource |
| [aws_security_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group_rule.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [aws_sqs_queue.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue) | resource |
| [aws_ami.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ami) | data source |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_iam_policy.ansible_vault_ro](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_iam_policy.cloudwatch_agent_policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_key_pair.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/key_pair) | data source |
| [aws_kms_key.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_rds_cluster.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/rds_cluster) | data source |
| [aws_security_group.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.vpn](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_subnet.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnet) | data source |
| [aws_vpc.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [external_external.rds](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
<!-- END_TF_DOCS -->
