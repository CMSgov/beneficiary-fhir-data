# `server` Terraservice

This "Terraservice" encapsulates the Terraform necessary to create AWS infrastructure related to the BFD Server. This Terraservice explicitly depends upon the `base` and `common` Terraservices. Ephemeral environments are supported through usage of `terraform workspace`s.

## Usage

The `base` and `common` Terraservices _must_ be applied, to the _same Terraform workspace_, prior to `apply`ing this Terraservice. This Terraservice relies on the resources created by those Terraservices, and will fail to apply if they do not exist.

Assuming you have created a Terraform workspace corresponding to your target environment and have switched to it, this Terraservice can be applied without specifying any variables using:

```bash
terraform apply
terraform apply
```

**NOTE** the above double-invocation of terraform is correct. Two executions of `terraform apply` are necessary to achieve the desired state as of BFD-2558.

## Blue/Green Workflow

This Terraservice implements the logic and resources necessary to support a Blue/Green Deployment strategy for the BFD Server.

Blue (`blue`) refers to the "active" or _production_ infrastructure that serves traffic to our consumers. Resources in `blue` are considered to "known-good" resources. Green (`green`) refers to _incoming_, new infrastructure for a _new_ version of the BFD Server that needs to be verified as good before it being promoted to `blue` and made available to serve traffic to our consumers.

This Terraservice achieves a Blue/Green Deployment strategy by utilizing two AutoScaling Groups, two Target Groups, two Load Balancer Listeners, and two Network Load Balancers. Each NLB routes to its respective `green`/`blue` Listener which routes to the respective `green`/`blue` Target Group. If an environment is "public" (internet-facing) only the `blue` NLB will be internet-facing, whereas the `green` will remain internal. This way, external consumers will only be able to reach `blue` BFD Server Instances, while our automation can reach the `green` Instances to verify them.

