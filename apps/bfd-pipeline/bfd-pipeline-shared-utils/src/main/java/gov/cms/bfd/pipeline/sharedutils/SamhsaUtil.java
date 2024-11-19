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
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimLine;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.model.rif.samhsa.CarrierTag;
import gov.cms.bfd.model.rif.samhsa.DmeTag;
import gov.cms.bfd.model.rif.samhsa.HhaTag;
import gov.cms.bfd.model.rif.samhsa.HospiceTag;
import gov.cms.bfd.model.rif.samhsa.InpatientTag;
import gov.cms.bfd.model.rif.samhsa.OutpatientTag;
import gov.cms.bfd.model.rif.samhsa.SnfTag;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaEntry;
import gov.cms.bfd.pipeline.sharedutils.model.TagCode;
import gov.cms.bfd.pipeline.sharedutils.model.TagDetails;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
  /** Map of the SAMHSA code entries, with the SAMHSA code as the key. */
  private Map<String, SamhsaEntry> samhsaMap = new HashMap<>();

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
   * Gets an InputStream and passes it along to the method to initialize the map of SAMHSA entries.
   *
   * @throws IOException Exception thrown when the resource cannot be loaded.
   */
  public void createSamhsaMap() throws IOException {
    InputStream is = getFileInputStream(SAMHSA_LIST_RESOURCE);
    samhsaMap = initializeSamhsaMap(is);
  }

  /**
   * Process a claim to check for SAMHSA codes. This will be the external entry point for other
   * parts of the application.
   *
   * @param claim The claim to process.
   * @param entityManager the EntityManager used to persist the tag.
   * @param <TClaim> Generic type of the claim.
   */
  public <TClaim> void processClaim(TClaim claim, EntityManager entityManager) {
    switch (claim) {
      case RdaFissClaim fissClaim -> {
        Optional<List<FissTag>> tags = Optional.of(checkAndProcessFissClaim(fissClaim));
        persistTags(tags, entityManager);
      }
      case RdaMcsClaim mcsClaim -> {
        Optional<List<McsTag>> tags = Optional.of(checkAndProcessMcsClaim(mcsClaim));
        persistTags(tags, entityManager);
      }
      case CarrierClaim carrierClaim -> {
        Optional<List<CarrierTag>> tags = Optional.of(checkAndProcessCarrierClaim(carrierClaim));
        persistTags(tags, entityManager);
      }
      case HHAClaim hhaClaim -> {
        Optional<List<HhaTag>> tags = Optional.of(checkAndProcessHhaClaim(hhaClaim));
        persistTags(tags, entityManager);
      }
      case DMEClaim dmeClaim -> {
        Optional<List<DmeTag>> tags = Optional.of(checkAndProcessDmeClaim(dmeClaim));
        persistTags(tags, entityManager);
      }
      case HospiceClaim hospiceClaim -> {
        Optional<List<HospiceTag>> tags = Optional.of(checkAndProcessHospiceClaim(hospiceClaim));
        persistTags(tags, entityManager);
      }
      case OutpatientClaim outpatientClaim -> {
        Optional<List<OutpatientTag>> tags =
            Optional.of(checkAndProcessOutpatientClaim(outpatientClaim));
        persistTags(tags, entityManager);
      }
      case InpatientClaim inpatientClaim -> {
        Optional<List<InpatientTag>> tags =
            Optional.of(checkAndProcessInpatientClaim(inpatientClaim));
        persistTags(tags, entityManager);
      }
      case SNFClaim snfClaim -> {
        Optional<List<SnfTag>> tags = Optional.of(checkAndProcessSnfClaim(snfClaim));
        persistTags(tags, entityManager);
      }
      default -> throw new RuntimeException("Error: unknown claim type.");
    }
  }

  /**
   * Persists the tags to the database.
   *
   * @param tags List of tags to persist.
   * @param entityManager the EntityManager.
   * @param <TTag> Generic type of the tags.
   */
  private <TTag> void persistTags(Optional<List<TTag>> tags, EntityManager entityManager) {
    if (tags.isPresent()) {
      for (TTag tag : tags.get()) {
        entityManager.merge(tag);
      }
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
   * Checks for SAMHSA codes in an HHA claim and constructs the tags.
   *
   * @param claim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<HhaTag> checkAndProcessHhaClaim(HHAClaim claim) {
    Optional<List<TagDetails>> entries = getPossibleHhaFields(claim);
    if (entries.isPresent()) {
      List<HhaTag> hhaTags = new ArrayList<>();
      hhaTags.add(
          HhaTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      hhaTags.add(
          HhaTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return hhaTags;
    }
    return Collections.emptyList();
  }

  /**
   * Checks for SAMHSA codes in an Hospice claim and constructs the tags.
   *
   * @param claim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<HospiceTag> checkAndProcessHospiceClaim(HospiceClaim claim) {
    Optional<List<TagDetails>> entries = getPossibleHospiceFields(claim);
    if (entries.isPresent()) {
      List<HospiceTag> hospiceTags = new ArrayList<>();
      hospiceTags.add(
          HospiceTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      hospiceTags.add(
          HospiceTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return hospiceTags;
    }
    return Collections.emptyList();
  }

  /**
   * Checks for SAMHSA codes in an DME claim and constructs the tags.
   *
   * @param claim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<DmeTag> checkAndProcessDmeClaim(DMEClaim claim) {
    Optional<List<TagDetails>> entries = getPossibleDmeFields(claim);
    if (entries.isPresent()) {
      List<DmeTag> dmeTags = new ArrayList<>();
      dmeTags.add(
          DmeTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      dmeTags.add(
          DmeTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return dmeTags;
    }
    return Collections.emptyList();
  }

  /**
   * Checks for SAMHSA codes in an Outpatient claim and constructs the tags.
   *
   * @param claim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<OutpatientTag> checkAndProcessOutpatientClaim(OutpatientClaim claim) {
    Optional<List<TagDetails>> entries = getPossibleOutpatientFields(claim);
    if (entries.isPresent()) {
      List<OutpatientTag> outpatientTags = new ArrayList<>();
      outpatientTags.add(
          OutpatientTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      outpatientTags.add(
          OutpatientTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return outpatientTags;
    }
    return Collections.emptyList();
  }

  /**
   * Checks for SAMHSA codes in an Inpatient claim and constructs the tags.
   *
   * @param claim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<InpatientTag> checkAndProcessInpatientClaim(InpatientClaim claim) {
    Optional<List<TagDetails>> entries = getPossibleInpatientFields(claim);
    if (entries.isPresent()) {
      List<InpatientTag> inpatientTags = new ArrayList<>();
      inpatientTags.add(
          InpatientTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      inpatientTags.add(
          InpatientTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return inpatientTags;
    }
    return Collections.emptyList();
  }

  /**
   * Checks for SAMHSA codes in an SNF claim and constructs the tags.
   *
   * @param claim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<SnfTag> checkAndProcessSnfClaim(SNFClaim claim) {
    Optional<List<TagDetails>> entries = getPossibleSnfFields(claim);
    if (entries.isPresent()) {
      List<SnfTag> snfTags = new ArrayList<>();
      snfTags.add(
          SnfTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      snfTags.add(
          SnfTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return snfTags;
    }
    return Collections.emptyList();
  }

  /**
   * Constructs a list of TagDetail objects for a SNF claim.
   *
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleSnfFields(SNFClaim claim) {
    Class<?> claimClass = SNFClaim.class;
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getDateFrom() == null ? LocalDate.parse("1970-01-01") : claim.getDateFrom();
    LocalDate throughDate =
        claim.getDateThrough() == null ? LocalDate.now() : claim.getDateThrough();
    buildDetails(
        getSamhsaCode(claim.getDiagnosisRelatedGroupCd()),
        "snf_claims",
        "clm_drg_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    buildDetails(
        getSamhsaCode(claim.getDiagnosisPrincipalCode()),
        "snf_claims",
        "prncpal_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    buildDetails(
        getSamhsaCode(claim.getDiagnosisAdmittingCode()),
        "snf_claims",
        "admtg_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    buildDetails(
        getSamhsaCode(claim.getDiagnosisExternalFirstCode()),
        "snf_claims",
        "fst_dgns_e_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    try {
      for (int i = 1; i <= 25; i++) {
        Method method = claimClass.getMethod("getDiagnosis" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "snf_claims",
            "icd_dgns_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    try {
      for (int i = 1; i <= 12; i++) {
        Method method = claimClass.getMethod("getDiagnosisExternal" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "snf_claims",
            "icd_dgns_e_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    try {
      for (int i = 1; i <= 25; i++) {
        Method method = claimClass.getMethod("getProcedure" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "snf_claims",
            "icd_prcdr_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    for (SNFClaimLine line : claim.getLines()) {
      buildDetails(
          getSamhsaCode(line.getHcpcsCode()),
          "snf_claim_lines",
          "hcpcs_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
  }

  /**
   * Constructs a list of TagDetail objects for a Inpatient claim.
   *
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleInpatientFields(InpatientClaim claim) {
    Class<?> claimClass = InpatientClaim.class;
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getDateFrom() == null ? LocalDate.parse("1970-01-01") : claim.getDateFrom();
    LocalDate throughDate =
        claim.getDateThrough() == null ? LocalDate.now() : claim.getDateThrough();
    buildDetails(
        getSamhsaCode(claim.getDiagnosisRelatedGroupCd()),
        "inpatient_claims",
        "clm_drg_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    buildDetails(
        getSamhsaCode(claim.getDiagnosisPrincipalCode()),
        "inpatient_claims",
        "prncpal_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    buildDetails(
        getSamhsaCode(claim.getDiagnosisAdmittingCode()),
        "inpatient_claims",
        "admtg_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    buildDetails(
        getSamhsaCode(claim.getDiagnosisExternalFirstCode()),
        "inpatient_claims",
        "fst_dgns_e_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    try {
      for (int i = 1; i <= 25; i++) {
        Method method = claimClass.getMethod("getDiagnosis" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "inpatient_claims",
            "icd_dgns_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    try {
      for (int i = 1; i <= 12; i++) {
        Method method = claimClass.getMethod("getDiagnosisExternal" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "inpatient_claims",
            "icd_dgns_e_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    try {
      for (int i = 1; i <= 25; i++) {
        Method method = claimClass.getMethod("getProcedure" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "outpatient_claims",
            "icd_prcdr_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    for (InpatientClaimLine line : claim.getLines()) {
      buildDetails(
          getSamhsaCode(line.getHcpcsCode()),
          "inpatient_claim_lines",
          "hcpcs_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
  }

  /**
   * Constructs a list of TagDetail objects for a Outpatient claim.
   *
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleOutpatientFields(OutpatientClaim claim) {
    Class<?> claimClass = OutpatientClaim.class;
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getDateFrom() == null ? LocalDate.parse("1970-01-01") : claim.getDateFrom();
    LocalDate throughDate =
        claim.getDateThrough() == null ? LocalDate.now() : claim.getDateThrough();
    buildDetails(
        getSamhsaCode(claim.getDiagnosisPrincipalCode()),
        "outpatient_claims",
        "prncpal_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    try {
      for (int i = 1; i <= 25; i++) {
        Method method = claimClass.getMethod("getDiagnosis" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "outpatient_claims",
            "icd_dgns_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    try {
      for (int i = 1; i <= 12; i++) {
        Method method = claimClass.getMethod("getDiagnosisExternal" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "outpatient_claims",
            "icd_dgns_e_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    try {
      for (int i = 1; i <= 25; i++) {
        Method method = claimClass.getMethod("getProcedure" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "outpatient_claims",
            "icd_prcdr_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    for (OutpatientClaimLine line : claim.getLines()) {
      buildDetails(
          getSamhsaCode(line.getHcpcsCode()),
          "outpatient_claim_lines",
          "hcpcs_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
      buildDetails(
          getSamhsaCode(line.getApcOrHippsCode()),
          "outpatient_claim_lines",
          "rev_cntr_apc_hipps_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
  }

  /**
   * Constructs a list of TagDetail objects for a DME claim.
   *
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleDmeFields(DMEClaim claim) {
    Class<?> claimClass = DMEClaim.class;
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getDateFrom() == null ? LocalDate.parse("1970-01-01") : claim.getDateFrom();
    LocalDate throughDate =
        claim.getDateThrough() == null ? LocalDate.now() : claim.getDateThrough();
    buildDetails(
        getSamhsaCode(claim.getDiagnosisPrincipalCode()),
        "dme_claims",
        "prncpal_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    try {
      for (int i = 1; i <= 12; i++) {
        Method method = claimClass.getMethod("getDiagnosis" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "dme_claims",
            "icd_dgns_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    for (DMEClaimLine line : claim.getLines()) {
      buildDetails(
          getSamhsaCode(line.getHcpcsCode()),
          "dme_claim_lines",
          "hcpcs_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
      buildDetails(
          getSamhsaCode(line.getDiagnosisCode()),
          "dme_claim_lines",
          "line_icd_dgns_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
  }

  /**
   * Constructs a list of TagDetail objects for a Hospice claim.
   *
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleHospiceFields(HospiceClaim claim) {
    Class<?> claimClass = HospiceClaim.class;
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getDateFrom() == null ? LocalDate.parse("1970-01-01") : claim.getDateFrom();
    LocalDate throughDate =
        claim.getDateThrough() == null ? LocalDate.now() : claim.getDateThrough();
    buildDetails(
        getSamhsaCode(claim.getDiagnosisPrincipalCode()),
        "hospice_claims",
        "prncpal_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    try {
      for (int i = 1; i <= 25; i++) {
        Method method = claimClass.getMethod("getDiagnosis" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "hha_claims",
            "icd_dgns_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    try {
      for (int i = 1; i <= 12; i++) {
        Method method = claimClass.getMethod("getDiagnosisExternal" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "hospice_claims",
            "icd_dgns_e_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    buildDetails(
        getSamhsaCode(claim.getDiagnosisExternalFirstCode()),
        "hospice_claims",
        "fst_dgns_e_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    for (HospiceClaimLine line : claim.getLines()) {
      buildDetails(
          getSamhsaCode(line.getHcpcsCode()),
          "hospice_claim_lines",
          "hcpcs_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
  }

  /**
   * Constructs a list of TagDetail objects for an HHA claim.
   *
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleHhaFields(HHAClaim claim) {
    Class<?> claimClass = HHAClaim.class;
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getDateFrom() == null ? LocalDate.parse("1970-01-01") : claim.getDateFrom();
    LocalDate throughDate =
        claim.getDateThrough() == null ? LocalDate.now() : claim.getDateThrough();
    buildDetails(
        getSamhsaCode(claim.getDiagnosisPrincipalCode()),
        "hha_claims",
        "prncpal_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    try {
      for (int i = 1; i <= 25; i++) {
        Method method = claimClass.getMethod("getDiagnosis" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "hha_claims",
            "icd_dgns_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    try {
      for (int i = 1; i <= 12; i++) {
        Method method = claimClass.getMethod("getDiagnosisExternal" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "hha_claims",
            "icd_dgns_e_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    buildDetails(
        getSamhsaCode(claim.getDiagnosisExternalFirstCode()),
        "hha_claims",
        "fst_dgns_e_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    for (HHAClaimLine line : claim.getLines()) {
      buildDetails(
          getSamhsaCode(line.getHcpcsCode()),
          "hha_claim_lines",
          "hcpcs_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
      buildDetails(
          getSamhsaCode(line.getApcOrHippsCode()),
          "hha_claim_lines",
          "apcOrHippsCode",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
  }

  /**
   * Checks for SAMHSA codes in a carrier claim and constructs the tags.
   *
   * @param claim The claim to check.
   * @return A list of tag entities to persist.
   */
  private List<CarrierTag> checkAndProcessCarrierClaim(CarrierClaim claim) {
    Optional<List<TagDetails>> entries = getPossibleCarrierFields(claim);
    if (entries.isPresent()) {
      List<CarrierTag> carrierTags = new ArrayList<>();
      carrierTags.add(
          CarrierTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      carrierTags.add(
          CarrierTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return carrierTags;
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
   * Constructs a list of TagDetail objects for an Carrier claim.
   *
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleCarrierFields(CarrierClaim claim) {
    Class<?> claimClass = CarrierClaim.class;
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getDateFrom() == null ? LocalDate.parse("1970-01-01") : claim.getDateFrom();
    LocalDate throughDate =
        claim.getDateThrough() == null ? LocalDate.now() : claim.getDateThrough();
    buildDetails(
        getSamhsaCode(claim.getDiagnosisPrincipalCode()),
        "carrier_claims",
        "prncpal_dgns_cd",
        null,
        entries,
        serviceDate,
        throughDate);
    try {
      for (int i = 1; i <= 12; i++) {
        Method method = claimClass.getMethod("getDiagnosis" + i + "Code");
        buildDetails(
            (getSamhsaCode((Optional<String>) method.invoke(claim))),
            "carrier_claims",
            "icd_dgns_cd" + i,
            null,
            entries,
            serviceDate,
            throughDate);
      }
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("There was a problem getting diagnosis codes.", e);
    }
    for (CarrierClaimLine line : claim.getLines()) {
      buildDetails(
          getSamhsaCode(line.getDiagnosisCode()),
          "carrier_claim_lines",
          "line_icd_dgns_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
      buildDetails(
          getSamhsaCode(line.getHcpcsCode()),
          "carrier_claim_lines",
          "hcpcs_cd",
          (int) line.getLineNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
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
  private boolean isDateOutsideOfRange(
      LocalDate earlyDate, LocalDate laterDate, LocalDate dateToTest) {
    return dateToTest.isBefore(earlyDate) || dateToTest.isAfter(laterDate);
  }

  /**
   * Builds a TagDetails object, and adds it to a list of TagDetails.
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
  public Optional<SamhsaEntry> getSamhsaCode(Optional<String> code) {
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
  private Map<String, SamhsaEntry> initializeSamhsaMap(InputStream stream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<SamhsaEntry> entries =
        mapper.readValue(
            stream, mapper.getTypeFactory().constructCollectionType(List.class, SamhsaEntry.class));
    return entries.stream().collect(Collectors.toMap(SamhsaEntry::getCode, entry -> entry));
  }
}
