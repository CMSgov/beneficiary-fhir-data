package gov.cms.bfd.server.war.r4.providers.partadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PartAdjMcsClaim;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.r4.providers.partadj.common.AbstractTransformerV2;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.codesystems.ClaimType;

/** Transforms FISS/MCS instances into FHIR {@link ClaimResponse} resources. */
public class McsClaimResponseTransformerV2 extends AbstractTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimResponseTransformerV2.class.getSimpleName(), "transform");

  /**
   * There are only 2 statuses currently being used, and only the ones listed below are mapped to
   * "CANCELLED". For brevity, the rest are defaulted to "ACTIVE" using {@link
   * Map#getOrDefault(Object, Object)}.
   */
  private static final Map<String, ClaimResponse.ClaimResponseStatus> STATUS_MAP =
      Map.of(
          "r", ClaimResponse.ClaimResponseStatus.CANCELLED,
          "z", ClaimResponse.ClaimResponseStatus.CANCELLED,
          "9", ClaimResponse.ClaimResponseStatus.CANCELLED);

  private static final Map<String, ClaimResponse.RemittanceOutcome> OUTCOME_MAP =
      Map.ofEntries(
          Map.entry("a", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("b", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("j", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("k", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("1", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("2", ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry("c", ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry("l", ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry("3", ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry("8", ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry("d", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("e", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("f", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("g", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("m", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("n", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("p", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("q", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("r", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("u", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("v", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("w", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("x", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("y", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("z", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("4", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("5", ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry("9", ClaimResponse.RemittanceOutcome.COMPLETE));

  private McsClaimResponseTransformerV2() {}

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link PartAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  @Trace
  static ClaimResponse transform(MetricRegistry metricRegistry, Object claimEntity) {
    if (!(claimEntity instanceof PartAdjMcsClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((PartAdjMcsClaim) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link PartAdjMcsClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link
   *     PartAdjMcsClaim}
   */
  private static ClaimResponse transformClaim(PartAdjMcsClaim claimGroup) {
    ClaimResponse claim = new ClaimResponse();

    claim.setId("m-" + claimGroup.getIdrClmHdIcn());
    claim.setContained(List.of(getContainedPatient(claimGroup)));
    claim.setExtension(getExtension(claimGroup));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setStatus(getStatus(claimGroup));
    claim.setOutcome(getOutcome(claimGroup));
    claim.setType(getType());
    claim.setUse(ClaimResponse.Use.CLAIM);
    claim.setInsurer(new Reference().setIdentifier(new Identifier().setValue("CMS")));
    claim.setPatient(new Reference("#patient"));
    claim.setRequest(new Reference(String.format("Claim/m-%s", claimGroup.getIdrClmHdIcn())));

    claim.setMeta(new Meta().setLastUpdated(Date.from(claimGroup.getLastUpdated())));
    claim.setCreated(new Date());

    return claim;
  }

  private static ClaimResponse.ClaimResponseStatus getStatus(PartAdjMcsClaim claimGroup) {
    ClaimResponse.ClaimResponseStatus status;

    if (claimGroup.getIdrStatusCode() == null) {
      status = ClaimResponse.ClaimResponseStatus.ACTIVE;
    } else {
      // If it's not mapped, we assume it's ACTIVE
      status =
          STATUS_MAP.getOrDefault(
              claimGroup.getIdrStatusCode().toLowerCase(),
              ClaimResponse.ClaimResponseStatus.ACTIVE);
    }

    return status;
  }

  private static ClaimResponse.RemittanceOutcome getOutcome(PartAdjMcsClaim claimGroup) {
    ClaimResponse.RemittanceOutcome outcome;

    if (claimGroup.getIdrStatusCode() == null) {
      outcome = ClaimResponse.RemittanceOutcome.QUEUED;
    } else {
      // If it's not mapped, we assume it's QUEUED
      outcome =
          OUTCOME_MAP.getOrDefault(
              claimGroup.getIdrStatusCode().toLowerCase(), ClaimResponse.RemittanceOutcome.QUEUED);
    }

    return outcome;
  }

  private static Resource getContainedPatient(PartAdjMcsClaim claimGroup) {
    PatientInfo patientInfo =
        new PatientInfo(
            ifNotNull(claimGroup.getIdrBeneFirstInit(), s -> s + "."),
            ifNotNull(claimGroup.getIdrBeneLast_1_6(), s -> s.charAt(0) + "."),
            ifNotNull(claimGroup.getIdrBeneMidInit(), s -> s + "."),
            null, // MCS claims don't contain dob
            claimGroup.getIdrBeneSex());

    return getContainedPatient(claimGroup.getIdrClaimMbi(), patientInfo);
  }

  private static List<Extension> getExtension(PartAdjMcsClaim claimGroup) {
    List<Extension> extensions = new ArrayList<>();

    if (claimGroup.getIdrStatusCode() != null) {
      extensions.add(
          new Extension(BBCodingSystems.MCS.STATUS_CODE)
              .setValue(
                  new Coding(
                      BBCodingSystems.MCS.STATUS_CODE, claimGroup.getIdrStatusCode(), null)));
    }

    if (claimGroup.getIdrClaimReceiptDate() != null) {
      extensions.add(
          new Extension(BBCodingSystems.MCS.CLAIM_RECEIPT_DATE)
              .setValue(new DateType(localDateToDate(claimGroup.getIdrClaimReceiptDate()))));
    }

    if (claimGroup.getIdrStatusDate() != null) {
      extensions.add(
          new Extension(BBCodingSystems.MCS.STATUS_DATE)
              .setValue(new DateType(localDateToDate(claimGroup.getIdrStatusDate()))));
    }

    return List.copyOf(extensions);
  }

  private static List<Identifier> getIdentifier(PartAdjMcsClaim claimGroup) {
    return List.of(
        new Identifier()
            .setType(
                new CodeableConcept(
                    new Coding(
                        C4BBIdentifierType.UC.getSystem(),
                        C4BBIdentifierType.UC.toCode(),
                        C4BBIdentifierType.UC.getDisplay())))
            .setSystem(BBCodingSystems.CARR_CLM_CONTROL_NUM)
            .setValue(claimGroup.getIdrClmHdIcn()));
  }

  private static CodeableConcept getType() {
    return new CodeableConcept()
        .setCoding(
            List.of(
                new Coding(
                    ClaimType.PROFESSIONAL.getSystem(),
                    ClaimType.PROFESSIONAL.toCode(),
                    ClaimType.PROFESSIONAL.getDisplay())));
  }
}
