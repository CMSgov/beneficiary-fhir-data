"""
This script can be used, with the requisite copybooks from the IDR, to do some comparisons for accuracy on given profiles/applicability in BFD v3.
This will be used as part of future work to enhance the IDR copybooks and improve the quality of our data dictionary.
"""
from collections import Counter

import pandas as pd
import yaml

nch_stt_mapping = "ReferenceTables/source-to-target-mappings/IDR-CLMNCH-Mapping.xlsx"
other_sources = {
    "FISS": "ReferenceTables/source-to-target-mappings/IDR-SS-FISS-Mapping.xlsx",
    "MCS": "ReferenceTables/source-to-target-mappings/IDR-SS-MCS-Mapping.xlsx",
    "VMS": "ReferenceTables/source-to-target-mappings/IDR-SS-VMS-Mapping.xlsx",
}

eob_dict_yaml = "dictionary-support-files/ExplanationOfBenefit.yaml"

applies_to = {}

translations = {"CARR":"Carrier","DME":"DME","IP":"Inpatient","OP":"Outpatient","HHA":"HHA","HOSPC":"Hospice"}


for cur_profile in translations:
    df = pd.read_excel(nch_stt_mapping, sheet_name=cur_profile,header=3, usecols=['Target Table','Target Column'])
    for _, row in df.iterrows():
        element_concatenated = f"{row['Target Table'].strip()}.{row['Target Column'].strip()}"
        #there are newlines in some, we should ask IDR to consider changing the DD structure?
        if row['Target Table'] != '-' and row['Target Column'] != '-' and "\n" not in element_concatenated:
            if element_concatenated in applies_to and translations[cur_profile] not in applies_to[element_concatenated]['profiles']:
                applies_to[element_concatenated]['profiles'].append(translations[cur_profile])
            else:
                applies_to[element_concatenated] = {"profiles":[translations[cur_profile]],"sources":["NCH"]}
                
#In the IDR source to target mapping, IP and SNF are consolidated. So technically, they can be there. 
#this may be an opportunity for improvement in the future.
for element in applies_to:
    if('Inpatient' in applies_to[element]['profiles']):
        applies_to[element]['profiles'].append('SNF')


#If we pull more than the current CLM_FISS / CLM_MCS fields (1 and 0, respectively) then we'll need to profile 
#the individual claim types within PAC data. Otherwise, this just updates the source
for cur_source in other_sources:
    df = pd.read_excel(other_sources[cur_source], sheet_name='Claim Header',header=3, usecols=['Target Table','Target Column'])
    for _, row in df.iterrows():
        element_concatenated = f"{str(row['Target Table']).strip()}.{str(row['Target Column']).strip()}"
        #there are newlines in some, we should ask IDR to consider changing the DD structure?
        if row['Target Table'] != '-' and row['Target Column'] != '-' and "\n" not in element_concatenated:
            if element_concatenated in applies_to and cur_source not in applies_to[element_concatenated]['sources']:
                applies_to[element_concatenated]['sources'].append(cur_source)
    df = pd.read_excel(other_sources[cur_source], sheet_name='Claim Line',header=3, usecols=['Target Table','Target Column'])
    for _, row in df.iterrows():
        element_concatenated = f"{str(row['Target Table']).strip()}.{str(row['Target Column']).strip()}"
        #there are newlines in some, we should ask IDR to consider changing the DD structure?
        if row['Target Table'] != '-' and row['Target Column'] != '-' and "\n" not in element_concatenated:
            if element_concatenated in applies_to and cur_source not in applies_to[element_concatenated]['sources']:
                applies_to[element_concatenated]['sources'].append(cur_source)

#this is more of a heuristic. for example, HCPCS_5_MDFR_CD appears to be missing from the copybook but it's there in reality. 
profile_divergence_counter = 0
with open(eob_dict_yaml) as f:
    data = yaml.safe_load(f)
for i in data:
    if 'sourceView' in i:
        source_concatenated = f"{i['sourceView']}.{i['sourceColumn']}"[8:]
        if(source_concatenated in applies_to and Counter(i['appliesTo']) != Counter(applies_to[source_concatenated]['profiles'])):
            profile_divergence_counter +=1
            print(source_concatenated)
            print("in yaml:",i['appliesTo'])
            print("in source:",applies_to[source_concatenated]['profiles'])
print("remaining diverging for profiles:",profile_divergence_counter)

source_divergence_counter = 0
with open(eob_dict_yaml) as f:
    data = yaml.safe_load(f)
for i in data:
    if 'sourceView' in i:
        source_concatenated = f"{i['sourceView']}.{i['sourceColumn']}"[8:]
        if(source_concatenated in applies_to and Counter(i['sources']) != Counter(applies_to[source_concatenated]['sources'])):
            source_divergence_counter +=1
            print(source_concatenated)
            print("in yaml:",i['sources'])
            print("in source:",applies_to[source_concatenated]['sources'])
print("remaining diverging for profiles:",source_divergence_counter)
