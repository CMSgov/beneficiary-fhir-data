# BFD Base Environment Definitions

This module is the _service_ responsible for defining a BFD environment through AWS SSM Parameter Store.
This is supported by a [terraform workspaces-enabled](https://www.terraform.io/language/state/workspaces) state.
**If you're manipulating this state manually, you must verify that you're operating in the appropriate workspace for the targeted environment.**

## General

### Overview

Configuration management can get away from us, becoming wildly complex if there isn't a conscious effort to _keep things simple_.
While simplicity is in the eye of the beholder, this module seeks to be as simple as possible and no simpler.
To that end, this module will primarily be composed of two files, `sensitive.tf` and `nonsensitive.tf` that configure sensitive and nonsensitve values, respectively.
At this writing, the logic presented here is only supportive of sensitive values.
Additional terraform instructions are housed in somewhat canonical, expected files, e.g. `main.tf`.
This _generally_ takes values from specific, yaml-formatted files stored in the [values directory](.values), and produces AWS SSM Parameters.

Each environment is configured in a terraform workspace, named for the environment it defines.
There are four, known _established environments_ (`local.established_envs`) that enjoy _special_ treatment.
Their respective configurations come from appropriately named, environment-specific yaml files found in the aforementioned values directory.

To summarize, terraform reads encrypted **and** plaintext yaml-encoded _key:value_ pairs and stores environment-specific AWS SSM Parameter Store _parameter-path:parameter-value_ pairs.

### Formatting and Validation

As of mid July 2022, technical controls for standards enforcement are still forthcoming. As a stopgap, here are some guidelines in the spirit of keeping things simple:
- hierarchies or paths conform to a 4-tuple prefix and leaf `/bfd/${env}/${group}/sensitive/${leaf}` format <!-- NOTE: _nonsensitive support is forthcoming_ -->
- `${env}` must be one of `test`, `prod-sbx`, or `prod` <!-- NOTE: _full ephemeral environment support is forthcoming_ -->
- `${group}` must be one of the supported groups: `common`, `migrator`, `pipeline`, `server`
- `${leaf}` _should_ be lower_snake_case formatted
- if the hierarchy should match the _regex_ `/ami.id/`, the value [**must** point to an existing Amazon Machine Image](https://docs.aws.amazon.com/systems-manager/latest/userguide/parameter-store-ec2-aliases.html#parameter-ami-validation)
- only string-formatted values only are accepted
- sensitive values must be encrypted with appropriate [AWS Key Management Service-stored CMK](https://us-east-1.console.aws.amazon.com/kms/home?region=us-east-1#/kms/keys)

**As of mid July 2022, this module only manages sensitive configuration. Nonsensitive configuration is forthcoming.**

### Usage and User Additions

If the below [prerequisites](#prerequisites) are met, users will _generally_ interact with the environment-specific configuration by using one or more scripts in the [scripts](./scripts) directory.

#### Viewing with read-and-decrypt-eyaml.sh
**WARNING:** This will present unencrypted, sensitive data to stdout. Do not execute this while sharing your screen during presentations or pairing opportunities.

To see the raw, _untemplated_ configuration as terraform does through via external data source for e.g. `./values/prod-sbx.eyaml`, execute the following from the module root directory:

```sh
scripts/read-and-decrypt-eyaml.sh prod-sbx
```

#### Editing with edit-eyaml.sh and Updating with terraform
To edit the encrypted values under e.g. `./values/prod-sbx.eyaml` use the following steps:
1. Select the appropriate workspace: `terraform workspace select prod-sbx`
2. Ensure a familiar editor is defined in your environment, e.g. `export EDITOR=vim`
3. Run the edit script from the module root directory: `scripts/edit-eyaml.sh prod-sbx`
4. Save and quit after making any desired changes
5. Review updates using the read script module root directory: `scripts/read-and-decrypt-eyaml.sh prod-sbx`
6. Ensure terraform can successfully plan by running `terraform plan`
7. Commit your changes to an appropriate feature branch
8. Solicit feedback by pull request

**NOTE:** As of this writing, automated workflows through a typical CI process are not yet defined. Please see [BFD-1786](https://jira.cms.gov/browse/BFD-1786) for forthcoming implementation details.

### Prerequisites
In addition to the [Requirements (below)](#requirements), you (or the automation) will need:
- software packages supporting awscli, yq, and jq
- sufficient access to the `/bfd/mgmt/jenkins/sensitive` hierarchy for ansible-vault password access
- `ansible` installed with `ansible-vault` available along your path (as of this writing, `ansible ~> 2.9`)
- sufficient AWS IAM privileges for the AWS provider [Resources and Date Sources (below)](#resources)
- access outlined for the remote [AWS S3 Backend](https://www.terraform.io/language/settings/backends/s3#s3-bucket-permissions)
- read/write privileges to the state-locking [AWS DynamoDB Table](https://www.terraform.io/language/settings/backends/s3#dynamodb-table-permissions)

<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | = 4.17 |

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



<!-- GENERATED WITH `terraform-docs .`
Manually updating the README.md will be overwritten.
For more details, see the file '.terraform-docs.yml' or
https://terraform-docs.io/user-guide/configuration/
-->

## Resources

| Name | Type |
|------|------|
| [aws_ssm_parameter.common_sensitive](https://registry.terraform.io/providers/hashicorp/aws/4.17/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.migrator_sensitive](https://registry.terraform.io/providers/hashicorp/aws/4.17/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.pipeline_sensitive](https://registry.terraform.io/providers/hashicorp/aws/4.17/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.server_sensitive](https://registry.terraform.io/providers/hashicorp/aws/4.17/docs/resources/ssm_parameter) | resource |
| [aws_kms_key.cmk](https://registry.terraform.io/providers/hashicorp/aws/4.17/docs/data-sources/kms_key) | data source |
| [external_external.eyaml](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
<!-- END_TF_DOCS -->
