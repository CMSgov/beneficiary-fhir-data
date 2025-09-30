# `config` Terraservice

This Terraservice is responsible for defining a given BFD environment's configuration through AWS SSM Parameter Store by reading from [sops-encrypted](https://github.com/getsops/sops) YAML files.

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

N/A

## Overview

Configuration management can get away from us, becoming wildly complex if there isn't a conscious effort to _keep things simple_.
While simplicity is in the eye of the beholder, this Terraservice seeks to be as simple as possible and no simpler.
To that end, this service is comprised of a single `main.tf`, 3 per-environment `.sopsw.yaml` configuration files, and a special `ephemeral.yaml`. Further, this Terraservice creates _only_ `aws_ssm_parameter` resources.

In general, terraform reads encrypted **and** plaintext yaml-encoded _<key>:<value>_ pairs from specific files found in the [values directory](.values) and translates them into the appropriate AWS SSM Parameter Store Hierarchies.

For more general documentation on formatting and usage, expand the sections under [Additional Information](#additional-information) below.

## Environments

Each environment is configured in a terraform workspace, named for the environment it defines.
Environments can generally be classified as _established_ or _ephemeral_.

### Established Environments

There are three, known _established environments_ with their respective configuration coming from appropriately named, environment-specific yaml files found in the aforementioned values directory.
Practically speaking, this module configures the path-to-production established environments of `test`, `prod-sbx`/`sandbox`, and `prod`.
These environments are not only established but they endure: they are **not** ephemeral nor temporary.

Each of the established environments has specific yaml files in the values directory specific to their configuration.
These environments may act as a _seed_ or source environment for ephemeral environment creation described below.

### Ephemeral Environments

Ephemeral environments use the values stored in `values/ephemeral.yaml` under the `values` property for some of their **nonsensitive** configuration but they generally yield to _copying_ from the _seed_ environment's configuration for the more consequential inputs.
These environments effectively copy the values from their seed environment's AWS SSM Parameters as defined in the `copy` list in `ephemeral.yaml`.

Terraform identifies the ephemeral environment's seed from the workspace name itself.
This module expects that all workspace environment names will end in one of `test`, `prod-sbx`/`sandbox`, or `prod`.

## Additional Information

<details><summary>More...</summary>

### Known Limitations

AWS SSM Parameter Store has very limited support for storing non-string values in plain-text (`nonsensitive`) data and virtually no options for storing encrypted non-string (`sensitive`) data.
This forces us to handle some data that would more naturally be represented as collections like maps and arrays as formatted string types.
To work with this, you might consider using commas to delimit your collection and parse accordingly, which can easily be achieved using yaml's `>` _folding block_ for multi-line strings. Or, formatting your list as a stringified JSON array.
Other techniques might involve storing more complex data in formats that are more machine-readable, like JSON.
Between storing JSON strings in the yaml context here and being fetching those values from AWS SSM Parameter Store, it will be in an _escaped_ format and the data will likely need special handling, e.g. `jq`'s `fromjson` function may be handy in these circumstances.

### Formatting and Validation

Technical controls for standards enforcement are still forthcoming. As a stopgap, here are some guidelines in the spirit of keeping things simple:

- All workspaces must end in one of the three path-to-production established environments of `test`, `prod-sbx`/`sandbox`, or `prod`
- Ephemeral environment workspace should generally be of a pattern similar to `<jira-id>-<env>`, e.g. `2544-test`, `2544-prod-sbx`/`2544-sandbox`, `2554-prod`.
- Keys must conform the following (nested keys within YAML transformed into paths):
  - `/${root}/${env}/${service}/${sensitivity}/...`
- `${root}` is typically `bfd`, but may be any of our partners
- `${env}` is typically one of `test`, `prod-sbx`/`sandbox`, `prod` or ephemeral format `<jira-id>-<env>`, e.g. `2544-test`
- `${group}` must be one of the supported groups: `common`, `migrator`, `pipeline`, `server`
- `${subgroup}` is optional, as of January 2023, examples include `ccw`, `rda`, `shared`
- `${sensitivity}` should one of `nonsensitive` or `sensitive`, and indicates whether the parameter is encrypted at rest (thus, sensitive/secret) or not
- `...` represents additional hierarchies that are user-defined
- Non-string formatted values will be converted to a string using [Terraform's `tostring()` built-in](https://developer.hashicorp.com/terraform/language/functions/tostring)
- Empty strings, i.e '' are not supported
- We've adopted a _local_ convention where the literal `UNDEFINED`, case _insensitive_, makes an SSM-derived value absent
- Sensitive values must be encrypted with appropriate [AWS Key Management Service-stored CMK](https://us-east-1.console.aws.amazon.com/kms/home?region=us-east-1#/kms/keys)

### Usage and User Additions

If the below [prerequisites](#prerequisites) are met, users will _generally_ interact with the environment-specific configuration by using one or more scripts in the [scripts](./scripts) directory for those encrypted values, otherwise a text-editor of their choosing when adjusting plain text values.

**Note that `.sopsw.yaml` files are not fully `sops`-compliant encrypted YAML**. Operators _MUST_ use the `sopsw` (in `apps/utils/scripts/sopsw` or the symlink'd variant at `./scripts/sopsw`) utility script when working with these files. It is recommended to add the `apps/utils/scripts` folder to your shell's `PATH` for easier use.

The `sopsw` script provides a simplified CLI exposing all of the operations that would be necessary to interact with these files on a regular basis. View the available commands using `sopsw -h`.

#### Viewing decrypted YAML using `sopsw -d/--decrypt`

**WARNING:** This will present unencrypted, sensitive data to stdout. Do not execute this while sharing your screen during presentations or pairing opportunities.

To see the raw, _untemplated_ configuration as terraform does through via external data source for e.g. `./values/test.sopsw.yaml`, execute the following from the module root directory:

```sh
scripts/sopsw -d values/test.sopsw.yaml
```

#### Editing encrypted YAML using `sopsw -e/--edit`

To edit the encrypted values under e.g. `./values/prod-sbx.sopsw.yaml`:

1. Ensure `EDITOR` is set to your preferred editor; for VS Code, `EDITOR` should be `code -w`
2. Use `sopsw` like so:

   ```bash
   scripts/sopsw -e values/prod-sbx.sopsw.yaml
   ```

3. Make the relevant changes to `prod-sbx.sopsw.yaml` in the opened editor window/tab
4. Save and close the editor window/tab

### Prerequisites

In addition to the [Requirements (below)](#requirements), you (or the automation) will need:

- software packages supporting awscli, yq, jq, and sops
- sufficient access to the various Multi-Region KMS Keys used for encrypting configuration
- sufficient AWS IAM privileges for the AWS provider [Resources and Date Sources (below)](#resources)
- access outlined for the remote [AWS S3 Backend](https://www.terraform.io/language/settings/backends/s3#s3-bucket-permissions)
- read/write privileges to the state-locking [AWS DynamoDB Table](https://www.terraform.io/language/settings/backends/s3#dynamodb-table-permissions)

</details>

<!-- BEGIN_TF_DOCS -->
<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Requirements

| Name | Version |
|------|---------|
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | ~> 6 |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_parent_env"></a> [parent\_env](#input\_parent\_env) | The parent environment of the current solution. Will correspond with `terraform.workspace`".<br/>Necessary on `tofu init` and `tofu workspace select` \_only\_. In all other situations, parent env<br/>will be divined from `terraform.workspace`. | `string` | `null` | no |
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
| <a name="module_terraservice"></a> [terraservice](#module\_terraservice) | ../../terraform-modules/bfd/bfd-terraservice | n/a |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Resources

| Name | Type |
|------|------|
| [aws_ssm_parameter.this](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/ssm_parameter) | resource |
| [external_external.decrypted_sops](https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/external) | data source |

<!--WARNING: GENERATED CONTENT with terraform-docs, e.g.
     'terraform-docs --config "$(git rev-parse --show-toplevel)/.terraform-docs.yml" .'
     Manually updating sections between TF_DOCS tags may be overwritten.
     See https://terraform-docs.io/user-guide/configuration/ for more information.
-->
## Outputs

No outputs.
<!-- END_TF_DOCS -->
