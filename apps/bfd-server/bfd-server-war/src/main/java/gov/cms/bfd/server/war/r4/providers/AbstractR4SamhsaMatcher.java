package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.commons.AbstractSamhsaMatcher;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractR4SamhsaMatcher<T> extends AbstractSamhsaMatcher<T> {

  // Valid system url for productOrService coding
  private static final Set<String> HCPCS_SYSTEM = Set.of(TransformerConstants.CODING_SYSTEM_HCPCS);
  // Additional valid coding system URL for backwards-compatibility
  // See: https://jira.cms.gov/browse/BFD-1345
  private static final Set<String> BACKWARDS_COMPATIBLE_HCPCS_SYSTEM =
      Set.of(
          TransformerConstants.CODING_SYSTEM_HCPCS,
          CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.HCPCS_CD));

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

    return codingSystems.equals(HCPCS_SYSTEM)
        || codingSystems.equals(BACKWARDS_COMPATIBLE_HCPCS_SYSTEM);
  }
}
