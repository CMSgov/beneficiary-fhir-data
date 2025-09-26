package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Entity representing beneficiary_third_party. */
@Entity
@Getter
@Table(name = "beneficiary_third_party_latest", schema = "idr")
public class BeneficiaryThirdParty {

  @EmbeddedId private BeneficiaryThirdPartyId id;

  @Column(name = "bene_buyin_cd")
  private Optional<String> buyInCode;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

  /**
   * create BuyIn Code Extension.
   *
   * @return optional extension
   */
  public Optional<Extension> toFhir() {
    return buyInCode.map(
        validCode ->
            new Extension(SystemUrls.EXT_BENE_BUYIN_CD_URL)
                .setValue(new Coding(SystemUrls.SYS_BENE_BUYIN_CD, validCode, null)));
  }
}
