package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.Sex;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
    patient
        .getMeta()
        .addProfile("http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Patient");

    patient.setId(beneficiary.getBeneficiaryId());

    TransformerUtilsV2.addIdentifierSlice(
        patient,
        TransformerUtilsV2.createCodeableConcept(
            TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE, "MB"),
        Optional.of(beneficiary.getBeneficiaryId()),
        Optional.of(TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE));

    // Add hicn-hash identifier ONLY if raw hicn is requested.
    // TODO - check if this is MC or is HICN something else
    if (requestHeader.isHICNinIncludeIdentifiers()) {
      Optional<String> hicnUnhashedCurrent = beneficiary.getHicnUnhashed();

      if (hicnUnhashedCurrent.isPresent()) {
        /*
        addUnhashedIdentifier(
            patient,
            hicnUnhashedCurrent.get(),
            TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
            TransformerUtilsV2.createIdentifierCurrencyExtension(CurrencyIdentifier.CURRENT));
        */
        TransformerUtilsV2.addIdentifierSlice(
            patient,
            TransformerUtilsV2.createCodeableConcept(
                TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE, "MC"),
            hicnUnhashedCurrent,
            Optional.of(TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE));
      }
    }

    if (beneficiary.getMbiHash().isPresent()) {
      Period mbiPeriod = new Period();

      if (beneficiary.getMbiEffectiveDate().isPresent()) {
        TransformerUtilsV2.setPeriodStart(mbiPeriod, beneficiary.getMbiEffectiveDate().get());
      }

      if (beneficiary.getMbiObsoleteDate().isPresent()) {
        TransformerUtilsV2.setPeriodEnd(mbiPeriod, beneficiary.getMbiObsoleteDate().get());
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
        TransformerUtilsV2.createIdentifierCurrencyExtension(CurrencyIdentifier.CURRENT);
    Extension historicalIdentifier =
        TransformerUtilsV2.createIdentifierCurrencyExtension(CurrencyIdentifier.HISTORIC);
    // Add lastUpdated
    TransformerUtilsV2.setLastUpdated(patient, beneficiary.getLastUpdated());

    if (requestHeader.isHICNinIncludeIdentifiers()) {
      Optional<String> hicnUnhashedCurrent = beneficiary.getHicnUnhashed();

      if (hicnUnhashedCurrent.isPresent()) {
        addUnhashedIdentifier(
            patient,
            hicnUnhashedCurrent.get(),
            TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED,
            currentIdentifier);
      }

      List<String> unhashedHicns = new ArrayList<String>();
      for (BeneficiaryHistory beneHistory : beneficiary.getBeneficiaryHistories()) {
        Optional<String> hicnUnhashedHistoric = beneHistory.getHicnUnhashed();
        if (hicnUnhashedHistoric.isPresent()) {
          unhashedHicns.add(hicnUnhashedHistoric.get());
        }
        TransformerUtilsV2.updateMaxLastUpdated(patient, beneHistory.getLastUpdated());
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

    if (requestHeader.isMBIinIncludeIdentifiers()) {
      Optional<String> mbiUnhashedCurrent = beneficiary.getMedicareBeneficiaryId();

      if (mbiUnhashedCurrent.isPresent()) {
        addUnhashedIdentifier(
            patient,
            mbiUnhashedCurrent.get(),
            TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
            currentIdentifier);
      }

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
            TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
            historicalIdentifier);
      }
    }

    patient.setActive(!beneficiary.getBeneficiaryDateOfDeath().isPresent());

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

    // Death Date
    if (beneficiary.getBeneficiaryDateOfDeath().isPresent()) {
      patient.setDeceased(
          new DateTimeType(
              TransformerUtilsV2.convertToDate(beneficiary.getBeneficiaryDateOfDeath().get()),
              TemporalPrecisionEnum.DAY));
    }

    char sex = beneficiary.getSex();
    if (sex == Sex.MALE.getCode()) patient.setGender((AdministrativeGender.MALE));
    else if (sex == Sex.FEMALE.getCode()) patient.setGender((AdministrativeGender.FEMALE));
    else patient.setGender((AdministrativeGender.UNKNOWN));

    if (beneficiary.getRace().isPresent()) {
      patient.addExtension(
          TransformerUtilsV2.createExtensionCoding(
              patient, CcwCodebookVariable.RACE, beneficiary.getRace().get()));

      String ombCode = TransformerConstants.HL7_RACE_UNKNOWN_CODE;
      String ombDisplay = TransformerConstants.HL7_RACE_UNKNOWN_DISPLAY;

      Extension parentOMBRace = new Extension();
      Extension raceChildOMBExt1 = new Extension();

      raceChildOMBExt1
          .setValue(
              new Coding()
                  .setCode(ombCode)
                  .setSystem(TransformerConstants.CODING_V3_NULL)
                  .setDisplay(ombDisplay))
          .setUrl("ombCategory");

      Extension raceChildOMBExt2 = new Extension();
      raceChildOMBExt2.setValue(new StringType().setValue(ombDisplay)).setUrl("text");

      parentOMBRace.setUrl(TransformerConstants.CODING_RACE_US);
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
   */
  private static void addUnhashedIdentifier(
      Patient patient, String value, String system, Extension identifierCurrencyExtension) {

    patient
        .addIdentifier()
        .setValue(value)
        .setSystem(system)
        .getType()
        .addCoding()
        .setCode("MC")
        .setSystem(system)
        .setDisplay("Patient's Medicare Number")
        .addExtension(identifierCurrencyExtension);
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

    if (mbiPeriod != null) {

      patient
          .addIdentifier()
          .setValue(value)
          .setSystem(system)
          .setPeriod(mbiPeriod)
          .getType()
          .addCoding()
          .setCode("MC")
          .setSystem(TransformerConstants.CARIN_IDENTIFIER_SYSTEM)
          .setDisplay("Patient's Medicare Number")
          .addExtension(identifierCurrencyExtension);
    } else {
      patient
          .addIdentifier()
          .setValue(value)
          .setSystem(system)
          .getType()
          .addCoding()
          .setCode("MC")
          .setSystem(TransformerConstants.CARIN_IDENTIFIER_SYSTEM)
          .setDisplay("Patient's Medicare Number")
          .addExtension(identifierCurrencyExtension);
    }
  }

  /** Enumerates the options for the currency of an {@link Identifier}. */
  public static enum CurrencyIdentifier {
    CURRENT,
    HISTORIC;
  }
}
