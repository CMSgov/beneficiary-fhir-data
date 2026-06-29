package gov.cms.bfd;

import org.ministack.testcontainers.MiniStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for tests that need to use a shared MiniStack container. Refer to <a
 * href="https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">MiniStack
 * docs</a> for explanation of the singleton container pattern.
 */
// False positive - not a utility class
@SuppressWarnings("java:S1118")
public abstract class AbstractMiniStackTest {
  /** The system property defined in pom.xml containing the image to use for test AWS stack. */
  public static final String TEST_CONTAINER_AWS_IMAGE_PROPERTY = "its.testcontainer.aws.image";

  /** Global container used by multiple tests. Will be destroyed automatically by test container. */
  protected static final MiniStackContainer miniStack;

  // Creates the instance when first test class loads.   Starts all services that we use in BFD.
  // Add more here if we need any others.
  static {
    var miniStackImageName = System.getProperty(TEST_CONTAINER_AWS_IMAGE_PROPERTY);
    miniStack =
        new MiniStackContainer(
            DockerImageName.parse(miniStackImageName)
                .asCompatibleSubstituteFor("ministackorg/ministack"));
    miniStack.start();
  }
}
