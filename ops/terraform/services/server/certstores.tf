locals {
  truststore_certs = [
    for ssm_path, pubkey in local.nonsensitive_service_map
    : {
      alias  = element(split("/", ssm_path), length(split("/", ssm_path)) - 1)
      pubkey = pubkey
    }
    if strcontains(ssm_path, "/client_certificates/")
  ]
  local_truststore_path = "${path.module}/generated/truststore.pfx"
  s3_truststore_key     = "truststore.pfx"

  local_keystore_path = "${path.module}/generated/keystore.pfx"
  s3_keystore_key     = "keystore.pfx"
}

resource "aws_s3_bucket" "certstores" {
  bucket        = "bfd-${local.env}-${local.service}-certstores"
  force_destroy = true
}

data "aws_iam_policy_document" "certstores_bucket_policy_doc" {
  statement {
    sid       = "AllowSSLRequestsOnly"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = "${aws_s3_bucket.certstores.arn}/*"
    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = "false"
    }
  }
}

resource "aws_s3_bucket_policy" "certstores" {
  bucket = aws_s3_bucket.this.id
  policy = data.aws_iam_policy_document.certstores_bucket_policy_doc.json
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.certstores.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.certstores.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = data.aws_kms_key.env_key.arn
      sse_algorithm     = "aws:kms"
    }

    bucket_key_enabled = true
  }
}

# Necessary as the certstores may not exist on first run and Terraform's aws_s3_object data resource
# will fail if they don't exist. Used to trigger the null_resources to run if there are any
# out-of-band changes to the stores in S3.
data "external" "certstore_etag" {
  for_each = toset([local.s3_truststore_key, local.s3_keystore_key])

  program = ["${path.module}/scripts/get-s3-object-etag.sh"]
  query = {
    bucket = aws_s3_bucket.certstores.id
    s3_key = each.key
  }
}

resource "null_resource" "generate_truststore" {
  triggers = {
    # Ensures that the truststore is regenerated if the certificates it's comprised of change
    truststore_certs = jsonencode(local.truststore_certs)
    # Ensures that the truststore is regenerated if the store in s3 changes
    truststore_etag = data.external.certstore_etag[local.s3_truststore_key].result.ETag
  }

  provisioner "local-exec" {
    command = <<-EOF
chmod +x ${path.module}/scripts/generate-truststore.sh
${path.module}/scripts/generate-truststore.sh "$TRUSTSTORE_PATH" "$CERTS_JSON"
EOF

    environment = {
      TRUSTSTORE_PATH = local.local_truststore_path
      CERTS_JSON      = self.triggers.truststore_certs
    }
  }
}
