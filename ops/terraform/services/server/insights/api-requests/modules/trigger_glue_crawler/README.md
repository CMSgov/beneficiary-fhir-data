# `trigger_glue_crawler` Submodule

This submodule defines a Lambda and its corresponding permissions and triggers that will run
whenever an S3 event notification from the API requests S3 bucket is received. When run, the Lambda
will trigger the corresponding API requests' Glue Crawler _if_ the S3 event notification encodes a
creation event for a partition that does not yet exist in the `bfd_insights_bfd_${env}_api_requests`
Glue Table.

This submodule is referenced (and used) in the parent module `api-requests`. The S3 event
notifications that trigger the aforementioned Lambda are defined in
`ops/terraform/env/mgmt/insights/s3.tf` due to [constraints on duplicate S3 event notification resources](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_notification)

<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Requirements

No requirements.

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Resources

No resources.

<!-- END_TF_DOCS -->
