package gov.cms.bfd.server.war.commons;

/** Open API content short and value descriptions for each BFD API endpoint. */
public final class OpenAPIContentProvider {

  /** Open API content short description for /Patient's identifier parameter. */
  public static final String PATIENT_SP_RES_ID_SHORT =
      "The patient resource identifier (_id) to search for";

  /** Open API content value for /Patient's identifier parameter. */
  public static final String PATIENT_SP_RES_ID_VALUE =
"""
**NOTE: TO MAKE A REQUEST TO THIS ENDPOINT IT IS REQUIRED TO CHOOSE ONE OUT OF THE FOLLOWING THREE PARAMETERS AT A GIVEN TIME (_id, identifier, _has:Coverage.extension)**

Fetch _Patient_ data using a FHIR _IdType_ identifier; an IdType
represents the logical identity for a resource, or as much of that
identity that is known. In FHIR, every resource must have a _logical ID_ which is
defined by the [FHIR specification](https://www.hl7.org/fhir/r4/datatypes.html#id) as:

`Any combination of upper or lower case ASCII letters ('A'..'Z', and 'a'..'z', numerals ('0'..'9'),
'-' and '.', with a length limit of 64 characters. (This might be an integer, an un-prefixed OID, UUID
or any other identifier pattern that meets these constraints.)`

This class contains that logical ID, and can optionally also contain a relative or absolute URL
representing the resource identity; the following are all valid values for IdType, and all might
represent the same resource:
  - `_id=567834`
  - `_id=1234`""";

  /** Open API content short description for /Patient's identifier parameter. */
  public static final String PATIENT_SP_IDENTIFIER_SHORT = "The patient identifier to search for";

  /** Open API content value for /Patient's identifier parameter. */
  public static final String PATIENT_SP_IDENTIFIER_VALUE =
"""
**NOTE: TO MAKE A REQUEST TO THIS ENDPOINT IT IS REQUIRED TO CHOOSE ONE OUT OF THE FOLLOWING THREE PARAMETERS AT A GIVEN TIME (_id, identifier, _has:Coverage.extension)**

Fetch _Patient_ data using a FHIR _identifier_; an identifier contains a set of values that
include the logical identity for a resource. In FHIR, the _identifier_ is a parent element
defined by the [FHIR specification](https://www.hl7.org/fhir/r4/datatypes-definitions.html#Identifier) as:

`A string, typically numeric or alphanumeric, that is associated with a single object or entity within a given system.
Typically, identifiers are used to connect content in resources to external content available in other frameworks or protocols.
Identifiers are associated with objects and may be changed or retired due to human or system process and errors.`

This class contains the identifier, which is usually represented as a URL, along with a single, url encoded, pipe-delimited key|value pair, with the value as
(mbi, hicn, etc); the following are all valid values for Identifier, and all might represent the same resource:
  - `identifier=https://bluebutton.cms.gov/resources/identifier/hicn-hash|<your hicn hash>`
  - `identifier=https://bluebutton.cms.gov/resources/identifier/mbi-hash|<your mbi hash>`""";

  /** Open API content short description for /Patient's resource bundle offset. */
  public static final String PATIENT_START_INDEX_SHORT =
      "The starting offset used for result pagination";

  /** Open API content value for /Patient's resource bundle offset. */
  public static final String PATIENT_START_INDEX_VALUE =
      """
      When fetching a _Bundle Response_ using pagination, this URL parameter represents an offset
      (starting point) into the list of elements for the _Request_.
      It is optional and defaults to 1 if not supplied.
      A value 0 is not allowed and negative indices are not currently supported.

      Example:
         - `startIndex=100`""";

  /** Open API content short description for /Patient's lastUpdated parameter. */
  public static final String PATIENT_LAST_UPDATED_SHORT = "When the resource version last changed";

  /** Open API content value for /Patient's lastUpdated parameter. */
  public static final String PATIENT_LAST_UPDATED_VALUE =
"""
Only satisfy the Search if the Beneficiary's `last_updated` Date falls within a specified _DateRange_.
A _DateRange_ can be defined by providing less than `lt` and/or greater than `gt` values.
This parameter can be included in a request one or more times.

Inexact timestamps are accepted, but not recommended, since the input will implicitly be converted to use the server's timezone.

Examples:
  - `_lastUpdated=gt2023-01-02T00:00+00:00&_lastUpdated=lt2023-05-01T00:00+00:00` defines a range between two provided dates
  - `_lastUpdated=gt2023-01-02T00:00+00:00` defines a range between the provided date and today
  - `_lastUpdated=lt2023-05-01T00:00+00:00` defines a range from the earliest available records until the provided date""";

  /** Open API content short description for /Patient's Part D contract ID to be used. */
  public static final String PATIENT_PARTD_CONTRACT_SHORT = "Part D coverage contract identifier";

