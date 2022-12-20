# FHIR Validator
Utility for validating FHIR resources using the HAPI provided [FHIR validator cli tool](https://github.com/hapifhir/org.hl7.fhir.core/).

## Installation
The validator script, `validations.py` is written in python and utilizes pyyaml.  Using pip, install the requirements with the command `pip install -r requirements.txt`

The validator requires the HAPI provided FHIR validation cli tool, found here: https://github.com/hapifhir/org.hl7.fhir.core/releases.  The most recent release used to test resources is `5.6.48`.

## Run Validator
To run the validation script, enter the command `python3 validations.py -d path/to/resources`, where `path/to/resources` points to a directory containing a `v1` and / or `v2` directory with FHIR resources within in JSON format.

By default, the script will validate every resource (takes about a minute or two) within the directories, or you can optionally use the `-r` flag to only validate resources in the current branch that differ from the main branch.

The `-i` flag can be used to designate an "ignore list" to use when filtering the results of the validations, ex:
`python3 validations.py -i myignorelist.yml`.  By default, it searches for a local file called `validations_ignorelist.yml`.
