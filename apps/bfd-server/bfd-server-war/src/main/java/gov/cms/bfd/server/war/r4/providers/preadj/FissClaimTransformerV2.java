package gov.cms.bfd.server.war.r4.providers.preadj;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissPayer;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.CommonCodings;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBSupportingInfoType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ExDiagnosistype;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
public class FissClaimTransformerV2 {

  private static final Map<String, Enumerations.AdministrativeGender> GENDER_MAP =
      Map.of(
          "m", Enumerations.AdministrativeGender.MALE,
          "f", Enumerations.AdministrativeGender.FEMALE,
          "u", Enumerations.AdministrativeGender.UNKNOWN);

  private static final List<String> WHITE_LISTED_POA = List.of("u", "w", "n", "y");

  private static final LocalDate ICD_9_CUTOFF_DATE = LocalDate.of(2015, 10, 1);

  private static final String MEDICARE = "MEDICARE";

  private static final String METRIC_NAME =
      MetricRegistry.name(FissClaimTransformerV2.class.getSimpleName(), "transform");

  private FissClaimTransformerV2() {}

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the FISS {@link PreAdjFissClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified claim
   */
  @Trace
  static Claim transform(MetricRegistry metricRegistry, Object claimEntity) {
    if (!(claimEntity instanceof PreAdjFissClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((PreAdjFissClaim) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link PreAdjFissClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified {@link PreAdjFissClaim}
   */
  private static Claim transformClaim(PreAdjFissClaim claimGroup) {
    Claim claim = new Claim();

    boolean isIcd9 =
        claimGroup.getStmtCovToDate() != null
            && claimGroup.getStmtCovToDate().isBefore(ICD_9_CUTOFF_DATE);

    claim.setId("f-" + claimGroup.getDcn());
    claim.setContained(List.of(getContainedPatient(claimGroup), getContainedProvider(claimGroup)));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setExtension(getExtension(claimGroup));
    claim.setStatus(Claim.ClaimStatus.ACTIVE);
    claim.setType(getType());
    claim.setSupportingInfo(getSupportingInfo(claimGroup));
    claim.setBillablePeriod(getBillablePeriod(claimGroup));
    claim.setUse(Claim.Use.CLAIM);
    claim.setPriority(getPriority());
    claim.setTotal(getTotal(claimGroup));
    claim.setProvider(new Reference("#provider-org"));
    claim.setPatient(new Reference("#patient"));
    claim.setFacility(getFacility(claimGroup));
    claim.setDiagnosis(getDiagnosis(claimGroup, isIcd9));
    claim.setProcedure(getProcedure(claimGroup, isIcd9));
    claim.setInsurance(getInsurance(claimGroup));

    FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();

    String claimContent = parser.encodeResourceToString(claim);

    String resourceHash = DigestUtils.sha1Hex(claimContent);

    claim.setMeta(
        new Meta()
            .setLastUpdated(Date.from(claimGroup.getLastUpdated()))
            .setVersionId("f-" + claimGroup.getDcn() + "-" + resourceHash));
    claim.setCreated(new Date());

    return claim;
  }

  private static List<Identifier> getIdentifier(PreAdjFissClaim claimGroup) {
    return Collections.singletonList(
        new Identifier()
            .setType(
                new CodeableConcept(
                    new Coding(
                        C4BBClaimIdentifierType.UC.getSystem(),
                        C4BBClaimIdentifierType.UC.toCode(),
                        C4BBClaimIdentifierType.UC.getDisplay())))
            .setSystem(BBCodingSystems.FI_DOC_CLM_CONTROL_NUM)
            .setValue(claimGroup.getDcn()));
  }

  private static List<Extension> getExtension(PreAdjFissClaim claimGroup) {
    return claimGroup.getServTypeCd() == null
        ? null
        : List.of(
            new Extension(BBCodingSystems.CLM_SERVICE_CLSFCTN_TYPE_CODE)
                .setValue(
                    new Coding(
                        BBCodingSystems.CLM_SERVICE_CLSFCTN_TYPE_CODE,
                        claimGroup.getServTypeCd(),
                        null)));
  }

  private static CodeableConcept getType() {
    return new CodeableConcept()
        .setCoding(
            List.of(
                new Coding(
                    ClaimType.INSTITUTIONAL.getSystem(),
                    ClaimType.INSTITUTIONAL.toCode(),
                    ClaimType.INSTITUTIONAL.getDisplay())));
  }

  private static List<Claim.SupportingInformationComponent> getSupportingInfo(
      PreAdjFissClaim claimGroup) {
    return claimGroup.getFreqCd() == null
        ? null
        : List.of(
            new Claim.SupportingInformationComponent()
                .setCategory(
                    new CodeableConcept(
                        new Coding(
                            C4BBSupportingInfoType.TYPE_OF_BILL.getSystem(),
                            C4BBSupportingInfoType.TYPE_OF_BILL.toCode(),
                            C4BBSupportingInfoType.TYPE_OF_BILL.getDisplay())))
                .setCode(
                    new CodeableConcept(
                        new Coding(BBCodingSystems.CLM_FREQ_CODE, claimGroup.getFreqCd(), null)))
                .setSequence(1));
  }

  private static Period getBillablePeriod(PreAdjFissClaim claimGroup) {
    return new Period()
        .setStart(localDateToDate(claimGroup.getStmtCovToDate()))
        .setEnd(localDateToDate(claimGroup.getStmtCovFromDate()));
  }

  private static CodeableConcept getPriority() {
    return new CodeableConcept(
        new Coding(
            ProcessPriority.NORMAL.getSystem(),
            ProcessPriority.NORMAL.toCode(),
            ProcessPriority.NORMAL.getDisplay()));
  }

  private static Money getTotal(PreAdjFissClaim claimGroup) {
    Money total;

    if (claimGroup.getTotalChargeAmount() != null) {
      total = new Money();

      total.setValue(claimGroup.getTotalChargeAmount());
      total.setCurrency("USD");
    } else {
      total = null;
    }

    return total;
  }

  private static Resource getContainedPatient(PreAdjFissClaim claimGroup) {
    Optional<PreAdjFissPayer> optional =
        claimGroup.getPayers().stream()
            .filter(p -> p.getPayerType() == PreAdjFissPayer.PayerType.BeneZ)
            .findFirst();

    Patient patient =
        new Patient()
            .setIdentifier(
                List.of(
                    new Identifier()
                        .setType(
                            new CodeableConcept(
                                new Coding(
                                    CommonCodings.MC.getSystem(),
                                    CommonCodings.MC.getCode(),
                                    CommonCodings.MC.getDisplay())))
                        .setSystem(
                            TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)
                        .setValue(claimGroup.getMbi())));
    patient.setId("patient");

    if (optional.isPresent()) {
      PreAdjFissPayer benePayer = optional.get();
      patient
          .setName(getBeneName(benePayer))
          .setBirthDate(localDateToDate(benePayer.getBeneDob()))
          .setGender(
              benePayer.getBeneSex() == null
                  ? null
                  : GENDER_MAP.get(benePayer.getBeneSex().toLowerCase()));
    }

    return patient;
  }

  private static List<HumanName> getBeneName(PreAdjFissPayer benePayer) {
    HumanName name = new HumanName().setFamily(benePayer.getBeneLastName());

    if (benePayer.getBeneFirstName() != null || benePayer.getBeneMidInit() != null) {
      name.setGiven(
          List.of(
              new StringType(benePayer.getBeneFirstName()),
              new StringType(
                  benePayer.getBeneMidInit() == null ? null : benePayer.getBeneMidInit() + ".")));
    }

    return List.of(name);
  }

  private static Resource getContainedProvider(PreAdjFissClaim claimGroup) {
    Organization organization = new Organization();

    if (claimGroup.getMedaProvId() != null) {
      organization
          .getIdentifier()
          .add(
              new Identifier()
                  .setType(
                      new CodeableConcept(
                          new Coding(
                              C4BBOrganizationIdentifierType.PRN.getSystem(),
                              C4BBOrganizationIdentifierType.PRN.toCode(),
                              C4BBOrganizationIdentifierType.PRN.getDisplay())))
                  .setSystem(BBCodingSystems.PROVIDER_NUM)
                  .setValue(claimGroup.getMedaProvId()));
    }

    if (claimGroup.getFedTaxNumber() != null) {
      organization
          .getIdentifier()
          .add(
              new Identifier()
                  .setType(
                      new CodeableConcept(
                          new Coding(
                              C4BBOrganizationIdentifierType.TAX.getSystem(),
                              C4BBOrganizationIdentifierType.TAX.toCode(),
                              C4BBOrganizationIdentifierType.TAX.getDisplay())))
                  .setSystem(BBCodingSystems.TAX_NUM)
                  .setValue(claimGroup.getFedTaxNumber()));
    }

    if (claimGroup.getNpiNumber() != null) {
      organization
          .getIdentifier()
          .add(
              new Identifier()
                  .setType(
                      new CodeableConcept(
                          new Coding(
                              C4BBIdentifierType.NPI.getSystem(),
                              C4BBIdentifierType.NPI.toCode(),
                              C4BBIdentifierType.NPI.getDisplay())))
                  .setSystem(TransformerConstants.CODING_NPI_US)
                  .setValue(claimGroup.getNpiNumber()));
    }

    organization.setId("provider-org");

    return organization;
  }

  private static Reference getFacility(PreAdjFissClaim claimGroup) {
    if (claimGroup.getLobCd() != null || claimGroup.getNpiNumber() != null) {
      Reference reference = new Reference();

      if (claimGroup.getNpiNumber() != null) {
        reference.setIdentifier(
            new Identifier()
                .setType(
                    new CodeableConcept(
                        new Coding(
                            C4BBIdentifierType.NPI.getSystem(),
                            C4BBIdentifierType.NPI.toCode(),
                            C4BBIdentifierType.NPI.getDisplay())))
                .setSystem(TransformerConstants.CODING_NPI_US)
                .setValue(claimGroup.getNpiNumber()));
      }

      if (claimGroup.getLobCd() != null) {
        reference.setExtension(
            List.of(
                new Extension(BBCodingSystems.CLM_FACILITY_TYPE_CODE)
                    .setValue(
                        new Coding(
                            BBCodingSystems.CLM_FACILITY_TYPE_CODE, claimGroup.getLobCd(), null))));
      }

      return reference;
    }

    return null;
  }

  private static List<Claim.DiagnosisComponent> getDiagnosis(
      PreAdjFissClaim claimGroup, boolean isIcd9) {
    final String systemSuffix = isIcd9 ? "9-cm" : "10";

    return ObjectUtils.defaultIfNull(claimGroup.getDiagCodes(), List.<PreAdjFissDiagnosisCode>of())
        .stream()
        .map(
            diagnosisCode -> {
              Claim.DiagnosisComponent component =
                  new Claim.DiagnosisComponent()
                      .setSequence(diagnosisCode.getPriority() + 1)
                      .setDiagnosis(
                          new CodeableConcept()
                              .setCoding(
                                  Collections.singletonList(
                                      new Coding(
                                          "http://hl7.org/fhir/sid/icd-" + systemSuffix,
                                          diagnosisCode.getDiagCd2(),
                                          null))));

              if (diagnosisCode.getDiagPoaInd() != null // Present on Admission
                  && WHITE_LISTED_POA.contains(diagnosisCode.getDiagPoaInd().toLowerCase())) {
                component.setOnAdmission(
                    new CodeableConcept(
                        // If any other value, omit
                        new Coding(
                            "http://terminology.hl7.org/CodeSystem/ex-diagnosis-on-admission",
                            diagnosisCode.getDiagPoaInd().toLowerCase(Locale.ROOT),
                            null)));
              }

              if (claimGroup.getAdmitDiagCode() != null
                  && codesAreEqual(claimGroup.getAdmitDiagCode(), diagnosisCode.getDiagCd2())) {
                component.setType(
                    List.of(
                        new CodeableConcept()
                            .setCoding(
                                List.of(
                                    new Coding(
                                        ExDiagnosistype.ADMITTING.getSystem(),
                                        ExDiagnosistype.ADMITTING.toCode(),
                                        ExDiagnosistype.ADMITTING.getDisplay())))));
              }

              return component;
            })
        .sorted(Comparator.comparing(Claim.DiagnosisComponent::getSequence))
        .collect(Collectors.toList());
  }

  private static List<Claim.ProcedureComponent> getProcedure(
      PreAdjFissClaim claimGroup, boolean isIcd9) {
    final String systemSuffix = isIcd9 ? "9-cm" : "10";

    return ObjectUtils.defaultIfNull(claimGroup.getProcCodes(), List.<PreAdjFissProcCode>of())
        .stream()
        .map(
            procCode ->
                new Claim.ProcedureComponent()
                    .setSequence((procCode.getPriority() + 1))
                    .setDate(
                        procCode.getProcDate() == null
                            ? null
                            : localDateToDate(procCode.getProcDate()))
                    .setProcedure(
                        new CodeableConcept()
                            .setCoding(
                                List.of(
                                    new Coding(
                                        "http://hl7.org/fhir/sid/icd-" + systemSuffix,
                                        procCode.getProcCode(),
                                        null)))))
        .sorted(Comparator.comparing(Claim.ProcedureComponent::getSequence))
        .collect(Collectors.toList());
  }

  private static List<Claim.InsuranceComponent> getInsurance(PreAdjFissClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getPayers(), List.<PreAdjFissPayer>of()).stream()
        .map(
            payer -> {
              Claim.InsuranceComponent component =
                  new Claim.InsuranceComponent()
                      .setSequence(payer.getPriority() + 1)
                      .setFocal(Objects.equals(payer.getPayersName(), MEDICARE));

              if (payer.getPayersName() != null) {
                component.setExtension(
                    List.of(
                        new Extension(
                            BBCodingSystems.FISS_PAYERS_NAME,
                            new StringType(payer.getPayersName()))));
              }

              return component;
            })
        .sorted(Comparator.comparing(Claim.InsuranceComponent::getSequence))
        .collect(Collectors.toList());
  }

  private static Date localDateToDate(LocalDate localDate) {
    return localDate == null
        ? null
        : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private static String normalizeIcdCode(String code) {
    return code.trim().replace(".", "").toUpperCase();
  }

  private static boolean codesAreEqual(String code1, String code2) {
    return code1 != null
        && code2 != null
        && normalizeIcdCode(code1).equals(normalizeIcdCode(code2));
  }
}
