locals {
  # All seed environment hierarchies
  seed = local.is_ephemeral_env ? zipmap(data.aws_ssm_parameters_by_path.seed[0].names, data.aws_ssm_parameters_by_path.seed[0].values) : {}

  # Targeted COMMON hierarchy paths to be "copied" from the seed environment into requested ephemeral environment
  common_seed_paths = local.is_ephemeral_env ? {
    "/bfd/${local.env}/common/nonsensitive/enterprise_tools_security_group" = "/bfd/${local.seed_env}/common/nonsensitive/enterprise_tools_security_group"
    "/bfd/${local.env}/common/nonsensitive/key_pair"                        = "/bfd/${local.seed_env}/common/nonsensitive/key_pair"
    "/bfd/${local.env}/common/nonsensitive/kms_key_alias"                   = "/bfd/${local.seed_env}/common/nonsensitive/kms_key_alias"
    "/bfd/${local.env}/common/nonsensitive/kms_config_key_alias"            = "/bfd/${local.seed_env}/common/nonsensitive/kms_config_key_alias"
    "/bfd/${local.env}/common/nonsensitive/management_security_group"       = "/bfd/${local.seed_env}/common/nonsensitive/management_security_group"
    "/bfd/${local.env}/common/nonsensitive/rds_instance_class"              = "/bfd/${local.seed_env}/common/nonsensitive/rds_instance_class"
    "/bfd/${local.env}/common/nonsensitive/vpc_name"                        = "/bfd/${local.seed_env}/common/nonsensitive/vpc_name"
    "/bfd/${local.env}/common/nonsensitive/vpn_security_group"              = "/bfd/${local.seed_env}/common/nonsensitive/vpn_security_group"
  } : {}

  # Targeted MIGRATOR hierarchy paths to be "copied" from the seed environment into requested ephemeral environment
  migrator_seed_paths = local.is_ephemeral_env ? {
    "/bfd/${local.env}/migrator/sensitive/db/password" = "/bfd/${local.seed_env}/migrator/sensitive/db/password"
    "/bfd/${local.env}/migrator/sensitive/db/username" = "/bfd/${local.seed_env}/migrator/sensitive/db/username"
  } : {}

  # Targeted PIPELINE hierarchy paths to be "copied" from the seed environment into requested ephemeral environment
  pipeline_seed_paths = local.is_ephemeral_env ? {
    "/bfd/${local.env}/pipeline/sensitive/db/password"          = "/bfd/${local.seed_env}/pipeline/sensitive/db/password"
    "/bfd/${local.env}/pipeline/sensitive/db/username"          = "/bfd/${local.seed_env}/pipeline/sensitive/db/username"
    "/bfd/${local.env}/pipeline/sensitive/hicn_hash/iterations" = "/bfd/${local.seed_env}/pipeline/sensitive/hicn_hash/iterations"
    "/bfd/${local.env}/pipeline/sensitive/hicn_hash/pepper"     = "/bfd/${local.seed_env}/pipeline/sensitive/hicn_hash/pepper"
    # The prod-sbx environment includes an in-process server instead of a communicating with an external, gRPC host
    "/bfd/${local.env}/pipeline/sensitive/rda/grpc/auth_token" = local.seed_env == "prod-sbx" ? "" : "/bfd/${local.seed_env}/pipeline/sensitive/rda/grpc/auth_token"
    "/bfd/${local.env}/pipeline/sensitive/rda/grpc/port"       = local.seed_env == "prod-sbx" ? "" : "/bfd/${local.seed_env}/pipeline/sensitive/rda/grpc/port"
    "/bfd/${local.env}/pipeline/sensitive/rda/grpc/host"       = local.seed_env == "prod-sbx" ? "" : "/bfd/${local.seed_env}/pipeline/sensitive/rda/grpc/host"
  } : {}

  # FUTURE: Fix this when hierarchies are supported with Terraform module.
  seed_env_certs = {
    prod = {
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_root_ca"                              = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_root_ca"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bcda_prod_client"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bcda_prod_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/performance_tests"                               = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/performance_tests"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/regression_tests"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/regression_tests"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/dpc_prod_client"                                 = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/dpc_prod_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/ab2d_prod_client"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/ab2d_prod_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/ab2d_prod_validation_client"                     = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/ab2d_prod_validation_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_backend_prod_data_server_client_test" = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_backend_prod_data_server_client_test"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_backend_test_samhsa"                  = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_backend_test_samhsa"
    }
    prod-sbx = {
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_backend_dpr_data_server_client_test" = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_backend_dpr_data_server_client_test"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_backend_test_samhsa"                 = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_backend_test_samhsa"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_root_ca_test"                        = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_root_ca_test"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_root_ca_sbx"                         = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_root_ca_sbx"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bb2_local_client"                               = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bb2_local_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bcda_dev_client"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bcda_dev_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bcda_test_client"                               = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bcda_test_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bcda_sbx_client"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bcda_sbx_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bcda_local_client"                              = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bcda_local_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/performance_tests"                              = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/performance_tests"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/regression_tests"                               = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/regression_tests"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/dpc_prod_sbx_client"                            = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/dpc_prod_sbx_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/dpc_test_client"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/dpc_test_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/dpc_dev_client"                                 = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/dpc_dev_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/dpc_local_client"                               = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/dpc_local_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/ab2d_dev_prod_sbx_client"                       = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/ab2d_dev_prod_sbx_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/ab2d_sbx_client"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/ab2d_sbx_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/ab2d_imp_client"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/ab2d_imp_client"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/prod_sbx_bfd_cms_gov"                           = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/prod_sbx_bfd_cms_gov"
    }
    test = {
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_backend_test_data_server_client_test" = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_backend_test_data_server_client_test"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_backend_test_samhsa"                  = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_backend_test_samhsa"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/bluebutton_root_ca"                              = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/bluebutton_root_ca"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/performance_tests"                               = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/performance_tests"
      "/bfd/${local.env}/server/nonsensitive/client_certificates/regression_tests"                                = "/bfd/${local.seed_env}/server/nonsensitive/client_certificates/regression_tests"
    }
  }

  # Targeted SERVER hierarchy paths to be "copied" from the (merged) seed environment into requested ephemeral environment
  server_seed_paths = local.is_ephemeral_env ? merge({
    "/bfd/${local.env}/server/nonsensitive/asg_desired_instance_count"              = "/bfd/${local.seed_env}/server/nonsensitive/asg_desired_instance_count"
    "/bfd/${local.env}/server/nonsensitive/asg_instance_warmup_time"                = "/bfd/${local.seed_env}/server/nonsensitive/asg_instance_warmup_time"
    "/bfd/${local.env}/server/nonsensitive/asg_max_instance_count"                  = "/bfd/${local.seed_env}/server/nonsensitive/asg_max_instance_count"
    "/bfd/${local.env}/server/nonsensitive/asg_max_warm_instance_count"             = "/bfd/${local.seed_env}/server/nonsensitive/asg_max_warm_instance_count"
    "/bfd/${local.env}/server/nonsensitive/asg_min_instance_count"                  = "/bfd/${local.seed_env}/server/nonsensitive/asg_min_instance_count"
    "/bfd/${local.env}/server/nonsensitive/launch_template_instance_type"           = "/bfd/${local.seed_env}/server/nonsensitive/launch_template_instance_type"
    "/bfd/${local.env}/server/nonsensitive/launch_template_volume_size_gb"          = "/bfd/${local.seed_env}/server/nonsensitive/launch_template_volume_size_gb"
    "/bfd/${local.env}/server/nonsensitive/heathcheck/request_timeout_seconds"      = "/bfd/${local.seed_env}/server/nonsensitive/heathcheck/request_timeout_seconds"
    "/bfd/${local.env}/server/nonsensitive/heathcheck/request_backoff_seconds"      = "/bfd/${local.seed_env}/server/nonsensitive/heathcheck/request_backoff_seconds"
    "/bfd/${local.env}/server/nonsensitive/heathcheck/testing_bene_id"              = "/bfd/${local.seed_env}/server/nonsensitive/heathcheck/testing_bene_id"
    "/bfd/${local.env}/server/nonsensitive/samhsa_allowed_certificate_aliases_json" = "/bfd/${local.seed_env}/server/nonsensitive/samhsa_allowed_certificate_aliases_json"
    "/bfd/${local.env}/server/sensitive/server_keystore_base64"                     = "/bfd/${local.seed_env}/server/sensitive/server_keystore_base64"
    "/bfd/${local.env}/server/sensitive/service_port"                               = "/bfd/${local.seed_env}/server/sensitive/service_port"
    "/bfd/${local.env}/server/sensitive/db/username"                                = "/bfd/${local.seed_env}/server/sensitive/db/username"
    "/bfd/${local.env}/server/sensitive/db/password"                                = "/bfd/${local.seed_env}/server/sensitive/db/password"
    "/bfd/${local.env}/server/sensitive/test_client_cert"                           = "/bfd/${local.seed_env}/server/sensitive/test_client_cert"
    "/bfd/${local.env}/server/sensitive/test_client_key"                            = "/bfd/${local.seed_env}/server/sensitive/test_client_key"
    "/bfd/${local.env}/server/sensitive/test_client_cert"                           = "/bfd/${local.seed_env}/server/sensitive/test_client_samhsa_cert"
    "/bfd/${local.env}/server/sensitive/test_client_key"                            = "/bfd/${local.seed_env}/server/sensitive/test_client_samhsa_key"
    "/bfd/${local.env}/server/sensitive/server_regression_cert"                     = "/bfd/${local.seed_env}/server/sensitive/server_regression_cert"
    "/bfd/${local.env}/server/sensitive/server_regression_key"                      = "/bfd/${local.seed_env}/server/sensitive/server_regression_key"
  }, local.seed_env_certs[local.seed_env]) : {}
}

