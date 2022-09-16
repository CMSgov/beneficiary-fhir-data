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

variable "git_repo_version" {
  description = "Branch, tag, or hash. [Details on ansible's `git` module parameter version](https://docs.ansible.com/ansible/2.9/modules/git_module.html#parameter-version)"
  type        = string
  default     = "master"
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
  default     = 80
}

variable "max_spawned_users" {
  description = "The maximum number of simulated Locust users (not worker nodes) to spawn. Use this and spawn rate to constrain the load during a test run"
  type        = number
  default     = 5000
}

variable "user_spawn_rate" {
  description = "The rate at which simulated Locust users (not worker nodes) will spawn. Set this equal to max_spawned_users if all users should be spawned immediately"
  type        = number
  default     = 1
}

variable "test_runtime_limit" {
  description = "The maximum runtime for the current load test. Acts as a failsafe against runaway load testing"
  type        = string
  default     = "10m30s"
}

variable "coasting_time" {
  description = "The amount of time the load test should continue for after receiving a scaling notification. Does not effect operator stop signals"
  type        = number
  default     = 10
}

variable "warm_instance_target" {
  description = "The number of BFD Server instances to target before scaling causes the load test to stop"
  type        = number
  default     = 7
}

variable "stop_on_scaling" {
  description = "Whether the load test run should end once receiving a scaling notification. Set to false for scenarios where a static load test is desired"
  type        = bool
  default     = true
}

variable "stop_on_node_limit" {
  description = "Whether the load test run should end once the maximum Lambda worker node limit is reached. Set to false for scenarios where a static load test is desired"
  type        = bool
  default     = true
}