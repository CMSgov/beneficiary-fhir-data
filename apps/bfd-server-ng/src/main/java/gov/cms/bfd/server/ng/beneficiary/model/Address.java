package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

/** Beneficiary address information. */
@Embeddable
public class Address {
  @Column(name = "geo_usps_state_cd")
  private Optional<String> stateCode;

  @Column(name = "geo_zip5_cd")
  private Optional<String> zipCode;

  @Column(name = "geo_zip_plc_name")
  private Optional<String> city;

  @Column(name = "bene_line_1_adr")
  private Optional<String> addressLine1;

  @Column(name = "bene_line_2_adr")
  private Optional<String> addressLine2;

  @Column(name = "bene_line_3_adr")
  private Optional<String> addressLine3;

  @Column(name = "bene_line_4_adr")
  private Optional<String> addressLine4;

  @Column(name = "bene_line_5_adr")
  private Optional<String> addressLine5;

  @Column(name = "bene_line_6_adr")
  private Optional<String> addressLine6;

  /**
   * Transform the address to its FHIR representation.
   *
   * @return FHIR address
   */
  public Optional<org.hl7.fhir.r4.model.Address> toFhir() {
    // This is a no-op currently since we don't have approval to share address data.
    // We should either remove or add these mappings later when a final decision is made.
    return Optional.empty();
  }
}
