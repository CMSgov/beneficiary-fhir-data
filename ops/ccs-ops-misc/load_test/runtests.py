import os
import sys
import getopt
import common.config as config
import common.test_setup as setup
from locust.main import main
from multiprocessing import Process

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
     '\n--resetStats (Optional)'
     )

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

    ## Check if all required params are set
    if not all([configData.homePath, configData.clientCertPath, configData.dbUri, configData.testHost, testFile]):
        print("Missing required arg (See -h for help on params)")
        sys.exit(2)

    ## write out config file
    config.save(configData)
    setup.set_locust_test_name(testFile)

    # strip off extra command line params for locust, or else it tries to parse them
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
