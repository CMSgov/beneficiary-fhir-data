from jinja2 import Environment, FileSystemLoader
import json
import argparse

# define arguments
parser = argparse.ArgumentParser()
parser.add_argument('-s','--source', dest='source', type=str, required=True, help='source JSON file')
parser.add_argument('-t','--target', dest='target', type=str,  required=True, help='target file name/path')
parser.add_argument('-m', '--templateDir', dest='templateDir', type=str,  required=True, help='dir wher templates live')
parser.add_argument('-n', '--templateFile', dest='templateFile', type=str,  required=True, help='template file name')
args = parser.parse_args()

print("Generating HTML from DD content")
print("")
print("Template: " + args.templateDir + "\\" + args.templateFile)
print("Source:   " + args.source)
print("Target:   " + args.target)


# open source file (consolidated JSON file)
# TODO: refactor to allow user to specify folder as well as file
with open(args.source,encoding='utf-8') as json_file:
    elements = json.load(json_file)

# sort elements by name
def customSort(k):
    return k['name'].lower()
elements.sort(key=customSort)

# setup jina template vars
environment = Environment(loader=FileSystemLoader(args.templateDir))  # TODO: this forces caller to specify a template from /template folder
results_filename = args.target
results_template = environment.get_template(args.templateFile)
context = {
    "elements": elements
}

# run jinja template engine
with open(args.target, mode="w", encoding="utf-8") as results:
    results.write(results_template.render(context))

#close up
print("")
print(f"Finished generating file: {args.target}")