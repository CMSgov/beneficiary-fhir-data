import pandas as pd
import json
import sys
from dataclasses import dataclass, field, asdict

prvdr_info_file = 'sample-data/PRVDR_HSTRY_POC.csv'
df = pd.read_csv(prvdr_info_file, dtype={"PRVDR_SK": str})
df.head()

cur_sample = sys.argv[1]
cur_sample_data = {}
with open(cur_sample, 'r') as file:
    cur_sample_data = json.load(file)

header_columns = {"PRVDR_BLG_PRVDR_NPI_NUM":"","PRVDR_RFRG_PRVDR_NPI_NUM":"referring","PRVDR_OTHR_PRVDR_NPI_NUM":"otheroperating","PRVDR_ATNDG_PRVDR_NPI_NUM":"attending","PRVDR_OPRTG_PRVDR_NPI_NUM":"operating","PRVDR_RNDRNG_PRVDR_NPI_NUM":"rendering","PRVDR_PRSCRBNG_PRVDR_NPI_NUM":"prescribing","PRVDR_SRVC_PRVDR_NPI_NUM":""}
line_columns = {"PRVDR_RNDRNG_PRVDR_NPI_NUM":"rendering","CLM_LINE_ORDRG_PRVDR_NPI_NUM":"","CLM_FAC_PRVDR_NPI_NUM":""}
npis_used = []
cur_sample_data['providerList'] = []
cur_careteam_sequence = 1
#we only use PRVDR_SRVC_PRVDR_NPI_NUM for part D events.
if(cur_sample_data['CLM_TYPE_CD'] not in (1,2,3,4)):
    header_columns.pop('PRVDR_SRVC_PRVDR_NPI_NUM')

populate_fields_except_na = ['PRVDR_LGL_NAME','PRVDR_OSCAR_NUM','PRVDR_LAST_NAME','PRVDR_1ST_NAME','PRVDR_MDL_NAME','PRVDR_TYPE_CD']
provider_list = []

#There may be an opportunity to consolidate even the duplicate NPIs into a single careTeam reference, but we should wait to get feedback on this
#The reason being: it's possible to lose context on rendering vs ordering 
for column in header_columns:
    if(column not in cur_sample_data):
        continue
    provider_object = {}
    provider_object['PRVDR_SK'] = cur_sample_data[column]
    if(provider_object['PRVDR_SK'] in npis_used):
        provider_object['isDuplicate']=True
    else:
        npis_used.append(provider_object['PRVDR_SK'])
    prvdr_hstry_for_npi = json.loads(df[df["PRVDR_SK"] == str(provider_object['PRVDR_SK'])].iloc[0].to_json())
    provider_object['NPI_TYPE'] = '2' if prvdr_hstry_for_npi['PRVDR_LGL_NAME'] is not None else '1'

    for fld in populate_fields_except_na:
        if(prvdr_hstry_for_npi[fld] is not None):
            provider_object[fld] = prvdr_hstry_for_npi[fld]

    if(prvdr_hstry_for_npi['PRVDR_TXNMY_CMPST_CD'] is not None ):
        taxonomy_codes = [prvdr_hstry_for_npi['PRVDR_TXNMY_CMPST_CD'][i:i+10] for i in range(0, len(prvdr_hstry_for_npi['PRVDR_TXNMY_CMPST_CD']), 10)]
        provider_object['taxonomyCodes'] = taxonomy_codes
        
    #assign care team type + sequence for header-level info
    
    if(len(header_columns[column]) > 0):
        provider_object['careTeamType'] = header_columns[column]
        provider_object['careTeamSequenceNumber'] = cur_careteam_sequence
        cur_careteam_sequence += 1
    
        for idx in range(0,len(cur_sample_data['lineItemComponents'])):
            #If there's a column that matches the header level NPI at the line level, then we want to populate the sequence.
            #Because there can be multiple rendering providers on line items, we need to ensure those NPIs match. 
            if(column in cur_sample_data['lineItemComponents'][idx] and cur_sample_data['lineItemComponents'][idx][column] == cur_sample_data[column] ):
                if('careTeamSequences' in cur_sample_data['lineItemComponents'][idx]):
                    cur_sample_data['lineItemComponents'][idx]['careTeamSequence'].append(provider_object['careTeamSequenceNumber'])
                else:
                    cur_sample_data['lineItemComponents'][idx]['careTeamSequence'] = [provider_object['careTeamSequenceNumber']]

    #We may want to remove this in the future, depending on requirements regarding address info.
    if(column == 'PRVDR_BLG_PRVDR_NPI_NUM' and 'CLM_BLG_PRVDR_ZIP5_CD' in cur_sample_data):
        provider_object['prvdr_zip'] = cur_sample_data['CLM_BLG_PRVDR_ZIP5_CD']

    provider_list.append(provider_object)


