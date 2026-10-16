package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hl7.fhir.r4.model.Extension;

/** Entity representing Beneficiary Status. */
@Entity
@Getter
@Subselect(
"""
 SELECT DISTINCT ON (bene_sk) bene_sk,
    bene_mdcr_stus_cd,
    mdcr_stus_bgn_dt,
    mdcr_stus_end_dt,
    idr_ltst_trans_flg,
    idr_trans_efctv_ts,
    idr_trans_obslt_ts,
    idr_insrt_ts,
    idr_updt_ts,
    bfd_created_ts,
    bfd_updated_ts
   FROM idr.beneficiary_status
  WHERE idr_trans_obslt_ts >= '9999-12-31 00:00:00+00'::timestamp with time zone AND mdcr_stus_bgn_dt <= (now() - '12:00:00'::interval)
  ORDER BY bene_sk, mdcr_stus_bgn_dt DESC, idr_trans_efctv_ts DESC
""")
@Immutable
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
