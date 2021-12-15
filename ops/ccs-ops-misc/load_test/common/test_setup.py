import os
import gevent
import time
import common.config as config
from locust.main import main
from locust import events
from locust.runners import STATE_STOPPING, STATE_STOPPED, STATE_CLEANUP, WorkerRunner, MasterRunner

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

def is_worker_thread():
    return 'LOCUST_MODE_WORKER' in os.environ and os.environ['LOCUST_MODE_WORKER'] == "True"

def is_master_thread():
    return 'LOCUST_MODE_MASTER' in os.environ and os.environ['LOCUST_MODE_MASTER'] == "True"

def is_distributed():
    return 'LOCUST_MODE_MASTER' in os.environ or 'LOCUST_MODE_WORKER' in os.environ

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
Adds a global listener to ensure that if the test response time is
too long (in the event the database is being overwhelmed) we stop the test.
'''
def check_global_fail(environment):
    while not environment.runner.state in [STATE_STOPPING, STATE_STOPPED, STATE_CLEANUP]:
        time.sleep(1)
        if environment.stats.total.avg_response_time > 2000:
            print("WARNING: Test aborted due to triggering global failsafe (average response time ratio > 2s)")
            environment.runner.quit()
            return

'''
Adds a listener that will add a repeating check for the global failsafe response time in order to stop the test
if the event the environment/box under test is overwhelmed and at risk of crashing.
'''
@events.init.add_listener
def on_locust_init(environment, **_kwargs):
    if not isinstance(environment.runner, WorkerRunner):
        gevent.spawn(check_global_fail, environment)

'''
If there is no server cert, disable warnings because thousands will appear in the logs and make it difficult
to see anything else.

We need to pass in urllib3 because if it's imported in this class it causes some conflict
with locust and produces a recursion error.
'''
def disable_no_cert_warnings(server_public_key, urllib3):
    if not server_public_key:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)