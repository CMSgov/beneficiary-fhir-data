## Overview

This proposal consolidates tables and aligns columns with their respective claim types to reduce join complexity and eliminate largely null fields.

The resulting model limits each claim type to **a maximum of three joins**.

### Claim Table Decomposition

Decompose the existing **CLAIM** table into the following claim-type–specific tables:

- **CLAIM_PROFESSIONAL_PAC**
- **CLAIM_PROFESSIONAL_ADJ**
- **CLAIM_INSTITUTIONAL_PAC**
- **CLAIM_INSTITUTIONAL_ADJ**
- **CLAIM_RX**

Only retain columns that are specific and applicable to each claim type.  
Column mappings can be found here:  
https://lucid.app/lucidchart/563d087a-04f2-49a2-aa2c-4bd376fec893/edit?view_items=1Vrrl88sfbsS&page=0_0&invitationId=inv_417f877f-f6ed-4296-8e5f-9dcf7f2749e5

### Part D Support

- Create a new table **CLAIM_RX** to support Part D claims.

### Date Signature Consolidation

Merge **CLAIM_DATE_SIGNATURE** into each claim-type–specific table:

- **CLAIM_PROFESSIONAL_PAC**
- **CLAIM_PROFESSIONAL_ADJ**
- **CLAIM_INSTITUTIONAL_PAC**
- **CLAIM_INSTITUTIONAL_ADJ**
- **CLAIM_RX**

---

## 2. Provider History Consolidation (Claim Level)

### Institutional Claims

Merge selected fields from **PROVIDER_HISTORY** into:

- **CLAIM_INSTITUTIONAL_PAC**
- **CLAIM_INSTITUTIONAL_ADJ**

Fields merged:
- CLM_ATNDG_PRVDR_NPI_NUM
- CLM_OPRTG_PRVDR_NPI_NUM
- CLM_RNDRG_PRVDR_NPI_NUM
- PRVDR_BLG_PRVDR_NPI_NUM
- PRVDR_RFRG_PRVDR_NPI_NUM
- CLM_OTHR_PRVDR_NPI_NUM

### Professional Claims

Merge selected fields from **PROVIDER_HISTORY** into:

- **CLAIM_PROFESSIONAL_PAC**
- **CLAIM_PROFESSIONAL_ADJ**

Fields merged:
- PRVDR_BLG_PRVDR_NPI_NUM
- PRVDR_RFRG_PRVDR_NPI_NUM
- CLM_OTHR_PRVDR_NPI_NUM

### RX Claims

Merge selected fields from **PROVIDER_HISTORY** into **CLAIM_RX**:

- PRVDR_PRSCRBNG_PRVDR_NPI_NUM

---

## 3. Institutional-Specific Consolidations

### CLAIM_FISS

**CLAIM_FISS** contains only Institutional PAC claims. Consolidate it into **CLAIM_INSTITUTIONAL_PAC**, moving the following fields:

- CLM_CRNT_STUS_CD
- CLM_PPS_IND

### CLAIM_ANSI_SIGNATURE

Consolidate **CLAIM_ANSI_SIGNATURE** into:

- **CLAIM_INSTITUTIONAL_PAC**
- **CLAIM_INSTITUTIONAL_ADJ**

> These columns are always null for Professional claims.

Fields moved:
- CLM_1_REV_CNTR_ANSI_GRP_CD
- CLM_2_REV_CNTR_ANSI_GRP_CD
- CLM_3_REV_CNTR_ANSI_GRP_CD
- CLM_4_REV_CNTR_ANSI_GRP_CD
- CLM_1_REV_CNTR_ANSI_RSN_CD
- CLM_2_REV_CNTR_ANSI_RSN_CD
- CLM_3_REV_CNTR_ANSI_RSN_CD
- CLM_4_REV_CNTR_ANSI_RSN_CD

### CLAIM_DCMTN

Consolidate **CLAIM_DCMTN** into:

- **CLAIM_INSTITUTIONAL_PAC**
- **CLAIM_INSTITUTIONAL_ADJ**

> These columns are always null for Professional claims.

Fields moved:
- CLM_NRLN_RIC_CD
- CLM_BNFT_ENHNCMT_1_CD
- CLM_BNFT_ENHNCMT_2_CD
- CLM_BNFT_ENHNCMT_3_CD
- CLM_BNFT_ENHNCMT_4_CD
- CLM_BNFT_ENHNCMT_5_CD
- CLM_NGACO_CPTATN_SW
- CLM_NGACO_PBPMT_SW
- CLM_NGACO_PDSCHRG_HCBS_SW
- CLM_NGACO_SNF_WVR_SW
- CLM_NGACO_TLHLTH_SW

### Claim Location History

Move **CLM_AUDT_TRL_STUS_CD** from **CLM_LCTN_HSTRY** into:

- **CLAIM_PROFESSIONAL_PAC**
- **CLAIM_PROFESSIONAL_ADJ**
- **CLAIM_INSTITUTIONAL_PAC**
- **CLAIM_INSTITUTIONAL_ADJ**

---

## 4. Claim Line Tables

Decompose **CLAIM_ITEM** into the following claim line tables:

