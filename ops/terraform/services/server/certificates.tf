locals {
  truststore_certs = [
    for ssm_path, pubkey in local.nonsensitive_service_map
    : {
      alias  = element(split("/", ssm_path), length(split("/", ssm_path)) - 1)
      pubkey = pubkey
    }
    if strcontains(ssm_path, "/client_certificates/")
  ]
  truststore_path = "${path.module}/generated/truststore.pfx"
}

resource "null_resource" "generate_truststore" {
  triggers = {
    truststore_certs = jsonencode(local.truststore_certs)
  }

  provisioner "local-exec" {
    command = <<-EOF
chmod +x ${path.module}/scripts/generate-truststore.sh
${path.module}/scripts/generate-truststore.sh "$TRUSTSTORE_PATH" "$CERTS_JSON"
EOF

    environment = {
      TRUSTSTORE_PATH = local.truststore_path
      CERTS_JSON      = self.triggers.truststore_certs
    }
  }
}
