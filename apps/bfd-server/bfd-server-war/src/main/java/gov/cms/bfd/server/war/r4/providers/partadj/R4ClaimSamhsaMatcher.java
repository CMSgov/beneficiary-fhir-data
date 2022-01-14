package gov.cms.bfd.server.war.r4.providers.partadj;

import gov.cms.bfd.server.war.adapters.r4.ClaimAdapter;
import gov.cms.bfd.server.war.r4.providers.AbstractR4SamhsaMatcher;
import java.util.function.Predicate;
import org.hl7.fhir.r4.model.Claim;
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
public final class R4ClaimSamhsaMatcher extends AbstractR4SamhsaMatcher<Claim> {

  /** @see Predicate#test(Object) */
  @Override
  public boolean test(Claim claim) {
    ClaimAdapter adapter = new ClaimAdapter(claim);

    return containsSamhsaIcdProcedureCode(adapter.getProcedure())
        || containsSamhsaIcdDiagnosisCode(adapter.getDiagnosis())
        || containsSamhsaLineItem(adapter.getItem());
  }
}
