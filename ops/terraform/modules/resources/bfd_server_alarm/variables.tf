variable "env" {
  type = string
}

variable "alarm_config" {
  type = object({
    alarm_name       = string,
    partner_name     = string,
    metric_prefix    = string,
    eval_periods     = string,
    period           = string,
    statistic        = string,
    ext_statistic    = string,
    threshold        = string,
    datapoints       = string,
    alarm_notify_arn = string,
    ok_notify_arn    = string
  })
}
