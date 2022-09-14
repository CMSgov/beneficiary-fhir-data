resource "aws_codeartifact_domain" "this" {
  domain         = "bfd-mgmt"
  encryption_key = data.aws_kms_key.cmk.arn
  tags           = local.shared_tags
}

resource "aws_codeartifact_repository" "this" {
  domain       = aws_codeartifact_domain.this.domain
  domain_owner = data.aws_caller_identity.current.account_id
  repository   = "bfd-mgmt"
  tags         = local.shared_tags
}
