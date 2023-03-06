# BFD Data Dictionary

The BFD Data Dictionary project provides data dictionary and mapping information for the Beneficiary FHIR Data Server.


## Design

The /data folder contains JSON respresentations of each element in the BFD Data Dictionary, separated into folders for major BFD versions V1 and V2.

The /app folder contains various apps and scripts relating to the data dictionary.

The /app/dd-transformer folder contains simple python scripts to read and transform content into external/user facing formats e.g. CSV, HTML, PDF, etc


## Development Environment

### DD-transformer Dependencies

- Python 3.10 or later
- Jinja2 Python package  


## Running Locally

### Running the DD-Transformer

The DD-Transformer is a collection of python scripts located in the /app folder.  See Development Environment above for dependencies.

#### DD to CSV
The dd_to_csv script transforms all data dictionary elements found in the caller supplied source folder into a CSV file specified in the target parameter.  The template file determines how the contents are written to the CSV file.  See ./template for more information.

> $ python dd_to_csv.py --template [template file] --source [source data folder] --target [target file]

#### DD to JSON
The dd_to_json script combines all data dictionary elements found in the caller supplied source folder into a single JSON file specified in the target parameter.  
> $ python dd_to_json.py --source [source folder] --target [target file]

#### DD to HTML
The dd_to_html script combines all data dictionary elements found in the caller supplied source folder into an HTML file specified in the target parameter.  This script uses the Jinja2 templating engine.  The directory containing the templates must be supplied in the templateDir parameter, and the actual template file that drives layout and formatting must be supplied in the templateFile parameter. See ./template for more information.
> $ python dd_to_html.py --templateDir [template dir] --templateFile [template file] --source [source data folder] --target [target file]



## License

See ../../README.md 

