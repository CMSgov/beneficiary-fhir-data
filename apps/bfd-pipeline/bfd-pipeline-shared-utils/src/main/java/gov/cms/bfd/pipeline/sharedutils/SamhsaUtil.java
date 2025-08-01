package gov.cms.bfd.pipeline.sharedutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import gov.cms.bfd.model.rda.samhsa.FissTag;
import gov.cms.bfd.model.rda.samhsa.McsTag;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.pipeline.sharedutils.adapters.SamhsaAdapterBase;
import gov.cms.bfd.pipeline.sharedutils.adapters.SamhsaCarrierAdapter;
import gov.cms.bfd.pipeline.sharedutils.adapters.SamhsaDmeAdapter;
import gov.cms.bfd.pipeline.sharedutils.adapters.SamhsaHHAAdapter;
import gov.cms.bfd.pipeline.sharedutils.adapters.SamhsaHospiceAdapter;
import gov.cms.bfd.pipeline.sharedutils.adapters.SamhsaInpatientAdapter;
import gov.cms.bfd.pipeline.sharedutils.adapters.SamhsaOutpatientAdapter;
import gov.cms.bfd.pipeline.sharedutils.adapters.SamhsaSnfAdapter;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaEntry;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import gov.cms.bfd.pipeline.sharedutils.model.TagDetails;
import gov.cms.bfd.sharedutils.TagCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.util.Strings;

/**
 * Class to create SAMHSA tags. This class will take a claim, iterate through the SAMHSA-related
 * fields, and determine if there are any SAMHSA codes. If any are found, a tag is created to mark
 * the claim as SAMHSA.
 */
public class SamhsaUtil {

  /** fiss_claims constant. */
  public static final String FISS_CLAIMS = "fiss_claims";

  /** admit_diag_code constant. */
  public static final String ADMIT_DIAG_CODE = "admit_diag_code";

  /** fiss_revenue_lines constant. */
  public static final String FISS_REVENUE_LINES = "fiss_revenue_lines";

  /** apc_hcpcs_apc constant. */
  public static final String APC_HCPCS_APC = "apc_hcpcs_apc";

  /** hcpcs_cd constant. */
  public static final String HCPCS_CD = "hcpcs_cd";

  /** drg_cd constant. */
  public static final String DRG_CD = "drg_cd";

  /** principle_diag constant. */
  public static final String PRINCIPLE_DIAG = "principle_diag";

  /** fiss_diagnosis_codes constant. */
  public static final String FISS_DIAGNOSIS_CODES = "fiss_diagnosis_codes";

  /** diag_cd2 constant. */
  public static final String DIAG_CD_2 = "diag_cd2";

  /** fiss_proc_codes constant. */
  public static final String FISS_PROC_CODES = "fiss_proc_codes";

  /** proc_code constant. */
  public static final String PROC_CODE = "proc_code";

  /** mcs_diagnosis_codes constant. */
  public static final String MCS_DIAGNOSIS_CODES = "mcs_diagnosis_codes";

  /** idr_diag_code constant. */
  public static final String IDR_DIAG_CODE = "idr_diag_code";

  /** mcs_details constant. */
  public static final String MCS_DETAILS = "mcs_details";

  /** idr_dtl_primary_diag_code constant. */
  public static final String IDR_DTL_PRIMARY_DIAG_CODE = "idr_dtl_primary_diag_code";

  /** idr_proc_code constant. */
  public static final String IDR_PROC_CODE = "idr_proc_code";

  /**
   * Diagnosis column portions. All diagnosis columns will contain one of these values in the name.
   */
  public static final String[] DIAG_COLUMN_PORTION = {"diag", "dgns", "rsn_visit"};

  /**
   * Procedure column portions. All procedure columns will contain one of these values in the name.
   */
  public static final String[] PROC_COLUMN_PORTION = {"prcdr", "proc_code"};

