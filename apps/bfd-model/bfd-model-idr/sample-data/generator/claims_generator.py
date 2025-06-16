import random
import copy
import string
import argparse
import os 
import json
import sys
import subprocess
import pandas as pd
from pathlib import Path
from dateutil.parser import parse
from datetime import date, datetime, timedelta


def save_output_files(clm,clm_line,clm_val,clm_dt_sgntr,clm_prod,clm_instnl,clm_line_instnl,clm_dcmtn):
    Path("out").mkdir(exist_ok=True)
    df = pd.json_normalize(clm)
    df['CLM_BLOOD_PT_FRNSH_QTY'] = df['CLM_BLOOD_PT_FRNSH_QTY'].astype('Int64')
    df.to_csv('out/SYNTHETIC_CLM.csv', index=False)
    df = pd.json_normalize(clm_line)
    df.to_csv('out/SYNTHETIC_CLM_LINE.csv', index=False)
    df = pd.json_normalize(clm_val)
    df.to_csv('out/SYNTHETIC_CLM_VAL.csv', index=False)
    df = pd.json_normalize(clm_dt_sgntr)
    df.to_csv('out/SYNTHETIC_CLM_DT_SGNTR.csv', index=False)
    df = pd.json_normalize(clm_prod)
    df.to_csv('out/SYNTHETIC_CLM_PROD.csv', index=False)
    df = pd.json_normalize(clm_instnl)
    df.to_csv('out/SYNTHETIC_CLM_INSTNL.csv', index=False)
    df = pd.DataFrame(clm_line_instnl)
    df.to_csv('out/SYNTHETIC_CLM_LINE_INSTNL.csv', index=False)
    df = pd.json_normalize(clm_dcmtn)
    df.to_csv('out/SYNTHETIC_CLM_DCMTN.csv', index=False)

claims_to_generate_per_person = 5

fiss_clm_type_cds = [1011,1041,1012,1013,1014,1022,1023,1034,1071,1072,1073,1074,1075,1076,1077,1083,1085,1087,1089,1032,1033,1081,1082,1021,1018,2011,2041,2012,2013,2014,2022,2023,2034,2071,2072,2073,2074,2075,2076,2077,2083,2085,2087,2089,2032,2033,2081,2082,2021,2018]
institutional_claim_types = [10,20,30,40,50,60,61,62,63,64]+fiss_clm_type_cds

type_1_npis = [1942945159,1437702123,1972944437,1447692959,1558719914,1730548868,1023051596,1003488552,1720749690]
type_2_npis = [1093792350,1548226988,1477643690,1104867175,1669572467,1508565987,1649041195]
avail_oscar_codes_institutional = ['39T14','000000','001500','001502','001503','001504','001505','001509','001510']

code_systems = {}

available_icd_10_codes = ['W6162','V972','V970','W5922XA','Z631',
                          'W5541XA','Y92311','E1169','R465','V9733',
                          'Y931','R461','E0170','E0290','W5529', 'W213',
                          'W5813XD','W303XXA']
available_procedure_codes_icd10_pcs = ['02HV33Z','5A1D70Z','30233N1','B2111ZZ','0BH17EZ',
                                       '4A023N7','5A09357','5A1955Z','5A1945Z']
proc_codes_cpt_hcpcs = ['99213','99453','J2270']
hcpcs_mods = ['1P','22','23','28','32','U6','US','PC','PD']
available_ndc = ['00338004904','00264180032','00338011704','00264180031','00264780020']
clm_poa_ind_choices = ['N','1','U','X','W','0','~','Z','Y','']


def run_command(cmd, cwd=None):
    try:
        result = subprocess.run(
            cmd,
            cwd=cwd,
            shell=True,
            check=True,
            text=True,
            capture_output=True
        )
        return result.stdout, result.stderr
    except subprocess.CalledProcessError as e:
        print("Error running command:",cmd)
        if(e.stderr):
            print("Error output:",e.stderr)
        else:
            print("Error info (not necessarily stderr):",e)
        sys.exit(1) #kill process and debug.

def random_date(start_date, end_date):
    start_formatted = date.fromisoformat(start_date).toordinal()
    end_formatted = date.fromisoformat(end_date).toordinal()
    rand_date = random.randint(start_formatted, end_formatted)
    return date.fromordinal(rand_date).isoformat()

def gen_thru_dt(frm_dt, max_days = 30):
    from_date = date.fromisoformat(frm_dt)
    days_to_add = random.randint(0,max_days)
    return (from_date + timedelta(days=days_to_add)).isoformat()

def add_days(input_dt, days_to_add = 0):
    return (date.fromisoformat(input_dt) + timedelta(days=days_to_add)).isoformat()

