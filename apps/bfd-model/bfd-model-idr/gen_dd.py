import pandas as pd
import subprocess
import os
import json
import yaml

print("Generating Data Dictionary")
dd_support_folder = './dictionary-support-files'
idr_ref_folder = './ReferenceTables'
structure_def_folder = './StructureDefinitions/Source'

'''
This data structure will get more complex as BFD adds more "types" of data. We're effectively trying to populate a minimum spanning tree.
We want to have >=1 potential example to populate for each row in the data dictionary. 
In practice, Patient will be the least complex (no dependent variables), followed by Coverage (1 dependent variable), and then EOB (2 dependent variables).
For the initial version, we'll start simple and hard code no dependent variables for each of those.
'''
sample_sources = {'Patient':'outputs/Patient.json','ExplanationOfBenefit':'outputs/ExplanationOfBenefit.json','Coverage':'outputs/Coverage-FFS.json'}
sample_resources = {}
dd_df = []
idr_table_descriptors = {}
structure_def_names_descriptions = {}

for resource_type in sample_sources:
    with open(sample_sources[resource_type], 'r') as file:
        sample_resources[resource_type] = json.load(file)

for walk_info in os.walk(structure_def_folder):
    files = list(filter(lambda file: '.json' in file, walk_info[2]))
    for file_name in files:   
        with open(structure_def_folder+'/'+file_name, 'r') as file:
            test_resource = json.load(file)
            for element in test_resource['differential']['element']:
                structure_def_names_descriptions[element['id']] = {}
                structure_def_names_descriptions[element['id']]['name'] = element['label']
                if('definition' in element):
                    structure_def_names_descriptions[element['id']]['definition'] = element['definition']

for walk_info in os.walk(idr_ref_folder):
    files = list(filter(lambda file: '.csv' in file, walk_info[2]))
    for file_name in files:   
        idr_table_descriptors[file_name[0:len(file_name)-4]]={}
        df = pd.read_csv(idr_ref_folder+'/'+str(file_name))
        for index, row in df.iterrows():
            idr_table_descriptors[file_name[0:len(file_name)-4]][row['name']]=row['comment']

for walk_info in os.walk(dd_support_folder):
    files = list(filter(lambda file: '.yaml' in file, walk_info[2]))
    for file_name in files:
        with open(str(dd_support_folder)+'/'+str(file_name), 'r') as file:
            data = yaml.safe_load(file)
            current_resource_type = file_name[0:len(file_name)-5]
            for entry in data:
                if('fhirPath' in entry):
                    entry['appliesTo'].sort()
                    if('sources' in entry):
                        entry['sources'].sort()
                    result = subprocess.run(
                        ["node", "eval_fhirpath.js", json.dumps(sample_resources[current_resource_type]), entry['fhirPath']],
                        stdout=subprocess.PIPE
                    )
                    entry['example'] = json.loads(result.stdout)
                    if('iif' in entry['fhirPath'] or 'union' in entry['fhirPath']):
                        pass
                    elif(len(entry['example'])>0):
                        entry['example'] =  entry['example'][0]
                    else:
                        entry['example']=''
                    if('sourceView' in entry and entry['sourceView'] in idr_table_descriptors):
                        entry['Description'] = idr_table_descriptors[entry['sourceView']][entry['sourceColumn']]

                    #Populate the element names + missing descriptions
                    if(entry['inputPath'] in structure_def_names_descriptions):
                        entry['Concept Name'] = structure_def_names_descriptions[entry['inputPath']]['name']
                        if 'definition' in structure_def_names_descriptions[entry['inputPath']]:
                            entry['Description'] = structure_def_names_descriptions[entry['inputPath']]['definition']
                    entry.pop('inputPath')
                    if('nameOverride' in entry):
                        entry['Concept Name'] = entry['nameOverride']
                    
                    dd_df.append(entry)

dd_df = pd.DataFrame(dd_df)
def replace_str(input_str):
    #Yes, the below is intentional. 
    if(input_str==input_str and len(str(input_str))>0):
        return 'https://bluebutton.cms.gov/fhir/CodeSystem/'+str(input_str).replace("_","-")
    return ''
        
dd_df['referenceTable'] = list(map(replace_str,dd_df['referenceTable']))

dd_df.to_csv('outputs/bfd_data_dictionary.csv',columns=['Concept Name','Description','appliesTo','fhirPath','example','notes','sourceView','sourceColumn','bfdDerived','sources','referenceTable'])
export_columns = ['Concept Name','Description','appliesTo','fhirPath','example','notes','sourceView','sourceColumn','bfdDerived','sources','referenceTable']
export_df = dd_df[export_columns]
tips_df = pd.read_csv(dd_support_folder+"/tips.csv")

with pd.ExcelWriter('outputs/bfd_data_dictionary.xlsx', engine='xlsxwriter') as writer:
    export_df.to_excel(writer, sheet_name='Data Dictionary', index=True)
    
    workbook  = writer.book
    worksheet = writer.sheets['Data Dictionary']
    header_format = workbook.add_format({'bold': True, 'bg_color': '#DCE6F2', 'border': 1})
    text_format = workbook.add_format({'border': 1})

    worksheet.write(0, 0, "Row", header_format)
    
    for col_num, value in enumerate(export_df.columns,start=1):
        worksheet.write(0, col_num, value, header_format)

    worksheet.set_column('A:A', 4, text_format)
    worksheet.set_column('B:B', 30, text_format)
    worksheet.set_column('C:C', 65, text_format)
    worksheet.set_column('D:D', 20, text_format)
    worksheet.set_column('E:E', 55, text_format)
    worksheet.set_column('F:F', 25, text_format)
    worksheet.set_column('G:G', 25, text_format)
    worksheet.set_column('H:H', 18, text_format)
    worksheet.set_column('I:I', 20, text_format)
    worksheet.set_column('J:J', 5, text_format)
    worksheet.set_column('K:K', 10, text_format)
    worksheet.set_column('L:L', 20, text_format)

    tips_df.to_excel(writer, sheet_name='Tips and Tricks', index=True)
    worksheet = writer.sheets['Tips and Tricks']
    worksheet.set_column('A:A', 4, text_format)
    worksheet.set_column('B:B', 30, text_format)
    worksheet.set_column('C:C', 100, text_format)
print("Completed generating data dictionary")
