'''Run Locust tests from the command line.
'''

import re
from math import ceil
from datetime import timedelta
import sys
import getopt
from multiprocessing import Process

from common import config, test_setup as setup
from locust.main import main

from common.stats.stats_config import StatsConfiguration

def parse_run_time(run_time):
    '''Parse a given run time setting (which Locust accepts as combinations of "1m", "30s", "2h",
    etc.), and return the duration in seconds.
    '''

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


def adjusted_run_time(run_time, max_clients, clients_per_second):
    '''Adjust the run time of the test to account for the time it takes to instantiate and connect
    all the clients.

    If a user specifies a one-minute test, but it's going to take thirty seconds to ramp up to full
    clients, then we actually run for one minute and thirty seconds, so that we can have the
    specified time with full client capacity. You can optionally reset the statistics to zero at
    the end of this ramp-up period using the --resetStats command line flag.
    '''

    run_time_seconds = parse_run_time(run_time)
    if run_time_seconds is None:
        return None
    return run_time_seconds + ceil(int(max_clients) // int(clients_per_second))


def run_with_params(argv):
    '''Runs a specified test via the input args.
    '''

    ## Dictionary that holds the default values of each config value
    default_config_data = {
        'homePath': '',
        'clientCertPath': '',
        'databaseUri': '',
        'testHost': '',
        'configPath': 'config.yml',
        'serverPublicKey': '',
        'tableSamplePct': '0.25',
        'testRunTime': "1m",
        'testNumTotalClients': "100",
        'testCreatedClientsPerSecond': "5",
        'resetStatsAfterClientSpawn': False,
        'stats': None
    }

    # Dictionary to hold data passed in via the CLI that will be stored in the root config.yml file
    config_data = {}

    test_file = ''
    worker_threads = "1"

    help_string = ('runtests.py \n--homePath="<path/to/home/directory>" (Required) '
     '\n--clientCertPath="<path/to/client/pem/file>" (Required)'
     '\n--databaseUri="postgresql://<username:password>@<database-aws-node>.rds.amazonaws.com:port/'
        '<dbname>" (Required)'
     '\n--testHost="https://<nodeIp>:7443 or https://<environment>.bfd.cms.gov" (Required)'
     '\n--test_file="/<v1/v2>/test_to_run.py" (Required)'
     '\n--configPath="<path to a YAML configuration that will be read for CLI values but _not_ '
        'written to>" (Optional, Default: "./config.yml")'
     '\n--serverPublicKey="<server public key>" (Optional, Default: "")'
     '\n--tableSamplePct=<% of table to sample> (Optional, Default: 0.25)'
     '\n--testRunTime="<Test run time, ex. 30s, 1m, 2d 1h>" (Optional, Default 1m)'
     '\n--maxClients="<Max number of clients to create at once, int>" (Optional, Default 100)'
     '\n--clientsPerSecond="<Clients to create per second until maxClients is reached, int>" '
        '(Optional, Default 5)'
     '\n--worker_threads="<If >1 the test is run as distributed, and expects this many worker '
        'processes to start, int>" (Optional, Default 1 - non distributed mode)'
     '\n--stats="<If set, stores stats in JSON to S3 or local file. Key-value list seperated by semi-colons. See README.>" (Optional)'
     '\n--resetStats (Optional)')

    try:
        opts, _args = getopt.getopt(argv, "h", ["homePath=", "clientCertPath=", "databaseUri=",
        "testHost=", "serverPublicKey=", 'tableSamplePct=', "configPath=", "testRunTime=",
        "maxClients=", "clientsPerSecond=", "testFile=", "workerThreads=", "stats=", "resetStats"])
    except getopt.GetoptError as err:
        print(err)
        print(help_string)
        sys.exit(2)

    for opt, arg in opts:
        if opt == '-h':
            print(help_string)
            sys.exit()
        elif opt == "--homePath":
            config_data["homePath"] = arg
        elif opt == "--clientCertPath":
            config_data["clientCertPath"] = arg
        elif opt == "--databaseUri":
            config_data["dbUri"] = arg
        elif opt == "--testHost":
            config_data["testHost"] = arg
        elif opt == "--configPath":
            config_data["configPath"] = arg
        elif opt == "--serverPublicKey":
            config_data["serverPublicKey"] = arg
        elif opt == '--tableSamplePct':
            config_data['tableSamplePct'] = arg
        elif opt == "--testRunTime":
            config_data["testRunTime"] = arg
        elif opt == "--maxClients":
            config_data["testNumTotalClients"] = arg
        elif opt == "--clientsPerSecond":
            config_data["testCreatedClientsPerSecond"] = arg
        elif opt == "--testFile":
            test_file = arg
        elif opt == "--workerThreads":
            worker_threads = arg
        elif opt == "--stats":
            try:
                config_data["stats"] = StatsConfiguration.from_key_val_str(arg)
            except ValueError as err:
                print(f'--stats was invalid: {err}\n')
                print(help_string)
                sys.exit()
        elif opt == "--resetStats":
            config_data["resetStatsAfterClientSpawn"] = True
        else:
            print(help_string)
            sys.exit()

    ## Read the specified configuration file
    yaml_config = config.load_from_path(config_data.get("configPath",
        default_config_data["configPath"])) or {}
    ## Merge the stored data with data passed in via the CLI, with the
    ## CLI data taking priority
    config_data = {**yaml_config, **config_data}
    ## Finally, merge the merged configuration values with the defaults,
    ## in case any optional arguments were not set via the CLI or the specified
    ## YAML configuration file
    config_data = {**default_config_data, **config_data}

    ## Add on extra time to the run-time to account for ramp-up of clients.
    adjusted_time = adjusted_run_time(config_data["testRunTime"],
        config_data["testNumTotalClients"], config_data["testCreatedClientsPerSecond"])
    if adjusted_time is None:
        print("Could not determine adjusted run time. Please use a format " +
            "like \"1m 30s\" for the --testRunTime option")
        sys.exit(1)
    config_data["testRunTime"] = f"{adjusted_time}s"
    print('Run time adjusted to account for ramp-up time. New run time: '
        f'{timedelta(seconds=adjusted_time)}')

    ## Check if all required params are set
    if not all([config_data["homePath"], config_data["clientCertPath"], config_data["dbUri"],
        config_data["testHost"], test_file]):

        print("Missing required arg (See -h for help on params)")
        sys.exit(2)

    ## write out to repository root config file (_NOT_ the file specified by "configPath")
    config.save(config_data)
    setup.set_locust_test_name(test_file)

    ## strip off extra command line params for locust, or else it tries to parse them
    sys.argv = sys.argv[:1]

    if int(worker_threads) > 1:
        ## Spawn worker threads to connect to the main thread
        for i in range(int(worker_threads)):
            print(f"Creating worker #{i}")
            process = Process(target=setup.run_worker_test, args=(i,worker_threads,))
            process.start()

        ## Run the master test
        setup.set_locust_env(config.load())
        setup.run_master_test(worker_threads)
    else:
        # Reset the distributed values
        setup.reset_distributed_values()
        # call locust to run the master test
        main()

## Runs the test via run args when this file is run
if __name__ == "__main__":
    run_with_params(sys.argv[1:])
