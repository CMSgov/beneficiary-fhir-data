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
import gov.cms.bfd.pipeline.sharedutils.model.ClaimSamhsaCodeEntries;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaEntry;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
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
  /**
   * Will contain the list of the methods for each entity to retrieve the fields with possible
   * SAMHSA codes.
   */
  public static final String CLAIM_SAMHSA_METHODS_YAML = "claim_samhsa_methods.yaml";

  /** Map of the SAMHSA code entries, with the SAMHSA code as the key. */
  private Map<String, SamhsaEntry> samhsaMap = new HashMap<>();

  /** Instance of this class. Will be a singleton. */
  private static SamhsaUtil samhsaUtil;

  /** The file from which SAMHSA entries are pulled. Must be in the resources folder. */
  private static final String SAMHSA_LIST_RESOURCE = "security_labels.yml";

  /** The entries for CLAIM_SAMHSA_METHODS_YAML. */
  private List<ClaimSamhsaCodeEntries> claimSamhsaCodeMethods;

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
    createClaimSamhsaMethods();
  }

  /** Loads the yaml file for the claim samhsa methods, and passes it along to be processed. */
  private void createClaimSamhsaMethods() {
    InputStream fileStream = getFileInputStream(CLAIM_SAMHSA_METHODS_YAML);
    try {
      claimSamhsaCodeMethods = createSamhsaCodeMethodList(fileStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets an input stream from a file in the resources folder.
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
    try {
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
    } catch (Exception e) {
      throw new RuntimeException("There was an error creating SAMHSA tags.", e);
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
  private List<McsTag> checkAndProcessMcsClaim(RdaMcsClaim mcsClaim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
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
  private List<HhaTag> checkAndProcessHhaClaim(HHAClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Optional<List<TagDetails>> entries =
        getPossibleCCWFields(
            claim,
            claim.getLines(),
            HHAClaim.class,
            HHAClaimLine.class,
            "HHAClaim",
            "HHAClaimLine");
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
  private List<HospiceTag> checkAndProcessHospiceClaim(HospiceClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Optional<List<TagDetails>> entries =
        getPossibleCCWFields(
            claim,
            claim.getLines(),
            HospiceClaim.class,
            HospiceClaimLine.class,
            "HospiceClaim",
            "HospiceClaimLine");
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
  private List<DmeTag> checkAndProcessDmeClaim(DMEClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Optional<List<TagDetails>> entries =
        getPossibleCCWFields(
            claim,
            claim.getLines(),
            DMEClaim.class,
            DMEClaimLine.class,
            "DMEClaim",
            "DMEClaimLine");
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
  private List<OutpatientTag> checkAndProcessOutpatientClaim(OutpatientClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Optional<List<TagDetails>> entries =
        getPossibleCCWFields(
            claim,
            claim.getLines(),
            OutpatientClaim.class,
            OutpatientClaimLine.class,
            "OutpatientClaim",
            "OutpatientClaimLine");
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
  private List<InpatientTag> checkAndProcessInpatientClaim(InpatientClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Optional<List<TagDetails>> entries =
        getPossibleCCWFields(
            claim,
            claim.getLines(),
            InpatientClaim.class,
            InpatientClaimLine.class,
            "InpatientClaim",
            "InpatientClaimLine");
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
  private List<SnfTag> checkAndProcessSnfClaim(SNFClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Optional<List<TagDetails>> entries =
        getPossibleCCWFields(
            claim,
            claim.getLines(),
            SNFClaim.class,
            SNFClaimLine.class,
            "SNFClaim",
            "SNFClaimLine");
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
   * /** * Constructs a list of TagDetail objects for a CCW claim.
   *
   * @param claim The claim
   * @param claimLines the claim lines
   * @param claimClass the class for the claim
   * @param claimLinesClass the class for th claim lines
   * @param claimClassName a String containing the name of the claim class
   * @param lineClassName a string containing the name of the claimLine clas
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   * @param <TClaim> The type of the claim
   * @param <TClaimLine> The type of the claim line
   * @throws NoSuchMethodException if the method doesn't exist
   * @throws InvocationTargetException on a bad invocation target
   * @throws IllegalAccessException on illegal access
   */
  private <TClaim, TClaimLine> Optional<List<TagDetails>> getPossibleCCWFields(
      TClaim claim,
      List<TClaimLine> claimLines,
      Class claimClass,
      Class claimLinesClass,
      String claimClassName,
      String lineClassName)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    List<TagDetails> entries = new ArrayList<>();
    Method fromDateMethod = claimClass.getMethod("getDateFrom");
    LocalDate serviceDate = (LocalDate) fromDateMethod.invoke(claim);
    if (serviceDate == null) {
      serviceDate = LocalDate.parse("1970-01-01");
    }
    Method throughDateMethod = claimClass.getMethod("getDateThrough");
    LocalDate throughDate = (LocalDate) throughDateMethod.invoke(claim);
    if (throughDate == null) {
      throughDate = LocalDate.MAX;
    }
    Optional<ClaimSamhsaCodeEntries> claimEntry =
        claimSamhsaCodeMethods.stream()
            .filter(e -> e.getClaimClass().equals(claimClassName))
            .findFirst();
    Optional<ClaimSamhsaCodeEntries> claimLinesEntry =
        claimSamhsaCodeMethods.stream()
            .filter(e -> e.getClaimClass().equals(lineClassName))
            .findFirst();
    iterateSamhsaMethods(claim, claimEntry, claimClass, null, entries, serviceDate, throughDate);
    for (TClaimLine claimLine : claimLines) {
      Method lineNumberMethod = claimLinesClass.getMethod("getLineNumber");
      short claimLineNumber = (short) lineNumberMethod.invoke(claimLine);
      iterateSamhsaMethods(
          claimLine,
          claimLinesEntry,
          claimLinesClass,
          (int) claimLineNumber,
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
  private List<CarrierTag> checkAndProcessCarrierClaim(CarrierClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Optional<List<TagDetails>> entries =
        getPossibleCCWFields(
            claim,
            claim.getLines(),
            CarrierClaim.class,
            CarrierClaimLine.class,
            "CarrierClaim",
            "CarrierClaimLine");
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
  private List<FissTag> checkAndProcessFissClaim(RdaFissClaim fissClaim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
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
   * Creates the list of methods to access SAMHSA codes.
   *
   * @param stream the file stream of the yaml to process.
   * @return a list of entries for each table.
   * @throws IOException if the file cannot be read.
   */
  private List<ClaimSamhsaCodeEntries> createSamhsaCodeMethodList(InputStream stream)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    List<ClaimSamhsaCodeEntries> entries =
        mapper.readValue(
            stream,
            mapper
                .getTypeFactory()
                .constructCollectionType(List.class, ClaimSamhsaCodeEntries.class));
    return entries;
  }

  /**
   * Iterates over the SAMHSA fields for an entity, and uses reflection to call the entity's get
   * method for that class.
   *
   * @param claim The Claim
   * @param claimEntry The entry for this claim in the yaml.
   * @param claimClass The class of the claim.
   * @param lineNumber The line number of the claim, if this is a ClaimLine class.
   * @param entries The list to save the tag details to.
   * @param serviceDate the service date.
   * @param throughDate the through date.
   * @param <TClaim> the type of the claim.
   * @throws NoSuchMethodException throws if method is not found.
   * @throws IllegalAccessException throws if illegal access.
   * @throws InvocationTargetException throws if invocationTarget exception.
   */
  private <TClaim> void iterateSamhsaMethods(
      TClaim claim,
      Optional<ClaimSamhsaCodeEntries> claimEntry,
      Class<?> claimClass,
      Integer lineNumber,
      List<TagDetails> entries,
      LocalDate serviceDate,
      LocalDate throughDate)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    if (claimEntry.isPresent()) {
      String tableName = claimEntry.get().getTable();
      for (SamhsaFields field : claimEntry.get().getFields()) {
        Method method = claimClass.getMethod(field.getMethod());
        var code = method.invoke(claim);
        if (!(code instanceof Optional)) {
          code = Optional.ofNullable(code);
        }
        buildDetails(
            getSamhsaCode((Optional<String>) code),
            tableName,
            field.getColumn(),
            lineNumber,
            entries,
            serviceDate,
            throughDate);
      }
    }
  }

  /**
   * Constructs a list of TagDetail objects for an MCS claim.
   *
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleMcsSamhsaFields(RdaMcsClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Class<?> mcsDetailsClass = RdaMcsDetail.class;
    Class<?> mcsDiagnosisCodeClass = RdaMcsDiagnosisCode.class;
    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getIdrHdrFromDateOfSvc() == null
            ? LocalDate.parse("1970-01-01")
            : claim.getIdrHdrFromDateOfSvc();
    LocalDate throughDate =
        claim.getIdrHdrToDateOfSvc() == null ? LocalDate.now() : claim.getIdrHdrToDateOfSvc();
    Optional<ClaimSamhsaCodeEntries> claimDetailsEntry =
        claimSamhsaCodeMethods.stream()
            .filter(e -> e.getClaimClass().equals("RdaMcsDetail"))
            .findFirst();
    Optional<ClaimSamhsaCodeEntries> claimDiagnosisEntry =
        claimSamhsaCodeMethods.stream()
            .filter(e -> e.getClaimClass().equals("RdaMcsDiagnosisCode"))
            .findFirst();
    for (RdaMcsDetail detail : claim.getDetails()) {
      iterateSamhsaMethods(
          detail,
          claimDetailsEntry,
          mcsDetailsClass,
          (int) detail.getIdrDtlNumber(),
          entries,
          serviceDate,
          throughDate);
    }
    for (RdaMcsDiagnosisCode diagnosis : claim.getDiagCodes()) {
      iterateSamhsaMethods(
          diagnosis,
          claimDiagnosisEntry,
          mcsDiagnosisCodeClass,
          (int) diagnosis.getRdaPosition(),
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
   * @param claim The claim to check.
   * @return a list of TagDetail objects, one for each SAMHSA code found in the claim.
   */
  private Optional<List<TagDetails>> getPossibleFissSamhsaFields(RdaFissClaim claim)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Class<?> fissClaimClass = RdaFissClaim.class;
    Class<?> fissDiagnosisCodeClass = RdaFissDiagnosisCode.class;
    Class<?> fissProcCodeClass = RdaFissProcCode.class;
    Class<?> fissRevenueLineClass = RdaFissRevenueLine.class;

    List<TagDetails> entries = new ArrayList<>();
    LocalDate serviceDate =
        claim.getStmtCovFromDate() == null
            ? LocalDate.parse("1970-01-01")
            : claim.getStmtCovFromDate();
    LocalDate throughDate =
        claim.getStmtCovToDate() == null ? LocalDate.now() : claim.getStmtCovToDate();
    Optional<ClaimSamhsaCodeEntries> claimEntry =
        claimSamhsaCodeMethods.stream()
            .filter(e -> e.getClaimClass().equals("RdaFissClaim"))
            .findFirst();
    Optional<ClaimSamhsaCodeEntries> claimDiagEntry =
        claimSamhsaCodeMethods.stream()
            .filter(e -> e.getClaimClass().equals("RdaFissDiagnosisCode"))
            .findFirst();
    Optional<ClaimSamhsaCodeEntries> claimProcEntry =
        claimSamhsaCodeMethods.stream()
            .filter(e -> e.getClaimClass().equals("RdaFissProcCode"))
            .findFirst();
    Optional<ClaimSamhsaCodeEntries> claimRevenueEntry =
        claimSamhsaCodeMethods.stream()
            .filter(e -> e.getClaimClass().equals("RdaFissRevenueLine"))
            .findFirst();
    iterateSamhsaMethods(
        claim, claimEntry, fissClaimClass, null, entries, serviceDate, throughDate);
    for (RdaFissDiagnosisCode diagCode : claim.getDiagCodes()) {
      iterateSamhsaMethods(
          diagCode,
          claimDiagEntry,
          fissDiagnosisCodeClass,
          (int) diagCode.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    for (RdaFissProcCode procCode : claim.getProcCodes()) {
      iterateSamhsaMethods(
          procCode,
          claimProcEntry,
          fissProcCodeClass,
          (int) procCode.getRdaPosition(),
          entries,
          serviceDate,
          throughDate);
    }
    for (RdaFissRevenueLine line : claim.getRevenueLines()) {
      iterateSamhsaMethods(
          line,
          claimRevenueEntry,
          fissRevenueLineClass,
          (int) line.getRdaPosition(),
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
