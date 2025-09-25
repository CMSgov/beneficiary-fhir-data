# `server-insights` Terraservice

This Terraservice encodes the resources related to insights-gathering for the Server as part of BFD Insights.

Many of the resources have been lifted with little modification from the Legacy `server/insights/api-requests` Terraservice, and so there may be some idiosyncracies with respect to other, more modern Terraform.

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

| Terraservice | Required for Established? | Required for Ephemeral? | Details |
|---|---|---|---|
| `server` | Yes | Yes | N/A |

## `athena-views.tf`

This file defines the various [Athena Views](https://docs.aws.amazon.com/athena/latest/ug/views.html) that make working with BFD Insights data a little more ergonomic.

Terraform has no built-in resource for defining these views, and while there _are_ workarounds (using the `aws_glue_catalog_table` resource and some undocumented "magic", see [this Slack Overflow Answer](https://stackoverflow.com/a/56347331)) to allow for Terraform to define views they are, unfortunately, not applicable to our specific use-case (due to the `api_requests` view dynamically `SELECT`ing all columns from its corresponding table). Instead, Terraform's `null_resource` resource and `local-exec` provisioners are used with the `local-exec` provisioners executing a custom-made `bash` script that runs the appropriate AWS CLI commands to create and destroy Athena Views. This solution is not _quite_ as optimal as strictly defining each view in Terraform using `resource`s but works well enough for our use-case.

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
| <a name="module_database"></a> [database](#module\_database) | ../../../insights/terraform/modules/database | n/a |
| <a name="module_glue_table_api_requests"></a> [glue\_table\_api\_requests](#module\_glue\_table\_api\_requests) | ../../../insights/terraform/modules/table | n/a |
| <a name="module_lambda_trigger_crawler"></a> [lambda\_trigger\_crawler](#module\_lambda\_trigger\_crawler) | ../../terraform-modules/general/trigger-glue-crawler | n/a |
| <a name="module_terraservice"></a> [terraservice](#module\_terraservice) | ../../terraform-modules/bfd/bfd-terraservice | n/a |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_cloudwatch_log_subscription_filter.cloudwatch_access_log_subscription](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_subscription_filter) | resource |
| [aws_cloudwatch_log_subscription_filter.ecs_access_log](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_subscription_filter) | resource |
| [aws_glue_crawler.glue_crawler_api_requests](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/glue_crawler) | resource |
| [aws_iam_policy.iam_policy_firehose](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.iam_role_cloudwatch_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.iam_role_firehose](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.iam_role_firehose_lambda](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_kinesis_firehose_delivery_stream.firehose_ingester](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/kinesis_firehose_delivery_stream) | resource |
| [aws_lambda_function.lambda_function_format_firehose_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [null_resource.athena_view_api_requests](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.athena_view_api_requests_by_bene](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.athena_view_new_benes_by_day](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [archive_file.zip_archive_format_firehose_logs](https://registry.terraform.io/providers/hashicorp/archive/latest/docs/data-sources/file) | data source |
| [aws_cloudwatch_log_group.server_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/cloudwatch_log_group) | data source |
| [aws_iam_role.iam_role_glue](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_role) | data source |
| [aws_kms_key.kms_key](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_s3_bucket.bfd_insights_bucket](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/s3_bucket) | data source |
| [aws_ssm_parameter.bfd_insights_bucket](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
| [external_external.src_table_version](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->
