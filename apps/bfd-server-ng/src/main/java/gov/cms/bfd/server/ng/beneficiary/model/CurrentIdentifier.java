package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;

/**
 * Represents the beneficiary's primary identity information (specifically MBI) as relevant for
 * populating a FHIR Coverage resource's identifier. This is simpler than the Patient Identity as it
 * typically deals with the beneficiary's single, current MBI.
 */
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CurrentIdentifier {

  @Column(name = "bene_mbi_id")
  private String mbi;

  /**
   * Transforms this identity information into a FHIR {@link org.hl7.fhir.r4.model.Identifier} for
   * the MBI. This identifier does not include a period, as it represents the beneficiary's primary
   * MBI in the context of the Coverage resource.
   *
   * @param assigner optional reference to the organization assigning this identifier; may be null
   * @return An {@link Optional} containing the FHIR {@link org.hl7.fhir.r4.model.Identifier} if an
   *     MBI is present, otherwise {@link Optional#empty()}.
   */
  public Optional<org.hl7.fhir.r4.model.Identifier> toFhir(@NonNull String assigner) {
    if (mbi.isEmpty()) {
      return Optional.empty();
    }

    org.hl7.fhir.r4.model.Identifier mbiIdentifier = new org.hl7.fhir.r4.model.Identifier();
    mbiIdentifier.setType(
        new CodeableConcept()
            .addCoding(
                new Coding(SystemUrls.HL7_IDENTIFIER, "MB", null) // "MB" for Member Number
                ));
    mbiIdentifier.setSystem(SystemUrls.CMS_MBI);
    mbiIdentifier.setValue(mbi);
    if (!assigner.isEmpty()) {
      mbiIdentifier.setAssigner(new Reference("Organization/" + assigner));
    }

    // No period is set here, as this represents the general MBI for the beneficiary
    // in the context of this Coverage.

    return Optional.of(mbiIdentifier);
  }
}
