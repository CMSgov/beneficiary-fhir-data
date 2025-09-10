import json
import os
import subprocess
from pathlib import Path

import pandas as pd
import yaml

print("Generating Data Dictionary")
dd_support_folder = "./dictionary-support-files"
idr_ref_folder = "./ReferenceTables"
structure_def_folder = "./StructureDefinitions/Source"

"""
This data structure will get more complex as BFD adds more "types" of data. We're effectively trying
to populate a minimum spanning tree.
We want to have >=1 potential example to populate for each row in the data dictionary. 
In practice, Patient will be the least complex (no dependent variables), followed by Coverage 
(1 dependent variable), and then EOB (2 dependent variables).
For the initial version, we'll start simple and hard code no dependent variables for each of those.
"""
sample_sources = {
    "Patient": "out/Patient.json",
    "ExplanationOfBenefit": "out/ExplanationOfBenefit.json",
    "ExplanationOfBenefit-Pharmacy": "out/ExplanationOfBenefit-Pharmacy.json",
    "Coverage": "out/Coverage-FFS.json",
}
sample_sources_by_profile = {
    "PartA": "out/Coverage-FFS.json",
    "PartB": "out/Coverage-FFS-PartB.json",
    "PartC": "out/Coverage-PartC.json",
    "PartD": "out/Coverage-PartD.json",
    "DUAL": "out/Coverage-Dual.json",
    "Inpatient": "out/ExplanationOfBenefit.json",
    "SNF": "out/ExplanationOfBenefit-SNF.json",
    "HHA": "out/ExplanationOfBenefit-HHA.json",
    "Hospice": "out/ExplanationOfBenefit-Hospice.json",
    "Outpatient": "out/ExplanationOfBenefit-Outpatient.json",
    "Carrier": "out/ExplanationOfBenefit-Carrier.json",
    "DME": "out/ExplanationOfBenefit-DME.json",
    "Pharmacy": "out/ExplanationOfBenefit-Pharmacy.json",
    "Patient": "out/Patient.json"
}

sample_resources_by_profile = {}
dd_df = []
idr_table_descriptors = {}
structure_def_names_descriptions = {}

for resource_type in sample_sources_by_profile:
    with Path(sample_sources_by_profile[resource_type]).open() as file:
        sample_resources_by_profile[resource_type] = json.load(file)


for walk_info in os.walk(structure_def_folder):
    files = list(filter(lambda file: ".json" in file, walk_info[2]))
    for file_name in files:
        with Path(structure_def_folder + "/" + file_name).open() as file:
            test_resource = json.load(file)
            for element in test_resource["differential"]["element"]:
                structure_def_names_descriptions[element["id"]] = {}
                structure_def_names_descriptions[element["id"]]["name"] = element["label"]
                if "definition" in element:
                    structure_def_names_descriptions[element["id"]]["definition"] = element[
                        "definition"
                    ]

for walk_info in os.walk(idr_ref_folder):
    files = list(filter(lambda file: ".csv" in file, walk_info[2]))
    for file_name in files:
        idr_table_descriptors[file_name[0 : len(file_name) - 4]] = {}
        df = pd.read_csv(idr_ref_folder + "/" + str(file_name))
        for _, row in df.iterrows():
            idr_table_descriptors[file_name[0 : len(file_name) - 4]][row["name"]] = row["comment"]

