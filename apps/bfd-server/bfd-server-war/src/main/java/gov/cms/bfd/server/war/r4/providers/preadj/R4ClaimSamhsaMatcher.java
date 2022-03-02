package gov.cms.bfd.server.war.r4.providers.preadj;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.r4.ClaimAdapter;
import gov.cms.bfd.server.war.commons.AbstractSamhsaMatcher;
import gov.cms.bfd.server.war.commons.TransformerConstants;
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
public final class R4ClaimSamhsaMatcher extends AbstractSamhsaMatcher<Claim> {

  /** @see Predicate#test(Object) */
  @Override
  public boolean test(Claim claim) {
    ClaimAdapter adapter = new ClaimAdapter(claim);

    return containsSamhsaIcdProcedureCode(adapter.getProcedure())
        || containsSamhsaIcdDiagnosisCode(adapter.getDiagnosis())
        || containsSamhsaLineItem(adapter.getItem());
  }

  /**
   * Checks if the given {@link CodeableConcept} contains only known coding systems.
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains only the
   *     carin HCPCS system, <code>false</code> otherwise.
   */
  @Override
  protected boolean containsOnlyKnownSystems(CodeableConcept procedureConcept) {
    return procedureConcept.getCoding().stream()
        .allMatch(c -> c.getSystem().equals(TransformerConstants.CODING_SYSTEM_CARIN_HCPCS));
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
