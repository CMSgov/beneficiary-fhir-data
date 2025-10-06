package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.*;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.jetbrains.annotations.NotNull;

/** Entity representing beneficiary_third_party. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "beneficiary_third_party_latest", schema = "idr")
public class BeneficiaryThirdParty implements Comparable<BeneficiaryThirdParty> {

  @EmbeddedId private BeneficiaryThirdPartyId id;

  @Column(name = "bene_buyin_cd")
  private Optional<String> buyInCode;

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

  @Override
  public int compareTo(@NotNull BeneficiaryThirdParty o) {
    return this.id.compareTo(o.id);
  }
}
