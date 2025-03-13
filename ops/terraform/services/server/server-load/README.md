# BFD Server Load Test Suite

<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 5.53.0 |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_ami_id_override"></a> [ami\_id\_override](#input\_ami\_id\_override) | BFD Server Load override ami-id. Defaults to latest server-load AMI from `master`. | `string` | `null` | no |
| <a name="input_coasting_time"></a> [coasting\_time](#input\_coasting\_time) | The amount of time, in seconds, the load test should continue for after receiving a scaling notification. Ignored if stop\_on\_scaling is false. Ends immediately on operator stop signal | `number` | `0` | no |
| <a name="input_container_image_tag_node_override"></a> [container\_image\_tag\_node\_override](#input\_container\_image\_tag\_node\_override) | Overrides the Container image URI used by the built load suite worker node lambda | `string` | `null` | no |
| <a name="input_create_locust_instance"></a> [create\_locust\_instance](#input\_create\_locust\_instance) | When true, create the locust instance | `bool` | `false` | no |
| <a name="input_initial_worker_nodes"></a> [initial\_worker\_nodes](#input\_initial\_worker\_nodes) | The number of initial Locust worker nodes to spawn before checking for stop signals. Useful for static load tests | `number` | `0` | no |
| <a name="input_locust_exclude_tags"></a> [locust\_exclude\_tags](#input\_locust\_exclude\_tags) | Space-delimited. The locust tasks with ANY of the given tags will be excluded from execution | `string` | `""` | no |
| <a name="input_locust_master_port"></a> [locust\_master\_port](#input\_locust\_master\_port) | The port to connect to that is used by the locust master for distributed load testing. | `number` | `5557` | no |
| <a name="input_locust_tags"></a> [locust\_tags](#input\_locust\_tags) | Space-delimited. Run the locust tasks with ANY of the given @tag(s). Will run all tasks if not provided | `string` | `""` | no |
| <a name="input_max_spawned_nodes"></a> [max\_spawned\_nodes](#input\_max\_spawned\_nodes) | The maximum number of Lambda worker nodes to spawn over the lifetime of a given test run. Does not account for failed nodes or nodes that reach their Lambda timeout | `number` | `0` | no |
| <a name="input_max_spawned_users"></a> [max\_spawned\_users](#input\_max\_spawned\_users) | The maximum number of simulated Locust users (not worker nodes) to spawn. Use this and spawn rate to constrain the load during a test run | `number` | `0` | no |
| <a name="input_node_lambda_name"></a> [node\_lambda\_name](#input\_node\_lambda\_name) | The name of the Locust worker node Lambda function that will be executed to spawn a Locust worker instance | `string` | `"bfd-test-server-load-node"` | no |
| <a name="input_node_spawn_time"></a> [node\_spawn\_time](#input\_node\_spawn\_time) | The amount of time to wait between spawning more Lambda Locust worker nodes. Does not affect initial spawned nodes | `number` | `10` | no |
| <a name="input_server_load_dir"></a> [server\_load\_dir](#input\_server\_load\_dir) | BFD Server Load directory. | `string` | `"/opt/server-load"` | no |
| <a name="input_server_load_user"></a> [server\_load\_user](#input\_server\_load\_user) | BFD Server Load user. | `string` | `"bb-server-load"` | no |
| <a name="input_sqs_queue_name"></a> [sqs\_queue\_name](#input\_sqs\_queue\_name) | The name of the SQS queue that will be polled for scaling notifications or stop signals | `string` | `"bfd-test-server-load"` | no |
| <a name="input_stop_on_node_limit"></a> [stop\_on\_node\_limit](#input\_stop\_on\_node\_limit) | Whether the load test run should end once the maximum Lambda worker node limit is reached. Set to false for scenarios where a static load test is desired | `bool` | `true` | no |
| <a name="input_stop_on_scaling"></a> [stop\_on\_scaling](#input\_stop\_on\_scaling) | Whether the load test run should end, if coasting\_time is zero, or start coasting once receiving a scaling notification. Set to false for scenarios where a static load test is desired | `bool` | `true` | no |
| <a name="input_test_host"></a> [test\_host](#input\_test\_host) | The URL under test -- should match the given environment | `string` | `"https://test.bfd.cms.gov"` | no |
| <a name="input_test_runtime_limit"></a> [test\_runtime\_limit](#input\_test\_runtime\_limit) | Runtime limit in seconds. If stop\_on\_scaling is false, this limit is the total amount of time the load test has to run. If stop\_on\_scaling is true, this limit indicates the amount of time to check for scaling notifications during a test run before stopping | `number` | `0` | no |
| <a name="input_user_spawn_rate"></a> [user\_spawn\_rate](#input\_user\_spawn\_rate) | The rate at which simulated Locust users (not worker nodes) will spawn. Set this equal to max\_spawned\_users if all users should be spawned immediately | `number` | `1` | no |
| <a name="input_warm_instance_target"></a> [warm\_instance\_target](#input\_warm\_instance\_target) | The number of BFD Server instances to target before scaling causes the load test to stop | `number` | `0` | no |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_autoscaling_notification.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/autoscaling_notification) | resource |
| [aws_iam_instance_profile.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_instance_profile) | resource |
| [aws_iam_policy.ecr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.sqs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.ec2](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_instance.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/instance) | resource |
| [aws_lambda_function.node](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_security_group.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group_rule.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group_rule) | resource |
| [aws_sns_topic.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic) | resource |
| [aws_sns_topic_subscription.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_subscription) | resource |
| [aws_sqs_queue.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue) | resource |
| [aws_sqs_queue_policy.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue_policy) | resource |
| [random_integer.this](https://registry.terraform.io/providers/hashicorp/random/latest/docs/resources/integer) | resource |
| [aws_ami.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ami) | data source |
| [aws_autoscaling_group.asg](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/autoscaling_group) | data source |
| [aws_availability_zones.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/availability_zones) | data source |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_ecr_image.image_node](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_ecr_repository.ecr_controller](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_repository) | data source |
| [aws_ecr_repository.ecr_node](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_repository) | data source |
| [aws_iam_policy.cloudwatch_agent_policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_iam_policy.cloudwatch_agent_xray_policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_iam_policy.permissions_boundary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_kms_key.cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_kms_key.config_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_kms_key.mgmt_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_kms_key.mgmt_config_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_launch_template.template](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/launch_template) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_security_group.rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.vpn](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_ssm_parameter.container_image_tag_controller](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
| [aws_ssm_parameter.container_image_tag_node](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
| [aws_ssm_parameters_by_path.nonsensitive_common](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_subnet.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnet) | data source |
| [aws_subnets.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnets) | data source |
| [aws_vpc.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Outputs

| Name | Description |
|------|-------------|
| <a name="output_controller_ip"></a> [controller\_ip](#output\_controller\_ip) | For development purposes. When present, displays the locust 'controller' private IP address. |
<!-- END_TF_DOCS -->
