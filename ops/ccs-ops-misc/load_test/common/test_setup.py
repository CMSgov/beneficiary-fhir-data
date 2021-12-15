import os
import common.config as config
import common.pull_bene_ids as benes
import common.pull_hashed_mbis as mbi

'''
Makes a database call to get a list of beneficiary ids and returns the resulting list.
'''
def generateAndLoadIds():
    configFile = config.load()
    return benes.loadData()

'''
Makes a database call to get a list of hashed mbis and returns the resulting list.
'''
def generateAndLoadMbis():
    configFile = config.load()
    return mbi.loadData()

'''
Checks the config file for the client cert value.
'''
def getClientCert():
    configFile = config.load()
    return configFile["clientCertPath"]

'''
If there is a public key to verify the BFD Server's responses
then it can be passed in with an environment variable. Otherwise,
the error from the self-signed cert is ignored.
'''
def loadServerPublicKey():
    try:
        configFile = config.load()
        server_public_key = configFile["serverPublicKey"]
        if not server_public_key:
            return False
        else:
            return server_public_key
    except KeyError:
        return False

'''
Sets a number of locust variables needed to run the test.
'''
def set_locust_env(configFile):
    os.environ['LOCUST_HOST'] = configFile["testHost"]
    os.environ['LOCUST_RUN_TIME'] = configFile["testRunTime"]
    os.environ['LOCUST_HEADLESS'] = "True"
    os.environ['LOCUST_USERS'] = configFile["testNumTotalClients"]
    os.environ['LOCUST_SPAWN_RATE'] = configFile["testCreatedClientsPerSecond"]
    os.environ['LOCUST_LOGLEVEL'] = "INFO"

'''
Sets the test name for the test, useful when running from the command line.
'''
def set_locust_test_name(testFileName):
    os.environ['LOCUST_LOCUSTFILE'] = testFileName