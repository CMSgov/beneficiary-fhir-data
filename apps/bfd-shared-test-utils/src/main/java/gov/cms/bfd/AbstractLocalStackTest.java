package gov.cms.bfd;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for tests that need to use a shared localstack container. Refer to <a
 * href="https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">localstack
 * docs</a> for explanation of the singleton container pattern.
 */
public abstract class AbstractLocalStackTest {
  /** The system property defined in pom.xml containing the image to use for test AWS stack. */
  public static final String TEST_CONTAINER_AWS_IMAGE_PROPERTY = "its.testcontainer.aws.image";

  /** The default test container image to use when nothing is provided. */
  public static final String TEST_CONTAINER_AWS_IMAGE_DEFAULT = "localstack/localstack:2.2.0";

  /** Global container used by multiple tests. Will be destroyed automatically by test container. */
  protected static final LocalStackContainer localstack;

  // Creates the instance when first test class loads.   Starts all services that we use in BFD.
  // Add more here if we need any others.
  static {
    var localStackImageName =
        System.getProperty(TEST_CONTAINER_AWS_IMAGE_PROPERTY, TEST_CONTAINER_AWS_IMAGE_DEFAULT);
    localstack =
        new LocalStackContainer(
                DockerImageName.parse(localStackImageName).asCompatibleSubstituteFor("localstack"))
            .withServices(
                LocalStackContainer.Service.S3,
                LocalStackContainer.Service.SQS,
                LocalStackContainer.Service.SSM);
    localstack.start();
  }
}
