# Blue Button API FHIR Data Model

This document contains (some) details on the Blue Button API's FHIR data model.

## Coding Systems

The following [Coding](http://www.hl7.org/implement/standards/fhir/datatypes.html#coding) systems can be found in API responses:

* Industry standards:
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/betos.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drg_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcfaspcl.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr.txt`
    * `https://www.cms.gov/Medicare/Coding/MedHCPCSGenInfo/index.html`
    * `urn:std:iso:4217`
    * `https://www.accessdata.fda.gov/scripts/cder/ndc`
    * `http://hl7.org/fhir/sid/us-npi`
    * `http://hl7.org/fhir/sid/icd-10`
    * `http://hl7.org/fhir/sid/icd-9-cm`
* Standard FHIR:
    * `http://hl7.org/fhir/v3/ActCode`
    * `http://hl7.org/fhir/ValueSet/v3-ActInvoiceGroupCode`
    * `http://hl7.org/fhir/benefit-category`
    * `http://hl7.org/fhir/claimcareteamrole`
    * `http://hl7.org/fhir/ex-claimtype`
* Systems specific to Medicare billing forms and/or the CCW itself:
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/race.txt`
    * `CMS Adjudications`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/type_adm.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/freq_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_id.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_type.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_day.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbtp.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hha_rfrl.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lupaind.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mcopdsw.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr_ndc_qty_qlfr_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/stus_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptntstus.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/astnt_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw1.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcngind.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/asgmntcd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ric_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/brnd_gnrc_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typcsrvcb.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dspnsng_stus_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/adjstmt_dltn_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ctstrphc_cvrg_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drug_cvrg_stus_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/daw_prod_slctn_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nstd_frmt_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptnt_rsdnc_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_orgn_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_excptn_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/submsn_clr_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/src_adms.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtdnlcd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typesrvc.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/revdedcd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/esrd_ind.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fac_type.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_sw.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/crec.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orec.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ms_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtindsw.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nopay_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lclty_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_st.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prtcptg.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvstate.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prv_type.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provzip.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/query_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/phrmcy_srvc_type_cd.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sup_type.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mtus_ind.txt`
    * `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpay_cd.txt`

## Identifiers

The following [Identifier](http://hl7.org/fhir/STU3/datatypes.html#identifier) systems can be found in API responses:

* `https://bluebutton.cms.gov/developer/docs/reference/some-thing` 
	* temporary placeholder for claim type link
* `http://bluebutton.cms.hhs.gov/identifier#bene_id`
* `CCW.BENE_ID`
* `http://bluebutton.cms.hhs.gov/identifier#hicnHash`
* `http://bluebutton.cms.hhs.gov/identifier#claimGroup`
    * Debatably, this could instead be categorized as CCW-specific.
* `CCW.PDE_ID`
* `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_num.txt`
* `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_line_clia_lab_num.txt`
* `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ccltrnum.txt`
* `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_pbp_rec_num.txt`
* `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_cntrct_rec_id.txt`
* `https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provider.txt`
* `CCW.RX_SRVC_RFRNC_NUM`