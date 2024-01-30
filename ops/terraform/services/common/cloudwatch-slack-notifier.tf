## Slack notifications for CloudWatch Alarms by environment
module "cw_alarms_slack_notifier" {
  count = local.is_ephemeral_env ? 0 : 1

  source = "./modules/bfd_cw_alarms_slack_notifier"
  env    = local.env
}
