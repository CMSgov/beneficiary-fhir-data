import os
import common.config as config
from locust.main import main

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
    os.environ['LOCUST_HEADLESS'] = "True"
    os.environ['LOCUST_USERS'] = configFile["testNumTotalClients"]
    os.environ['LOCUST_SPAWN_RATE'] = configFile["testCreatedClientsPerSecond"]
    os.environ['LOCUST_LOGLEVEL'] = "INFO"
    ## set the runtime if not running distributed or if test master
    if not is_distributed() or is_master_thread():
        os.environ['LOCUST_RUN_TIME'] = configFile["testRunTime"]

'''
Sets some settings specific to the distributed master test and begins the main test.
'''
def run_master_test(workers):
    os.environ['LOCUST_MODE_WORKER'] = "False"
    os.environ['LOCUST_MODE_MASTER'] = "True"
    os.environ['LOCUST_EXPECT_WORKERS'] = workers
    main()

'''
Sets some settings specific to the distributed worker test and begins the worker test.
This should be called in a threaded capacity, or
'''
def run_worker_test(workerNum, workers):
    os.environ['LOCUST_MODE_WORKER'] = "True"
    os.environ['LOCUST_MODE_MASTER'] = "False"
    os.environ['LOCUST_NUM_WORKERS'] = workers
    os.environ['LOCUST_WORKER_NUM'] = str(workerNum)
    main()

'''
Checks if the currently running test thread is a worker thread.
Returns false if the test is not running in distributed mode.
'''
def is_worker_thread():
    return 'LOCUST_MODE_WORKER' in os.environ and os.environ['LOCUST_MODE_WORKER'] == "True"

'''
Checks if the currently running test thread is the singular master test thread.
Returns false if the test is not running in distributed mode.
'''
def is_master_thread():
    return 'LOCUST_MODE_MASTER' in os.environ and os.environ['LOCUST_MODE_MASTER'] == "True"

'''
Checks if the currently running test is running in distributed mode.
'''
def is_distributed():
    return 'LOCUST_MODE_MASTER' in os.environ or 'LOCUST_MODE_WORKER' in os.environ

'''
Resets the distributed mode environment variables, to allow
for non-distributed test runs.
'''
def reset_distributed_values():
    if "LOCUST_MODE_WORKER" in os.environ:
        os.environ.pop("LOCUST_MODE_WORKER")
    if "LOCUST_MODE_MASTER" in os.environ:
        os.environ.pop("LOCUST_MODE_MASTER")
    if "LOCUST_EXPECT_WORKERS" in os.environ:
        os.environ.pop("LOCUST_EXPECT_WORKERS")
    if "LOCUST_WORKER_NUM" in os.environ:
        os.environ.pop("LOCUST_WORKER_NUM")
    if "LOCUST_NUM_WORKERS" in os.environ:
        os.environ.pop("LOCUST_NUM_WORKERS")


'''
Sets the test name for the test, useful when running from the command line.
'''
def set_locust_test_name(testFileName):
    os.environ['LOCUST_LOCUSTFILE'] = testFileName

'''
If there is no server cert, disable warnings because thousands will appear in the logs and make it difficult
to see anything else.

We need to pass in urllib3 because if it's imported in this class it causes some conflict
with locust and produces a recursion error.
'''
def disable_no_cert_warnings(server_public_key, urllib3):
    if not server_public_key:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