def add_diagnoses(clm_type_cd=-1):
    diagnosis_list = []
    num_diagnoses = 0
    if(clm_type_cd in (10,20,30,50,60,61,62,63,64)):
        #inpatient uses concepts of principal, admitting, other, external
        principal_diagnosis = {'CLM_DGNS_CD':random.choice(available_icd_10_codes),
                               'CLM_VAL_SQNC_NUM':"1",
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'P',
                               'CLM_POA_IND':'~'}
        first_diagnosis = {'CLM_DGNS_CD':principal_diagnosis['CLM_DGNS_CD'],
                               'CLM_VAL_SQNC_NUM':"1",
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'D',
                               'CLM_POA_IND':random.choice(clm_poa_ind_choices)
                               }
        admitting_diagnosis = {'CLM_DGNS_CD':random.choice(available_icd_10_codes),
                               'CLM_VAL_SQNC_NUM':"1",
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'A',
                               'CLM_POA_IND':'~'} 
        external_1 = {'CLM_DGNS_CD':random.choice(available_icd_10_codes),
                               'CLM_VAL_SQNC_NUM':"1",
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'E',
                               'CLM_POA_IND':random.choice(clm_poa_ind_choices)} 
        first_external = {'CLM_DGNS_CD':external_1['CLM_DGNS_CD'],
                               'CLM_VAL_SQNC_NUM':"1",
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'1',
                               'CLM_POA_IND':'~'} 
        diagnosis_list.append(principal_diagnosis)
        diagnosis_list.append(first_diagnosis)
        diagnosis_list.append(admitting_diagnosis)
        diagnosis_list.append(external_1)
        diagnosis_list.append(first_external)
        num_diagnoses = random.randint(2,15)
    elif(clm_type_cd == 40):
        #outpatient uses principal, other, external cause of injury, patient reason for visit
        principal_diagnosis = {'CLM_DGNS_CD':random.choice(available_icd_10_codes),
                               'CLM_VAL_SQNC_NUM':"1",
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'P',
                               'CLM_POA_IND':'~'}
        first_diagnosis = {'CLM_DGNS_CD':principal_diagnosis['CLM_DGNS_CD'],
                               'CLM_VAL_SQNC_NUM':"1",
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'D',
                               'CLM_POA_IND':'~'
                               }
        rfv_diag = {'CLM_DGNS_CD':principal_diagnosis['CLM_DGNS_CD'],
                               'CLM_VAL_SQNC_NUM':"1",
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'1',
                               'CLM_POA_IND':'~'} 
        diagnosis_list.append(principal_diagnosis)
        diagnosis_list.append(first_diagnosis)
        diagnosis_list.append(rfv_diag)
        num_diagnoses = random.randint(2,15)

    if(num_diagnoses>1 and clm_type_cd in (10,20,30,50,60,61,62,63,64)):
        for diagnosis_sqnc in range(2,num_diagnoses):
            diagnosis = {'CLM_DGNS_CD':random.choice(available_icd_10_codes),
                               'CLM_VAL_SQNC_NUM':diagnosis_sqnc,
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'D',
                               'CLM_POA_IND':random.choice(clm_poa_ind_choices)}
            diagnosis_list.append(diagnosis)
    elif(clm_type_cd == 40):
        for diagnosis_sqnc in range(2,num_diagnoses):
            diagnosis = {'CLM_DGNS_CD':random.choice(available_icd_10_codes),
                               'CLM_VAL_SQNC_NUM':diagnosis_sqnc,
                               'CLM_DGNS_PRCDR_ICD_IND':"0",
                               'CLM_PROD_TYPE_CD':'D'}
            diagnosis_list.append(diagnosis)


    return diagnosis_list


def gen_procedure_icd10pcs():
    procedure = {}
    procedure['CLM_PROD_TYPE_CD'] = 'S'
    procedure['CLM_PRCDR_CD'] = random.choice(available_procedure_codes_icd10_pcs)
    procedure['CLM_DGNS_PRCDR_ICD_IND'] = '0'
    return procedure


