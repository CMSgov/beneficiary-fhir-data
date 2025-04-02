package gov.cms.bfd.server.war.stu3.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SEX_EXTENSION_ENABLED;
import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.Sex;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants.CurrencyIdentifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Transforms CCW {@link Beneficiary} instances into FHIR {@link Patient} resources. */
@Component
final class BeneficiaryTransformer {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  private final boolean sexExtensionEnabled;

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param sexExtensionEnabled whether to enable the sex extension
   */
  public BeneficiaryTransformer(
      MetricRegistry metricRegistry,
      @Value("${" + SSM_PATH_SEX_EXTENSION_ENABLED + ":false}") boolean sexExtensionEnabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.sexExtensionEnabled = sexExtensionEnabled;
  }

  /**
   * Transforms a {@link Beneficiary} into a {@link Patient}.
   *
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param requestHeader {@link RequestHeaders} the holder that contains all supported resource
   *     request headers
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  public Patient transform(Beneficiary beneficiary, RequestHeaders requestHeader) {
    Objects.requireNonNull(beneficiary);
    try (Timer.Context timer =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry, this.getClass().getSimpleName(), "transform")) {

      Patient patient = new Patient();

      patient.setId(String.valueOf(beneficiary.getBeneficiaryId()));
      patient.addIdentifier(
          TransformerUtils.createIdentifier(
              CcwCodebookVariable.BENE_ID, String.valueOf(beneficiary.getBeneficiaryId())));

      // Add hicn-hash identifier ONLY if raw hicn is requested.
      if (requestHeader.isHICNinIncludeIdentifiers()) {
        patient
            .addIdentifier()
            .setSystem(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH)
            .setValue(beneficiary.getHicn());
      }

      if (beneficiary.getMbiHash().isPresent()) {
        Period mbiPeriod = new Period();

        if (beneficiary.getMbiEffectiveDate().isPresent()) {
          TransformerUtils.setPeriodStart(mbiPeriod, beneficiary.getMbiEffectiveDate().get());
        }

        if (beneficiary.getMbiObsoleteDate().isPresent()) {
          TransformerUtils.setPeriodEnd(mbiPeriod, beneficiary.getMbiObsoleteDate().get());
        }

        if (mbiPeriod.hasStart() || mbiPeriod.hasEnd()) {
          patient
              .addIdentifier()
              .setSystem(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH)
              .setValue(beneficiary.getMbiHash().get())
              .setPeriod(mbiPeriod);
        } else {
          patient
              .addIdentifier()
              .setSystem(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH)
              .setValue(beneficiary.getMbiHash().get());
        }
      }

      Extension currentIdentifier =
          TransformerUtils.createIdentifierCurrencyExtension(CurrencyIdentifier.CURRENT);
      Extension historicalIdentifier =
          TransformerUtils.createIdentifierCurrencyExtension(CurrencyIdentifier.HISTORIC);
      // Add lastUpdated
      TransformerUtils.setLastUpdated(patient, beneficiary.getLastUpdated());

      if (requestHeader.isHICNinIncludeIdentifiers()) {
        Optional<String> hicnUnhashedCurrent = beneficiary.getHicnUnhashed();

        if (hicnUnhashedCurrent.isPresent()) {
          addUnhashedIdentifier(
              patient,
              hicnUnhashedCurrent.get(),
              TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED,
              currentIdentifier);
        }

        List<String> unhashedHicns = new ArrayList<>();
        for (BeneficiaryHistory beneHistory : beneficiary.getBeneficiaryHistories()) {
          Optional<String> hicnUnhashedHistoric = beneHistory.getHicnUnhashed();
          if (hicnUnhashedHistoric.isPresent()) {
            unhashedHicns.add(hicnUnhashedHistoric.get());
          }
          TransformerUtils.updateMaxLastUpdated(patient, beneHistory.getLastUpdated());
        }

        List<String> unhashedHicnsNoDupes = unhashedHicns.stream().distinct().toList();
        for (String hicn : unhashedHicnsNoDupes) {
          addUnhashedIdentifier(
              patient,
              hicn,
              TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED,
              historicalIdentifier);
        }
      }

      if (requestHeader.isMBIinIncludeIdentifiers()) {
        Optional<String> mbiUnhashedCurrent = beneficiary.getMedicareBeneficiaryId();

        if (mbiUnhashedCurrent.isPresent()) {
          addUnhashedIdentifier(
              patient,
              mbiUnhashedCurrent.get(),
              TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
              currentIdentifier);
        }
        addHistoricalMbiExtensions(patient, beneficiary, historicalIdentifier);
      }

      // support header includeAddressFields from downstream components e.g. BB2
      // per requirement of BFD-379, BB2 always send header includeAddressFields = False
      Boolean addrHdrVal =
          requestHeader.getValue(PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS);
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

      // Death Date
      if (beneficiary.getBeneficiaryDateOfDeath().isPresent()) {
        patient.setDeceased(
            new DateTimeType(
                CommonTransformerUtils.convertToDate(beneficiary.getBeneficiaryDateOfDeath().get()),
                TemporalPrecisionEnum.DAY));
      }

      char sex = beneficiary.getSex();
      if (sexExtensionEnabled) {
        String sexExtensionCode;
        if (sex == Sex.MALE.getCode()) {
          sexExtensionCode = TransformerConstants.US_CORE_SEX_MALE;
        } else if (sex == Sex.FEMALE.getCode()) {
          sexExtensionCode = TransformerConstants.US_CORE_SEX_FEMALE;
        } else {
          sexExtensionCode = TransformerConstants.US_CORE_SEX_UNKNOWN;
        }
        patient.addExtension(
            new Extension()
                .setValue(new CodeType().setValue(sexExtensionCode))
                .setUrl(TransformerConstants.US_CORE_SEX_URL));
      } else {
        if (sex == Sex.MALE.getCode()) {
          patient.setGender((AdministrativeGender.MALE));
        } else if (sex == Sex.FEMALE.getCode()) {
          patient.setGender((AdministrativeGender.FEMALE));
        } else {
          patient.setGender((AdministrativeGender.UNKNOWN));
        }
      }

      if (beneficiary.getRace().isPresent()) {
        patient.addExtension(
            TransformerUtils.createExtensionCoding(
                patient, CcwCodebookVariable.RACE, beneficiary.getRace().get()));
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
            TransformerUtils.createExtensionDate(
                CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear()));

        addMedicaidDualEligibility(patient, beneficiary);
      }

      timer.stop();
      return patient;
    }
  }

  /**
   * Adds the unhashed identifier.
   *
   * @param patient the FHIR {@link Patient} resource to add the {@link Identifier} to
   * @param value the value for {@link Identifier#getValue()}
   * @param system the value for {@link Identifier#getSystem()}
   * @param identifierCurrencyExtension the {@link Extension} to add to the {@link Identifier}
   */
  private void addUnhashedIdentifier(
      Patient patient, String value, String system, Extension identifierCurrencyExtension) {
    patient
        .addIdentifier()
        .setSystem(system)
        .setValue(value)
        .addExtension(identifierCurrencyExtension);
  }

