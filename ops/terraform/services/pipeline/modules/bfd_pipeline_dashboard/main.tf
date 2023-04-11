locals {
  env = terraform.workspace

  day_offset_to_monday = {
    "Sunday"    = 1
    "Monday"    = 0
    "Tuesday"   = -1
    "Wednesday" = -2
    "Thursday"  = -3
    "Friday"    = -4
    "Saturday"  = -5
  }
  duration_to_monday = {
    for day_name, day_offset in local.day_offset_to_monday : day_name => "${day_offset * 24}h"
  }
  current_time        = timestamp()
  current_date        = formatdate("YYYY-MM-DD", local.current_time)
  current_day_name    = formatdate("EEEE", local.current_time)
  # Technically, since EST observes daylight saving time, this timestamp will be 8 am EST during
  # the fall and winter. Supporting DST is fairly complicated, and Terraform's inadequate date
  # functions make it impractical to do so correctly 
  current_day_9am_est = "${local.current_date}T13:00:00Z"
  current_week_monday_9am_est = timeadd(
    local.current_day_9am_est, local.duration_to_monday[local.current_day_name]
  )
  next_monday_9am_est      = timeadd(local.current_week_monday_9am_est, "${7 * 24}h")
  next_next_monday_9am_est = timeadd(local.current_week_monday_9am_est, "${2 * 7 * 24}h")
  prev_monday_9am_est      = timeadd(local.current_week_monday_9am_est, "-${7 * 24}h")
  prev_prev_monday_9am_est = timeadd(local.current_week_monday_9am_est, "-${2 * 7 * 24}h")
}

resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = "bfd-${env}-pipeline"
  dashboard_body = templatefile(
    "${path.module}/templates/pipeline_dashboard.json.tftpl",
    merge({
      env = var.env
    }, local.client_ssls)
  )
}