  /** HCPCS column portions. All HCPCS columns will contain one of these values in the name. */
  public static final String[] HCPCS_COLUMN_PORTION = {"hcpc", "hipps", "idr_proc_code"};

  /** DRG column portions. All DRG columns will contain one of these values in the name. */
  public static final String[] DRG_COLUMN_PORTION = {"drg"};

  /** Map of the SAMHSA code entries, with the entry's system as the key. */
  private static Map<String, List<SamhsaEntry>> samhsaMap = new HashMap<>();

  /** Instance of this class. Will be a singleton. */
  private static SamhsaUtil samhsaUtil;

  /** The file from which SAMHSA entries are pulled. Must be in the `resources` folder. */
  private static final String SAMHSA_LIST_RESOURCE = "security_labels.yml";

  private static final String[] PRCDR_SYSTEMS = {
    "http://www.cms.gov/Medicare/Coding/ICD10", "http://www.cms.gov/Medicare/Coding/ICD9"
  };
  private static final String[] DGNS_SYSTEMS = {
    "http://hl7.org/fhir/sid/icd-10-cm", "http://hl7.org/fhir/sid/icd-9-cm"
  };
  private static final String[] DRG_SYSTEMS = {
    "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software"
  };
  private static final String[] HCPCS_SYSTEMS = {
    "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets", "http://www.ama-assn.org/go/cpt"
  };

  private static final Map<String[], String[]> COLUMN_PORTION_TO_SYSTEMS = new HashMap<>();

  static {
    COLUMN_PORTION_TO_SYSTEMS.put(DIAG_COLUMN_PORTION, DGNS_SYSTEMS);
    COLUMN_PORTION_TO_SYSTEMS.put(PROC_COLUMN_PORTION, PRCDR_SYSTEMS);
    COLUMN_PORTION_TO_SYSTEMS.put(HCPCS_COLUMN_PORTION, HCPCS_SYSTEMS);
    COLUMN_PORTION_TO_SYSTEMS.put(DRG_COLUMN_PORTION, DRG_SYSTEMS);
  }

