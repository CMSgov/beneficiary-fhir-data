locals {
  env    = terraform.workspace
  region = data.aws_region.current.name

  tz_to_duration_offset = {
    "EDT" = "-4h"
    "EST" = "-5h"
  }
  tz_to_9am_eastern_in_utc = {
    "EDT" = "13:00:00"
    "EST" = "14:00:00"
  }
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

  current_eastern_tz = data.external.edt_or_est.result.timezone
  # Whenever EST (or est) is used here, it's encompassing _both_ EST and EDT
  current_time_utc = data.external.current_time_utc.result.rfc3339_timestamp
  offset_time_eastern = timeadd(
    local.current_time_utc, local.tz_to_duration_offset[local.current_eastern_tz]
  )
  current_date_est     = formatdate("YYYY-MM-DD", local.offset_time_eastern)
  current_day_name_est = formatdate("EEEE", local.offset_time_eastern)
  current_day_9am_est  = "${local.current_date_est}T${local.tz_to_9am_eastern_in_utc[local.current_eastern_tz]}Z"
  current_week_monday_9am_est = timeadd(
    local.current_day_9am_est, local.duration_to_monday[local.current_day_name_est]
  )
  next_monday_9am_est      = timeadd(local.current_week_monday_9am_est, "${7 * 24}h")
  next_next_monday_9am_est = timeadd(local.current_week_monday_9am_est, "${2 * 7 * 24}h")
  prev_monday_9am_est      = timeadd(local.current_week_monday_9am_est, "-${7 * 24}h")
  prev_prev_monday_9am_est = timeadd(local.current_week_monday_9am_est, "-${2 * 7 * 24}h")
}

resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = "bfd-${local.env}-pipeline"
  dashboard_body = templatefile(
    "${path.module}/templates/pipeline_dashboard.json.tftpl",
    {
      env                         = local.env
      region                      = local.region
      current_week_monday_9am_est = local.current_week_monday_9am_est
      next_monday_9am_est         = local.next_monday_9am_est
      next_next_monday_9am_est    = local.next_next_monday_9am_est
      prev_monday_9am_est         = local.prev_monday_9am_est
      prev_prev_monday_9am_est    = local.prev_prev_monday_9am_est
    }
  )
}