coverage_parts = ['PartA','PartB','PartC','PartD','DUAL']
claim_profiles = ['HHA','Hospice','SNF','DME','Carrier','Inpatient','Outpatient','Pharmacy']
for walk_info in os.walk(dd_support_folder):
    files = list(filter(lambda file: ".yaml" in file, walk_info[2]))
    for file_name in files:
        with Path(str(dd_support_folder) + "/" + str(file_name)).open() as file:
            data = yaml.safe_load(file)
            current_resource_type = file_name[0 : len(file_name) - 5]
            for entry in data:
                if 'suppressInDD' in entry and entry['suppressInDD']:
                    continue
                if "fhirPath" in entry:
                    entry["appliesTo"].sort()
                    if "sources" in entry:
                        entry["sources"].sort()
                    if('Patient' in entry['appliesTo']):
                        entry['FHIR Resource'] = 'Patient'
                    elif(any(x in coverage_parts for x in entry['appliesTo'])):
                        entry['FHIR Resource'] = 'Coverage'
                        entry['Coverage / Claim Type'] = entry['appliesTo']
                    elif(any(x in claim_profiles for x in entry['appliesTo'])):
                        entry['FHIR Resource'] = 'ExplanationOfBenefit'
                        entry['Coverage / Claim Type'] = entry['appliesTo']

                    #This opportunistically populates examples based upon the samples created from executing FML
                    result = subprocess.run(
                        [
                            "node",
                            "eval_fhirpath.js",
                            json.dumps(sample_resources_by_profile[entry['appliesTo'][0]]),
                            entry["fhirPath"],
                        ],
                        check=True,
                        stdout=subprocess.PIPE,
                    )
                    entry['example']=json.loads(result.stdout)
                    if "iif" in entry["fhirPath"] or "union" in entry["fhirPath"]:
                        pass
                    elif len(entry["example"]) > 0:
                        entry["example"] = entry["example"][0]
                    else:
                        entry["example"] = ""
                    if "sourceView" in entry and entry["sourceView"] in idr_table_descriptors:
                        entry["Description"] = idr_table_descriptors[entry["sourceView"]][
                            entry["sourceColumn"]
                        ]

                    # Populate the element names + missing descriptions
                    if entry["inputPath"] in structure_def_names_descriptions:
                        entry["Field Name"] = structure_def_names_descriptions[
                            entry["inputPath"]
                        ]["name"]
                        if "definition" in structure_def_names_descriptions[entry["inputPath"]]:
                            entry["Description"] = structure_def_names_descriptions[
                                entry["inputPath"]
                            ]["definition"]
                    entry.pop("inputPath")
                    if "nameOverride" in entry:
                        entry["Field Name"] = entry["nameOverride"]

                    dd_df.append(entry)

dd_df = pd.DataFrame(dd_df)


def replace_str(input_str):
    # Yes, the below is intentional.
    if input_str == input_str and len(str(input_str)) > 0:
        return "https://bluebutton.cms.gov/fhir/CodeSystem/" + str(input_str).replace("_", "-")
    return ""


dd_df["referenceTable"] = list(map(replace_str, dd_df["referenceTable"]))

dd_df.to_csv(
    "out/bfd_data_dictionary.csv",
    columns=[
        "Field Name",
        "Description",
        "FHIR Resource",
        "Coverage / Claim Type",
        "fhirPath",
        "example",
        "notes",
        "sourceView",
        "sourceColumn",
        "bfdDerived",
        "sources",
        "referenceTable",
        "cclfMapping",
        "ccwMapping"
    ],
)
export_columns = [
    "Field Name",
    "Description",
    "FHIR Resource",
    "Coverage / Claim Type",
    "fhirPath",
    "example",
    "notes",
    "sourceView",
    "sourceColumn",
    "bfdDerived",
    "sources",
    "referenceTable",
    "cclfMapping",
    "ccwMapping"
]
export_df = dd_df[export_columns]
tips_df = pd.read_csv(dd_support_folder + "/tips.csv")

with pd.ExcelWriter("out/bfd_data_dictionary.xlsx", engine="xlsxwriter") as writer:
    export_df.to_excel(writer, sheet_name="Data Dictionary", index=True)

    workbook = writer.book
    worksheet = writer.sheets["Data Dictionary"]
    header_format = workbook.add_format({"bold": True, "bg_color": "#DCE6F2", "border": 1})
    text_format = workbook.add_format({"border": 1})

    worksheet.write(0, 0, "Row", header_format)

    for col_num, value in enumerate(export_df.columns, start=1):
        worksheet.write(0, col_num, value, header_format)

    worksheet.set_column("A:A", 4, text_format)
    worksheet.set_column("B:B", 30, text_format)
    worksheet.set_column("C:C", 65, text_format)
    worksheet.set_column("D:D", 20, text_format)
    worksheet.set_column("E:E", 20, text_format)
    worksheet.set_column("F:F", 55, text_format)
    worksheet.set_column("G:G", 25, text_format)
    worksheet.set_column("H:H", 25, text_format)
    worksheet.set_column("I:I", 18, text_format)
    worksheet.set_column("J:J", 20, text_format)
    worksheet.set_column("K:K", 5, text_format)
    worksheet.set_column("L:L", 10, text_format)
    worksheet.set_column("M:M", 20, text_format)
    worksheet.set_column("N:N", 30, text_format)
    worksheet.set_column("O:O", 30, text_format)

    tips_df.to_excel(writer, sheet_name="Tips and Tricks", index=True)
    worksheet = writer.sheets["Tips and Tricks"]
    worksheet.set_column("A:A", 4, text_format)
    worksheet.set_column("B:B", 30, text_format)
    worksheet.set_column("C:C", 100, text_format)
print("Completed generating data dictionary")
