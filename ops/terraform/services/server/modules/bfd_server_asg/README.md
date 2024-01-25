## Autoscaling group

What's included:

- Autoscaling group
- Launch configuration
- Autoscaling policies (based on CPU usage)
- Cloudwatch alarms (to inform autoscaling policies)
- Security group with ingress rules to allow CI access to running EC2 instances
- Associated load balancer

Example usage:

```
module "asg" {
  source = "../modules/bfd_server_asg"

  env_config    = {env = "test", tags={}, vpc_id =  }
  role          = "api"
  layer         = "app"

  launch_config = {
    key_name      = "instance-key-pair-name"
    ami_id        = "ami-aabbccx"
    instance_type = "m3.medium"
  }

  lb_config     = {
    name        = "elb-name-goes-here"
    port        = 7443         
  }

  asg_config    = {
    min         = 1
    max         = 2
    desired     = 1
  }

  mgmt_config   = {
    ci_cidrs    = ["1.2.3.4/32", "5.6.7.8/32"]
    tool_sg     = "sg-aabbccdd"
    vpn_sg      = "sb-eeffgghh"
  }
}
```

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
|------|---------|
| <a name="provider_aws"></a> [aws](#provider\_aws) | n/a |
| <a name="provider_external"></a> [external](#provider\_external) | n/a |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [aws_autoscaling_group.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_group) | resource |
| [aws_autoscaling_notification.asg_notifications](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_notification) | resource |
| [aws_autoscaling_policy.filtered_networkin_high_scaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_policy) | resource |
| [aws_autoscaling_policy.filtered_networkin_low_scaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_policy) | resource |
| [aws_cloudwatch_metric_alarm.filtered_networkin_high](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.filtered_networkin_low](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_launch_template.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/launch_template) | resource |
| [aws_security_group.app](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group.base](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group_rule.allow_db_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [aws_kms_key.master_key](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_rds_cluster.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/rds_cluster) | data source |
| [aws_subnet.app_subnets](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnet) | data source |
| [external_external.rds](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_asg_config"></a> [asg\_config](#input\_asg\_config) | n/a | `object({ min = number, max = number, max_warm = number, desired = number, sns_topic_arn = string, instance_warmup = number })` | n/a | yes |
| <a name="input_db_config"></a> [db\_config](#input\_db\_config) | Setup a db ingress rules if defined | `object({ db_sg = string, role = string, db_cluster_identifier = string })` | `null` | no |
| <a name="input_env_config"></a> [env\_config](#input\_env\_config) | All high-level info for the whole vpc | `object({ default_tags = map(string), vpc_id = string, azs = list(string) })` | n/a | yes |
| <a name="input_jdbc_suffix"></a> [jdbc\_suffix](#input\_jdbc\_suffix) | boolean controlling logging of detail SQL values if a BatchUpdateException occurs; false disables detail logging | `string` | `"?logServerErrorDetail=false"` | no |
| <a name="input_kms_key_alias"></a> [kms\_key\_alias](#input\_kms\_key\_alias) | Key alias of environment's KMS key | `string` | n/a | yes |
| <a name="input_launch_config"></a> [launch\_config](#input\_launch\_config) | n/a | `object({ instance_type = string, volume_size = number, ami_id = string, key_name = string, profile = string, user_data_tpl = string, account_id = string })` | n/a | yes |
| <a name="input_layer"></a> [layer](#input\_layer) | app or data | `string` | n/a | yes |
| <a name="input_lb_config"></a> [lb\_config](#input\_lb\_config) | Load balancer information | `object({ name = string, port = number, sg = string })` | `null` | no |
| <a name="input_mgmt_config"></a> [mgmt\_config](#input\_mgmt\_config) | n/a | `object({ vpn_sg = string, tool_sg = string, remote_sg = string, ci_cidrs = list(string) })` | n/a | yes |
| <a name="input_role"></a> [role](#input\_role) | n/a | `string` | n/a | yes |
| <a name="input_scaling_networkin_interval_mb"></a> [scaling\_networkin\_interval\_mb](#input\_scaling\_networkin\_interval\_mb) | The interval value in megabytes for evaluating the asg scaling capacity, based on the metric FilteredNetworkIn | `number` | `100000000` | no |
| <a name="input_seed_env"></a> [seed\_env](#input\_seed\_env) | The solution's source environment. For established environments this is equal to the environment's name | `any` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_asg_id"></a> [asg\_id](#output\_asg\_id) | n/a |
<!-- END_TF_DOCS -->