def gen_claim(bene_sk = '-1', minDate = '2018-01-01', maxDate = str(date.today())):
    claim = {'CLM':{},'CLM_LINE':[],'CLM_DT_SGNTR':{},'CLM_LINE_INSTNL':[], 'CLM_DCMTN':{}}
    clm_dt_sgntr = {}
    clm_dt_sgntr['CLM_DT_SGNTR_SK'] = ''.join(random.choices(string.digits, k=12))
    claim['CLM']['CLM_DT_SGNTR_SK'] = clm_dt_sgntr['CLM_DT_SGNTR_SK']
    claim['CLM']['CLM_UNIQ_ID'] = ''.join(random.choices(string.digits, k=13))
    #clm_type_cd = 60
    clm_type_cd = random.choice([40,60])
    claim['CLM']['CLM_TYPE_CD'] = clm_type_cd

    clm_src_id = -1
    if(clm_type_cd < 100):
        clm_src_id = 20000
    elif(clm_type_cd in fiss_clm_type_cds):
        clm_src_id = 21000
    claim['CLM']['CLM_SRC_ID'] = clm_src_id
    claim['CLM']['CLM_FROM_DT'] = random_date(minDate, maxDate)
    claim['CLM']['CLM_THRU_DT'] = gen_thru_dt(claim['CLM']['CLM_FROM_DT'])


    #NON-PDE
    claim['CLM']['CLM_CNTL_NUM'] = ''.join(random.choices(string.digits, k=14)) + ''.join(random.choices(string.ascii_uppercase,k=3))
    #PDE -> diff Claim control number process.

    if(claim['CLM']['CLM_TYPE_CD'] in (20,30,40,60,61,62,63,71,72)):
        claim['CLM']['CLM_BLOOD_PT_FRNSH_QTY'] = random.randint(0,20)
    
    claim['CLM']['CLM_NUM_SK'] = 1
    claim['CLM']['CLM_EFCTV_DT'] = str(date.today())
    claim['CLM']['CLM_IDR_LD_DT'] = str(date.today())
    claim['CLM']['CLM_OBSLT_DT'] = '9999-12-31'
    claim['CLM']['GEO_BENE_SK'] = ''.join(random.choices(string.digits, k=5))
    claim['CLM']['BENE_SK'] = bene_sk
    claim['CLM']['CLM_DISP_CD'] = random.choice(code_systems['CLM_DISP_CD'])
    claim['CLM']['CLM_QUERY_CD'] = random.choice(code_systems['CLM_QUERY_CD'])

    tob_code = random.choice(code_systems['CLM_BILL_FREQ_CD'])
    claim['CLM']['CLM_BILL_FAC_TYPE_CD'] = tob_code[0]
    claim['CLM']['CLM_BILL_CLSFCTN_CD'] = tob_code[1]
    claim['CLM']['CLM_BILL_FREQ_CD'] = tob_code[2]

    claim['CLM']['CLM_CNTRCTR_NUM'] = random.choice(code_systems['CLM_CNTRCTR_NUM'])
    claim['CLM']['CLM_NCH_PRMRY_PYR_CD'] = random.choice(code_systems['CLM_NCH_PRMRY_PYR_CD'])

    clm_finl_actn_ind = 'N'
    if(clm_type_cd in (10,20,30,40,50,60,61,62,63,71,72,81,82)):
        clm_finl_actn_ind = 'Y'
    elif (clm_type_cd >= 2000 and clm_type_cd <2800):
        clm_finl_actn_ind = random.choice(['Y','N'])
    claim['CLM']['CLM_FINL_ACTN_IND'] = clm_finl_actn_ind

    clm_ltst_clm_ind = 'N'
    if(clm_type_cd in (10,20,30,40,50,60,61,62,63,71,72,81,82)):
        clm_ltst_clm_ind = 'Y'
    claim['CLM']['CLM_LTST_CLM_IND'] = clm_ltst_clm_ind
    

    #CLM_RIC_CDs are generally tied to the claim type code.
    if(claim['CLM']['CLM_TYPE_CD'] in (20,30,50,60,61,62,63,64)):
        #part A!
        claim['CLM_DCMTN']['CLM_NRLN_RIC_CD'] = 'V' #inpatient
        claim['CLM_DCMTN']['CLM_DT_SGNTR_SK'] = claim['CLM']['CLM_DT_SGNTR_SK']
        claim['CLM_DCMTN']['CLM_NUM_SK'] = claim['CLM']['CLM_NUM_SK']
        claim['CLM_DCMTN']['GEO_BENE_SK'] = claim['CLM']['GEO_BENE_SK']
        claim['CLM_DCMTN']['CLM_TYPE_CD'] = claim['CLM']['CLM_TYPE_CD']
    elif(claim['CLM']['CLM_TYPE_CD'] == 40):
        #outpatient
        claim['CLM_DCMTN']['CLM_NRLN_RIC_CD'] = 'W' #outpatient
        claim['CLM_DCMTN']['CLM_DT_SGNTR_SK'] = claim['CLM']['CLM_DT_SGNTR_SK']
        claim['CLM_DCMTN']['CLM_NUM_SK'] = claim['CLM']['CLM_NUM_SK']
        claim['CLM_DCMTN']['GEO_BENE_SK'] = claim['CLM']['GEO_BENE_SK']
        claim['CLM_DCMTN']['CLM_TYPE_CD'] = claim['CLM']['CLM_TYPE_CD']

    #provider elements:
    if((clm_type_cd < 65 and clm_type_cd >= 10) or clm_type_cd in fiss_clm_type_cds):
        claim['CLM']['PRVDR_BLG_PRVDR_NPI_NUM'] = random.choice(type_2_npis)
        claim['CLM']['CLM_ATNDG_PRVDR_NPI_NUM'] = random.choice(type_1_npis)
        claim['CLM']['CLM_OPRTG_PRVDR_NPI_NUM'] = random.choice(type_1_npis)
        claim['CLM']['CLM_OTHR_PRVDR_NPI_NUM'] = random.choice(type_1_npis)
        claim['CLM']['CLM_RNDRG_PRVDR_NPI_NUM'] = random.choice(type_1_npis)
        claim['CLM']['CLM_BLG_PRVDR_OSCAR_NUM'] = random.choice(avail_oscar_codes_institutional)
        claim['CLM']['CLM_MDCR_COINSRNC_AMT'] = round(random.uniform(0,25),2)

    #generate claim header financial elements here
    claim['CLM']['CLM_SBMT_CHRG_AMT'] = round(random.uniform(1, 1000000),2)
    claim['CLM']['CLM_PMT_AMT'] = round(random.uniform(1, claim['CLM']['CLM_SBMT_CHRG_AMT']),2)
    claim['CLM']['CLM_MDCR_DDCTBL_AMT'] = round(random.uniform(1, 1676),2)
    claim['CLM']['CLM_NCVRD_CHRG_AMT'] = round(claim['CLM']['CLM_SBMT_CHRG_AMT']-claim['CLM']['CLM_PMT_AMT'],2)
    claim['CLM']['CLM_BLOOD_LBLTY_AMT'] = round(random.uniform(0,25),2)

    if(clm_type_cd in (40,71,72,81,82)):
        #be sure to check that DME claims meet the above.
        claim['CLM']['CLM_PRVDR_PMT_AMT'] = round(random.uniform(0,25),2)

    claim['CLM_VAL'] = []
    #CLM_OPRTNL_DSPRTNT_AMT + CLM_OPRTNL_IME_AMT
    if(claim['CLM']['CLM_TYPE_CD'] in (20,40,60,61,62,63,64)):
        #Note, this is a table we'll use sparsely, it appears. I've replaced the 5 key unique identifier with CLM_UNIQ_ID.
        clm_val_dsprtnt = {'CLM_DT_SGNTR_SK':claim['CLM']['CLM_DT_SGNTR_SK'],
                        'CLM_NUM_SK':claim['CLM']['CLM_NUM_SK'],
                        'GEO_BENE_SK':claim['CLM']['GEO_BENE_SK'],
                        'CLM_TYPE_CD':claim['CLM']['CLM_TYPE_CD'],
                        'CLM_VAL_CD':18,
                        'CLM_VAL_AMT':round(random.uniform(1,15000),2),
                        'CLM_VAL_SQNC_NUM':14}
        claim['CLM_VAL'].append(clm_val_dsprtnt)
        clm_val_ime = {'CLM_DT_SGNTR_SK':claim['CLM']['CLM_DT_SGNTR_SK'],
                       'CLM_NUM_SK':claim['CLM']['CLM_NUM_SK'],
                        'GEO_BENE_SK':claim['CLM']['GEO_BENE_SK'],
                        'CLM_TYPE_CD':claim['CLM']['CLM_TYPE_CD'],
                        'CLM_VAL_CD':19,
                        'CLM_VAL_AMT':round(random.uniform(1,15000),2),
                        'CLM_VAL_SQNC_NUM':3}
        claim['CLM_VAL'].append(clm_val_ime)


    #CHECKPOINT outpatient. 
    #Add procedures
    claim['CLM_PROD'] = []
    if(clm_type_cd == 60):
        num_procedures_to_add = random.randint(1,5)
        for proc in range(1,num_procedures_to_add):
            procedure = gen_procedure_icd10pcs()
            procedure['CLM_PRCDR_PRFRM_DT'] = random_date(claim['CLM']['CLM_FROM_DT'],claim['CLM']['CLM_THRU_DT'])
            procedure['CLM_VAL_SQNC_NUM'] = proc
            procedure['CLM_DT_SGNTR_SK'] = claim['CLM']['CLM_DT_SGNTR_SK']
            procedure['CLM_NUM_SK'] = claim['CLM']['CLM_NUM_SK']
            procedure['GEO_BENE_SK'] = claim['CLM']['GEO_BENE_SK']
            procedure['CLM_TYPE_CD'] = claim['CLM']['CLM_TYPE_CD']
            claim['CLM_PROD'].append(procedure)

    #add diagnoses
    diagnoses = add_diagnoses(clm_type_cd=clm_type_cd)
    for diagnosis in diagnoses:
        diagnosis['CLM_DT_SGNTR_SK'] = claim['CLM']['CLM_DT_SGNTR_SK']
        diagnosis['CLM_NUM_SK'] = claim['CLM']['CLM_NUM_SK']
        diagnosis['GEO_BENE_SK'] = claim['CLM']['GEO_BENE_SK']
        diagnosis['CLM_TYPE_CD'] = claim['CLM']['CLM_TYPE_CD']
        claim['CLM_PROD'].append(diagnosis)

    #clm_dt_sgntr info
    if(clm_type_cd in (10,20,30,50,60,61,62,63,64)):
        clm_dt_sgntr['CLM_ACTV_CARE_FROM_DT'] = claim['CLM']['CLM_FROM_DT']
        clm_dt_sgntr['CLM_DSCHRG_DT'] = claim['CLM']['CLM_THRU_DT']
        if(clm_type_cd in (50,60,61,62,63,64)):
            clm_dt_sgntr['CLM_MDCR_EXHSTD_DT'] = claim['CLM']['CLM_THRU_DT']
            if(clm_type_cd >= 60 ):
                clm_dt_sgntr['CLM_ACTV_CARE_THRU_DT'] = claim['CLM']['CLM_THRU_DT']
                if(random.choice([0,1])):
                    clm_dt_sgntr['CLM_NCVRD_FROM_DT'] = claim['CLM']['CLM_THRU_DT']
                    clm_dt_sgntr['CLM_NCVRD_THRU_DT'] = claim['CLM']['CLM_THRU_DT']
                else:
                    clm_dt_sgntr['CLM_NCVRD_FROM_DT'] = '1000-01-01'
                    clm_dt_sgntr['CLM_NCVRD_THRU_DT'] = '1000-01-01'


    clm_dt_sgntr['CLM_SUBMSN_DT'] = claim['CLM']['CLM_THRU_DT'] #This synthetic hospital is really on top of it!
    

    #clm_dt_sgntr['CLM_MDCR_NCH_PTNT_STUS_IND_CD'] = random.choice(code_systems['CLM_MDCR_NCH_PTNT_STUS_IND_CD'])
    clm_dt_sgntr['CLM_CMS_PROC_DT'] = claim['CLM']['CLM_THRU_DT']
    clm_dt_sgntr['CLM_NCH_WKLY_PROC_DT'] = claim['CLM']['CLM_THRU_DT']
    claim['CLM_DT_SGNTR'] = clm_dt_sgntr

    if(clm_type_cd in institutional_claim_types):
        institutional_parts = {}
        institutional_parts['GEO_BENE_SK'] = claim['CLM']['GEO_BENE_SK']
        institutional_parts['CLM_DT_SGNTR_SK'] = claim['CLM']['CLM_DT_SGNTR_SK']
        institutional_parts['CLM_TYPE_CD'] = claim['CLM']['CLM_TYPE_CD']
        institutional_parts['CLM_NUM_SK'] = claim['CLM']['CLM_NUM_SK']

        institutional_parts['CLM_FI_ACTN_CD'] = random.choice(code_systems['CLM_FI_ACTN_CD'])
        institutional_parts['CLM_ADMSN_TYPE_CD'] = random.choice(code_systems['CLM_ADMSN_TYPE_CD'])
        institutional_parts['BENE_PTNT_STUS_CD'] = random.choice(code_systems['BENE_PTNT_STUS_CD'])
        institutional_parts['CLM_MDCR_INSTNL_MCO_PD_SW'] = random.choice(code_systems['CLM_MDCR_INSTNL_MCO_PD_SW'])
        institutional_parts['CLM_ADMSN_SRC_CD'] = random.choice(code_systems['CLM_ADMSN_SRC_CD'])
        institutional_parts['DGNS_DRG_CD'] = random.randint(0,42)
        institutional_parts['CLM_INSTNL_CVRD_DAY_CNT'] = random.randint(0,10)
        institutional_parts['CLM_MDCR_IP_LRD_USE_CNT'] = random.randint(0,10)
        institutional_parts['CLM_INSTNL_PER_DIEM_AMT'] = round(random.uniform(0,350),2)
        institutional_parts['CLM_HIPPS_UNCOMPD_CARE_AMT'] = round(random.uniform(0,350),2)
        institutional_parts['CLM_MDCR_INSTNL_PRMRY_PYR_AMT'] = round(random.uniform(0,3500),2)
        institutional_parts['CLM_INSTNL_DRG_OUTLIER_AMT'] =  round(random.uniform(0,3500),2)
        institutional_parts['CLM_MDCR_IP_PPS_DSPRPRTNT_AMT'] = round(random.uniform(0,3500),2)
        institutional_parts['CLM_INSTNL_MDCR_COINS_DAY_CNT'] = random.randint(0,5)
        institutional_parts['CLM_INSTNL_NCVRD_DAY_CNT'] = random.randint(0,5)
        institutional_parts['CLM_MDCR_IP_PPS_DRG_WT_NUM'] = round(random.uniform(0.5,1.5),2)
        institutional_parts['CLM_MDCR_IP_PPS_EXCPTN_AMT'] = round(random.uniform(0,25),2)
        institutional_parts['CLM_MDCR_IP_PPS_CPTL_FSP_AMT'] = round(random.uniform(0,25),2)
        institutional_parts['CLM_MDCR_IP_PPS_CPTL_IME_AMT'] = round(random.uniform(0,25),2)
        institutional_parts['CLM_MDCR_IP_PPS_OUTLIER_AMT'] = round(random.uniform(0,25),2)
        institutional_parts['CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT'] = round(random.uniform(0,25),2)
        institutional_parts['CLM_MDCR_IP_PPS_CPTL_TOT_AMT'] = round(random.uniform(0,25),2)
        institutional_parts['CLM_MDCR_IP_BENE_DDCTBL_AMT'] = round(random.uniform(0,25),2)
        institutional_parts['CLM_PPS_IND_CD'] = random.choice(['','2'])
        if(clm_type_cd==40):
            institutional_parts['CLM_MDCR_INSTNL_BENE_PD_AMT'] = round(random.uniform(0,25),2)
        #We'll throw in a non-payment code on occasion 
        if(random.choice([0,10])>1):
            institutional_parts['CLM_MDCR_NPMT_RSN_CD'] = random.choice(code_systems['CLM_MDCR_NPMT_RSN_CD'])
        claim['CLM_INSTNL'] = institutional_parts


    num_clm_lines = random.randint(1,15)
    for line in range(num_clm_lines):
        claim_line = {}
        claim_line_inst = {}
        claim_line['GEO_BENE_SK'] = claim['CLM']['GEO_BENE_SK']
        claim_line['CLM_DT_SGNTR_SK'] = claim['CLM']['CLM_DT_SGNTR_SK']
        claim_line['CLM_TYPE_CD'] = claim['CLM']['CLM_TYPE_CD']
        claim_line['CLM_NUM_SK'] = claim['CLM']['CLM_NUM_SK']
        claim_line['CLM_FROM_DT'] = claim['CLM']['CLM_FROM_DT']
        claim_line_inst['GEO_BENE_SK'] = claim['CLM']['GEO_BENE_SK']
        claim_line_inst['CLM_DT_SGNTR_SK'] = claim['CLM']['CLM_DT_SGNTR_SK']
        claim_line_inst['CLM_TYPE_CD'] = claim['CLM']['CLM_TYPE_CD']
        claim_line_inst['CLM_NUM_SK'] = claim['CLM']['CLM_TYPE_CD']
        claim_line_inst['CLM_FROM_DT'] = claim['CLM']['CLM_FROM_DT']

        claim_line['CLM_LINE_HCPCS_CD'] = random.choice(proc_codes_cpt_hcpcs)
        num_mods = random.randint(0,5)
        if(num_mods):
            claim_line['HCPCS_1_MDFR_CD'] = random.choice(hcpcs_mods)
        if(num_mods>1):
            claim_line['HCPCS_2_MDFR_CD'] = random.choice(hcpcs_mods)
        if(num_mods>2):
            claim_line['HCPCS_3_MDFR_CD'] = random.choice(hcpcs_mods)
        if(num_mods>3):
            claim_line['HCPCS_4_MDFR_CD'] = random.choice(hcpcs_mods)
        if(num_mods>4):
            claim_line['HCPCS_5_MDFR_CD'] = random.choice(hcpcs_mods)
        if(random.choice([0,1])):
            claim_line['CLM_LINE_NDC_CD'] = random.choice(available_ndc)
            claim_line['CLM_LINE_NDC_QTY'] = round(random.uniform(1,1000),2)
            claim_line['CLM_LINE_NDC_QTY_QLFYR_CD'] = 'ML'
        claim_line['CLM_LINE_SRVC_UNIT_QTY'] = round(random.uniform(0,5),2)
        claim_line['CLM_LINE_REV_CTR_CD'] = random.choice(code_systems['CLM_REV_CNTR_CD'])
        claim_line['CLM_LINE_BENE_PMT_AMT'] = round(random.uniform(0,5),2)
        claim_line['CLM_LINE_BENE_PD_AMT'] = round(random.uniform(0,5),2)
        claim_line['CLM_LINE_ALOWD_CHRG_AMT'] = round(random.uniform(0,5),2)
        claim_line['CLM_LINE_SBMT_CHRG_AMT'] = round(random.uniform(0,5),2)
        claim_line['CLM_LINE_CVRD_PD_AMT'] = round(random.uniform(0,5),2)
        claim_line['CLM_LINE_BLOOD_DDCTBL_AMT'] = round(random.uniform(0,15),2)
        claim_line['CLM_LINE_MDCR_DDCTBL_AMT'] = round(random.uniform(0,5),2)

        claim_line['CLM_LINE_PRVDR_PMT_AMT'] = round(random.uniform(0,1500),2)
        claim_line['CLM_LINE_NCVRD_CHRG_AMT'] = round(random.uniform(0,1500),2)

        claim_line_inst['CLM_LINE_INSTNL_ADJSTD_AMT'] = round(random.uniform(0,1500),2)
        claim_line_inst['CLM_LINE_INSTNL_RDCD_AMT'] = round(random.uniform(0,1500),2)
        claim_line_inst['CLM_DDCTBL_COINSRNC_CD'] = random.choice(code_systems['CLM_DDCTBL_COINSRNC_CD'])
        claim_line_inst['CLM_LINE_INSTNL_RATE_AMT'] = round(random.uniform(0,15),2)
        claim_line_inst['CLM_LINE_INSTNL_MSP1_PD_AMT'] = round(random.uniform(0,15),2)
        claim_line_inst['CLM_LINE_INSTNL_MSP2_PD_AMT'] = round(random.uniform(0,2),2)
        claim_line_inst['CLM_LINE_INSTNL_REV_CTR_DT'] = claim['CLM']['CLM_FROM_DT']

        #In contrast to v2 DD this appears to populated in many.
        claim_line_inst['CLM_REV_DSCNT_IND_CD'] = random.choice(code_systems['CLM_REV_DSCNT_IND_CD'])
        claim_line_inst['CLM_OTAF_ONE_IND_CD'] = random.choice(code_systems['CLM_OTAF_ONE_IND_CD'])
        claim_line_inst['CLM_REV_PACKG_IND_CD'] = random.choice(code_systems['CLM_REV_PACKG_IND_CD'])
        claim_line_inst['CLM_REV_PMT_MTHD_CD'] = random.choice(code_systems['CLM_REV_PMT_MTHD_CD'])
        claim_line_inst['CLM_REV_CNTR_STUS_CD'] = random.choice(code_systems['CLM_REV_CNTR_STUS_CD'])
        claim_line_inst['CLM_ANSI_SGNTR_SK'] = random.choice(['8585','1','4365','1508','5555','9204','6857','5816','11978'])

        claim_line['CLM_UNIQ_ID'] = claim['CLM']['CLM_UNIQ_ID']
        claim_line['CLM_LINE_NUM'] = line
        claim_line_inst['CLM_LINE_NUM'] = line
        claim['CLM_LINE'].append(claim_line)
        claim['CLM_LINE_INSTNL'].append(claim_line_inst)

        #CLM_REV_APC_HIPPS_CD never populated for CLM_TYPE_CD 60 apart from null values (00000,0,~)
    return claim



