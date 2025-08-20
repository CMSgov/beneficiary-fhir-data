package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Extension;

/** Entity representing Beneficiary Status. */
@Entity
@Getter
@Table(name = "beneficiary_status_current", schema = "idr")
public class BeneficiaryStatus {

  @Id
  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_mdcr_stus_cd")
  private Optional<MedicareStatusCode> medicareStatusCode;

  /**
   * create create Medicare Status Extension.
   *
   * @return optional extension
   */
  public List<Extension> toFhir() {
    return medicareStatusCode.map(MedicareStatusCode::toFhir).orElse(List.of());
  }
}
