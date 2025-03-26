<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | 1.5.0 |
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~>5 |
| <a name="requirement_external"></a> [external](#requirement\_external) | ~>2 |
| <a name="requirement_http"></a> [http](#requirement\_http) | ~>3 |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_environment_name"></a> [environment\_name](#input\_environment\_name) | The solution's environment name. Generally, `terraform.workspace` | `string` | n/a | yes |
| <a name="input_relative_module_root"></a> [relative\_module\_root](#input\_relative\_module\_root) | The solution's relative path from the root of beneficiary-fhir-data repository | `string` | n/a | yes |
| <a name="input_service"></a> [service](#input\_service) | Service _or_ terraservice name. | `string` | n/a | yes |
| <a name="input_additional_tags"></a> [additional\_tags](#input\_additional\_tags) | Additional tags to merge into final default\_tags output | `map(string)` | `{}` | no |
| <a name="input_ssm_hierarchy_roots"></a> [ssm\_hierarchy\_roots](#input\_ssm\_hierarchy\_roots) | List of SSM Hierarchy roots. Module executes a recursive lookup for all roots for `common` and service-specific hierarchies. | `list(string)` | <pre>[<br/>  "bfd"<br/>]</pre> | no |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Modules

No modules.

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_iam_policy.permissions_boundary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy) | data source |
| [aws_kms_key.env_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_kms_key.env_config_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_ssm_parameters_by_path.params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [external_external.github_token](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
| [http_http.latest_bfd_release](https://registry.terraform.io/providers/hashicorp/http/latest/docs/data-sources/http) | data source |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_account_id"></a> [account\_id](#output\_account\_id) | The account ID associated with the current caller identity |
| <a name="output_default_iam_path"></a> [default\_iam\_path](#output\_default\_iam\_path) | Default path for IAM policies and roles. |
| <a name="output_default_permissions_boundary_arn"></a> [default\_permissions\_boundary\_arn](#output\_default\_permissions\_boundary\_arn) | ARN of the default permissions boundary for IAM Roles. |
| <a name="output_default_tags"></a> [default\_tags](#output\_default\_tags) | n/a |
| <a name="output_env"></a> [env](#output\_env) | The solution's environment name. Generally, `terraform.workspace` |
| <a name="output_env_config_key_alias"></a> [env\_config\_key\_alias](#output\_env\_config\_key\_alias) | Alias name for the current environment's configuration-specific, multi-region CMK. |
| <a name="output_env_config_key_arns"></a> [env\_config\_key\_arns](#output\_env\_config\_key\_arns) | ARNs of the current environment's configuration-specific, multi-region CMK. |
| <a name="output_env_key_alias"></a> [env\_key\_alias](#output\_env\_key\_alias) | Alias name for the current environment's general-purpose CMK. |
| <a name="output_env_key_arn"></a> [env\_key\_arn](#output\_env\_key\_arn) | ARN of the current environment's general-purpose CMK. |
| <a name="output_is_ephemeral_env"></a> [is\_ephemeral\_env](#output\_is\_ephemeral\_env) | Returns true when environment is \_ephemeral\_, false when \_established\_ |
| <a name="output_latest_bfd_release"></a> [latest\_bfd\_release](#output\_latest\_bfd\_release) | This is the latest CMSgov/beneficiary-fhir-data release. Excludes Pre-Releases. |
| <a name="output_region"></a> [region](#output\_region) | The region name associated with the current caller identity |
| <a name="output_seed_env"></a> [seed\_env](#output\_seed\_env) | The solution's source environment. For established environments this is equal to the environment's name |
| <a name="output_service"></a> [service](#output\_service) | The name of the current Terraservice |
| <a name="output_ssm_config"></a> [ssm\_config](#output\_ssm\_config) | Parameter:Value map that elides repetitive keys, e.g. ssm:/bfd/test/common/vpc\_name is /bfd/comon/vpc\_name |
<!-- END_TF_DOCS -->