def gen_pac_version_of_claim(claim):
    #note the fields to delete
    '''
    Generating a Synthetic PAC claim is done in a rather naive way. 
    1. Create a new CLM_UNIQ_ID
    2. Create a new 5 part key (eg GEO_BENE_SK, CLM_DT_SGNTR_SK)
    3. Update the relevant parts
    4. Delete information that's not accessible from that given source. This can probably be done via config files in the future.
    '''
    pac_claim = copy.deepcopy(claim)
    pac_claim['CLM']['CLM_UNIQ_ID'] = ''.join(random.choices(string.digits, k=13))

    if(pac_claim['CLM']['CLM_TYPE_CD'] in (60,61,62,63,64)):
        pac_claim['CLM']['CLM_TYPE_CD'] = random.choices([1011,2011,1041,2041],weights=[.48,.48,.02,.02])[0]
    
    if('CLM_BLOOD_PT_FRNSH_QTY' in pac_claim['CLM']):
        pac_claim['CLM'].pop('CLM_BLOOD_PT_FRNSH_QTY')
    pac_claim['CLM']['CLM_DT_SGNTR_SK'] = ''.join(random.choices(string.digits, k=12))
    pac_claim['CLM']['GEO_BENE_SK'] = ''.join(random.choices(string.digits, k=5))
    for i in range(len(pac_claim['CLM_LINE'])):
        pac_claim['CLM_LINE'][i]['CLM_UNIQ_ID'] = pac_claim['CLM']['CLM_UNIQ_ID']
        pac_claim['CLM_LINE'][i]['GEO_BENE_SK'] = pac_claim['CLM']['GEO_BENE_SK']
        pac_claim['CLM_LINE'][i]['CLM_DT_SGNTR_SK'] = pac_claim['CLM']['CLM_DT_SGNTR_SK']
        pac_claim['CLM_LINE'][i]['CLM_TYPE_CD'] = pac_claim['CLM']['CLM_TYPE_CD']
    for i in range(len(pac_claim['CLM_LINE_INSTNL'])):
        pac_claim['CLM_LINE_INSTNL'][i]['GEO_BENE_SK'] = pac_claim['CLM']['GEO_BENE_SK']
        pac_claim['CLM_LINE_INSTNL'][i]['CLM_DT_SGNTR_SK'] = pac_claim['CLM']['CLM_DT_SGNTR_SK']
        pac_claim['CLM_LINE_INSTNL'][i]['CLM_TYPE_CD'] = pac_claim['CLM']['CLM_TYPE_CD']
    for i in range(len(pac_claim['CLM_VAL'])):
        pac_claim['CLM_VAL'][i]['GEO_BENE_SK'] = pac_claim['CLM']['GEO_BENE_SK']
        pac_claim['CLM_VAL'][i]['CLM_DT_SGNTR_SK'] = pac_claim['CLM_DT_SGNTR']['CLM_DT_SGNTR_SK'] 
        pac_claim['CLM_VAL'][i]['CLM_TYPE_CD'] = pac_claim['CLM']['CLM_TYPE_CD']
    for i in range(len(pac_claim['CLM_INSTNL'])):
        pac_claim['CLM_INSTNL']['GEO_BENE_SK'] = pac_claim['CLM']['GEO_BENE_SK']
        pac_claim['CLM_INSTNL']['CLM_DT_SGNTR_SK'] = pac_claim['CLM_DT_SGNTR']['CLM_DT_SGNTR_SK'] 
        pac_claim['CLM_INSTNL']['CLM_TYPE_CD'] = pac_claim['CLM']['CLM_TYPE_CD']
   
    for i in range(len(pac_claim['CLM_PROD'])):
        pac_claim['CLM_PROD'][i]['CLM_DT_SGNTR_SK'] = pac_claim['CLM']['CLM_DT_SGNTR_SK']
        pac_claim['CLM_PROD'][i]['GEO_BENE_SK'] = pac_claim['CLM']['GEO_BENE_SK']
        pac_claim['CLM_PROD'][i]['CLM_TYPE_CD'] = pac_claim['CLM']['CLM_TYPE_CD']
    pac_claim['CLM_DT_SGNTR']['CLM_DT_SGNTR_SK'] = pac_claim['CLM']['CLM_DT_SGNTR_SK']
    if('CLM_MDCR_EXHSTD_DT' in pac_claim['CLM_DT_SGNTR']):
        pac_claim['CLM_DT_SGNTR'].pop('CLM_MDCR_EXHSTD_DT')
    if('CLM_NCVRD_FROM_DT' in pac_claim['CLM_DT_SGNTR']):
        pac_claim['CLM_DT_SGNTR'].pop('CLM_NCVRD_FROM_DT')
    if('CLM_NCVRD_THRU_DT' in pac_claim['CLM_DT_SGNTR']):
        pac_claim['CLM_DT_SGNTR'].pop('CLM_NCVRD_THRU_DT')
    if('CLM_NCH_WKLY_PROC_DT' in pac_claim['CLM_DT_SGNTR']):
        pac_claim['CLM_DT_SGNTR'].pop('CLM_NCH_WKLY_PROC_DT')
    if('CLM_ACTV_CARE_THRU_DT' in pac_claim['CLM_DT_SGNTR']):
        pac_claim['CLM_DT_SGNTR'].pop('CLM_ACTV_CARE_THRU_DT')
    if('CLM_MDCR_IP_BENE_DDCTBL_AMT' in pac_claim['CLM_INSTNL']):
        pac_claim['CLM_INSTNL'].pop('CLM_MDCR_IP_BENE_DDCTBL_AMT')
    if('CLM_MDCR_INSTNL_PRMRY_PYR_AMT' in pac_claim['CLM_INSTNL']):
        pac_claim['CLM_INSTNL'].pop('CLM_MDCR_INSTNL_PRMRY_PYR_AMT')
    if('CLM_PPS_IND_CD' in pac_claim['CLM_INSTNL']):
        pac_claim['CLM_INSTNL'].pop('CLM_PPS_IND_CD')
    if('CLM_INSTNL_DRG_OUTLIER_AMT' in pac_claim['CLM_INSTNL']):
        pac_claim['CLM_INSTNL'].pop('CLM_INSTNL_DRG_OUTLIER_AMT')
    if('CLM_MDCR_INSTNL_BENE_PD_AMT' in pac_claim['CLM_INSTNL']):
        pac_claim['CLM_INSTNL'].pop('CLM_MDCR_INSTNL_BENE_PD_AMT')
    if('CLM_ANSI_SGNTR_SK' in pac_claim['CLM_LINE_INSTNL']):
        pac_claim['CLM_LINE_INSTNL'].pop('CLM_ANSI_SGNTR_SK')
    if('CLM_OTAF_ONE_IND_CD' in pac_claim['CLM_LINE_INSTNL']):
        pac_claim['CLM_LINE_INSTNL'].pop('CLM_OTAF_ONE_IND_CD')
    if('CLM_REV_CNTR_STUS_CD' in pac_claim['CLM_LINE_INSTNL']):
        pac_claim['CLM_LINE_INSTNL'].pop('CLM_REV_CNTR_STUS_CD')

    
    #pac_claim['CLM_INSTNL']['CLM_UNIQ_ID'] = pac_claim['CLM']['CLM_UNIQ_ID']
    pac_claim['CLM_INSTNL']['GEO_BENE_SK'] = pac_claim['CLM']['GEO_BENE_SK']
    pac_claim['CLM_INSTNL']['CLM_DT_SGNTR_SK'] = pac_claim['CLM_DT_SGNTR']['CLM_DT_SGNTR_SK'] 

    pac_claim['CLM']['CLM_SRC_ID']=21000 #FISS

    if('CLM_DCMTN' in pac_claim):
        #if('CLM_NRLN_RIC_CD' in pac_claim['CLM_DCMTN']):
            #pac_claim['CLM']['CLM_RIC_CD'] = pac_claim['CLM_DCMTN']['CLM_NRLN_RIC_CD']
        pac_claim.pop('CLM_DCMTN')

    return pac_claim

