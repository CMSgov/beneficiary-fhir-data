# EC2 Instance to run the BFD Pipeline app.
#
module "ec2_instance" {
  source = "../ec2"

  env_config      = var.env_config
  role            = "etl"
  layer           = "data"
  az              = "us-east-1b" # Same as the master db

  launch_config   = {
    instance_type = "m5.2xlarge"
    volume_size   = 100 # GB
    ami_id        = var.launch_config.ami_id

    key_name      = var.launch_config.ssh_key_name
    profile       = var.launch_config.profile
    user_data_tpl = "pipeline_server.tpl"
    git_branch    = var.launch_config.git_branch
    git_commit    = var.launch_config.git_commit
  }

  mgmt_config     = {
    vpn_sg        = var.mgmt_config.vpn_sg
    tool_sg       = var.mgmt_config.tool_sg
    remote_sg     = var.mgmt_config.remote_sg
    ci_cidrs      = var.mgmt_config.ci_cidrs
  }
}

# Security group for application-specific (i.e. non-management) traffic.
#
resource "aws_security_group" "app" {
  name          = "bfd-${var.env_config.env}-etl-app"
  description   = "Access specific to the BFD Pipeline application."
  vpc_id        = var.env_config.vpc_id
  tags          = merge({Name="bfd-${var.env_config.env}-etl-app"}, var.env_config.tags)

  # Note: The application does not currently listen on any ports, so no ingress rules are needed.

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# App access to the database
#
resource "aws_security_group_rule" "allow_db_primary_access" {
  type                      = "ingress"
  from_port                 = 5432
  to_port                   = 5432
  protocol                  = "tcp"
  description               = "Allows BFD Pipeline access to the primary DB."

  security_group_id         = var.db_config.db_sg         # The SG associated with the primary DB.
  source_security_group_id  = aws_security_group.app.id   # The EC2 instance for the BFD Pipeline app.
}
