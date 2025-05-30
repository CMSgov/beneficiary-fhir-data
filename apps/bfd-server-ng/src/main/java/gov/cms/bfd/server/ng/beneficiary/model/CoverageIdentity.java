package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;

/**
 * Represents the beneficiary's primary identity information (specifically MBI) as relevant for
 * populating a FHIR Coverage resource's identifier. This is simpler than the Patient Identity as it
 * typically deals with the beneficiary's single, current MBI.
 */
@Getter
public class CoverageIdentity {

  private final Optional<String> mbi;

  /**
   * Constructs a CoverageBeneficiaryIdentity.
   *
   * @param mbi The beneficiary's current MBI. Can be null if not available.
   */
  public CoverageIdentity(String mbi) {
    this.mbi = Optional.ofNullable(mbi);
  }

  /**
   * Factory method to create a CoverageBeneficiaryIdentity from an MBI string.
   *
   * @param mbi The beneficiary's MBI.
   * @return A new CoverageIdentity instance.
   */
  public static CoverageIdentity from(String mbi) {
    return new CoverageIdentity(mbi);
  }

  /**
   * Transforms this identity information into a FHIR {@link Identifier} for the MBI. This
   * identifier does not include a period, as it represents the beneficiary's primary MBI in the
   * context of the Coverage resource.
   *
   * @return An {@link Optional} containing the FHIR {@link Identifier} if an MBI is present,
   *     otherwise {@link Optional#empty()}.
   */
  public Optional<Identifier> toFhirMbiIdentifier() {
    if (mbi.isEmpty()) {
      return Optional.empty();
    }

    Identifier mbiIdentifier = new Identifier();
    mbiIdentifier.setType(
        new CodeableConcept()
            .addCoding(
                new Coding(SystemUrls.HL7_IDENTIFIER, "MB", null) // "MB" for Member Number
                ));
    mbiIdentifier.setSystem(SystemUrls.CMS_MBI);
    mbiIdentifier.setValue(mbi.get());
    // No period is set here, as this represents the general MBI for the beneficiary
    // in the context of this Coverage.

    return Optional.of(mbiIdentifier);
  }

  /**
   * Gets the MBI string, if present.
   *
   * @return An {@link Optional} containing the MBI string.
   */
  public Optional<String> getMbiValue() {
    return mbi;
  }
}
