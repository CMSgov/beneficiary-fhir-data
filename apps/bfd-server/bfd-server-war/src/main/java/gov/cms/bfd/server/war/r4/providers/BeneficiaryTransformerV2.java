package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_C4DIC_ENABLED;
import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.RaceCategory;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.Sex;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants.CurrencyIdentifier;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Transforms CCW {@link Beneficiary} instances into FHIR {@link Patient} resources. */
@Component
public class BeneficiaryTransformerV2 {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** Enabled CARIN profiles. */
  private final EnumSet<Profile> enabledProfiles;

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param c4dicEnabled whether to enable the C4DIC profile
   */
  public BeneficiaryTransformerV2(
      MetricRegistry metricRegistry,
      @Value("${" + SSM_PATH_C4DIC_ENABLED + ":false}") Boolean c4dicEnabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.enabledProfiles = Profile.getEnabledProfiles(c4dicEnabled);
  }

  /**
   * Transforms a {@link Beneficiary} into a {@link Patient}.
   *
   * <p>Implicitly does not add mbi historical extensions to the response. Should be used for
   * beneficiaries that do not join on the history table during the db query that finds the
   * beneficiary data (i.e. does not have the data for beneficiaryHistories on the beneficiary
   * model).
   *
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param requestHeader {@link RequestHeaders} the holder that contains all supported resource
   *     request headers
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  public Patient transform(Beneficiary beneficiary, RequestHeaders requestHeader) {
    return transform(beneficiary, requestHeader, false);
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
  public Patient transform(
      Beneficiary beneficiary, RequestHeaders requestHeader, boolean addHistoricalMbiExtensions) {
    try (Timer.Context timer =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry, getClass().getSimpleName(), "transform")) {
      requireNonNull(beneficiary);

      Patient patient = new Patient();

      for (Profile profile : enabledProfiles) {
        patient.getMeta().addProfile(profile.getVersionedPatientUrl());
      }

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

      /*
       * Only add this if we know the beneficiary was queried in such a way that the
       * bene history
       * table was left joined, such that the beneficiary model object has the
       * historical MBI data to draw from
       * Otherwise JPA will complain.
       */
      if (addHistoricalMbiExtensions) {
        addHistoricalMbiExtensions(patient, beneficiary);
      }

      // support header includeAddressFields from downstream components e.g. BB2
      // per requirement of BFD-379, BB2 always send header includeAddressFields =
      // False
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
        patient.setBirthDate(CommonTransformerUtils.convertToDate(beneficiary.getBirthDate()));
      }

      // "Patient.deceased[x]": ["boolean", "dateTime"],
      if (beneficiary.getBeneficiaryDateOfDeath().isPresent()) {
        patient.setDeceased(
            new DateTimeType(
                CommonTransformerUtils.convertToDate(beneficiary.getBeneficiaryDateOfDeath().get()),
                TemporalPrecisionEnum.DAY));
      } else {
        patient.setDeceased(new BooleanType(false));
      }

      char sex = beneficiary.getSex();
      if (sex == Sex.MALE.getCode()) {
        patient.setGender((AdministrativeGender.MALE));
        patient.addExtension(
            new Extension()
                .setValue(new CodeType().setValue(TransformerConstants.US_CORE_SEX_MALE))
                .setUrl(TransformerConstants.US_CORE_SEX_URL));
      } else if (sex == Sex.FEMALE.getCode()) {
        patient.setGender((AdministrativeGender.FEMALE));
        patient.addExtension(
            new Extension()
                .setValue(new CodeType().setValue(TransformerConstants.US_CORE_SEX_FEMALE))
                .setUrl(TransformerConstants.US_CORE_SEX_URL));
      } else {
        // US Core sex extension doesn't support "unknown"
        patient.setGender((AdministrativeGender.UNKNOWN));
      }

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
      timer.stop();
      return patient;
    }
  }

  /**
   * Adds the historical mbi data to the patient from the beneficiary data. The historical mbi data
   * is queried from the database and added to the beneficiary model in the resource provider before
   * reaching this point.
   *
   * @param patient the patient to add the historical mbi extensions to
   * @param beneficiary the beneficiary to get the historical data from
   */
  private void addHistoricalMbiExtensions(Patient patient, Beneficiary beneficiary) {
    Set<String> uniqueHistoricalMbis = new HashSet<>();
    Extension historicalIdentifier =
        TransformerUtilsV2.createIdentifierCurrencyExtension(CurrencyIdentifier.HISTORIC);
    String currentMbi = beneficiary.getMedicareBeneficiaryId().orElse("");

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
  private void addUnhashedIdentifier(
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
  private void transformMedicaidDualEligibility(Patient patient, Beneficiary beneficiary) {
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
}
