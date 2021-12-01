package gov.cms.bfd.server.war.stu3.providers;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.stu3.ExplanationOfBenefitAdapter;
import gov.cms.bfd.server.war.commons.AbstractSamhsaMatcher;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
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

  // Valid system url for productOrService coding
  private static final Set<String> HCPCS_SYSTEM = Set.of(TransformerConstants.CODING_SYSTEM_HCPCS);

  /** @see Predicate#test(Object) */
  // S128 - Fallthrough is intentional.
  @SuppressWarnings("squid:S128")
  @Override
  public boolean test(ExplanationOfBenefit eob) {
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

  /**
   * Checks if the given {@link CodeableConcept} contains only known coding systems.
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains at least
   *     one {@link Coding} and only contains {@link Coding}s that have known coding systems, <code>
   *     false</code> otherwise
   */
  @Override
  protected boolean containsOnlyKnownSystems(CodeableConcept procedureConcept) {
    Set<String> codingSystems =
        procedureConcept.getCoding().stream().map(Coding::getSystem).collect(Collectors.toSet());

    return codingSystems.equals(HCPCS_SYSTEM);
  }
}
