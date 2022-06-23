'''Load and save configuration file.
'''

from enum import Enum
from typing import Dict, Optional

import yaml

from common.stats.stats_config import StatsConfiguration

def save(file_data: Dict[str, str]):
    '''Saves a config file using the input file data.
    '''

    with open('config.yml', 'w', encoding='utf-8') as config:
        yaml.dump(file_data, config, default_flow_style=False, Dumper=_get_dumper())


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
        config = yaml.load(open('config.yml', encoding='utf-8'), Loader=_get_loader())
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
        return yaml.load(open(path, encoding='utf-8'), Loader=_get_loader())
    except yaml.YAMLError:
        print("Unable to parse YAML configuration file; please ensure the format matches the "
            "example file.")
        return None
    except OSError:
        print("Could not find/read configuration file; let's set it up!")
        return create()


def get_client_cert() -> str:
    '''Checks the config file for the client cert value.
    '''

    config_file = load()
    return config_file["clientCertPath"]


def load_server_public_key() -> str:
    '''Load the public key to verify the BFD Server's responses or else ignore the warnings from
    the self-signed cert.
    '''

    try:
        config_file = load()
        server_public_key = config_file["serverPublicKey"]
        return server_public_key if server_public_key else False
    except KeyError:
        return False


def load_stats_config() -> Optional[StatsConfiguration]:
    """Load the stats configuration for storing and comparing aggregated statistics.

    Returns:
        StatsConfiguration: A dataclass representing the user-specified options for comparing and loading statistics
    """

    config_file = load()
    if not config_file or not 'stats' in config_file:
        return None
    
    return config_file["stats"]  # type: ignore

def _stats_config_representer(dumper: yaml.SafeDumper, stats_config: StatsConfiguration) -> yaml.nodes.ScalarNode:
    """Returns a scalar representer that instructs PyYAML how to serialize a StatsConfiguration instance
    to a key-value list seperated by semi-colons ("key1=value1;key2=value2").

    Args:
        dumper (yaml.SafeDumper): PyYAML's default SafeDumper instance
        stats_config (StatsConfiguration): An instance of StatsConfiguration to serialize

    Returns:
        yaml.nodes.ScalarNode: A scalar YAML node representing a StatsConfiguration instance
    """
    return dumper.represent_scalar('!StatsConfig', stats_config.to_key_val_str())

def _stats_config_constructor(loader: yaml.SafeLoader, node: yaml.nodes.ScalarNode) -> StatsConfiguration:
    """Returns a scalar constructor that instructs PyYAML how to deserialize a StatsConfiguration
    instance from a key-value list seperated by semi-colons ("key1=value1;key2=value2").

    Args:
        loader (yaml.SafeLoader): PyYAML's default SafeLoader instance
        node (yaml.nodes.ScalarNode): A YAML scalar node with a string value representing a StatsStorageConfing instance

    Returns:
        StatsConfiguration: A StatsConfiguration instance deserialized from its string scalar representation
    """
    return StatsConfiguration.from_key_val_str(loader.construct_scalar(node))

def _get_loader() -> yaml.SafeLoader:
    """Returns a PyYAML SafeLoader with custom constructors added to it.

    Returns:
        yaml.SafeLoader: A PyYAML SafeLoader with custom constructors added to it
    """
    safe_loader = yaml.SafeLoader
    safe_loader.add_constructor('!StatsConfig', _stats_config_constructor)

    return safe_loader

def _get_dumper() -> yaml.SafeDumper:
    """Returns a PyYAML SafeDumper with custom representers added to it.

    Returns:
        yaml.SafeDumper: A PyYAML SafeDumper with custom representers added to it
    """
    safe_dumper = yaml.SafeDumper
    safe_dumper.add_representer(StatsConfiguration, _stats_config_representer)

    return safe_dumper
