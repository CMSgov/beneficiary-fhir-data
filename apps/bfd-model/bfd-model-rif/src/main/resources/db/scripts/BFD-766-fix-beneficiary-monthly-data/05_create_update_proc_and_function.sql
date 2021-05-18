--
-- Create PROCEDURE update_bene_monthly
--
CREATE OR REPLACE PROCEDURE update_bene_monthly(
    bene_id                     "BeneficiaryMonthly"."parentBeneficiary"%TYPE,
    yr_month                    "BeneficiaryMonthly"."yearMonth"%TYPE,
    fips_cnty_code              "BeneficiaryMonthly"."fipsStateCntyCode"%TYPE,
    medi_status_code            "BeneficiaryMonthly"."medicareStatusCode"%TYPE,
    buy_in_ind                  "BeneficiaryMonthly"."entitlementBuyInInd"%TYPE,
    hmo_ind                     "BeneficiaryMonthly"."hmoIndicatorInd"%TYPE,
    partc_contract_number_id    "BeneficiaryMonthly"."partCContractNumberId"%TYPE,
    partc_pbp_number_id         "BeneficiaryMonthly"."partCPbpNumberId"%TYPE,
    partc_plan_type             "BeneficiaryMonthly"."partCPlanTypeCode"%TYPE,
    partd_contract_number_id    "BeneficiaryMonthly"."partDContractNumberId"%TYPE,
    partd_pbp_number_id         "BeneficiaryMonthly"."partDPbpNumberId"%TYPE,
    partd_segment_num           "BeneficiaryMonthly"."partDSegmentNumberId"%TYPE,
    partd_retiree_mnthly        "BeneficiaryMonthly"."partDRetireeDrugSubsidyInd"%TYPE,
    partd_low_inc_cost_share    "BeneficiaryMonthly"."partDLowIncomeCostShareGroupCode"%TYPE,
    dual_elig_code              "BeneficiaryMonthly"."medicaidDualEligibilityCode"%TYPE
)
AS
$$
BEGIN
    IF     fips_cnty_code            IS NOT NULL
        OR medi_status_code          IS NOT NULL
        OR buy_in_ind                IS NOT NULL
        OR hmo_ind                   IS NOT NULL
        OR partc_contract_number_id  IS NOT NULL
        OR partc_pbp_number_id       IS NOT NULL
        OR partc_plan_type           IS NOT NULL
        OR partd_contract_number_id  IS NOT NULL
        OR partd_pbp_number_id       IS NOT NULL
        OR partd_segment_num         IS NOT NULL
        OR partd_retiree_mnthly      IS NOT NULL
        OR partd_low_inc_cost_share  IS NOT NULL
        OR dual_elig_code            IS NOT NULL
    THEN
        UPDATE public."BeneficiaryMonthly"
        SET
            "fipsStateCntyCode"                 = fips_cnty_code,
            "medicareStatusCode"                = medi_status_code,
            "entitlementBuyInInd"               = buy_in_ind,
            "hmoIndicatorInd"                   = hmo_ind,
            "partCContractNumberId"             = partc_contract_number_id,
            "partCPbpNumberId"                  = partc_pbp_number_id,
            "partCPlanTypeCode"                 = partc_plan_type,
            "partDContractNumberId"             = partd_contract_number_id,
            "partDPbpNumberId"                  = partd_pbp_number_id,
            "partDSegmentNumberId"              = partd_segment_num,
            "partDRetireeDrugSubsidyInd"        = partd_retiree_mnthly,
            "partDLowIncomeCostShareGroupCode"  = partd_low_inc_cost_share,
            "medicaidDualEligibilityCode"       = dual_elig_code
        WHERE
            "parentBeneficiary" = bene_id
        AND
            "yearMonth" = yr_month;

        IF NOT FOUND THEN
            INSERT INTO public."BeneficiaryMonthly"
            VALUES(
                bene_id,
                yr_month,
                fips_cnty_code,
                medi_status_code,
                buy_in_ind,
                hmo_ind,
                partc_contract_number_id,
                partc_pbp_number_id,
                partc_plan_type,
                partd_contract_number_id,
                partd_pbp_number_id,
                partd_segment_num,
                partd_retiree_mnthly,
                dual_elig_code,
                partd_low_inc_cost_share
            );
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION update_bene_monthly_with_delete(
   IN in_year varchar
)
returns INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
  rcd           CCW_LOAD_TEMP%ROWTYPE;
  rcd_cnt       INTEGER := 0;
  Jan1          DATE := TO_DATE(in_year || '-01-01', 'YYYY-MM-DD');
  Feb1          DATE := TO_DATE(in_year || '-02-01', 'YYYY-MM-DD');
  Mar1          DATE := TO_DATE(in_year || '-03-01', 'YYYY-MM-DD');
  Apr1          DATE := TO_DATE(in_year || '-04-01', 'YYYY-MM-DD');
  May1          DATE := TO_DATE(in_year || '-05-01', 'YYYY-MM-DD');
  Jun1          DATE := TO_DATE(in_year || '-06-01', 'YYYY-MM-DD');
  Jul1          DATE := TO_DATE(in_year || '-07-01', 'YYYY-MM-DD');
  Aug1          DATE := TO_DATE(in_year || '-08-01', 'YYYY-MM-DD');
  Sep1          DATE := TO_DATE(in_year || '-09-01', 'YYYY-MM-DD');
  Oct1          DATE := TO_DATE(in_year || '-10-01', 'YYYY-MM-DD');
  Nov1          DATE := TO_DATE(in_year || '-11-01', 'YYYY-MM-DD');
  Dec1          DATE := TO_DATE(in_year || '-12-01', 'YYYY-MM-DD');
  
  -- Modeled after Postgresql Trnasaction Management
  --
  -- https://www.postgresql.org/docs/11/plpgsql-transactions.html
 --
