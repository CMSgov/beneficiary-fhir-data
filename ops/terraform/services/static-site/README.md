# `static-site` Terraservice

This Terraservice contains the Terraform IaC for BFD's static-site documentation infrastructure. 

## Usage

The `base` and `common` Terraservices _must_ be applied, to the _same Terraform workspace_, prior to `apply`ing this Terraservice. This Terraservice relies on the resources created by those Terraservices, and will fail to apply if they do not exist.

Assuming you have created a Terraform workspace corresponding to your target environment and have switched to it, this Terraservice can be applied without specifying any variables using:

```bash
terraform apply
```

<!-- BEGIN_TF_DOCS -->


<!-- END_TF_DOCS -->
