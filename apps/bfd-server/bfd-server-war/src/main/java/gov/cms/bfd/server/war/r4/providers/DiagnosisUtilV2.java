package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.Diagnosis.DiagnosisLabel;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimInpatientInstitutionalDiagnosisType;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimOutpatientInstitutionalDiagnosisType;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianDiagnosisType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.codesystems.ExDiagnosistype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains all functionality related to adding {@link Diagnosis} elements to an {@link
 * ExplanationOfBenefit}.
 */
public class DiagnosisUtilV2 {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosisUtilV2.class);

  /** Stores the diagnosis ICD codes and their display values. */
  private static Map<String, String> icdMap = null;

  /** Tracks the national drug codes that have already had code lookup failures. */
  private static final Set<String> icdLookupMissingFailures = new HashSet<>();

  /**
   * Retrieves the Diagnosis display value from a Diagnosis code look up file.
   *
   * @param icdCode - Diagnosis code
   * @return the icd code display value
   */
  public static String retrieveIcdCodeDisplay(String icdCode) {
    if (icdCode.isEmpty()) {
      return null;
    }

    /*
     * There's a race condition here: we may initialize this static field more than once if multiple
     * requests come in at the same time. However, the assignment is atomic, so the race and
     * reinitialization is harmless other than maybe wasting a bit of time.
     */
    // read the entire ICD file the first time and put in a Map
    if (icdMap == null) {
      icdMap = readIcdCodeFile();
    }

    if (icdMap.containsKey(icdCode.toUpperCase())) {
      String icdCodeDisplay = icdMap.get(icdCode);
      return icdCodeDisplay;
    }

    // log which ICD codes we couldn't find a match for in our downloaded ICD file
    if (!icdLookupMissingFailures.contains(icdCode)) {
      icdLookupMissingFailures.add(icdCode);
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
   * @return the map of ICD codes
   */
  private static Map<String, String> readIcdCodeFile() {
    Map<String, String> icdDiagnosisMap = new HashMap<String, String>();

    try (final InputStream icdCodeDisplayStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("DGNS_CD.txt");
        final BufferedReader icdCodesIn =
            new BufferedReader(new InputStreamReader(icdCodeDisplayStream))) {
      /*
       * We want to extract the ICD Diagnosis codes and display values and put in a map for easy
       * retrieval to get the display value icdColumns[1] is DGNS_DESC(i.e. 7840 code is HEADACHE
       * description)
       */
      String line = "";
      icdCodesIn.readLine();
      while ((line = icdCodesIn.readLine()) != null) {
        String icdColumns[] = line.split("\t");
        icdDiagnosisMap.put(icdColumns[0], icdColumns[1]);
      }
      icdCodesIn.close();
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read ICD code data.", e);
    }

    return icdDiagnosisMap;
  }

  /**
   * Checks to see if a diagnosis that matches already exists.
   *
   * @param diag the diagnosis to check for
   * @param codeableConcept the codeable concept to check within
   * @return {@code true} if the diagnosis exists within the codeableConcept
   */
  static boolean containedIn(Diagnosis diag, CodeableConcept codeableConcept) {
    return codeableConcept.getCoding().stream()
            .filter(c -> diag.getCode().equals(c.getCode()))
            .filter(c -> diag.getFhirSystem().equals(c.getSystem()))
            .count()
        != 0;
  }

  /**
   * Converts a {@link Diagnosis} to an R4 {@link CodeableConcept}.
   *
   * @param diag The diagnosis to convert
   * @return the converted codeable concept
   */
  static CodeableConcept toCodeableConcept(Diagnosis diag) {
    CodeableConcept codeableConcept = new CodeableConcept();
    String system = diag.getFhirSystem();
    String code = diag.getCode();

    /*
     * Due to meeting CARIN conformance, an additional coding with the ICD-10-CM system URL
     * must be added. A coding with the ICD-10 system URL will still be present for backwards compatibility.
     * See JIRA ticket: https://jira.cms.gov/browse/BFD-1894
     */
    if (system.equals(IcdCode.CODING_SYSTEM_ICD_10)) {
      addCodingToCodeableConcept(codeableConcept, IcdCode.CODING_SYSTEM_ICD_10_CM, code);
    }
    addCodingToCodeableConcept(codeableConcept, system, code);

    return codeableConcept;
  }

  /**
   * Creates a {@link Coding} from an R4 {@link CodeableConcept}.
   *
   * @param codeableConcept The CodeableConcept to add a Coding for
   * @param system The system to use in the new coding
   * @param code the code to use in the new coding
   */
  private static void addCodingToCodeableConcept(
      CodeableConcept codeableConcept, String system, String code) {
    codeableConcept
        .addCoding()
        .setSystem(system)
        .setCode(code)
        // TODO: This code should be pulled out to a common library
        .setDisplay(retrieveIcdCodeDisplay(code));
  }

  /**
   * Generically attempts to retrieve a diagnosis from the current claim.
   *
   * @param substitution The methods to retrive diagnosis information all follow a similar pattern.
   *     This value is used to substitute into that pattern when looking up the specific method to
   *     retrive information with.
   * @param codes The mapping of diagnosis codes by their property name and respective value
   * @param codeVersions The mapping of diagnosis code versions by their property name and
   *     respective value
   * @param presentOnAdms The mapping of diagnosis "PresentOnAdmissionCode" codes by their property
   *     name and respective value
   * @param ccw CCW Codebook value that represents which "PresentOnAdmissionCode" is being used.
   *     Example: {@link CcwCodebookVariable#CLM_POA_IND_SW5}
   * @param label One or more labels to use when mapping the diagnosis.
   * @return a {@link Diagnosis} or {@link Optional#empty()}
   */
  public static Optional<Diagnosis> extractDiagnosis(
      String substitution,
      Map<String, Optional<String>> codes,
      Map<String, Optional<Character>> codeVersions,
      Map<String, Optional<Character>> presentOnAdms,
      Optional<CcwCodebookInterface> ccw,
      DiagnosisLabel label) {
    Optional<String> code =
        codes.getOrDefault(String.format("diagnosis%sCode", substitution), Optional.empty());
    Optional<Character> codeVersion =
        codeVersions.getOrDefault(
            String.format("diagnosis%sCodeVersion", substitution), Optional.empty());
    Optional<Character> presentOnAdm =
        presentOnAdms.getOrDefault(
            String.format("diagnosis%sPresentOnAdmissionCode", substitution), Optional.empty());
    return Diagnosis.from(code, codeVersion, presentOnAdm, ccw, label);
  }

  /**
   * Extracts nearly all diagnosis types from a Claim. If the specific claim type doesn't have the
   * given diagnosis it will just be skipped.
   *
   * @param codes The mapping of diagnosis codes by their property name and respective value
   * @param codeVersions The mapping of diagnosis code versions by their property name and
   *     respective value
   * @param presentOnAdms The mapping of diagnosis "PresentOnAdmissionCode" codes by their property
   *     name and respective value
   * @return the {@link Diagnosis} that can be extracted from the specified {@link
   *     gov.cms.bfd.model.rif.entities.InpatientClaim}
   */
  static List<Diagnosis> extractDiagnoses(
      Map<String, Optional<String>> codes,
      Map<String, Optional<Character>> codeVersions,
      Map<String, Optional<Character>> presentOnAdms) {
    List<Optional<Diagnosis>> diagnosis = new ArrayList<>();

    // Handle the "special" diagnosis fields
    diagnosis.add(
        extractDiagnosis(
            "Admitting",
            codes,
            codeVersions,
            presentOnAdms,
            Optional.empty(),
            DiagnosisLabel.ADMITTING));
    diagnosis.add(
        extractDiagnosis(
            "1",
            codes,
            codeVersions,
            presentOnAdms,
            Optional.of(CcwCodebookVariable.CLM_POA_IND_SW1),
            DiagnosisLabel.PRINCIPAL));
    diagnosis.add(
        extractDiagnosis(
            "Principal",
            codes,
            codeVersions,
            presentOnAdms,
            Optional.empty(),
            DiagnosisLabel.PRINCIPAL));

    // Generically handle the rest (2-25)
    final int FIRST_DIAG = 2;
    final int LAST_DIAG = 25;
    IntStream.range(FIRST_DIAG, LAST_DIAG + 1)
        .mapToObj(
            i ->
                extractDiagnosis(
                    String.valueOf(i),
                    codes,
                    codeVersions,
                    presentOnAdms,
                    Optional.of(CcwCodebookVariable.valueOf("CLM_POA_IND_SW" + i)),
                    DiagnosisLabel.OTHER))
        .forEach(diagnosis::add);

    // Handle first external diagnosis
    diagnosis.add(
        extractDiagnosis(
            "External1",
            codes,
            codeVersions,
            presentOnAdms,
            Optional.of(CcwCodebookVariable.CLM_E_POA_IND_SW1),
            DiagnosisLabel.FIRSTEXTERNAL));
    diagnosis.add(
        extractDiagnosis(
            "ExternalFirst",
            codes,
            codeVersions,
            presentOnAdms,
            Optional.empty(),
            DiagnosisLabel.FIRSTEXTERNAL));

    // Generically handle the rest (2-12)
    final int FIRST_EX_DIAG = 2;
    final int LAST_EX_DIAG = 12;
    IntStream.range(FIRST_EX_DIAG, LAST_EX_DIAG + 1)
        .mapToObj(
            i ->
                extractDiagnosis(
                    "External" + String.valueOf(i),
                    codes,
                    codeVersions,
                    presentOnAdms,
                    Optional.of(CcwCodebookVariable.valueOf("CLM_E_POA_IND_SW" + i)),
                    DiagnosisLabel.EXTERNAL))
        .forEach(diagnosis::add);

    // Handle Inpatient Diagnosis.
    // RSN_VISIT_CD(1-3)        => diagnosis.diagnosisCodeableConcept
    // RSN_VISIT_VRSN_CD(1-3)   => diagnosis.diagnosisCodeableConcept
    final int FIRST_INPATIENT_DIAGNOSIS = 1;
    final int LAST_INPATIENT_DIAGNOSIS = 3;
    IntStream.range(FIRST_INPATIENT_DIAGNOSIS, LAST_INPATIENT_DIAGNOSIS + 1)
        .mapToObj(
            i ->
                extractDiagnosis(
                    String.format("Admission%d", i),
                    codes,
                    codeVersions,
                    Map.of(),
                    Optional.empty(),
                    DiagnosisLabel.REASONFORVISIT))
        .forEach(diagnosis::add);

    // Some may be empty.  Convert from List<Optional<Diagnosis>> to List<Diagnosis>
    return diagnosis.stream()
        .filter(d -> d.isPresent())
        .map(d -> d.get())
        .collect(Collectors.toList());
  }

  /**
   * Translates from {@link DiagnosisLabel} to {@link C4BBClaimInpatientInstitutionalDiagnosisType}.
   *
   * @param label the label to transform
   * @return the label translated to the specified type
   */
  static Coding translateLabelCodeInpatient(DiagnosisLabel label) {
    switch (label) {
      case PRINCIPAL:
        return new Coding(
            C4BBClaimInpatientInstitutionalDiagnosisType.PRINCIPAL.getSystem(),
            C4BBClaimInpatientInstitutionalDiagnosisType.PRINCIPAL.toCode(),
            C4BBClaimInpatientInstitutionalDiagnosisType.PRINCIPAL.getDisplay());

      case ADMITTING:
        return new Coding(
            C4BBClaimInpatientInstitutionalDiagnosisType.ADMITTING.getSystem(),
            C4BBClaimInpatientInstitutionalDiagnosisType.ADMITTING.toCode(),
            C4BBClaimInpatientInstitutionalDiagnosisType.ADMITTING.getDisplay());

      case FIRSTEXTERNAL:
      case EXTERNAL:
        return new Coding(
            C4BBClaimInpatientInstitutionalDiagnosisType.EXTERNAL_CAUSE.getSystem(),
            C4BBClaimInpatientInstitutionalDiagnosisType.EXTERNAL_CAUSE.toCode(),
            C4BBClaimInpatientInstitutionalDiagnosisType.EXTERNAL_CAUSE.getDisplay());

      case REASONFORVISIT:
      case OTHER:
      default:
        return new Coding(
            C4BBClaimInpatientInstitutionalDiagnosisType.OTHER.getSystem(),
            C4BBClaimInpatientInstitutionalDiagnosisType.OTHER.toCode(),
            C4BBClaimInpatientInstitutionalDiagnosisType.OTHER.getDisplay());
    }
  }

  /**
   * Translates from {@link DiagnosisLabel} to {@link
   * C4BBClaimOutpatientInstitutionalDiagnosisType}.
   *
   * @param label the label to transform
   * @return the label translated to the specified type
   */
  static Coding translateLabelCodeOutpatient(DiagnosisLabel label) {
    switch (label) {
      case PRINCIPAL:
        return new Coding(
            C4BBClaimOutpatientInstitutionalDiagnosisType.PRINCIPAL.getSystem(),
            C4BBClaimOutpatientInstitutionalDiagnosisType.PRINCIPAL.toCode(),
            C4BBClaimOutpatientInstitutionalDiagnosisType.PRINCIPAL.getDisplay());

      case FIRSTEXTERNAL:
      case EXTERNAL:
        return new Coding(
            C4BBClaimOutpatientInstitutionalDiagnosisType.EXTERNAL_CAUSE.getSystem(),
            C4BBClaimOutpatientInstitutionalDiagnosisType.EXTERNAL_CAUSE.toCode(),
            C4BBClaimOutpatientInstitutionalDiagnosisType.EXTERNAL_CAUSE.getDisplay());

      case REASONFORVISIT:
        return new Coding(
            C4BBClaimOutpatientInstitutionalDiagnosisType.PATIENT_REASON.getSystem(),
            C4BBClaimOutpatientInstitutionalDiagnosisType.PATIENT_REASON.toCode(),
            C4BBClaimOutpatientInstitutionalDiagnosisType.PATIENT_REASON.getDisplay());

      case ADMITTING:
      case OTHER:
      default:
        return new Coding(
            C4BBClaimOutpatientInstitutionalDiagnosisType.OTHER.getSystem(),
            C4BBClaimOutpatientInstitutionalDiagnosisType.OTHER.toCode(),
            C4BBClaimOutpatientInstitutionalDiagnosisType.OTHER.getDisplay());
    }
  }

  /**
   * Translates from {@link DiagnosisLabel} to {@link ExDiagnosistype}.
   *
   * <p>*Note* : Pharmacy EOBs do not get diagnosis mapped at this point in time. This is here for
   * completeness and consistency.
   *
   * @param label the label to transform
   * @return the label translated to the specified type
   */
  static Coding translateLabelPharmacy(DiagnosisLabel label) {
    switch (label) {
      case PRINCIPAL:
        return new Coding(
            ExDiagnosistype.PRINCIPAL.getSystem(),
            ExDiagnosistype.PRINCIPAL.toCode(),
            ExDiagnosistype.PRINCIPAL.getDisplay());

      case ADMITTING:
        return new Coding(
            ExDiagnosistype.ADMITTING.getSystem(),
            ExDiagnosistype.ADMITTING.toCode(),
            ExDiagnosistype.ADMITTING.getDisplay());

      case FIRSTEXTERNAL:
      case EXTERNAL:
      case REASONFORVISIT:
      case OTHER:
      default:
        return new Coding(label.getSystem(), label.toCode(), label.getDisplay());
    }
  }

  /**
   * Translates from {@link DiagnosisLabel} to {@link
   * C4BBClaimProfessionalAndNonClinicianDiagnosisType}.
   *
   * @param label the label to transform
   * @return the label translated to the specified type
   */
  static Coding translateLabelProfessional(DiagnosisLabel label) {
    switch (label) {
      case PRINCIPAL:
        return new Coding(
            C4BBClaimProfessionalAndNonClinicianDiagnosisType.PRINCIPAL.getSystem(),
            C4BBClaimProfessionalAndNonClinicianDiagnosisType.PRINCIPAL.toCode(),
            C4BBClaimProfessionalAndNonClinicianDiagnosisType.PRINCIPAL.getDisplay());

      // Only Principal and Secondary are valid for this EOB type
      case ADMITTING:
      case FIRSTEXTERNAL:
      case EXTERNAL:
      case REASONFORVISIT:
      case OTHER:
      default:
        return new Coding(
            C4BBClaimProfessionalAndNonClinicianDiagnosisType.SECONDARY.getSystem(),
            C4BBClaimProfessionalAndNonClinicianDiagnosisType.SECONDARY.toCode(),
            C4BBClaimProfessionalAndNonClinicianDiagnosisType.SECONDARY.getDisplay());
    }
  }

  /**
   * Translates from {@link DiagnosisLabel} to an EOB type specific Coding based on the {@link
   * ClaimType}.
   *
   * @param label the label to transform
   * @param claimType the claim type to use for the translation
   * @return the label translated to the specified type
   */
  static Coding translateLabelCode(DiagnosisLabel label, ClaimType claimType) {
    switch (claimType) {
      case PDE:
        return translateLabelPharmacy(label);

      case INPATIENT:
        return translateLabelCodeInpatient(label);

      case OUTPATIENT:
      case HOSPICE:
      case SNF:
      case DME:
        return translateLabelCodeOutpatient(label);

      case CARRIER:
      case HHA:
        return translateLabelProfessional(label);

      default:
        // All options on ClaimType are covered above, but this is there to appease linter
        throw new BadCodeMonkeyException("No match found for DiagnosisLabel");
    }
  }

  /**
   * Translates a list of {@link DiagnosisLabel} to an EOB type specific Coding based on the {@link
   * ClaimType}.
   *
   * <p>In practice, the list will only ever be one {@link DiagnosisLabel}. The {@link Diagnosis}
   * class allows multiple labels to be present, so we cover that case here. In V2, only a single
   * label will ever be assigned.
   *
   * @param labels the labels to transform
   * @param claimType the claim type to use for the label translation
   * @return a new {@link CodeableConcept} with the coding set based on the labels
   */
  static CodeableConcept translateLabels(Set<DiagnosisLabel> labels, ClaimType claimType) {
    CodeableConcept diagType = new CodeableConcept();

    List<Coding> codings =
        labels.stream().map(l -> translateLabelCode(l, claimType)).collect(Collectors.toList());

    return diagType.setCoding(codings);
  }

  /**
   * Adds a diagnosis code to the specified {@link ExplanationOfBenefit} and returns the sequence
   * value added. If the specified code already exists, returns the existing sequence value and does
   * not add anything.
   *
   * @param eob the {@link ExplanationOfBenefit} to (possibly) modify
   * @param diagnosis the {@link Diagnosis} to add, if it's not already present
   * @param claimType the claim type to use when adding the diagnosis code
   * @return the {@link DiagnosisComponent#getSequence()} of the existing or newly-added entry
   */
  static int addDiagnosisCode(ExplanationOfBenefit eob, Diagnosis diagnosis, ClaimType claimType) {
    // Filter out if the diagnosis is already contained in the document
    Optional<DiagnosisComponent> existingDiagnosis =
        eob.getDiagnosis().stream()
            .filter(d -> d.getDiagnosis() instanceof CodeableConcept)
            .filter(d -> containedIn(diagnosis, (CodeableConcept) d.getDiagnosis()))
            .findAny();

    // If we already have a match, we are done
    if (existingDiagnosis.isPresent()) {
      return existingDiagnosis.get().getSequenceElement().getValue();
    }

    // Set diagnosisCodeableConcept
    DiagnosisComponent diagnosisComponent =
        new DiagnosisComponent().setSequence(eob.getDiagnosis().size() + 1);
    diagnosisComponent.setDiagnosis(toCodeableConcept(diagnosis));

    // Set Type
    diagnosisComponent.addType(translateLabels(diagnosis.getLabels(), claimType));

    if (diagnosis.getPresentOnAdmission().isPresent()
        && diagnosis.getPresentOnAdmissionCode().isPresent()) {
      diagnosisComponent.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              eob, diagnosis.getPresentOnAdmissionCode().get(), diagnosis.getPresentOnAdmission()));
    }

    eob.getDiagnosis().add(diagnosisComponent);

    return diagnosisComponent.getSequenceElement().getValue();
  }

  /**
   * Optionally adds a diagnosis.
   *
   * @param eob the {@link ExplanationOfBenefit} to (possibly) modify
   * @param diagnosis the {@link Diagnosis} to add, if it's not already present
   * @param claimType the claim type to use when adding the diagnosis
   * @return the {@link DiagnosisComponent#getSequence()} of the existing or newly-added entry
   */
  static Optional<Integer> addDiagnosisCode(
      ExplanationOfBenefit eob, Optional<Diagnosis> diagnosis, ClaimType claimType) {
    return diagnosis.map(d -> (addDiagnosisCode(eob, d, claimType)));
  }

  /**
   * Add a diagnosis link.
   *
   * @param eob the {@link ExplanationOfBenefit} that the specified {@link ItemComponent} is a child
   *     of
   * @param item the {@link ItemComponent} to add a diagnosis code entry to
   * @param diagnosis the {@link Diagnosis} to add a link for
   * @param claimType the claim type to use when adding the sequence
   */
  static void addDiagnosisLink(
      ExplanationOfBenefit eob,
      ItemComponent item,
      Optional<Diagnosis> diagnosis,
      ClaimType claimType) {
    diagnosis.ifPresent(diag -> item.addDiagnosisSequence(addDiagnosisCode(eob, diag, claimType)));
  }
}
