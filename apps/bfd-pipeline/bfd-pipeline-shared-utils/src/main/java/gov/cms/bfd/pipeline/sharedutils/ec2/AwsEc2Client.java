package gov.cms.bfd.pipeline.sharedutils.ec2;

import java.time.Instant;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.PutScheduledUpdateGroupActionRequest;

/** test. */
public class AwsEc2Client implements Ec2Client {
  /** test. */
  private final AutoScalingClient autoScalingClient;

  /** test. */
  private final AutoScalingGroup autoScalingGroup;

  /** test. */
  private static final String SCALE_IN_ACTION_NAME = "scale_in_pipeline_finished";

  /** test. */
  private static final int CLEANUP_GRACE_PERIOD_SECONDS = 30;

  /** test. */
  public AwsEc2Client() {

    AutoScalingClient autoScalingClient =
        AutoScalingClient.builder().defaultsMode(DefaultsMode.STANDARD).build();

    try (Ec2MetadataClient metadataClient = Ec2MetadataClient.create()) {
      String instanceId = metadataClient.get("/latest/meta-data/instance-id").asString();
      AutoScalingGroup autoScalingGroup =
          autoScalingClient.describeAutoScalingGroups().autoScalingGroups().stream()
              .filter(a -> a.instances().stream().anyMatch(i -> i.instanceId().equals(instanceId)))
              .findFirst()
              .get();
      this.autoScalingClient = autoScalingClient;
      this.autoScalingGroup = autoScalingGroup;
    }
  }

  /** test. */
  public void scheduleScaleIn() {

    autoScalingClient.putScheduledUpdateGroupAction(
        PutScheduledUpdateGroupActionRequest.builder()
            .autoScalingGroupName(autoScalingGroup.autoScalingGroupName())
            .scheduledActionName(SCALE_IN_ACTION_NAME)
            .startTime(Instant.now().plusSeconds(CLEANUP_GRACE_PERIOD_SECONDS))
            .desiredCapacity(0)
            .build());
  }
}
