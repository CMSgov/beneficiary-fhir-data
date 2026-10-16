package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.*;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.jetbrains.annotations.NotNull;

/** Entity representing beneficiary_third_party. */
@Entity
@Getter
@EqualsAndHashCode
@Subselect(
"""
 SELECT DISTINCT ON (bene_sk, bene_tp_type_cd) bene_sk,
    bene_buyin_cd,
    bene_tp_type_cd,
    bene_rng_bgn_dt,
    bene_rng_end_dt,
    idr_ltst_trans_flg,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_insrt_ts,
    idr_updt_ts,
    bfd_created_ts,
    bfd_updated_ts
   FROM idr.beneficiary_third_party
  WHERE idr_trans_obslt_ts >= '9999-12-31 00:00:00+00'::timestamp with time zone AND bene_rng_bgn_dt <= (now() - '12:00:00'::interval)
  ORDER BY bene_sk, bene_tp_type_cd, bene_rng_bgn_dt DESC, idr_trans_efctv_ts DESC
""")
@Immutable
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
