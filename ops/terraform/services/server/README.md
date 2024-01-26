# `server` Terraservice

> _NOTE: As of writing, this Terraservice is used only for ephemeral environments and not for our established environments. This Terraservice will eventually be promoted to manage the BFD Server's infrastructure in those established environments, but until then **do not** `apply` this Terraservice in any established environment._

This "Terraservice" encapsulates the Terraform necessary to create AWS infrastructure related to the BFD Server. This Terraservice explicitly depends upon the `base` and `common` Terraservices. Ephemeral environments are supported through usage of `terraform workspace`s.

## Usage

The `base` and `common` Terraservices _must_ be applied, to the _same Terraform workspace_, prior to `apply`ing this Terraservice. This Terraservice relies on the resources created by those Terraservices, and will fail to apply if they do not exist.

Assuming you have created a Terraform workspace corresponding to your target environment and have switched to it, this Terraservice can be applied without specifying any variables using:

```bash
terraform apply
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
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 4.22 |

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_ami_id_override"></a> [ami\_id\_override](#input\_ami\_id\_override) | BFD Server override ami-id. Defaults to latest server/fhir AMI from `master`. | `string` | `null` | no |
| <a name="input_force_create_server_dashboards"></a> [force\_create\_server\_dashboards](#input\_force\_create\_server\_dashboards) | Forces the creation of bfd\_server\_dashboards for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_disk_alarms"></a> [force\_create\_server\_disk\_alarms](#input\_force\_create\_server\_disk\_alarms) | Forces the creation of bfd\_server\_disk\_alarms for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_error_alerts"></a> [force\_create\_server\_error\_alerts](#input\_force\_create\_server\_error\_alerts) | Forces the creation of bfd\_server\_error\_alerts for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_lb_alarms"></a> [force\_create\_server\_lb\_alarms](#input\_force\_create\_server\_lb\_alarms) | Forces the creation of bfd\_server\_lb\_alarms for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_log_alarms"></a> [force\_create\_server\_log\_alarms](#input\_force\_create\_server\_log\_alarms) | Forces the creation of bfd\_server\_log\_alarms for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_metrics"></a> [force\_create\_server\_metrics](#input\_force\_create\_server\_metrics) | Forces the creation of bfd\_server\_metrics for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_slo_alarms"></a> [force\_create\_server\_slo\_alarms](#input\_force\_create\_server\_slo\_alarms) | Forces the creation of bfd\_server\_slo\_alarms for ephemeral environments | `bool` | `false` | no |
| <a name="input_jdbc_suffix"></a> [jdbc\_suffix](#input\_jdbc\_suffix) | boolean controlling logging of detail SQL values if a BatchUpdateException occurs; false disables detail logging | `string` | `"?logServerErrorDetail=false"` | no |

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_ami.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ami) | data source |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_ec2_managed_prefix_list.jenkins](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ec2_managed_prefix_list) | data source |
| [aws_ec2_managed_prefix_list.vpn](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ec2_managed_prefix_list) | data source |
| [aws_s3_bucket.logs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/s3_bucket) | data source |
| [aws_security_group.aurora_cluster](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.remote](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.tools](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.vpn](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_ssm_parameters_by_path.nonsensitive_common](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_ssm_parameters_by_path.nonsensitive_service](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_vpc.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [aws_vpc.mgmt](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [aws_vpc_peering_connection.peers](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc_peering_connection) | data source |

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Outputs

No outputs.
<!-- END_TF_DOCS -->