  /**
   * Creates a stream from a file.
   *
   * @param fileName The file to load.
   * @return an InputStream of the file.
   */
  private static InputStream getFileInputStream(String fileName) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
  }

  /**
   * Returns a singleton instance of this class.
   *
   * @return the instance of this class.
   */
  public static SamhsaUtil getSamhsaUtil() {
    if (samhsaUtil == null) {
      try {
        samhsaUtil = new SamhsaUtil();
      } catch (IOException e) {
        throw new RuntimeException("Unable to create SamhsaUtil.", e);
      }
    }
    return samhsaUtil;
  }

  /**
   * Private constructor. Initializes the map of SAMHSA entries.
   *
   * @throws IOException exception thrown when the resource cannot be loaded.
   */
  private SamhsaUtil() throws IOException {
    createSamhsaMap();
  }

  /**
   * Gets an input stream from a file in the `resources` folder.
   *
   * @throws IOException Exception thrown when the resource cannot be loaded.
   */
  public static void createSamhsaMap() throws IOException {
    InputStream is = getFileInputStream(SAMHSA_LIST_RESOURCE);
    samhsaMap = initializeSamhsaMap(is);
  }

  /**
   * Gets the date ranges for a line item table from its parent table.
   *
   * @param claimId The claim Id.
   * @param tableEntry The table entry.
   * @param entityManager The entity manager.
   * @return the date ranges.
   */
  private LocalDate[] getClaimDates(
      Object claimId, TableEntry tableEntry, EntityManager entityManager) {
    Map<String, String> params =
        Map.of("claimTable", tableEntry.getParentTable(), "claimField", tableEntry.getClaimField());
    StringSubstitutor strSub = new StringSubstitutor(params);
    String queryStr = strSub.replace(tableEntry.getDatesQuery());
    Query query = entityManager.createNativeQuery(queryStr);
    query.setParameter("claimId", claimId);
    Object[] result = (Object[]) query.getSingleResult();
    LocalDate fromDate = result[0] != null ? ((Date) result[0]).toLocalDate() : null;
    LocalDate toDate = result[1] != null ? ((Date) result[1]).toLocalDate() : null;
    return new LocalDate[] {fromDate, toDate};
  }

  /**
   * Process a list of codes. Does not use Entities.
   *
   * @param claim The results list.
   * @param columnIndexMap Map of columns to their index in the claim[] object array.
   * @param columnSystems A map of columns to their systems.
   * @param tableEntry The tableEntry object for this table.
   * @param dates If present, contains the active dates for this claim.
   * @param datesMap Contains previously fetched claim dates for this claim id. This is useful if a
   *     claim has more than one line item.
   * @param nonCodeFields Fields that are not Samhsa codes. This will be skipped during iteration.
   * @param entityManager The entity manager.
   * @return true if a SAMHSA tag should be created.
   */
  public boolean processCodeList(
      Object[] claim,
      Map<String, Integer> columnIndexMap,
      Map<String, String[]> columnSystems,
      TableEntry tableEntry,
      Optional<LocalDate[]> dates,
      Map<String, LocalDate[]> datesMap,
      List<String> nonCodeFields,
      EntityManager entityManager) {

    for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
      if (nonCodeFields.contains(entry.getKey())) {
        continue;
      }
      String code = (String) claim[entry.getValue()];
      // having a `continue` here instead of a nested block reduces cognitive complexity.
      if (code == null) {
        continue;
      }

      Optional<SamhsaEntry> samhsaEntry =
          getSamhsaCode(
              Optional.of(code),
              Optional.of(entry.getKey()),
              Optional.of(columnSystems.get(entry.getKey())));
      if (samhsaEntry.isPresent()) {
        LocalDate[] datesObject =
            getDatesObjectsForClaim(
                tableEntry,
                claim[columnIndexMap.get(tableEntry.getClaimField())],
                dates,
                datesMap,
                entityManager);
        if (isInvalidClaimDate(datesObject, samhsaEntry.get())) {
          continue;
        }
        // This is a valid SAMHSA code, that belongs to the correct system, and the date is in
        // range.
        // Since we're not creating a details object for this claim, we can stop here.
        return true;
      }
    }
    return false;
  }

  private LocalDate[] getDatesObjectsForClaim(
      TableEntry tableEntry,
      Object claimId,
      Optional<LocalDate[]> dates,
      Map<String, LocalDate[]> datesMap,
      EntityManager entityManager) {
    LocalDate[] datesObject;
    if (dates.isPresent()) {
      datesObject = dates.get();
    } else {
      datesObject = getClaimDates(claimId, tableEntry, entityManager);
      // Put the dates in a map, in case any other line items with the same id have samhsa
      // codes.
      datesMap.put(claimId.toString(), datesObject);
    }
    return datesObject;
  }

  private static boolean isInvalidClaimDate(LocalDate[] datesObject, SamhsaEntry entry) {
    LocalDate coverageStartDate =
        datesObject[0] == null ? LocalDate.parse("1970-01-01") : (datesObject[0]);
    LocalDate coverageEndDate = datesObject[1] == null ? LocalDate.now() : (datesObject[1]);
    CodeDateRange result = getGetStartEndDateForCode(entry);

    // if the throughDate is not between the start and end date,
    // and the serviceDate is not between the start and end date,
    // then the claim falls outside the date range of the SAMHSA code.
    return isDateOutsideOfRange(result.startDate(), result.endDate(), coverageEndDate)
        && isDateOutsideOfRange(result.startDate(), result.endDate(), coverageStartDate);
  }

  private static CodeDateRange getGetStartEndDateForCode(SamhsaEntry entry) {
    LocalDate startDate = LocalDate.parse(entry.getStartDate());
    LocalDate endDate =
        entry.getEndDate().equalsIgnoreCase("Active")
            ? LocalDate.MAX
            : LocalDate.parse(entry.getEndDate());
    return new CodeDateRange(startDate, endDate);
  }

  private record CodeDateRange(LocalDate startDate, LocalDate endDate) {}

  private static SamhsaEntry getEntryForCode(String columnName, String code, String[] systems) {
    String[] columnSystems;
    if (systems != null) {
      columnSystems = systems;
    } else {
      columnSystems = getSystemsForColumn(columnName);
    }
    // Get the entry that this code belongs to by filtering.
    // mapMulti is a bit less cpu intensive than flatMap.
    return Arrays.stream(columnSystems)
        .<SamhsaEntry>mapMulti((s, consumer) -> samhsaMap.get(s).forEach(consumer))
        .filter(e -> e.getCode().equals(code))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets a list of systems that this column may belong to.
   *
   * @param columnName The column name.
   * @return A list of systems that the column may belong to.
   */
  public static String[] getSystemsForColumn(String columnName) {
    // Use HashSet to avoid duplicate systems in the result and order doesn't matter.
    Set<String> systems = new HashSet<>();

    // Loop over all defined column name patterns
    // adding their associated code systems when matching (e.g., "diag", "proc")
    for (Map.Entry<String[], String[]> entry : COLUMN_PORTION_TO_SYSTEMS.entrySet()) {
      if (columnMatchesCodeType(columnName, entry.getKey())) {
        Collections.addAll(systems, entry.getValue());
      }
    }

    // Convert the set to an array and return.
    return systems.toArray(new String[0]);
  }

  /**
   * Checks if a column contains the String in a name portion. This allows us to generalize the
   * column names to diagnosis, procedure, DRG, or HCPCS without needing to know the full name of
   * all the columns.
   *
   * @param columnName The column to check.
   * @param columnPortion The column name portion.
   * @return True if the column contains the name portion.
   */
  private static boolean columnMatchesCodeType(String columnName, String[] columnPortion) {
    return Arrays.stream(columnPortion).anyMatch(columnName::contains);
  }

  /**
   * Process am RDA claim to check for SAMHSA codes. This will be the external entry point for other
   * parts of the application.
   *
   * @param claim The claim to process.
   * @param entityManager the EntityManager used to persist the tag.
   * @param <TClaim> Generic type of the claim.
   */
  public <TClaim> void processRdaClaim(TClaim claim, EntityManager entityManager) {
    switch (claim) {
      case RdaFissClaim fissClaim -> {
        Optional<List<FissTag>> tags = Optional.of(checkAndProcessFissClaim(fissClaim));
        persistTags(tags, entityManager);
      }
      case RdaMcsClaim mcsClaim -> {
        Optional<List<McsTag>> tags = Optional.of(checkAndProcessMcsClaim(mcsClaim));
        persistTags(tags, entityManager);
      }
      default -> throw new RuntimeException("Unknown claim type.");
    }
  }

  /**
   * Persists the tags to the database.
   *
   * @param tags List of tags to persist.
   * @param entityManager the EntityManager.
   * @param <TTag> Generic type of the tags.
   * @return True if tags were merged.
   */
  public static <TTag> boolean persistTags(Optional<List<TTag>> tags, EntityManager entityManager) {
    boolean persisted = false;
    if (tags.isPresent()) {
      for (TTag tag : tags.get()) {
        entityManager.merge(tag);
        persisted = true;
      }
    }
    return persisted;
  }

  /**
   * Process a CCW claim to check for SAMHSA codes. This will be the external entry point for other
   * parts of the application.
   *
   * @param claim The claim to process.
   * @param entityManager the EntityManager used to persist the tag.
   * @param <TClaim> Generic type of the claim.
   */
  public <TClaim> void processCcwClaim(TClaim claim, EntityManager entityManager) {
    try {
      SamhsaAdapterBase adapter =
          switch (claim) {
            case CarrierClaim carrierClaim -> new SamhsaCarrierAdapter(carrierClaim);
            case HHAClaim hhaClaim -> new SamhsaHHAAdapter(hhaClaim);
            case DMEClaim dmeClaim -> new SamhsaDmeAdapter(dmeClaim);
            case HospiceClaim hospiceClaim -> new SamhsaHospiceAdapter(hospiceClaim);
            case OutpatientClaim outpatientClaim -> new SamhsaOutpatientAdapter(outpatientClaim);
            case InpatientClaim inpatientClaim -> new SamhsaInpatientAdapter(inpatientClaim);
            case SNFClaim snfClaim -> new SamhsaSnfAdapter(snfClaim);
            default -> throw new RuntimeException("Error: unknown claim type.");
          };
      adapter.checkAndProcessClaim(entityManager);
    } catch (Exception e) {
      throw new RuntimeException("There was an error creating SAMHSA tags.", e);
    }
  }

  /**
   * Checks for SAMHSA codes in an MCS claim and constructs the tags.
   *
   * @param mcsClaim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<McsTag> checkAndProcessMcsClaim(RdaMcsClaim mcsClaim) {
    Optional<List<TagDetails>> entries = getPossibleMcsSamhsaFields(mcsClaim);
    if (entries.isPresent()) {
      List<McsTag> mcsTags = new ArrayList<>();
      mcsTags.add(
          McsTag.builder()
              .claim(mcsClaim.getIdrClmHdIcn())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      mcsTags.add(
          McsTag.builder()
              .claim(mcsClaim.getIdrClmHdIcn())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return mcsTags;
    }
    return Collections.emptyList();
  }

  /**
   * Checks for SAMHSA codes in a FISS Claim and constructs the tags.
   *
   * @param fissClaim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<FissTag> checkAndProcessFissClaim(RdaFissClaim fissClaim) {
    Optional<List<TagDetails>> entries = getPossibleFissSamhsaFields(fissClaim);
    if (entries.isPresent()) {
      List<FissTag> fissTags = new ArrayList<>();
      fissTags.add(
          FissTag.builder()
              .claim(fissClaim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      fissTags.add(
          FissTag.builder()
              .claim(fissClaim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return fissTags;
    }
    return Collections.emptyList();
  }

  /**
   * Constructs a list of TagDetail objects for an MCS claim.
   *
   * @param mcsClaim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleMcsSamhsaFields(RdaMcsClaim mcsClaim) {
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        mcsClaim.getIdrHdrFromDateOfSvc() == null
            ? LocalDate.parse("1970-01-01")
            : mcsClaim.getIdrHdrFromDateOfSvc();
    LocalDate throughDate =
        mcsClaim.getIdrHdrToDateOfSvc() == null ? LocalDate.now() : mcsClaim.getIdrHdrToDateOfSvc();
    for (RdaMcsDiagnosisCode diagCode : mcsClaim.getDiagCodes()) {
      buildDetails(
          getSamhsaCode(Optional.ofNullable(diagCode.getIdrDiagCode()), Optional.of(IDR_DIAG_CODE)),
          MCS_DIAGNOSIS_CODES,
          IDR_DIAG_CODE,
          (int) diagCode.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    for (RdaMcsDetail detail : mcsClaim.getDetails()) {
      buildDetails(
          getSamhsaCode(
              Optional.ofNullable(detail.getIdrDtlPrimaryDiagCode()),
              Optional.of(IDR_DTL_PRIMARY_DIAG_CODE)),
          MCS_DETAILS,
          IDR_DTL_PRIMARY_DIAG_CODE,
          (int) detail.getIdrDtlNumber(),
          entries,
          serviceDate,
          throughDate);

      buildDetails(
          getSamhsaCode(Optional.ofNullable(detail.getIdrProcCode()), Optional.of(IDR_PROC_CODE)),
          MCS_DETAILS,
          IDR_PROC_CODE,
          (int) detail.getIdrDtlNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
  }

  /**
   * Tests if a given date is outside the range of two other dates.
   *
   * @param earlyDate early date to test against
   * @param laterDate later date to test against
   * @param dateToTest the date that's being tested
   * @return true if dateToTest is outside the range of the two other dates.
   */
  public static boolean isDateOutsideOfRange(
      LocalDate earlyDate, LocalDate laterDate, LocalDate dateToTest) {
    return dateToTest.isBefore(earlyDate) || dateToTest.isAfter(laterDate);
  }

  /**
   * Builds a TagDetails object for an RDA claim, and adds it to a list of TagDetails.
   *
   * @param entry The SamhsaEntry that holds information about the SAMHSA code.
   * @param table The table that the code belongs to
   * @param column The column that the cod belongs to
   * @param lineNum The line number that the code belongs to
   * @param detailsList The TagDetails list to add to
   * @param serviceDate The first date of service for the claim
   * @param throughDate The service through date
   */
  private void buildDetails(
      Optional<SamhsaEntry> entry,
      String table,
      String column,
      Integer lineNum,
      List<TagDetails> detailsList,
      LocalDate serviceDate,
      LocalDate throughDate) {
    if (entry.isPresent()) {
      try {
        CodeDateRange result = getGetStartEndDateForCode(entry.get());
        // if the throughDate is not between the start and end date,
        // and the serviceDate is not between the start and end date,
        // then the claim falls outside the date range of the SAMHSA code.
        if (isDateOutsideOfRange(result.startDate, result.endDate, throughDate)
            && isDateOutsideOfRange(result.startDate, result.endDate, serviceDate)) {
          return;
        }
      } catch (DateTimeParseException ignore) {
        // Parsing the date from the SamhsaEntry failed, so the tag should be created by default.
      }
      // Use the last part of the system path as the type
      String type =
          Arrays.stream(entry.get().getSystem().split("/"))
              .reduce((first, second) -> second)
              .orElse(Strings.EMPTY);
      TagDetails detail =
          TagDetails.builder().table(table).column(column).clmLineNum(lineNum).type(type).build();
      detailsList.add(detail);
    }
  }

  /**
   * Constructs a list of TagDetail objects for a Fiss claim.
   *
   * @param fissClaim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleFissSamhsaFields(RdaFissClaim fissClaim) {
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        fissClaim.getStmtCovFromDate() == null
            ? LocalDate.parse("1970-01-01")
            : fissClaim.getStmtCovFromDate();
    LocalDate throughDate =
        fissClaim.getStmtCovToDate() == null ? LocalDate.now() : fissClaim.getStmtCovToDate();

    buildDetails(
        getSamhsaCode(
            Optional.ofNullable(fissClaim.getAdmitDiagCode()), Optional.of(ADMIT_DIAG_CODE)),
        FISS_CLAIMS,
        ADMIT_DIAG_CODE,
        null,
        entries,
        serviceDate,
        throughDate);
    for (RdaFissRevenueLine revenueLine : fissClaim.getRevenueLines()) {
      buildDetails(
          // Ideally, this column should never contain SAMHSA data, but it is
          // possible that SAMHSA data could end up here due to user error.
          getSamhsaCode(
              Optional.ofNullable(revenueLine.getApcHcpcsApc()), Optional.of(APC_HCPCS_APC)),
          FISS_REVENUE_LINES,
          APC_HCPCS_APC,
          (int) revenueLine.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
      buildDetails(
          getSamhsaCode(Optional.ofNullable(revenueLine.getHcpcCd()), Optional.of(HCPCS_CD)),
          FISS_REVENUE_LINES,
          HCPCS_CD,
          (int) revenueLine.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    buildDetails(
        getSamhsaCode(Optional.ofNullable(fissClaim.getDrgCd()), Optional.of(DRG_CD)),
        FISS_CLAIMS,
        DRG_CD,
        null,
        entries,
        serviceDate,
        throughDate);
    buildDetails(
        getSamhsaCode(
            Optional.ofNullable(fissClaim.getPrincipleDiag()), Optional.of(PRINCIPLE_DIAG)),
        FISS_CLAIMS,
        PRINCIPLE_DIAG,
        null,
        entries,
        serviceDate,
        throughDate);
    for (RdaFissDiagnosisCode diagCode : fissClaim.getDiagCodes()) {
      buildDetails(
          getSamhsaCode(Optional.ofNullable(diagCode.getDiagCd2()), Optional.of(DIAG_CD_2)),
          FISS_DIAGNOSIS_CODES,
          DIAG_CD_2,
          (int) diagCode.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    for (RdaFissProcCode procCode : fissClaim.getProcCodes()) {
      buildDetails(
          getSamhsaCode(Optional.ofNullable(procCode.getProcCode()), Optional.of(PROC_CODE)),
          FISS_PROC_CODES,
          PROC_CODE,
          (int) procCode.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
  }

  /**
   * Check if a given code is a SAMHSA code.
   *
   * @param code the code to check.
   * @param columnName The column of the code.
   * @return If the code is SAMHSA, returns the SAMHSA entry. Otherwise, an empty optional.
   */
  public static Optional<SamhsaEntry> getSamhsaCode(
      Optional<String> code, Optional<String> columnName) {
    return getSamhsaCode(code, columnName, Optional.empty());
  }

  /**
   * Check if a given code is a SAMHSA code.
   *
   * @param code the code to check.
   * @param columnName The column of the code.
   * @param systems The possible systems for this column.
   * @return If the code is SAMHSA, returns the SAMHSA entry. Otherwise, an empty optional.
   */
  public static Optional<SamhsaEntry> getSamhsaCode(
      Optional<String> code, Optional<String> columnName, Optional<String[]> systems) {
    if (code.isEmpty() || columnName.isEmpty()) {
      return Optional.empty();
    }
    if (samhsaMap.isEmpty()) {
      try {
        initializeSamhsaMap(getFileInputStream(SAMHSA_LIST_RESOURCE));
      } catch (IOException ioe) {
        throw new RuntimeException("Cannot retrieve list of SAMHSA codes.");
      }
    }
    String normalizedCode = normalizeCode(code.get());

    SamhsaEntry entry = getEntryForCode(columnName.get(), normalizedCode, systems.orElse(null));
    return Optional.ofNullable(entry);
  }

  private static String normalizeCode(String code) {
    code = code.trim();
    code = code.replaceFirst("\\.", "");
    code = code.toUpperCase();
    return code;
  }

  /**
   * Converts a YAML file into a Map of SAMHSA entries.
   *
   * @param stream The fileStream to convert.
   * @return a Map of SAMHSA entries.
   * @throws IOException IOException if the stream cannot be read.
   */
  private static Map<String, List<SamhsaEntry>> initializeSamhsaMap(InputStream stream)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<SamhsaEntry> entries =
        mapper.readValue(
            stream, mapper.getTypeFactory().constructCollectionType(List.class, SamhsaEntry.class));
    entries.forEach(entry -> entry.setCode(normalizeCode(entry.getCode())));

    // Add all the systems together into a list
    List<String> systems =
        Stream.of(DGNS_SYSTEMS, PRCDR_SYSTEMS, DRG_SYSTEMS, HCPCS_SYSTEMS)
            .flatMap(Stream::of)
            .toList();

    Map<String, List<SamhsaEntry>> entryMap = new HashMap<>();
    // iterate over each system
    systems.forEach(
        sys ->
            entryMap.put(
                sys,
                // Filter the entries by system, and add the resultant list to the map under this
                // system.
                entries.stream().filter(entry -> entry.getSystem().equals(sys)).toList()));
    return entryMap;
  }
}
