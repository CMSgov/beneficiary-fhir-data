# `server-alarms` Terraservice

This Terraservice defines the resources that implement the BFD Server's alarming and alerts. Most of these alarms are implemented in the form of CloudWatch Alarms, with the exception of the `error-alerter` Lambda that sends Slack alerts whenever a `500` internal error is logged.

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

| Terraservice | Required for Established? | Required for Ephemeral? | Details |
|---|---|---|---|
| `base` | Yes | Yes | N/A |
| `config` | Yes | Yes | N/A |
| `server-metrics` | Yes | Yes | N/A |

## Alerting/Alarming

### SLO Alarms

Our SLOs are defined as various CloudWatch Alarms within `alarms-slos.tf`.

### Log Alarms

Various useful `access`/`messages` log-based CloudWatch Alarms are implemented in `alarms-logs.tf`.

### Load Balancer Alarms

For now, a healthy host count CloudWatch Alarm is defined in `alarms-elb.tf` so we are aware if there are no healthy hosts serving traffic in an environment.

### `500` Errors Alerting (`error-alerter` Lambda)

This Terraservice implements Slack alerts for _any_ `500`s (errors) logged by BFD Server in a particular environment (`terraform workspace`). Whenever a particular environment's `server` logs a `500`, an alert will be sent within 10 to 15 seconds.

As it is possible that _many_ errors occur in a short period, these alerts summarize the number of errors that have been returned during a "lookback" period (defined per-environment). These summaries include a count of `500`s per-partner, per-API-operation, along with a total number of errors returned within the "lookback" period.

Along with the summary, the alert includes various links to Log Insights queries that allow an operator to view the log messages related to the errors with more detailed information such as exception stack traces, request URLs, etc.

#### Diagram

![`bfd_server_error_alerts` Diagram](docs/resources/bfd_server_error_alerts_diagram.png)

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
| [aws_cloudwatch_log_group.alerter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_cloudwatch_metric_alarm.elb_healthy_hosts](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.samhsa_mismatch_error](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.server_log_availability_1hr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.server_query_logging_listener_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_availability_failures_sum](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_availability_uptime_percent](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_claim_no_resources_latency_mean_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_claim_no_resources_latency_mean_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_claim_with_resources_bulk_latency_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_claimresponse_no_resources_latency_mean_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_claimresponse_no_resources_latency_mean_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_claimresponse_with_resources_bulk_latency_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_coverage_bulk_latency_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_coverage_bulk_latency_99p_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_coverage_latency_mean_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_coverage_latency_mean_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_coverage_nonbulk_latency_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_coverage_nonbulk_latency_99p_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_eob_no_resources_latency_mean_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_eob_no_resources_latency_mean_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_eob_with_resources_bulk_latency_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_eob_with_resources_bulk_latency_per_kb_99p_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_eob_with_resources_latency_per_kb_mean_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_eob_with_resources_latency_per_kb_mean_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_eob_with_resources_nonbulk_latency_per_kb_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_eob_with_resources_nonbulk_latency_per_kb_99p_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_http500_count_percent](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_patient_by_contract_count_4000_latency_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_patient_by_contract_count_4000_latency_99p_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_patient_by_contract_count_4000_latency_mean_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_patient_by_contract_count_4000_latency_mean_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_patient_no_contract_bulk_latency_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_patient_no_contract_bulk_latency_99p_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_patient_no_contract_nonbulk_latency_99p_15m_alert](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_cloudwatch_metric_alarm.slo_patient_no_contract_nonbulk_latency_99p_15m_warning](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_metric_alarm) | resource |
| [aws_iam_policy.alerter_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.alerter_query_log](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.invoke_alerter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.alerter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.scheduler_assume_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.alerter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.invoke_alerter_to_scheduler_assume_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_function.alerter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_scheduler_schedule.alerter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/scheduler_schedule) | resource |
| [aws_scheduler_schedule_group.alerter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/scheduler_schedule_group) | resource |
| [archive_file.alerter_lambda_src](https://registry.terraform.io/providers/hashicorp/archive/latest/docs/data-sources/file) | data source |
| [aws_cloudwatch_log_group.server_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/cloudwatch_log_group) | data source |
| [aws_cloudwatch_log_group.server_messages](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/cloudwatch_log_group) | data source |
| [aws_iam_policy_document.alerter_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.alerter_query_log](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.assume_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.assume_scheduler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.invoke_alerter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_lb.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/lb) | data source |
| [aws_lb_target_group.tg_0](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/lb_target_group) | data source |
| [aws_lb_target_group.tg_1](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/lb_target_group) | data source |
| [aws_sns_topic.elb_high_alert_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.logs_alert_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.logs_warning_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.slos_alert_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.slos_high_alert_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_sns_topic.slos_warning_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
| [aws_ssm_parameter.alerter_slack_webhook](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
| [external_external.client_ssls_by_partner](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->
