locals {
  truststore_filename   = "truststore.pfx"
  truststore_local_path = "${path.module}/generated/${local.truststore_filename}"
  truststore_s3_key     = local.truststore_filename
  truststore_certs = nonsensitive([
    for ssm_path, pubkey in local.ssm_config
    : {
      alias  = element(split("/", ssm_path), length(split("/", ssm_path)) - 1)
      pubkey = pubkey
    }
    if strcontains(ssm_path, "/client_certificates/")
  ])

  keystore_filename   = "keystore.pfx"
  keystore_local_path = "${path.module}/generated/${local.keystore_filename}"
  keystore_s3_key     = local.keystore_filename
  keystore_base64     = local.ssm_config["/bfd/server/server_keystore_base64"]
}

module "bucket_certstores" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_prefix      = "bfd-${local.env}-${local.service}-certstores"
  bucket_kms_key_arn = local.env_key_arn
  force_destroy      = true

  ssm_param_name = "/bfd/${local.env}/${local.service}/nonsensitive/certstores_bucket"
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
    bucket = module.bucket_certstores.bucket.bucket
    s3_key = local.truststore_s3_key
  }
}

data "external" "keystore_object_size" {
  program = ["${path.module}/scripts/get-s3-object-size.sh"]
  query = {
    bucket = module.bucket_certstores.bucket.bucket
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
  bucket             = module.bucket_certstores.bucket.bucket
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
  bucket             = module.bucket_certstores.bucket.bucket
  content_base64     = sensitive(local.keystore_base64)
  bucket_key_enabled = true
}
