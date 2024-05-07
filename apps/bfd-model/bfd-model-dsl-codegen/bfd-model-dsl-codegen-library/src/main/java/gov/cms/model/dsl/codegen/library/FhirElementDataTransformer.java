package gov.cms.model.dsl.codegen.library;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.model.dsl.codegen.library.carin.C4BBClaimIdentifierType;
import java.util.Arrays;
import java.util.function.Supplier;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** POC. */
public class FhirElementDataTransformer {
  /**
   * Sets identifier on ExplanationOfBenefit.
   *
   * @param eob eob
   * @param identifierSupplier identifierValue
   * @param ccwMapping ccwMapping
   * @return this
   */
  public FhirElementDataTransformer setIdentifier(
      ExplanationOfBenefit eob, Supplier<Long> identifierSupplier, String ccwMapping) {
    Long identifierValue = identifierSupplier.get();
    String identifier = null;

    if (!ccwMapping.isEmpty()) {
      // PDE_ID / CLM_ID => ExplanationOfBenefit.identifier
      identifier = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.valueOf(ccwMapping));
    } else {
      // CLM_GRP_ID => ExplanationOfBenefit.identifier
      identifier = TransformerConstants.IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID;
    }
    eob.addIdentifier()
        .setSystem(identifier)
        .setValue(String.valueOf(identifierValue))
        .setType(createC4BBClaimCodeableConcept());
    return this;
  }

  /**
   * Helper function to create a {@link CodeableConcept} from a {@link C4BBClaimIdentifierType}.
   * Since this type only has one value this uses a hardcoded value.
   *
   * @return the codeable concept
   */
  static CodeableConcept createC4BBClaimCodeableConcept() {
    return new CodeableConcept()
        .setCoding(
            Arrays.asList(
                new Coding(
                    C4BBClaimIdentifierType.UC.getSystem(),
                    C4BBClaimIdentifierType.UC.toCode(),
                    C4BBClaimIdentifierType.UC.getDisplay())));
  }
}
