## VPC Private Local Zone for CNAME Records
module "local_zone" {
  source     = "../modules/resources/dns"
  env_config = { env = local.env, vpc_id = data.aws_vpc.main.id }
  public     = false
}
