import csv
import sys
from numpy.random import choice


'''
This tool was used for remapping and formatting the files provided for
synthetic outpatient data in the keybase directory:

    /keybase/team/oeda_bfs/team/synthetic-data/outpatient-claims

  The following command line is accepted:

      $1 = Filename of the CSV type file.  Example, "file.txt"

It performs the following:

1. Uses a column header ordering list: header_enum_order_list
   This is taken from the OutpatientClaimColumn ENUM list in the following BFD
   code:
   ./apps/bfd-model/bfd-model-rif/target/generated-sources/annotations/
                       gov/cms/bfd/model/rif/OutpatientClaimColumn.java

   The RIF loader expects the columns in the files to be in this order.

2. Verifies all column headers from the input CSV file's top header row are
   all in the expected headers list: header_enum_order_list.

3. Verifies BENE_ID's are for sythetic.  Converts BENE_ID to negative.

4. Validates DATE fields. There have column header names ending in "_DT"
   such as "CLM_FROM_DT". Also those that end in "_DT" and a number, like
   "_DT1", "_DT2", "_DT11", ETC.

   Dates in the CSV files are in a format like "01APR1999". The RIF loader
   is expecting "01-Apr-1999".

   These are converted to the RIF loader format and will ERROR if not matching
   this format (and not blank).

5  Validates that the count of fields output is the same
   as header_enum_order_list.

6. If a column header from header_enum_order_list is not included in the file,
   the field entry will be included and empty in the output.

7. Strip leading and trailing whitespace for field entries.
   
     Was getting this error from the RIF processor:
           gov.cms.bfd.model.rif.parse.RifParsingUtils.parseDecimal
       Caused by: gov.cms.bfd.model.rif.parse.InvalidRifValueException:
            Unable to parse decimal value: ' 2267800412'.

8. For _AMT (amount) type decimal values, remove any commas (",").

     This is in reference to the following RIF loader error: 
         In: RifFilesProcessor.java:154
         gov.cms.bfd.model.rif.parse.InvalidRifValueException:
            gov.cms.bfd.model.rif.parse.InvalidRifValueException:
               Unable to parse decimal value: '2,000.00'.

9. For REV_CNTR_STUS_IND_CD with empty values, replace with a random value based
   on the distribution of production data. 

10. For PTNT_DSCHRG_STUS_CD with empty values, replace with 0 which indicates an unknown value.

'''

filename = sys.argv[1]

in_delimiter = ","

