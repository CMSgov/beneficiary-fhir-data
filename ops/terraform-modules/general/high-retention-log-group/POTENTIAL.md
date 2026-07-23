# TMP FILE WILL DELETE BERORE MERGING
# Potential Adoption Targets for `high-retention-log-group`

This document lists places in `ops/services` where the `high-retention-log-group` module can replace direct `aws_cloudwatch_log_group` resources.

## Best Immediate Candidates

These already use long retention patterns or are core ECS message logs where standardization is high value.

- `ops/services/04-server-ng/service.tf`
  - 5 log groups already use `local.ten_year_retention_days`.
  - Good first target for consistency (`kms_key_id`, `tags`, `prevent_destroy`).
- `ops/services/04-server-ng/s3logs.tf`
  - Single log group with long retention; easy drop-in replacement.
- `ops/services/04-server/service.tf`
  - 6 ECS-related log groups defined directly.
  - Good candidate to enforce explicit high retention and consistent hardening.
- `ops/services/03-migrator/main.tf`
- `ops/services/03-migrator-ng/main.tf`
- `ops/services/04-ccw-pipeline/main.tf`
- `ops/services/04-idr-pipeline/main.tf`
- `ops/services/04-npi-pipeline/main.tf`
- `ops/services/04-rda-pipeline/main.tf`
  - Similar ECS `messages` log group pattern across these services.

## Lambda Log Group Candidates (Policy Decision)

These are also eligible, but should follow a deliberate retention policy because Lambda log volume can increase cost.

- `ops/services/02-bene-prefs/lambda.tf`
- `ops/services/02-eft/outbound.tf`
- `ops/services/03-eft-o11y/notifier-lambda.tf`
- `ops/services/03-locust/run-locust.tf`
- `ops/services/04-ccw-pipeline/ccw-runner.tf`
- `ops/services/04-server/regression-wrapper.tf`
- `ops/services/06-ccw-pipeline-alarms/manifests-verifier.tf`
- `ops/services/06-server-alarms/error-alerter.tf`

## Exclusions / Review Before Migration

These have explicit shorter retention and likely should not move to this high-retention module unless policy changes.

- `ops/services/02-cluster/main.tf`
  - `ecs_events` log group uses `retention_in_days = 7`.
- `ops/services/04-idr-pipeline/events-lambda.tf`
  - Uses `retention_in_days = 30`.
- `ops/services/04-idr-pipeline/run-idr-lambda.tf`
  - Uses `retention_in_days = 30`.

## Suggested Rollout Order

1. Start with ECS service/pipeline message logs.
2. Apply to server and server-ng clusters.
3. Decide Lambda retention policy and migrate Lambda log groups after agreement.

## Notes

- Writer permissions like `logs:CreateLogStream` and `logs:PutLogEvents` are IAM role permissions for workloads, not module-level log group settings.
- Keep any IAM references updated when replacing direct resources with module outputs (for example, ARN/name references in service IAM policies).
