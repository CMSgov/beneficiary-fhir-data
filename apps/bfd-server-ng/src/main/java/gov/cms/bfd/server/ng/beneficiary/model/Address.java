package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

@Embeddable
public class Address {
  @Column(name = "geo_usps_state_cd")
  private String stateCode;

  @Column(name = "geo_zip5_cd")
  private String zipCode;

  @Column(name = "geo_zip_plc_name")
  private String city;

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

  org.hl7.fhir.r4.model.Address toFhir() {
    var address =
        new org.hl7.fhir.r4.model.Address()
            .setState(stateCode)
            .setPostalCode(zipCode)
            .setCity(city);
    addressLine1.ifPresent(address::addLine);
    addressLine2.ifPresent(address::addLine);
    addressLine3.ifPresent(address::addLine);
    addressLine4.ifPresent(address::addLine);
    addressLine5.ifPresent(address::addLine);
    addressLine6.ifPresent(address::addLine);

    return address;
  }
}
