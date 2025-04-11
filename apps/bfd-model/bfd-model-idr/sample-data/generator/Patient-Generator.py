import csv
import random
import copy
import string
import datetime
import json
import pandas as pd
from dateutil.parser import parse
from faker import Faker
fake = Faker()

patients_to_generate = 80000
used_bene_sk = [] #We have this + used_mbi in case we want to expand this and ensure no collisions (just populate w/ existing list)
used_mbi = []
bene_table = []
bene_hstry_table = []
mbi_table = {}
address_options = []
available_given_names = ['Alex','Frankie','Joey','Caroline','Kartoffel','Elmo','Abby','Snuffleupagus','Bandit','Bluey','Bingo','Chilli','Le Petit Prince']
available_family_names = ['Erdapfel','Heeler','Coffee','Jones','Smith','Sheep']
with open('beneficiary-components/addresses.csv', 'r') as file:
    csvreader = csv.reader(file)
    header = next(csvreader)
    for row in csvreader:
        cur_row = {}
        for col in range(len(row)):
            cur_row[header[col]] = row[col]
        address_options.append(cur_row)

#this will deliberately generate invalid (but close!) MBIs. 
def gen_mbi():
    mbi = []
    set_1 = set(string.ascii_uppercase) - set(['S','L','O','I','B','Z'])
    set_2 = set(list(set_1) + list(string.digits))
    mbi.append(random.choice(['1','2','3','4','5','6','7','8','9']))
    mbi.append(random.choice(['S','L','O','I','B','Z']))
    mbi.append(random.choice(list(set_2)))
    mbi.append(random.choice(string.digits))
    mbi.append(random.choice(list(set_1)))
    mbi.append(random.choice(list(set_2)))
    mbi.append(random.choice(string.digits))
    mbi.append(random.choice(list(set_1)))
    mbi.append(random.choice(list(set_1)))
    mbi.append(random.choice(string.digits))
    mbi.append(random.choice(string.digits))
    mbi = ''.join(mbi)
    if(mbi in mbi_table.keys()):
        return gen_mbi()
    return mbi

def gen_bene_sk():
    bene_sk = random.randint(1000,1000000000)
    if(bene_sk in used_bene_sk):
        return gen_bene_sk()
    return bene_sk

for i in range(patients_to_generate):
    if i>0 and i % 10000 == 0:
        print("10000 done")
    patient = {}
    patient['BENE_1ST_NAME'] = random.choice(available_given_names)
    if(random.randint(0, 1)):
        patient['BENE_MIDL_NAME'] = random.choice(available_given_names)
    patient['BENE_LAST_NAME'] = random.choice(available_family_names)
    dob = fake.date_of_birth(minimum_age=45)
    patient['BENE_DOB'] = str(dob)
    if(random.randint(0,10)<2):
        #death!
        death_date = fake.date_between_dates(datetime.date(year=2020, month=1, day=1),datetime.date.today())
        patient['BENE_DEATH_DT']=str(death_date)
        if(random.randint(0,1)==1):
            patient['BENE_VRFY_DEATH_DAY_SW'] = 'Y'
        else:
            patient['BENE_VRFY_DEATH_DAY_SW'] = 'N'

    patient['BENE_SEX_CD'] = str(random.randint(1,2))
    patient['BENE_RACE_CD'] = random.choice(['~','0','1','2','3','4','5','6','7','8'])
    address = address_options[random.randint(0,len(address_options)-1)]
    for component in address:
        patient[component] = address[component]
    patient['CNTCT_LANG_CD'] = random.choice(['~','ENG','SPA'])
    
    '''
    We should make sure that we always generate at least one instance where there's an edge case, per run. 
    
    generating the merges. 
    #400000/90000000  ~.4% have duplicates. 
    Let's guarantee at least one difficult patient per run, and then the rest with some probability.
    '''
    #In BENE, IDR_TRANS_OBSLT_TS < 9999-12-31 if BENE_XREF_EFCTV_SK != BENE_SK

    pt_bene_sk = gen_bene_sk()
    patient['BENE_SK'] = str(pt_bene_sk)
    patient['XREF_EFCTV_BENE_SK'] = str(pt_bene_sk)
    used_bene_sk.append(pt_bene_sk)
    patient['IDR_TRANS_EFCTV_TS']  = str(datetime.datetime.now()-datetime.timedelta(days=365*10))
    patient['IDR_UPDT_TS']  = str(datetime.datetime.now()-datetime.timedelta(days=1))
    patient['IDR_TRANS_OBSLT_TS'] = '9999-12-31 00:00:00.000000'


    #Create a history, sometimes!
    #If they have a prior MBI on this, then let's create a bene_history
    num_mbis = random.choices([1,2,3,4],weights=[.8,.14,.05,.01])[0]

    for mbi_idx in range(0,num_mbis):
        mbi_obj = {}
        mbi = gen_mbi()
        mbi_table[mbi] = {}
        #fun fact, an MBI can be assigned after death
        if(mbi_idx == 0):
            efctv_dt = fake.date_between_dates(datetime.date(year=2017, month=5, day=20),datetime.date(year=2021, month=1, day=1))
            patient['BENE_MBI_ID'] = mbi
        else:
            efctv_dt = fake.date_between_dates(datetime.date(year=2021, month=1, day=2),datetime.date(year=2025-num_mbis+mbi_idx, month=1, day=1))

        mbi_obj['BENE_MBI_EFCTV_DT'] = str(efctv_dt)
        if(mbi_idx!=num_mbis-1):
            #print("ha")
            mbi_obj['BENE_MBI_OBSLT_DT'] = fake.date_between_dates(parse(mbi_obj['BENE_MBI_EFCTV_DT']),datetime.date(year=2025-num_mbis+mbi_idx, month=1, day=1)).strftime("%Y-%m-%d")
            patient_2 = copy.deepcopy(patient)
            patient_2['BENE_MBI_ID'] = mbi
            bene_hstry_table.append(patient_2)
        mbi_obj['IDR_TRANS_EFCTV_TS']  = str(datetime.datetime.now()-datetime.timedelta(days=365*10))
        mbi_obj['IDR_UPDT_TS']  = str(datetime.datetime.now()-datetime.timedelta(days=1))
        mbi_obj['IDR_TRANS_OBSLT_TS'] = '9999-12-31 00:00:00.000000'
        mbi_table[mbi] = mbi_obj
    
    for idx in range(0,random.randint(0,2)):
        if(idx == 0):
            continue

        patient_3 = copy.deepcopy(patient)
        #The key things here are that the different BENE_SK will be obsolete. 
        pt_bene_sk = gen_bene_sk()
        patient_3['BENE_SK'] = str(pt_bene_sk)
        used_bene_sk.append(pt_bene_sk)
        patient_3['IDR_TRANS_OBSLT_TS'] = '2021-01-02 00:00:00.000000'
        patient_3['IDR_UPDT_TS']
        bene_table.append(patient_3)

    bene_table.append(patient)
df = pd.json_normalize(bene_table)
csv_file_path = 'BENE.csv'
df.to_csv(csv_file_path, index=False)
df = pd.json_normalize(bene_hstry_table)
csv_file_path = 'BENE_HSTRY.csv'
df.to_csv(csv_file_path, index=False)
arr = []
for i in mbi_table.keys():
    arr.append(mbi_table[i])
df = pd.json_normalize(arr)
csv_file_path = 'BENE_MBI_ID.csv'
df.to_csv(csv_file_path, index=False)
