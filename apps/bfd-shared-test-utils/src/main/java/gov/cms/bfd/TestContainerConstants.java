package gov.cms.bfd;

import org.testcontainers.utility.DockerImageName;

/** Constants used with test containers based integration tests. */
public class TestContainerConstants {
  /** The localstack image and version to use for integration tests. */
  public static final DockerImageName LocalStackImageName =
      DockerImageName.parse("localstack/localstack:2.0.2");
}