header_enum_order_list = ["DML_IND", "BENE_ID", "CLM_ID", "CLM_GRP_ID",
                          "FINAL_ACTION", "NCH_NEAR_LINE_REC_IDENT_CD",
                          "NCH_CLM_TYPE_CD", "CLM_FROM_DT", "CLM_THRU_DT",
                          "NCH_WKLY_PROC_DT", "FI_CLM_PROC_DT",
                          "CLAIM_QUERY_CODE", "PRVDR_NUM", "CLM_FAC_TYPE_CD",
                          "CLM_SRVC_CLSFCTN_TYPE_CD", "CLM_FREQ_CD", "FI_NUM",
                          "CLM_MDCR_NON_PMT_RSN_CD", "CLM_PMT_AMT",
                          "NCH_PRMRY_PYR_CLM_PD_AMT", "NCH_PRMRY_PYR_CD",
                          "PRVDR_STATE_CD", "ORG_NPI_NUM", "AT_PHYSN_UPIN",
                          "AT_PHYSN_NPI", "OP_PHYSN_UPIN", "OP_PHYSN_NPI",
                          "OT_PHYSN_UPIN", "OT_PHYSN_NPI", "CLM_MCO_PD_SW",
                          "PTNT_DSCHRG_STUS_CD", "CLM_TOT_CHRG_AMT",
                          "NCH_BENE_BLOOD_DDCTBL_LBLTY_AM",
                          "NCH_PROFNL_CMPNT_CHRG_AMT", "PRNCPAL_DGNS_CD",
                          "PRNCPAL_DGNS_VRSN_CD", "ICD_DGNS_CD1",
                          "ICD_DGNS_VRSN_CD1", "ICD_DGNS_CD2",
                          "ICD_DGNS_VRSN_CD2", "ICD_DGNS_CD3",
                          "ICD_DGNS_VRSN_CD3", "ICD_DGNS_CD4",
                          "ICD_DGNS_VRSN_CD4", "ICD_DGNS_CD5",
                          "ICD_DGNS_VRSN_CD5", "ICD_DGNS_CD6",
                          "ICD_DGNS_VRSN_CD6", "ICD_DGNS_CD7",
                          "ICD_DGNS_VRSN_CD7", "ICD_DGNS_CD8",
                          "ICD_DGNS_VRSN_CD8", "ICD_DGNS_CD9",
                          "ICD_DGNS_VRSN_CD9", "ICD_DGNS_CD10",
                          "ICD_DGNS_VRSN_CD10", "ICD_DGNS_CD11",
                          "ICD_DGNS_VRSN_CD11", "ICD_DGNS_CD12",
                          "ICD_DGNS_VRSN_CD12", "ICD_DGNS_CD13",
                          "ICD_DGNS_VRSN_CD13", "ICD_DGNS_CD14",
                          "ICD_DGNS_VRSN_CD14", "ICD_DGNS_CD15",
                          "ICD_DGNS_VRSN_CD15", "ICD_DGNS_CD16",
                          "ICD_DGNS_VRSN_CD16", "ICD_DGNS_CD17",
                          "ICD_DGNS_VRSN_CD17", "ICD_DGNS_CD18",
                          "ICD_DGNS_VRSN_CD18", "ICD_DGNS_CD19",
                          "ICD_DGNS_VRSN_CD19", "ICD_DGNS_CD20",
                          "ICD_DGNS_VRSN_CD20", "ICD_DGNS_CD21",
                          "ICD_DGNS_VRSN_CD21", "ICD_DGNS_CD22",
                          "ICD_DGNS_VRSN_CD22", "ICD_DGNS_CD23",
                          "ICD_DGNS_VRSN_CD23", "ICD_DGNS_CD24",
                          "ICD_DGNS_VRSN_CD24", "ICD_DGNS_CD25",
                          "ICD_DGNS_VRSN_CD25", "FST_DGNS_E_CD",
                          "FST_DGNS_E_VRSN_CD", "ICD_DGNS_E_CD1",
                          "ICD_DGNS_E_VRSN_CD1", "ICD_DGNS_E_CD2",
                          "ICD_DGNS_E_VRSN_CD2", "ICD_DGNS_E_CD3",
                          "ICD_DGNS_E_VRSN_CD3", "ICD_DGNS_E_CD4",
                          "ICD_DGNS_E_VRSN_CD4", "ICD_DGNS_E_CD5",
                          "ICD_DGNS_E_VRSN_CD5", "ICD_DGNS_E_CD6",
                          "ICD_DGNS_E_VRSN_CD6", "ICD_DGNS_E_CD7",
                          "ICD_DGNS_E_VRSN_CD7", "ICD_DGNS_E_CD8",
                          "ICD_DGNS_E_VRSN_CD8", "ICD_DGNS_E_CD9",
                          "ICD_DGNS_E_VRSN_CD9", "ICD_DGNS_E_CD10",
                          "ICD_DGNS_E_VRSN_CD10", "ICD_DGNS_E_CD11",
                          "ICD_DGNS_E_VRSN_CD11", "ICD_DGNS_E_CD12",
                          "ICD_DGNS_E_VRSN_CD12", "ICD_PRCDR_CD1",
                          "ICD_PRCDR_VRSN_CD1", "PRCDR_DT1",
                          "ICD_PRCDR_CD2", "ICD_PRCDR_VRSN_CD2",
                          "PRCDR_DT2", "ICD_PRCDR_CD3",
                          "ICD_PRCDR_VRSN_CD3", "PRCDR_DT3",
                          "ICD_PRCDR_CD4", "ICD_PRCDR_VRSN_CD4",
                          "PRCDR_DT4", "ICD_PRCDR_CD5",
                          "ICD_PRCDR_VRSN_CD5", "PRCDR_DT5",
                          "ICD_PRCDR_CD6", "ICD_PRCDR_VRSN_CD6",
                          "PRCDR_DT6", "ICD_PRCDR_CD7",
                          "ICD_PRCDR_VRSN_CD7", "PRCDR_DT7",
                          "ICD_PRCDR_CD8", "ICD_PRCDR_VRSN_CD8",
                          "PRCDR_DT8", "ICD_PRCDR_CD9",
                          "ICD_PRCDR_VRSN_CD9", "PRCDR_DT9",
                          "ICD_PRCDR_CD10", "ICD_PRCDR_VRSN_CD10",
                          "PRCDR_DT10", "ICD_PRCDR_CD11",
                          "ICD_PRCDR_VRSN_CD11", "PRCDR_DT11",
                          "ICD_PRCDR_CD12", "ICD_PRCDR_VRSN_CD12",
                          "PRCDR_DT12", "ICD_PRCDR_CD13",
                          "ICD_PRCDR_VRSN_CD13", "PRCDR_DT13",
                          "ICD_PRCDR_CD14", "ICD_PRCDR_VRSN_CD14",
                          "PRCDR_DT14", "ICD_PRCDR_CD15",
                          "ICD_PRCDR_VRSN_CD15", "PRCDR_DT15",
                          "ICD_PRCDR_CD16", "ICD_PRCDR_VRSN_CD16",
                          "PRCDR_DT16", "ICD_PRCDR_CD17",
                          "ICD_PRCDR_VRSN_CD17", "PRCDR_DT17",
                          "ICD_PRCDR_CD18", "ICD_PRCDR_VRSN_CD18",
                          "PRCDR_DT18", "ICD_PRCDR_CD19",
                          "ICD_PRCDR_VRSN_CD19", "PRCDR_DT19",
                          "ICD_PRCDR_CD20", "ICD_PRCDR_VRSN_CD20",
                          "PRCDR_DT20", "ICD_PRCDR_CD21",
                          "ICD_PRCDR_VRSN_CD21", "PRCDR_DT21",
                          "ICD_PRCDR_CD22", "ICD_PRCDR_VRSN_CD22",
                          "PRCDR_DT22", "ICD_PRCDR_CD23",
                          "ICD_PRCDR_VRSN_CD23", "PRCDR_DT23",
                          "ICD_PRCDR_CD24", "ICD_PRCDR_VRSN_CD24",
                          "PRCDR_DT24", "ICD_PRCDR_CD25",
                          "ICD_PRCDR_VRSN_CD25", "PRCDR_DT25",
                          "RSN_VISIT_CD1", "RSN_VISIT_VRSN_CD1",
                          "RSN_VISIT_CD2", "RSN_VISIT_VRSN_CD2",
                          "RSN_VISIT_CD3", "RSN_VISIT_VRSN_CD3",
                          "NCH_BENE_PTB_DDCTBL_AMT",
                          "NCH_BENE_PTB_COINSRNC_AMT",
                          "CLM_OP_PRVDR_PMT_AMT", "CLM_OP_BENE_PMT_AMT",
                          "CLM_LINE_NUM", "REV_CNTR", "REV_CNTR_DT",
                          "REV_CNTR_1ST_ANSI_CD", "REV_CNTR_2ND_ANSI_CD",
                          "REV_CNTR_3RD_ANSI_CD", "REV_CNTR_4TH_ANSI_CD",
                          "REV_CNTR_APC_HIPPS_CD", "HCPCS_CD",
                          "HCPCS_1ST_MDFR_CD", "HCPCS_2ND_MDFR_CD",
                          "REV_CNTR_PMT_MTHD_IND_CD", "REV_CNTR_DSCNT_IND_CD",
                          "REV_CNTR_PACKG_IND_CD", "REV_CNTR_OTAF_PMT_CD",
                          "REV_CNTR_IDE_NDC_UPC_NUM", "REV_CNTR_UNIT_CNT",
                          "REV_CNTR_RATE_AMT", "REV_CNTR_BLOOD_DDCTBL_AMT",
                          "REV_CNTR_CASH_DDCTBL_AMT",
                          "REV_CNTR_COINSRNC_WGE_ADJSTD_C",
                          "REV_CNTR_RDCD_COINSRNC_AMT",
                          "REV_CNTR_1ST_MSP_PD_AMT",
                          "REV_CNTR_2ND_MSP_PD_AMT", "REV_CNTR_PRVDR_PMT_AMT",
                          "REV_CNTR_BENE_PMT_AMT",
                          "REV_CNTR_PTNT_RSPNSBLTY_PMT",
                          "REV_CNTR_PMT_AMT_AMT", "REV_CNTR_TOT_CHRG_AMT",
                          "REV_CNTR_NCVRD_CHRG_AMT", "REV_CNTR_STUS_IND_CD",
                          "REV_CNTR_NDC_QTY", "REV_CNTR_NDC_QTY_QLFR_CD",
                          "RNDRNG_PHYSN_UPIN", "RNDRNG_PHYSN_NPI"]

