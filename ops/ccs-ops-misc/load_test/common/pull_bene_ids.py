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

    ## FUTURE: calculate the limit needed based on test duration * maxUsers, throw error if not enough data
    fileLimit = "100000"

    ## Use this until the data is cleaned up; right now there are tons of benes with null ref year which return 404 if we use them
    beneQuery = "SELECT \"beneficiaryId\" FROM \"Beneficiaries\" WHERE \"beneEnrollmentReferenceYear\" IS NOT NULL LIMIT " + fileLimit + ";"

    ## Use this query once the data is cleaned up, which will use random benes to avoid caching issues
    ## beneQuery = "SELECT \"beneficiaryId\" FROM \"Beneficiaries\" TABLESAMPLE SYSTEM (0.25) LIMIT 100000;"

    print("Collecting test data...")

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