locals {
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", var.environment_name))])
}
