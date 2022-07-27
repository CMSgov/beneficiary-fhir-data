import sys
import psycopg2
import re
from pathlib import Path

def generate_characteristics_file(args):
    """
    Generates a beneficiary characteristics file for a given
    synthea load, and exports it as a csv.
    """
    
    # TODO: What do we need? Synthea start props?
    # TODO: Check/Find the queries we use for this and do the thing. we'll know what info we need too

    end_state_properties_file = Path(args[0]).read_text()
    db_string = args[3]
    generated_benes = args[2]
    synthea_prop_filepath = args[1]
    

    

## Runs the program via run args when this file is run
if __name__ == "__main__":
    # 5 args:
    # arg1: previous end state properties file location
    # arg2: number of items to be generated
    # arg3: file system location of synthea properties file to edit
    # arg4: db string for target environment DB, in this format: postgres://<dbName>:<db-pass>@<aws db url>:5432/fhirdb
    # arg5: (optional) skip validation, useful if re-generating a bad batch, True or False
    validate_and_update(sys.argv[1:])
