resource "aws_db_instance" "default" {
  allocated_storage      = "${var.allocated_storage}"
  storage_type           = "${var.storage_type}"
  iops                   = "${var.iops}"
  instance_class         = "${var.instance_class}"
  name                   = "${var.name}"
  identifier             = "${var.identifier != "" ? var.identifier : replace(var.name, "_", "-")}"
  multi_az               = "${var.multi_az}"
  kms_key_id             = "${var.kms_key_id}"
  db_subnet_group_name   = "${var.db_subnet_group_name}"
  vpc_security_group_ids = ["${var.vpc_security_group_ids}"]
  tags                   = "${var.tags}"
  monitoring_interval    = "${var.monitoring_interval}"
  monitoring_role_arn    = "${var.monitoring_role_arn}"
  engine                 = "postgres"
  engine_version         = "9.6.6"
  username               = "bfdmaster"
  storage_encrypted      = true
  copy_tags_to_snapshot  = true
  skip_final_snapshot    = true
  snapshot_identifier    = "${var.snapshot_identifier}"
  apply_immediately      = true

  lifecycle {
    ignore_changes = ["password"]
  }
}
