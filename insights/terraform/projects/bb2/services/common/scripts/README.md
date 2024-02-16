# Overview

This directory contains symlinks to the scripts located in `ops/utils/cipher` which assist in managing the AWS parameter store SSM resources for BB2.

<details>

### Formatting and Validation

- nested hierarchies must conform to one of the following (nested keys within YAML transformed into paths):
    - `/bb2/${env}/${group}/${leaf}/...`
    - `/bb2/${env}/${group}/${subgroup}/${leaf}/...`
- `${env}` is typically one of `test`, `impl`, `prod` 
- `${group}` is typically the overarching category/group and repeatable among other SSM parameters
- `${subgroup}` is optional
- `${leaf}` _should_ be lower_snake_case formatted
- `...` represents additional hierarchies that are user-defined
- only string-formatted values are accepted
- empty strings, i.e '' are not supported
- we've adopted a _local_ convention where the literal `UNDEFINED` makes an SSM-derived value absent
- sensitive values must be encrypted with appropriate [AWS Key Management Service-stored CMK](https://us-east-1.console.aws.amazon.com/kms/home?region=us-east-1#/kms/keys)

#### Viewing with read-and-decrypt-yaml.sh

**WARNING:** This will present unencrypted, sensitive data to stdout. Do not execute this while sharing your screen during presentations or pairing opportunities.

To see the raw, _untemplated_ configuration as terraform does through via external data source for e.g. `../values/test.yaml`, execute the following from the module root directory:

```sh
scripts/read-and-decrypt-yaml.sh test <<KMS_KEY_ALIAS>>
```

where <<KMS_KEY_ALIAS>> is the alias of the KMS key used to initially encrypt the .yaml file, e.g. `bfd-insights-bb2-cmk`.

#### Editing with edit-yaml.sh and Updating with terraform

To edit the encrypted values under e.g. `../values/test.yaml` use the following steps:

1. Select the appropriate workspace: `terraform workspace select test`
2. Ensure a familiar editor is defined in your environment, e.g. `export EDITOR=vim`
3. Run the edit script from the module root directory: `./scripts/edit-yaml.sh test <<KMS_KEY_ALIAS>>`
4. Save and quit after making any desired changes
5. Review updates using the read script module root directory: `./scripts/read-and-decrypt-yaml.sh test <<KMS_KEY_ALIAS>>`
6. Ensure terraform can successfully plan by running `terraform plan`
7. Commit your changes to an appropriate feature branch
8. Solicit feedback by pull request
9. Follow the typical, monolithic release process via Jenkins

#### How to use SSM parameter store values

You can use the terraform data sources [aws_ssm_parameter](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameter) 
and/or [aws_ssm_parameters_by_path](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/ssm_parameters_by_path)
to get information about one or more SSM parameters in a specific hierarchy.

Sample usage:

```terraform
# Use this data source to get information about a specific SSM parameter.
data "aws_ssm_parameter" cross_account_arns {
  name = "/bb2/test/common/sensitive/cross_account_arns"
}

locals {
  cross_account_arns = data.aws_ssm_parameter.cross_account_arns.value
}
```

or

```terraform
# Use this data source to get information about one or more SSM parameters in a specific hierarchy.
data "aws_ssm_parameters_by_path" bb2_common_sensitive {
  name = "/bb2/test/common/sensitive/" # Trailing slash is optional
}

locals {
  bb2_common_sensitive = data.aws_ssm_parameters_by_path.bb2_common_sensitive.values
}
```

### Prerequisites
In addition to the [Requirements (below)](#requirements), you (or the automation) will need:
- software packages supporting awscli, yq, and jq
- sufficient access to the KMS Key(s) used for encrypting configuration
- sufficient AWS IAM privileges for the AWS provider [Resources and Date Sources (below)](#resources)
- access outlined for the remote [AWS S3 Backend](https://www.terraform.io/language/settings/backends/s3#s3-bucket-permissions)
- read/write privileges to the state-locking [AWS DynamoDB Table](https://www.terraform.io/language/settings/backends/s3#dynamodb-table-permissions)

</details>
