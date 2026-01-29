This redesign consolidates tables and aligns columns with their respective claim types to reduce join complexity and eliminate largely null fields.

The resulting model limits each claim type to a maximum of three joins.

- Decompose the existing **CLAIM** table into:

    - **CLAIM_PROFESSIONAL**
    - **CLAIM_INSTITUTIONAL**
    - **CLAIM_RX**

- Retain only columns that are specific and applicable to each claim type. The columns can be found here:
  https://lucid.app/lucidchart/563d087a-04f2-49a2-aa2c-4bd376fec893/edit?view_items=1Vrrl88sfbsS&page=0_0&invitationId=inv_417f877f-f6ed-4296-8e5f-9dcf7f2749e5

- Create a new table **CLAIM_RX** to support  Part D claims.

- Merge **CLAIM_DATE_SIGNATURE** into each claim-type–specific table:

    - CLAIM_PROFESSIONAL
    - CLAIM_INSTITUTIONAL
    - CLAIM_RX


#### 2. Provider History Consolidation (Claim Level)

- Merge selected fields from **PROVIDER_HISTORY** into **CLAIM_INSTITUTIONAL**:
    - CLM_ATNDG_PRVDR_NPI_NUM
    - CLM_OPRTG_PRVDR_NPI_NUM
    - CLM_RNDRG_PRVDR_NPI_NUM
    - PRVDR_BLG_PRVDR_NPI_NUM
    - PRVDR_RFRG_PRVDR_NPI_NUM
    - CLM_OTHR_PRVDR_NPI_NUM

- Merge selected fields from **PROVIDER_HISTORY** into **CLAIM_PROFESSIONAL**:
    - PRVDR_BLG_PRVDR_NPI_NUM
    - CLM_OTHR_PRVDR_NPI_NUM

- Merge selected fields from **PROVIDER_HISTORY** into **CLAIM_RX**:
    - PRVDR_PRSCRBNG_PRVDR_NPI_NUM
#### 3. Institutional-Specific Consolidations

- Consolidate **CLAIM_FISS** into **CLAIM_INSTITUTIONAL**, moving the following fields:
    - CLM_CRNT_STUS_CD
    - CLM_PPS_IND

- Consolidate **CLAIM_ANSI_SIGNATURE** into **CLAIM_INSTITUTIONAL**, including:

    - CLM_1_REV_CNTR_ANSI_GRP_CD
    - CLM_2_REV_CNTR_ANSI_GRP_CD
    - CLM_3_REV_CNTR_ANSI_GRP_CD
    - CLM_4_REV_CNTR_ANSI_GRP_CD
    - CLM_1_REV_CNTR_ANSI_RSN_CD
    - CLM_2_REV_CNTR_ANSI_RSN_CD
    - CLM_3_REV_CNTR_ANSI_RSN_CD
    - CLM_4_REV_CNTR_ANSI_RSN_CD

- Move **CLM_AUDT_TRL_STUS_CD** into both:

    - CLAIM_PROFESSIONAL
    - CLAIM_INSTITUTIONAL


#### 4. Claim Line Tables

- Decompose **CLAIM_ITEM** into:

    - **CLAIM_LINE_PROFESSIONAL**
    - **CLAIM_LINE_INSTITUTIONAL**
    - **CLAIM_LINE_RX**

- Retain only columns that are specific to each claim line type.

#### 5. Claim Line Consolidations

- Consolidate **CLAIM_LINE_DCMTN** into **CLAIM_LINE_PROFESSIONAL**.
- Consolidate **CLAIM_VAL**, **CLAIM_LINE_FISS**, and **CLM_LINE_FISS_BNFT_SVG** into **CLAIM_LINE_INSTITUTIONAL**.
#### 6. Provider History Consolidation (Claim Line Level)

- Merge **PROVIDER_HISTORY** into both **CLAIM_LINE_PROFESSIONAL** and **CLAIM_LINE_INSTITUTIONAL** for:
    - CLM_RNDRG_PRVDR_NPI_NUM