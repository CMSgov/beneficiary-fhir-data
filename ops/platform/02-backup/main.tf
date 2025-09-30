data "aws_backup_vault" "main" {
  name = "CMS_OIT_Backups_Vault"
}

data "aws_backup_vault" "dr" {
  provider = aws.secondary
  name     = "CMS_OIT_Backups_Vault"
}

data "aws_iam_role" "backup" {
  name = "cms-oit-aws-backup-service-role"
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  service              = local.service
  relative_module_root = "ops/platform/02-backup"
}

resource "aws_backup_plan" "main" {
  name = "bfd-daily-weekly"

  rule {
    rule_name         = "weekly35"
    target_vault_name = data.aws_backup_vault.main.name
    schedule          = "cron(0 3 ? * SAT *)"

    lifecycle {
      delete_after = 35
    }

    copy_action {
      destination_vault_arn = data.aws_backup_vault.dr.arn
      lifecycle {
        delete_after = 7
      }
    }
  }
  rule {
    rule_name         = "daily3"
    target_vault_name = data.aws_backup_vault.main.name
    schedule          = "cron(0 1 * * ? *)"

    lifecycle {
      delete_after = 3
    }
  }
  tags = { layer = "data" }
}


resource "aws_backup_selection" "main" {
  iam_role_arn = data.aws_iam_role.backup.arn
  name         = "bfd-daily-weekly-backup-selection"
  plan_id      = aws_backup_plan.main.id

  selection_tag {
    type  = "STRINGEQUALS"
    key   = "bfd_backup"
    value = "daily3_weekly35"
  }
}
