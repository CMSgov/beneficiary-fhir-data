# BFD Base Environment Definitions

This module is the _service_ responsible for defining a BFD environment through AWS SSM Parameter Store.
This is supported by a [terraform workspaces-enabled](https://www.terraform.io/language/state/workspaces) state.
**If you're manipulating this state manually, you must verify that you're operating in the appropriate workspace for the targeted environment.**

## Overview

Configuration management can get away from us, becoming wildly complex if there isn't a conscious effort to _keep things simple_.
While simplicity is in the eye of the beholder, this module seeks to be as simple as possible and no simpler.
To that end, this module will _primarily_ be composed of three files, `sensitive.tf`, `nonsensitive.tf`, and `ephemeral.tf`.
The former two terraform files deal with sensitive and nonsensitive values, respectively, while `ephemeral.tf` applies exclusively to ephemeral environments.

Additional terraform instructions are located in the canonical _or_ expected files, e.g. `main.tf`, `variables.tf`, etc.
In general, terraform reads encrypted **and** plaintext yaml-encoded _<key>:<value>_ pairs from specific files found in the [values directory](.values) and translates them into the appropriate AWS SSM Parameter Store Hierarchies.

For more general documentation on formatting and usage, expand the sections under [Additional Information](#additional-information) below.

## Environments 
Each environment is configured in a terraform workspace, named for the environment it defines.
Environments can generally be classified as _established_ or _ephemeral_.

### Established Environments
There are four, known _established environments_ (`local.established_envs`) with their respective configuration coming from appropriately named, environment-specific yaml files found in the aforementioned values directory.
Practically speaking, this module configures the path-to-production established environments of `test`, `prod-sbx`, and `prod`.
These environments are not only established but they endure: they are **not** ephemeral nor temporary.

Each of the established environments has specific yaml files in the values directory specific to their configuration.
These environments may act as a _seed_ or source environment for ephemeral environment creation described below.

### Ephemeral Environments
**As of mid May 2023, ephemeral environment supported remains limited. Please work with BFD engineers working in the infrastructure space if you need ephemeral environment support.**

Ephemeral environments use the values stored in `values/ephemeral.yaml` for some of their configuration but they generally yield to the _seed_ environment configuration for the more consequential inputs.
These environments effectively copy the values from their seed environment's AWS SSM Parameters as defined in the `ephemeral.tf`.

Terraform identifies the ephemeral environment's seed from the workspace name itself.
This module expects that all workspace environment names will end in one of `test`, `prod-sbx`, or `prod`.

As of mid May 2023, there are few controls to enforce this, but the hazard of getting this _wrong_ is only wasted time in receiving unhelpful errors in attempting to create ephemeral environments.
Historically, the seed environment was derived from an unusual terraform input variable `var.ephemeral_environment_seed` that was only required on the first execution of terraform.

## Additional Information

<details><summary>More...</summary>

### Known Limitations
AWS SSM Parameter Store has very limited support for storing non-string values in plain-text (`nonsensitive`) data and virtually no options for storing encrypted non-string (`sensitive`) data.
This forces us to handle some data that would more naturally be represented as collections like maps and arrays as formatted string types.
To work with this, you might consider using spaces to delimit your collection and parse accordingly, which can easily be achieved using yaml's `>` _folding block_ for multi-line strings.
Other techniques might involve storing more complex data in formats that are more machine-readable, like JSON.
Between storing JSON strings in the yaml context here and being fetching those values from AWS SSM Parameter Store, it will be in an _escaped_ format and the data will likely need special handling, e.g. `jq`'s `fromjson` function may be handy in these circumstances.

### Formatting and Validation

As of mid-May 2023, technical controls for standards enforcement are still forthcoming. As a stopgap, here are some guidelines in the spirit of keeping things simple:
- All workspaces must end in one of the three path-to-production established environments of `test`, `prod-sbx`, or `prod`
- Ephemeral environment workspace should generally be of a pattern similar to `<jira-id>-<env>`, e.g. `2544-test`, `2544-prod-sbx`, `2554-prod`.
- nested hierarchies must conform to one of the following (nested keys within YAML transformed into paths):
  - `/bfd/${env}/${group}/${leaf}/...`
  - `/bfd/${env}/${group}/${subgroup}/${leaf}/...`
- `${env}` is typically one of `test`, `prod-sbx`, `prod` or ephemeral format `<jira-id>-<env>`, e.g. `2544-test`
- `${group}` must be one of the supported groups: `common`, `migrator`, `pipeline`, `server`
- `${subgroup}` is optional, as of January 2023, examples include `ccw`, `rda`, `shared`
- `${leaf}` _should_ be lower_snake_case formatted
- `...` represents additional hierarchies that are user-defined
- if the hierarchy should match the _regex_ `/ami.id/`, the value [**must** point to an existing Amazon Machine Image](https://docs.aws.amazon.com/systems-manager/latest/userguide/parameter-store-ec2-aliases.html#parameter-ami-validation)
- only string-formatted values are accepted
- empty strings, i.e '' are not supported
- we've adopted a _local_ convention where the literal `UNDEFINED` makes an SSM-derived value absent
- sensitive values must be encrypted with appropriate [AWS Key Management Service-stored CMK](https://us-east-1.console.aws.amazon.com/kms/home?region=us-east-1#/kms/keys)

### Usage and User Additions

If the below [prerequisites](#prerequisites) are met, users will _generally_ interact with the environment-specific configuration by using one or more scripts in the [scripts](./scripts) directory for those encrypted values, otherwise a text-editor of their choosing when adjusting plain text values.

#### Viewing with read-and-decrypt-yaml.sh

**WARNING:** This will present unencrypted, sensitive data to stdout. Do not execute this while sharing your screen during presentations or pairing opportunities.

To see the raw, _untemplated_ configuration as terraform does through via external data source for e.g. `./values/prod-sbx.yaml`, execute the following from the module root directory:

```sh
scripts/read-and-decrypt-yaml.sh prod-sbx
```

#### Editing with edit-yaml.sh and Updating with terraform

To edit the encrypted values under e.g. `./values/prod-sbx.yaml` use the following steps:

1. Select the appropriate workspace: `terraform workspace select prod-sbx`
2. Ensure a familiar editor is defined in your environment, e.g. `export EDITOR=vim`
3. Run the edit script from the module root directory: `scripts/edit-yaml.sh prod-sbx`
4. Save and quit after making any desired changes
5. Review updates using the read script module root directory: `scripts/read-and-decrypt-yaml.sh prod-sbx`
6. Ensure terraform can successfully plan by running `terraform plan`
7. Commit your changes to an appropriate feature branch
8. Solicit feedback by pull request
9. Follow the typical, monolithic release process via Jenkins

### Prerequisites
In addition to the [Requirements (below)](#requirements), you (or the automation) will need:
- software packages supporting awscli, yq, and jq
- sufficient access to the various Multi-Region KMS Keys used for encrypting configuration
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
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 5.53.0 |

<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
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
| [aws_ssm_parameter.eft_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.eft_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.ephemeral_common](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.ephemeral_migrator](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.ephemeral_pipeline](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.ephemeral_server](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.migrator_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.migrator_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.pipeline_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.pipeline_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.server_alarms_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.server_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_ssm_parameter.server_sensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
| [aws_db_cluster_snapshot.seed](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/db_cluster_snapshot) | data source |
| [aws_kms_key.cmk](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_ssm_parameters_by_path.common_nonsensitive](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [aws_ssm_parameters_by_path.seed](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path) | data source |
| [external_external.yaml](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |
<!-- END_TF_DOCS -->
