variable "event_bridge_schedules" {
  description = "Map of EventBridge lambda schedules for envs"
  type        = map(any)
  default = {
    impl = "cron(10 9 ? * 2 *)"
    prod = "cron(10 10 ? * 2 *)"
  }
}
