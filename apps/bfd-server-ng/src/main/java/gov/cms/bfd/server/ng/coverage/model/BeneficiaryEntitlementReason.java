package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.*;
import java.util.Optional;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Entity representing BeneficiaryEntitlementReason. */
@Entity
@Getter
@Subselect(
    """
   SELECT DISTINCT ON (bene_sk) bene_sk,
     bene_rng_bgn_dt,
     bene_rng_end_dt,
     bene_mdcr_entlmt_rsn_cd,
     idr_ltst_trans_flg,
     idr_trans_efctv_ts,
     idr_trans_obslt_ts,
     idr_insrt_ts,
     idr_updt_ts,
     bfd_created_ts,
     bfd_updated_ts
    FROM idr.beneficiary_entitlement_reason
   WHERE idr_trans_obslt_ts >= '9999-12-31 00:00:00+00'::timestamp with time zone AND idr_ltst_trans_flg::text = 'Y'::text AND bene_rng_bgn_dt <= (now() - '12:00:00'::interval)
   ORDER BY bene_sk, bene_rng_bgn_dt DESC, idr_trans_efctv_ts DESC
   """)
@Immutable
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
  public Optional<Extension> toFhir() {

    return medicareEntitlementReasonCode.map(
        validCode ->
            new Extension(SystemUrls.EXT_BENE_MDCR_ENTLMT_RSN_CD_URL)
                .setValue(new Coding(SystemUrls.SYS_BENE_MDCR_ENTLMT_RSN_CD, validCode, null)));
  }
}
