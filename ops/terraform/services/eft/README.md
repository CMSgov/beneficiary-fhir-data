# `eft` Terraservice

> _NOTE: `eft` is a `prod` and `test` Terraservice and so cannot be applied to environments other than `test` and `prod` due to those environments missing necessary configuration. This is intentional. At this time, `eft` does **not** support ephemeral environments, even those seeded from the aforementioned, supported environments._

This Terraservice contains the Terraform IaC for BFD's EFT infrastructure. This implementation roughly follows the recommended implementation detailed by EFT SWEEPS 2.0.

## `terraform destroy`ing `eft`

The `eft` Terraservice defines multiple `aws_ec2_subnet_cidr_reservation` resources that should be left applied to our infrastructure at all times. These resources ensure that the static, internal IP addresses allocated for the SFTP server endpoints in each environment remain free and unallocated to other AWS services. Therefore, these resources have an explicit `prevent_destroy` `lifecycle` attached to them, so attempting to `terraform destroy` `eft` in an environment with these resources will result in an error.

So, to workaround this, the following command can be used to do a targeted `terraform destroy` targeting every resource other than `aws_ec2_subnet_cidr_reservation` resources and the data sources it depends on:

```bash
terraform destroy $(for r in `terraform state list | grep -vE "aws_ec2_subnet_cidr_reservation.this|data.*"` ; do printf "-target=${r} "; done)
```

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
| <a name="input_bfd_version_override"></a> [bfd\_version\_override](#input\_bfd\_version\_override) | BFD release version override. When empty, defaults to resolving the release version from GitHub releases. | `string` | `null` | no |
| <a name="input_cloudtamer_iam_path"></a> [cloudtamer\_iam\_path](#input\_cloudtamer\_iam\_path) | IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts | `string` | `"/delegatedadmin/developer/"` | no |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->



<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Outputs

No outputs.

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_alb_target_group_attachment.nlb_to_vpc_endpoint](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/alb_target_group_attachment) | resource |
| [aws_ec2_subnet_cidr_reservation.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ec2_subnet_cidr_reservation) | resource |
| [aws_iam_policy.eft_user](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.isp_bcda_bucket_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.outbound_notifs_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.outbound_partner_notifs_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.partner_bucket_access](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.sftp_outbound_transfer_kms](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.sftp_outbound_transfer_logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.sftp_outbound_transfer_s3](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.sftp_outbound_transfer_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.sftp_outbound_transfer_sqs_dlq](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.sftp_outbound_transfer_ssm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.ct_isp_bcda_bucket_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.ct_partner_bucket_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.eft_user](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.isp_bcda_bucket_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.outbound_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.outbound_partner_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.partner_bucket_role](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.sftp_outbound_transfer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.outbound_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.outbound_partner_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.sftp_outbound_transfer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_function.sftp_outbound_transfer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_function_event_invoke_config.sftp_outbound_transfer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function_event_invoke_config) | resource |
| [aws_lambda_permission.sftp_outbound_transfer_sns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [aws_lb.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lb) | resource |
| [aws_lb_listener.nlb_to_vpc_endpoint](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lb_listener) | resource |
| [aws_lb_target_group.nlb_to_vpc_endpoint](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lb_target_group) | resource |
| [aws_route53_record.nlb_alias](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/route53_record) | resource |
| [aws_s3_bucket.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket) | resource |
| [aws_s3_bucket_lifecycle_configuration.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_lifecycle_configuration) | resource |
| [aws_s3_bucket_logging.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_logging) | resource |
| [aws_s3_bucket_notification.bucket_notifications](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_notification) | resource |
| [aws_s3_bucket_policy.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_policy) | resource |
| [aws_s3_bucket_public_access_block.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_public_access_block) | resource |
| [aws_s3_bucket_server_side_encryption_configuration.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_server_side_encryption_configuration) | resource |
| [aws_s3_bucket_versioning.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_versioning) | resource |
| [aws_security_group.nlb](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group.sftp_outbound_transfer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_security_group.vpc_endpoint](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/security_group) | resource |
| [aws_sns_topic.inbound_received_s3_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic) | resource |
| [aws_sns_topic.outbound_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic) | resource |
| [aws_sns_topic.outbound_partner_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic) | resource |
| [aws_sns_topic.outbound_pending_s3_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic) | resource |
| [aws_sns_topic_policy.inbound_received_s3_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_policy) | resource |
| [aws_sns_topic_policy.outbound_partner_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_policy) | resource |
| [aws_sns_topic_policy.outbound_pending_s3_notifs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_policy) | resource |
| [aws_sns_topic_subscription.sftp_outbound_transfer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_subscription) | resource |
| [aws_sns_topic_subscription.sftp_outbound_transfer_contingency](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sns_topic_subscription) | resource |
| [aws_sqs_queue.sftp_outbound_transfer_contingency](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue) | resource |
| [aws_sqs_queue.sftp_outbound_transfer_dlq](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue) | resource |
| [aws_sqs_queue_policy.sftp_outbound_transfer_sqs_contingency](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/sqs_queue_policy) | resource |
| [aws_transfer_server.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/transfer_server) | resource |
| [aws_transfer_ssh_key.eft_user](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/transfer_ssh_key) | resource |
| [aws_transfer_user.eft_user](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/transfer_user) | resource |
| [aws_vpc_endpoint.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/vpc_endpoint) | resource |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_ec2_managed_prefix_list.vpn](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ec2_managed_prefix_list) | data source |
| [aws_ecr_image.sftp_outbound_transfer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_image) | data source |
| [aws_ecr_repository.ecr](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ecr_repository) | data source |
| [aws_iam_policy.permissions_boundary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_kms_key.cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_kms_key.config_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_network_interface.vpc_endpoint](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/network_interface) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_route53_zone.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/route53_zone) | data source |
| [aws_ssm_parameter.partners_list_json](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
| [aws_ssm_parameter.zone_is_private](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
| [aws_ssm_parameter.zone_name](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
| [aws_ssm_parameters_by_path.params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_subnet.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnet) | data source |
| [aws_subnets.sftp_outbound_transfer](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnets) | data source |
| [aws_vpc.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [aws_vpc_endpoint_service.transfer_server](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc_endpoint_service) | data source |
<!-- END_TF_DOCS -->
