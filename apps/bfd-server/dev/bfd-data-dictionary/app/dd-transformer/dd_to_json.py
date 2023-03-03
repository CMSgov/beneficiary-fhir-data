import os
import json
import argparse

# define arguments
parser = argparse.ArgumentParser()
parser.add_argument('-s','--source', dest='source', type=str, required=True, help='folder containing source file(s)')
parser.add_argument('-t','--target', dest='target', type=str,  required=True, help='target file name/path')
args = parser.parse_args()
source_dir = args.source
target_path = args.target
out_file = open(target_path, 'w', newline='')

print("Generating consolidated JSON file from DD content")
print("")
print("Source: " + source_dir)
print("Target: " + target_path)

# main loop
out_json = json.loads("[]")  # initialize output to an empty json array
ct = 0
for file_name in os.listdir(source_dir):   # loop thru source data folder, add object from each json file to array
    if file_name.endswith(".json"):
        source_path = source_dir + "\\" + file_name
        with open(source_path) as element_file:
            element_json = json.load(element_file)
            element_file.close()
            out_json.append(element_json)
            ct += 1

# close up
json.dump(out_json, out_file)
out_file.close()

print("")
print(str(ct) + " source files processed.")
print("JSON file generation complete.")
