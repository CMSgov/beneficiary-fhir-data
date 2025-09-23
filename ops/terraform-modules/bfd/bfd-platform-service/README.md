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
| <a name="input_greenfield"></a> [greenfield](#input\_greenfield) | Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed. | `bool` | `false` | no |
| <a name="input_lookup_kms_key"></a> [lookup\_kms\_key](#input\_lookup\_kms\_key) | Toggles whether or not this module does data lookups for the platform KMS key.<br/>If false, the KMS-related outputs will all be null. Set to false for services that create the key<br/>or are otherwise applied prior to the keys existing | `bool` | `true` | no |
| <a name="input_ssm_hierarchy_roots"></a> [ssm\_hierarchy\_roots](#input\_ssm\_hierarchy\_roots) | List of SSM Hierarchy roots. Module executes a recursive lookup for all roots for `common` and service-specific hierarchies. | `list(string)` | <pre>[<br/>  "bfd"<br/>]</pre> | no |

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
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_iam_account_alias.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_account_alias) | data source |
| [aws_iam_policy.permissions_boundary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_kms_key.platform](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_ssm_parameters_by_path.params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [external_external.bfd_version](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

| Name | Description |
|------|-------------|
| <a name="output_account_alias"></a> [account\_alias](#output\_account\_alias) | The account ID associated with the current caller identity |
| <a name="output_account_id"></a> [account\_id](#output\_account\_id) | The account ID associated with the current caller identity |
| <a name="output_bfd_version"></a> [bfd\_version](#output\_bfd\_version) | The BFD version that is being deployed. Corresponds to the most recent tag of the checked-out repository |
| <a name="output_canary"></a> [canary](#output\_canary) | Canary output used to ensure that any Terraservice using the root.tofu.tf also uses this module. |
| <a name="output_default_iam_path"></a> [default\_iam\_path](#output\_default\_iam\_path) | Default path for IAM policies and roles. |
| <a name="output_default_permissions_boundary_arn"></a> [default\_permissions\_boundary\_arn](#output\_default\_permissions\_boundary\_arn) | ARN of the default permissions boundary for IAM Roles. |
| <a name="output_default_tags"></a> [default\_tags](#output\_default\_tags) | n/a |
| <a name="output_key_alias"></a> [key\_alias](#output\_key\_alias) | Alias name for the platform general-purpose CMK. |
| <a name="output_key_arn"></a> [key\_arn](#output\_key\_arn) | ARN of the current region's primary platform CMK. |
| <a name="output_region"></a> [region](#output\_region) | The region name associated with the current caller identity |
| <a name="output_service"></a> [service](#output\_service) | The name of the current Terraservice |
| <a name="output_ssm_config"></a> [ssm\_config](#output\_ssm\_config) | Parameter:Value map that elides repetitive keys, e.g. ssm:/bfd/test/common/vpc\_name is /bfd/comon/vpc\_name |
<!-- END_TF_DOCS -->
