'''Load and save configuration file.
'''

import yaml

def save(file_data):
    '''Saves a config file using the input file data.
    '''

    config_file = open('config.yml', 'w', encoding='utf-8')
    config_file.write(f"homePath: \"{file_data['homePath']}\"\n")
    config_file.write(f"clientCertPath: \"{file_data['clientCertPath']}\"\n")
    config_file.write(f"serverPublicKey: \"{file_data['serverPublicKey']}\"\n")
    config_file.write(f"dbUri: \"{file_data['dbUri']}\"\n")
    config_file.write(f"testHost: \"{file_data['testHost']}\"\n")
    config_file.write(f"tableSamplePct: \"{file_data['tableSamplePct']}\"\n")
    config_file.write(f"testRunTime: \"{file_data['testRunTime']}\"\n")
    config_file.write(f"testNumTotalClients: \"{file_data['testNumTotalClients']}\"\n")
    config_file.write(f"testCreatedClientsPerSecond: \"{file_data['testCreatedClientsPerSecond']}"
        "\"\n")
    config_file.write(f"resetStatsAfterClientSpawn: \"{file_data['resetStatsAfterClientSpawn']}\"")
    config_file.close()


def create():
    '''Requests config data from the user and creates a new test config file using that data.

    Returns the loaded config, or None if nothing could be loaded or an error occurred.
    '''

    ## Create a dictionary for holding the input data
    file_data = {}
    ## Prompt user for 4 config values and write to file
    file_data["homePath"] = input("Input full path to the home directory: ")
    file_data["clientCertPath"] = input("Input full path to the client cert file (pem): ")
    file_data["serverPublicKey"] = input("Input server public key (optional, hit enter to skip): ")
    file_data["dbUri"] = input("Input database uri for environment under test: ")
    file_data["testHost"] = input("Input desired test host (BFD server ip+port to test against, "
        "ex: https://10.235.16.152:7443 or load balancer address ex. https://test.bfd.cms.gov): ")
    file_data["tableSamplePct"] = input("Percent of database to sample (e.g. 5): ")
    file_data["testRunTime"] = input("Input desired test run time (eg. 30s, 1m): ")
    file_data["testNumTotalClients"] = input("Input total number of clients to create: ")
    file_data["testCreatedClientsPerSecond"] = input("Input number of clients to create per second "
        "(ramp-up speed): ")
    file_data["resetStatsAfterClientSpawn"] = (input("Reset statistics after spawning clients? "
        "[y/N]: ").lower == 'y')
    save(file_data)

    ## Attempt to read the new file
    try:
        config = yaml.safe_load(open('config.yml', encoding='utf-8'))
        return config
    except yaml.YAMLError:
        print('Unable to parse YAML configuration file; please check/create the file manually from '
            'the sample file.')
    except OSError:
        print("Could not read the new file; please try again.")
    return []


def load():
    '''Loads a config from the default config file (./config.yml); if no file exists, will attempt
    to create one via user prompts.

    Returns the loaded config, or None if nothing could be loaded or an error occurred.
    '''
    return load_from_path('config.yml')


def load_from_path(path: str):
    '''Loads a config from the specified config file path; if no file exists, will attempt to
    create one via user prompts.

    Returns the loaded config, or None if nothing could be loaded or an error occurred.
    '''

    try:
        return yaml.safe_load(open(path, encoding='utf-8'))
    except yaml.YAMLError:
        print("Unable to parse YAML configuration file; please ensure the format matches the "
            "example file.")
        return None
    except OSError:
        print("Could not find/read configuration file; let's set it up!")
        return create()
