# BFD Base Environment Definitions

This module is the _service_ responsible for defining a BFD environment through AWS SSM Parameter Store.
This is supported by a [terraform workspaces-enabled](https://www.terraform.io/language/state/workspaces) state.
**If you're manipulating this state manually, you must verify that you're operating in the appropriate workspace for the targeted environment.**

## General

### Overview

Configuration management can get away from us, becoming wildly complex if there isn't a conscious effort to _keep things simple_.
While simplicity is in the eye of the beholder, this module seeks to be as simple as possible and no simpler.
To that end, this module will _primarily_ be composed of three files, `sensitive.tf`, `nonsensitive.tf`, and `ephemeral.tf`.
The former two terraform files deal with sensitive and nonsensitive values, respectively, while `ephemeral.tf` applies exclusively to ephemeral environments.

Additional terraform instructions are housed in somewhat canonical, expected files, e.g. `main.tf`, `variables.tf`, etc.
In general, this modules takes configuration settings from specific, yaml-formatted files stored in the [values directory](.values), and stores them under the appropriate AWS SSM Parameter Store hierarchy.

Each environment is configured in a terraform workspace, named for the environment it defines.
There are four, known _established environments_ (`local.established_envs`) with their respective configurations coming from appropriately named, environment-specific yaml and eyaml files found in the aforementioned values directory.
Ephemeral environments use a combination of the values stored in the `ephemeral.yaml` and effectively copy necessary values from their _seed_ environments. 

**As of early September 2022, ephemeral environment support is limited. Please work with BFD engineers working in the infrastructure space if you need ephemeral environment support.**

To summarize, terraform reads encrypted **and** plaintext yaml-encoded _<key>:<value>_ pairs and stores environment-specific AWS SSM Parameter Store _<parameter-path>:<parameter-value>_ pairs.

For more general documentation on formatting and usage, expand the `More...` below.

<details><summary>More...</summary>

### Known Limitations
AWS SSM Parameter Store has very limited support for storing non-string values in plain-text (`nonsensitive`) data and virtually no options for storing encrypted (`sensitive`)data.
This forces us to handle some data that would more naturally be represented as collections like maps and arrays as formatted string types.
To work with this, you might consider using spaces to delimit your collection and parse accordingly, which can easily be achieved using yaml's `>` _folding block_ for multi-line strings.
Other techniques might involve storing more complex data in formats that are more machine-readable, like JSON.
However between writing a JSON string to yaml here and being fetched from AWS SSM Parameter Store, it will be in an _escaped_ format and the data will need to be pre-processed.
`jq`'s `fromjson` function is especially handy for this kind of conversion.

### Formatting and Validation

As of mid-January 2023, technical controls for standards enforcement are still forthcoming. As a stopgap, here are some guidelines in the spirit of keeping things simple:
- hierarchies or paths generally conform to a 4 or 5 tuple prefix and leaf format, e.g.
  - `/bfd/${env}/${group}/${sensitivity}/${leaf}`
  - `/bfd/${env}/${group}/${subgroup}/${sensitivity}/${leaf}`
- `${env}` is typically one of `test`, `prod-sbx`, or `prod` but limited support for ephemeral environments exists. Guidance on ephemeral environment naming conventions is forthcoming.
- `${group}` must be one of the supported groups: `common`, `migrator`, `pipeline`, `server`
- `${subgroup}` is optional, as of January 2023, examples include `ccw`, `rda`, `shared`
- `${sensitivity}` must be one of `sensitive` when encrypted or `nonsensitive` when in plain text
- `${leaf}` _should_ be lower_snake_case formatted
- if the hierarchy should match the _regex_ `/ami.id/`, the value [**must** point to an existing Amazon Machine Image](https://docs.aws.amazon.com/systems-manager/latest/userguide/parameter-store-ec2-aliases.html#parameter-ami-validation)
- only string-formatted values are accepted
- empty strings, i.e '' are not supported
- we've adopted a _local_ convention where the literal `UNDEFINED` makes an SSM-derived value absent
- sensitive values must be encrypted with appropriate [AWS Key Management Service-stored CMK](https://us-east-1.console.aws.amazon.com/kms/home?region=us-east-1#/kms/keys)

### Usage and User Additions

If the below [prerequisites](#prerequisites) are met, users will _generally_ interact with the environment-specific configuration by using one or more scripts in the [scripts](./scripts) directory for those encrypted values (stored in `.eyaml`), otherwise a text-editor of their choosing when adjusting plain text values (stored in `.yaml`).

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
9. Follow the typical, monolithic release process via Jenkins

### Prerequisites
In addition to the [Requirements (below)](#requirements), you (or the automation) will need:
- software packages supporting awscli, yq, and jq
- sufficient access to the `/bfd/mgmt/jenkins/sensitive` hierarchy for ansible-vault password access
- `ansible` installed with `ansible-vault` available along your path (as of this writing, `ansible ~> 2.9`)
- sufficient AWS IAM privileges for the AWS provider [Resources and Date Sources (below)](#resources)
- access outlined for the remote [AWS S3 Backend](https://www.terraform.io/language/settings/backends/s3#s3-bucket-permissions)
- read/write privileges to the state-locking [AWS DynamoDB Table](https://www.terraform.io/language/settings/backends/s3#dynamodb-table-permissions)

</details>

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

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_ephemeral_environment_seed"></a> [ephemeral\_environment\_seed](#input\_ephemeral\_environment\_seed) | The model for a novel ephemeral environment. **Required** for new ephemeral environments. | `string` | `null` | no |
| <a name="input_ephemeral_rds_snapshot_id_override"></a> [ephemeral\_rds\_snapshot\_id\_override](#input\_ephemeral\_rds\_snapshot\_id\_override) | Specify DB Cluster Snapshot ID from `ephemeral_environment_seed`. Defaults to latest snapshot from the seed cluster on initial definition, falls back to previously specified snapshot on subsequent execution. | `string` | `null` | no |

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
| [aws_ssm_parameter.common_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.common_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.ephemeral_common](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.ephemeral_migrator](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.ephemeral_pipeline](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.migrator_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.migrator_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.pipeline_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.pipeline_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.server_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.server_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_db_cluster_snapshot.seed](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/db_cluster_snapshot) | data source |
| [aws_kms_key.cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_ssm_parameters_by_path.common_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_ssm_parameters_by_path.seed](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [external_external.eyaml](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
<!-- END_TF_DOCS -->