def pull_code_systems():
    code_systems = {}
    relative_path = "../../sushi/fsh-generated/resources"
    for file in os.listdir(relative_path):
        if('.json' not in file or 'CodeSystem' not in file):
            continue
        full_path = relative_path+"/"+file
        try:
            with open(full_path, 'r') as file:
                data = json.load(file)
                concepts = []
                for i in data['concept']:
                    #print(i['code'])
                    concepts.append(i['code'])
                code_systems[data['name']] = concepts
        except FileNotFoundError:
            print(f"Error: File not found at path: {full_path}")
        except json.JSONDecodeError:
            print(f"Error: Invalid JSON format in file: {full_path}") 
    return code_systems
    

def main():
    parser = argparse.ArgumentParser(description='Generate Synthetic Data for Ingestion by the BFD v3 pipeline.')
    parser.add_argument('--sushi', '-s', action='store_true', help='Generate new StructureDefinitions. Use when testing locally if new .fsh files have been added.')
    parser.add_argument('--benes', '-b', type=str, help='Pull BENE_SKs from the input file. Expected format is that of SYNTHETIC_BENE.csv')

    args = parser.parse_args()
    if(args.sushi):
        print("Running sushi build")
        _, stderr = run_command("sushi build", cwd="../../sushi")
        print("SUSHI output:")
        #print(stdout)
        if stderr:
            print("SUSHI errors:")
            print(stderr)

    bene_sk_list = [-1]
    if(args.benes):
        df = pd.read_csv(args.benes)  # Replace with your actual filename
        bene_sk_list = df['BENE_SK'].unique()

    CLM = []
    CLM_LINE = []
    CLM_VAL = []
    CLM_INSTNL = []
    CLM_LINE_INSTNL = []
    CLM_DT_SGNTR = []
    CLM_PROD = []
    CLM_DCMTN = []
    pt_complete = 0
    for pt_bene_sk in bene_sk_list:
        if((pt_complete)%1000 == 0 and pt_complete>0):
            print(f"Completed {pt_complete} patients with {claims_to_generate_per_person} claims per patient.")
        for i in range(claims_to_generate_per_person):
            clm_from_dt_min = '2018-01-01'
            claim = gen_claim(bene_sk = pt_bene_sk,minDate = clm_from_dt_min)
            CLM.append(claim['CLM'])
            for line in claim['CLM_LINE']:
                CLM_LINE.append(line)
            for line in claim['CLM_VAL']:
                CLM_VAL.append(line)
            CLM_DT_SGNTR.append(claim['CLM_DT_SGNTR'])
            for line in claim['CLM_PROD']:
                CLM_PROD.append(line)
            CLM_INSTNL.append(claim['CLM_INSTNL'])
            for line in claim['CLM_LINE_INSTNL']:
                CLM_LINE_INSTNL.append(line)
            CLM_DCMTN.append(claim['CLM_DCMTN'])
            if(random.choice([0,1])):
                pac_claim = gen_pac_version_of_claim(claim)
                CLM.append(pac_claim['CLM'])
                for line in pac_claim['CLM_LINE']:
                    CLM_LINE.append(line)
                for line in pac_claim['CLM_VAL']:
                    CLM_VAL.append(line)
                CLM_DT_SGNTR.append(pac_claim['CLM_DT_SGNTR'])
                for line in pac_claim['CLM_PROD']:
                    CLM_PROD.append(line)
                CLM_INSTNL.append(pac_claim['CLM_INSTNL'])
                for line in pac_claim['CLM_LINE_INSTNL']:
                    CLM_LINE_INSTNL.append(line)
        pt_complete+=1
    save_output_files(CLM,CLM_LINE,CLM_VAL,CLM_DT_SGNTR,CLM_PROD,CLM_INSTNL,CLM_LINE_INSTNL,CLM_DCMTN)
    
if __name__ == "__main__":
    code_systems = pull_code_systems()
    main() 
