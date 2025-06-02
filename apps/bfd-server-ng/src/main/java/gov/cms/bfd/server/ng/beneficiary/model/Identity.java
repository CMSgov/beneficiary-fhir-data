package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;

/**
 * Represents the beneficiary's primary identity information (specifically MBI) as relevant for
 * populating a FHIR Coverage resource's identifier. This is simpler than the Patient Identity as it
 * typically deals with the beneficiary's single, current MBI.
 */
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Identity {

  // The id field is not actually used here, but JPA requires some unique ID for every entity, even
  // if it's just used for joins.
  // Ideally, we could use beneSk + mbi, but this becomes tricky because MBI may not always be
  // present.
  //  @Id Long rowId;
  @Column(name = "bene_mbi_id")
  private String mbi;

  //  /**
  //   * Constructs a CoverageBeneficiaryIdentity.
  //   *
  //   * <p>// * @param rowId row ID from the query that created this object
  //   *
  //   * @param mbi The beneficiary's current MBI. Can be null if not available.
  //   */
  //  public Identity(String mbi) {
  //    this.mbi = mbi;
  //    //    this.rowId = rowId;
  //  }

  //  /**
  //   * Factory method to create a CoverageBeneficiaryIdentity from an MBI string.
  //   *
  //   * @param mbi The beneficiary's MBI.
  //   * @return A new CoverageIdentity instance.
  //   */
  //  public static Identity from(String mbi) {
  //    return new Identity(mbi);
  //  }

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
    mbiIdentifier.setValue(mbi);
    // No period is set here, as this represents the general MBI for the beneficiary
    // in the context of this Coverage.

    return Optional.of(mbiIdentifier);
  }

  /**
   * Gets the MBI string, if present.
   *
   * @return An {@link Optional} containing the MBI string.
   */
  public String getMbiValue() {
    return mbi;
  }
}
