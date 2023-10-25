import argparse
import csv
import json

from pathlib import Path

# func to build csv header row
def build_csv_header():
    hrow = []
    for x in template_json["fields"]:
        hrow.append(x["title"])
    return hrow

# func to retrieve value for given field - most fields are just simple key[value] but some fields require somewhat specialized handling
# TODO: refactor this func to address hardcoded field names
def field_val(element_json, field):
    # handling varies by field
    if field in {"appliesTo", "ccwMapping", "cclfMapping"}:
        return ";".join(element_json[field])
    elif field in {"resource", "element", "derived", "note", "fhirPath", "example"}:
        return element_json["fhirMapping"][0][field]
    elif field in {"discriminator", "additional"}:
        return ";".join(element_json["fhirMapping"][0][field])
    elif field in {"AB2D", "BB2", "BCDA", "BFD", "DPC", "SyntheticData"}:
        if field in element_json["suppliedIn"]:
            return "X"
        else:
            return ""
    else:
        return element_json[field]

# func to build a csv of the element
def build_csv_row(element):
    row = []
    for x in template_json["fields"]:
        row.append(field_val(element,x["field"]))
    return row

# define arguments
parser = argparse.ArgumentParser()
parser.add_argument('-s','--source', dest='source', type=str, required=True, help='folder containing data dictionary source data files e.g. bfd-data-dictionary/data/V2')
parser.add_argument('-t','--target', dest='target', type=str,  required=True, help='target file name e.g. /BFD-V2-data-dict.csv')
parser.add_argument('-m', '--template', dest='template', type=str,  required=True, help='template file name to use e.g. ./template/v2-to-csv.json')
args = parser.parse_args()

template = Path(args.template)
source_dir = Path(args.source)
target_path = Path(args.target)

# open files
with open(template, 'r') as template_file:
    template_json = json.load(template_file)

print("Generating CSV from DD content")
print("")
print("Template:  " + str(template))
print("Source:    " + str(source_dir))
print("Target:    " + str(target_path))

# main loop
#
with open(target_path, 'w', newline='') as out_file:
    csv_writer = csv.writer(out_file)
    csv_writer.writerow(build_csv_header())
    ct = 0
    for file_name in sorted(source_dir.iterdir()):    # loop thru source data folder, read each json file, translate to csv and write row to file
        try:
            with open(file_name) as element_file:
                element_json = json.load(element_file)
                row = build_csv_row(element_json)
                csv_writer.writerow(row)
                ct += 1
        except Exception as exc:
            # TODO: Make this more robust...
            print(str(exc))
            continue

print("")
print("CSV file generated. " + str(ct) + " source files processed.")
