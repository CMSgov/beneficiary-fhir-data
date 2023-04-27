package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.RaceCategory;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.Sex;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;

/** Transforms CCW {@link Beneficiary} instances into FHIR {@link Patient} resources. */
final class BeneficiaryTransformerV2 {
  /**
   * Transforms a {@link Beneficiary} into a {@link Patient}.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param requestHeader {@link RequestHeaders} the holder that contains all supported resource
   *     request headers
   * @param addHistoricalMbis whether to add historical MBIs as extensions to the transformed
   *     Patient; the beneficiary must have been called using a join against the bene history table
   *     in order to do this
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  @Trace
  public static Patient transform(
      MetricRegistry metricRegistry,
      Beneficiary beneficiary,
      RequestHeaders requestHeader,
      boolean addHistoricalMbis) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(BeneficiaryTransformerV2.class.getSimpleName(), "transform"))
            .time();
    Patient patient = transform(beneficiary, requestHeader, addHistoricalMbis);
    timer.stop();

    return patient;
  }

  /**
   * Transforms a {@link Beneficiary} into a {@link Patient}.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param requestHeader {@link RequestHeaders} the holder that contains all supported resource
   *     request headers
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  @Trace
  public static Patient transform(
      MetricRegistry metricRegistry, Beneficiary beneficiary, RequestHeaders requestHeader) {
    return transform(metricRegistry, beneficiary, requestHeader, false);
  }

  /**
   * Transforms a {@link Beneficiary} into a {@link Patient}.
   *
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param requestHeader {@link RequestHeaders} the holder that contains all supported resource
   *     request headers
   * @param addHistoricalMbiExtensions the add historical mbi extensions
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  private static Patient transform(
      Beneficiary beneficiary, RequestHeaders requestHeader, boolean addHistoricalMbiExtensions) {
    Objects.requireNonNull(beneficiary);

    Patient patient = new Patient();

    /*
     * Notify end users when they receive Patient records impacted by
     * https://jira.cms.gov/browse/BFD-1566. See the documentation on
     * LoadAppOptions.isFilteringNonNullAndNon2023Benes() for details.
     */
    if (!beneficiary.getSkippedRifRecords().isEmpty()) {
      patient
          .getMeta()
          .addTag(
              TransformerConstants.CODING_SYSTEM_BFD_TAGS,
              TransformerConstants.CODING_BFD_TAGS_DELAYED_BACKDATED_ENROLLMENT,
              TransformerConstants.CODING_BFD_TAGS_DELAYED_BACKDATED_ENROLLMENT_DISPLAY);
    }

    // Required values not directly mapped
    patient.getMeta().addProfile(ProfileConstants.C4BB_PATIENT_URL);
    patient.setId(String.valueOf(beneficiary.getBeneficiaryId()));

    // BENE_ID => patient.identifier
    TransformerUtilsV2.addIdentifierSlice(
        patient,
        TransformerUtilsV2.createCodeableConcept(
            TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
            null,
            TransformerConstants.PATIENT_MB_ID_DISPLAY,
            "MB"),
        Optional.of(String.valueOf(beneficiary.getBeneficiaryId())),
        Optional.of(TransformerConstants.CODING_BBAPI_BENE_ID));

    // Unhashed MBI
    if (beneficiary.getMedicareBeneficiaryId().isPresent()) {
      Period mbiPeriod = new Period();
      if (beneficiary.getMbiEffectiveDate().isPresent()) {
        TransformerUtilsV2.setPeriodStart(mbiPeriod, beneficiary.getMbiEffectiveDate().get());
      }
      if (beneficiary.getMbiObsoleteDate().isPresent()) {
        TransformerUtilsV2.setPeriodEnd(mbiPeriod, beneficiary.getMbiObsoleteDate().get());
      }

      addUnhashedIdentifier(
          patient,
          beneficiary.getMedicareBeneficiaryId().get(),
          TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
          TransformerUtilsV2.createIdentifierCurrencyExtension(CurrencyIdentifier.CURRENT),
          mbiPeriod);
    }

    // Add lastUpdated
    TransformerUtilsV2.setLastUpdated(patient, beneficiary.getLastUpdated());

    // NOTE - No longer returning any HCIN value(s) in V2

    /* Only add this if we know the beneficiary was queried in such a way that the bene history / medicare_beneficiaryid_history
     * tables were left joined, such that the beneficiary model object has the historical MBI data to draw from
     * Otherwise JPA will complain. */
    if (addHistoricalMbiExtensions) {
      addHistoricalMbiExtensions(patient, beneficiary);
    }

