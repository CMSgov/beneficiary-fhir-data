import sys
import ssl
import getopt
import os
import yaml
import urllib3
import requests
import json
import config

'''
Takes the args (expected from calling this file directly as a standalone script) and makes a database call
to get n number of contract search urls and all page cursor urls for those contracts. After the database call
writes the urls to a file for later test execution.
'''
def loadDataFromArgs(argv):

    ## Load configuration data, like db creds
    configFile = config.load()

    ## if we failed to load the config, bail out
    if configFile is None:
        return -1

    envHost = configFile["testHost"]
    contractIds = []
    month = ""
    year = ""
    count = ""
    version = ""

    helpString = ('write_contract_cursors.py \n--contracts="List of contract IDs to look up cursors for, 1-n contract ids separated by commas, no spaces" (Required) '
         '\n--year="year of the contract" (Required)'
         '\n--month="month of the contract, must be in MM format (ex. 01)" (Required)'
         '\n--count="number of results to pull per cursor" (Required)',
         '\n--version="which endpoint version to use (v1 or v2)" (Required)')

    try:
        opts, args = getopt.getopt(argv,"h",["contracts=", "year=", "month=", "count=", "version="])
    except getopt.GetoptError:
        print("Missing required arg. ")
        print(helpString)
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-h':
            print(helpString)
            sys.exit()
        elif opt == "--contracts":
            contractIds = arg.split(",")
        elif opt == "--year":
            year = arg
        elif opt == "--month":
            month = arg
        elif opt == "--count":
            count = arg
        elif opt == "--version":
            version = arg
        else:
            print("Invalid arg passed, exiting...")
            sys.exit()

    cursorFile = open(configFile["homePath"] + f"{version}_contract_cursors.txt", "w")
    certFile = configFile["clientCertPath"]

    # disable noisy warnings when making requests
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    totalContractCount = 0

    print("Collecting cursors for each contract...")
    for contractId in contractIds:
        print(f'[ContractId: {contractId}]')
        contractCount = 0
        url = envHost + (f'/{version}/fhir/Patient'
                      f'?_has%3ACoverage.extension=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Fptdcntrct{month}%7C{contractId}')

        url += f'&_has%3ACoverage.rfrncyr=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Frfrnc_yr%7C{year}'
        url += (f'&_count={count}&_format=json')

        # Write in the initial (first page) request
        cursorFile.write(f"{url}\n")
        totalContractCount += 1
        # keep writing cursors to the file as long as the response has "next" cursor entries
        while url:

            contents = requests.get(url, cert=certFile, verify="", headers={"IncludeIdentifiers": "mbi"})
            # Reset the next url to hit, so if no "next" page we exit loop
            url = ""
            responseJson = json.loads(contents.content)

            ## Get the "next" link in the response
            if responseJson and "link" in responseJson:
                links = responseJson["link"]
                if len(links) > 0:
                    for link in links:
                        if link["relation"] == "next":
                            url = link["url"]
                            cursorFile.write(f"{url}\n")
                            totalContractCount += 1
                            contractCount += 1
                            break
            # Give some indication the script is running every once in a while
            if contractCount % 100 == 0 and contractCount > 0:
                print(f"Still collecting... (currently at {contractCount} cursors)")
        print(f"Wrote {contractCount} cursor links from contract id {contractId}")

    print(f"Wrote {totalContractCount} total links to file")
    cursorFile.close()

if __name__ == "__main__":
    if len(sys.argv) > 1:
        loadDataFromArgs(sys.argv[1:])
