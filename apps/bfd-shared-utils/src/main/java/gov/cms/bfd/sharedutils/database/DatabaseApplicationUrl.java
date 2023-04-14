package gov.cms.bfd.sharedutils.database;

/** DatabaseApplicationUrl sets the application name for a jdbc url. */
public class DatabaseApplicationUrl {
  /**
   * Sets the db url to include the application name if its not a hsqldb.
   *
   * @param databaseUrl database url to use
   * @return formatted string of url with application name
   */
  public static String includeApplicationNameInDbUrl(String databaseUrl) {
    if (!databaseUrl.contains("jdbc:hsqldb") && !databaseUrl.contains("ApplicationName=")) {
      if (databaseUrl.contains("?")) {
        String[] dbUrl = databaseUrl.split("\\?");
        return String.format("%s?ApplicationName=bfdserver&%s", dbUrl[0], dbUrl[1]);
      } else {
        return String.format("%s?ApplicationName=bfdserver", databaseUrl);
      }
    }

    return databaseUrl;
  }
}
