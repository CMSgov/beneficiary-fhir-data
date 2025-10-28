package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.Optional;

/** Pharmacy claim line table. */
@Getter
@Entity
@Table(name = "claim_line_rx", schema = "idr")
public class ClaimLineRx {
    @EmbeddedId
    ClaimLineRxId claimLineInstitutionalId;

    @Column(name = "clm_line_authrzd_fill_num")
    private String refillsAuthorized;

    @Column(name = "clm_line_days_suply_qty")
    private int daysSupply;

    @Column(name = "clm_line_rx_fill_num")
    private int fullNumber;

    @Column(name = "clm_rptd_mftr_dscnt_amt")
    private double gapDiscountAmount;

    @Column(name = "clm_line_vccn_admin_fee_amt")
    private double vaccineAdminFeeAmount;

    @Column(name = "clm_line_troop_tot_amt")
    private double otherTrueOutOfPockPaidAmount;

    @Column(name = "clm_line_srvc_cst_amt")
    private double dispensingFeeAmount;

    @Column(name = "clm_line_sls_tax_amt")
    private double salesTaxAmount;

    @Column(name = "clm_line_plro_amt")
    private double patientLiabReductOtherPaidAmount;

    @Column(name = "clm_line_lis_amt")
    private double lowIncomeCostShareSubAmount;

    @Column(name = "clm_line_ingrdnt_cst_amt")
    private double ingredientCostAmount;

    @Column(name = "clm_line_grs_blw_thrshld_amt")
    private double grossCostBelowThresholdAmount;

    @Column(name = "clm_line_grs_above_thrshld_amt")
    private double grossCostAboveThresholdAmount;

    @Column(name = "clm_brnd_gnrc_cd")
    private Optional<ClaimLineBrandGenericCode> brandGenericCode; //refactor to follow ClaimInstitutionalSupportingInfo

    @Column(name = "clm_cmpnd_cd")
    private Optional<ClaimLineCompoundCode> compoundCode;


    /*clm_ctstrphc_cvrg_ind_cd , catastrophicCovCode
            clm_daw_prod_slctn_cd , dispenseAsWritProdSelectCode
    clm_drug_cvrg_stus_cd , drugCovStatusCode
            clm_dspnsng_stus_cd , dispensingStatusCode
    clm_ltc_dspnsng_mthd_cd , submissionClarificationCode
            clm_phrmcy_srvc_type_cd , pharmacyServiceTypeCode
    clm_ptnt_rsdnc_cd , patientResidenceCode
            clm_line_rx_orgn_cd , prescriptionOriginCode
    clm_prcng_excptn_cd , pricingCode*/

    @OneToOne(mappedBy = "claimLineRx")
    private ClaimItem claimLine;
}
