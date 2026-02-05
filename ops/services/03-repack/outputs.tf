locals {
  ssm_sensitive    = "sensitive"
  ssm_nonsensitive = "nonsensitive"

  outputs_map = {
    for k, v in {
      instance_user_privkey    = [tls_private_key.this.private_key_openssh, local.ssm_sensitive]
      instance_user_pubkey_ssh = [tls_private_key.this.public_key_openssh, local.ssm_nonsensitive]
      instance_user_pubkey_pem = [tls_private_key.this.public_key_pem, local.ssm_nonsensitive]
    }
    : "/bfd/${local.env}/${local.service}/${v[1]}/tf-outputs/${k}" => v[0]
  }
}

resource "aws_ssm_parameter" "outputs" {
  for_each = local.outputs_map

  name           = each.key
  tier           = "Intelligent-Tiering"
  type           = "String"
  insecure_value = strcontains(each.key, local.ssm_nonsensitive) ? each.value : null
  value          = strcontains(each.key, local.ssm_nonsensitive) ? null : each.value

  tags = {
    tf_output      = true
    auto_generated = true
  }
}
