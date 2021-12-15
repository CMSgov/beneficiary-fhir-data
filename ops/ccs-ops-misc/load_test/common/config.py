import os
import yaml

'''
Saves a config file using the input file data.
'''
def save(fileData):
    configFile = open("config.yml", 'w')
    configFile.write("homePath: \"%s\"\n" % fileData.homePath)
    configFile.write("clientCertPath: \"%s\"\n" % fileData.clientCertPath)
    configFile.write("serverPublicKey: \"%s\"\n" % fileData.serverPublicKey)
    configFile.write("dbHost: \"%s\"\n" % fileData.dbHost)
    configFile.write("dbUsername: \"%s\"\n" % fileData.dbUsername)
    configFile.write("dbPassword: \"%s\"\n" % fileData.dbPassword)
    configFile.write("testHost: \"%s\"\n" % fileData.testHost)
    configFile.write("testRunTime: \"%s\"\n" % fileData.testRunTime)
    configFile.write("testNumTotalClients: \"%s\"\n" % fileData.testNumTotalClients)
    configFile.write("testCreatedClientsPerSecond: \"%s\"\n" % fileData.testCreatedClientsPerSecond)
    configFile.close()

'''
Requests config data from the user and creates a new test config file using that data.

Returns the loaded config, or None if nothing could be loaded or an error occurred.
'''
def create():

    ## Create a small data object for holding the input data
    class fileData: pass
    ## Prompt user for 4 config values and write to file
    fileData.homePath = input("Input full path to the home directory: ")
    fileData.clientCertPath = input("Input full path to the client cert file (pem): ")
    fileData.serverPublicKey = input("Input server public key (optional, hit enter to skip): ")
    fileData.dbHost = input("Input database host for environment under test: ")
    fileData.dbUsername = input("Input database username for environment under test: ")
    fileData.dbPassword = input("Input database password for environment under test: ")
    fileData.testHost = input("Input desired test host (BFD server ip+port to test against, ex: https://10.235.16.152:7443 or load balancer address ex. https://test.bfd.cms.gov): ")
    fileData.testRunTime = input("Input desired test run time (eg. 30s, 1m): ")
    fileData.testNumTotalClients = input("Input total number of clients to create: ")
    fileData.testCreatedClientsPerSecond = input("Input number of clients to create per second (ramp-up speed): ")
    save(fileData)

    ## Attempt to read the new file
    try:
        config = yaml.safe_load(open("config.yml"))
        return config
    except yaml.YAMLError as err:
        print("Unable to parse YAML configuration file; please check/create the file manually from the sample file.")
    except OSError as err:
        print("Could not read the new file; please try again.")

'''
Loads a config from the config file; if no file exists, will attempt to create one via user prompts.

Returns the loaded config, or None if nothing could be loaded or an error occurred.
'''
def load():
    try:
        return yaml.safe_load(open("config.yml"))
    except yaml.YAMLError as err:
        print("Unable to parse YAML configuration file; please ensure the format matches the example file.")
        return
    except OSError as err:
        print("Could not find/read configuration file; let's set it up!")
        return create()