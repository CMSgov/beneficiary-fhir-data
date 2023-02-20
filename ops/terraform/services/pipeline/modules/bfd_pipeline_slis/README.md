# `bfd_pipeline_slis` Sub-module

This sub-module contains the Terraform IaC and Lambda source code for the per-environment
`bfd-ENV-update-pipeline-slis` Lambda. This Lambda is invoked whenever a new file is uploaded to the
environment's corresponding pipeline/ETL S3 Bucket in either the `Done` or `Incoming` "folders". The
Lambda then puts various metrics up to CloudWatch Metrics based upon the timestamp of the incoming
event notification. These metrics allow for the calculation of various SLIs, such as the time it
takes to load a certain RIF file type, the time it takes to fully load incoming data, the time at
which data was first made available and when it was finally loaded, and more.

The metrics that are put to CloudWatch Metrics, and their dimensions, are as follows:

| Metric | Dimensions (Aggregations) | Data Type | Description
| --- | --- | --- | --- |
| `time/data-available` | - None<br>- `data_type`<br>- `group_timestamp`<br>- `data_type`, `group_timestamp` | UTC Timestamp in seconds | Indicates when data was first made available, in `Incoming`, to the pipeline |
| `time/data-loaded` | - None<br>- `data_type`<br>- `group_timestamp`<br>- `data_type`, `group_timestamp` | UTC Timestamp in seconds | Indicates when data was loaded by the pipeline |
| `time/data-first-available` | - None<br>- `group_timestamp` | UTC Timestamp in seconds | Indicates the time of the first data file, in its group, being made available to the pipeline |
| `time/data-fully-loaded` | - None<br>- `group_timestamp` | UTC Timestamp in seconds | Indicates the time when the final data file, in its group, was loaded by the pipeline |
| `time-delta/data-load-time` | - None<br>- `data_type`<br>- `group_timestamp`<br>- `data_type`, `group_timestamp` | A time delta in seconds | Indicates the amount of time, in seconds, that the pipeline took to load a particular RIF file in a particular group |

<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_archive"></a> [archive](#requirement\_archive) | 2.2.0 |

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_iam_policy.cloudwatch_metrics](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_lambda_function.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [archive_file.lambda_src](https://registry.terraform.io/providers/hashicorp/archive/2.2.0/docs/data-sources/file) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_s3_bucket.etl](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/s3_bucket) | data source |
<!-- END_TF_DOCS -->
