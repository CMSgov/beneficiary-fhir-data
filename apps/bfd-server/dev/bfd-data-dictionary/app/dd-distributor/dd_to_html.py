from jinja2 import Environment, FileSystemLoader
import json
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('-s','--source', dest='source', type=str, required=True, help='source JSON file')
parser.add_argument('-t','--target', dest='target', type=str,  required=True, help='target file name/path')
parser.add_argument('-m', '--template', dest='template', type=str,  required=True, help='path of template to use to build csv - see /template')
args = parser.parse_args()


# read consolidate json file from DIST folder 
# TODO: Note this is a dependency on something other than content in the /DATA folder; may want to rethink this
# TODO: this also assumes a single JSON file; woudl be nice to have a ability to pass folder and convert all JSON in folder to HTML
with open(args.source,encoding='utf-8') as json_file:
    elements = json.load(json_file)

# sort elements by name
def customSort(k):
    return k['name'].lower()
elements.sort(key=customSort)

# setup jina template vars
environment = Environment(loader=FileSystemLoader("template/"))  # TODO: this forces caller to specify a template from /template folder
results_filename = args.target
results_template = environment.get_template(args.template)
context = {
    "elements": elements
}

# run jinja template engine
with open(args.target, mode="w", encoding="utf-8") as results:
    results.write(results_template.render(context))
    print(f"... wrote {args.target}")

