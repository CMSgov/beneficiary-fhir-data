package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for Database Application Url Test. */
public class DatabaseUtilsTest {
  /** Database url that is being set. */
  public String databaseUrl;
  /** The expected database url returned. */
  public String expectedDatabaseUrl;
  /** The actual database url that is returned. */
  public String actualDatabaseUrl;

  /** Setup before every tests. */
  @BeforeEach
  public void setup() {
    databaseUrl = "";
    expectedDatabaseUrl = "";
    actualDatabaseUrl = "";
  }

  /**
   * A test that returns the actual formatted jdbc url with application name without additional
   * parameters.
   */
  @Test
  public void returnsFormattedJdbcUrlWithApplicationNameWithoutAdditionalParameters() {
    databaseUrl = "jdbc:postgresql://localhost:54492/fhirdb";
    actualDatabaseUrl = DatabaseUtils.includeApplicationNameInDbUrl(databaseUrl);
    expectedDatabaseUrl =
        String.format(
            "%s?ApplicationName=%s", databaseUrl, DatabaseUtils.DATABASE_APPLICATION_NAME);
    assertEquals(expectedDatabaseUrl, actualDatabaseUrl);
  }

  /**
   * A test that returns the actual formatted jdbc url with application name with additional
   * parameters.
   */
  @Test
  public void returnsFormattedJdbcUrlWithApplicationNameWithAdditionalParameters() {
    databaseUrl = "jdbc:postgresql://localhost:54492/fhirdb?loggerLevel=OFF";
    actualDatabaseUrl = DatabaseUtils.includeApplicationNameInDbUrl(databaseUrl);
    expectedDatabaseUrl =
        String.format(
            "jdbc:postgresql://localhost:54492/fhirdb?loggerLevel=OFF&ApplicationName=%s",
            DatabaseUtils.DATABASE_APPLICATION_NAME);
    assertEquals(expectedDatabaseUrl, actualDatabaseUrl);
  }
}