BEGIN
    FOR rcd IN SELECT * FROM public.CCW_LOAD_TEMP WHERE RFRNC_YR = in_year LIMIT 20000
    LOOP
        -- Jan
        call update_bene_monthly(
            rcd.BENE_ID,
            Jan1,
            rcd.FIPS_STATE_CNTY_JAN_CD,
            rcd.MDCR_STUS_JAN_CD,
            rcd.MDCR_ENTLMT_BUYIN_1_IND,
            rcd.HMO_1_IND,
            rcd.PTC_CNTRCT_JAN_ID,
            rcd.PTC_PBP_JAN_ID,
            rcd.PTC_PLAN_TYPE_JAN_CD,
            rcd.PTD_CNTRCT_JAN_ID,
            rcd.PTD_PBP_JAN_ID,
            rcd.PTD_SGMT_JAN_ID,
            rcd.RDS_JAN_IND,
            rcd.CST_SHR_GRP_JAN_CD,
            rcd.META_DUAL_ELGBL_STUS_JAN_CD);
    
        -- Feb
        call update_bene_monthly(
            rcd.BENE_ID,
            Feb1,
            rcd.FIPS_STATE_CNTY_FEB_CD,
            rcd.MDCR_STUS_FEB_CD,
            rcd.MDCR_ENTLMT_BUYIN_2_IND,
            rcd.HMO_2_IND,
            rcd.PTC_CNTRCT_FEB_ID,
            rcd.PTC_PBP_FEB_ID,
            rcd.PTC_PLAN_TYPE_FEB_CD,
            rcd.PTD_CNTRCT_FEB_ID,
            rcd.PTD_PBP_FEB_ID,
            rcd.PTD_SGMT_FEB_ID,
            rcd.RDS_FEB_IND,
            rcd.CST_SHR_GRP_FEB_CD,
            rcd.META_DUAL_ELGBL_STUS_FEB_CD);
    
        -- Mar
        call update_bene_monthly(
            rcd.BENE_ID,
            Mar1,
            rcd.FIPS_STATE_CNTY_MAR_CD,
            rcd.MDCR_STUS_MAR_CD,
            rcd.MDCR_ENTLMT_BUYIN_3_IND,
            rcd.HMO_3_IND,
            rcd.PTC_CNTRCT_MAR_ID,
            rcd.PTC_PBP_MAR_ID,
            rcd.PTC_PLAN_TYPE_MAR_CD,
            rcd.PTD_CNTRCT_MAR_ID,
            rcd.PTD_PBP_MAR_ID,
            rcd.PTD_SGMT_MAR_ID,
            rcd.RDS_MAR_IND,
            rcd.CST_SHR_GRP_MAR_CD,
            rcd.META_DUAL_ELGBL_STUS_MAR_CD);
    
        -- Apr
        call update_bene_monthly(
            rcd.BENE_ID,
            Apr1,
            rcd.FIPS_STATE_CNTY_APR_CD,
            rcd.MDCR_STUS_APR_CD,
            rcd.MDCR_ENTLMT_BUYIN_4_IND,
            rcd.HMO_4_IND,
            rcd.PTC_CNTRCT_APR_ID,
            rcd.PTC_PBP_APR_ID,
            rcd.PTC_PLAN_TYPE_APR_CD,
            rcd.PTD_CNTRCT_APR_ID,
            rcd.PTD_PBP_APR_ID,
            rcd.PTD_SGMT_APR_ID,
            rcd.RDS_APR_IND,
            rcd.CST_SHR_GRP_APR_CD,
            rcd.META_DUAL_ELGBL_STUS_APR_CD);
    
        -- May
        call update_bene_monthly(
            rcd.BENE_ID,
            May1,
            rcd.FIPS_STATE_CNTY_MAY_CD,
            rcd.MDCR_STUS_MAY_CD,
            rcd.MDCR_ENTLMT_BUYIN_5_IND,
            rcd.HMO_5_IND,
            rcd.PTC_CNTRCT_MAY_ID,
            rcd.PTC_PBP_MAY_ID,
            rcd.PTC_PLAN_TYPE_MAY_CD,
            rcd.PTD_CNTRCT_MAY_ID,
            rcd.PTD_PBP_MAY_ID,
            rcd.PTD_SGMT_MAY_ID,
            rcd.RDS_MAY_IND,
            rcd.CST_SHR_GRP_MAY_CD,
            rcd.META_DUAL_ELGBL_STUS_MAY_CD);
    
        -- Jun
        call update_bene_monthly(
            rcd.BENE_ID,
            Jun1,
            rcd.FIPS_STATE_CNTY_JUN_CD,
            rcd.MDCR_STUS_JUN_CD,
            rcd.MDCR_ENTLMT_BUYIN_6_IND,
            rcd.HMO_6_IND,
            rcd.PTC_CNTRCT_JUN_ID,
            rcd.PTC_PBP_JUN_ID,
            rcd.PTC_PLAN_TYPE_JUN_CD,
            rcd.PTD_CNTRCT_JUN_ID,
            rcd.PTD_PBP_JUN_ID,
            rcd.PTD_SGMT_JUN_ID,
            rcd.RDS_JUN_IND,
            rcd.CST_SHR_GRP_JUN_CD,
            rcd.META_DUAL_ELGBL_STUS_JUN_CD);
    
        -- Jul
        call update_bene_monthly(
            rcd.BENE_ID,
            Jul1,
            rcd.FIPS_STATE_CNTY_JUL_CD,
            rcd.MDCR_STUS_JUL_CD,
            rcd.MDCR_ENTLMT_BUYIN_7_IND,
            rcd.HMO_7_IND,
            rcd.PTC_CNTRCT_JUL_ID,
            rcd.PTC_PBP_JUL_ID,
            rcd.PTC_PLAN_TYPE_JUL_CD,
            rcd.PTD_CNTRCT_JUL_ID,
            rcd.PTD_PBP_JUL_ID,
            rcd.PTD_SGMT_JUL_ID,
            rcd.RDS_JUL_IND,
            rcd.CST_SHR_GRP_JUL_CD,
            rcd.META_DUAL_ELGBL_STUS_JUL_CD);
    
        -- Aug
        call update_bene_monthly(
            rcd.BENE_ID,
            Aug1,
            rcd.FIPS_STATE_CNTY_AUG_CD,
            rcd.MDCR_STUS_AUG_CD,
            rcd.MDCR_ENTLMT_BUYIN_8_IND,
            rcd.HMO_8_IND,
            rcd.PTC_CNTRCT_AUG_ID,
            rcd.PTC_PBP_AUG_ID,
            rcd.PTC_PLAN_TYPE_AUG_CD,
            rcd.PTD_CNTRCT_AUG_ID,
            rcd.PTD_PBP_AUG_ID,
            rcd.PTD_SGMT_AUG_ID,
            rcd.RDS_AUG_IND,
            rcd.CST_SHR_GRP_AUG_CD,
            rcd.META_DUAL_ELGBL_STUS_AUG_CD);
    
        -- Sept
        call update_bene_monthly(
            rcd.BENE_ID,
            Sep1,
            rcd.FIPS_STATE_CNTY_SEPT_CD,
            rcd.MDCR_STUS_SEPT_CD,
            rcd.MDCR_ENTLMT_BUYIN_9_IND,
            rcd.HMO_9_IND,
            rcd.PTC_CNTRCT_SEPT_ID,
            rcd.PTC_PBP_SEPT_ID,
            rcd.PTC_PLAN_TYPE_SEPT_CD,
            rcd.PTD_CNTRCT_SEPT_ID,
            rcd.PTD_PBP_SEPT_ID,
            rcd.PTD_SGMT_SEPT_ID,
            rcd.RDS_SEPT_IND,
            rcd.CST_SHR_GRP_SEPT_CD,
            rcd.META_DUAL_ELGBL_STUS_SEPT_CD);
    
        -- Oct
        call update_bene_monthly(
            rcd.BENE_ID,
            Oct1,
            rcd.FIPS_STATE_CNTY_OCT_CD,
            rcd.MDCR_STUS_OCT_CD,
            rcd.MDCR_ENTLMT_BUYIN_10_IND,
            rcd.HMO_10_IND,
            rcd.PTC_CNTRCT_OCT_ID,
            rcd.PTC_PBP_OCT_ID,
            rcd.PTC_PLAN_TYPE_OCT_CD,
            rcd.PTD_CNTRCT_OCT_ID,
            rcd.PTD_PBP_OCT_ID,
            rcd.PTD_SGMT_OCT_ID,
            rcd.RDS_OCT_IND,
            rcd.CST_SHR_GRP_OCT_CD,
            rcd.META_DUAL_ELGBL_STUS_OCT_CD);
    
        -- Nov
        call update_bene_monthly(
            rcd.BENE_ID,
            Nov1,
            rcd.FIPS_STATE_CNTY_NOV_CD,
            rcd.MDCR_STUS_NOV_CD,
            rcd.MDCR_ENTLMT_BUYIN_11_IND,
            rcd.HMO_11_IND,
            rcd.PTC_CNTRCT_NOV_ID,
            rcd.PTC_PBP_NOV_ID,
            rcd.PTC_PLAN_TYPE_NOV_CD,
            rcd.PTD_CNTRCT_NOV_ID,
            rcd.PTD_PBP_NOV_ID,
            rcd.PTD_SGMT_NOV_ID,
            rcd.RDS_NOV_IND,
            rcd.CST_SHR_GRP_NOV_CD,
            rcd.META_DUAL_ELGBL_STUS_NOV_CD);
    
        -- Dec
        call update_bene_monthly(
            rcd.BENE_ID,
            Dec1,
            rcd.FIPS_STATE_CNTY_DEC_CD,
            rcd.MDCR_STUS_DEC_CD,
            rcd.MDCR_ENTLMT_BUYIN_12_IND,
            rcd.HMO_12_IND,
            rcd.PTC_CNTRCT_DEC_ID,
            rcd.PTC_PBP_DEC_ID,
            rcd.PTC_PLAN_TYPE_DEC_CD,
            rcd.PTD_CNTRCT_DEC_ID,
            rcd.PTD_PBP_DEC_ID,
            rcd.PTD_SGMT_DEC_ID,
            rcd.RDS_DEC_IND,
            rcd.CST_SHR_GRP_DEC_CD,
            rcd.META_DUAL_ELGBL_STUS_DEC_CD);

        DELETE FROM CCW_LOAD_TEMP WHERE BENE_ID = rcd.BENE_ID AND RFRNC_YR = in_year;
        rcd_cnt := rcd_cnt + 1;
    END LOOP;
    RETURN rcd_cnt;
END;
$$;