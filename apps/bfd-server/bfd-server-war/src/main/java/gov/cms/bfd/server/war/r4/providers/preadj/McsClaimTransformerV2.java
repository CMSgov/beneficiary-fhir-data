package gov.cms.bfd.server.war.r4.providers.preadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
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
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
public class McsClaimTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimTransformerV2.class.getSimpleName(), "transform");

  private McsClaimTransformerV2() {}

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified claim
   */
  @Trace
  static Claim transform(MetricRegistry metricRegistry, Object claimEntity) {
    if (!(claimEntity instanceof PreAdjMcsClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((PreAdjMcsClaim) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified {@link PreAdjMcsClaim}
   */
  private static Claim transformClaim(PreAdjMcsClaim claimGroup) {
    Claim claim = new Claim();

    claim.setId("m-" + claimGroup.getIdrClmHdIcn());
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

  private static List<Resource> getContainedProvider(PreAdjMcsClaim claimGroup) {
    return Collections.singletonList(
        new Organization()
            .setIdentifier(
                Arrays.asList(
                    new Identifier()
                        .setType(
                            new CodeableConcept(
                                new Coding(
                                    C4BBOrganizationIdentifierType.PRN.getSystem(),
                                    C4BBOrganizationIdentifierType.PRN.toCode(),
                                    C4BBOrganizationIdentifierType.PRN.getDisplay())))
                        .setValue(claimGroup.getIdrBillProvNum()),
                    new Identifier()
                        .setType(
                            new CodeableConcept(
                                new Coding(
                                    C4BBIdentifierType.NPI.getSystem(),
                                    C4BBIdentifierType.NPI.toCode(),
                                    C4BBIdentifierType.NPI.getDisplay())))
                        .setSystem("http://hl7.org/fhir/sid/us-npi")
                        .setValue(claimGroup.getIdrBillProvNpi())))
            .setId("provider-org")
            .setMeta(
                new Meta()
                    .setProfile(
                        Collections.singletonList(
                            new CanonicalType(
                                "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization")))));
  }

  private static List<Identifier> getIdentifier(PreAdjMcsClaim claimGroup) {
    return Collections.singletonList(
        new Identifier()
            .setType(
                new CodeableConcept(
                    new Coding(
                        C4BBIdentifierType.UC.getSystem(),
                        C4BBIdentifierType.UC.toCode(),
                        C4BBIdentifierType.UC.getDisplay())))
            .setSystem("https://dcgeo.cms.gov/resources/variables/icn")
            .setValue(claimGroup.getIdrClmHdIcn()));
  }

  private static CodeableConcept getType() {
    return new CodeableConcept()
        .setCoding(
            Arrays.asList(
                new Coding("https://dcgeo.cms.gov/resources/codesystem/rda-type", "MCS", null),
                new Coding(
                    ClaimType.PROFESSIONAL.getSystem(),
                    ClaimType.PROFESSIONAL.toCode(),
                    ClaimType.PROFESSIONAL.getDisplay())));
  }

  private static CodeableConcept getPriority() {
    return new CodeableConcept(
        new Coding(
            ProcessPriority.NORMAL.getSystem(),
            ProcessPriority.NORMAL.toCode(),
            ProcessPriority.NORMAL.getDisplay()));
  }

  private static Money getTotal(PreAdjMcsClaim claimGroup) {
    Money total;

    if (claimGroup.getIdrTotBilledAmt() != null) {
      total = new Money();

      total.setValue(claimGroup.getIdrTotBilledAmt());
      total.setCurrency("USD");
    } else {
      total = null;
    }

    return total;
  }

  private static Reference getPatient(PreAdjMcsClaim claimGroup) {
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
                .setValue(claimGroup.getIdrClaimMbi()));
  }

  private static List<Claim.DiagnosisComponent> getDiagnosis(PreAdjMcsClaim claimGroup) {
    List<Claim.DiagnosisComponent> diagnosisComponents = new ArrayList<>();

    // Sort diagnosis codes by priority prior to building resource
    List<PreAdjMcsDiagnosisCode> dxCodes = new ArrayList<>(claimGroup.getDiagCodes());
    dxCodes.sort(Comparator.comparingInt(PreAdjMcsDiagnosisCode::getPriority));

    for (int i = 0; i < dxCodes.size(); ++i) {
      diagnosisComponents.add(getDiagnosis(dxCodes.get(i), i + 1));
    }

    return diagnosisComponents;
  }

  private static Claim.DiagnosisComponent getDiagnosis(PreAdjMcsDiagnosisCode code, int sequence) {
    String icdVersion = code.getIdrDiagIcdType().equals("0") ? "10" : "9";

    return new Claim.DiagnosisComponent()
        .setSequence(sequence)
        .setDiagnosis(
            new CodeableConcept()
                .setCoding(
                    Collections.singletonList(
                        new Coding(
                            "http://hl7.org/fhir/sid/icd-" + icdVersion,
                            code.getIdrDiagCode(),
                            null))));
  }

  private static List<Claim.ProcedureComponent> getProcedure(PreAdjMcsClaim claimGroup) {
    List<Claim.ProcedureComponent> procedure = new ArrayList<>();

    // Sort proc codes by priority prior to building resource
    List<PreAdjMcsDetail> procCodes = new ArrayList<>(claimGroup.getDetails());
    procCodes.sort(Comparator.comparingInt(PreAdjMcsDetail::getPriority));

    for (int i = 0; i < procCodes.size(); ++i) {
      procedure.add(getProcedure(procCodes.get(i), i + 1));
    }

    return procedure;
  }

  private static Claim.ProcedureComponent getProcedure(PreAdjMcsDetail procCode, int sequence) {
    return new Claim.ProcedureComponent()
        .setSequence(sequence)
        .setDate(
            procCode.getIdrDtlToDate() == null
                ? null
                : Date.from(
                    procCode.getIdrDtlToDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
        .setProcedure(
            new CodeableConcept()
                .setCoding(
                    Collections.singletonList(
                        new Coding(
                            "http://www.ama-assn.org/go/cpt", procCode.getIdrProcCode(), null))));
  }
}
