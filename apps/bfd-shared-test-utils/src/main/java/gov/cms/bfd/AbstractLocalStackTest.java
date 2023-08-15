package gov.cms.bfd;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for tests that need to use a shared localstack container. Refer to <a
 * href="https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">localstack
 * docs</a> for explanation of the singleton container pattern.
 */
public abstract class AbstractLocalStackTest {
  /** The localstack image and version to use for integration tests. */
  public static final DockerImageName LocalStackImageName =
      DockerImageName.parse("localstack/localstack:2.2.0");

  /** Global container used by multiple tests. Will be destroyed automatically by test container. */
  protected static final LocalStackContainer localstack;

  // Creates the instance when first test class loads.   Starts all services that we use in BFD.
  // Add more here if we need any others.
  static {
    localstack =
        new LocalStackContainer(LocalStackImageName)
            .withServices(
                LocalStackContainer.Service.S3,
                LocalStackContainer.Service.SQS,
                LocalStackContainer.Service.SSM);
    localstack.start();
  }
}
