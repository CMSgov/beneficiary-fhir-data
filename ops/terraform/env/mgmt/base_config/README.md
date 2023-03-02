# `base_config` Submodule

This submodule of the `mgmt` environment's Terraform module defines global SSM parameter data
belonging to the `mgmt` "environment". Such data is shared between _all_ environments and services,
and so duplicating it across environments is unnecessary. If data needs to be accessed in multiple
environments, and its value should not change between them, then it should probably live in this
module's `values`.

The techniques for configuration management have been adapted from the existing work done in the
`base` Terraform service module. See its [README](../../../services/base/README.md) for more
information on known limitations with this strategy as well as proper formatting for SSM values.

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

| Name | Type |
|------|------|
| [aws_ssm_parameter.common_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [external_external.eyaml](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
<!-- END_TF_DOCS -->
