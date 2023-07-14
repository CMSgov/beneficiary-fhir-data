# How to Manually Adjust Scheduled RDS Application Autoscaling

**NOTE** As of May 2023, the operations detailed here are limited to the `test` environment and are believed to be strictly optional.
Scenarios where an operator elects to make manual adjustments _may_ yield more predictable performance, improved performance, and if nothing else, increased confidence in the results achieved in `test` regression suite execution.
Otherwise, it's deemed harmless to ignore these steps.

Follow this runbook if you need to:
- temporarily suspend scheduled RDS application autoscaling events in `test`
- scale-out `test` RDS cluster outside of normal scaled-out hours (M-F 0700-1900 ET)

Scenarios where these operations may be helpful include operations work in `test` such as:
- out-of-band database maintenance, e.g. upgrades, long-running operations such as database migrations, one-off script execution, etc
- deployments before or after normal scaled-out hours, especially when supporting incident resolution

## View All Scheduled Scaling Events

```sh
aws application-autoscaling describe-scheduled-actions --service-namespace rds \
  --resource-id cluster:bfd-test-aurora-cluster
```

**NOTE** This command reports all scheduled scaling events, regardless of the suspended/unsuspended state of scheduled scaling events.

## Suspend/Disable Scheduled Scaling Events

Suspend/Disable with the following command:

```sh
aws application-autoscaling register-scalable-target --service-namespace rds \
--scalable-dimension rds:cluster:ReadReplicaCount \
--resource-id cluster:bfd-test-aurora-cluster \
--suspended-state '{"ScheduledScalingSuspended":true}' && \
aws application-autoscaling describe-scalable-targets --service-namespace rds \
    --resource-id cluster:bfd-test-aurora-cluster \
    --query 'ScalableTargets[0].SuspendedState.ScheduledScalingSuspended'
```
**This should return `true` when disabling scheduled application autoscaling**


## Unsuspend/Enable Scheduled Scaling Events

Unsuspend/Enable with the following command:

```sh
aws application-autoscaling register-scalable-target --service-namespace rds \
--scalable-dimension rds:cluster:ReadReplicaCount \
--resource-id cluster:bfd-test-aurora-cluster \
--suspended-state '{"ScheduledScalingSuspended":false}' && \
aws application-autoscaling describe-scalable-targets --service-namespace rds \
    --resource-id cluster:bfd-test-aurora-cluster \
    --query 'ScalableTargets[0].SuspendedState.ScheduledScalingSuspended'
```
**This should return `false` when enabling scheduled application autoscaling**
