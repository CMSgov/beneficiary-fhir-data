import os
import psycopg2 as p
import yaml
from common import config

def loadData():
    ## Load configuration data, like db creds
    configFile = config.load()

    ## if we failed to load the config, bail out
    if configFile is None:
        return -1

    fileName = "ids.txt"
    beneQuery = "SELECT \"beneficiaryId\" FROM \"Beneficiaries\" TABLESAMPLE SYSTEM (0.25) LIMIT 100000;"

    ## Make the query to the DB
    conn = p.connect(
            user = configFile["dbUsername"],
            password = configFile["dbPassword"],
            host = configFile["dbHost"],
            port = '5432',
            database = 'fhirdb'
    )

    cursor = conn.cursor()
    cursor.execute(beneQuery)
    results = cursor.fetchall()

    eob_ids = []
    for row in results:
        eob_ids.append(str(row[0]))

    print("Returned " + str(len(results)) + " results from the database for the test.")
    return eob_ids