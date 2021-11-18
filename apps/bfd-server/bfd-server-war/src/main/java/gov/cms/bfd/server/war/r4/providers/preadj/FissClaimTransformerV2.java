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
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
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
          "u", Enumerations.AdministrativeGender.UNKNOWN,
          "o", Enumerations.AdministrativeGender.OTHER);

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
    claim.setDiagnosis(getDiagnosis(claimGroup));
    claim.setProcedure(getProcedure(claimGroup));

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
            //            .setType(
            //                new CodeableConcept(
            //                    new Coding(
            //                        C4BBClaimIdentifierType.UC.getSystem(),
            //                        C4BBClaimIdentifierType.UC.toCode(),
            //                        C4BBClaimIdentifierType.UC.getDisplay())))
            .setSystem("https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntrl_num/")
            .setValue(claimGroup.getDcn()));
  }

  private static CodeableConcept getType(PreAdjFissClaim claimGroup) {
    return new CodeableConcept()
        .setCoding(
            List.of(
                //                new Coding("https://dcgeo.cms.gov/resources/codesystem/rda-type",
                // "FISS", null),
                new Coding(
                    "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
                    claimGroup.getServTypeCd(),
                    null),
                new Coding(
                    "http://hl7.org/fhir/ex-claimtype", // ClaimType.INSTITUTIONAL.getSystem(),
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
                        "https://bluebutton.cms.gov/resources/codesystem/information",
                        "https://bluebutton.cms.gov/resources/variables/clm_freq_cd", // ?
                        "Claim Frequency Code")))
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
        .setGender(GENDER_MAP.get(benePayer.getBeneSex()))
        .setId("patient");
  }

  private static Resource getContainedProvider(PreAdjFissClaim claimGroup) {
    return new Organization()
        .setIdentifier(
            List.of(
                new Identifier()
                    .setType(
                        new CodeableConcept(
                            new Coding(
                                C4BBOrganizationIdentifierType.PRN.getSystem(),
                                C4BBOrganizationIdentifierType.PRN.toCode(),
                                C4BBOrganizationIdentifierType.PRN.getDisplay())))
                    .setSystem("https://bluebutton.cms.gov/rources/variables/prvdr_num")
                    .setValue(claimGroup.getMedaProvId()),
                new Identifier()
                    .setType(new CodeableConcept())
                    .setSystem("hps://bluebutton.cms.gov/resources/variables/tax_num")
                    .setValue(claimGroup.getFedTaxNumber()),
                new Identifier()
                    .setType(
                        new CodeableConcept(
                            new Coding(
                                C4BBIdentifierType.NPI.getSystem(),
                                C4BBIdentifierType.NPI.toCode(),
                                C4BBIdentifierType.NPI.getDisplay())))
                    .setSystem("http://hl7.org/fhir/sid/us-npi")
                    .setValue(claimGroup.getNpiNumber())))
        .setId("provider-org");
    //            .setMeta(
    //                new Meta()
    //                    .setProfile(
    //                        Collections.singletonList(
    //                            new CanonicalType(
    //
    // "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization"))));
  }

  private static List<Claim.DiagnosisComponent> getDiagnosis(PreAdjFissClaim claimGroup) {
    List<PreAdjFissDiagnosisCode> diagnosisCodes = new ArrayList<>(claimGroup.getDiagCodes());
    diagnosisCodes.sort(Comparator.comparing(PreAdjFissDiagnosisCode::getPriority));

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
                                          "http://hl7.org/fhir/sid/icd-10-cm",
                                          diagnosisCode.getDiagCd2(),
                                          null))))
                      .setOnAdmission(
                          new CodeableConcept(
                              new Coding(
                                  // ???
                                  )));

              if (claimGroup.getPrincipleDiag() != null
                  && claimGroup.getPrincipleDiag().equals(diagnosisCode.getDiagCd2())) {
                component.setType(
                    List.of(
                        new CodeableConcept()
                            .setCoding(
                                List.of(
                                    new Coding(
                                        ExDiagnosistype.PRINCIPAL.getSystem(),
                                        ExDiagnosistype.PRINCIPAL.toCode(),
                                        ExDiagnosistype.PRINCIPAL.getDisplay())))));
              } else if (claimGroup.getAdmitDiagCode() != null
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

  private static List<Claim.ProcedureComponent> getProcedure(PreAdjFissClaim claimGroup) {
    List<PreAdjFissProcCode> procCodes = new ArrayList<>(claimGroup.getProcCodes());
    procCodes.sort(Comparator.comparingInt(PreAdjFissProcCode::getPriority));

    return procCodes.stream()
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
                                        "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets",
                                        procCode.getProcCode(),
                                        null)))))
        .collect(Collectors.toList());
  }

  private static Date localDateToDate(LocalDate localDate) {
    return localDate == null
        ? null
        : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