  /**
   * Open API content value for /Patient's Part D contract ID to be used in determining Part D
   * events.
   */
  public static final String PATIENT_PARTD_CONTRACT_VALUE =
"""
**NOTE: TO MAKE A REQUEST TO THIS ENDPOINT IT IS REQUIRED TO CHOOSE ONE OUT OF THE FOLLOWING THREE PARAMETERS AT A GIVEN TIME (_id, identifier, _has:Coverage.extension)**

When searching for a Patient's Part D events information, this resource identifies
the Part D contract value that will be used when determining eligibility.

Example:
   - `_has:Coverage.extension=<Part D Contract ID Here>`
   - `_has:Coverage.extension=ABCD`""";

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
      """
      When searching for a Patient's Part D events information, this resource identifies
      the reference year that will be applied when determining applicable Part D events.

      Example:
         - `_has:Coverage.rfrncyr=2023`""";

  /**
   * Open API short description for /Patient's Part D event data; provides a URI for the _next_ set
   * of data.
   */
  public static final String PATIENT_PARTD_CURSOR_SHORT = "The cursor used for result pagination";

  /**
   * Open API content value for /Patient's Part D event data; provides a URI for the _next_ set of
   * data.
   */
  public static final String PATIENT_PARTD_CURSOR_VALUE =
"""
Provides a pagination cursor or numeric _offset_ for processing Patient's Part D events information.

Examples:
   - `cursor=200` the first record is the 201st record
   - `cursor=1000` the first record is the 1001st record""";

  /***
   * Open API content for the _count parameter.
   */
  public static final String COUNT_SHORT = "The number of records to return";

  /***
   * Open API content for the _count parameter.
   */
  public static final String COUNT_VALUE =
"""
Provides the number of records to be used for pagination.

Examples:
  - `_count=10`: return 10 values.
""";

  /** Open API content short description for /Patient's identifier parameter. */
  public static final String BENEFICIARY_SP_RES_ID_SHORT =
      "Identifier resource for the covered party";

  /** Open API content value for /Patient's identifier parameter. */
  public static final String BENEFICIARY_SP_RES_ID_VALUE =
"""
Fetch _Beneficiary_ data using a FHIR _IdType_ identifier; an IdType
represents the logical identity for a resource, or as much of that
identity that is known. In FHIR, every resource must have a _logical ID_ which is
defined by the [FHIR specification](https://www.hl7.org/fhir/r4/datatypes.html#id) as:

`Any combination of upper or lower case ASCII letters ('A'..'Z', and 'a'..'z', numerals ('0'..'9'),
'-' and '.', with a length limit of 64 characters. (This might be an integer, an un-prefixed OID, UUID
or any other identifier pattern that meets these constraints.)`

This class contains that logical ID, and can optionally also contain a relative or absolute URL
representing the resource identity; the following are all valid values for IdType, and all might
represent the same resource:
- `beneficiary=567834`
- `beneficiary=1234`""";

  /** Open API content short description for /Coverage's profile parameter. */
  public static final String COVERAGE_SP_SUPPORTED_PROFILE_SHORT =
      "Profiles for use cases supported";

  /** Open API content value for /Coverage's profile parameter. */
  public static final String COVERAGE_SP_SUPPORTED_PROFILE_VALUE =
      """
      Filters the response to contain only _Coverage_ data that support the specified profile.

      The following are all valid values for profile:
      - `http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Coverage`
      - `http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-Coverage`""";

  /** Open API short description for /ExplanationOfBenefit's EOB claim type parameter. */
  public static final String EOB_CLAIM_TYPE_SHORT = "A list of BFD claim types to include";

  /** Open API content value for /ExplanationOfBenefit's EOB claim type parameter. */
  public static final String EOB_CLAIM_TYPE_VALUE =
"""
One or more comma-delimited claim types that filters the response to contain only EoBs that include the specified claim types.
This is optional and defaults to `*` denoting all types.
Supported values for claim type:
   - `*` all types (default)
   - `carrier`
   - `dme`
   - `hha`
   - `hospice`
   - `inpatient`
   - `outpatient`
   - `partd`
   - `snf`

Examples:
   - `type=carrier,inpatient,snf,dme`
   - `type=outpatient`
   - `type=*`""";

  /** Open API short description for /ExplanationOfBenefit's serviceDate parameter. */
  public static final String EOB_SERVICE_DATE_SHORT =
      "Filter claims by the _billable period_. In other words, filter claims whose _through date_ fall within the given range";

  /** Open API content value for /ExplanationOfBenefit's serviceDate parameter. */
  public static final String EOB_SERVICE_DATE_VALUE =
"""
Only satisfy the Search request if a claim's _billable period_
falls within a specified _DateRange_. A _DateRange_ can be
defined by providing less than `lt` and/or greater than `gt` values.
This parameter can be included in a request one or more times.

Examples:
  - `service-date=gt2023-01-02&service-date=lt2023-05-01` defines a range between two provided dates
  - `service-date=gt2023-01-02` defines a range between the provided date and today
  - `service-date=lt2023-05-01` defines a range from the earliest available records until the provided date""";

  /** Open API short description for /ExplanationOfBenefit's excludeSAMSHA parameter. */
  public static final String EOB_EXCLUDE_SAMSHA_SHORT =
      "If _true_, exclude any (all) SAMHSA-related claims";

