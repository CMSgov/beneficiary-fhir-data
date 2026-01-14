package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

@Embeddable
public class ClaimOptional {
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimInstitutional claimInstitutional;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimProfessional claimProfessional;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_uniq_id")
  private ClaimFiss claimFiss;

  @Nullable
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "clm_sbmtr_cntrct_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "cntrct_num")
  @JoinColumn(
      name = "clm_sbmtr_cntrct_pbp_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "cntrct_pbp_num")
  private Contract contract;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_srvc_prvdr_gnrc_id_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory serviceProviderHistory;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_atndg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory attendingProviderHistory;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_oprtg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory operatingProviderHistory;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "prvdr_blg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory billingProviderHistory;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_rndrg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory renderingProviderHistory;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "prvdr_prscrbng_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory prescribingProviderHistory;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "prvdr_rfrg_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory referringProviderHistory;

  @Nullable
  @ManyToOne
  @JoinColumn(
      name = "clm_othr_prvdr_npi_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "prvdr_npi_num")
  private ProviderHistory otherProviderHistory;

  public Optional<ClaimInstitutional> getClaimInstitutional() {
    return Optional.ofNullable(claimInstitutional);
  }

  public Optional<ClaimProfessional> getClaimProfessional() {
    return Optional.ofNullable(claimProfessional);
  }

  public Optional<Contract> getContract() {
    return Optional.ofNullable(contract);
  }

  public Optional<ClaimFiss> getClaimFiss() {
    return Optional.ofNullable(claimFiss);
  }

  public Optional<ProviderHistory> getBillingProviderHistory() {
    return Optional.ofNullable(billingProviderHistory);
  }

  public Optional<ProviderHistory> getOperatingProviderHistory() {
    return Optional.ofNullable(operatingProviderHistory);
  }

  public Optional<ProviderHistory> getAttendingProviderHistory() {
    return Optional.ofNullable(attendingProviderHistory);
  }

  public Optional<ProviderHistory> getPrescribingProviderHistory() {
    return Optional.ofNullable(prescribingProviderHistory);
  }

  public Optional<ProviderHistory> getOtherProviderHistory() {
    return Optional.ofNullable(otherProviderHistory);
  }

  public Optional<ProviderHistory> getRenderingProviderHistory() {
    return Optional.ofNullable(renderingProviderHistory);
  }

  public Optional<ProviderHistory> getReferringProviderHistory() {
    return Optional.ofNullable(referringProviderHistory);
  }

  public Optional<ProviderHistory> getServiceProviderHistory() {
    return Optional.ofNullable(serviceProviderHistory);
  }
}
