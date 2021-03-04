package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.RaceCategory;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.Sex;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
 * <p>In order to not create discrepancies in a beneficiary?s address by grabbing different
 * components of a beneficiary?s address from data sources, we will map the following variables from
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
 * <p>CLM_UNCOMPD_CARE_PMT_AMT EFCTV_BGN_DT EFCTV_END_DT BENE_LINK_KEY CLM_CNTL_NUM
 * FI_DOC_CLM_CNTL_NUM FI_ORIG_CLM_CNTL_NUM TAX_NUM BENE_DEATH_DT NCH_WKLY_PROC_DT REV_CNTR_DT
 * IME_OP_CLM_VAL_AMT DSH_OP_CLM_VAL_AMT CLM_HOSPC_START_DT_ID NCH_BENE_DSCHRG_DT
 *
 * <p>Note that BB2.0 API will be filtering out all derived line address fields (1-6) and CITY_NAME.
 * Please announce this to your respective customer communities as you see fit. A list of which
 * fields will map to which resource is forthcoming - we'll share that with you as we complete the
 * mapping work this upcoming sprint.
 */
final class BeneficiaryTransformerV2 {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param requestHeader {@link RequestHeaders} the holder that contains all supported resource
   *     request headers
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  @Trace
  public static Patient transform(
      MetricRegistry metricRegistry, Beneficiary beneficiary, RequestHeaders requestHeader) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(BeneficiaryTransformerV2.class.getSimpleName(), "transform"))
            .time();
    Patient patient = transform(beneficiary, requestHeader);
    timer.stop();

    return patient;
  }

  /**
   * @param beneficiary the CCW {@link Beneficiary} to transform
   * @param requestHeader {@link RequestHeaders} the holder that contains all supported resource
   *     request headers
   * @return a FHIR {@link Patient} resource that represents the specified {@link Beneficiary}
   */
  private static Patient transform(Beneficiary beneficiary, RequestHeaders requestHeader) {
    Objects.requireNonNull(beneficiary);

    Patient patient = new Patient();

    // Required values not directly mapped
    patient.getMeta().addProfile(ProfileConstants.C4BB_PATIENT_URL);
    patient.setId(beneficiary.getBeneficiaryId());
    Optional<String> mbiUnhashedCurrent = beneficiary.getMedicareBeneficiaryId();

    TransformerUtilsV2.addIdentifierSlice(
        patient,
        TransformerUtilsV2.createCodeableConcept(
            TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE, "MB"),
        mbiUnhashedCurrent,
        Optional.of(TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE));

    // Add lastUpdated
    TransformerUtilsV2.setLastUpdated(patient, beneficiary.getLastUpdated());

    // NOTE - No longer returning any HCIN value(s) in V2

    if (requestHeader.isMBIinIncludeIdentifiers()) {
      Period mbiPeriod = new Period();
      if (beneficiary.getMbiEffectiveDate().isPresent()) {
        TransformerUtilsV2.setPeriodStart(mbiPeriod, beneficiary.getMbiEffectiveDate().get());
      }
      if (beneficiary.getMbiObsoleteDate().isPresent()) {
        TransformerUtilsV2.setPeriodEnd(mbiPeriod, beneficiary.getMbiObsoleteDate().get());
      }

      addUnhashedIdentifier(
          patient,
          mbiUnhashedCurrent.get(),
          TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
          TransformerUtilsV2.createIdentifierCurrencyExtension(CurrencyIdentifier.CURRENT),
          mbiPeriod);

      Extension historicalIdentifier =
          TransformerUtilsV2.createIdentifierCurrencyExtension(CurrencyIdentifier.HISTORIC);
      List<String> unhashedMbis = new ArrayList<String>();
      for (MedicareBeneficiaryIdHistory mbiHistory :
          beneficiary.getMedicareBeneficiaryIdHistories()) {
        Optional<String> mbiUnhashedHistoric = mbiHistory.getMedicareBeneficiaryId();

        if (mbiUnhashedHistoric.isPresent()) {
          unhashedMbis.add(mbiUnhashedHistoric.get());
        }
        TransformerUtilsV2.updateMaxLastUpdated(patient, mbiHistory.getLastUpdated());
      }

      List<String> unhashedMbisNoDupes =
          unhashedMbis.stream().distinct().collect(Collectors.toList());
      for (String mbi : unhashedMbisNoDupes) {
        addUnhashedIdentifier(
            patient,
            mbi,
            TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
            historicalIdentifier,
            null);
      }
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
              CcwCodebookVariable.RFRNC_YR, beneficiary.getBeneEnrollmentReferenceYear()));
    }

    // Monthly Medicare-Medicaid dual eligibility codes
    if (beneficiary.getMedicaidDualEligibilityJanCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_01,
              beneficiary.getMedicaidDualEligibilityJanCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_02,
              beneficiary.getMedicaidDualEligibilityFebCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityMarCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_03,
              beneficiary.getMedicaidDualEligibilityMarCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityAprCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_04,
              beneficiary.getMedicaidDualEligibilityAprCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityMayCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_05,
              beneficiary.getMedicaidDualEligibilityMayCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityJunCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_06,
              beneficiary.getMedicaidDualEligibilityJunCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityJulCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_07,
              beneficiary.getMedicaidDualEligibilityJulCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityAugCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_08,
              beneficiary.getMedicaidDualEligibilityAugCode()));
    }
    if (beneficiary.getMedicaidDualEligibilitySeptCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_09,
              beneficiary.getMedicaidDualEligibilitySeptCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityOctCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_10,
              beneficiary.getMedicaidDualEligibilityOctCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityNovCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_11,
              beneficiary.getMedicaidDualEligibilityNovCode()));
    }
    if (beneficiary.getMedicaidDualEligibilityDecCode().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient,
              CcwCodebookVariable.DUAL_12,
              beneficiary.getMedicaidDualEligibilityDecCode()));
    }

    // Last Updated => Patient.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(patient, beneficiary.getLastUpdated());
    return patient;
  }

  /**
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
          .setCode("MB")
          .setDisplay("Member Number")
          .addExtension(identifierCurrencyExtension);
    } else {
      patient
          .addIdentifier()
          .setValue(value)
          .setSystem(system)
          .getType()
          .addCoding()
          .setCode("MB")
          .setDisplay("Member Number")
          .addExtension(identifierCurrencyExtension);
    }
  }

  /** Enumerates the options for the currency of an {@link Identifier}. */
  public static enum CurrencyIdentifier {
    CURRENT,
    HISTORIC;
  }
}
