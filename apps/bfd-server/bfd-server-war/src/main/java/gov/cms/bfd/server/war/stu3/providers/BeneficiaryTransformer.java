package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.server.war.commons.Sex;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;

/** Transforms CCW {@link Beneficiary} instances into FHIR {@link Patient} resources. */
/**
 * The BFD is in the process of mapping new data fields to the BFD API. We wanted to let you know of
 * a few changes that pertain to all APIs. Currently, the following variables are sent from CCW
 * through the FHIR export and available to all APIs in the patient resource. The source for these
 * is EDB:
 *
 * <p>STATE_CODE BENE_CNTY_CD BENE_ZIP_CD
 *
 * <p>We'll be adding the following fields be added to the FHIR export, which come from the CME
 * Derived Mailing Source:
 *
 * <p>DRVD_LINE_1_ADR DRVD_LINE_2_ADR DRVD_LINE_3_ADR DRVD_LINE_4_ADR DRVD_LINE_5_ADR
 * DRVD_LINE_6_ADR CITY_NAME STATE_CD STATE_CNTY_ZIP_CD
 *
 * <p>In order to not create discrepancies in a beneficiary�s address by grabbing different
 * components of a beneficiary�s address from data sources, we will map the following variables from
 * the CME Derived Mailing source:
 *
 * <p>STATE_CD STATE_CNTY_ZIP_CD
 *
 * <p>and stop mapping the following fields from EDB:
 *
 * <p>STATE_CODE BENE_CNTY_CD BENE_ZIP_CD
 *
 * <p>This will result in all address-related fields coming from a single source. It also means
 * that, if your API was sending along this field previously, they will no longer receive
 * BENE_CNTY_CD in the payload from the BFD API. In addition, we will also be adding the following
 * fields to the FHIR export - and your customers will receive for the first time - the following
 * fields:
 *
 * <p>CLM_UNCOMPD_CARE_PMT_AMT EFCTV_BGN_DT EFCTV_END_DT CLM_CNTL_NUM FI_DOC_CLM_CNTL_NUM
 * FI_ORIG_CLM_CNTL_NUM BENE_DEATH_DT NCH_WKLY_PROC_DT REV_CNTR_DT IME_OP_CLM_VAL_AMT
 * DSH_OP_CLM_VAL_AMT CLM_HOSPC_START_DT_ID NCH_BENE_DSCHRG_DT
 *
 * <p>Note that BB2.0 API will be filtering out all derived line address fields (1-6) and CITY_NAME.
 * Please announce this to your respective customer communities as you see fit. A list of which
 * fields will map to which resource is forthcoming - we'll share that with you as we complete the
 * mapping work this upcoming sprint.
 */
