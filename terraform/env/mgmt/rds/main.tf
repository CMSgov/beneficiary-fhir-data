provider "aws" {
  region = "us-east-1"
}

#TODO jzulim

module "rds" {
  source = "../../modules/resources/rds"

  allocated_storage     =
  storage_type          =
  iops                  =
  instance_class        =
  identifier            =
  multi_az              =
  name                  =
  kms_key_id            =
  db_subnet_group_name  =

  vpc_security_group_ids = [
    "",
  ]

  tags = {
    "cpm backup"    = "Daily Weekly Monthly"
    "workload-type" = "production"
  }
}

data "aws_route53_zone" "selected" {
  name         = ""
  private_zone = true
}

resource "aws_route53_record" "rds" {
  zone_id = "${data.aws_route53_zone.selected.zone_id}"
  name    = "db.${data.aws_route53_zone.selected.name}"
  type    = "CNAME"
  ttl     = "300"
  records = ["${replace(module.rds.endpoint, "/:\\d*$/", "")}"]
}
