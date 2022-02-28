package gov.cms.bfd.server.war.r4.providers.preadj;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.r4.ClaimAdapter;
import gov.cms.bfd.server.war.commons.AbstractSamhsaMatcher;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

  // Valid system url for productOrService coding
  private static final Set<String> HCPCS_SYSTEM =
      Set.of(TransformerConstants.CODING_SYSTEM_CARIN_HCPCS);

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
   * <p>For v2 FHIR resources, we include backwards compatability for the
   * https://bluebutton.cms.gov/resources/variables/hcpcs_cd system.
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
