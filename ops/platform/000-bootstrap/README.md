# `bootstrap` Platform Terraservice

This Platform Terraservice is intended to be the _very_ first OpenTofu `apply`'d in any new BFD AWS Account; as such, it has been granted a special "layer prefix" of `000`.

Because OpenTofu requires state to function, and a new BFD AWS Account will have no resources (that we control) within it, there must be _some_ mechanism to "boostrap" ourselves so that we can begin `apply`ing our OpenTofu IaC. This Terraservice is the means by which we do this. Once `bootstrap` has been `apply`'d, _all_ of the fundamental resources (KMS keys, state S3 Buckets, CodeBuild Pipelines) will exist such that it will be possible to `apply` all other OpenTofu.

## Bootstrapping a new Account

Since this Terraservice is special and must be applied prior to any of the state Buckets that the `root.tofu.tf` expects, a `local-tofu.tf` file is provided within this Terraservice's directory. Operators boostrapping a new AWS Account must follow these steps:

1. _Temporarily_ replace the symlink'd `tofu.tf` with the contents of `local-tofu.tf`
1. `tofu init` the `local` state
1. `apply` this Terraservice
1. Restore the symlink'd `tofu.tf` and rename `local-tofu.tf` to `local-tofu.tf.disabled`
1. `tofu init -var account_type=<ACCOUNT_TYPE> -reconfigure` to ensure that the appopriate state Bucket, created by _this_ Terraservice, is used
1. Push the `local` state into the remote `s3` state backend: `tofu state push terraform.tfstate`

Following these steps ensures that the boostrapped resources are stored in state using the S3 Bucket and KMS key created by this Terraservice. This also enables us to automatically deploy _changes_ to these resources (and other fundamental resources unrelated to OpenTofu state management) in our CI/CD Workflows, provided that these changes do not fundamentally alter the OpenTofu state resources.

<!-- BEGIN_TF_DOCS -->
<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 5.9 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_account_type"></a> [account\_type](#input\_account\_type) | The account type being targeted to create platform resources within. Will correspond with<br/>`terraform.workspace`. Necessary on `tofu init` and `tofu workspace select` \_only\_. In all other<br/>situations, the account type will be divined from `terraform.workspace`. | `string` | `null` | no |
| <a name="input_greenfield"></a> [greenfield](#input\_greenfield) | Temporary feature flag enabling compatibility for applying Terraform in the legacy and Greenfield accounts. Will be removed when Greenfield migration is completed. | `bool` | `false` | no |
| <a name="input_region"></a> [region](#input\_region) | n/a | `string` | `"us-east-1"` | no |
| <a name="input_secondary_region"></a> [secondary\_region](#input\_secondary\_region) | n/a | `string` | `"us-west-2"` | no |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Modules

| Name | Source | Version |
|------|--------|---------|
| <a name="module_bucket_tf_state"></a> [bucket\_tf\_state](#module\_bucket\_tf\_state) | ../../terraform-modules/general/secure-bucket | n/a |
| <a name="module_terraservice"></a> [terraservice](#module\_terraservice) | ../../terraform-modules/bfd/bfd-platform-service | n/a |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_kms_alias.primary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/kms_alias) | resource |
| [aws_kms_alias.secondary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/kms_alias) | resource |
| [aws_kms_key.primary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/kms_key) | resource |
| [aws_kms_key.secondary](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/kms_key) | resource |
| [aws_iam_policy_document.autoscaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.combined](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.data](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.default_kms_key_policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_policy_document.rds_autoscaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_iam_roles.autoscaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_roles) | data source |
| [aws_iam_roles.cpm](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_roles) | data source |
| [aws_iam_roles.rds_autoscaling](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_roles) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->