#There can be line item NPIs that are not present at header level, but need to be added to the CareTeam. This populates those.
for line in range(0,len(cur_sample_data['lineItemComponents'])):
    for line_col in line_columns:
        if (line_col in cur_sample_data['lineItemComponents'][line] and cur_sample_data['lineItemComponents'][line][line_col] not in npis_used):
            npi = cur_sample_data['lineItemComponents'][line][line_col]
            print(npi)
            provider_object = {}
            provider_object['PRVDR_SK'] = cur_sample_data['lineItemComponents'][line][line_col]
            npis_used.append(provider_object['PRVDR_SK'])
            prvdr_hstry_for_npi = json.loads(df[df["PRVDR_SK"] == str(provider_object['PRVDR_SK'])].iloc[0].to_json())
            provider_object['NPI_TYPE'] = '2' if prvdr_hstry_for_npi['PRVDR_LGL_NAME'] is not None else '1'
            for fld in populate_fields_except_na:
                if(prvdr_hstry_for_npi[fld] is not None):
                    provider_object[fld] = prvdr_hstry_for_npi[fld]
            if(prvdr_hstry_for_npi['PRVDR_TXNMY_CMPST_CD'] is not None):
                taxonomy_codes = [prvdr_hstry_for_npi['PRVDR_TXNMY_CMPST_CD'][i:i+10] for i in range(0, len(prvdr_hstry_for_npi['PRVDR_TXNMY_CMPST_CD']), 10)]
                provider_object['taxonomyCodes'] = taxonomy_codes
            if(len(line_columns[line_col]) > 0):
                provider_object['careTeamType'] = line_columns[line_col]
                provider_object['careTeamSequenceNumber'] = cur_careteam_sequence
                cur_careteam_sequence += 1
            cur_sample_data['lineItemComponents'][line]['careTeamSequence'] = [provider_object['careTeamSequenceNumber']]
            provider_list.append(provider_object)
                
        elif(line_col in cur_sample_data['lineItemComponents'][line] and 'careTeamSequence' not in cur_sample_data['lineItemComponents'][line] and cur_sample_data['lineItemComponents'][line][line_col] in npis_used ):
            npi = cur_sample_data['lineItemComponents'][line][line_col]
            careTeamSequence = [x['careTeamSequenceNumber'] for x in provider_list if 'careTeamSequenceNumber' in x and x['PRVDR_SK'] == npi and 'careTeamType' in x and x['careTeamType'] == line_columns[line_col]][0]
            cur_sample_data['lineItemComponents'][line]['careTeamSequence'] = [careTeamSequence]
            print(cur_sample_data['lineItemComponents'][line])
cur_sample_data['providerList'] = provider_list

#diagnoses section
@dataclass
class Diagnosis:
    CLM_DGNS_CD: str
    CLM_PROD_TYPE_CD: str = 'D'
    CLM_POA_IND: str = '~'
    CLM_DGNS_PRCDR_ICD_IND: str = '0'
    ROW_NUM: str = '1'
    clm_prod_type_cd_map: list[str] = field(default_factory=list)

diagnosis_codes = [
    Diagnosis(CLM_DGNS_CD = x['CLM_DGNS_CD'], CLM_POA_IND = x['CLM_POA_IND'], 
              CLM_DGNS_PRCDR_ICD_IND = x['CLM_DGNS_PRCDR_ICD_IND'],
             ROW_NUM = x['CLM_VAL_SQNC_NUM'])
    for x in cur_sample_data["diagnoses"]
    if x["CLM_PROD_TYPE_CD"] == "D"
]
#of note, 1 and E appear to always be the same, so we only care about the E code. 
clm_prod_type_cds = ['P','A','R','E']
#this loop ensures that 'rogue' (eg principal not present in main list) diagnoses are not missed.
for clm_prod_type_cd in clm_prod_type_cds:
    code = [x['CLM_DGNS_CD'] for x in cur_sample_data['diagnoses'] if x['CLM_PROD_TYPE_CD'] == clm_prod_type_cd]
    if code and code[0] not in [x.CLM_DGNS_CD for x in diagnosis_codes]:
        diagnosis = Diagnosis(CLM_DGNS_CD = code, CLM_PROD_TYPE_CD = clm_prod_type_cd,
                              CLM_DGNS_PRCDR_ICD_IND = diagnosis_codes[0].CLM_DGNS_PRCDR_ICD_IND,
                             ROW_NUM = str(len(diagnosis_codes)+1))
        diagnosis_codes.append(diagnosis)

for diagnosis_code in diagnosis_codes:
    for clm_prod_type_cd in clm_prod_type_cds:
        cur_code = [x['CLM_DGNS_CD'] for x in cur_sample_data['diagnoses'] if x['CLM_PROD_TYPE_CD'] == clm_prod_type_cd]
        if cur_code and cur_code[0] == diagnosis_code.CLM_DGNS_CD:
            diagnosis_code.clm_prod_type_cd_map.append(clm_prod_type_cd)
    if len(diagnosis_code.clm_prod_type_cd_map) == 0:
        diagnosis_code.clm_prod_type_cd_map.append('D')

cur_sample_data['diagnoses']=[asdict(d) for d in diagnosis_codes]

filename = "out/temporary-sample.json"

with open(filename, 'w') as f:
    json.dump(cur_sample_data, f, indent=4)

print("Temporary augmented file created")
