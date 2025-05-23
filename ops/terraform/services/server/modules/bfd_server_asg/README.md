## Sub-module for Blue/Green-enabled ASGs and NLBs

What's included:

- Autoscaling group (even/odd)
- Associated Green/Blue load balancers
- Launch configuration
- Autoscaling policies (based on CPU usage)
- Cloudwatch alarms (to inform autoscaling policies)
- Security groups for EC2 Instances and Load Balancers

<!-- BEGIN_TF_DOCS -->
<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Requirements

No requirements.

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_asg_config"></a> [asg\_config](#input\_asg\_config) | n/a | `object({ min = number, max = number, max_warm = number, desired = number, sns_topic_arn = string, instance_warmup = number })` | n/a | yes |
| <a name="input_env_config"></a> [env\_config](#input\_env\_config) | All high-level info for the whole vpc | `object({ default_tags = map(string), vpc_id = string, azs = list(string) })` | n/a | yes |
| <a name="input_kms_key_alias"></a> [kms\_key\_alias](#input\_kms\_key\_alias) | Key alias of environment's KMS key | `string` | n/a | yes |
| <a name="input_launch_config"></a> [launch\_config](#input\_launch\_config) | n/a | <pre>object({<br/>    account_id        = string<br/>    ami_id            = string<br/>    instance_type     = string<br/>    key_name          = string<br/>    profile           = string<br/>    user_data_tpl     = string<br/>    volume_iops       = string<br/>    volume_size       = number<br/>    volume_throughput = number<br/>    volume_type       = string<br/>  })</pre> | n/a | yes |
| <a name="input_layer"></a> [layer](#input\_layer) | app or data | `string` | n/a | yes |
| <a name="input_mgmt_config"></a> [mgmt\_config](#input\_mgmt\_config) | n/a | `object({ vpn_sg = string, tool_sg = string, remote_sg = string, ci_cidrs = list(string) })` | n/a | yes |
| <a name="input_role"></a> [role](#input\_role) | n/a | `string` | n/a | yes |
| <a name="input_seed_env"></a> [seed\_env](#input\_seed\_env) | The solution's source environment. For established environments this is equal to the environment's name | `string` | n/a | yes |
| <a name="input_db_config"></a> [db\_config](#input\_db\_config) | Setup a db ingress rules if defined | `object({ db_sg = list(string), role = string, db_cluster_identifier = string })` | <pre>{<br/>  "db_cluster_identifier": null,<br/>  "db_sg": [],<br/>  "role": null<br/>}</pre> | no |
| <a name="input_jdbc_suffix"></a> [jdbc\_suffix](#input\_jdbc\_suffix) | boolean controlling logging of detail SQL values if a BatchUpdateException occurs; false disables detail logging | `string` | `"?logServerErrorDetail=false"` | no |
| <a name="input_lb_config"></a> [lb\_config](#input\_lb\_config) | Load balancer information | <pre>object({<br/>    is_public                  = bool<br/>    enable_deletion_protection = bool<br/>    server_listen_port         = string<br/>    internal_ingress_cidrs     = list(string)<br/>    internal_prefix_lists      = list(string)<br/>  })</pre> | `null` | no |
| <a name="input_legacy_clb_name"></a> [legacy\_clb\_name](#input\_legacy\_clb\_name) | Name of the legacy CLB to associate ASGs to; only necessary for established environments | `string` | `null` | no |
| <a name="input_legacy_sg_id"></a> [legacy\_sg\_id](#input\_legacy\_sg\_id) | Name of the legacy Security Group to allow ingress from in the app SG | `string` | `null` | no |

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
| [aws_autoscaling_group.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_group) | resource |
| [aws_autoscaling_policy.avg_cpu_high](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_policy) | resource |
| [aws_autoscaling_policy.avg_cpu_low](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_policy) | resource |
| [aws_cloudwatch_metric_alarm.avg_cpu_high](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.avg_cpu_low](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_launch_template.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/launch_template) | resource |
| [aws_lb.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lb) | resource |
| [aws_lb_listener.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lb_listener) | resource |
| [aws_lb_target_group.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lb_target_group) | resource |
| [aws_security_group.app](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group.base](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group.lb](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group_rule.allow_db_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [null_resource.manage_warm_pool](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.set_load_balancer](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.set_target_groups](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [aws_kms_key.master_key](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_rds_cluster.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/rds_cluster) | data source |
| [aws_subnet.app_subnets](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnet) | data source |
| [aws_subnet.dmz_subnets](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnet) | data source |
| [aws_vpc.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [external_external.current_asg](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
| [external_external.current_lt_version](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
| [external_external.rds](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

| Name | Description |
|------|-------------|
| <a name="output_asg_ids"></a> [asg\_ids](#output\_asg\_ids) | n/a |
<!-- END_TF_DOCS -->
