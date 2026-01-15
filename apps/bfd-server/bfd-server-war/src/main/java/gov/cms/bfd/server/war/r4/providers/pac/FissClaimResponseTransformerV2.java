package gov.cms.bfd.server.war.r4.providers.pac;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudicationDiscriminator;
import gov.cms.bfd.server.war.r4.providers.pac.common.AbstractTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.r4.providers.pac.common.FissTransformerV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.CodeableConcept;
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
public class FissClaimResponseTransformerV2 extends AbstractTransformerV2
    implements ResourceTransformer<ClaimResponse> {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(FissClaimResponseTransformerV2.class.getSimpleName(), "transform");

  /** The securityTagManager. */
  private final SecurityTagManager securityTagManager;

  private final boolean samhsaV2Enabled;

  /**
   * The known FISS status codes and their associated {@link ClaimResponse.RemittanceOutcome}
   * mappings.
   */
  private static final Map<Character, ClaimResponse.RemittanceOutcome> STATUS_TO_OUTCOME =
      Map.ofEntries(
          Map.entry(' ', ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry('a', ClaimResponse.RemittanceOutcome.QUEUED),
          Map.entry('d', ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry('f', ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry('i', ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry('m', ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry('p', ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry('r', ClaimResponse.RemittanceOutcome.COMPLETE),
          Map.entry('s', ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry('t', ClaimResponse.RemittanceOutcome.PARTIAL),
          Map.entry('u', ClaimResponse.RemittanceOutcome.COMPLETE));

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param securityTagManager the security tag manager
   * @param samhsaV2Enabled samhsaV2Enabled flag
   */
  public FissClaimResponseTransformerV2(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    requireNonNull(metricRegistry);
    this.metricRegistry = metricRegistry;
    this.securityTagManager = securityTagManager;
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a claim entity into a {@link ClaimResponse}.
   *
   * @param claimEntity the FISS {@link RdaFissClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  public ClaimResponse transform(ClaimWithSecurityTags<?> claimEntity) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimEntity.getSecurityTags());

    if (!(claim instanceof RdaFissClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      RdaFissClaim rdaFissClaim = (RdaFissClaim) claim;
      return transformClaim(rdaFissClaim, securityTags);
    }
  }

  /**
   * Transforms an {@link RdaFissClaim} to a FHIR {@link ClaimResponse}.
   *
   * @param claimGroup the {@link RdaFissClaim} to transform
   * @param securityTags securityTags of the claim
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link
   *     RdaFissClaim}
   */
  private ClaimResponse transformClaim(RdaFissClaim claimGroup, List<Coding> securityTags) {
    ClaimResponse claim = new ClaimResponse();

    claim.setId("f-" + claimGroup.getClaimId());
    claim.setContained(List.of(FissTransformerV2.getContainedPatient(claimGroup)));
    claim.getIdentifier().add(createClaimIdentifier(BBCodingSystems.FISS.DCN, claimGroup.getDcn()));
    claim.setExtension(getExtension(claimGroup));
    claim.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
    claim.setOutcome(getOutcome(claimGroup.getCurrStatus()));
    claim.setType(createCodeableConcept(ClaimType.INSTITUTIONAL));
    claim.setUse(ClaimResponse.Use.CLAIM);
    claim.setInsurer(new Reference().setIdentifier(new Identifier().setValue("CMS")));
    claim.setPatient(new Reference("#patient"));
    claim.setRequest(new Reference(String.format("Claim/f-%s", claimGroup.getClaimId())));
    claim.setItem(getClaimItems(claimGroup));

    // Add the Coding to the list
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
   * Builds a list of {@link Extension} objects using data from the given {@link RdaFissClaim}.
   *
   * @param claimGroup The {@link RdaFissClaim} to pull associated data from.
   * @return A list of {@link Extension} objects build from the given {@link RdaFissClaim} data.
   */
  private List<Extension> getExtension(RdaFissClaim claimGroup) {
    List<Extension> extensions = new ArrayList<>();
    addExtension(extensions, BBCodingSystems.FISS.CURR_STATUS, "" + claimGroup.getCurrStatus());
    addExtension(extensions, BBCodingSystems.FISS.RECD_DT_CYMD, claimGroup.getReceivedDate());
    addExtension(extensions, BBCodingSystems.FISS.CURR_TRAN_DT_CYMD, claimGroup.getCurrTranDate());
    addExtension(extensions, BBCodingSystems.FISS.GROUP_CODE, claimGroup.getGroupCode());

    return extensions;
  }

  /**
   * Maps the given status code to an associated {@link ClaimResponse.RemittanceOutcome}. Unknown
   * status codes are mapped to {@link ClaimResponse.RemittanceOutcome#PARTIAL}.
   *
   * @param statusCode The statusCode from the {@link RdaFissClaim}.
   * @return The {@link ClaimResponse.RemittanceOutcome} associated with the given status code.
   */
  private ClaimResponse.RemittanceOutcome getOutcome(char statusCode) {
    return STATUS_TO_OUTCOME.getOrDefault(
        Character.toLowerCase(statusCode), ClaimResponse.RemittanceOutcome.PARTIAL);
  }

  /**
   * Maps the {@link RdaFissRevenueLine} data to the appropriate FHIR fields.
   *
   * @param claimGroup The claim data to map.
   * @return The mapped FHIR objects.
   */
  private List<ClaimResponse.ItemComponent> getClaimItems(RdaFissClaim claimGroup) {
    return claimGroup.getRevenueLines().stream()
        .sorted(Comparator.comparing(RdaFissRevenueLine::getRdaPosition))
        .map(
            revenueLine -> {
              ClaimResponse.ItemComponent itemComponent = new ClaimResponse.ItemComponent();

              itemComponent.addAdjudication(
                  getClaimItemAdjudication(
                      BBCodingSystems.FISS.ACO_RED_RARC, revenueLine.getAcoRedRarc()));
              itemComponent.addAdjudication(
                  getClaimItemAdjudication(
                      BBCodingSystems.FISS.ACO_RED_CARC, revenueLine.getAcoRedCarc()));
              itemComponent.addAdjudication(
                  getClaimItemAdjudication(
                      BBCodingSystems.FISS.ACO_RED_CAGC, revenueLine.getAcoRedCagc()));

              return itemComponent;
            })
        .toList();
  }

  /**
   * Creates FHIR {@link ClaimResponse.AdjudicationComponent} objects from the given system and
   * code.
   *
   * @param system The system to use to create the component.
   * @param code The code to use to create the component.
   * @return The created FHIR component.
   */
  private ClaimResponse.AdjudicationComponent getClaimItemAdjudication(String system, String code) {
    ClaimResponse.AdjudicationComponent adjComponent;

    if (Strings.isNotBlank(code)) {
      adjComponent = new ClaimResponse.AdjudicationComponent();

      adjComponent.setCategory(createCodeableConcept(C4BBAdjudicationDiscriminator.DENIAL_REASON));
      adjComponent.setReason(new CodeableConcept(new Coding(system, code, null)));
    } else {
      adjComponent = null;
    }

    return adjComponent;
  }
}