The Terraservice logic decides which AutoScaling Group is associated with the `blue`/`green` Target Group by looking at the oddness/evenness of the _latest_ Launch Template version number _iff_ the Launch Template is changing upon the `terraform apply`. Correspondingly, the ASGs are suffixed with `-odd` and `-even`. Given latest Launch Template version number, if it is _odd_ the ASG suffixed as `-odd` will be chosen as `green` whereas if it is _even_ `-even` will be chosen as `green`. In this scenario, we expect no changes to the existing `blue` ASG nor its Target Group so that it continues to serve traffic uninterrupted.

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
| <a name="input_ami_id_override"></a> [ami\_id\_override](#input\_ami\_id\_override) | BFD Server override ami-id. Defaults to latest server/fhir AMI from `master`. | `string` | `null` | no |
| <a name="input_db_environment_override"></a> [db\_environment\_override](#input\_db\_environment\_override) | For use in database maintenance contexts only. | `string` | `null` | no |
| <a name="input_force_create_server_dashboards"></a> [force\_create\_server\_dashboards](#input\_force\_create\_server\_dashboards) | Forces the creation of bfd\_server\_dashboards for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_disk_alarms"></a> [force\_create\_server\_disk\_alarms](#input\_force\_create\_server\_disk\_alarms) | Forces the creation of bfd\_server\_disk\_alarms for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_error_alerts"></a> [force\_create\_server\_error\_alerts](#input\_force\_create\_server\_error\_alerts) | Forces the creation of bfd\_server\_error\_alerts for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_lb_alarms"></a> [force\_create\_server\_lb\_alarms](#input\_force\_create\_server\_lb\_alarms) | Forces the creation of bfd\_server\_lb\_alarms for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_log_alarms"></a> [force\_create\_server\_log\_alarms](#input\_force\_create\_server\_log\_alarms) | Forces the creation of bfd\_server\_log\_alarms for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_metrics"></a> [force\_create\_server\_metrics](#input\_force\_create\_server\_metrics) | Forces the creation of bfd\_server\_metrics for ephemeral environments | `bool` | `false` | no |
| <a name="input_force_create_server_slo_alarms"></a> [force\_create\_server\_slo\_alarms](#input\_force\_create\_server\_slo\_alarms) | Forces the creation of bfd\_server\_slo\_alarms for ephemeral environments | `bool` | `false` | no |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_bfd_dashboards"></a> [bfd\_dashboards](#module\_bfd\_dashboards) | ./modules/bfd_server_dashboards | n/a |
| <a name="module_bfd_server_error_alerts"></a> [bfd\_server\_error\_alerts](#module\_bfd\_server\_error\_alerts) | ./modules/bfd_server_error_alerts | n/a |
| <a name="module_bfd_server_log_alarms"></a> [bfd\_server\_log\_alarms](#module\_bfd\_server\_log\_alarms) | ./modules/bfd_server_log_alarms | n/a |
| <a name="module_bfd_server_logs"></a> [bfd\_server\_logs](#module\_bfd\_server\_logs) | ./modules/bfd_server_logs | n/a |
| <a name="module_bfd_server_metrics"></a> [bfd\_server\_metrics](#module\_bfd\_server\_metrics) | ./modules/bfd_server_metrics | n/a |
| <a name="module_bfd_server_slo_alarms"></a> [bfd\_server\_slo\_alarms](#module\_bfd\_server\_slo\_alarms) | ./modules/bfd_server_slo_alarms | n/a |
| <a name="module_disk_usage_alarms"></a> [disk\_usage\_alarms](#module\_disk\_usage\_alarms) | ./modules/bfd_server_disk_alarms | n/a |
| <a name="module_fhir_asg"></a> [fhir\_asg](#module\_fhir\_asg) | ./modules/bfd_server_asg | n/a |
| <a name="module_fhir_iam"></a> [fhir\_iam](#module\_fhir\_iam) | ./modules/bfd_server_iam | n/a |
| <a name="module_fhir_lb"></a> [fhir\_lb](#module\_fhir\_lb) | ./modules/bfd_server_lb | n/a |
| <a name="module_lb_alarms"></a> [lb\_alarms](#module\_lb\_alarms) | ./modules/bfd_server_lb_alarms | n/a |
| <a name="module_terraservice"></a> [terraservice](#module\_terraservice) | git::https://github.com/CMSgov/beneficiary-fhir-data.git//ops/terraform/services/_modules/bfd-terraservice | 2.181.0 |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_s3_bucket.certstores](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket) | resource |
| [aws_s3_bucket_policy.certstores](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_policy) | resource |
| [aws_s3_bucket_public_access_block.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_public_access_block) | resource |
| [aws_s3_bucket_server_side_encryption_configuration.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_server_side_encryption_configuration) | resource |
| [aws_s3_object.keystore](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_object) | resource |
| [aws_s3_object.truststore](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_object) | resource |
| [null_resource.generate_truststore](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [terraform_data.keystore_object_size](https://registry.terraform.io/providers/hashicorp/terraform/latest/docs/resources/data) | resource |
| [aws_ami.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ami) | data source |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_ec2_managed_prefix_list.jenkins](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ec2_managed_prefix_list) | data source |
| [aws_ec2_managed_prefix_list.vpn](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ec2_managed_prefix_list) | data source |
| [aws_iam_policy_document.certstores_bucket_policy_doc](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_kms_key.env_key](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_security_group.remote](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.tools](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.vpn](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_groups.aurora_cluster](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_groups) | data source |
| [aws_ssm_parameters_by_path.client_certificates](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_ssm_parameters_by_path.nonsensitive_common](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_ssm_parameters_by_path.nonsensitive_service](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_ssm_parameters_by_path.sensitive_service](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_vpc.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [aws_vpc.mgmt](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [aws_vpc_peering_connection.peers](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc_peering_connection) | data source |
| [external_external.keystore_object_size](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
| [external_external.truststore_object_size](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Outputs

No outputs.
<!-- END_TF_DOCS -->
