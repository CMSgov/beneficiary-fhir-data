data "aws_caller_identity" "current" {}

data "external" "yaml" {
  program = ["${path.module}/scripts/read-and-decrypt-yaml.sh", local.env, local.kms_key_id]
}
