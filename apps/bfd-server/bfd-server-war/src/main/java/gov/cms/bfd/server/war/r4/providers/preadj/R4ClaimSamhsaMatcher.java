package gov.cms.bfd.server.war.r4.providers.preadj;

import gov.cms.bfd.server.war.r4.providers.AbstractSamhsaMatcher;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.List;
import java.util.function.Predicate;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Claim.DiagnosisComponent;
import org.hl7.fhir.r4.model.Claim.ProcedureComponent;
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
    return containsSamhsaIcdProcedureCode(claim.getProcedure())
        || containsSamhsaIcdCode(claim.getDiagnosis())
        || containsSamhsaLineItems(claim.getItem());
  }

  /**
   * @param procedure the {@link ProcedureComponent}s to check
   * @return <code>true</code> if any of the specified {@link ProcedureComponent}s match any of the
   *     {@link AbstractSamhsaMatcher#icd9ProcedureCodes} or {@link
   *     AbstractSamhsaMatcher#icd10ProcedureCodes} entries, <code>false</code> if they all do not
   */
  private boolean containsSamhsaIcdProcedureCode(List<ProcedureComponent> procedure) {
    return procedure.stream().anyMatch(this::isSamhsaIcdProcedure);
  }

  /**
   * @param diagnoses the {@link DiagnosisComponent}s to check
   * @return <code>true</code> if any of the specified {@link DiagnosisComponent}s match any of the
   *     {@link AbstractSamhsaMatcher#icd9DiagnosisCodes} or {@link
   *     AbstractSamhsaMatcher#icd10DiagnosisCodes} entries, <code>false</code> if they all do not
   */
  private boolean containsSamhsaIcdCode(List<DiagnosisComponent> diagnoses) {
    return diagnoses.stream().anyMatch(this::isSamhsaDiagnosis);
  }

  private boolean containsSamhsaLineItems(List<Claim.ItemComponent> items) {
    return items.stream().anyMatch(c -> containsSamhsaProcedureCode(c.getProductOrService()));
  }

  /**
   * @param procedure the {@link ProcedureComponent} to check
   * @return <code>true</code> if the specified {@link ProcedureComponent} matches one of the {@link
   *     AbstractSamhsaMatcher#icd9ProcedureCodes} or {@link
   *     AbstractSamhsaMatcher#icd10ProcedureCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaIcdProcedure(ProcedureComponent procedure) {
    try {
      return isSamhsaIcdProcedure(procedure.getProcedureCodeableConcept());
    } catch (FHIRException e) {
      /*
       * This will only be thrown if the ProcedureComponent doesn't have a
       * CodeableConcept, which isn't how we build ours.
       */
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * @param diagnosis the {@link DiagnosisComponent} to check
   * @return <code>true</code> if the specified {@link DiagnosisComponent} matches one of the {@link
   *     AbstractSamhsaMatcher#icd9DiagnosisCodes} or {@link
   *     AbstractSamhsaMatcher#icd10DiagnosisCodes}, or {@link AbstractSamhsaMatcher#drgCodes}
   *     entries, <code>
   *     false</code> if it does not
   */
  private boolean isSamhsaDiagnosis(DiagnosisComponent diagnosis) {
    try {
      return isSamhsaDiagnosis(diagnosis.getDiagnosisCodeableConcept())
          || isSamhsaPackageCode(diagnosis.getPackageCode());
    } catch (FHIRException e) {
      /*
       * This will only be thrown if the DiagnosisComponent doesn't have a
       * CodeableConcept, which isn't how we build ours.
       */
      throw new BadCodeMonkeyException(e);
    }
  }
}
