
# `bfd-mgmt-server-fluent-bit` Image

This subdirectory contains the `Dockerfile` and Fluent Bit configuration, `server-fluentbit.conf`, for generating an AWS Fluent Bit image for use as the `log_router`/`awsfirelens` container in the `server` ECS Service.

As we are using ECS Fargate, it is required we use the [`file` type when specifying `config-file-type`](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/firelens-taskdef.html#:~:text=tasks%20hosted%20on%20aws%20fargate%20only%20support%20the%20file%20configuration%20file%20type.) in the Task Definition for the Fluent Bit container. Therefore, we need to build an image that contains the custom configuration within it.

Additionally, we use the `init` variant of the [AWS Fluent Bit](https://github.com/aws/aws-for-fluent-bit) image so that [some useful environment variables](https://github.com/aws-samples/amazon-ecs-firelens-examples/tree/mainline/examples/fluent-bit/init-metadata) are exposed within the container--namely, `ECS_TASK_ID`, which we use to name the generated Log Streams.

## Fluent Bit Configuration

> [!IMPORTANT]
> It is _vital_ to note that logs emitted by the Docker logging daemon are _wrapped_ in a JSON format, e.g.:
>
> ```json
> {"log": "log_message", ...}
> ```
>
> Where `"log_message"` could be an escaped JSON string, e.g. `"{\"some_key\": \"some_value\"}"`. This is relevant for our structured logs emitted by both Servers, as the _real_ log is the escaped JSON within the `log` key.

### v1/v2 Server

All logs emitted from the `server` container in the `server` Service are written to `STDOUT` without any separation, so using the default `awslogs` log driver is not possible as we would like our generic logs separate from our _access_ logs. So, the `server-fluentbit.conf` Fluent Bit configuration simply specifies that generic log messages go to a Log Group and Log Stream with the `messages` name, and _access_ logs go to a Log Stream/Log Group with the `access` name. Access logs are identified by the string `"HTTP_ACCESS"`.

This is functionally equivalent to the EC2-based reality where logs were being written to a `messages.json` and `access.json` Group.

### v3 Server

Logs emitted from the `server` container are routed to one of 3 different Log Groups in CloudWatch depending on their content:

1. `messages`: This Log Group contains _relevant_, structured log messages emitted by the Server application; e.g. request logs, startup logs, etc.
2. `healthchecks`: This Log Group contains structured, healthcheck logs that are irrelevant when investigating issues or otherwise
    - A log message is considered a "healthcheck" if it is a JSON log, its `mdc.remoteAddress` is the loopback IPv4 or IPv6 address, and `mdc.uri` ends in `metadata`
3. `nonjson`: This Log Group contains unstructured, non-JSON logs
