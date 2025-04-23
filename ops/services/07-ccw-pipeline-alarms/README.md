# `ccw-pipeline-alarms` Service

This subfolder contains the Terraform configuration for CloudWatch Alarms and alarming Lambdas related to the CCW Pipeline.

## Direct Terraservice Dependencies

_Note: This does not include transitive dependencies (dependencies of dependencies)._

| Terraservice | Required for Established? | Required for Ephemeral? | Details |
|---|---|---|---|
| `base` | Yes | Yes | N/A |
| `config` | Yes | Yes | N/A |
| `ccw-pipeline` | Yes | Yes | N/A |
| `ccw-pipeline-metrics` | Yes | Yes | N/A |
