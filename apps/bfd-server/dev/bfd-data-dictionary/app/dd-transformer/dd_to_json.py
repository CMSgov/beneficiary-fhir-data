import argparse
import json

from pathlib import Path

# define arguments
parser = argparse.ArgumentParser()
parser.add_argument('-s','--source', dest='source', type=str, required=True, help='folder containing data dictionary source JSON file(s)')
parser.add_argument('-t','--target', dest='target', type=str,  required=True, help='target file name')
args = parser.parse_args()
source_dir = Path(args.source)
target_path = Path(args.target)

print("Generating consolidated JSON file from DD content")
print("")
print("Source: " + str(source_dir))
print("Target: " + str(target_path))

# main loop
with open(target_path, 'w', newline='') as out_file:
    out_json = json.loads('[]')  # initialize output to an empty json array
    ct = 0
    for file_name in source_dir.iterdir():    # loop thru source data folder, read each json file, translate to csv and write row to file
        try:
            with open(file_name) as element_file:
                element_json = json.load(element_file)
                out_json.append(element_json)
                ct += 1
        except Exception as exc:
            # TODO: Make this more robust...
            print(str(exc))
    json.dump(out_json, out_file)

print("")
print(str(ct) + " source files processed.")
print("JSON file generation complete.")