data "aws_db_cluster_snapshot" "seed" {
  count = local.is_ephemeral_env ? 1 : 0

  db_cluster_identifier = "bfd-${local.seed_env}-aurora-cluster"
  most_recent           = true
  # default to latest snapshot if no override is provided
  db_cluster_snapshot_identifier = var.ephemeral_rds_snapshot_id_override
}

# NOTE: Contains *all* seed environment hierarchies including sensitive and nonsensitive values
data "aws_ssm_parameters_by_path" "seed" {
  count = local.is_ephemeral_env ? 1 : 0

  path            = "/bfd/${local.seed_env}/"
  with_decryption = true
  recursive       = true
}

# Copy targeted COMMON hierarchy paths from seed environment into requested ephemeral environment
resource "aws_ssm_parameter" "ephemeral_common" {
  for_each  = local.common_seed_paths
  key_id    = contains(split("/", each.key), "sensitive") ? local.kms_key_id : null
  name      = each.key
  overwrite = true
  type      = contains(split("/", each.key), "sensitive") ? "SecureString" : "String"
  value     = local.seed[each.value]
}

# Copy targeted MIGRATOR hierarchy paths from seed environment into requested ephemeral environment
resource "aws_ssm_parameter" "ephemeral_migrator" {
  for_each  = local.migrator_seed_paths
  key_id    = contains(split("/", each.key), "sensitive") ? local.kms_key_id : null
  name      = each.key
  overwrite = true
  type      = contains(split("/", each.key), "sensitive") ? "SecureString" : "String"
  value     = local.seed[each.value]
}

# Copy targeted PIPELINE hierarchy paths from seed environment into requested ephemeral environment
resource "aws_ssm_parameter" "ephemeral_pipeline" {
  for_each  = { for k, v in local.pipeline_seed_paths : k => v if v != "" }
  key_id    = contains(split("/", each.key), "sensitive") ? local.kms_key_id : null
  name      = each.key
  overwrite = true
  type      = contains(split("/", each.key), "sensitive") ? "SecureString" : "String"
  value     = local.seed[each.value]
}

# Copy targeted SERVER hierarchy paths from seed environment into requested ephemeral environment
resource "aws_ssm_parameter" "ephemeral_server" {
  for_each  = local.server_seed_paths
  key_id    = contains(split("/", each.key), "sensitive") ? local.kms_key_id : null
  name      = each.key
  overwrite = true
  type      = contains(split("/", each.key), "sensitive") ? "SecureString" : "String"
  value     = local.seed[each.value]
  tier      = "Intelligent-Tiering"
}
