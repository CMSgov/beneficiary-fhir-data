package gov.cms.bfd.server.ng.claim.model;

import java.util.Comparator;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

// helper for ClaimToFhirTranslator.
final class ClaimToFhirTranslatorHelper {
  private ClaimToFhirTranslatorHelper() {}

  static ExplanationOfBenefit sortedEob(ExplanationOfBenefit eob) {
    eob.getCareTeam()
        .sort(Comparator.comparing(ExplanationOfBenefit.CareTeamComponent::getSequence));
    eob.getProcedure()
        .sort(Comparator.comparing(ExplanationOfBenefit.ProcedureComponent::getSequence));
    eob.getDiagnosis()
        .sort(Comparator.comparing(ExplanationOfBenefit.DiagnosisComponent::getSequence));
    eob.getSupportingInfo()
        .sort(
            Comparator.comparing(ExplanationOfBenefit.SupportingInformationComponent::getSequence));
    eob.getItem().sort(Comparator.comparing(ExplanationOfBenefit.ItemComponent::getSequence));
    return eob;
  }
}
