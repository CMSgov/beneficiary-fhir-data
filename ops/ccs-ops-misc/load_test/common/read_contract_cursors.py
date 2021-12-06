import os
import sys
import yaml
from common import config

def loaddata():
    ## Load configuration data, like db creds
    config_file = config.load()

    ## if we failed to load the config, bail out
    if config_file is None:
        return -1

    file_path = config_file["homePath"] + "contract_cursors.txt"

    try:
        cursor_file = open(file_path, "r")
    except IOError:
        print(f"Could not find or open {file_path}, please ensure you've run write_contract_cursors.py to pull the cursors.")
        return []

    cursor_urls = cursor_file.readlines()

    print("Read " + str(len(cursor_urls)) + " results from the cursor file for the test.")
    return cursor_urls