import psycopg2 as p
from common import config


def loadData():
    ## Load configuration data, like db creds
    configFile = config.load()

    ## if we failed to load the config, bail out
    if configFile is None:
        return -1

    fileName = "mbis.txt"
    beneQuery = """select distinct f."mbi"
                    from "pre_adj"."FissClaims" f
                    where f."mbi" IS NOT NULL
                    union
                    select distinct m."idrClaimMbi"
                    from "pre_adj"."McsClaims" m
                    where m."idrClaimMbi" IS NOT NULL
                    and m."idrClaimMbi" NOT IN (
                        select distinct "idrClaimMbi" from "pre_adj"."McsClaims" where "idrStatusCode" IS NULL
                    )
                    LIMIT 100000;"""

    ## Make the query to the DB
    conn = p.connect(
        user=configFile["dbUsername"],
        password=configFile["dbPassword"],
        host=configFile["dbHost"],
        port='5432',
        database='fhirdb'
    )

    with conn:
        with conn.cursor() as cursor:
            cursor.execute(beneQuery)
            results = cursor.fetchall()
    conn.close()

    eob_ids = [str(r[0]) for r in results]

    print("Returned " + str(len(results)) + " results from the database for the test.")
    return eob_ids
