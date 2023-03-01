import os
import json
import csv
import argparse



# func to build csv header row
def build_csv_header():
    hrow = []
    for x in template_json["fields"]:
        hrow.append(x["title"])
    return hrow

# func to retrieve value for given field - most fields are just simple key[value] but some fields require somewhat specialized handling
def field_val(element_json, field):
    # handling varies by field
    match field:
        case "appliesTo" | "ccwMapping" | "cclfMapping":   # convert array to csv with ; as delimiter
            return ";".join(element_json[field])
        case "resource" | "element" | "derived" | "note" | "fhirPath" | "example":   # pull from fhirMapping object
            return element_json["fhirMapping"][0][field]
        case "discriminator" | "additional":  # pull from fhir mapping object and convert from array to csv with ; as delim
            return ";".join(element_json["fhirMapping"][0][field])
        case "AB2D" | "BB2" | "BCDA" | "BFD" | "DPC" | "SyntheticData": # convert from array to individual columns
            if field in element_json["suppliedIn"]:
                return "X"
            else:
                return ""
        case _:  # default just return the value from the top level key
            return element_json[field]

# func to build a csv of the element
def build_csv_row(element):
    row = []
    for x in template_json["fields"]:
        row.append(field_val(element,x["field"]))
    return row

########################
# MAIN
########################

parser = argparse.ArgumentParser()
parser.add_argument('-s','--source', dest='source', type=str, required=True, help='folder containing source data file e.g. bfd-data-dictionary/data/V2')
parser.add_argument('-t','--target', dest='target', type=str,  required=True, help='target file path e.g. c:/output/BFD-V2-data-dict.csv')
parser.add_argument('-m', '--template', dest='template', type=str,  required=True, help='path of template to use to build csv e.g. template/v2-to-csv.json')
args = parser.parse_args()
template_path = args.template
source_dir = args.source

with open(args.template, 'r') as template_file:
    template_json = json.load(template_file)
template_file.close()
out_file = open(args.target, 'w', newline='')
csv_writer = csv.writer(out_file)

print("Generating CSV from DD content")
print("Template:      " + args.template)
print("Source Folder: " + args.source)
print("Target File:   " + args.target)


csv_writer.writerow(build_csv_header())
ct = 0
for file_name in os.listdir(args.source):    # loop thru source data folder, read each json file, translate to csv and write row to file
    if file_name.endswith(".json"):
        source_path = args.source + file_name
        with open(source_path) as element_file:
            element_json = json.load(element_file)
            row = build_csv_row(element_json)
            csv_writer.writerow(row)
            element_file.close()
            ct += 1
out_file.close()
print("")
print("CSV file generated. " + str(ct) + " source files processed.")

# TODO:
# Lots of bulletproofing needed, esp around os/dir/file operations
# Improve error handling
