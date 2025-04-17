package gov.cms.bfd.server.war.commons;

import static gov.cms.bfd.server.war.NPIOrgLookup.ENTITY_TYPE_CODE_ORGANIZATION;
import static gov.cms.bfd.server.war.NPIOrgLookup.ENTITY_TYPE_CODE_PROVIDER;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;
import gov.cms.bfd.model.codebook.model.Value;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.CanonicalOperation;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains shared methods used for looking up NPI and Procedure code descriptions and logging
 * unfound entries only once.
 */
public final class CommonTransformerUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonTransformerUtils.class);

  /** file containing NPI code entries. */
  private static final String PROCEDURE_CODE_FILE = "PRCDR_CD.txt";

  /** Stores the diagnosis ICD codes and their display values. */
  private static Map<String, String> icdMap = null;

  /** Stores the procedure codes and their display values. */
  private static Map<String, String> procedureMap = null;

  /** Stores the NPI codes and their display values. */
  private static Map<String, String> npiMap = null;

  /** default NPI org description for failed code lookup. */
  private static final String NPI_ORG_DISPLAY_DEFAULT = "UNKNOWN";

  /**
   * string value used when constructing {@link IdDt} Patient identifier; since the class will never
   * be renamed, OK to use the STU3 string value.
   */
  private static final String PATIENT_SIMPLE_CLASSNAME =
      org.hl7.fhir.dstu3.model.Patient.class.getSimpleName();

  /**
   * string value used when constructing {@link IdDt} Coverage identifier; since the class will
   * never be renamed, OK to use the STU3 string value.
   */
  private static final String COVERAGE_SIMPLE_CLASSNAME =
      org.hl7.fhir.dstu3.model.Coverage.class.getSimpleName();

  /**
   * Constant for setting and retrieving the attribute from the request that determines if the
   * client can see SAMHSA data.
   */
  public static final String SHOULD_FILTER_SAMHSA = "SHOULD_FILTER_SAMHSA";

  /**
   * Tracks the {@link CcwCodebookInterface} that have already had code lookup failures due to
   * missing {@link Value} matches. Why track this? To ensure that we don't spam log events for
   * failed lookups over and over and over. This was needed to fix CBBF-162, where those log events
   * were flooding our logs and filling up the drive.
   *
   * @see CommonTransformerUtils#calculateCodingDisplay(IAnyResource, CcwCodebookInterface, String)
   */
  private static final Set<CcwCodebookInterface> codebookLookupMissingFailures =
      ConcurrentHashMap.newKeySet();

  /**
   * Tracks the {@link CcwCodebookInterface} that have already had code lookup failures due to
   * duplicate {@link Value} matches. Why track this? To ensure that we don't spam log events for
   * failed lookups over and over and over. This was needed to fix CBBF-162, where those log events
   * were flooding our logs and filling up the drive.
   */
  private static final Set<CcwCodebookInterface> codebookLookupDuplicateFailures =
      ConcurrentHashMap.newKeySet();

  /** Tracks the icd codes that have already had code lookup failures. */
  private static final Set<String> icdCodeLookupMissingFailures = ConcurrentHashMap.newKeySet();

  /** Tracks the procedure codes that have already had code lookup failures. */
  private static final Set<String> procedureLookupMissingFailures = ConcurrentHashMap.newKeySet();

  /** Tracks the NPI codes that have already had code lookup failures. */
  private static final Set<String> npiCodeLookupMissingFailures = ConcurrentHashMap.newKeySet();

  /** Instance of NPIOrgLookup. */
  private static NPIOrgLookup npiOrgLookup;

  /**
   * Sets NPIOrgLookup to lookup NPI information.
   *
   * @param orgLookup The instance of NPIOrgLookup.
   */
  public static void setNpiOrgLookup(NPIOrgLookup orgLookup) {
    npiOrgLookup = orgLookup;
  }

  /**
   * Builds an id for an FHIR STU3/R4 ExplanationOfBenefit.
   *
   * <p>Internally BFD treats claimId as a Long (db bigint); however, within FHIR, an Identifier has
   * a data type of StringType that does not constrain itself to numeric. So this convenience method
   * will continue to exist as a means to create a {@link
   * org.hl7.fhir.dstu3.model.ExplanationOfBenefit#getId()} or {@link
   * org.hl7.fhir.r4.model.ExplanationOfBenefit#getId()} whose claim ID is not numeric. This
   * non-numeric handling may be used in integration tests to trigger {@link
   * ca.uhn.fhir.rest.server.exceptions.InvalidRequestException}.
   *
   * @param claimType the {@link ClaimType} to compute an {@link
   *     org.hl7.fhir.dstu3.model.ExplanationOfBenefit#getId()} or {@link
   *     org.hl7.fhir.r4.model.ExplanationOfBenefit#getId()} for
   * @param claimId the <code>claimId</code> field value (e.g. from {@link
   *     CarrierClaim#getClaimId()}) to compute an {@link
   *     org.hl7.fhir.dstu3.model.ExplanationOfBenefit#getId()} or {@link
   *     org.hl7.fhir.r4.model.ExplanationOfBenefit#getId()} for
   * @return the {@link String} that is used as an {@link
   *     org.hl7.fhir.dstu3.model.ExplanationOfBenefit#getId()} or {@link
   *     org.hl7.fhir.r4.model.ExplanationOfBenefit#getId()} value to use for the specified <code>
   *     claimId</code> value
   */
  public static String buildEobId(ClaimType claimType, String claimId) {
    return String.format("%s-%s", claimType.name().toLowerCase(), claimId);
  }

  /**
   * Builds an id for an ExplanationOfBenefit.
   *
   * @param claimType the {@link ClaimType} to compute an STU3/R4 ExplanationOfBenefit#getId() for
   * @param claimId the <code>claimId</code> field value (e.g. from {@link
   *     CarrierClaim#getClaimId()}) to compute an{@link
   *     org.hl7.fhir.dstu3.model.ExplanationOfBenefit#getId()} or {@link
   *     org.hl7.fhir.r4.model.ExplanationOfBenefit#getId()} for
   * @return the {@link String} that is used as an {@link
   *     org.hl7.fhir.dstu3.model.ExplanationOfBenefit#getId()} or {@link
   *     org.hl7.fhir.r4.model.ExplanationOfBenefit#getId()} value to use for the specified <code>
   *     claimId</code> value
   */
  public static String buildEobId(ClaimType claimType, Long claimId) {
    return String.format("%s-%d", claimType.name().toLowerCase(), claimId);
  }

  /**
   * Builds a patient id from a {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} to calculate the patient identifier value for
   * @return the {@link org.hl7.fhir.dstu3.model.Patient#getId()} or {@link
   *     org.hl7.fhir.r4.model.Patient#getId()} value that will be used for the specified {@link
   *     Beneficiary}
   */
  public static IdDt buildPatientId(Beneficiary beneficiary) {
    return buildPatientId(beneficiary.getBeneficiaryId());
  }

  /**
   * Builds a patient id from a beneficiary id.
   *
   * @param beneficiaryId the {@link Beneficiary#getBeneficiaryId()} to calculate the {@link
   *     ca.uhn.fhir.model.primitive.IdDt} for
   * @return the {@link ca.uhn.fhir.model.primitive.IdDt} for STU3/R4 or patient identifier
   */
  public static IdDt buildPatientId(Long beneficiaryId) {
    return new IdDt(PATIENT_SIMPLE_CLASSNAME, String.valueOf(beneficiaryId));
  }

  /**
   * Builds a patient id from a {@link Beneficiary} and a {@link MedicareSegment}.
   *
   * @param medicareSegment the {@link MedicareSegment} to compute a Coverage identifier for
   * @param beneficiary the {@link Beneficiary} to compute a Coverage identifier for
   * @return the {@link ca.uhn.fhir.model.primitive.IdDt} for STU3/R4 or Coverage identifier
   */
  public static IdDt buildCoverageId(MedicareSegment medicareSegment, Beneficiary beneficiary) {
    return buildCoverageId(medicareSegment, beneficiary.getBeneficiaryId());
  }

  /**
   * Builds a patient id from a {@link Beneficiary} and a {@link MedicareSegment}.
   *
   * @param medicareSegment the {@link MedicareSegment} to compute a Coverage identifier for
   * @param beneficiary the {@link Beneficiary} to compute a Coverage identifier for
   * @param profile a supported CARIN {@link Profile}
   * @return the {@link ca.uhn.fhir.model.primitive.IdDt} for STU3/R4 or Coverage identifier
   */
  public static IdDt buildCoverageId(
      MedicareSegment medicareSegment, Beneficiary beneficiary, Profile profile) {
    return buildCoverageId(medicareSegment, beneficiary.getBeneficiaryId(), profile);
  }

  /**
   * Builds a patient id from a beneficiary id and a {@link MedicareSegment}.
   *
   * @param medicareSegment the {@link MedicareSegment} to compute a Coverage identifier for
   * @param beneficiaryId the {@link Beneficiary#getBeneficiaryId()} value to compute an STU3/R4
   *     Coverage for
   * @return the {@link ca.uhn.fhir.model.primitive.IdDt} for STU3/R4 or Coverage identifier
   */
  public static IdDt buildCoverageId(MedicareSegment medicareSegment, Long beneficiaryId) {
    return new IdDt(
        COVERAGE_SIMPLE_CLASSNAME,
        String.format("%s-%d", medicareSegment.getUrlPrefix(), beneficiaryId));
  }

  /**
   * Builds a patient id from a beneficiary id and a {@link MedicareSegment}.
   *
   * @param medicareSegment the {@link MedicareSegment} to compute a Coverage identifier for
   * @param beneficiaryId the {@link Beneficiary#getBeneficiaryId()} value to compute an STU3/R4
   *     Coverage for
   * @param profile a supported CARIN {@link Profile}
   * @return the {@link ca.uhn.fhir.model.primitive.IdDt} for STU3/R4 or Coverage identifier
   */
  public static IdDt buildCoverageId(
      MedicareSegment medicareSegment, Long beneficiaryId, Profile profile) {
    return new IdDt(
        COVERAGE_SIMPLE_CLASSNAME,
        String.format("%s-%d", medicareSegment.getUrlPrefix(profile), beneficiaryId));
  }

  /**
   * Internally BFD treats beneficiaryId as a Long (db bigint); however, within FHIR, an {@link
   * ca.uhn.fhir.model.primitive.IdDt} does not constrain itself to numeric. So this convenience
   * method will continue to exist as a means to create a non-numeric IdDt. This non-numeric
   * handling may be used in integration tests to trigger {@link
   * ca.uhn.fhir.rest.server.exceptions.InvalidRequestException}.
   *
   * @param medicareSegment the {@link MedicareSegment} to compute a {@link
   *     org.hl7.fhir.dstu3.model.Coverage#getId()} or {@link
   *     org.hl7.fhir.r4.model.Coverage#getId()} for
   * @param beneficiaryId the {@link Beneficiary#getBeneficiaryId()} value to compute a {@link
   *     org.hl7.fhir.dstu3.model.Coverage#getId()} or {@link
   *     org.hl7.fhir.r4.model.Coverage#getId()} for
   * @return the {@link org.hl7.fhir.dstu3.model.Coverage#getId()} or {@link
   *     org.hl7.fhir.r4.model.Coverage#getId()} value to use for the specified values
   */
  public static IdDt buildCoverageId(MedicareSegment medicareSegment, String beneficiaryId) {
    return new IdDt(
        COVERAGE_SIMPLE_CLASSNAME,
        String.format("%s-%s", medicareSegment.getUrlPrefix(), beneficiaryId));
  }

  /**
   * Converts a {@link LocalDate} to a {@link Date} using the system timezone.
   *
   * <p>We use the system TZ here to ensure that the date doesn't shift at all, as FHIR will just
   * use this as an unzoned Date (I think, and if not, it's almost certainly using the same TZ as
   * this system).
   *
   * @param localDate the {@link LocalDate} to convert
   * @return a {@link Date} version of the specified {@link LocalDate}
   */
  public static Date convertToDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Creates a url-encoded version of the specified text.
   *
   * @param urlText the URL or URL portion to be encoded
   * @return a URL-encoded version of the specified text
   */
  public static String urlEncode(String urlText) {
    try {
      return URLEncoder.encode(urlText, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Validate the from/thru dates to ensure the from date is before or the same as the thru date.
   *
   * @param dateFrom start date {@link LocalDate}
   * @param dateThrough through date {@link LocalDate} to verify
   */
  public static void validatePeriodDates(LocalDate dateFrom, LocalDate dateThrough) {
    if (dateFrom == null || dateThrough == null) {
      return;
    }
    // FIXME see CBBD-236 (ETL service fails on some Hospice claims "From
    // date is after the Through Date")
    // We are seeing this scenario in production where the from date is
    // after the through date so we are just logging the error for now.
    if (dateFrom.isAfter(dateThrough))
      LOGGER.debug(
          String.format(
              "Error - From Date '%s' is after the Through Date '%s'", dateFrom, dateThrough));
  }

  /**
   * Validate the from/thru dates to ensure the from date is before or the same as the thru date.
   *
   * @param dateFrom the date from
   * @param dateThrough the date through
   */
  public static void validatePeriodDates(
      Optional<LocalDate> dateFrom, Optional<LocalDate> dateThrough) {
    if (dateFrom.isPresent() && dateThrough.isPresent()) {
      validatePeriodDates(dateFrom.get(), dateThrough.get());
    }
  }

  /**
   * Builds the provider name from NPIData.
   *
   * @param npiData the NPIData
   * @return a String with the Provider name.
   */
  public static String buildProviderFromNpiData(NPIData npiData) {
    String entityTypeCode = npiData.getEntityTypeCode();
    if (entityTypeCode.equals(ENTITY_TYPE_CODE_PROVIDER)) {
      String[] name =
          new String[] {
            npiData.getProviderNamePrefix(),
            npiData.getProviderFirstName(),
            npiData.getProviderMiddleName(),
            npiData.getProviderLastName(),
            npiData.getProviderNameSuffix(),
            npiData.getProviderCredential()
          };
      return Arrays.stream(name)
          .map(Strings::trimToNull)
          .filter(Objects::nonNull)
          .collect(Collectors.joining(" "));
    } else if (entityTypeCode.equals(ENTITY_TYPE_CODE_ORGANIZATION)) {
      return npiData.getProviderOrganizationName();
    }
    return null;
  }

  /**
   * Sets a placeholder for NPICodeDisplay for future enrichment.
   *
   * @param npiCode NPI code
   * @return the npi code display
   */
  public static String retrieveNpiCodeDisplay(String npiCode) {
    return "replaceProvider[" + npiCode + "]";
  }

  /**
   * Retrieves the Procedure code and display value from a Procedure code look up file.
   *
   * @param procedureCode procedure code
   * @return the procedure code display
   */
  public static String retrieveProcedureCodeDisplay(String procedureCode) {
    if (procedureCode.isEmpty()) {
      return null;
    }
    // read the entire Procedure code file the first time and put in a Map
    if (procedureMap == null) {
      // There's a race condition here: we may initialize this static field more than
      // once if multiple concurrent requests come in. However, the assignment is
      // atomic, so the
      // race and reinitialization is harmless other than maybe wasting a bit of time.
      procedureMap = readProcedureCodeFile();
    }
    if (procedureMap.containsKey(procedureCode.toUpperCase())) {
      return procedureMap.get(procedureCode);
    }

    // log which Procedure codes we couldn't find a match for in our procedure codes
    if (!procedureLookupMissingFailures.contains(procedureCode)) {
      procedureLookupMissingFailures.add(procedureCode);
      /*LOGGER.info(
      "No procedure code display value match found for procedure code: {} in resource {}.",
      procedureCode,
      PROCEDURE_CODE_FILE);*/
    }
    return null;
  }

  /**
   * Reads all the procedure codes and display values from the PRCDR_CD.txt file Refer to the README
   * file in the src/main/resources directory.
   *
   * @return the map of procedure codes
   */
  private static Map<String, String> readProcedureCodeFile() {

    Map<String, String> procedureCodeMap = new HashMap<String, String>();
    try (final InputStream procedureCodeDisplayStream =
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(PROCEDURE_CODE_FILE);
        final BufferedReader reader =
            new BufferedReader(new InputStreamReader(procedureCodeDisplayStream))) {

      // extract the procedure codes and display values and put in a map
      // for easy retrieval of the display value.
      // icdColumns[0] is PRCDR_CD
      // icdColumns[1] is PRCDR_DESC(i.e. 8295 is INJECT TENDON OF HAND description)

      String line = "";
      reader.readLine();
      while ((line = reader.readLine()) != null) {
        String[] icdColumns = line.split("\t");
        procedureCodeMap.put(icdColumns[0], icdColumns[1]);
      }
      reader.close();
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read Procedure code data.", e);
    }
    return procedureCodeMap;
  }

  /**
   * Calculates the {@link org.hl7.fhir.dstu3.model.Coding#getDisplay()} or {@link
   * org.hl7.fhir.r4.model.Coding#getDisplay()} value to use for the specified {@link
   * CcwCodebookInterface}; {@link Optional#empty()} if no matching display value could be
   * determined.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link
   *     org.hl7.fhir.dstu3.model.Coding} or {@link org.hl7.fhir.r4.model.Coding#getDisplay()} will
   *     be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param code the FHIR {@link org.hl7.fhir.dstu3.model.Coding#getCode()} or {@link
   *     org.hl7.fhir.r4.model.Coding#getDisplay()} value to determine a corresponding value for
   * @return the {@link org.hl7.fhir.dstu3.model.Coding#getDisplay()} or {@link
   *     org.hl7.fhir.r4.model.Coding#getDisplay()} value to use for the specified {@link
   *     CcwCodebookInterface}; or {@link Optional#empty()} if no matching display value could be
   *     determined.
   */
  public static Optional<String> calculateCodingDisplay(
      IAnyResource rootResource, CcwCodebookInterface ccwVariable, String code) {
    if (rootResource == null || ccwVariable == null || code == null) {
      throw new IllegalArgumentException();
    }
    if (!ccwVariable.getVariable().getValueGroups().isPresent()) {
      throw new BadCodeMonkeyException("No display values for Variable: " + ccwVariable);
    }
    /*
     * We know that the specified CCW Variable is coded, but there's no guarantee
     * that the Coding's code matches one of the known/allowed Variable values;
     * data is messy. When that happens, we log the event and return normally.
     * The log event will at least allow for further investigation, if warranted.
     * Also, there's a chance that the CCW Variable data itself is messy, and
     * that the Coding's code matches more than one value -- log those events too.
     */
    List<Value> matchingVariableValues =
        ccwVariable.getVariable().getValueGroups().get().stream()
            .flatMap(g -> g.getValues().stream())
            .filter(v -> v.getCode().equals(code))
            .collect(Collectors.toList());
    if (matchingVariableValues.size() == 1) {
      return Optional.of(matchingVariableValues.get(0).getDescription());
    } else if (matchingVariableValues.isEmpty()) {
      if (!codebookLookupMissingFailures.contains(ccwVariable)) {
        codebookLookupMissingFailures.add(ccwVariable);
        /*if (ccwVariable instanceof CcwCodebookVariable) {
          LOGGER.info(
              "No display value match found for {}.{} in resource '{}/{}'.",
              CcwCodebookVariable.class.getSimpleName(),
              ccwVariable.name(),
              rootResource.getClass().getSimpleName(),
              rootResource.getId());
        } else {
          LOGGER.info(
              "No display value match found for {}.{} in resource '{}/{}'.",
              CcwCodebookMissingVariable.class.getSimpleName(),
              ccwVariable.name(),
              rootResource.getClass().getSimpleName(),
              rootResource.getId());
        }*/
      }
      return Optional.empty();
    } else if (matchingVariableValues.size() > 1) {
      if (!codebookLookupDuplicateFailures.contains(ccwVariable)) {
        // Note: The race condition here (from concurrent requests) is harmless.
        codebookLookupDuplicateFailures.add(ccwVariable);
        /*if (ccwVariable instanceof CcwCodebookVariable) {
          LOGGER.info(
              "Multiple display value matches found for {}.{} in resource '{}/{}'.",
              CcwCodebookVariable.class.getSimpleName(),
              ccwVariable.name(),
              rootResource.getClass().getSimpleName(),
              rootResource.getId());
        } else {
          LOGGER.info(
              "Multiple display value matches found for {}.{} in resource '{}/{}'.",
              CcwCodebookMissingVariable.class.getSimpleName(),
              ccwVariable.name(),
              rootResource.getClass().getSimpleName(),
              rootResource.getId());
        }*/
      }
      return Optional.empty();
    } else {
      throw new BadCodeMonkeyException();
    }
  }

  /**
   * Retrieves the Diagnosis display value from a Diagnosis code look up file.
   *
   * @param icdCode Diagnosis code
   * @return the icd code display
   */
  public static String retrieveIcdCodeDisplay(String icdCode) {

    if (icdCode.isEmpty()) {
      return null;
    }
    // There's a race condition here: we may initialize this static field more than
    // once if multiple concurrent requests come in. However, the assignment is
    // atomic, so the
    // race and reinitialization is harmless other than maybe wasting a bit of time.
    // read the entire ICD file the first time and put in a Map
    if (icdMap == null) {
      icdMap = readIcdCodeFile();
    }
    if (icdMap.containsKey(icdCode.toUpperCase())) {
      return icdMap.get(icdCode);
    }

    // log which ICD codes we couldn't find a match for in our downloaded ICD file
    if (!icdCodeLookupMissingFailures.contains(icdCode)) {
      icdCodeLookupMissingFailures.add(icdCode);
      /*LOGGER.info(
      "No ICD code display value match found for ICD code {} in resource {}.",
      icdCode,
      "DGNS_CD.txt");*/
    }
    return null;
  }

  /**
   * Reads ALL the ICD codes and display values from the DGNS_CD.txt file. Refer to the README file
   * in the src/main/resources directory.
   *
   * @return the map of idc codes
   */
  private static Map<String, String> readIcdCodeFile() {
    Map<String, String> map = new HashMap<String, String>();

    try (final InputStream icdCodeDisplayStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("DGNS_CD.txt");
        final BufferedReader reader =
            new BufferedReader(new InputStreamReader(icdCodeDisplayStream))) {

      // Want to extract the ICD Diagnosis codes and display values and put
      // in a map for easy retrieval of display value;
      // icdColumns[1] is DGNS_DESC(i.e. 7840 code is HEADACHE description)

      String line = "";
      reader.readLine();
      while ((line = reader.readLine()) != null) {
        String[] icdColumns = line.split("\t");
        map.put(icdColumns[0], icdColumns[1]);
      }
      reader.close();
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read ICD code data.", e);
    }
    return map;
  }

  /**
   * Records the JPA query details in {@link BfdMDC}.
   *
   * @param queryId an ID that identifies the type of JPA query being run, e.g. "bene_by_id"
   * @param queryDurationNanoseconds the JPA query's duration, in nanoseconds
   * @param recordCount the number of top-level records (e.g. JPA entities) returned by the query
   */
  public static void recordQueryInMdc(
      String queryId, long queryDurationNanoseconds, long recordCount) {
    String keyPrefix = String.format("jpa_query_%s", queryId);
    BfdMDC.put(
        String.format("%s_duration_nanoseconds", keyPrefix),
        Long.toString(queryDurationNanoseconds));
    BfdMDC.put(
        String.format("%s_duration_milliseconds", keyPrefix),
        Long.toString(queryDurationNanoseconds / 1000000));
    BfdMDC.put(String.format("%s_record_count", keyPrefix), Long.toString(recordCount));
  }

  /**
   * Gets the metrics registry timer.
   *
   * @param metricRegistry the metric registry
   * @param baseRegistryName the base registry name
   * @param registrySubNames the registry sub names
   * @return the metrics registry timer
   */
  public static Timer.Context createMetricsTimer(
      MetricRegistry metricRegistry, String baseRegistryName, String... registrySubNames) {
    return metricRegistry.timer(MetricRegistry.name(baseRegistryName, registrySubNames)).time();
  }

  /**
   * Gets the part D ccw codebook month map.
   *
   * @return the ccw codebook month map
   */
  public static Map<Integer, CcwCodebookVariable> getPartDCcwCodebookMonthMap() {
    return new HashMap<>() {
      {
        put(1, CcwCodebookVariable.PTDCNTRCT01);
        put(2, CcwCodebookVariable.PTDCNTRCT02);
        put(3, CcwCodebookVariable.PTDCNTRCT03);
        put(4, CcwCodebookVariable.PTDCNTRCT04);
        put(5, CcwCodebookVariable.PTDCNTRCT05);
        put(6, CcwCodebookVariable.PTDCNTRCT06);
        put(7, CcwCodebookVariable.PTDCNTRCT07);
        put(8, CcwCodebookVariable.PTDCNTRCT08);
        put(9, CcwCodebookVariable.PTDCNTRCT09);
        put(10, CcwCodebookVariable.PTDCNTRCT10);
        put(11, CcwCodebookVariable.PTDCNTRCT11);
        put(12, CcwCodebookVariable.PTDCNTRCT12);
      }
    };
  }

  /**
   * Checks that the eob id passed in is not null, has no version set, had an id set, and matches
   * the expected EOB id pattern. If so, returns the matcher to pull the eob id parts from, else
   * throws an {@link InvalidRequestException}.
   *
   * @param versionIdPart the version id part of the eob id
   * @param idPart the id part of the eob id
   * @return the eob id matcher (contains the pieces of the eob id) if valid
   */
  public static Matcher validateAndReturnEobMatcher(Long versionIdPart, String idPart) {

    /*
     * A {@link Pattern} that will match the {@link ExplanationOfBenefit#getId()}s used in this
     * application, e.g. <code>pde-1234</code> or <code>pde--1234</code> (for negative IDs).
     */
    Pattern EOB_ID_PATTERN = Pattern.compile("(\\p{Alpha}+)-(-?\\p{Digit}+)");

    if (versionIdPart != null) {
      throw new InvalidRequestException("ExplanationOfBenefit ID must not define a version");
    }

    if (idPart == null || idPart.trim().isEmpty()) {
      throw new InvalidRequestException("Missing required ExplanationOfBenefit ID");
    }

    Matcher eobIdMatcher = EOB_ID_PATTERN.matcher(idPart);
    if (!eobIdMatcher.matches()) {
      throw new InvalidRequestException(
          "ExplanationOfBenefit ID pattern: '"
              + idPart
              + "' does not match expected pattern: {alphaString}-{idNumber}");
    }
    return eobIdMatcher;
  }

  /**
   * Publish mdc operation name.
   *
   * @param endpoint the endpoint
   * @param operationOptions the operation options
   */
  public static void publishMdcOperationName(
      CanonicalOperation.Endpoint endpoint, Map<String, String> operationOptions) {
    CanonicalOperation operation = new CanonicalOperation(endpoint);
    for (String key : operationOptions.keySet()) {
      operation.setOption(key, operationOptions.get(key));
    }
    operation.publishOperationName();
  }

  /**
   * Parses the claim types to return in the search by parsing out the type tokens parameters.
   *
   * @param type a {@link TokenAndListParam} for the "type" field in a search
   * @return The {@link ClaimType}s to be searched, as computed from the specified "type" {@link
   *     TokenAndListParam} search param
   */
  public static Set<ClaimType> parseTypeParam(TokenAndListParam type) {
    if (type == null) {
      type =
          new TokenAndListParam()
              .addAnd(
                  new TokenOrListParam()
                      .add(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, null));
    }

    /*
     * This logic kinda' stinks, but HAPI forces us to handle some odd query
     * formulations, e.g. (in postfix notation):
     * "and(or(claimType==FOO, claimType==BAR), or(claimType==FOO))".
     */
    Set<ClaimType> claimTypes = new HashSet<>(Arrays.asList(ClaimType.values()));
    for (TokenOrListParam typeToken : type.getValuesAsQueryTokens()) {
      /*
       * Each OR entry is additive: we start with an empty set and add every (valid)
       * ClaimType that's encountered.
       */
      Set<ClaimType> claimTypesInner = new HashSet<>();
      for (TokenParam codingToken : typeToken.getValuesAsQueryTokens()) {
        if (codingToken.getModifier() != null) {
          throw new IllegalArgumentException();
        }

        /*
         * Per the FHIR spec (https://www.hl7.org/fhir/search.html), there are lots of
         * edge cases here: we could have null or wildcard or exact system, we can have
         * an exact or wildcard code. All of those need to be handled carefully -- see
         * the spec for details.
         */
        Optional<ClaimType> claimType =
            codingToken.getValue() != null
                ? ClaimType.parse(codingToken.getValue().toLowerCase())
                : Optional.empty();

        if (codingToken.getSystem() != null
            && codingToken.getSystem().equals(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE)
            && claimType.isEmpty()) {
          claimTypesInner.addAll(Arrays.asList(ClaimType.values()));
        } else if (codingToken.getSystem() == null
            || codingToken.getSystem().equals(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE)) {
          if (claimType.isPresent()) {
            claimTypesInner.add(claimType.get());
          }
        }
      }

      /*
       * All multiple AND parameters will do is reduce the number of possible matches.
       */
      claimTypes.retainAll(claimTypesInner);
    }

    return claimTypes;
  }

  /**
   * Determines if SAMHSA data should be filtered based on the client's identity and the
   * "excludeSAMHSA" request parameter.
   *
   * @param excludeSamhsa the value of the "excludeSAMHSA" parameter
   * @param requestDetails the {@link RequestDetails} containing the authentication info
   * @return whether to filter SAMHSA
   */
  public static boolean shouldFilterSamhsa(String excludeSamhsa, RequestDetails requestDetails) {

    if (Boolean.parseBoolean(excludeSamhsa)) {
      return true;
    }
    Object shouldFilterSamhsa = requestDetails.getAttribute(SHOULD_FILTER_SAMHSA);
    if (shouldFilterSamhsa == null) {
      throw new BadCodeMonkeyException(SHOULD_FILTER_SAMHSA + " attribute missing from request");
    }
    return (boolean) shouldFilterSamhsa;
  }

  /**
   * Builds taxonomy for future enrichment.
   *
   * @param npi NIP
   * @return the NPIData
   */
  public static Optional<NPIData> buildReplaceTaxonomy(Optional<String> npi) {
    if (npi.isPresent()) {
      return Optional.of(
          NPIData.builder()
              .taxonomyCode(String.format("replaceTaxonomyCode[%s]", npi.get()))
              .taxonomyDisplay(String.format("replaceTaxonomyDisplay[%s]", npi.get()))
              .build());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Builds the drug code for future enrichment.
   *
   * @param drugCode The drug code
   * @return placeholder for the drug code.
   */
  public static String buildReplaceDrugCode(Optional<String> drugCode) {
    if (drugCode.isPresent()) {
      String normalizedDrugCode = normalizeDrugCode(drugCode.get());
      return normalizedDrugCode != null
          ? String.format("replaceDrugCode[%s]", normalizedDrugCode)
          : null;
    } else {
      return null;
    }
  }

  /**
   * Normalizes the drug code to match the database values. *
   *
   * @param drugCode The drug code to normalize.
   * @return The normalized drug code.
   */
  public static String normalizeDrugCode(String drugCode) {

    return drugCode.length() >= 9
        ? drugCode.substring(0, 5) + "-" + drugCode.substring(5, 9)
        : null;
  }

  /**
   * Replaces organization for future enrichment.
   *
   * @param npi NPI
   * @return NPIData
   */
  public static Optional<NPIData> buildReplaceOrganization(Optional<String> npi) {
    if (npi.isPresent()) {
      return Optional.of(
          NPIData.builder()
              .providerOrganizationName(String.format("replaceOrganization[%s]", npi.get()))
              .build());
    } else {
      return Optional.empty();
    }
  }
}
