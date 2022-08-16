<!-- BEGIN_TF_DOCS -->
<!-- GENERATED WITH `terraform-docs .`
     Manually updating the README.md will be overwritten.
     For more details, see the file '.terraform-docs.yml' or
     https://terraform-docs.io/user-guide/configuration/
-->
# S3 Bucket module

This module provisions relatively hardened S3 Buckets by default:
- server side encryption is enabled
- bucket access logs are enabled
- block public access is enabled
- all requests require Secure Socket Layer
- versioning is enabled


### Bucket naming

We are following the standard OEDA/CMS naming convention for AWS resources.

> $project-[$sub-project]-$env-[$sub-env]-$role-[$resource-type]-[$aws-acct-number]

This allows for easy identification of resource ownership and purpose.

Examples:

```sh
bfd-test-foo-1234567890      # s3 bucket holding foo- only decryptable in the test environment
bfd-prod-sbx-foo-1234567890  # where prod-sbx is $env-$sub-env
```

#### References

- https://confluence.cms.gov/display/ODI/AWS+Naming+and+Tagging+Conventions
- https://docs.aws.amazon.com/AmazonS3/latest/userguide/acl-overview.html#canned-acl

## Requirements

No requirements.

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_env_config"></a> [env\_config](#input\_env\_config) | Environment description | `object({ env = string, tags = map(string), vpc_id = string, zone_id = string })` | n/a | yes |
| <a name="input_role"></a> [role](#input\_role) | (REQUIRED) Role of the bucket. Ie 'logs', 'data', 'artifacts', etc. | `string` | n/a | yes |
| <a name="input_acl"></a> [acl](#input\_acl) | The canned ACL to apply to the bucket. Defaults to private. | `string` | `"private"` | no |
| <a name="input_block_public_acls"></a> [block\_public\_acls](#input\_block\_public\_acls) | Whether Amazon S3 should block public ACLs for this bucket. Defaults to true. | `bool` | `true` | no |
| <a name="input_block_public_policy"></a> [block\_public\_policy](#input\_block\_public\_policy) | Whether Amazon S3 should block public bucket policies for this bucket. Defaults to true. | `bool` | `true` | no |
| <a name="input_id"></a> [id](#input\_id) | Explicitly set the bucket id. | `string` | `null` | no |
| <a name="input_ignore_public_acls"></a> [ignore\_public\_acls](#input\_ignore\_public\_acls) | Whether Amazon S3 should ignore public ACLs for this bucket. Defaults to true. | `bool` | `true` | no |
| <a name="input_kms_key_id"></a> [kms\_key\_id](#input\_kms\_key\_id) | KMS key (arn) to use for encryption. Defaults to the AWS managed S3 key. | `string` | `"alias/aws/s3"` | no |
| <a name="input_lifecycle_config"></a> [lifecycle\_config](#input\_lifecycle\_config) | Default bucket lifecycle settings. Transitioning numbers are in days or null to ignore. Any 'noncurrent' rules requires versioning to be enabled. | <pre>object({<br>    enabled = bool<br>    transition_objects_to_ia_days = number<br>    transition_noncurrent_versions_to_ia_days = number<br>    expire_noncurrent_versions_days = number<br>  })</pre> | <pre>{<br>  "enabled": false,<br>  "expire_noncurrent_versions_days": 60,<br>  "transition_noncurrent_versions_to_ia_days": 7,<br>  "transition_objects_to_ia_days": 90<br>}</pre> | no |
| <a name="input_lifecycle_enabled"></a> [lifecycle\_enabled](#input\_lifecycle\_enabled) | Enable bucket lifecycle rules. | `bool` | `false` | no |
| <a name="input_logging_bucket"></a> [logging\_bucket](#input\_logging\_bucket) | Explicitly set a logging bucket. | `string` | `null` | no |
| <a name="input_logging_enabled"></a> [logging\_enabled](#input\_logging\_enabled) | Enable bucket logging. Bucket logs are stored in the designated environment's 'logs' bucket. | `bool` | `true` | no |
| <a name="input_logging_prefix"></a> [logging\_prefix](#input\_logging\_prefix) | Explicitly set a logging prefix. | `string` | `null` | no |
| <a name="input_restrict_public_buckets"></a> [restrict\_public\_buckets](#input\_restrict\_public\_buckets) | Whether Amazon S3 should restrict public bucket policies for this bucket. Defaults to true. | `bool` | `true` | no |
| <a name="input_versioning_enabled"></a> [versioning\_enabled](#input\_versioning\_enabled) | Enable bucket versioning. | `bool` | `true` | no |

## Resources

| Name | Type |
|------|------|
| [aws_s3_bucket.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket) | resource |
| [aws_s3_bucket_acl.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_acl) | resource |
| [aws_s3_bucket_lifecycle_configuration.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_lifecycle_configuration) | resource |
| [aws_s3_bucket_logging.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_logging) | resource |
| [aws_s3_bucket_public_access_block.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_public_access_block) | resource |
| [aws_s3_bucket_server_side_encryption_configuration.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_server_side_encryption_configuration) | resource |
| [aws_s3_bucket_versioning.main](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/s3_bucket_versioning) | resource |
| [aws_caller_identity.current](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/caller_identity) | data source |
<!-- END_TF_DOCS -->