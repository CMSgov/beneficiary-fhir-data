data "aws_caller_identity" "current" {}

data "external" "yaml" {
  program = ["${path.module}/scripts/tf-decrypt-shim.sh"]
  query = {
    seed_env    = local.env
    env         = local.env
    kms_key_alias = local.kms_key_alias
  }
}
