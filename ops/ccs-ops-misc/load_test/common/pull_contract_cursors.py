import sys
import ssl
import getopt
import os
import yaml
import urllib3
import requests
import json
from common import config

def loadDataFromArgs(argv):

    ## Load configuration data, like db creds
    configFile = config.load()

    ## if we failed to load the config, bail out
    if configFile is None:
        return -1

    envHost = configFile["testHost"]
    contractId = ""
    month = ""
    year = ""
    count = ""

    helpString = ('pull_contract_cursors.py \n--contract="Contract ID to look up cursors for" (Required) '
         '\n--year="year of the contract" (Required)'
         '\n--month="month of the contract, must be in MM format (ex. 01)" (Required)'
         '\n--count="number of results to pull per cursor" (Required)')

    try:
        opts, args = getopt.getopt(argv,"h",["contract=", "year=", "month=", "count="])
    except getopt.GetoptError:
        print("Missing required arg. ")
        print(helpString)
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-h':
            print(helpString)
            sys.exit()
        elif opt == "--contract":
            contractId = arg
        elif opt == "--year":
            year = arg
        elif opt == "--month":
            month = arg
        elif opt == "--count":
            count = arg
        else:
            print("Invalid arg passed, exiting...")
            sys.exit()

    url = envHost + ('/v2/fhir/Patient'
                  f'?_has%3ACoverage.extension=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Fptdcntrct{month}%7C{contractId}'
                  f'&_has%3ACoverage.rfrncyr=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Frfrnc_yr%7C{year}'
                  f'&_count={count}'
                  '&_format=json')

    certFile = configFile["clientCertPath"]
    print(f"Hitting url: {url}")

    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    contents = requests.get(url, cert=certFile, verify="", headers={"IncludeIdentifiers": "mbi"})

    resultObj = json.loads(contents.content)

    ## TODO: look inside the json object for object.meta.link[] for the one with relation: "next"
    ## Once you have this, take the object.meta.link["next"].url and save to certFile
    ## repeat until you run out, and you have all the cursors!

    print(contents.content)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        loadDataFromArgs(sys.argv[1:])