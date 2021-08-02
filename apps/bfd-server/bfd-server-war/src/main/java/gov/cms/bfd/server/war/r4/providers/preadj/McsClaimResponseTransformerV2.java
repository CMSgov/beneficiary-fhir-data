package gov.cms.bfd.server.war.r4.providers.preadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
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
import org.hl7.fhir.r4.model.codesystems.ClaimType;

/** Transforms FISS/MCS instances into FHIR {@link ClaimResponse} resources. */
public class McsClaimResponseTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimResponseTransformerV2.class.getSimpleName(), "transform");

  private static final Map<String, String> STATUS_TEXT;

  static {
    STATUS_TEXT =
        ImmutableMap.<String, String>builder()
            .put("a", "Current Active Claim")
            .put("b", "Suspended")
            .put("c", "Approved Awaiting CWF Tesponse")
            .put("d", "Approved and Paid")
            .put("e", "Denied")
            .put("f", "Full Claim Refund")
            .put("g", "Partial Refund Applied")
            .put("j", "Claim Still Active")
            .put("k", "Claim Pending Suspense")
            .put("l", "CWF Suspense")
            .put("m", "Approved and Paid")
            .put("n", "Denied for Payment")
            .put("p", "Partial Claim Refund")
            .put("q", "Adjusted")
            .put("r", "Claim Deleted")
            .put("u", "Paid, but Not for Dup Use")
            .put("v", "Denied, but Not for Dup Use")
            .put("w", "Rejected")
            .put("x", "Partial Refund")
            .put("y", "Full Refund")
            .put("z", "Voided")
            .put("1", "Current Active Claim")
            .put("2", "Suspended")
            .put("3", "Approved Awaiting CWF")
            .put("4", "Approved and Paid")
            .put("5", "Denied")
            // .put("6", "Not Used")
            .put("8", "Claim Moved to Another Hic")
            .put("9", "Claim Deleted")
            .build();
  }

  private static final Map<String, ClaimResponse.RemittanceOutcome> STATUS_TO_OUTCOME;

  static {
    STATUS_TO_OUTCOME =
        ImmutableMap.<String, ClaimResponse.RemittanceOutcome>builder()
            .put("a", ClaimResponse.RemittanceOutcome.QUEUED)
            .put("b", ClaimResponse.RemittanceOutcome.QUEUED)
            .put("c", ClaimResponse.RemittanceOutcome.PARTIAL)
            .put("d", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("e", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("f", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("g", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("j", ClaimResponse.RemittanceOutcome.QUEUED)
            .put("k", ClaimResponse.RemittanceOutcome.QUEUED)
            .put("l", ClaimResponse.RemittanceOutcome.PARTIAL)
            .put("m", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("n", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("p", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("q", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("r", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("u", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("v", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("w", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("x", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("y", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("z", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("1", ClaimResponse.RemittanceOutcome.QUEUED)
            .put("2", ClaimResponse.RemittanceOutcome.QUEUED)
            .put("3", ClaimResponse.RemittanceOutcome.PARTIAL)
            .put("4", ClaimResponse.RemittanceOutcome.COMPLETE)
            .put("5", ClaimResponse.RemittanceOutcome.COMPLETE)
            // .put("6", null)
            .put("8", ClaimResponse.RemittanceOutcome.PARTIAL)
            .put("9", ClaimResponse.RemittanceOutcome.COMPLETE)
            .build();
  }

  private McsClaimResponseTransformerV2() {}

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  @Trace
  static ClaimResponse transform(MetricRegistry metricRegistry, Object claimEntity) {
    if (!(claimEntity instanceof PreAdjMcsClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((PreAdjMcsClaim) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link PreAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link
   *     PreAdjMcsClaim}
   */
  private static ClaimResponse transformClaim(PreAdjMcsClaim claimGroup) {
    ClaimResponse claim = new ClaimResponse();

    claim.setId("m-" + claimGroup.getIdrClmHdIcn());
    claim.setMeta(new Meta().setLastUpdated(Date.from(claimGroup.getLastUpdated())));
    claim.setExtension(getExtension(claimGroup));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
    claim.setOutcome(STATUS_TO_OUTCOME.get(claimGroup.getIdrStatusCode().toLowerCase()));
    claim.setType(getType());
    claim.setUse(ClaimResponse.Use.CLAIM);
    claim.setCreated(new Date());
    claim.setInsurer(new Reference().setIdentifier(new Identifier().setValue("CMS")));
    claim.setPatient(getPatient(claimGroup));
    claim.setRequest(new Reference(String.format("Claim/m-%s", claimGroup.getIdrClmHdIcn())));

    return claim;
  }

  private static List<Extension> getExtension(PreAdjMcsClaim claimGroup) {
    return Collections.singletonList(
        new Extension()
            .setUrl("https://dcgeo.cms.gov/resources/variables/mcs-status")
            .setValue(
                new Coding(
                    "https://dcgeo.cms.gov/resources/variables/mcs-status",
                    claimGroup.getIdrStatusCode(),
                    STATUS_TEXT.get(claimGroup.getIdrStatusCode().toLowerCase()))));
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
}
