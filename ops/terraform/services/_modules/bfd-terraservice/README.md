<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_terraform"></a> [terraform](#requirement_terraform) | 1.5.0 |
| <a name="requirement_aws"></a> [aws](#requirement_aws) | ~>5 |
| <a name="requirement_external"></a> [external](#requirement_external) | ~>2 |
| <a name="requirement_http"></a> [http](#requirement_http) | ~>3 |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_environment_name"></a> [environment_name](#input_environment_name) | The solution's environment name. Generally, `terraform.workspace` | `string` | n/a | yes |
| <a name="input_relative_module_root"></a> [relative_module_root](#input_relative_module_root) | The solution's relative path from the root of beneficiary-fhir-data repository | `string` | n/a | yes |
| <a name="input_service"></a> [service](#input_service) | Service _or_ terraservice name. | `string` | n/a | yes |
| <a name="input_additional_tags"></a> [additional_tags](#input_additional_tags) | Additional tags to merge into final default_tags output | `map(string)` | `{}` | no |
| <a name="input_ssm_hierarchy_roots"></a> [ssm_hierarchy_roots](#input_ssm_hierarchy_roots) | List of SSM Hierarchy roots. Module executes a recursive lookup for all roots for `common` and service-specific hierarchies. | `list(string)` | <pre>[<br>  "bfd"<br>]</pre> | no |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_default_tags"></a> [default_tags](#output_default_tags) | n/a |
| <a name="output_env"></a> [env](#output_env) | The solution's environment name. Generally, `terraform.workspace` |
| <a name="output_is_ephemeral_env"></a> [is_ephemeral_env](#output_is_ephemeral_env) | Returns true when environment is _ephemeral_, false when _established_ |
| <a name="output_latest_bfd_release"></a> [latest_bfd_release](#output_latest_bfd_release) | This is the latest CMSgov/beneficiary-fhir-data release. Excludes Pre-Releases. |
| <a name="output_seed_env"></a> [seed_env](#output_seed_env) | The solution's source environment. For established environments this is equal to the environment's name |
| <a name="output_ssm_config"></a> [ssm_config](#output_ssm_config) | Parameter:Value map that elides repetitive keys, e.g. ssm:/bfd/test/common/vpc_name is /bfd/comon/vpc_name |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_ssm_parameters_by_path.params](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [external_external.github_token](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
| [http_http.latest_bfd_release](https://registry.terraform.io/providers/hashicorp/http/latest/docs/data-sources/http) | data source |
<!-- END_TF_DOCS -->