    // support header includeAddressFields from downstream components e.g. BB2
    // per requirement of BFD-379, BB2 always send header includeAddressFields = False
    Boolean addrHdrVal =
        requestHeader.getValue(R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS);

    if (addrHdrVal != null && addrHdrVal) {
      patient
          .addAddress()
          .setState(beneficiary.getStateCode())
          .setPostalCode(beneficiary.getPostalCode())
          .setCity(beneficiary.getDerivedCityName().orElse(null))
          .addLine(beneficiary.getDerivedMailingAddress1().orElse(null))
          .addLine(beneficiary.getDerivedMailingAddress2().orElse(null))
          .addLine(beneficiary.getDerivedMailingAddress3().orElse(null))
          .addLine(beneficiary.getDerivedMailingAddress4().orElse(null))
          .addLine(beneficiary.getDerivedMailingAddress5().orElse(null))
          .addLine(beneficiary.getDerivedMailingAddress6().orElse(null));
    } else {
      patient
          .addAddress()
          .setState(beneficiary.getStateCode())
          .setPostalCode(beneficiary.getPostalCode());
    }

    if (beneficiary.getBirthDate() != null) {
      patient.setBirthDate(TransformerUtilsV2.convertToDate(beneficiary.getBirthDate()));
    }

    // "Patient.deceased[x]": ["boolean", "dateTime"],
    if (beneficiary.getBeneficiaryDateOfDeath().isPresent()) {
      patient.setDeceased(
          new DateTimeType(
              TransformerUtilsV2.convertToDate(beneficiary.getBeneficiaryDateOfDeath().get()),
              TemporalPrecisionEnum.DAY));
    } else {
      patient.setDeceased(new BooleanType(false));
    }

    char sex = beneficiary.getSex();
    if (sex == Sex.MALE.getCode()) patient.setGender((AdministrativeGender.MALE));
    else if (sex == Sex.FEMALE.getCode()) patient.setGender((AdministrativeGender.FEMALE));
    else patient.setGender((AdministrativeGender.UNKNOWN));

