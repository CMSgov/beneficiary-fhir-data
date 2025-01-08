package gov.cms.bfd.pipeline.sharedutils.ec2;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.SetDesiredCapacityRequest;

/** EC2 client methods. */
public class AwsEc2Client {
  /** Logger for writing messages. */
  private static final Logger LOGGER = LoggerFactory.getLogger(AwsEc2Client.class);

  /** Auto-scaling client. */
  private final AutoScalingClient autoScalingClient;

  /** Auto-scaling group that contains the current EC2 instance. */
  private AutoScalingGroup autoScalingGroup;

  /** Creates a new EC2 client. */
  public AwsEc2Client() {
    autoScalingClient = AutoScalingClient.builder().defaultsMode(DefaultsMode.STANDARD).build();

    try (Ec2MetadataClient metadataClient = Ec2MetadataClient.create()) {
      String instanceId = metadataClient.get("/latest/meta-data/instance-id").asString();
      Optional<AutoScalingGroup> foundAutoScalingGroup =
          autoScalingClient.describeAutoScalingGroups().autoScalingGroups().stream()
              .filter(a -> isInstanceInAutoScalingGroup(a, instanceId))
              .findFirst();
      foundAutoScalingGroup.ifPresent(g -> this.autoScalingGroup = g);

    } catch (Exception ex) {
      LOGGER.warn(
          "Error creating EC2 autoscaling client. This is expect if the application is not running in EC2",
          ex);
    }
  }

  /**
   * Checks if the EC2 instance is in the given ASG.
   *
   * @param asg ASG
   * @param instanceId EC2 instance ID
   * @return boolean
   */
  private boolean isInstanceInAutoScalingGroup(AutoScalingGroup asg, String instanceId) {
    return asg.instances().stream().anyMatch(i -> i.instanceId().equals(instanceId));
  }

  /** Scales in the EC2 instance. */
  public void scaleInNow() {
    if (autoScalingGroup == null) {
      return;
    }
    autoScalingClient.setDesiredCapacity(
        SetDesiredCapacityRequest.builder()
            .autoScalingGroupName(autoScalingGroup.autoScalingGroupName())
            .desiredCapacity(0)
            .build());
  }
}
