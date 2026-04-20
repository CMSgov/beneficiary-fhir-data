# `idr-pipeline` Terraservice

This Terraservice defines several important IDR Pipeline related resources:

- the ECS Task Definition for the IDR Pipeline Task workload
- the `run-idr-pipeline` Lambda that manages running the `idr-pipeline` Task and allows for manual override of its container's `CMD` and environment
- the IDR Load Events SQS Queue
- the `consume-idr-events` Lambda that consumes events from the aforementioned Queue, loading events into the `idr.source_load_events` and starting the `run-idr-pipeline` Lambda

> [!IMPORTANT]
> The following variables are statically defined within the `idr-pipeline`'s Task Definition so as to ensure that IDR data is only loaded as far back as necessary:
>
> - `IDR_MIN_CLAIM_NCH_TRANSACTION_DATE`: `2014-06-30`
> - `IDR_MIN_CLAIM_SS_TRANSACTION_DATE`: `2021-03-01`
> Additional context for these dates is available within Confluence under the `v3 Decision Log`

## `run-idr-pipeline` Lambda

### Invocation

> [!IMPORTANT]
> It is **vital** that invocations specify a read timeout _greater_ than the `spawned_runtime` of the Locust test, otherwise the the synchronous invocation will fail and start another Lambda erroneously. This can be done by specifying the `--cli-read-timeout <seconds>` flag in the AWS CLI or `read_timeout` in the [`botocore.config` object](https://botocore.amazonaws.com/v1/documentation/api/latest/reference/config.html)

> [!NOTE]
> Specifying no invocation payload will result in a _typical_ `idr-pipeline` Task spawning in `incremental`, `IDR` mode with the `run-idr-pipeline` _not_ rescheduling itself if an `idr-pipeline` Task is already running

The `run-locust` Lambda can be [invoked synchronously](https://docs.amazonaws.cn/en_us/lambda/latest/dg/invocation-sync.html) using the `InvokeFunction` AWS API Action (e.g. `aws lambda invoke...`) or in the AWS Console in the `Test` tab given a JSON payload following the schema below.

The schema is implemented within the Lambda's source as the `InvokeModel`, for further context.

#### Payload Schema

| Field | Type | Optional | Description |
| :--- | :--- | :--- | :--- |
| `env` | `object` (`dict[str, str]`) or `null` | Yes | A mapping of environment variable name to value for the container environment; specifically, for the IDR Pipeline process |
| `command` | `string` or `null` | Yes | A command-line string for a container to run as its `CMD` entrypoint |
| `cpu` | `int` or `null` | Yes | Amount of CPU to give to the Task |
| `memory` | `int` or `null` | Yes | Amount of memory to give to the Task |
| `ecs_exec` | `boolean` or `null` | Yes | A boolean indicating whether "ECS exec" is enabled for the container session so that an operator can interactively connect to the running `idr-pipeline` Task |
| `reschedule` | `boolean` or `null` | Yes | A boolean indicating whether the Lambda will reschedule itself to run again if an `idr-pipeline` Task is already running |

#### Example JSON Payload

The following JSON structure represents an example payload for invoking the `run-idr-pipeline` Lambda that:

- sets a few environment variables
- sets the `CMD` to `sleep infinity` to keep the Task alive indefinitely
- sets `ecs_exec` to `true` allowing an operator to `aws ecs execute-command ...` into the container
- sets `reschedule` to `true` signaling to the Lambda to reschedule itself if the `idr-pipeline` Task is already running
- leaves `cpu` and `memory` unset to use the defaults provided in the Task Definition

```json
{
  "env": {
    "SOME_IDR_PIPELINE_ENV_VAR": "some_value",
    "ANOTHER_VAR": "another_value"
  },
  "command": "sleep infinity",
  "ecs_exec": true,
  "reschedule": true
}
```

### Result

Upon a successful invocation of `run-idr-pipeline`, it will return a JSON object with details about the result of running the Lambda. The `result_type` property indicates the overall result with the `details` property containing further context.

Errors will return the error message and exception context.

#### `result_type`s

| `result_type` | Description |
| :--- | :--- |
| `AlreadyRunningResult` | The `idr-pipeline` Task is already running and `reschedule` was `false`, so there was nothing to do |
| `AlreadyScheduledResult` | The `idr-pipeline` Task is already running and `reschedule` was `true`, _but_ a Schedule already exists within the `LAMBDA_RESCHEDULE_IN` period that will re-run the Lambda, so there was nothing to do |
| `RescheduledResult` | The `idr-pipeline` Task is already running and `reschedule` was `true` _and_ no Schedule exists within the `LAMBDA_RESCHEDULE_IN` period so a Schedule was created to re-invoke the Lambda when that period of time elapses |
| `PipelineStartedResult` | The `idr-pipeline` Task was started with the provided inputs successfully |

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

| Terraservice | Required for Established? | Required for Ephemeral? | Details |
|---|---|---|---|
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
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | ~> 1.10.0 |
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 6 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_consume_idr_events_repository_override"></a> [consume\_idr\_events\_repository\_override](#input\_consume\_idr\_events\_repository\_override) | Overrides the ECR repository for the consume-idr-events container image. If not provided, the default will be used | `string` | `null` | no |
| <a name="input_consume_idr_events_version_override"></a> [consume\_idr\_events\_version\_override](#input\_consume\_idr\_events\_version\_override) | Overrides the version for consume-idr-events container image resolution. If not provided, the latest BFD version will be used | `string` | `null` | no |
| <a name="input_db_environment_override"></a> [db\_environment\_override](#input\_db\_environment\_override) | For use in database maintenance contexts or in ephemeral environments only | `string` | `null` | no |
| <a name="input_parent_env"></a> [parent\_env](#input\_parent\_env) | The parent environment of the current solution. Will correspond with `terraform.workspace`".<br/>Necessary on `tofu init` and `tofu workspace select` \_only\_. In all other situations, parent env<br/>will be divined from `terraform.workspace`. | `string` | `null` | no |
| <a name="input_pipeline_repository_override"></a> [pipeline\_repository\_override](#input\_pipeline\_repository\_override) | Overrides the ECR repository for the pipeline container image. If not provided, the default will be used | `string` | `null` | no |
| <a name="input_pipeline_version_override"></a> [pipeline\_version\_override](#input\_pipeline\_version\_override) | Overrides the version for pipeline container image resolution. If not provided, the latest BFD version will be used | `string` | `null` | no |
| <a name="input_region"></a> [region](#input\_region) | n/a | `string` | `"us-east-1"` | no |
| <a name="input_run_idr_pipeline_repository_override"></a> [run\_idr\_pipeline\_repository\_override](#input\_run\_idr\_pipeline\_repository\_override) | Overrides the ECR repository for the run-idr-pipeline container image. If not provided, the default will be used | `string` | `null` | no |
| <a name="input_run_idr_pipeline_version_override"></a> [run\_idr\_pipeline\_version\_override](#input\_run\_idr\_pipeline\_version\_override) | Overrides the version for run-idr-pipeline container image resolution. If not provided, the latest BFD version will be used | `string` | `null` | no |
| <a name="input_secondary_region"></a> [secondary\_region](#input\_secondary\_region) | n/a | `string` | `"us-west-2"` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

| Name | Source | Version |
|------|--------|---------|
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
| [aws_cloudwatch_log_group.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_cloudwatch_log_group.idr_messages](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_cloudwatch_log_group.run_idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_ecs_task_definition.idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ecs_task_definition) | resource |
| [aws_iam_policy.events_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.events_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.events_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.events_sqs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.events_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.idr_ecs_exec](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.idr_execution_ecr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.idr_execution_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.idr_execution_ssm_params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.idr_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.idr_rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.idr_ssm_params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_idr_ecs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_idr_iam](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_idr_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_idr_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_idr_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_idr_scheduler_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.idr_execution](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.idr_task](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.run_idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.run_idr_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.idr_execution](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.idr_task](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.run_idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.run_idr_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_event_source_mapping.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_event_source_mapping) | resource |
| [aws_lambda_function.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_function.run_idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_function_event_invoke_config.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function_event_invoke_config) | resource |
| [aws_lambda_permission.events_allow_sqs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [aws_scheduler_schedule.run_idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/scheduler_schedule) | resource |
| [aws_scheduler_schedule_group.run_idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/scheduler_schedule_group) | resource |
| [aws_security_group.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group.idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_sqs_queue.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue) | resource |
| [aws_sqs_queue.events_dlq](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue) | resource |
| [aws_sqs_queue_policy.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue_policy) | resource |
| [aws_sqs_queue_redrive_allow_policy.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue_redrive_allow_policy) | resource |
| [aws_sqs_queue_redrive_policy.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue_redrive_policy) | resource |
| [aws_vpc_security_group_egress_rule.events_allow_all_traffic_ipv4](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_egress_rule) | resource |
| [aws_vpc_security_group_egress_rule.idr_allow_all_traffic_ipv4](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_egress_rule) | resource |
| [aws_vpc_security_group_ingress_rule.allow_events_rds_cluster](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_ingress_rule) | resource |
| [aws_vpc_security_group_ingress_rule.idr_allow_db_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_ingress_rule) | resource |
| [aws_ecr_image.events](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_ecr_image.pipeline](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_ecr_image.run_idr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_ecr_repository.pipeline](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_repository) | data source |
| [aws_ecs_cluster.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecs_cluster) | data source |
| [aws_iam_policy_document.events_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.events_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.events_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.events_queue_policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.events_sqs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.events_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.idr_ecs_exec](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.idr_execution_ecr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.idr_execution_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.idr_execution_ssm_params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.idr_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.idr_rds](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.idr_ssm_params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_idr_ecs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_idr_iam](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_idr_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_idr_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_idr_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_idr_scheduler_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
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
