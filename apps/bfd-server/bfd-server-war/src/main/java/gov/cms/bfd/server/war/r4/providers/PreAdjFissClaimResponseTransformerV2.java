package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;

/** Transforms FISS/MCS instances into FHIR {@link ClaimResponse} resources. */
public class PreAdjFissClaimResponseTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(PreAdjFissClaimResponseTransformerV2.class.getSimpleName(), "transform");

  private static final Map<Character, String> STATUS_TEXT;

  static {
    STATUS_TEXT = new HashMap<>();
    STATUS_TEXT.put('a', "Active");
    STATUS_TEXT.put('s', "Suspend");
    STATUS_TEXT.put('p', "Paid");
    STATUS_TEXT.put('d', "Denied");
    STATUS_TEXT.put('i', "Inactive");
    STATUS_TEXT.put('r', "Reject");
    STATUS_TEXT.put('t', "RTP");
    STATUS_TEXT.put('m', "Move");
  }

  private static final Map<Character, ClaimResponse.RemittanceOutcome> OUTCOME_TEXT;

  static {
    OUTCOME_TEXT = new HashMap<>();
    OUTCOME_TEXT.put('a', ClaimResponse.RemittanceOutcome.QUEUED);
    OUTCOME_TEXT.put('s', ClaimResponse.RemittanceOutcome.PARTIAL);
    OUTCOME_TEXT.put('p', ClaimResponse.RemittanceOutcome.COMPLETE);
    OUTCOME_TEXT.put('d', ClaimResponse.RemittanceOutcome.ERROR);
    OUTCOME_TEXT.put('i', ClaimResponse.RemittanceOutcome.PARTIAL);
    OUTCOME_TEXT.put('r', ClaimResponse.RemittanceOutcome.ERROR);
    OUTCOME_TEXT.put('t', ClaimResponse.RemittanceOutcome.PARTIAL);
    OUTCOME_TEXT.put('m', ClaimResponse.RemittanceOutcome.QUEUED);
  }

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the FISS {@link PreAdjFissClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  @Trace
  static ClaimResponse transform(MetricRegistry metricRegistry, Object claimEntity) {
    Timer.Context timer = metricRegistry.timer(METRIC_NAME).time();

    if (!(claimEntity instanceof PreAdjFissClaim)) {
      throw new BadCodeMonkeyException();
    }

    ClaimResponse claim = transformClaim((PreAdjFissClaim) claimEntity);

    timer.stop();
    return claim;
  }

  /**
   * @param claimGroup the {@link PreAdjFissClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link
   *     PreAdjFissClaim}
   */
  private static ClaimResponse transformClaim(PreAdjFissClaim claimGroup) {
    ClaimResponse claim = new ClaimResponse();

    claim.setId("f-" + claimGroup.getDcn());
    claim.setMeta(new Meta().setLastUpdated(Date.from(claimGroup.getLastUpdated())));
    claim.setExtension(getExtension(claimGroup));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
    claim.setOutcome(OUTCOME_TEXT.get(Character.toLowerCase(claimGroup.getCurrStatus())));
    claim.setType(getType());
    claim.setUse(ClaimResponse.Use.CLAIM);
    claim.setCreated(new Date());
    claim.setInsurer(new Reference().setIdentifier(new Identifier().setValue("CMS")));
    claim.setPatient(getPatient(claimGroup));
    claim.setRequest(new Reference(String.format("Claim/f-%s", claimGroup.getDcn())));

    return claim;
  }

  private static List<Extension> getExtension(PreAdjFissClaim claimGroup) {
    return Collections.singletonList(
        new Extension()
            .setUrl("https://dcgeo.cms.gov/resources/variables/fiss-status")
            .setValue(
                new Coding(
                    "https://dcgeo.cms.gov/resources/variables/fiss-status",
                    "" + claimGroup.getCurrStatus(),
                    STATUS_TEXT.get(Character.toLowerCase(claimGroup.getCurrStatus())))));
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
}
