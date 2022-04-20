import re
from math import ceil
from datetime import timedelta
import sys
import getopt
import common.config as config
import common.test_setup as setup
from locust.main import main
from multiprocessing import Process

'''
Parse a given run time setting (which Locust accepts as combinations of "1m",
"30s", "2h", etc.), and return the duration in seconds
'''
def parse_run_time(run_time):
    # At least one digit, optional whitespace, single letter
    base_pattern = r'\d+\s*[A-Z]'

    # At least one base_pattern, optional whitespace in between, and optional
    # whitespace on either end
    full_pattern = rf"^\s*({base_pattern}\s*)+\s*$"

    # Verify that the entire string is properly formatted
    if not re.match(full_pattern, run_time, re.IGNORECASE):
        return None

    seconds_per_unit = {"s": 1, "m": 60, "h": 3600, "d": 86400}
    total_seconds = 0
    matches = re.findall(base_pattern, run_time, re.IGNORECASE)

    for raw_match in matches:
        match = raw_match.replace(" ", "").lower()
        total_seconds += int(match[:-1]) * seconds_per_unit[match[-1]]

    return total_seconds

'''
Adjust the run time of the test to account for the time it takes to instantiate
and connect all the clients. If a user specifies a one-minute test, but it's
going to take thirty seconds to ramp up to full clients, then we actually run
for one minute and thirty seconds, so that we can have the specified time with
full client capacity. You can optionally reset the statistics to zero at the
end of this ramp-up period using the --resetStats command line flag.
'''
def adjusted_run_time(runTime, maxClients, clientsPerSecond):
    run_time_seconds = parse_run_time(runTime)
    if run_time_seconds is None:
        return None
    return run_time_seconds + ceil(int(maxClients) // int(clientsPerSecond))

'''
Runs a specified test via the input args.
'''
def run_with_params(argv):

    testFile = ''

    ## container to hold config data
    class configData: pass

    ## Optional Params with defaults
    configData.serverPublicKey = ''
    configData.testRunTime = "1m"
    configData.testNumTotalClients = "100"
    configData.testCreatedClientsPerSecond = "5"
    configData.resetStatsAfterClientSpawn = False
    workerThreads = "1"

    helpString = ('runtests.py \n--homePath="<path/to/home/directory>" (Required) '
     '\n--clientCertPath="<path/to/client/pem/file>" (Required)'
     '\n--databaseUri="postgres://<username:password>@<database-aws-node>.rds.amazonaws.com:port/<dbname>" (Required)'
     '\n--testHost="https://<nodeIp>:7443 or https://<environment>.bfd.cms.gov" (Required)'
     '\n--testFile="/<v1/v2>/test_to_run.py" (Required)'
     '\n--serverPublicKey="<server public key>" (Optional, Default: "")'
     '\n--testRunTime="<Test run time, ex. 30s, 1m, 2d 1h>" (Optional, Default 1m)'
     '\n--maxClients="<Max number of clients to create at once, int>" (Optional, Default 100)'
     '\n--clientsPerSecond="<Clients to create per second until maxClients is reached, int>" (Optional, Default 5)'
     '\n--workerThreads="<If >1 the test is run as distributed, and expects this many worker processes to start, int>" (Optional, Default 1 - non distributed mode)'
     '\n--resetStats (Optional)')

    try:
        opts, args = getopt.getopt(argv,"h",["homePath=", "clientCertPath=", "databaseUri=",
        "testHost=", "serverPublicKey=", "testRunTime=", "maxClients=", "clientsPerSecond=",
        "testFile=","workerThreads=","resetStats"])
    except getopt.GetoptError:
        print(helpString)
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-h':
            print(helpString)
            sys.exit()
        elif opt == "--homePath":
            configData.homePath = arg
        elif opt == "--clientCertPath":
            configData.clientCertPath = arg
        elif opt == "--databaseUri":
            configData.dbUri = arg
        elif opt == "--testHost":
            configData.testHost = arg
        elif opt == "--serverPublicKey":
            configData.serverPublicKey = arg
        elif opt == "--testRunTime":
            configData.testRunTime = arg
        elif opt == "--maxClients":
            configData.testNumTotalClients = arg
        elif opt == "--clientsPerSecond":
            configData.testCreatedClientsPerSecond = arg
        elif opt == "--testFile":
            testFile = arg
        elif opt == "--workerThreads":
            workerThreads = arg
        elif opt == "--resetStats":
            configData.resetStatsAfterClientSpawn = True
        else:
            print(helpString)
            sys.exit()

    ## Add on extra time to the run-time to account for ramp-up of clients.
    adjusted_time = adjusted_run_time(configData.testRunTime,
        configData.testNumTotalClients, configData.testCreatedClientsPerSecond)
    if adjusted_time is None:
        print("Could not determine adjusted run time. Please use a format " +
            "like \"1m 30s\" for the --testRunTime option")
        sys.exit(1)
    configData.testRunTime = f"{adjusted_time}s"
    print('Run time adjusted to account for ramp-up time. New run time: %s' %
        timedelta(seconds=adjusted_time))

    ## Check if all required params are set
    if not all([configData.homePath, configData.clientCertPath, configData.dbUri, configData.testHost, testFile]):
        print("Missing required arg (See -h for help on params)")
        sys.exit(2)

    ## write out config file
    config.save(configData)
    setup.set_locust_test_name(testFile)

    ## strip off extra command line params for locust, or else it tries to parse them
    sys.argv = sys.argv[:1]

    if(int(workerThreads) > 1):
        ## Spawn worker threads to connect to the main thread
        for i in range(int(workerThreads)):
            print(f"Creating worker #{i}")
            p = Process(target=setup.run_worker_test, args=(i,workerThreads,))
            p.start()

        ## Run the master test
        setup.set_locust_env(config.load())
        setup.run_master_test(workerThreads)
    else:
        # Reset the distributed values
        setup.reset_distributed_values()
        # call locust to run the master test
        main()

## Runs the test via run args when this file is run
if __name__ == "__main__":
    run_with_params(sys.argv[1:])
