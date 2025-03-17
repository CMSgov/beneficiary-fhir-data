locals {
  client_certificates_map = zipmap(
    data.aws_ssm_parameters_by_path.client_certificates.names,
    nonsensitive(data.aws_ssm_parameters_by_path.client_certificates.values)
  )
  truststore_local_path = "${path.module}/generated/truststore.pfx"
  truststore_s3_key     = "truststore.pfx"
  truststore_certs = [
    for ssm_path, pubkey in local.client_certificates_map
    : {
      alias  = element(split("/", ssm_path), length(split("/", ssm_path)) - 1)
      pubkey = pubkey
    }
    if strcontains(ssm_path, "/client_certificates/")
  ]

  keystore_local_path = "${path.module}/generated/keystore.pfx"
  keystore_s3_key     = "keystore.pfx"
  keystore_base64     = local.sensitive_service_config["server_keystore_base64"]
}

data "aws_ssm_parameters_by_path" "client_certificates" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive/client_certificates"
}

resource "aws_s3_bucket" "certstores" {
  bucket_prefix = "bfd-${local.env}-${local.service}-certstores"
  force_destroy = true
}

data "aws_iam_policy_document" "certstores_bucket_policy_doc" {
  statement {
    sid       = "AllowSSLRequestsOnly"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = ["${aws_s3_bucket.certstores.arn}/*"]

    principals {
      identifiers = ["*"]
      type        = "*"
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "certstores" {
  bucket = aws_s3_bucket.certstores.bucket
  policy = data.aws_iam_policy_document.certstores_bucket_policy_doc.json
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.certstores.bucket

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.certstores.bucket

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = data.aws_kms_key.env_key.arn
      sse_algorithm     = "aws:kms"
    }

    bucket_key_enabled = true
  }
}

# Necessary as the certstores may not exist on first run and Terraform's aws_s3_object data resource
# will fail if they don't exist. Used to trigger the null_resource and aws_s3_object resources to
# run if there are any out-of-band changes to the stores in S3. Checksums/hashes cannot be used as
# keystore creation is not deterministic (creation date and some other metadata is added during
# creation). Fortunately, object size seems to be stable across repeated generations if the same
# certificates are used.
data "external" "truststore_object_size" {
  program = ["${path.module}/scripts/get-s3-object-size.sh"]
  query = {
    bucket = aws_s3_bucket.certstores.bucket
    s3_key = local.truststore_s3_key
  }
}

data "external" "keystore_object_size" {
  program = ["${path.module}/scripts/get-s3-object-size.sh"]
  query = {
    bucket = aws_s3_bucket.certstores.bucket
    s3_key = local.keystore_s3_key
  }
}

resource "null_resource" "generate_truststore" {
  triggers = {
    # Ensures that the truststore is regenerated if the certificates it's comprised of change
    truststore_certs = jsonencode(local.truststore_certs)
    # The following trigger ensures that the truststore is regenerated if the store in s3 changes
    truststore_object_size = data.external.truststore_object_size.result.ObjectSize
  }

  provisioner "local-exec" {
    command = <<-EOF
chmod +x ${path.module}/scripts/generate-truststore.sh
${path.module}/scripts/generate-truststore.sh "$TRUSTSTORE_PATH" "$CERTS_JSON"
EOF

    environment = {
      TRUSTSTORE_PATH = local.truststore_local_path
      CERTS_JSON      = self.triggers.truststore_certs
    }
  }
}

resource "aws_s3_object" "truststore" {
  lifecycle {
    replace_triggered_by = [null_resource.generate_truststore]
  }

  key                = local.truststore_s3_key
  bucket             = aws_s3_bucket.certstores.bucket
  source             = local.truststore_local_path
  bucket_key_enabled = true
}

resource "terraform_data" "keystore_object_size" {
  input = data.external.keystore_object_size.result.ObjectSize
}

resource "aws_s3_object" "keystore" {
  lifecycle {
    replace_triggered_by = [terraform_data.keystore_object_size]
  }

  key                = local.keystore_s3_key
  bucket             = aws_s3_bucket.certstores.bucket
  content_base64     = sensitive(local.keystore_base64)
  bucket_key_enabled = true
}
