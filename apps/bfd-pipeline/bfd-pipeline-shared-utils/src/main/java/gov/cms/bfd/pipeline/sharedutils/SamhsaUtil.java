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
import gov.cms.bfd.pipeline.sharedutils.model.TagCode;
import gov.cms.bfd.pipeline.sharedutils.model.TagDetails;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;

/**
 * Class to create SAMHSA tags. This class will take a claim, iterate through the SAMHSA-related
 * fields, and determine if there are any SAMHSA codes. If any are found, a tag is created to mark
 * the claim as SAMHSA.
 */
public class SamhsaUtil {
  /**
   * Will contain the list of the methods for each entity to retrieve the fields with possible
   * SAMHSA codes.
   */
  public static final String CLAIM_SAMHSA_METHODS_YAML = "claim_samhsa_methods.yaml";

  /** Map of the SAMHSA code entries, with the SAMHSA code as the key. */
  private static Map<String, SamhsaEntry> samhsaMap = new HashMap<>();

  /** Instance of this class. Will be a singleton. */
  private static SamhsaUtil samhsaUtil;

  /** The file from which SAMHSA entries are pulled. Must be in the resources folder. */
  private static final String SAMHSA_LIST_RESOURCE = "security_labels.yml";

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
   * Gets an input stream from a file in the resources folder.
   *
   * @throws IOException Exception thrown when the resource cannot be loaded.
   */
  public static void createSamhsaMap() throws IOException {
    InputStream is = getFileInputStream(SAMHSA_LIST_RESOURCE);
    samhsaMap = initializeSamhsaMap(is);
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
   * @return true if a tag was persisted.
   */
  public <TClaim> boolean processCcwClaim(TClaim claim, EntityManager entityManager) {
    boolean persisted = false;
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
      return adapter.checkAndProcessClaim(entityManager);
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
          getSamhsaCode(Optional.ofNullable(diagCode.getIdrDiagCode())),
          "mcs_diagnosis_codes",
          "idr_diag_code",
          (int) diagCode.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    for (RdaMcsDetail detail : mcsClaim.getDetails()) {
      buildDetails(
          getSamhsaCode(Optional.ofNullable(detail.getIdrDtlPrimaryDiagCode())),
          "mcs_details",
          "idr_dtl_primary_diag_code",
          (int) detail.getIdrDtlNumber(),
          entries,
          serviceDate,
          throughDate);

      buildDetails(
          getSamhsaCode(Optional.ofNullable(detail.getIdrProcCode())),
          "mcs_details",
          "idr_proc_code",
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
        LocalDate startDate = LocalDate.parse(entry.get().getStartDate());
        LocalDate endDate =
            entry.get().getEndDate().equalsIgnoreCase("Active")
                ? LocalDate.MAX
                : LocalDate.parse(entry.get().getEndDate());

        // if the throughDate is not between the start and end date,
        // and the serviceDate is not between the start and end date,
        // then the claim falls outside the date range of the SAMHSA code.
        if (isDateOutsideOfRange(startDate, endDate, throughDate)
            && isDateOutsideOfRange(startDate, endDate, serviceDate)) {
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
          TagDetails.builder().table(table).column(column).clm_line_num(lineNum).type(type).build();
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
        getSamhsaCode(Optional.ofNullable(fissClaim.getAdmitDiagCode())),
        "fiss_claims",
        "admit_diag_code",
        null,
        entries,
        serviceDate,
        throughDate);
    for (RdaFissRevenueLine revenueLine : fissClaim.getRevenueLines()) {
      buildDetails(
          // Ideally, this column should never contain SAMHSA data, but it is
          // possible that SAMHSA data could end up here due to user error.
          getSamhsaCode(Optional.ofNullable(revenueLine.getApcHcpcsApc())),
          "fiss_revenue_lines",
          "apc_hcpcs_apc",
          (int) revenueLine.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
      buildDetails(
          getSamhsaCode(Optional.ofNullable(revenueLine.getHcpcCd())),
          "fiss_revenue_lines",
          "hcpcs_cd",
          (int) revenueLine.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    buildDetails(
        getSamhsaCode(Optional.ofNullable(fissClaim.getDrgCd())),
        "fiss_claims",
        "drg_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    buildDetails(
        getSamhsaCode(Optional.ofNullable(fissClaim.getPrincipleDiag())),
        "fiss_claims",
        "principle_diag",
        null,
        entries,
        serviceDate,
        throughDate);
    for (RdaFissDiagnosisCode diagCode : fissClaim.getDiagCodes()) {
      buildDetails(
          getSamhsaCode(Optional.ofNullable(diagCode.getDiagCd2())),
          "fiss_diagnosis_codes",
          "diag_cd2",
          (int) diagCode.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    for (RdaFissProcCode procCode : fissClaim.getProcCodes()) {
      buildDetails(
          getSamhsaCode(Optional.ofNullable(procCode.getProcCode())),
          "fiss_proc_codes",
          "proc_code",
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
   * @return If the code is SAMHSA, returns the SAMHSA entry. Otherwise, an empty optional.
   */
  public static Optional<SamhsaEntry> getSamhsaCode(Optional<String> code) {
    if (!code.isPresent()) {
      return Optional.empty();
    }
    if (samhsaMap.isEmpty()) {
      try {
        initializeSamhsaMap(getFileInputStream(SAMHSA_LIST_RESOURCE));
      } catch (IOException ioe) {
        throw new RuntimeException("Cannot retrieve list of SAMHSA codes.");
      }
    }
    if (samhsaMap.containsKey(code.get())) {
      return Optional.of(samhsaMap.get(code.get()));
    }
    return Optional.empty();
  }

  /**
   * Converts a YAML file into a Map of SAMHSA entries.
   *
   * @param stream The fileStream to convert.
   * @return a Map of SAMHSA entries.
   * @throws IOException IOException if the stream cannot be read.
   */
  private static Map<String, SamhsaEntry> initializeSamhsaMap(InputStream stream)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<SamhsaEntry> entries =
        mapper.readValue(
            stream, mapper.getTypeFactory().constructCollectionType(List.class, SamhsaEntry.class));
    return entries.stream().collect(Collectors.toMap(SamhsaEntry::getCode, entry -> entry));
  }
}
