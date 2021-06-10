package gov.cms.bfd.server.war.r4.providers.preadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
public class FissClaimTransformerV2 {

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
    claim.setMeta(new Meta().setLastUpdated(Date.from(claimGroup.getLastUpdated())));
    claim.setContained(getContainedProvider(claimGroup));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setStatus(Claim.ClaimStatus.ACTIVE);
    claim.setType(getType());
    claim.setUse(Claim.Use.CLAIM);
    claim.setCreated(new Date());
    claim.setPriority(getPriority());
    claim.setTotal(getTotal(claimGroup));
    claim.setProvider(new Reference("#provider-org"));
    claim.setPatient(getPatient(claimGroup));
    claim.setDiagnosis(getDiagnosis(claimGroup));
    claim.setProcedure(getProcedure(claimGroup));

    return claim;
  }

  private static List<Identifier> getIdentifier(PreAdjFissClaim claimGroup) {
    return Collections.singletonList(
        new Identifier()
            .setType(
                new CodeableConcept(
                    new Coding(
                        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
                        "uc",
                        "Unique Claim ID")))
            .setSystem("https://dcgeo.cms.gov/resources/variables/dcn")
            .setValue(claimGroup.getDcn()));
  }

  private static CodeableConcept getType() {
    return new CodeableConcept()
        .setCoding(
            Arrays.asList(
                new Coding("https://dcgeo.cms.gov/resources/codesystem/rda-type", "FISS", null),
                new Coding(
                    "http://terminology.hl7.org/CodeSystem/claim-type",
                    "institutional",
                    "Institutional")));
  }

  private static CodeableConcept getPriority() {
    return new CodeableConcept(
        new Coding("http://hl7.org/fhir/ValueSet/process-priority", "normal", "Normal"));
  }

  private static Money getTotal(PreAdjFissClaim claimGroup) {
    Money total = new Money();

    total.setValue(claimGroup.getTotalChargeAmount());
    total.setCurrency("USD");

    return total;
  }

  private static List<Resource> getContainedProvider(PreAdjFissClaim claimGroup) {
    return Collections.singletonList(
        new Organization()
            .setIdentifier(
                Arrays.asList(
                    new Identifier()
                        .setType(
                            new CodeableConcept(
                                new Coding(
                                    "http://terminology.hl7.org/CodeSystem/v2-0203",
                                    "PRN",
                                    "Provider Number")))
                        .setValue(claimGroup.getMedaProvId()),
                    new Identifier()
                        .setType(
                            new CodeableConcept(
                                new Coding(
                                    "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
                                    "npi",
                                    "National Provider Identifier")))
                        .setSystem("http://hl7.org/fhir/sid/us-npi")
                        .setValue(claimGroup.getNpiNumber())))
            .setId("provider-org")
            .setMeta(
                new Meta()
                    .setProfile(
                        Collections.singletonList(
                            new CanonicalType(
                                "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization")))));
  }

  private static Reference getPatient(PreAdjFissClaim claimGroup) {
    return new Reference()
        .setIdentifier(
            new Identifier()
                .setType(
                    new CodeableConcept()
                        .setCoding(
                            Collections.singletonList(
                                new Coding(
                                    "http://terminology.hl7.org/CodeSystem/v2-0203",
                                    "MC",
                                    "Patient's Medicare number"))))
                .setSystem("http://hl7.org/fhir/sid/us-mbi")
                .setValue(claimGroup.getMbi()));
  }

  private static List<Claim.DiagnosisComponent> getDiagnosis(PreAdjFissClaim claimGroup) {
    return Arrays.asList(
        new Claim.DiagnosisComponent()
            .setSequence(1)
            .setDiagnosis(
                new CodeableConcept()
                    .setCoding(
                        Collections.singletonList(
                            new Coding(
                                "http://hl7.org/fhir/sid/icd-10",
                                claimGroup.getPrincipleDiag(),
                                null))))
            .setType(
                Collections.singletonList(
                    new CodeableConcept()
                        .setCoding(
                            Collections.singletonList(
                                new Coding(
                                    "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                                    "principal",
                                    "Principal Diagnosis"))))),
        new Claim.DiagnosisComponent()
            .setSequence(2)
            .setDiagnosis(
                new CodeableConcept()
                    .setCoding(
                        Collections.singletonList(
                            new Coding(
                                "http://hl7.org/fhir/sid/icd-10",
                                claimGroup.getAdmitDiagCode(),
                                null))))
            .setType(
                Collections.singletonList(
                    new CodeableConcept()
                        .setCoding(
                            Collections.singletonList(
                                new Coding(
                                    "http://terminology.hl7.org/CodeSystem/ex-diagnosistype",
                                    "admitting",
                                    "Admitting Diagnosis"))))));
  }

  private static List<Claim.ProcedureComponent> getProcedure(PreAdjFissClaim claimGroup) {
    List<Claim.ProcedureComponent> procedure = new ArrayList<>();

    // Sort proc codes by priority prior to building resource
    List<PreAdjFissProcCode> procCodes = new ArrayList<>(claimGroup.getProcCodes());
    procCodes.sort(Comparator.comparingInt(PreAdjFissProcCode::getPriority));

    for (int i = 0; i < procCodes.size(); ++i) {
      PreAdjFissProcCode procCode = procCodes.get(i);
      Claim.ProcedureComponent component = new Claim.ProcedureComponent();

      component.setSequence((i + 1));
      component.setDate(
          Date.from(procCode.getProcDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
      component.setProcedure(
          new CodeableConcept()
              .setCoding(
                  Collections.singletonList(
                      new Coding("http://hl7.org/fhir/sid/icd-10", procCode.getProcCode(), null))));

      procedure.add(component);
    }

    return procedure;
  }
}
