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

# Necessary as the certstores may not exist on first run and Terraform's aws_s3_object data resource
# will fail if they don't exist. Used to trigger the null_resources to run if there are any
# out-of-band changes to the stores in S3.
data "external" "certstore_etag" {
  for_each = toset([local.s3_truststore_key, local.s3_keystore_key])

  program = ["${path.module}/scripts/get-s3-object-etag.sh"]
  query = {
    bucket = "my-bucket" #TODO: Replace with real bucket name
    s3_key = each.key
  }
}

resource "null_resource" "generate_truststore" {
  triggers = {
    # Ensures that the truststore is regenerated if the certificates it's comprised of changes
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
