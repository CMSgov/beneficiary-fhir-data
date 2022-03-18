package gov.cms.bfd.model.rda;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the McsDetails table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PreAdjMcsDetail.PK.class)
@Table(name = "mcs_details", schema = "part_adj")
public class PreAdjMcsDetail {
  @Id
  @Column(name = "idr_clm_hd_icn", length = 15, nullable = false)
  @EqualsAndHashCode.Include
  private String idrClmHdIcn;

  @Id
  @Column(name = "priority", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "idr_dtl_status", length = 1)
  private String idrDtlStatus;

  @Column(name = "idr_dtl_from_date")
  private LocalDate idrDtlFromDate;

  @Column(name = "idr_dtl_to_date")
  private LocalDate idrDtlToDate;

  @Column(name = "idr_proc_code", length = 5)
  private String idrProcCode;

  @Column(name = "idr_mod_one", length = 2)
  private String idrModOne;

  @Column(name = "idr_mod_two", length = 2)
  private String idrModTwo;

  @Column(name = "idr_mod_three", length = 2)
  private String idrModThree;

  @Column(name = "idr_mod_four", length = 2)
  private String idrModFour;

  @Column(name = "idr_dtl_diag_icd_type", length = 1)
  private String idrDtlDiagIcdType;

  @Column(name = "idr_dtl_primary_diag_code", length = 7)
  private String idrDtlPrimaryDiagCode;

  @Column(name = "idr_k_pos_lname_org", length = 60)
  private String idrKPosLnameOrg;

  @Column(name = "idr_k_pos_fname", length = 35)
  private String idrKPosFname;

  @Column(name = "idr_k_pos_mname", length = 25)
  private String idrKPosMname;

  @Column(name = "idr_k_pos_addr1", length = 55)
  private String idrKPosAddr1;

  @Column(name = "idr_k_pos_addr2_1st", length = 30)
  private String idrKPosAddr2_1st;

  @Column(name = "idr_k_pos_addr2_2nd", length = 25)
  private String idrKPosAddr2_2nd;

  @Column(name = "idr_k_pos_city", length = 30)
  private String idrKPosCity;

  @Column(name = "idr_k_pos_state", length = 2)
  private String idrKPosState;

  @Column(name = "idr_k_pos_zip", length = 15)
  private String idrKPosZip;

  @Column(name = "idr_tos", length = 1)
  private String idrTos;

  @Column(name = "idr_two_digit_pos", length = 2)
  private String idrTwoDigitPos;

  @Column(name = "idr_dtl_rend_type", length = 2)
  private String idrDtlRendType;

  @Column(name = "idr_dtl_rend_spec", length = 2)
  private String idrDtlRendSpec;

  @Column(name = "idr_dtl_rend_npi", length = 10)
  private String idrDtlRendNpi;

  @Column(name = "idr_dtl_rend_prov", length = 10)
  private String idrDtlRendProv;

  @Column(name = "idr_k_dtl_fac_prov_npi", length = 10)
  private String idrKDtlFacProvNpi;

  @Column(name = "idr_dtl_amb_pickup_addres1", length = 25)
  private String idrDtlAmbPickupAddres1;

  @Column(name = "idr_dtl_amb_pickup_addres2", length = 20)
  private String idrDtlAmbPickupAddres2;

  @Column(name = "idr_dtl_amb_pickup_city", length = 20)
  private String idrDtlAmbPickupCity;

  @Column(name = "idr_dtl_amb_pickup_state", length = 2)
  private String idrDtlAmbPickupState;

  @Column(name = "idr_dtl_amb_pickup_zipcode", length = 9)
  private String idrDtlAmbPickupZipcode;

  @Column(name = "idr_dtl_amb_dropoff_name", length = 24)
  private String idrDtlAmbDropoffName;

  @Column(name = "idr_dtl_amb_dropoff_addr_l1", length = 25)
  private String idrDtlAmbDropoffAddrL1;

  @Column(name = "idr_dtl_amb_dropoff_addr_l2", length = 20)
  private String idrDtlAmbDropoffAddrL2;

  @Column(name = "idr_dtl_amb_dropoff_city", length = 20)
  private String idrDtlAmbDropoffCity;

  @Column(name = "idr_dtl_amb_dropoff_state", length = 2)
  private String idrDtlAmbDropoffState;

  @Column(name = "idr_dtl_amb_dropoff_zipcode", length = 9)
  private String idrDtlAmbDropoffZipcode;

  @Column(name = "last_updated")
  private Instant lastUpdated;

  /** PK class for the McsDetails table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String idrClmHdIcn;

    private short priority;
  }
}
