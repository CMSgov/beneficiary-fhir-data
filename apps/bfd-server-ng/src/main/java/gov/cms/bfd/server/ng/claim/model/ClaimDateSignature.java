package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;

import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Getter
@Entity
@Table(name = "claim_date_signature", schema = "idr")
public class ClaimDateSignature {
  @Id
  @Column(name = "clm_dt_sgntr_sk")
  private long claimDateSignatureSk;

  @Embedded private ClaimDateSupportingInfo supportingInfo;
  @Embedded private ClaimProcessDate claimProcessDate;

  @JoinColumn(name = "clm_dt_sgntr_sk")
  @OneToOne
  private Claim claim;
}
