# `base_config` Module

**Note: This module _must_ be `apply`'d prior to `apply`ing the `mgmt` module**

This module is a _distinct_, seperate module within the `mgmt` environment's Terraform module that defines global SSM parameter data
belonging to the `mgmt` "environment". Such data is shared between _all_ environments and services,
and so duplicating it across environments is unnecessary. If data needs to be accessed in multiple
environments, and its value should not change between them, then it should probably live in this
module's `values`.

The techniques for configuration management have been adapted from the existing work done in the
`base` Terraform service module. See its [README](../../../services/base/README.md) for more
information on known limitations with this strategy as well as proper formatting for SSM values. The
sections detailing how to read and decrypt, and edit encrypted YAML apply for this module as well,
as this module contains similar scripts for doing this work.

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

## Resources

| Name | Type |
|------|------|
| [aws_ssm_parameter.common_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_kms_key.cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [external_external.eyaml](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
<!-- END_TF_DOCS -->
