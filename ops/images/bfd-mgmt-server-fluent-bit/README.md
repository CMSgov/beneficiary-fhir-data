# `bfd-mgmt-server-fluent-bit` Image

This subdirectory contains the `Dockerfile` and Fluent Bit configuration, `server-fluentbit.conf`, for generating an AWS Fluent Bit image for use as the `log_router`/`awsfirelens` container in the `server` ECS Service.

As we are using ECS Fargate, it is required we use the [`file` type when specifying `config-file-type`](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/firelens-taskdef.html#:~:text=tasks%20hosted%20on%20aws%20fargate%20only%20support%20the%20file%20configuration%20file%20type.) in the Task Definition for the Fluent Bit container. Therefore, we need to build an image that contains the custom configuration within it.

Additionally, we use the `init` variant of the [AWS Fluent Bit](https://github.com/aws/aws-for-fluent-bit) image so that [some useful environment variables](https://github.com/aws-samples/amazon-ecs-firelens-examples/tree/mainline/examples/fluent-bit/init-metadata) are exposed within the container--namely, `ECS_TASK_ID`, which we use to name the generated Log Streams.

## Fluent Bit Configuration

All logs emitted from the `server` container in the `server` Service are written to `STDOUT` without any separation, so using the default `awslogs` log driver is not possible as we would like our generic logs seperate from our _access_ logs. So, the `server-fluentbit.conf` Fluent Bit configuration simply specifies that generic log messages go to a Log Group and Log Stream with the `messages` name, and _access_ logs go to a Log Stream/Log Group with the `access` name. Access logs are identified by the string `"HTTP_ACCESS"`.

This is functionally equivalent to the EC2-based reality where logs were being written to a `messages.json` and `access.json` Group.
