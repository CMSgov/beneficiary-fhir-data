# BFD Insights for BCDA

## Contibuting

Before pushing changes to this project's terraform code, we ask that you please run the following commands from this
project's base directory:

* `terraform fmt`
* `terraform-docs markdown table .`

The first command will format the code according to the terraform style guide. The second command will update the table
in this README with the latest terraform inputs and outputs. Please commit these along with your code changes.

If you do not have `terraform-docs` installed, you can install it with `brew install terraform-docs` on MacOS, or by
following the instructions [here](https://github.com/terraform-docs/terraform-docs/) for your system.

## Terraforming resources

### Requirements

* [Terraform](https://www.terraform.io/downloads.html)
* [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
* [aws-vault](https://github.com/99designs/aws-vault)

Optional, but recommended to make installing the correct version of terraform easier:

* [tfswitch](https://tfswitch.warrensbox.com/Installation) or [tfenv](https://github.com/tfutils/tfenv)

Example on macOS using [Homebrew](https://brew.sh/):

```bash
brew install terraform awscli aws-vault tfswitch
mkdir -p ~/.aws
touch ~/.aws/config
aws-vault add bfd # add your BFD access credentials to your systems keychain
```

Note that managing your terraform requires MFA authentication to AWS. The following steps will help you get started.

### 1. Configure AWS CLI

After adding your BFD credentials to your systems keychain, make the following changes to your `~/.aws/config` file,
changing all occurences of `$BFD_ACCOUNT_ID` to the BFD account id (get from the team), and `$YOUR_EUA_USERNAME` to your
EUA username:

```ini
# Add a default section if not already present. This example sets the default region to us-east-1.
[default]
default_region = us-east-1

[profile bfd]
region = us-east-1
mfa_serial = arn:aws:iam::$BFD_ACCOUNT_ID:mfa/$YOUR_EUA_USERNAME
credential_process = aws-vault export --format=json bfd
output = json

[profile bfd-insights-developer]
source_profile = bfd-auth
mfa_serial = arn:aws:iam::$BFD_ACCOUNT_ID:mfa/$YOUR_EUA_USERNAME
role_arn = arn:aws:iam::$BFD_ACCOUNT_ID:role/bfd-insights-bcda-developer
role_session_name = $YOUR_EUA_USERNAME
```

### Assuming the role

You can now assume the role with the following command:

```bash
aws-vault exec bfd-insights-developer --duration=8h --assume-role-ttl=1h --
terraform plan
```

### Need Help?

If you encounter any issues or have questions, please reach out to our ops team! And feel free to customize this
document above to cater to your team's needs, but do not modify below this line as it's automatically updated with the
`terraform-docs markdown table .` command.

## Terraform Docs

| Key | Description | Data Source |
|-----|-------------|-------------|
| `/bcda/insights/$env/cross_account_role_arn` | BCDA Cross Account Role ARN | `data.aws_ssm_parameter.bcda_cross_account_role_arn` |

<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 4.30 |

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Inputs

No inputs.

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->



<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Outputs

No outputs.

<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_cloudwatch_event_rule.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_event_rule) | resource |
| [aws_cloudwatch_event_target.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_event_target) | resource |
| [aws_cloudwatch_log_group.bcda_load_partitions](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudwatch_log_group) | resource |
| [aws_iam_group.dev](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_group) | resource |
| [aws_iam_group_policy.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_group_policy) | resource |
| [aws_iam_policy.dev](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy.terraform](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_role.bcda_load_partitions](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role.dev](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role) | resource |
| [aws_iam_role_policy_attachment.dev](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_iam_role_policy_attachment.terraform](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_role_policy_attachment) | resource |
| [aws_lambda_function.bcda_load_partitions](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_function) | resource |
| [aws_lambda_permission.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/lambda_permission) | resource |
| [archive_file.bcda_load_partitions](https://registry.terraform.io/providers/hashicorp/archive/latest/docs/data-sources/file) | data source |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_kms_alias.moderate_cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_alias) | data source |
| [aws_region.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/region) | data source |
| [aws_s3_bucket.moderate_bucket](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/s3_bucket) | data source |
| [aws_ssm_parameter.cross_account_arns](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) | data source |
<!-- END_TF_DOCS -->
