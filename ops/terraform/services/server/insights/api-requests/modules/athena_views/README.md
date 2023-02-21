# `athena_views` Submodule

This submodule defines the various [Athena Views](https://docs.aws.amazon.com/athena/latest/ug/views.html)
that make working with BFD Insights data a little more ergonomic.

Terraform has no built-in resource for defining these views, and while there _are_ workarounds
(using the `aws_glue_catalog_table` resource and some undocumented "magic", see
[here](https://stackoverflow.com/a/56347331)) to allow for Terraform to define views they are,
unfortunately, not applicable to our specific use-case (due to the `api_requests` view dynamically
`SELECT`ing all columns from its corresponding table). Instead, Terraform's `null_resource` resource
and `local-exec` provisioners are used with the `local-exec` provisioners executing a custom-made
`bash` script that runs the appropriate AWS CLI commands to create and destroy Athena Views. This
solution is not _quite_ as optimal as strictly defining each view in Terraform using `resource`s
but works well enough for our use-case.

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
| [null_resource.athena_view_api_requests](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.athena_view_api_requests_by_bene](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
| [null_resource.athena_view_new_benes_by_day](https://registry.terraform.io/providers/hashicorp/null/latest/docs/resources/resource) | resource |
<!-- END_TF_DOCS -->
