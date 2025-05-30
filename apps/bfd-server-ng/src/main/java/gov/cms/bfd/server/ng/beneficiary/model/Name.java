package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.Optional;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.StringType;

/** Beneficiary name information. */
@Embeddable
public class Name {
  @Column(name = "bene_1st_name")
  private String firstName;

  @Column(name = "bene_midl_name")
  private Optional<String> middleName;

  @Column(name = "bene_last_name")
  private String lastName;

  HumanName toFhir() {
    var givens = new ArrayList<StringType>();
    givens.add(new StringType(firstName));
    middleName.ifPresent(n -> givens.add(new StringType(n)));
    return new HumanName().setGiven(givens).setFamily(lastName);
  }
}
