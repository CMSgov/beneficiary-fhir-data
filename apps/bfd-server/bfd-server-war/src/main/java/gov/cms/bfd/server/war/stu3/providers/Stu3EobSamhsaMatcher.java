package gov.cms.bfd.server.war.stu3.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.stu3.ExplanationOfBenefitAdapter;
import gov.cms.bfd.server.war.commons.AbstractSamhsaMatcher;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A {@link Predicate} that, when <code>true</code>, indicates that an {@link ExplanationOfBenefit}
 * (i.e. claim) is SAMHSA-related.
 *
 * <p>See <code>/bluebutton-data-server.git/dev/design-samhsa-filtering.md</code> for details on the
 * design of this feature.
 *
 * <p>This class is designed to be thread-safe, as it's expensive to construct and so should be used
 * as a singleton.
 */
@Component
public final class Stu3EobSamhsaMatcher extends AbstractSamhsaMatcher<ExplanationOfBenefit> {

  /** Valid system url for productOrService coding. * */
  private static final Set<String> HCPCS_SYSTEM = Set.of(TransformerConstants.CODING_SYSTEM_HCPCS);

  /** Additional valid system url for productOrService coding. */
  private static final Set<String> DATA_ABSENT_SYSTEM =
      Set.of(TransformerConstants.CODING_DATA_ABSENT);

  /** Flag to control whether SAMHSA filtering should be applied. */
  private final boolean samhsaV2Enabled;

  /**
   * Instantiates a R4EobSamhsaMatcher.
   *
   * <p>Resources should be instantiated by Spring, so this should only be directly called by tests.
   *
   * @param samhsaV2Enabled the samhsa2.0 flag
   */
  public Stu3EobSamhsaMatcher(
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /** {@inheritDoc} */
  // S128 - Fallthrough is intentional.
  @SuppressWarnings("squid:S128")
  @Override
  public boolean test(ExplanationOfBenefit eob) {

    // check here for the future flag samhsaV2Enabled and return false to skip the Samhsa matcher
    // check
    // and redact the data with Samhsa 2.0 Interceptors V1SamhsaConsentInterceptor and
    // V2SamhsaConsentInterceptor
    if (samhsaV2Enabled) {
      return false;
    }

    ExplanationOfBenefitAdapter adapter = new ExplanationOfBenefitAdapter(eob);

    ClaimType claimType = TransformerUtils.getClaimType(eob);

    boolean containsSamhsa = false;

    switch (claimType) {
      case INPATIENT:
      case OUTPATIENT:
      case SNF:
        containsSamhsa = containsSamhsaIcdProcedureCode(adapter.getProcedure());
      case CARRIER:
      case DME:
      case HHA:
      case HOSPICE:
        containsSamhsa =
            containsSamhsa
                || containsSamhsaIcdDiagnosisCode(adapter.getDiagnosis())
                || containsSamhsaLineItem(adapter.getItem());
      case PDE:
        // There are no SAMHSA fields in PDE claims
        break;
      default:
        throw new BadCodeMonkeyException("Unsupported claim type: " + claimType);
    }

    return containsSamhsa;
  }

  /** {@inheritDoc} */
  @Override
  protected boolean containsOnlyKnownSystems(CodeableConcept procedureConcept) {
    Set<String> codingSystems =
        procedureConcept.getCoding().stream().map(Coding::getSystem).collect(Collectors.toSet());
    if (codingSystems.isEmpty()) {
      return false;
    }
    for (String system : codingSystems) {
      if (!(HCPCS_SYSTEM.contains(system) || DATA_ABSENT_SYSTEM.contains(system))) {
        return false;
      }
    }
    return true;
  }
}
