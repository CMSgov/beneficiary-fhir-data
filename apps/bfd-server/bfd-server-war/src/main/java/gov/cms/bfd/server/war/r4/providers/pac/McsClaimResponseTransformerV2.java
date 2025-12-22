package gov.cms.bfd.server.war.r4.providers.pac;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.AbstractTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.r4.providers.pac.common.McsTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Transforms FISS/MCS instances into FHIR {@link ClaimResponse} resources. */
@Component
public class McsClaimResponseTransformerV2 extends AbstractTransformerV2
    implements ResourceTransformer<ClaimResponse> {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimResponseTransformerV2.class.getSimpleName(), "transform");

  /** The securityTagManager. */
  private final SecurityTagManager securityTagManager;

  private final boolean samhsaV2Enabled;

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

  /** The known MCS codes and their associated {@link ClaimResponse.RemittanceOutcome} mappings. */
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

  /**
   * Instantiates a new Mcs claim response transformer v2. @param metricRegistry the metric registry
   *
   * @param metricRegistry the metric registry
   * @param securityTagManager the security tag manager
   * @param samhsaV2Enabled samhsaV2Enabled flag
   */
  public McsClaimResponseTransformerV2(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = metricRegistry;
    this.securityTagManager = securityTagManager;
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a claim entity into a FHIR {@link ClaimResponse}.
   *
   * @param claimEntity the MCS {@link RdaMcsClaim} to transform
   * @param includeTaxNumbers Indicates if tax numbers should be included in the results
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  public ClaimResponse transform(ClaimWithSecurityTags<?> claimEntity, boolean includeTaxNumbers) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimEntity.getSecurityTags());

    if (!(claim instanceof RdaMcsClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      RdaMcsClaim rdaMcsClaim = (RdaMcsClaim) claim;
      return transformClaim(rdaMcsClaim, securityTags);
    }
  }

  /**
   * Transforms a {@link RdaMcsClaim} into a FHIR {@link Claim}.
   *
   * @param claimGroup the {@link RdaMcsClaim} to transform
   * @param securityTags securityTags of the claim
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link RdaMcsClaim}
   */
  private ClaimResponse transformClaim(RdaMcsClaim claimGroup, List<Coding> securityTags) {
    ClaimResponse claim = new ClaimResponse();

    claim.setId("m-" + claimGroup.getIdrClmHdIcn());
    claim.setContained(List.of(McsTransformerV2.getContainedPatient(claimGroup)));
    claim
        .getIdentifier()
        .add(createClaimIdentifier(BBCodingSystems.MCS.ICN, claimGroup.getIdrClmHdIcn()));
    claim.setExtension(getExtension(claimGroup));
    claim.setStatus(getStatus(claimGroup));
    claim.setOutcome(getOutcome(claimGroup));
    claim.setType(createCodeableConcept(ClaimType.PROFESSIONAL));
    claim.setUse(ClaimResponse.Use.CLAIM);
    claim.setInsurer(new Reference().setIdentifier(new Identifier().setValue("CMS")));
    claim.setPatient(new Reference("#patient"));
    claim.setRequest(new Reference(String.format("Claim/m-%s", claimGroup.getIdrClmHdIcn())));

    Meta meta = new Meta();
    meta.setLastUpdated(Date.from(claimGroup.getLastUpdated()));

    if (samhsaV2Enabled) {
      meta.setSecurity(securityTags);
    }
    claim.setMeta(meta);
    claim.setCreated(new Date());

    return claim;
  }

  /**
   * Gets the associated {@link ClaimResponse.ClaimResponseStatus} for the given {@link RdaMcsClaim}
   * object's status code.
   *
   * @param claimGroup The {@link RdaMcsClaim} object to get the status from.
   * @return The {@link ClaimResponse.ClaimResponseStatus} associated with the given data's status
   *     code.
   */
  private ClaimResponse.ClaimResponseStatus getStatus(RdaMcsClaim claimGroup) {
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

  /**
   * Gets the associated {@link ClaimResponse.RemittanceOutcome} for the given {@link RdaMcsClaim}
   * object's status code. Empty or null values are mapped to {@link
   * ClaimResponse.RemittanceOutcome#QUEUED} while unknown status codes are mapped to {@link
   * ClaimResponse.RemittanceOutcome#PARTIAL}.
   *
   * @param claimGroup The {@link RdaMcsClaim} object to get the status from.
   * @return The {@link ClaimResponse.RemittanceOutcome} associated with the given data's status
   *     code.
   */
  private ClaimResponse.RemittanceOutcome getOutcome(RdaMcsClaim claimGroup) {
    ClaimResponse.RemittanceOutcome outcome;

    if (Strings.isNullOrEmpty(claimGroup.getIdrStatusCode())) {
      outcome = ClaimResponse.RemittanceOutcome.QUEUED;
    } else {
      // If it's not mapped, we assume it's PARTIAL
      outcome =
          OUTCOME_MAP.getOrDefault(
              claimGroup.getIdrStatusCode().toLowerCase(), ClaimResponse.RemittanceOutcome.PARTIAL);
    }

    return outcome;
  }

  /**
   * Builds a list of {@link Extension} objects using data from the given {@link RdaMcsClaim}.
   *
   * @param claimGroup The {@link RdaMcsClaim} to pull associated data from.
   * @return A list of {@link Extension} objects build from the given {@link RdaMcsClaim} data.
   */
  private List<Extension> getExtension(RdaMcsClaim claimGroup) {
    List<Extension> extensions = new ArrayList<>();
    addExtension(extensions, BBCodingSystems.MCS.STATUS_CODE, claimGroup.getIdrStatusCode());
    addExtension(
        extensions, BBCodingSystems.MCS.CLAIM_RECEIPT_DATE, claimGroup.getIdrClaimReceiptDate());
    addExtension(extensions, BBCodingSystems.MCS.STATUS_DATE, claimGroup.getIdrStatusDate());

    return extensions;
  }
}