- **CLAIM_LINE_PROFESSIONAL_ADJ**
- **CLAIM_LINE_PROFESSIONAL_PAC**
- **CLAIM_LINE_INSTITUTIONAL_ADJ**
- **CLAIM_LINE_INSTITUTIONAL_PAC**
- **CLAIM_LINE_RX**

Retain only columns specific to each claim line type.

---

## 5. Claim Line Consolidations

- Consolidate **CLAIM_LINE_DCMTN** into:
    - **CLAIM_LINE_PROFESSIONAL_PAC**
    - **CLAIM_LINE_PROFESSIONAL_ADJ**

- Consolidate **CLAIM_VAL**, **CLAIM_LINE_FISS**, and **CLM_LINE_FISS_BNFT_SVG** into:
    - **CLAIM_LINE_INSTITUTIONAL_PAC**

---

## 6. Provider History Consolidation (Claim Line Level)

Merge **PROVIDER_HISTORY** into all claim line tables **except** **CLAIM_LINE_RX**:

- CLM_RNDRG_PRVDR_NPI_NUM

---

## Pulling Data from IDR

With the new model, additional nodes will be required for the newly created tables.

### Claim-Level Fetches

Claim data will be fetched in five separate steps:

- **CLAIM_PROFESSIONAL_PAC**
- **CLAIM_PROFESSIONAL_ADJ**
- **CLAIM_INSTITUTIONAL_PAC**
- **CLAIM_INSTITUTIONAL_ADJ**
- **CLAIM_RX**

### Claim Line Fetches

Claim line data will be fetched for:

- **CLAIM_LINE_PROFESSIONAL_ADJ**
- **CLAIM_LINE_PROFESSIONAL_PAC**
- **CLAIM_LINE_INSTITUTIONAL_ADJ**
- **CLAIM_LINE_INSTITUTIONAL_PAC**
- **CLAIM_LINE_RX**

---

## Source Tables by Target Table

### CLAIM_PROFESSIONAL_ADJ / CLAIM_PROFESSIONAL_PAC

> Approximately 10 fields differ between the two tables.

https://lucid.app/lucidchart/563d087a-04f2-49a2-aa2c-4bd376fec893/edit?beaconFlowId=303ED6B077DFCE23&invitationId=inv_417f877f-f6ed-4296-8e5f-9dcf7f2749e5&page=0_0#

Source tables:
- v2_mdcr_clm
- v2_mdcr_clm_dt_sgntr
- v2_mdcr_clm_rlt_cond_sgntr_mbr
- v2_mdcr_clm_prfnl
- v2_mdcr_prvdr_hstry
- v2_mdcr_clm_lctn_hstry

### CLAIM_INSTITUTIONAL_ADJ

> Approximately 21 fields differ between Institutional ADJ and PAC.

Source tables:
- v2_mdcr_clm
- v2_mdcr_clm_dt_sgntr
- v2_mdcr_clm_rlt_cond_sgntr_mbr
- v2_mdcr_clm_instnl
- v2_mdcr_prvdr_hstry
- v2_mdcr_clm_ansi_sgntr
- v2_mdcr_clm_dcmtn

### CLAIM_INSTITUTIONAL_PAC

Source tables:
- v2_mdcr_clm
- v2_mdcr_clm_dt_sgntr
- v2_mdcr_clm_rlt_cond_sgntr_mbr
- v2_mdcr_clm_instnl
- v2_mdcr_prvdr_hstry
- v2_mdcr_clm_ansi_sgntr
- v2_mdcr_clm_dcmtn
- v2_mdcr_clm_fiss

### CLAIM_RX

Source tables:
- v2_mdcr_clm
- v2_mdcr_clm_dt_sgntr
- v2_mdcr_clm_rlt_cond_sgntr_mbr
- v2_mdcr_prvdr_hstry

---

### CLAIM_LINE_PROFESSIONAL_ADJ

- v2_mdcr_clm_line
- v2_mdcr_clm_line_prfnl
- v2_mdcr_clm_line_dcmtn
- v2_mdcr_clm_prod
- v2_mdcr_prvdr_hstry

### CLAIM_LINE_PROFESSIONAL_PAC

- v2_mdcr_clm_line
- v2_mdcr_clm_line_prfnl
- v2_mdcr_clm_line_dcmtn
- v2_mdcr_clm_prod
- v2_mdcr_prvdr_hstry
- v2_mdcr_clm_line_mcs

### CLAIM_LINE_INSTITUTIONAL_ADJ

- v2_mdcr_clm_line
- v2_mdcr_clm_line_instnl
- v2_mdcr_clm_prod
- v2_mdcr_prvdr_hstry

### CLAIM_LINE_INSTITUTIONAL_PAC

- v2_mdcr_clm_line
- v2_mdcr_clm_line_instnl
- v2_mdcr_clm_prod
- v2_mdcr_clm_val
- v2_mdcr_prvdr_hstry
- v2_mdcr_clm_line_fiss
- v2_mdcr_clm_line_fiss_bnft_svg

### CLAIM_LINE_RX

> Most Part D fields are null in `v2_mdcr_clm_line`; see diagram for details.

- v2_mdcr_clm_line
- v2_mdcr_clm_line_rx
- v2_mdcr_clm_prod
- v2_mdcr_prvdr_hstry  