valid_month_list = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]

claim_ids = []

# reading csv file
with open(filename, 'r') as csvfile:
    # creating a csv reader object
    csvreader = csv.DictReader(csvfile, delimiter=in_delimiter)

    # Output header line
    out_line = '|'.join(header_enum_order_list)
    print(out_line)

    row_count = 0
    # Iterate over all rows
    for row in csvreader:
        
        row_count = row_count + 1

        # 2. Verify all header keys are in the header_enum_order_list.
        if row_count == 1:
            for key in row:
                if key not in header_enum_order_list:
                    raise SystemExit("ERROR: Input file header key is not" +
                                     " in the header_enum_order_list list: "
                                     + key)

        # Output row fields based on enum mapping
        row_list = []
        for col_name in header_enum_order_list:
            # Is the col_name include in the input file? If not,make empty.
            if col_name in row:
                # 4. Convert and validate DATE fields.
                #
                #    NOTE: For our columns list this string contains is OK.
                #          However may want to use a REGEX search if reusing
                #          this code for another purpose!
                if "_DT" in col_name:
                    dt = row[col_name]
                    if dt != "":
                        # Convert from "01APR1999" to "01-Apr-1999" format.
                        new_dt = dt[0:2] + "-" \
                                    + dt[2:3]  \
                                    + dt[3:5].lower() \
                                    + "-" + dt[5:9]

                        # Validate month is OK:
                        if new_dt[3:6] not in valid_month_list:
                            raise SystemExit("ERROR: Month value in date"
                                             " field is not valid: "
                                             + col_name
                                             + "=" + row[col_name])
                        row_list.append(new_dt)
                    else:
                        row_list.append("")
                elif col_name == "BENE_ID" and int(row["BENE_ID"]) > 0:
                    # Convert BENE_ID to negative
                        row_list.append(str(int(row[col_name])*-1))
                elif "_AMT" in col_name:
                    # Remove commas from _AMT type field.
                    row_list.append(row[col_name].strip().replace(',',''))  
                elif col_name == "PTNT_DSCHRG_STUS_CD" and len(row[col_name]) == 0:
                    # Use a default 0 which means Unknown Value (but present in data)
                    row_list.append(str(0)) 
                else:
                    # Copy field as is.
                    row_list.append(row[col_name].strip())
            else:
                if col_name == "REV_CNTR_STUS_IND_CD":
                    # add this column based on a distribution from prod
                    rev_status = choice(["A", "B", "E", "K", "N", "S", "T", "V"],
                        p=[0.43, 0.05, 0.03, 0.01, 0.37, 0.06, 0.01, 0.04])
                    row_list.append(rev_status)
                else:
                    # col_name is not in row, so provide empty value
                    row_list.append("")

        # Validate row_list count is same as header_enum_order_list count.
        if len(row_list) != len(header_enum_order_list):
            raise SystemExit("ERROR: Was expecting: " +
                             str(len(header_enum_order_list)) +
                             "  field count but got: " + str(len(row_list)))

        # Output row to stdout
        print('|'.join(row_list))
