<!-- BEGIN_TF_DOCS -->
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
| <a name="input_additional_tags"></a> [additional_tags](#input_additional_tags) | Additional tags to merge into final default_tags output | `map(string)` | `{}` | no |

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
| <a name="output_seed_env"></a> [seed_env](#output_seed_env) | The solution's source environment. For established environments this is equal to the environment's name |
<!-- END_TF_DOCS -->