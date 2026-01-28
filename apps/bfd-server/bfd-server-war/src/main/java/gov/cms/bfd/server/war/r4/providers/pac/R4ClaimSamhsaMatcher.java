package gov.cms.bfd.server.war.r4.providers.pac;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.r4.ClaimAdapter;
import gov.cms.bfd.server.war.commons.AbstractSamhsaMatcher;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A {@link Predicate} that, when <code>true</code>, indicates that an {@link Claim} (i.e. claim) is
 * SAMHSA-related.
 *
 * <p>See <code>/bluebutton-data-server.git/dev/design-samhsa-filtering.md</code> for details on the
 * design of this feature.
 *
 * <p>This class is designed to be thread-safe, as it's expensive to construct and so should be used
 * as a singleton.
 */
@Component
public final class R4ClaimSamhsaMatcher extends AbstractSamhsaMatcher<Claim> {

  /** The fiss claim transformer, used for converting resources to check for samhsa data. */
  private final FissClaimTransformerV2 fissTransformer;

  /** The mcs claim transformer, used for converting resources to check for samhsa data. */
  private final McsClaimTransformerV2 mcsTransformer;

  /** Flag to control whether SAMHSA filtering should be applied. */
  private final boolean samhsaV2Enabled;

  /**
   * Instantiates a new samhsa matcher.
   *
   * <p>Resources should be instantiated by Spring, so this should only be directly called by tests.
   *
   * @param fissClaimTransformer the fiss claim transformer
   * @param mcsClaimTransformer the mcs claim transformer
   * @param samhsaV2Enabled the samhsa2.0 flag
   */
  public R4ClaimSamhsaMatcher(
      FissClaimTransformerV2 fissClaimTransformer,
      McsClaimTransformerV2 mcsClaimTransformer,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.mcsTransformer = mcsClaimTransformer;
    this.fissTransformer = fissClaimTransformer;
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Determines if there are no samhsa entries in the claim.
   *
   * @param entity the claim to check
   * @return {@code true} if there are no samhsa entries in the claim
   */
  public boolean hasNoSamhsaData(ClaimWithSecurityTags<?> entity) {
    Claim claim;

    if (((ClaimWithSecurityTags<?>) entity).getClaimEntity() instanceof RdaFissClaim) {
      claim = fissTransformer.transform(entity);
    } else if (((ClaimWithSecurityTags<?>) entity).getClaimEntity() instanceof RdaMcsClaim) {
      claim = mcsTransformer.transform(entity);
    } else {
      throw new IllegalArgumentException(
          "Unsupported entity " + entity.getClass().getCanonicalName() + " for samhsa filtering");
    }

    return !test(claim);
  }

  /** {@inheritDoc} */
  @Override
  public boolean test(Claim claim) {

    // check here for the future flag samhsaV2Enabled and return false to skip the Samhsa matcher
    // and redact the data with Samhsa 2.0 Interceptors V1SamhsaConsentInterceptor and
    // V2SamhsaConsentInterceptor
    if (samhsaV2Enabled) {
      return false;
    }

    ClaimAdapter adapter = new ClaimAdapter(claim);

    return containsSamhsaIcdProcedureCode(adapter.getProcedure())
        || containsSamhsaIcdDiagnosisCode(adapter.getDiagnosis())
        || containsSamhsaLineItem(adapter.getItem())
        || containsSamhsaSupportingInfo(adapter.getSupportingInfo());
  }

  /** Additional valid coding system URL for backwards-compatibility. */
  private static final Set<String> BACKWARDS_COMPATIBLE_HCPCS_SYSTEM =
      Set.of(CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.HCPCS_CD));

  /**
   * Checks if the given {@link CodeableConcept} contains only known coding systems.
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains only the
   *     carin HCPCS system, <code>false</code> otherwise.
   */
  @Override
  protected boolean containsOnlyKnownSystems(CodeableConcept procedureConcept) {
    Set<String> codingSystems =
        procedureConcept.getCoding().stream().map(Coding::getSystem).collect(Collectors.toSet());
    if (codingSystems.isEmpty()) {
      return false;
    }
    for (String system : codingSystems) {
      if (!(BACKWARDS_COMPATIBLE_HCPCS_SYSTEM.contains(system)
          || TransformerConstants.CODING_SYSTEM_CARIN_HCPCS.equals(system))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Partially adjudicated data uses the Carin HCPCS system.
   *
   * @return The Carin HCPCS system.
   */
  @Override
  protected String getHcpcsSystem() {
    return TransformerConstants.CODING_SYSTEM_CARIN_HCPCS;
  }
}
