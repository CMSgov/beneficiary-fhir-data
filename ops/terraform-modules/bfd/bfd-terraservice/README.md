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
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~>6 |
| <a name="requirement_external"></a> [external](#requirement\_external) | ~>2 |
| <a name="requirement_http"></a> [http](#requirement\_http) | ~>3 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_relative_module_root"></a> [relative\_module\_root](#input\_relative\_module\_root) | The solution's relative path from the root of beneficiary-fhir-data repository | `string` | n/a | yes |
| <a name="input_service"></a> [service](#input\_service) | Service _or_ terraservice name. | `string` | n/a | yes |
| <a name="input_additional_tags"></a> [additional\_tags](#input\_additional\_tags) | Additional tags to merge into final default\_tags output | `map(string)` | `{}` | no |
| <a name="input_lookup_kms_keys"></a> [lookup\_kms\_keys](#input\_lookup\_kms\_keys) | Toggles whether or not this module does data lookups for the platform and current env KMS keys.<br/>If false, the KMS-related outputs will all be null. Set to false for services that create the keys<br/>or are otherwise applied prior to the keys existing | `bool` | `true` | no |
| <a name="input_ssm_hierarchy_roots"></a> [ssm\_hierarchy\_roots](#input\_ssm\_hierarchy\_roots) | List of SSM Hierarchy roots. Module executes a recursive lookup for all roots for `common` and service-specific hierarchies. | `list(string)` | <pre>[<br/>  "bfd"<br/>]</pre> | no |
| <a name="input_subnet_layers"></a> [subnet\_layers](#input\_subnet\_layers) | List of subnet 'layers' (app, data, dmz, etc.) from which each subnet associated with that layer will be looked up. | `list(string)` | `[]` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

No modules.

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_availability_zone.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/availability_zone) | data source |
| [aws_availability_zones.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/availability_zones) | data source |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_iam_policy.permissions_boundary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_kms_key.env](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_kms_key.platform](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_security_group.cms_cloud_security_tools](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.cms_cloud_shared_services](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_security_group.cms_cloud_vpn](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/security_group) | data source |
| [aws_ssm_parameters_by_path.params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_subnet.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnet) | data source |
| [aws_subnets.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnets) | data source |
| [aws_vpc.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [aws_vpc_peering_connection.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc_peering_connection) | data source |
| [aws_vpc_peering_connections.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc_peering_connections) | data source |
| [aws_vpcs.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpcs) | data source |
| [external_external.bfd_version](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

| Name | Description |
|------|-------------|
| <a name="output_account_id"></a> [account\_id](#output\_account\_id) | The account ID associated with the current caller identity |
| <a name="output_all_connections"></a> [all\_connections](#output\_all\_connections) | Map of all peering connections in all VPCs to their properties |
| <a name="output_bfd_version"></a> [bfd\_version](#output\_bfd\_version) | The BFD version that is being deployed. Corresponds to the most recent tag of the checked-out repository |
| <a name="output_canary"></a> [canary](#output\_canary) | Canary output used to ensure that any Terraservice using the root.tofu.tf also uses this module. |
| <a name="output_cms_cloud_security_tools_sg"></a> [cms\_cloud\_security\_tools\_sg](#output\_cms\_cloud\_security\_tools\_sg) | The OIT/CMS Cloud provided Security Tools Security Group (data.aws\_security\_group). |
| <a name="output_cms_cloud_shared_services_sg"></a> [cms\_cloud\_shared\_services\_sg](#output\_cms\_cloud\_shared\_services\_sg) | The OIT/CMS Cloud provided Shared Services Security Group (data.aws\_security\_group). |
| <a name="output_cms_cloud_vpn_sg"></a> [cms\_cloud\_vpn\_sg](#output\_cms\_cloud\_vpn\_sg) | The OIT/CMS Cloud provided VPN Security Group (data.aws\_security\_group). |
| <a name="output_default_azs"></a> [default\_azs](#output\_default\_azs) | Key-value map of AZ names to their attributes (data.aws\_availability\_zone) of all default AZs that all BFD services exist in. |
| <a name="output_default_iam_path"></a> [default\_iam\_path](#output\_default\_iam\_path) | Default path for IAM policies and roles. |
| <a name="output_default_permissions_boundary_arn"></a> [default\_permissions\_boundary\_arn](#output\_default\_permissions\_boundary\_arn) | ARN of the default permissions boundary for IAM Roles. |
| <a name="output_default_tags"></a> [default\_tags](#output\_default\_tags) | n/a |
| <a name="output_env"></a> [env](#output\_env) | The solution's environment name. Generally, `terraform.workspace` |
| <a name="output_env_key_alias"></a> [env\_key\_alias](#output\_env\_key\_alias) | Alias name for the current environment's CMK. |
| <a name="output_env_key_arn"></a> [env\_key\_arn](#output\_env\_key\_arn) | ARN of the current region's primary environment CMK. |
| <a name="output_is_ephemeral_env"></a> [is\_ephemeral\_env](#output\_is\_ephemeral\_env) | Returns true when environment is \_ephemeral\_, false when \_established\_ |
| <a name="output_platform_key_alias"></a> [platform\_key\_alias](#output\_platform\_key\_alias) | Alias name for the platform CMK. |
| <a name="output_platform_key_arn"></a> [platform\_key\_arn](#output\_platform\_key\_arn) | ARN of the current region's primary platform CMK. |
| <a name="output_region"></a> [region](#output\_region) | The region name associated with the current caller identity |
| <a name="output_service"></a> [service](#output\_service) | The name of the current Terraservice |
| <a name="output_ssm_config"></a> [ssm\_config](#output\_ssm\_config) | Parameter:Value map that elides repetitive keys, e.g. ssm:/bfd/test/common/vpc\_name is /bfd/comon/vpc\_name |
| <a name="output_subnets_map"></a> [subnets\_map](#output\_subnets\_map) | Map of subnet group to the subnets (data.aws\_subnet) in that layer in the current environment's VPC. |
| <a name="output_vpc"></a> [vpc](#output\_vpc) | The current environment's VPC (data.aws\_vpc). |
<!-- END_TF_DOCS -->
