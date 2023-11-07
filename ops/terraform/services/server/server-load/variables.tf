variable "container_image_tag_node_override" {
  description = "Overrides the Container image URI used by the built load suite worker node lambda"
  type        = string
  default     = null
}

variable "create_locust_instance" {
  description = "When true, create the locust instance"
  type        = bool
  default     = false
}

variable "sqs_queue_name" {
  description = "The name of the SQS queue that will be polled for scaling notifications or stop signals"
  type        = string
  default     = "bfd-test-server-load"
}

variable "node_lambda_name" {
  description = "The name of the Locust worker node Lambda function that will be executed to spawn a Locust worker instance"
  type        = string
  default     = "bfd-test-server-load-node"
}

variable "test_host" {
  description = "The URL under test -- should match the given environment"
  type        = string
  default     = "https://test.bfd.cms.gov"
}

variable "locust_tags" {
  description = "Space-delimited. Run the locust tasks with ANY of the given @tag(s). Will run all tasks if not provided"
  type        = string
  default     = ""
}

variable "locust_exclude_tags" {
  description = "Space-delimited. The locust tasks with ANY of the given tags will be excluded from execution"
  type        = string
  default     = ""
}

variable "initial_worker_nodes" {
  description = "The number of initial Locust worker nodes to spawn before checking for stop signals. Useful for static load tests"
  type        = number
  default     = 0
}

variable "node_spawn_time" {
  description = "The amount of time to wait between spawning more Lambda Locust worker nodes. Does not affect initial spawned nodes"
  type        = number
  default     = 10
}

variable "max_spawned_nodes" {
  description = "The maximum number of Lambda worker nodes to spawn over the lifetime of a given test run. Does not account for failed nodes or nodes that reach their Lambda timeout"
  type        = number
  default     = 0
}

variable "max_spawned_users" {
  description = "The maximum number of simulated Locust users (not worker nodes) to spawn. Use this and spawn rate to constrain the load during a test run"
  type        = number
  default     = 0
}

variable "user_spawn_rate" {
  description = "The rate at which simulated Locust users (not worker nodes) will spawn. Set this equal to max_spawned_users if all users should be spawned immediately"
  type        = number
  default     = 1
}

variable "test_runtime_limit" {
  description = "Runtime limit in seconds. If stop_on_scaling is false, this limit is the total amount of time the load test has to run. If stop_on_scaling is true, this limit indicates the amount of time to check for scaling notifications during a test run before stopping"
  type        = number
  default     = 0
}

variable "coasting_time" {
  description = "The amount of time, in seconds, the load test should continue for after receiving a scaling notification. Ignored if stop_on_scaling is false. Ends immediately on operator stop signal"
  type        = number
  default     = 0
}

variable "warm_instance_target" {
  description = "The number of BFD Server instances to target before scaling causes the load test to stop"
  type        = number
  default     = 0
}

variable "stop_on_scaling" {
  description = "Whether the load test run should end, if coasting_time is zero, or start coasting once receiving a scaling notification. Set to false for scenarios where a static load test is desired"
  type        = bool
  default     = true
}

variable "stop_on_node_limit" {
  description = "Whether the load test run should end once the maximum Lambda worker node limit is reached. Set to false for scenarios where a static load test is desired"
  type        = bool
  default     = true
}

variable "ami_id_override" {
  default     = null
  description = "BFD Server Load override ami-id. Defaults to latest server-load AMI from `master`."
  type        = string
}

variable "server_load_dir" {
  default     = "/opt/server-load"
  description = "BFD Server Load directory."
  type        = string
}

variable "server_load_user" {
  default     = "bb-server-load"
  description = "BFD Server Load user."
  type        = string
}

variable "locust_master_port" {
  default     = 5557
  description = "The port to connect to that is used by the locust master for distributed load testing."
  type        = number
}
