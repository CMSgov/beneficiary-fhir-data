import os
import sys
import getopt
import common.pull_bene_ids
import common.config as config
from locust.main import main

def set_locust_env(configFile, testFileName):
    os.environ['LOCUST_HOST'] = configFile["testHost"]
    os.environ['LOCUST_RUN_TIME'] = configFile["testRunTime"]
    os.environ['LOCUST_HEADLESS'] = "True"
    os.environ['LOCUST_LOCUSTFILE'] = testFileName
    os.environ['LOCUST_USERS'] = configFile["testNumTotalClients"]
    os.environ['LOCUST_SPAWN_RATE'] = configFile["testCreatedClientsPerSecond"]
    os.environ['LOCUST_LOGLEVEL'] = "INFO"

def run_tests():
    print(" --- BFD Load Tests ---")
    choice = ''
    configFile = {}

    while choice.upper() != 'Q':
        if choice.upper() == '1':
            configFile = config.create()
            choice = ''
        elif choice.upper() == '2':
            configFile = config.load()
            while choice.upper() != 'Q':
                print(" -- Individual Test List (v1) --")
                print("1: Explanation of Benefit (EOB) by ID \n2: Patient by ID \nQ: return to main menu")
                choice = input()
                if choice.upper() == '1':
                    set_locust_env(configFile, "./v1/eob_test.py")
                    main()
                elif choice.upper() == '2':
                    set_locust_env(configFile, "./v1/patient_test.py")
                    main()
            choice = ''
        elif choice.upper() == '3':
            configFile = config.load()
            print(" -- Individual Test List (v2) --")
            print("1: Coverage by ID - Count 10 \n2: Coverage by ID - Count 100 - last Updated 2 weeks"
            + "\n3: Coverage by ID - last Updated 2 weeks \n4: Explanation of Benefit (EOB) by ID - Count 10"
            + "\n5: Explanation of Benefit (EOB) by ID - Count 10 - last Updated 2 weeks"
            + "\n6: Explanation of Benefit (EOB) by ID - Include tax numbers - last Updated 2 weeks"
            + "\n7: Explanation of Benefit (EOB) by ID - minimal response"
            + "\n8: Patient - hashed MBI"
            + "\n9: Patient - ID"
            + "\n10: Patient - ID - IncludeIdentifiers=true"
            + "\n11: Patient - ID - IncludeIdentifiers=mbi - last updated 2 weeks"
            + "\n12: Patient - Coverage Contract - IncludeIdentifiers=mbi - Count 500"
            + "\n13: Claim - hashed MBI"
            + "\nQ: return to main menu")
            choice = input()
            if choice.upper() == '1':
                set_locust_env(configFile, "./v2/coverage_test_id_count10.py")
                main()
            elif choice.upper() == '2':
                set_locust_env(configFile, "./v2/coverage_test_id_count100_lastUpdated.py")
                main()
            elif choice.upper() == '3':
                set_locust_env(configFile, "./v2/coverage_test_id_lastUpdated.py")
                main()
            elif choice.upper() == '4':
                set_locust_env(configFile, "./v2/eob_test_id_count10.py")
                main()
            elif choice.upper() == '5':
                set_locust_env(configFile, "./v2/eob_test_id_count10_lastUpdated.py")
                main()
            elif choice.upper() == '6':
                set_locust_env(configFile, "./v2/eob_test_id_includeTaxNumbers_lastUpdated.py")
                main()
            elif choice.upper() == '7':
                set_locust_env(configFile, "./v2/eob_test_id_params_false.py")
                main()
            elif choice.upper() == '8':
                set_locust_env(configFile, "./v2/patient_test_hashedMbi.py")
                main()
            elif choice.upper() == '9':
                set_locust_env(configFile, "./v2/patient_test_id.py")
                main()
            elif choice.upper() == '10':
                set_locust_env(configFile, "./v2/patient_test_id_includeIdentifiers.py")
                main()
            elif choice.upper() == '11':
                set_locust_env(configFile, "./v2/patient_test_id_includeMbiIdentifiers_lastUpdated.py")
                main()
            elif choice.upper() == '12':
                set_locust_env(configFile, "./v2/patient_test_coverageContract_includeMbiIdentifiers_count500.py")
                main()
            elif choice.upper() == '13':
                set_locust_env(configFile, "./v2/claim_test_hashedMbi.py")
                main()
            choice = ''
        else:
            print("Please select an option: ")
            print("1: Setup config file (change target environment) \n2: Run single v1 test \n3: Run single v2 test \nQ: quit")
            choice = input()


def run_with_params(argv):

    testFile = ''

    ## container to hold config data
    class configData: pass

    ## Optional Params with defaults
    configData.serverPublicKey = ''
    configData.testRunTime = "1m"
    configData.testNumTotalClients = "100"
    configData.testCreatedClientsPerSecond = "5"

    helpString = ('runtests.py \n--homePath="<path/to/home/directory>" (Required) '
     '\n--clientCertPath="<path/to/client/pem/file>" (Required)'
     '\n--databaseHost="<database-aws-node>.rds.amazonaws.com" (Required)'
     '\n--databaseUsername="keybase-db-username-for-environment" (Required)'
     '\n--databasePassword="keybase-db-password-for-environment" (Required)'
     '\n--testHost="https://<nodeIp>:7443 or https://<environment>.bfd.cms.gov" (Required)'
     '\n--testFile="/<v1/v2>/test_to_run.py" (Required)'
     '\n--serverPublicKey="<server public key>" (Optional, Default: "")'
     '\n--testRunTime="<Test run time, ex. 30s, 1m, 2d 1h>" (Optional, Default 1m)'
     '\n--maxClients="<Max number of clients to create at once, int>" (Optional, Default 100)'
     '\n--clientsPerSecond="<Clients to create per second until maxClients is reached, int>" (Optional, Default 5)')

    try:
        opts, args = getopt.getopt(argv,"h",["homePath=", "clientCertPath=", "databaseHost=", "databaseUsername=",
        "databasePassword=", "testHost=", "serverPublicKey=", "testRunTime=", "maxClients=", "clientsPerSecond=",
        "testFile="])
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
        elif opt == "--databaseHost":
            configData.dbHost = arg
        elif opt == "--databaseUsername":
            configData.dbUsername = arg
        elif opt == "--databasePassword":
            configData.dbPassword = arg
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
        else:
            print(helpString)
            sys.exit()

    ## Check if all required params are set
    if not all([configData.homePath, configData.clientCertPath, configData.dbHost, configData.dbUsername, configData.dbPassword, configData.testHost, testFile]):
        print("Missing required arg (See -h for help on params)")
        sys.exit(2)

    ## write out config file
    config.save(configData)

    ## set up locust params
    set_locust_env(config.load(), testFile)
    # strip off extra command line params for locust, or else it tries to parse them
    sys.argv = sys.argv[:1]
    # call locust to run test
    main()


## Run either single-line mode, or interactive command-line mode based on if args were passed
if __name__ == "__main__":
    if len(sys.argv) > 1:
        run_with_params(sys.argv[1:])
    else:
        run_tests()