final class BeneficiaryTransformer {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param includeIdentifiersValues the includeIdentifiers header values to use
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  @Trace
  public static Patient transform(
      MetricRegistry metricRegistry,
      Beneficiary beneficiary,
      List<String> includeIdentifiersValues) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(BeneficiaryTransformer.class.getSimpleName(), "transform"))
            .time();
    Patient patient = transform(beneficiary, includeIdentifiersValues);
    timer.stop();

    return patient;
  }

  /**
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param includeIdentifiersValues the includeIdentifiers header values to use
   * @param includeAddressFields the includeAddressFields flag derived from header - used to
   *     determine if derived address info be included or not
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  private static Patient transform(Beneficiary beneficiary, List<String> includeIdentifiersValues) {
    Objects.requireNonNull(beneficiary);

    Patient patient = new Patient();

    patient.setId(beneficiary.getBeneficiaryId());
    patient.addIdentifier(
        TransformerUtils.createIdentifier(
            CcwCodebookVariable.BENE_ID, beneficiary.getBeneficiaryId()));

    // Add hicn-hash identifier ONLY if raw hicn is requested.
    if (PatientResourceProvider.hasHICN(includeIdentifiersValues)) {
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

    if (PatientResourceProvider.hasHICN(includeIdentifiersValues)) {
      Optional<String> hicnUnhashedCurrent = beneficiary.getHicnUnhashed();

      if (hicnUnhashedCurrent.isPresent())
        addUnhashedIdentifier(
            patient,
            hicnUnhashedCurrent.get(),
            TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED,
            currentIdentifier);

      List<String> unhashedHicns = new ArrayList<String>();
      for (BeneficiaryHistory beneHistory : beneficiary.getBeneficiaryHistories()) {
        Optional<String> hicnUnhashedHistoric = beneHistory.getHicnUnhashed();
        if (hicnUnhashedHistoric.isPresent()) unhashedHicns.add(hicnUnhashedHistoric.get());
        TransformerUtils.updateMaxLastUpdated(patient, beneHistory.getLastUpdated());
      }

      List<String> unhashedHicnsNoDupes =
          unhashedHicns.stream().distinct().collect(Collectors.toList());
      for (String hicn : unhashedHicnsNoDupes) {
        addUnhashedIdentifier(
            patient,
            hicn,
            TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED,
            historicalIdentifier);
      }
    }

    if (PatientResourceProvider.hasMBI(includeIdentifiersValues)) {
      Optional<String> mbiUnhashedCurrent = beneficiary.getMedicareBeneficiaryId();

      if (mbiUnhashedCurrent.isPresent())
        addUnhashedIdentifier(
            patient,
            mbiUnhashedCurrent.get(),
            TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
            currentIdentifier);

      List<String> unhashedMbis = new ArrayList<String>();
      for (MedicareBeneficiaryIdHistory mbiHistory :
          beneficiary.getMedicareBeneficiaryIdHistories()) {
        Optional<String> mbiUnhashedHistoric = mbiHistory.getMedicareBeneficiaryId();
        if (mbiUnhashedHistoric.isPresent()) unhashedMbis.add(mbiUnhashedHistoric.get());
        TransformerUtils.updateMaxLastUpdated(patient, mbiHistory.getLastUpdated());
      }

      List<String> unhashedMbisNoDupes =
          unhashedMbis.stream().distinct().collect(Collectors.toList());
      for (String mbi : unhashedMbisNoDupes) {
        addUnhashedIdentifier(
            patient,
            mbi,
            TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
            historicalIdentifier);
      }
    }

    patient
        .addAddress()
        .setState(beneficiary.getStateCode())
        .setDistrict(beneficiary.getCountyCode())
        .setPostalCode(beneficiary.getPostalCode());

    if (beneficiary.getBirthDate() != null) {
      patient.setBirthDate(TransformerUtils.convertToDate(beneficiary.getBirthDate()));
    }

    // Death Date
    if (beneficiary.getBeneficiaryDateOfDeath().isPresent()) {
      patient.setDeceased(
          new DateTimeType(
              TransformerUtils.convertToDate(beneficiary.getBeneficiaryDateOfDeath().get()),
              TemporalPrecisionEnum.DAY));
    }

    char sex = beneficiary.getSex();
    if (sex == Sex.MALE.getCode()) patient.setGender((AdministrativeGender.MALE));
    else if (sex == Sex.FEMALE.getCode()) patient.setGender((AdministrativeGender.FEMALE));
    else patient.setGender((AdministrativeGender.UNKNOWN));

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
    if (beneficiary.getNameMiddleInitial().isPresent())
      name.addGiven(String.valueOf(beneficiary.getNameMiddleInitial().get()));

    // The reference year of the enrollment data
    if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
      patient.addExtension(
          TransformerUtils.createExtensionDate(
              CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear()));
    }

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

    return patient;
  }

  /**
   * @param patient the FHIR {@link Patient} resource to add the {@link Identifier} to
   * @param value the value for {@link Identifier#getValue()}
   * @param system the value for {@link Identifier#getSystem()}
   * @param identifierCurrencyExtension the {@link Extension} to add to the {@link Identifier}
   */
  private static void addUnhashedIdentifier(
      Patient patient, String value, String system, Extension identifierCurrencyExtension) {
    patient
        .addIdentifier()
        .setSystem(system)
        .setValue(value)
        .addExtension(identifierCurrencyExtension);
  }

  /** Enumerates the options for the currency of an {@link Identifier}. */
  public static enum CurrencyIdentifier {
    CURRENT,

    HISTORIC;
  }
}
