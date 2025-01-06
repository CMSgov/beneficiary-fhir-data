package gov.cms.bfd.pipeline.sharedutils.ec2;

import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;

/** test. */
public class AwsEc2Client {
  /** test. */
  private AutoScalingClient autoScalingClient;

  /** test. */
  private AutoScalingGroup autoScalingGroup;

  /** test. */
  private static final String SCALE_IN_ACTION_NAME = "scale_in_pipeline_finished";

  /** test. */
  private static final int CLEANUP_GRACE_PERIOD_SECONDS = 30;

  /** test. */
  public AwsEc2Client() {
    AutoScalingClient autoScalingClient =
        AutoScalingClient.builder().defaultsMode(DefaultsMode.STANDARD).build();

    try (Ec2MetadataClient metadataClient = Ec2MetadataClient.create()) {
      try {
        String instanceId = metadataClient.get("/latest/meta-data/instance-id").asString();
        AutoScalingGroup autoScalingGroup =
            autoScalingClient.describeAutoScalingGroups().autoScalingGroups().stream()
                .filter(
                    a -> a.instances().stream().anyMatch(i -> i.instanceId().equals(instanceId)))
                .findFirst()
                .get();
        this.autoScalingClient = autoScalingClient;
        this.autoScalingGroup = autoScalingGroup;
      } catch (Exception ex) {
        this.autoScalingClient = null;
      }
    }
  }

  /** test. */
  public void scaleInNow() {
    if (autoScalingClient == null) {
      return;
    }
    autoScalingClient.setDesiredCapacity(
        SetDesiredCapacityRequest.builder()
            .autoScalingGroupName(autoScalingGroup.autoScalingGroupName())
            .desiredCapacity(0)
            .build());
  }
}
