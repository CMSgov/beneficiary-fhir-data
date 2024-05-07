package gov.cms.model.dsl.codegen.library;

/** POC. */
public final class TransformerConstants {
  /**
   * The base URL/URI/system for FHIR output when the domain concept in question is owned by (or at
   * least documented by) the Blue Button API team. Please note that the documentation on this site
   * is generated from the following project: <a href=
   * "https://github.com/CMSgov/bluebutton-site-static">bluebutton-site-static</a>.
   *
   * <p>This URL will never be used by itself; it will always be suffixed with a more specific path.
   */
  public static final String BASE_URL_BBAPI_RESOURCES = "https://bluebutton.cms.gov/resources";

  /** Test. */
  public static final String BASE_URL_CCW_VARIABLES = BASE_URL_BBAPI_RESOURCES + "/variables";

  /** Test. */
  public static final String IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID =
      BASE_URL_BBAPI_RESOURCES + "/identifier/claim-group";
}
