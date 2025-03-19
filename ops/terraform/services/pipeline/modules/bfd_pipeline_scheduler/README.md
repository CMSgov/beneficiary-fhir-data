# `bfd_pipeline_scheduler` Sub-module

This submodule defines a Lambda and its corresponding infrastructure. This Lambda,
`bfd-${env}-pipeline-scheduler`, schedules scale-in/scale-out for the CCW-variant of the BFD
Pipeline; it is invoked when new files are created in the `env`'s corresponding S3 pipeline/ETL
bucket at specific paths:

- `Synthetic/Incoming/`
- `Synthetic/Done/`
- `Incoming/`
- `Done/`

Objects uploaded to, moved to, or removed from these paths in the ETL bucket will invoke this
Lambda, which will then determine if the BFD CCW Pipeline should be started or not via
[scheduled actions](https://docs.aws.amazon.com/autoscaling/ec2/userguide/ec2-auto-scaling-scheduled-scaling.html)
applied to its AutoScaling Group. In more detail:

- If the file was uploaded to either of the `Incoming` folders, an action to scale-out a single
  instance is scheduled on the CCW BFD Pipeline's ASG. This action is scheduled either immediately
  if the file belongs to a data load timestamped in the past or it is scheduled for the time
  specified by the file's data load in the future. This means that incoming loads that should be
  loaded immediately will start the Pipeline immediately, and those that should be loaded in the
  future will start the Pipeline when they need to be loaded
- If the file was _removed_ from either of the `Incoming` folders _and_ there are no other files
  within that file's data load in its `Incoming` folder, then the corresponding ASG Scheduled Action
  is **removed** from the BFD Pipeline ASG such that it will no longer scale-out for a data load
  that no longer exists
- Else, if the file was uploaded to either of the `Done` folders, there are no files in the
  corresponding `Incoming` folder, _and_ there are no other data loads in `Incoming` timestamped for
  _within the next five minutes_, an action to scale-in the BFD Pipeline in the next five minutes is
  scheduled on the CCW BFD Pipeline ASG

In short, this Lambda reacts to signaling from the CCW Pipeline S3 Bucket and schedules the CCW
Pipeline EC2 instance to run or stop through its ASG. With this Lambda, the CCW Pipeline should only
run when there is data to load.

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
| [aws_iam_policy.autoscaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.autoscaling_attach_lambda_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.logs_attach_lambda_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.s3_attach_lambda_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_function.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [aws_sns_topic_subscription.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_subscription) | resource |
| [archive_file.lambda_src](https://registry.terraform.io/providers/hashicorp/archive/2.2.0/docs/data-sources/file) | data source |
| [aws_iam_policy.permissions_boundary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_kms_key.env_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_s3_bucket.etl](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/s3_bucket) | data source |
| [aws_sns_topic.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/sns_topic) | data source |
<!-- END_TF_DOCS -->
