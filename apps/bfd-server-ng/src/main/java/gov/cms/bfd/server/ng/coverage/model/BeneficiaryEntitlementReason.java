package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Entity representing BeneficiaryEntitlementReason. */
@Entity
@Getter
@Table(name = "beneficiary_entitlement_reason_latest", schema = "idr")
public class BeneficiaryEntitlementReason {

  @Id
  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_mdcr_entlmt_rsn_cd")
  private Optional<String> medicareEntitlementReasonCode;

  /**
   * create entitlement Status Extension.
   *
   * @return optional extension
   */
  public List<Extension> toFhir() {
    List<Extension> extensions = new ArrayList<>();

    medicareEntitlementReasonCode.ifPresent(
        validCode ->
            extensions.add(
                new Extension(SystemUrls.EXT_BENE_MDCR_ENTLMT_RSN_CD_URL)
                    .setValue(
                        new Coding(SystemUrls.SYS_BENE_MDCR_ENTLMT_RSN_CD, validCode, null))));
    return extensions;
  }
}
