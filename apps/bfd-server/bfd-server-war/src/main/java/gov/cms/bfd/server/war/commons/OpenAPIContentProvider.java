package gov.cms.bfd.server.war.commons;

/** Open API content value for /Patient resource bundle offset. */
public final class OpenAPIContentProvider {
  // NOTE: As much as we'd like to use Java 17 features for string concatentation,
  // our code formatter completely mangles MUCH of the following concatenated text
  // strings. So we'll fal back on tried-and-true structure to avoid
  // unpleasantness.

  /** Open API content short description for /Patient resource bundle offset. */
  public static final String PATIENT_SP_RES_ID_SHORT =
      "The patient resource identifier (_id) to search for";
  /** Open API content value for /Patient resource bundle offset. */
  public static final String PATIENT_SP_RES_ID_VALUE =
      "Fetch <i>Patient<i/> data using a FHIR <i>IdType<i/> identifier; an IdType"
          + " represents the logical identity for a resource, or as much of that"
          + " identity that is known. In FHIR, every resource must have a <i>logical ID<i/> which is"
          + " defined by the FHIR specification as:"
          + "<p><code>"
          + "Any combination of upper or lower case ASCII letters ('A'..'Z', and 'a'..'z', numerals ('0'..'9'),"
          + " '-' and '.', with a length limit of 64 characters. (This might be an integer, an un-prefixed OID, UUID"
          + " or any other identifier pattern that meets these constraints.)</code></p><p>"
          + "This class contains that logical ID, and can optionally also contain a relative or absolute URL"
          + " representing the resource identity; the following are all valid values for IdType, and all might"
          + " represent the same resource:</p><ul>"
          + "<li><code>123</code> (just a resource\u0027s ID)</li>"
          + "<li><code>Patient/123</code> (a relative identity)</li>"
          + "<li><code>http://example.com/Patient/123 (an absolute identity)</code></li>";

  /** Open API content short description for /Patient resource bundle offset. */
  public static final String PATIENT_START_INDEX_SHORT =
      "The starting offset used for result pagination";
  /** Open API content value for /Patient resource bundle offset. */
  public static final String PATIENT_START_INDEX_VALUE =
      "When fetching a <i>Bundle Response</i> using pagination, this URL parameter represents an offset"
          + " (starting point) into the list of elements for the <i>Request</i>."
          + "<p/>Example:<ul>"
          + "<li>startIndex=100</li>";

  /** Open API content short description for /Patient resource bundle offset. */
  public static final String PATIENT_LAST_UPDATED_SHORT = "When the resource version last changed";
  /** Open API content value for /Patient resource bundle offset. */
  public static final String PATIENT_LAST_UPDATED_VALUE =
      "Only satisfy the Search if the Beneficiary\u0027s <i>last_updated</i> Date"
          + " falls within a specified <i>DateRange<i/>. A DateRange can include both"
          + " lo and hi date values, only a lo date value, or only a hi date value."
          + "<p/>Examples:<ul>"
          + "<li>&_lastUpdated=gt2023-01-02&_lastUpdated=lt2023-05-01</li>"
          + "<li>&_lastUpdated=gt2023-01-02</li>"
          + "<li>&_lastUpdated=lt2023-05-01</li></ul><p/>"
          + "<a href=\"https://localhost:9876/v2/fhir/docs/foo.html\">"
          + "See BFD Documentation for _lastUpdated</a><p/>";

  /** Open API content short description for /Patient's Part D contract ID to be used. */
  public static final String PATIENT_PARTD_CONTRACT_SHORT = "Part D coverage contract identifier";
  /**
   * Open API content value for /Patient's Part D contract ID to be used in determining Part D
   * events.
   */
  public static final String PATIENT_PARTD_CONTRACT_VALUE =
      "When searching for a Patient\u0027s Part D events information, this resource identifies"
          + " the Part D contract value that will be used when determining eligibility."
          + "<p/>Example:<ul>"
          + "<li>_has:Coverage.extension=ABCD</li>";

  /**
   * Open API short description for /Patient's Part D reference year to be used in determining Part
   * D events.
   */
  public static final String PATIENT_PARTD_REFYR_SHORT = "Part D contract reference year";
  /**
   * Open API content value for /Patient's Part D reference year to be used in determining Part D
   * events.
   */
  public static final String PATIENT_PARTD_REFYR_VALUE =
      "When searching for a Patient\u0027s Part D events information, this resource identifies"
          + " the reference year that will be applied when determining applicable Part D events."
          + "<p/>Example:<ul>"
          + "<li>_has:Coverage.rfrncyr=2023</li>";

