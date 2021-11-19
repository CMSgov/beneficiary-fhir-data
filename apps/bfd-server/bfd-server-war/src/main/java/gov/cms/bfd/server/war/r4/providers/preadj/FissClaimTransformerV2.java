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
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimIdentifierType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    boolean isIcd9 = claimGroup.getStmtCovToDate().isBefore(ICD_9_CUTOFF_DATE);

    claim.setId("f-" + claimGroup.getDcn());
    claim.setContained(List.of(getContainedPatient(claimGroup), getContainedProvider(claimGroup)));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setStatus(Claim.ClaimStatus.ACTIVE);
    claim.setType(getType(claimGroup));
    claim.setSupportingInfo(getSupportingInfo(claimGroup));
    claim.setBillablePeriod(getBillablePeriod(claimGroup));
    claim.setUse(Claim.Use.CLAIM);
    claim.setPriority(getPriority());
    claim.setTotal(getTotal(claimGroup));
    claim.setProvider(new Reference("#provider-org"));
    claim.setPatient(new Reference("#patient"));
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
            .setSystem("https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntrl_num/")
            .setValue(claimGroup.getDcn()));
  }

  private static CodeableConcept getType(PreAdjFissClaim claimGroup) {
    return new CodeableConcept()
        .setCoding(
            List.of(
                new Coding(
                    "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
                    claimGroup.getServTypeCd(),
                    null),
                new Coding(
                    ClaimType.INSTITUTIONAL.getSystem(),
                    ClaimType.INSTITUTIONAL.toCode(),
                    ClaimType.INSTITUTIONAL.getDisplay())));
  }

  private static List<Claim.SupportingInformationComponent> getSupportingInfo(
      PreAdjFissClaim claimGroup) {
    return List.of(
        new Claim.SupportingInformationComponent()
            .setCategory(
                new CodeableConcept(
                    new Coding(
                        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                        "typeofbill",
                        "Type of Bill")))
            .setCode(
                new CodeableConcept(
                    new Coding(
                        "https://bluebutton.cms.gov/resources/variables/clm_freq_cd",
                        claimGroup.getFreqCd(),
                        null)))
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
    return new Money().setValue(claimGroup.getTotalChargeAmount()).setCurrency("USD");
  }

  private static Resource getContainedPatient(PreAdjFissClaim claimGroup) {
    PreAdjFissPayer benePayer =
        claimGroup.getPayers().stream()
            .filter(p -> p.getPayerType() == PreAdjFissPayer.PayerType.BeneZ)
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("No BeneZ payer found, this should not happen"));

    return new Patient()
        .setIdentifier(
            List.of(
                new Identifier()
                    .setType(
                        new CodeableConcept(
                            new Coding(
                                TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE,
                                "MC",
                                "Patient's Medicare Number")))
                    .setSystem("http://hl7.org/fhir/sid/us-mbi")
                    .setValue(claimGroup.getMbi())))
        .setName(
            List.of(
                new HumanName()
                    .setFamily(benePayer.getBeneLastName())
                    .setGiven(
                        List.of(
                            new StringType(benePayer.getBeneFirstName()),
                            new StringType(benePayer.getBeneMidInit())))))
        .setBirthDate(localDateToDate(benePayer.getBeneDob()))
        .setGender(
            benePayer.getBeneSex() == null
                ? null
                : GENDER_MAP.get(benePayer.getBeneSex().toLowerCase()))
        .setId("patient");
  }

  private static Resource getContainedProvider(PreAdjFissClaim claimGroup) {
    return new Organization()
        .setIdentifier(
            List.of(
                new Identifier()
                    .setSystem("https://bluebutton.cms.gov/resources/variables/prvdr_num")
                    .setValue(claimGroup.getMedaProvId()),
                new Identifier()
                    .setSystem("hps://bluebutton.cms.gov/resources/variables/tax_num")
                    .setValue(claimGroup.getFedTaxNumber()),
                new Identifier()
                    .setSystem("http://hl7.org/fhir/sid/us-npi")
                    .setValue(claimGroup.getNpiNumber())))
        .setId("provider-org");
  }

  private static List<Claim.DiagnosisComponent> getDiagnosis(
      PreAdjFissClaim claimGroup, boolean isIcd9) {
    List<PreAdjFissDiagnosisCode> diagnosisCodes = new ArrayList<>(claimGroup.getDiagCodes());
    diagnosisCodes.sort(Comparator.comparing(PreAdjFissDiagnosisCode::getPriority));

    final String systemSuffix = isIcd9 ? "9-cm" : "10";

    return diagnosisCodes.stream()
        .map(
            diagnosisCode -> {
              Claim.DiagnosisComponent component =
                  new Claim.DiagnosisComponent()
                      .setSequence(diagnosisCode.getPriority())
                      .setDiagnosis(
                          new CodeableConcept()
                              .setCoding(
                                  Collections.singletonList(
                                      new Coding(
                                          "http://hl7.org/fhir/sid/icd-" + systemSuffix,
                                          diagnosisCode.getDiagCd2(),
                                          null))));

              if (diagnosisCode.getDiagPoaInd() != null
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
                  && claimGroup.getAdmitDiagCode().equals(diagnosisCode.getDiagCd2())) {
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
        .collect(Collectors.toList());
  }

  private static List<Claim.ProcedureComponent> getProcedure(
      PreAdjFissClaim claimGroup, boolean isIcd9) {
    List<PreAdjFissProcCode> procCodes =
        new ArrayList<>(ObjectUtils.defaultIfNull(claimGroup.getProcCodes(), List.of()));
    procCodes.sort(Comparator.comparingInt(PreAdjFissProcCode::getPriority));

    final String systemSuffix = isIcd9 ? "9-cm" : "10";

    return procCodes.stream()
        .map(
            procCode ->
                new Claim.ProcedureComponent()
                    .setSequence((procCode.getPriority()))
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
        .collect(Collectors.toList());
  }

  private static List<Claim.InsuranceComponent> getInsurance(PreAdjFissClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getPayers(), List.<PreAdjFissPayer>of()).stream()
        .map(
            payer -> {
              Claim.InsuranceComponent component =
                  new Claim.InsuranceComponent()
                      .setSequence(payer.getPriority())
                      .setFocal(payer.getPayersName().equals(MEDICARE));

              component.setExtension(
                  List.of(
                      new Extension(
                          "https://bluebutton.cms.gov/resources/variables/fiss/payers-name/",
                          new StringType(payer.getPayersName()))));

              return component;
            })
        .collect(Collectors.toList());
  }

  private static Date localDateToDate(LocalDate localDate) {
    return localDate == null
        ? null
        : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
