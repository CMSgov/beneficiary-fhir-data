package gov.cms.bfd.pipeline.rif.load;

import gov.cms.bfd.model.rif.schema.DatabaseTestHelper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Unit tests for {@link gov.cms.bfd.pipeline.rif.load.RifLoader}. */
public final class RifLoaderTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderTest.class);

  /**
   * Runs a couple of fake HICNs through {@link
   * gov.cms.bfd.pipeline.rif.load.RifLoader#computeHicnHash(LoadAppOptions, SecretKeyFactory,
   * String)} to verify that the expected result is produced.
   */
  @Test
  public void computeHicnHash() {
    LoadAppOptions options =
        RifLoaderTestUtils.getLoadOptions(DatabaseTestHelper.getTestDatabase());
    options =
        new LoadAppOptions(
            1000,
            "nottherealpepper".getBytes(StandardCharsets.UTF_8),
            options.getDatabaseUrl(),
            options.getDatabaseUsername(),
            options.getDatabasePassword(),
            options.getLoaderThreads(),
            options.isIdempotencyRequired(),
            options.isFixupsEnabled(),
            options.getFixupThreads());
    LOGGER.info(
        "salt/pepper: {}", Arrays.toString("nottherealpepper".getBytes(StandardCharsets.UTF_8)));
    LOGGER.info("hash iterations: {}", 1000);
    SecretKeyFactory secretKeyFactory = RifLoader.createSecretKeyFactory();

    /*
     * These are the two samples from `dev/design-decisions-readme.md` that
     * the frontend and backend both have tests to verify the result of.
     */
    Assert.assertEquals(
        "d95a418b0942c7910fb1d0e84f900fe12e5a7fd74f312fa10730cc0fda230e9a",
        RifLoader.computeHicnHash(options, secretKeyFactory, "123456789A"));
    Assert.assertEquals(
        "6357f16ebd305103cf9f2864c56435ad0de5e50f73631159772f4a4fcdfe39a5",
        RifLoader.computeHicnHash(options, secretKeyFactory, "987654321E"));
  }

  /**
   * Runs a couple of fake MBIs through {@link
   * gov.cms.bfd.pipeline.rif.load.RifLoader#computeMbiHash(LoadAppOptions, SecretKeyFactory,
   * String)} to verify that the expected result is produced.
   */
  @Test
  public void computeMbiHash() {
    LoadAppOptions options =
        RifLoaderTestUtils.getLoadOptions(DatabaseTestHelper.getTestDatabase());
    options =
        new LoadAppOptions(
            1000,
            "nottherealpepper".getBytes(StandardCharsets.UTF_8),
            options.getDatabaseUrl(),
            options.getDatabaseUsername(),
            options.getDatabasePassword(),
            options.getLoaderThreads(),
            options.isIdempotencyRequired(),
            options.isFixupsEnabled(),
            options.getFixupThreads());
    LOGGER.info(
        "salt/pepper: {}", Arrays.toString("nottherealpepper".getBytes(StandardCharsets.UTF_8)));
    LOGGER.info("hash iterations: {}", 1000);
    SecretKeyFactory secretKeyFactory = RifLoader.createSecretKeyFactory();

    /*
     * These are the two samples from `dev/design-decisions-readme.md` that
     * the frontend and backend both have tests to verify the result of.
     */
    Assert.assertEquals(
        "ec49dc08f8dd8b4e189f623ab666cfc8b81f201cc94fe6aef860a4c3bd57f278",
        RifLoader.computeMbiHash(options, secretKeyFactory, "3456789"));
    Assert.assertEquals(
        "742086db6bf338dedda6175ea3af8ca5e85b81fda9cc7078004a4d3e4792494b",
        RifLoader.computeMbiHash(options, secretKeyFactory, "2456689"));
  }
}
