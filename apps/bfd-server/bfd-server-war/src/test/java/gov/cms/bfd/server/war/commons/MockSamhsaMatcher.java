package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import java.util.HashSet;
import java.util.Set;

/** Mock class for testing the default implementations of the {@link AbstractSamhsaMatcher}. */
public class MockSamhsaMatcher extends AbstractSamhsaMatcher {

  /** Instantiates a new Mock samhsa matcher with no data set. */
  public MockSamhsaMatcher() {
    super(
        new HashSet<>(),
        new HashSet<>(),
        new HashSet<>(),
        new HashSet<>(),
        new HashSet<>(),
        new HashSet<>());
  }

  /**
   * Instantiates a new Mock samhsa matcher with the specified data set.
   *
   * @param cptCodes the cpt codes
   * @param drgCodes the drg codes
   * @param icd9ProcedureCodes the icd 9 procedure codes
   * @param icd9DiagnosisCodes the icd 9 diagnosis codes
   * @param icd10ProcedureCodes the icd 10 procedure codes
   * @param icd10DiagnosisCodes the icd 10 diagnosis codes
   */
  protected MockSamhsaMatcher(
      Set<String> cptCodes,
      Set<String> drgCodes,
      Set<String> icd9ProcedureCodes,
      Set<String> icd9DiagnosisCodes,
      Set<String> icd10ProcedureCodes,
      Set<String> icd10DiagnosisCodes) {
    super(
        cptCodes,
        drgCodes,
        icd9ProcedureCodes,
        icd9DiagnosisCodes,
        icd10ProcedureCodes,
        icd10DiagnosisCodes);
  }

  /** {@inheritDoc} */
  @Override
  protected boolean containsOnlyKnownSystems(CodeableConcept procedureConcept) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean test(Object o) {
    return false;
  }
}