  /**
   * Adds monthly Patient extensions for Medicare-Medicaid dual eligibility codes.
   *
   * @param patient the FHIR {@link Patient} resource to add to
   * @param beneficiary the value for {@link Beneficiary}
   */
  private void addMedicaidDualEligibility(Patient patient, Beneficiary beneficiary) {
    // Monthly Medicare-Medicaid dual eligibility codes
    if (beneficiary.getMedicaidDualEligibilityJanCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_01,
              beneficiary.getMedicaidDualEligibilityJanCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_02,
              beneficiary.getMedicaidDualEligibilityFebCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityMarCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_03,
              beneficiary.getMedicaidDualEligibilityMarCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityAprCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_04,
              beneficiary.getMedicaidDualEligibilityAprCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityMayCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_05,
              beneficiary.getMedicaidDualEligibilityMayCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityJunCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_06,
              beneficiary.getMedicaidDualEligibilityJunCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityJulCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_07,
              beneficiary.getMedicaidDualEligibilityJulCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityAugCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_08,
              beneficiary.getMedicaidDualEligibilityAugCode()));
    }
    if (beneficiary.getMedicaidDualEligibilitySeptCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_09,
              beneficiary.getMedicaidDualEligibilitySeptCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityOctCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_10,
              beneficiary.getMedicaidDualEligibilityOctCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityNovCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_11,
              beneficiary.getMedicaidDualEligibilityNovCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityDecCode().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_12,
              beneficiary.getMedicaidDualEligibilityDecCode()));
    }
  }

  /**
   * Adds the historical mbi data to the patient from the beneficiary data. The historical mbi data
   * is queried from the database and added to the beneficiary model in the resource provider before
   * reaching this point.
   *
   * @param patient the patient to add the historical mbi extensions to
   * @param beneficiary the beneficiary to get the historical data from
   * @param historicalIdentifier FHIR {@link Extension} to add identifiers to
   */
  private void addHistoricalMbiExtensions(
      Patient patient, Beneficiary beneficiary, Extension historicalIdentifier) {
    Set<String> uniqueHistoricalMbis = new HashSet<>();
    String currentMbi = beneficiary.getMedicareBeneficiaryId().orElse("");

    // Add historical MBI data found in beneficiaries_history
    for (BeneficiaryHistory mbiHistory : beneficiary.getBeneficiaryHistories()) {

      if (mbiHistory.getMedicareBeneficiaryId().isPresent()) {
        uniqueHistoricalMbis.add(mbiHistory.getMedicareBeneficiaryId().get());
      }
      TransformerUtils.updateMaxLastUpdated(patient, mbiHistory.getLastUpdated());
    }

    // Add a historical extension for each unique non-current MBI found in the history table(s)
    for (String historicalMbi : uniqueHistoricalMbis) {
      // Don't add a historical entry for any MBI which matches the current MBI
      if (!historicalMbi.equals(currentMbi)) {
        addUnhashedIdentifier(
            patient,
            historicalMbi,
            TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
            historicalIdentifier);
      }
    }
  }
}
