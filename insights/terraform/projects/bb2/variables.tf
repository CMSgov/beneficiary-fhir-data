variable "firehose" {
  description = "Environments you wish to create a firehose"
  type        = map
  default = {
    # add a map for each firehose to be created
    # the firehose name should be used as the key for each firehose
    test-perf-mon = {
      table_name = "events_test_perf_mon"
      project    = "bb2"
      database   = "bb2"
    }
    impl-perf-mon = {
      table_name = "events_impl_perf_mon"
      project = "bb2"
      database = "bb2"
    }
    prod-perf-mon = {
      table_name = "events_prod_perf_mon"
      project = "bb2"
      database = "bb2"
    }
  }
}

variable "buffer_size" {
  description = "Size of the buffer in MB"
  type        = number
  default     = 5
}

variable "buffer_interval" {
  description = "The interval of buffer refresh in SEC"
  type        = number
  default     = 300
}

variable "bb2_acct" {
  type        = string
}