package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.claim.model.Contract;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;

/** Contract PBP Contact table. */
@Entity
@Getter
@Table(name = "contract_pbp_contact", schema = "idr")
public class ContractPlanContactInfo {

  @Id
  @Column(name = "cntrct_pbp_sk", insertable = false, updatable = false)
  private long contractPbpSk;

  @Column(name = "cntrct_pbp_bgn_dt")
  private LocalDate contractPlanBeginDate;

  @Column(name = "cntrct_pbp_end_dt")
  private LocalDate contractPlanEndDate;

  @Column(name = "cntrct_plan_cntct_obslt_dt")
  private LocalDate contractPlanContactObsoleteDate;

  @Column(name = "cntrct_plan_cntct_type_cd")
  private Optional<String> contractPlanContactTypeCode;

  @Column(name = "cntrct_plan_free_extnsn_num")
  private Optional<String> contractPlanFreeExtensionNumber;

  @Column(name = "cntrct_plan_cntct_free_num")
  private Optional<String> contractPlanContactFreeNumber;

  @Column(name = "cntrct_plan_cntct_extnsn_num")
  private Optional<String> contractPlanContactExtensionNumber;

  @Column(name = "cntrct_plan_cntct_tel_num")
  private Optional<String> contractPlanContactNumber;

  @Column(name = "cntrct_plan_cntct_st_1_adr")
  private Optional<String> contractPlanAddressLine1;

  @Column(name = "cntrct_plan_cntct_st_2_adr")
  private Optional<String> contractPlanAddressLine2;

  @Column(name = "cntrct_plan_cntct_city_name")
  private Optional<String> contractPlanCity;

  @Column(name = "cntrct_plan_cntct_state_cd")
  private Optional<String> contractPlanStateCode;

  @Column(name = "cntrct_plan_cntct_zip_cd")
  private Optional<String> contractPlanZipCode;

  @OneToOne(mappedBy = "contractPlanContactInfo")
  private Contract contract;
}