    if (beneficiary.getRace().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient, CcwCodebookVariable.RACE, beneficiary.getRace().get()));

      // for race category, v2 will just treat all race codes as Unknown (UNK);
      // thus we'll simply pass in the Unknown race code .
      RaceCategory raceCategory = TransformerUtilsV2.getRaceCategory('0');
      Extension raceChildOMBExt1 =
          new Extension()
              .setValue(
                  new Coding()
                      .setCode(raceCategory.toCode())
                      .setSystem(raceCategory.getSystem())
                      .setDisplay(raceCategory.getDisplay()))
              .setUrl("ombCategory");

      Extension raceChildOMBExt2 =
          new Extension()
              .setValue(new StringType().setValue(raceCategory.getDisplay()))
              .setUrl("text");

      Extension parentOMBRace = new Extension().setUrl(TransformerConstants.CODING_RACE_US);
      parentOMBRace.addExtension(raceChildOMBExt1);
      parentOMBRace.addExtension(raceChildOMBExt2);

      patient.addExtension(parentOMBRace);
    }

    HumanName name =
        patient
            .addName()
            .addGiven(beneficiary.getNameGiven())
            .setFamily(beneficiary.getNameSurname())
            .setUse(HumanName.NameUse.USUAL);
    if (beneficiary.getNameMiddleInitial().isPresent()) {
      name.addGiven(String.valueOf(beneficiary.getNameMiddleInitial().get()));
    }

    // The reference year of the enrollment data
    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionDate(
              CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear().get()));

      transformMedicaidDualEligibility(patient, beneficiary);
    }

    // Last Updated => Patient.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(patient, beneficiary.getLastUpdated());
    return patient;
  }

  /**
   * Adds the historical mbi data to the patient from the beneficiary data. The historical mbi data
   * is queried from the database and added to the beneficiary model in the resource provider before
   * reaching this point.
   *
   * @param patient the patient to add the historical mbi extensions to
   * @param beneficiary the beneficiary to get the historical data from
   */
  private static void addHistoricalMbiExtensions(Patient patient, Beneficiary beneficiary) {
    Set<String> uniqueHistoricalMbis = new HashSet<>();
    Extension historicalIdentifier =
        TransformerUtilsV2.createIdentifierCurrencyExtension(CurrencyIdentifier.HISTORIC);
    String currentMbi = beneficiary.getMedicareBeneficiaryId().orElse("");

    // Add historical MBI data found in medicare_beneficiaryid_history
    for (MedicareBeneficiaryIdHistory mbiHistory :
        beneficiary.getMedicareBeneficiaryIdHistories()) {

      if (mbiHistory.getMedicareBeneficiaryId().isPresent()) {
        uniqueHistoricalMbis.add(mbiHistory.getMedicareBeneficiaryId().get());
      }
      TransformerUtilsV2.updateMaxLastUpdated(patient, mbiHistory.getLastUpdated());
    }

    // Add historical MBI data found in beneficiaries_history
    for (BeneficiaryHistory mbiHistory : beneficiary.getBeneficiaryHistories()) {

      if (mbiHistory.getMedicareBeneficiaryId().isPresent()) {
        uniqueHistoricalMbis.add(mbiHistory.getMedicareBeneficiaryId().get());
      }
      TransformerUtilsV2.updateMaxLastUpdated(patient, mbiHistory.getLastUpdated());
    }

    // Add a historical extension for each unique non-current MBI found in the history table(s)
    for (String historicalMbi : uniqueHistoricalMbis) {
      // Don't add a historical entry for any MBI which matches the current MBI
      if (!historicalMbi.equals(currentMbi)) {
        addUnhashedIdentifier(
            patient,
            historicalMbi,
            TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
            historicalIdentifier,
            null);
      }
    }
  }

  /**
   * Adds the unhashed identifier.
   *
   * @param patient the FHIR {@link Patient} resource to add the {@link Identifier} to
   * @param value the value for {@link Identifier#getValue()}
   * @param system the value for {@link Identifier#getSystem()}
   * @param identifierCurrencyExtension the {@link Extension} to add to the {@link Identifier}
   * @param mbiPeriod the value for {@link Period}
   */
  private static void addUnhashedIdentifier(
      Patient patient,
      String value,
      String system,
      Extension identifierCurrencyExtension,
      Period mbiPeriod) {

    if (mbiPeriod != null && (mbiPeriod.hasStart() || mbiPeriod.hasEnd())) {
      patient
          .addIdentifier()
          .setValue(value)
          .setSystem(system)
          .setPeriod(mbiPeriod)
          .getType()
          .addCoding()
          .setCode("MC")
          .setSystem(TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE)
          .setDisplay("Patient's Medicare number")
          .addExtension(identifierCurrencyExtension);
    } else {
      patient
          .addIdentifier()
          .setValue(value)
          .setSystem(system)
          .getType()
          .addCoding()
          .setCode("MC")
          .setSystem(TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE)
          .setDisplay("Patient's Medicare number")
          .addExtension(identifierCurrencyExtension);
    }
  }

  /**
   * Adds monthly Patient extensions for Medicare-Medicaid dual eligibility codes.
   *
   * @param patient the FHIR {@link Patient} resource to add to
   * @param beneficiary the value for {@link Beneficiary}
   */
  private static void transformMedicaidDualEligibility(Patient patient, Beneficiary beneficiary) {
    // Monthly Medicare-Medicaid dual eligibility codes
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_01, beneficiary.getMedicaidDualEligibilityJanCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_02, beneficiary.getMedicaidDualEligibilityFebCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_03, beneficiary.getMedicaidDualEligibilityMarCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_04, beneficiary.getMedicaidDualEligibilityAprCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_05, beneficiary.getMedicaidDualEligibilityMayCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_06, beneficiary.getMedicaidDualEligibilityJunCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_07, beneficiary.getMedicaidDualEligibilityJulCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_08, beneficiary.getMedicaidDualEligibilityAugCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_09, beneficiary.getMedicaidDualEligibilitySeptCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_10, beneficiary.getMedicaidDualEligibilityOctCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_11, beneficiary.getMedicaidDualEligibilityNovCode());
    addPatientExtension(
        patient, CcwCodebookVariable.DUAL_12, beneficiary.getMedicaidDualEligibilityDecCode());
  }

  /**
   * Sets the Coverage.relationship Looks up or adds a contained {@link Identifier} object to the
   * current {@link Patient}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param patient The {@link Patient} to Patient details
   * @param ccwVariable The {@link CcwCodebookVariable} variable associated with the Coverage
   * @param optVal The {@link String} value associated with the Coverage
   */
  static void addPatientExtension(
      Patient patient, CcwCodebookVariable ccwVariable, Optional<String> optVal) {
    optVal.ifPresent(
        value ->
            patient.addExtension(
                TransformerUtilsV2.createExtensionCoding(patient, ccwVariable, value)));
  }

  /** Enumerates the options for the currency of an {@link Identifier}. */
  public static enum CurrencyIdentifier {
    /** Represents a current identifier. */
    CURRENT,
    /** Represents a historic identifier. */
    HISTORIC;
  }
}
