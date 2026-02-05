# `locust` Terraservice

This Terraservice defines the resources for a Lambda,`bfd-${env}-locust-run-locust`, allowing an operator or automation to run any of the Locust test suites within the `apps/utils/locust_tests` subdirectory. Additionally, this Terraservice creates an Athena Workgroup, corresponding S3 Bucket, and Glue Crawler and Trigger Lambda to allow for querying of test result statistics from running said Lambda.

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

| Terraservice | Required for Established? | Required for Ephemeral? | Details |
|---|---|---|---|
| `base` | Yes | Yes | N/A |
| `config` | Yes | Yes | N/A |
| `cluster` | Yes | Yes | N/A |
| `database` | Yes | Yes | N/A |

## `run-locust` Lambda

The `run-locust` Lambda can be [invoked synchronously](https://docs.amazonaws.cn/en_us/lambda/latest/dg/invocation-sync.html) using the `InvokeFunction` AWS API Action (e.g. `aws lambda invoke...`) with the following `payload` schema:

**IMPORTANT: It is _vital_ that invocations specify a read timeout _greater_ than the `spawned_runtime` of the Locust test, otherwise the the synchronous invocation will fail and start another Lambda erroneously. This can be done by specifying the `--cli-read-timeout <seconds>` flag in the AWS CLI or `read_timeout` in the [`botocore.config` object](https://botocore.amazonaws.com/v1/documentation/api/latest/reference/config.html)**

```json
{
  "suite": "<path relative to apps/utils/locust_tests to test suite to run>",
  "host": "<locust --host to run the test suite against>",
  "spawn_rate": <num users to spawn every second>,
  "users": <num users to spawn in total>,
  "spawned_runtime": "<human-readable span of time to run the tests for after full spawn>",
  "only_summary": <true|false; bool specifying whether to display only a summary of results after test; optional, defaults to false>
  "compare": { // optional, can be omitted and Lambda will not do any comparisons
    "type": "<average|previous; specifies the type of comparison>",
    "tag": "<tagged results to compare against>",
    "load_limit": <number of previous results to average if type is average, ignore otherwise; optional, defaults to null (taking the built-in default of 5)>
  },
  "store": { // optional, can be omitted and Lambda will not store any results to S3
    "tags": <non-empty list of string tags to store with the results of the test run in S3/Athena>
  }
}
```

The Lambda, if successful (note, that doesn't mean _Locust_ is successful, just that the `run-locust` Lambda ran) will return the following `payload` response schema:

```json
{
  "result": "<success|failed; overall result of the Locust test run>",
  "message": "Locust finished running. See 'log_stream_name' and 'log_group_name' properties for CloudWatch Log output location",
  "log_stream_name": "<log stream name>",
  "log_group_name": "<log group name>"
}
```

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
| <a name="input_parent_env"></a> [parent\_env](#input\_parent\_env) | The parent environment of the current solution. Will correspond with `terraform.workspace`".<br/>Necessary on `tofu init` and `tofu workspace select` \_only\_. In all other situations, parent env<br/>will be divined from `terraform.workspace`. | `string` | `null` | no |
| <a name="input_region"></a> [region](#input\_region) | n/a | `string` | `"us-east-1"` | no |
| <a name="input_run_locust_repository_override"></a> [run\_locust\_repository\_override](#input\_run\_locust\_repository\_override) | Overrides the ECR repository for the 'run-locust' container image. If not provided, the default will be used | `string` | `null` | no |
| <a name="input_run_locust_version_override"></a> [run\_locust\_version\_override](#input\_run\_locust\_version\_override) | Overrides the version for 'run-locust' container image resolution. If not provided, the latest BFD version will be used | `string` | `null` | no |
| <a name="input_secondary_region"></a> [secondary\_region](#input\_secondary\_region) | n/a | `string` | `"us-west-2"` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_bucket_athena"></a> [bucket\_athena](#module\_bucket\_athena) | ../../terraform-modules/general/secure-bucket | n/a |
| <a name="module_locust_stats_trigger"></a> [locust\_stats\_trigger](#module\_locust\_stats\_trigger) | ../../terraform-modules/general/trigger-glue-crawler | n/a |
| <a name="module_terraservice"></a> [terraservice](#module\_terraservice) | ../../terraform-modules/bfd/bfd-terraservice | n/a |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_athena_workgroup.locust_stats](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/athena_workgroup) | resource |
| [aws_cloudwatch_log_group.run_locust](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_glue_crawler.locust_stats](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/glue_crawler) | resource |
| [aws_iam_policy.crawler_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.crawler_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_locust_athena](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_locust_glue](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_locust_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_locust_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_locust_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.run_locust_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.crawler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.run_locust](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.crawler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.run_locust](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_function.run_locust](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.allow_s3_locust_stats_glue_trigger](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [aws_s3_bucket_notification.locust_stats_glue_trigger](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_notification) | resource |
| [aws_security_group.run_locust](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_vpc_security_group_ingress_rule.allow_run_locust](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_security_group_ingress_rule) | resource |
| [aws_ecr_image.run_locust](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_iam_policy.amazon_athena_full_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_iam_policy.aws_glue_service_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_iam_policy.lambda_vps_access_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_iam_policy_document.crawler_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.crawler_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.glue_assume_crawler](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.lambda_assume](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_locust_athena](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_locust_glue](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_locust_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_locust_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_locust_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.run_locust_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
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
