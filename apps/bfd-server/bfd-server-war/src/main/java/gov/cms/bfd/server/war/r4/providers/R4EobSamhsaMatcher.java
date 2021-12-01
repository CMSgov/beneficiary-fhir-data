package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.server.war.adapters.r4.ExplanationOfBenefitAdapter;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.function.Predicate;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
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
public final class R4EobSamhsaMatcher extends AbstractR4SamhsaMatcher<ExplanationOfBenefit> {

  /** @see Predicate#test(Object) */
  // S128 - Fallthrough is intentional.
  @SuppressWarnings("squid:S128")
  @Override
  public boolean test(ExplanationOfBenefit eob) {
    ExplanationOfBenefitAdapter adapter = new ExplanationOfBenefitAdapter(eob);

    ClaimTypeV2 claimType = TransformerUtilsV2.getClaimType(eob);

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
}