  /**
   * Open API short descriptionfor /Patient's Part D event data; provides a URI for the 'next' set
   * of data.
   */
  public static final String PATIENT_PARTD_CURSOR_SHORT = "The cursor used for result pagination";
  /**
   * Open API content value for /Patient's Part D event data; provides a URI for the 'next' set of
   * data.
   */
  public static final String PATIENT_PARTD_CURSOR_VALUE =
      "Provide a pagination cursor for processing Patient\u0027s Part D events information; this resource identifies"
          + " a numeric offset into a result set."
          + "<p/>Example:<ul>"
          + "<li>cursor=200</li>";

  /**
   * Open API short descriptionfor /Patient's Part D event data; provides a URI for the 'next' set
   * of data.
   */
  public static final String EOB_CLAIM_TYPE_SHORT = "A list of BFD claim types to include";
  /**
   * Open API content value for /Patient's Part D event data; provides a URI for the 'next' set of
   * data.
   */
  public static final String EOB_CLAIM_TYPE_VALUE =
      "A list of one or more comma-separated claim types to be included in the request;"
          + " within BFD, the claim types represent an <i>OR</i> inclusion logic meaning any claims matching one of the specified"
          + " claim types will be checked."
          + "<p/>Supported Claim Type values:<ul>"
          + "<li>carrier</li>"
          + "<li>inpatient</li>"
          + "<li>outpatient</li>"
          + "<li>hha</li>"
          + "<li>hospice</li>"
          + "<li>snf</li>"
          + "<li>dme</li>"
          + "<li>partd</li></ul><p>"
          + "BFD also supports a wildcard claims type denoted by an &#42; ; this includes all claim types in the filter."
          + "<p/>Examples:<ul>"
          + "<li>type=carrier,inpatient,snf,dme</li>"
          + "<li>type=outpatient</li>"
          + "<li>type=&#42;</li>";

  /**
   * Open API short descriptionfor /Patient's Part D event data; provides a URI for the 'next' set
   * of data.
   */
  public static final String EOB_SERVICE_DATE_SHORT =
      "Include claims whose <i>thru date</i> fall within the given range";
  /**
   * Open API content value for /Patient's Part D event data; provides a URI for the 'next' set of
   * data.
   */
  public static final String EOB_SERVICE_DATE_VALUE =
      "Only satisfy the Search request if a claim\u0027s <i>thru</i> Date"
          + " falls within a specified <i>DateRange<i/>. A DateRange can include both"
          + " lo and hi date values, only a lo date value, or only a hi date value."
          + "<p/>Examples:<ul>"
          + "<li>&service-date=gt2023-01-02&service-date=lt2023-05-01</li>"
          + "<li>&service-date=gt2023-01-02</li>"
          + "<li>&_service-date=lt2023-05-01</li></ul>";

  /**
   * Open API short descriptionfor /Patient's Part D event data; provides a URI for the 'next' set
   * of data.
   */
  public static final String EOB_EXCLUDE_SAMSHA_SHORT =
      "If <i>true</i>, exclude any (all) SAMHSA-related claims";
  /**
   * Open API content value for /Patient's Part D event data; provides a URI for the 'next' set of
   * data.
   */
  public static final String EOB_EXCLUDE_SAMSHA_VALUE =
      "The <i>Substance Abuse and Mental Health Services Administration</i> (SAMHSA)"
          + " is the agency within the U.S. Department of HHS that leads public health efforts to advance the behavioral health of the nation."
          + "<p/>Setting this flag to <i>true</i>, modifies the request to filter out all SAMSHA-related claims ifrom the response."
          + "<p/>Example:<ul>"
          + "<li>&excludeSAMHSA=true</li>";

  /** Open API content short description for /Patient resource bundle offset. */
  public static final String BENEFICIARY_SP_RES_ID_SHORT =
      "Identifier resource for the covered party";
  /** Open API content value for /Patient resource bundle offset. */
  public static final String BENEFICIARY_SP_RES_ID_VALUE =
      "Fetch <i>Beneficiary<i/> data using a FHIR <i>IdType<i/> identifier; an IdType"
          + " represents the logical identity for a resource, or as much of that"
          + " identity that is known. In FHIR, every resource must have a <i>logical ID<i/> which is"
          + " defined by the FHIR specification as:"
          + "<p><code>"
          + "Any combination of upper or lower case ASCII letters ('A'..'Z', and 'a'..'z', numerals ('0'..'9'),"
          + " '-' and '.', with a length limit of 64 characters. (This might be an integer, an un-prefixed OID, UUID"
          + " or any other identifier pattern that meets these constraints.)</code></p><p>"
          + "This class contains that logical ID, and can optionally also contain a relative or absolute URL"
          + " representing the resource identity; the following are all valid values for IdType, and all might"
          + " represent the same resource:</p><ul>"
          + "<li><code>123</code> (just a resource\u0027s ID)</li>"
          + "<li><code>beneficiary/123</code> (a relative identity)</li>"
          + "<li><code>http://example.com/beneficiary/123 (an absolute identity)</code></li>";
}
