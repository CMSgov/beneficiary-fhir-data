import os
import psycopg2 as p
import yaml
from common import config

'''
Calls out to the database located in the config file, and returns the list of hashed mbis from the query.

Returns a list of hashed mbis, or an empty array if nothing was found.
'''
def loadData():
    ## Load configuration data, like db creds
    configFile = config.load()

    ## if we failed to load the config, bail out (error msg printed in load())
    if configFile is None:
        return []

    beneQuery = "SELECT \"mbiHash\" FROM \"Beneficiaries\" WHERE \"mbiHash\" IS NOT NULL LIMIT 100000;"

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