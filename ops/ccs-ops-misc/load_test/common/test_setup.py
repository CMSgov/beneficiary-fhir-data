import common.config as config
import common.pull_bene_ids as benes
import common.pull_hashed_mbis as mbi
import common.pull_pre_adj_hashed_mbis as pambi


def generateAndLoadIds():
    configFile = config.load()
    return benes.loadData()

def generateAndLoadMbis():
    configFile = config.load()
    return mbi.loadData()

def generateAndLoadPreAdjMbis():
    configFile = config.load()
    return pambi.loadData()

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