  /** Open API content value for /ExplanationOfBenefit's excludeSAMSHA parameter. */
  public static final String EOB_EXCLUDE_SAMSHA_VALUE =
"""
The _Substance Abuse and Mental Health Services Administration_ (SAMHSA)
is the agency within the U.S. Department of HHS that leads public health efforts to advance the behavioral health of the nation.
Setting this flag to _true_, modifies the request to filter out all SAMSHA-related claims from the response.

Examples:
   - `excludeSAMHSA=true`""";

  /** Open API content short description for partially adjudicated claim MBI ID to be used. */
  public static final String PAC_MBI_SHORT =
      "The patient medicare beneficiary identifier (MBI) to search for.";

  /** Open API content value for partially adjudicated claim MBI ID to be used. */
  public static final String PAC_MBI_VALUE =
"""
 **NOTE: THIS IS A REQUIRED FIELD**

Fetch _Beneficiary_ data using a FHIR _MBI_ identifier; an MBI
represents the medicare benficiary ID, or as much of that
defined by the [FHIR specification](https://terminology.hl7.org/NamingSystem-cmsMBI.html) as:

```
A combination of upper case ASCII letters ('A'..'Z except for S, L, O, I, B, and Z.', numerals ('0'..'9'),
'-', with a length limit of 11 characters.
```

The following are all valid values for MBI, and all might
represent the same resource:
   - `mbi=9AB2WW3GR44` (unhashed MBI)
   - `mbi=82273caf4d2c3b5a8340190ae3575950957ce469e593efd7736d60c3b39d253c` (hashed)
   - `mbi=1S00E00HA26` (synthetic MBI)""";

  /**
   * Open API content short description for partially adjudicated claim MBI ID being hashed or not.
   */
  public static final String PAC_IS_HASHED =
      "A boolean indicating whether or not the MBI is hashed.";

  /** Open API content value for partially aducated claim MBI ID being hashed or not. */
  public static final String PAC_IS_HASHED_VALUE =
"""
Setting this flag to _true_, provides tax number in the EOB transformed data for the response.

Example:
  - `isHashed=true`""";

  /** Open API short description for partially adjudicated claim type parameter. */
  public static final String PAC_CLAIM_TYPE_SHORT =
      "A list of BFD partially adjudicated claim types to include";

  /** Open API content value for partially adjudicated claim type parameter. */
  public static final String PAC_CLAIM_TYPE_VALUE =
"""
A list of one or more comma-separated claim types to be included in the request;
within BFD, the claim types represent an _OR_ inclusion logic meaning any claims matching one of the specified
claim types will be checked

Supported Claim Type values:
  - `fiss`
  - `mcs`

Examples:
  - `type=fiss,mcs`
  - `type=fiss`""";

  /** Open API short description for partially adjudicated claims excludeSAMSHA parameter. */
  public static final String PAC_EXCLUDE_SAMSHA_SHORT =
      "If _true_, exclude any (all) SAMHSA-related claims";

  /** Open API content value for partially adjudicated claims excludeSAMSHA parameter. */
  public static final String PAC_EXCLUDE_SAMSHA_VALUE =
"""
The _Substance Abuse and Mental Health Services Administration_ (SAMHSA)
is the agency within the U.S. Department of HHS that leads public health efforts to advance the behavioral health of the nation.
Setting this flag to _true_, modifies the request to filter out all SAMSHA-related claims from the response.
_ClaimResponse_ doesn't contain any SAMHSA potential data. Due to the 1:1 relationship between _ClaimResponse_
and _Claim_, any ClaimResponse resource will be excluded when the associated Claim resource contains SAMHSA data.

Example:
  - `excludeSAMHSA=true`""";

  /** Open API short description for partially adjudicated claims data. */
  public static final String PAC_SERVICE_DATE_SHORT =
      "Include claims whose date fall within the given range";

  /** Open API content value for partially adjudicated claims data. */
  public static final String PAC_SERVICE_DATE_VALUE =
      """
      Only satisfy the Search request if a claim's Date
      falls within a specified _DateRange_. A _DateRange_ can be
      defined by providing less than `lt` and/or greater than `gt` values.
      This parameter can be included in a request one or more times.

      Examples:
         - `service-date=gt2023-01-02&service-date=lt2023-05-01`
         - `service-date=gt2023-01-02`
         - `service-date=lt2023-05-01`""";

  /** Open API short description for /ExplanationOfBenefit's _elements parameter. */
  public static final String EOB_CLIENT_ELEMENTS_SHORT =
      "A comma-separated list of strings with which to filter the FHIR response by element/path.";

  /** Open API content value for /ExplanationOfBenefit's _elements parameter. */
  public static final String EOB_CLIENT_ELEMENTS_VALUE =
"""
The _elements parameter consists of a comma-separated list of base element names. Only elements that are listed are to be returned.
 Clients SHOULD list desired elements in a resource as part of
the list of elements. The list of elements does not apply to included resources.
 Servers are not obliged to return just the requested elements; mandatory elements are returned whether
 they are requested or not. Each value of the list may also be a period-separated path to a specified element.
""";
}
