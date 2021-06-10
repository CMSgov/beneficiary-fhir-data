package gov.cms.bfd.server.war.r4.providers.preadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
public class FissClaimResponseTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(FissClaimResponseTransformerV2.class.getSimpleName(), "transform");

  private static final Map<Character, String> STATUS_TEXT;

  static {
    STATUS_TEXT =
        ImmutableMap.<Character, String>builder()
            .put(' ', "Unknown")
            .put('a', "Accepted")
            .put('s', "Suspend")
            .put('p', "Paid")
            .put('d', "Denied")
            .put('i', "Inactive")
            .put('r', "Rejected")
            .put('t', "Return To Provider")
            .put('m', "Move")
            .build();
  }

  private static final Map<Character, ClaimResponse.RemittanceOutcome> STATUS_TO_OUTCOME;

  static {
    STATUS_TO_OUTCOME =
        ImmutableMap.<Character, ClaimResponse.RemittanceOutcome>builder()
            .put(' ', ClaimResponse.RemittanceOutcome.QUEUED)
            .put('a', ClaimResponse.RemittanceOutcome.QUEUED)
            .put('s', ClaimResponse.RemittanceOutcome.PARTIAL)
            .put('p', ClaimResponse.RemittanceOutcome.COMPLETE)
            .put('d', ClaimResponse.RemittanceOutcome.COMPLETE)
            .put('i', ClaimResponse.RemittanceOutcome.PARTIAL)
            .put('r', ClaimResponse.RemittanceOutcome.COMPLETE)
            .put('t', ClaimResponse.RemittanceOutcome.COMPLETE)
            .put('m', ClaimResponse.RemittanceOutcome.PARTIAL)
            .build();
  }

  private FissClaimResponseTransformerV2() {}

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the FISS {@link PreAdjFissClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  @Trace
  static ClaimResponse transform(MetricRegistry metricRegistry, Object claimEntity) {
    if (!(claimEntity instanceof PreAdjFissClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((PreAdjFissClaim) claimEntity);
    }
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
    claim.setOutcome(STATUS_TO_OUTCOME.get(Character.toLowerCase(claimGroup.getCurrStatus())));
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
